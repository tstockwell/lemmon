package org.apache.felix.framework.searchpolicy;

import java.net.URL;
import java.util.Enumeration;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.cache.JarContent;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.R4Library;
import org.apache.felix.moduleloader.IContent;
import org.apache.felix.moduleloader.IModule;
import org.apache.felix.moduleloader.ResourceNotFoundException;

// lemmon
//    private static final Constructor m_dexFileClassConstructor;
//    private static final Method m_dexFileClassLoadClass;
//
//    static
//    {
//        Constructor dexFileClassConstructor = null;
//        Method dexFileClassLoadClass = null;
//        try
//        {
//            Class dexFileClass;
//            try
//            {
//                dexFileClass = Class.forName("dalvik.system.DexFile");
//            }
//            catch (Exception ex)
//            {
//                dexFileClass = Class.forName("android.dalvik.DexFile");
//            }
//
//            dexFileClassConstructor = dexFileClass.getConstructor(
//                new Class[] { java.io.File.class });
//            dexFileClassLoadClass = dexFileClass.getMethod("loadClass",
//                new Class[] { String.class, ClassLoader.class });
//        }
//        catch (Exception ex)
//        {
//           dexFileClassConstructor = null;
//           dexFileClassLoadClass = null;
//        }
//        m_dexFileClassConstructor = dexFileClassConstructor;
//        m_dexFileClassLoadClass = dexFileClassLoadClass;
//    }

//    public class ModuleClassLoader extends SecureClassLoader
    public class ModuleClassLoader extends ClassLoader
    {
//        private final Map m_jarContentToDexFile= null;
        ModuleImpl m_moduleImpl;

        public ModuleClassLoader(ModuleImpl moduleImpl)
        {
            m_moduleImpl= moduleImpl;
//lemmon            
//            if (m_dexFileClassConstructor != null)
//            {
//                m_jarContentToDexFile = new HashMap();
//            }
//            else
//            {
//                m_jarContentToDexFile = null;
//            }
        }

        public IModule getModule()
        {
            return m_moduleImpl;
        }

        protected Class loadClass(String name, boolean resolve)
            throws ClassNotFoundException
        {
            Class clazz = null;

            // Make sure the class was not already loaded.
            synchronized (this)
            {
                clazz = findLoadedClass(name);
            }

            if (clazz == null)
            {
                try
                {
                    clazz = (Class) m_moduleImpl.findClassOrResourceByDelegation(name, true);
                }
                catch (ResourceNotFoundException ex)
                {
                    // This should never happen since we are asking for a class,
                    // so just ignore it.
                }
                catch (ClassNotFoundException cnfe)
                {
                    ClassNotFoundException ex = cnfe;
                    String msg = name;
                    if (m_moduleImpl.m_logger.getLogLevel() >= Logger.LOG_DEBUG)
                    {
                        msg = m_moduleImpl.diagnoseClassLoadError(m_moduleImpl.m_resolver, m_moduleImpl, name);
                        ex = new ClassNotFoundException(msg, cnfe);
                    }
                    throw ex;
                }
            }

            // Resolve the class and return it.
            if (resolve)
            {
                resolveClass(clazz);
            }
            return clazz;
        }

        protected Class findClass(String name) throws ClassNotFoundException
        {
            Class clazz = null;

            // Search for class in module.
            if (clazz == null)
            {
                String actual = name.replace('.', '/') + ".class";

                byte[] bytes = null;

                // Check the module class path.
                IContent[] contentPath = m_moduleImpl.getContentPath();
                IContent content = null;
                for (int i = 0;
                    (bytes == null) &&
                    (i < contentPath.length); i++)
                {
                    bytes = contentPath[i].getEntryAsBytes(actual);
                    content = contentPath[i];
                }

                if (bytes != null)
                {
                    // Before we actually attempt to define the class, grab
                    // the lock for this class loader and make sure than no
                    // other thread has defined this class in the meantime.
                    synchronized (this)
                    {
                        clazz = findLoadedClass(name);

                        if (clazz == null)
                        {
                            // We need to try to define a Package object for the class
                            // before we call defineClass(). Get the package name and
                            // see if we have already created the package.
                            String pkgName = Util.getClassPackage(name);
                            if (pkgName.length() > 0)
                            {
                                if (getPackage(pkgName) == null)
                                {
                                    Object[] params = definePackage(pkgName);
                                    if (params != null)
                                    {
                                        definePackage(
                                            pkgName,
                                            (String) params[0],
                                            (String) params[1],
                                            (String) params[2],
                                            (String) params[3],
                                            (String) params[4],
                                            (String) params[5],
                                            null);
                                    }
                                    else
                                    {
                                        definePackage(pkgName, null, null,
                                            null, null, null, null, null);
                                    }
                                }
                            }

                            // If we can load the class from a dex file do so
                            if (content instanceof JarContent)
                            {
                                try
                                {
                                    clazz = getDexFileClass((JarContent) content, name, this);
                                }
                                catch (Exception ex)
                                {
                                    // Looks like we can't
                                }
                            }

                            if (clazz == null)
                            {
                                // If we have a security context, then use it to
                                // define the class with it for security purposes,
                                // otherwise define the class without a protection domain.
                                if (m_moduleImpl.m_protectionDomain != null)
                                {
                                    clazz = defineClass(name, bytes, 0, bytes.length,
                                        m_moduleImpl.m_protectionDomain);
                                }
                                else
                                {
                                    clazz = defineClass(name, bytes, 0, bytes.length);
                                }
                            }
                        }
                    }
                }
            }

            return clazz;
        }

        private Object[] definePackage(String pkgName)
        {
            String spectitle = (String) m_moduleImpl.m_headerMap.get("Specification-Title");
            String specversion = (String) m_moduleImpl.m_headerMap.get("Specification-Version");
            String specvendor = (String) m_moduleImpl.m_headerMap.get("Specification-Vendor");
            String impltitle = (String) m_moduleImpl.m_headerMap.get("Implementation-Title");
            String implversion = (String) m_moduleImpl.m_headerMap.get("Implementation-Version");
            String implvendor = (String) m_moduleImpl.m_headerMap.get("Implementation-Vendor");
            if ((spectitle != null)
                || (specversion != null)
                || (specvendor != null)
                || (impltitle != null)
                || (implversion != null)
                || (implvendor != null))
            {
                return new Object[] {
                    spectitle, specversion, specvendor, impltitle, implversion, implvendor
                };
            }
            return null;
        }

        private Class getDexFileClass(JarContent content, String name, ClassLoader loader)
            throws Exception
        {
            return null;
//lemmon
//            if (m_jarContentToDexFile == null)
//            {
//                return null;
//            }
//
//            Object dexFile = null;
//
//            if (!m_jarContentToDexFile.containsKey(content))
//            {
//                try
//                {
//                    dexFile = m_dexFileClassConstructor.newInstance(
//                        new Object[] { content.getFile() });
//                }
//                finally
//                {
//                    m_jarContentToDexFile.put(content, dexFile);
//                }
//            }
//            else
//            {
//                dexFile = m_jarContentToDexFile.get(content);
//            }
//
//            if (dexFile != null)
//            {
//                return (Class) m_dexFileClassLoadClass.invoke(dexFile,
//                    new Object[] { name.replace('.','/'), loader });
//            }
//            return null;
        }

        public URL getResource(String name)
        {
            return m_moduleImpl.getResourceByDelegation(name);
        }

        public URL findResource(String name)
        {
            return m_moduleImpl.getResourceLocal(name);
        }

        // The findResources() method should only look at the module itself, but
        // instead it tries to delegate because in Java version prior to 1.5 the
        // getResources() method was final and could not be overridden. We should
        // override getResources() like getResource() to make it delegate, but we
        // can't. As a workaround, we make findResources() delegate instead.
        public Enumeration findResources(String name)
        {
            return m_moduleImpl.getResourcesByDelegation(name);
        }

        protected String findLibrary(String name)
        {
            // Remove leading slash, if present.
            if (name.startsWith("/"))
            {
                name = name.substring(1);
            }

            R4Library[] libs = m_moduleImpl.getNativeLibraries();
            for (int i = 0; (libs != null) && (i < libs.length); i++)
            {
                if (libs[i].match(name))
                {
                    return m_moduleImpl.getContent().getEntryAsNativeLibrary(libs[i].getEntryName());
                }
            }

            return null;
        }

        public String toString()
        {
            return m_moduleImpl.toString();
        }
    }

