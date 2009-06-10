/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.framework.searchpolicy;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.Felix.FelixResolver;
import org.apache.felix.framework.cache.JarContent;
import org.apache.felix.framework.util.CompoundEnumeration;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.SecureAction;
import org.apache.felix.framework.util.SecurityManagerEx;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.ManifestParser;
import org.apache.felix.framework.util.manifestparser.R4Library;
import org.apache.felix.framework.util.manifestparser.Requirement;
import org.apache.felix.moduleloader.ICapability;
import org.apache.felix.moduleloader.IContent;
import org.apache.felix.moduleloader.IModule;
import org.apache.felix.moduleloader.IRequirement;
import org.apache.felix.moduleloader.IWire;
import org.apache.felix.moduleloader.ResourceNotFoundException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;

import com.googlecode.lemmon.Logging;

public class ModuleImpl implements IModule
{
    static {
        Logging.debug( "ModuleImpl:1" );
    }
    
    final Logger m_logger;
    private final Map m_configMap;
    final FelixResolver m_resolver;
    private final String m_id;
    private final IContent m_content;
    final Map m_headerMap;

    private final String m_manifestVersion;
    private final boolean m_isExtension;
    private final String m_symbolicName;
    private final Version m_version;

    private final ICapability[] m_capabilities;
    private final IRequirement[] m_requirements;
    private final IRequirement[] m_dynamicRequirements;
    private final R4Library[] m_nativeLibraries;
    private final Bundle m_bundle;

    private IModule[] m_fragments = null;
    private IWire[] m_wires = null;
    private IModule[] m_dependentHosts = new IModule[0];
    private IModule[] m_dependentImporters = new IModule[0];
    private IModule[] m_dependentRequirers = new IModule[0];
    private volatile boolean m_isResolved = false;

    private IContent[] m_contentPath;
    private IContent[] m_fragmentContents = null;
    private ModuleClassLoader m_classLoader;
    ProtectionDomain m_protectionDomain = null;
    private static SecureAction m_secureAction = new SecureAction();

    // Boot delegation packages.
    private final String[] m_bootPkgs;
    private final boolean[] m_bootPkgWildcards;

    // Re-usable security manager for accessing class context.
    private static SecurityManagerEx m_sm = new SecurityManagerEx();

    // Thread local to detect class loading cycles.
    private final ThreadLocal m_cycleCheck = new ThreadLocal();

    /**
     * This constructor is used by the extension manager, since it needs
     * a constructor that does not throw an exception.
     * @param logger
     * @param bundle
     * @param id
     * @param bootPkgs
     * @param bootPkgWildcards
     * @throws org.osgi.framework.BundleException
     */
    public ModuleImpl(
        Logger logger, Bundle bundle, String id,
        String[] bootPkgs, boolean[] bootPkgWildcards)
    {
        Logging.debug( "ModuleImpl:2" );
        m_logger = logger;
        m_configMap = null;
        m_resolver = null;
        m_bundle = bundle;
        m_id = id;
        m_headerMap = null;
        m_content = null;
        m_bootPkgs = bootPkgs;
        m_bootPkgWildcards = bootPkgWildcards;
        m_manifestVersion = null;
        m_symbolicName = null;
        m_isExtension = false;
        m_version = null;
        m_capabilities = null;
        m_requirements = null;
        m_dynamicRequirements = null;
        m_nativeLibraries = null;
    }

    public ModuleImpl(
        Logger logger, Map configMap, FelixResolver resolver,
        Bundle bundle, String id, Map headerMap, IContent content,
        String[] bootPkgs,
        boolean[] bootPkgWildcards)
        throws BundleException
    {
        m_logger = logger;
        m_configMap = configMap;
        m_resolver = resolver;
        m_bundle = bundle;
        m_id = id;
        m_headerMap = headerMap;
        m_content = content;
        m_bootPkgs = bootPkgs;
        m_bootPkgWildcards = bootPkgWildcards;

        ManifestParser mp = new ManifestParser(m_logger, m_configMap, m_headerMap);

        // Record some of the parsed metadata. Note, if this is an extension
        // bundle it's exports are removed, since they will be added to the
        // system bundle directly later on.
        m_manifestVersion = mp.getManifestVersion();
        m_version = mp.getBundleVersion();
        m_capabilities = mp.isExtension() ? null : mp.getCapabilities();
        m_requirements = mp.getRequirements();
        m_dynamicRequirements = mp.getDynamicRequirements();
        m_nativeLibraries = mp.getLibraries();
        m_symbolicName = mp.getSymbolicName();
        m_isExtension = mp.isExtension();

        // Verify that all native libraries exist in advance; this will
        // throw an exception if the native library does not exist.
        try
        {
            for (int i = 0;
                (m_nativeLibraries != null) && (i < m_nativeLibraries.length);
                i++)
            {
                String entryName = m_nativeLibraries[i].getEntryName();
                if (entryName != null)
                {
                    if (m_content.getEntryAsNativeLibrary(entryName) == null)
                    {
                        throw new BundleException("Native library does not exist: " + entryName);
                    }
                }
            }
        }
        finally
        {
            // We close the module content here to make sure it is closed
            // to avoid having to close it if there is an exception during
            // the entire module creation process.
// TODO: REFACTOR - If we do the above check here, then we open the module's content
//       immediately every time, which means we must close it here so we don't have
//       to remember to close it if there are other failures during module init.
            m_content.close();
        }
    }

    //
    // Metadata access methods.
    //

    public Map getHeaders()
    {
        return m_headerMap;
    }

    public boolean isExtension()
    {
        return m_isExtension;
    }

    public String getSymbolicName()
    {
        return m_symbolicName;
    }

    public String getManifestVersion()
    {
        return m_manifestVersion;
    }

    public Version getVersion()
    {
        return m_version;
    }

    public ICapability[] getCapabilities()
    {
        return m_capabilities;
    }

    public IRequirement[] getRequirements()
    {
        return m_requirements;
    }

    public IRequirement[] getDynamicRequirements()
    {
        return m_dynamicRequirements;
    }

    public R4Library[] getNativeLibraries()
    {
        return m_nativeLibraries;
    }

    //
    // Run-time data access.
    //

    public Bundle getBundle()
    {
        return m_bundle;
    }

    public String getId()
    {
        return m_id;
    }

    public synchronized IWire[] getWires()
    {
        return m_wires;
    }

    public synchronized void setWires(IWire[] wires)
    {
        // Remove module from old wire modules' dependencies,
        // since we are no longer dependent on any the moduels
        // from the old wires.
        for (int i = 0; (m_wires != null) && (i < m_wires.length); i++)
        {
            if (m_wires[i].getCapability().getNamespace().equals(ICapability.MODULE_NAMESPACE))
            {
                ((ModuleImpl) m_wires[i].getExporter()).removeDependentRequirer(this);
            }
            else if (m_wires[i].getCapability().getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                ((ModuleImpl) m_wires[i].getExporter()).removeDependentImporter(this);
            }
        }

        m_wires = wires;

        // Add ourself as a dependent to the new wires' modules.
        for (int i = 0; (m_wires != null) && (i < m_wires.length); i++)
        {
            if (m_wires[i].getCapability().getNamespace().equals(ICapability.MODULE_NAMESPACE))
            {
                ((ModuleImpl) m_wires[i].getExporter()).addDependentRequirer(this);
            }
            else if (m_wires[i].getCapability().getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                ((ModuleImpl) m_wires[i].getExporter()).addDependentImporter(this);
            }
        }
    }

    public boolean isResolved()
    {
        return m_isResolved;
    }

    public void setResolved()
    {
        m_isResolved = true;
    }

    //
    // Content access methods.
    //

    public IContent getContent()
    {
        return m_content;
    }

    synchronized IContent[] getContentPath()
    {
        if (m_contentPath == null)
        {
            try
            {
                m_contentPath = initializeContentPath();
            }
            catch (Exception ex)
            {
                m_logger.log(Logger.LOG_ERROR, "Unable to get module class path.", ex);
            }
        }
        return m_contentPath;
    }

    private IContent[] initializeContentPath() throws Exception
    {
        List contentList = new ArrayList();
        calculateContentPath(m_content, contentList, true);
        for (int i = 0; (m_fragmentContents != null) && (i < m_fragmentContents.length); i++)
        {
            calculateContentPath(m_fragmentContents[i], contentList, false);
        }
        return (IContent[]) contentList.toArray(new IContent[contentList.size()]);
    }

    private List calculateContentPath(IContent content, List contentList, boolean searchFragments)
        throws Exception
    {
        // Creating the content path entails examining the bundle's
        // class path to determine whether the bundle JAR file itself
        // is on the bundle's class path and then creating content
        // objects for everything on the class path.

        // Create a list to contain the content path for the specified content.
        List localContentList = new ArrayList();

        // Find class path meta-data.
        String classPath = (String) m_headerMap.get(FelixConstants.BUNDLE_CLASSPATH);
        // Parse the class path into strings.
        String[] classPathStrings = ManifestParser.parseDelimitedString(
            classPath, FelixConstants.CLASS_PATH_SEPARATOR);

        if (classPathStrings == null)
        {
            classPathStrings = new String[0];
        }

        // Create the bundles class path.
        for (int i = 0; i < classPathStrings.length; i++)
        {
            // Remove any leading slash, since all bundle class path
            // entries are relative to the root of the bundle.
            classPathStrings[i] = (classPathStrings[i].startsWith("/"))
                ? classPathStrings[i].substring(1)
                : classPathStrings[i];

            // Check for the bundle itself on the class path.
            if (classPathStrings[i].equals(FelixConstants.CLASS_PATH_DOT))
            {
                localContentList.add(content);
            }
            else
            {
                // Try to find the embedded class path entry in the current
                // content.
                IContent embeddedContent = content.getEntryAsContent(classPathStrings[i]);
                // If the embedded class path entry was not found, it might be
                // in one of the fragments if the current content is the bundle,
                // so try to search the fragments if necessary.
                for (int fragIdx = 0;
                    searchFragments && (embeddedContent == null)
                        && (m_fragmentContents != null) && (fragIdx < m_fragmentContents.length);
                    fragIdx++)
                {
                    embeddedContent = m_fragmentContents[fragIdx].getEntryAsContent(classPathStrings[i]);
                }
                // If we found the embedded content, then add it to the
                // class path content list.
                if (embeddedContent != null)
                {
                    localContentList.add(embeddedContent);
                }
                else
                {
// TODO: FRAMEWORK - Per the spec, this should fire a FrameworkEvent.INFO event;
//       need to create an "Eventer" class like "Logger" perhaps.
                    m_logger.log(Logger.LOG_INFO,
                        "Class path entry not found: "
                        + classPathStrings[i]);
                }
            }
        }

        // If there is nothing on the class path, then include
        // "." by default, as per the spec.
        if (localContentList.size() == 0)
        {
            localContentList.add(content);
        }

        // Now add the local contents to the global content list and return it.
        contentList.addAll(localContentList);
        return contentList;
    }

    public Class getClassByDelegation(String name) throws ClassNotFoundException
    {
        return getClassLoader().loadClass(name);
    }

    public URL getResourceByDelegation(String name)
    {
        try
        {
            return (URL) findClassOrResourceByDelegation(name, false);
        }
        catch (ClassNotFoundException ex)
        {
            // This should never be thrown because we are loading resources.
        }
        catch (ResourceNotFoundException ex)
        {
            m_logger.log(
                Logger.LOG_DEBUG,
                ex.getMessage(),
                ex);
        }
        return null;
    }

    Object findClassOrResourceByDelegation(String name, boolean isClass)
        throws ClassNotFoundException, ResourceNotFoundException
    {
        Object result = null;

        Set requestSet = (Set) m_cycleCheck.get();
        if (requestSet == null)
        {
            requestSet = new HashSet();
            m_cycleCheck.set(requestSet);
        }
        if (requestSet.add(name))
        {
            try
            {
                // First, try to resolve the originating module.
                m_resolver.resolve(this);

                // Get the package of the target class/resource.
                String pkgName = (isClass)
                    ? Util.getClassPackage(name)
                    : Util.getResourcePackage(name);

                // Delegate any packages listed in the boot delegation
                // property to the parent class loader.
                if (shouldBootDelegate(pkgName))
                {
                    try
                    {
                        result = (isClass)
                            ? (Object) getClass().getClassLoader().loadClass(name)
                            : (Object) getClass().getClassLoader().getResource(name);
                        // If this is a java.* package, then always terminate the
                        // search; otherwise, continue to look locally if not found.
                        if (pkgName.startsWith("java.") || (result != null))
                        {
                            return result;
                        }
                    }
                    catch (ClassNotFoundException ex)
                    {
                        // If this is a java.* package, then always terminate the
                        // search; otherwise, continue to look locally if not found.
                        if (pkgName.startsWith("java."))
                        {
                            throw ex;
                        }
                    }
                }

                // Look in the module's imports. Note that the search may
                // be aborted if this method throws an exception, otherwise
                // it continues if a null is returned.
                result = searchImports(name, isClass);

                // If not found, try the module's own class path.
                if (result == null)
                {
                    result = (isClass)
                        ? (Object) getClassLoader().findClass(name)
                        : (Object) getResourceLocal(name);

                    // If still not found, then try the module's dynamic imports.
                    if (result == null)
                    {
                        result = searchDynamicImports(name, pkgName, isClass);
                    }
                }
            }
            catch (ResolveException ex)
            {
                if (isClass)
                {
                    // We do not use the resolve exception as the
                    // cause of the exception, since this would
                    // potentially leak internal module information.
                    throw new ClassNotFoundException(
                        name + ": cannot resolve package "
                        + ex.getRequirement());
                }
                else
                {
                    // The spec states that if the bundle cannot be resolved, then
                    // only the local bundle's resources should be searched. So we
                    // will ask the module's own class path.
                    URL url = getResourceLocal(name);
                    if (url != null)
                    {
                        return url;
                    }

                    // We need to throw a resource not found exception.
                    throw new ResourceNotFoundException(
                        name + ": cannot resolve package "
                        + ex.getRequirement());
                }
            }
            finally
            {
                requestSet.remove(name);
            }
        }
        else
        {
            // If a cycle is detected, we should return null to break the
            // cycle. This should only ever be return to internal class
            // loading code and not to the actual instigator of the class load.
            return null;
        }

        if (result == null)
        {
            if (isClass)
            {
                throw new ClassNotFoundException(name);
            }
            else
            {
                throw new ResourceNotFoundException(name);
            }
        }

        return result;
    }

    URL getResourceLocal(String name)
    {
        URL url = null;

        // Remove leading slash, if present, but special case
        // "/" so that it returns a root URL...this isn't very
        // clean or meaninful, but the Spring guys want it.
        if (name.equals("/"))
        {
            // Just pick a class path index since it doesn't really matter.
            url = createURL(1, name);
        }
        else if (name.startsWith("/"))
        {
            name = name.substring(1);
        }

        // Check the module class path.
        IContent[] contentPath = getContentPath();
        for (int i = 0;
            (url == null) &&
            (i < contentPath.length); i++)
        {
            if (contentPath[i].hasEntry(name))
            {
                url = createURL(i + 1, name);
            }
        }

        return url;
    }

    public Enumeration getResourcesByDelegation(String name)
    {
        Set requestSet = (Set) m_cycleCheck.get();
        if (requestSet == null)
        {
            requestSet = new HashSet();
            m_cycleCheck.set(requestSet);
        }
        if (!requestSet.contains(name))
        {
            requestSet.add(name);
            try
            {
                return findResourcesByDelegation(name);
            }
            finally
            {
                requestSet.remove(name);
            }
        }

        return null;
    }

    private Enumeration findResourcesByDelegation(String name)
    {
        Enumeration urls = null;
        List completeUrlList = new ArrayList();

        // First, try to resolve the originating module.
        try
        {
            m_resolver.resolve(this);
        }
        catch (ResolveException ex)
        {
            // The spec states that if the bundle cannot be resolved, then
            // only the local bundle's resources should be searched. So we
            // will ask the module's own class path.
            urls = getResourcesLocal(name);
            return urls;
        }

        // Get the package of the target class/resource.
        String pkgName = Util.getResourcePackage(name);

        // Delegate any packages listed in the boot delegation
        // property to the parent class loader.
        if (shouldBootDelegate(pkgName))
        {
            try
            {
                urls = getClass().getClassLoader().getResources(name);
            }
            catch (IOException ex)
            {
                // This shouldn't happen and even if it does, there
                // is nothing we can do, so just ignore it.
            }
            // If this is a java.* package, then always terminate the
            // search; otherwise, continue to look locally.
            if (pkgName.startsWith("java."))
            {
                return urls;
            }

            completeUrlList.add(urls);
        }

        // Look in the module's imports.
        // We delegate to the module's wires for the resources.
        // If any resources are found, this means that the package of these
        // resources is imported, we must not keep looking since we do not
        // support split-packages.

        // Note that the search may be aborted if this method throws an
        // exception, otherwise it continues if a null is returned.
        IWire[] wires = getWires();
        for (int i = 0; (wires != null) && (i < wires.length); i++)
        {
            if (wires[i] instanceof R4Wire)
            {
                try
                {
                    // If we find the class or resource, then return it.
                    urls = wires[i].getResources(name);
                }
                catch (ResourceNotFoundException ex)
                {
                    urls = null;
                }
                if (urls != null)
                {
                    completeUrlList.add(urls);
                    return new CompoundEnumeration((Enumeration[])
                        completeUrlList.toArray(new Enumeration[completeUrlList.size()]));
                }
            }
        }

        // See whether we can get the resource from the required bundles and
        // regardless of whether or not this is the case continue to the next
        // step potentially passing on the result of this search (if any).
        for (int i = 0; (wires != null) && (i < wires.length); i++)
        {
            if (wires[i] instanceof R4WireModule)
            {
                try
                {
                    // If we find the class or resource, then add it.
                    urls = wires[i].getResources(name);
                }
                catch (ResourceNotFoundException ex)
                {
                    urls = null;
                }
                if (urls != null)
                {
                    completeUrlList.add(urls);
                }
            }
        }

        // Try the module's own class path. If we can find the resource then
        // return it together with the results from the other searches else
        // try to look into the dynamic imports.
        urls = getResourcesLocal(name);
        if (urls != null)
        {
            completeUrlList.add(urls);
        }
        else
        {
            // If not found, then try the module's dynamic imports.
            // At this point, the module's imports were searched and so was the
            // the module's content. Now we make an attempt to load the
            // class/resource via a dynamic import, if possible.
            IWire wire = null;
            try
            {
                wire = m_resolver.resolveDynamicImport(this, pkgName);
            }
            catch (ResolveException ex)
            {
                // Ignore this since it is likely normal.
            }
            if (wire != null)
            {
                try
                {
                    urls = wire.getResources(name);
                }
                catch (ResourceNotFoundException ex)
                {
                    urls = null;
                }
                if (urls != null)
                {
                    completeUrlList.add(urls);
                }
            }
        }

        return new CompoundEnumeration((Enumeration[])
            completeUrlList.toArray(new Enumeration[completeUrlList.size()]));
    }

    private Enumeration getResourcesLocal(String name)
    {
        Vector v = new Vector();

        // Special case "/" so that it returns a root URLs for
        // each bundle class path entry...this isn't very
        // clean or meaningful, but the Spring guys want it.
        final IContent[] contentPath = getContentPath();
        if (name.equals("/"))
        {
            for (int i = 0; i < contentPath.length; i++)
            {
                v.addElement(createURL(i + 1, name));
            }
        }
        else
        {
            // Remove leading slash, if present.
            if (name.startsWith("/"))
            {
                name = name.substring(1);
            }

            // Check the module class path.
            for (int i = 0; i < contentPath.length; i++)
            {
                if (contentPath[i].hasEntry(name))
                {
                    // Use the class path index + 1 for creating the path so
                    // that we can differentiate between module content URLs
                    // (where the path will start with 0) and module class
                    // path URLs.
                    v.addElement(createURL(i + 1, name));
                }
            }
        }

        return v.elements();
    }

    // TODO: API: Investigate how to handle this better, perhaps we need
    // multiple URL policies, one for content -- one for class path.
    public URL getEntry(String name)
    {
        URL url = null;

        // Check for the special case of "/", which represents
        // the root of the bundle according to the spec.
        if (name.equals("/"))
        {
            url = createURL(0, "/");
        }

        if (url == null)
        {
            // Remove leading slash, if present.
            if (name.startsWith("/"))
            {
                name = name.substring(1);
            }

            // Check the module content.
            if (getContent().hasEntry(name))
            {
                // Module content URLs start with 0, whereas module
                // class path URLs start with the index into the class
                // path + 1.
                url = createURL(0, name);
            }
        }

        return url;
    }

    public boolean hasInputStream(int index, String urlPath)
    {
        if (urlPath.startsWith("/"))
        {
            urlPath = urlPath.substring(1);
        }
        if (index == 0)
        {
            return m_content.hasEntry(urlPath);
        }
        return getContentPath()[index - 1].hasEntry(urlPath);
    }

    public InputStream getInputStream(int index, String urlPath)
        throws IOException
    {
        if (urlPath.startsWith("/"))
        {
            urlPath = urlPath.substring(1);
        }
        if (index == 0)
        {
            return m_content.getEntryAsStream(urlPath);
        }
        return getContentPath()[index - 1].getEntryAsStream(urlPath);
    }

    private URL createURL(int port, String path)
    {
         // Add a slash if there is one already, otherwise
         // the is no slash separating the host from the file
         // in the resulting URL.
         if (!path.startsWith("/"))
         {
             path = "/" + path;
         }

         try
         {
             return m_secureAction.createURL(
                 FelixConstants.BUNDLE_URL_PROTOCOL,
                 m_id, port, path);
         }
         catch (MalformedURLException ex)
         {
             m_logger.log(
                 Logger.LOG_ERROR,
                 "Unable to create resource URL.",
                 ex);
         }
         return null;
    }

    //
    // Fragment and dependency management methods.
    //

    public synchronized IModule[] getFragments()
    {
        return m_fragments;
    }

    public synchronized void attachFragments(IModule[] fragments) throws Exception
    {
        // Remove module from old fragment dependencies.
        // We will generally only remove module fragment
        // dependencies when we are uninstalling the module.
        for (int i = 0; (m_fragments != null) && (i < m_fragments.length); i++)
        {
            ((ModuleImpl) m_fragments[i]).removeDependentHost(this);
        }

        // Update the dependencies on the new fragments.
        m_fragments = fragments;

        // We need to add ourself as a dependent of each fragment
        // module. We also need to create an array of fragment contents
        // to attach to our content loader.
        if (m_fragments != null)
        {
            IContent[] fragmentContents = new IContent[m_fragments.length];
            for (int i = 0; (m_fragments != null) && (i < m_fragments.length); i++)
            {
                ((ModuleImpl) m_fragments[i]).addDependentHost(this);
                fragmentContents[i] =
                    m_fragments[i].getContent()
                        .getEntryAsContent(FelixConstants.CLASS_PATH_DOT);
            }
            // Now attach the fragment contents to our content loader.
            attachFragmentContents(fragmentContents);
        }
    }

    private void attachFragmentContents(IContent[] fragmentContents)
        throws Exception
    {
        // Close existing fragment contents.
        if (m_fragmentContents != null)
        {
            for (int i = 0; i < m_fragmentContents.length; i++)
            {
                m_fragmentContents[i].close();
            }
        }
        m_fragmentContents = fragmentContents;

        if (m_contentPath != null)
        {
            for (int i = 0; i < m_contentPath.length; i++)
            {
                m_contentPath[i].close();
            }
        }
        m_contentPath = initializeContentPath();
    }

    public synchronized IModule[] getDependentHosts()
    {
        return m_dependentHosts;
    }

    public synchronized void addDependentHost(IModule module)
    {
        m_dependentHosts = addDependent(m_dependentHosts, module);
    }

    public synchronized void removeDependentHost(IModule module)
    {
        m_dependentHosts = removeDependent(m_dependentHosts, module);
    }

    public synchronized IModule[] getDependentImporters()
    {
        return m_dependentImporters;
    }

    public synchronized void addDependentImporter(IModule module)
    {
        m_dependentImporters = addDependent(m_dependentImporters, module);
    }

    public synchronized void removeDependentImporter(IModule module)
    {
        m_dependentImporters = removeDependent(m_dependentImporters, module);
    }

    public synchronized IModule[] getDependentRequirers()
    {
        return m_dependentRequirers;
    }

    public synchronized void addDependentRequirer(IModule module)
    {
        m_dependentRequirers = addDependent(m_dependentRequirers, module);
    }

    public synchronized void removeDependentRequirer(IModule module)
    {
        m_dependentRequirers = removeDependent(m_dependentRequirers, module);
    }

    public synchronized IModule[] getDependents()
    {
        IModule[] dependents = new IModule[
            m_dependentHosts.length + m_dependentImporters.length + m_dependentRequirers.length];
        System.arraycopy(
            m_dependentHosts,
            0,
            dependents,
            0,
            m_dependentHosts.length);
        System.arraycopy(
            m_dependentImporters,
            0,
            dependents,
            m_dependentHosts.length,
            m_dependentImporters.length);
        System.arraycopy(
            m_dependentRequirers,
            0,
            dependents,
            m_dependentHosts.length + m_dependentImporters.length,
            m_dependentRequirers.length);
        return dependents;
    }

    private static IModule[] addDependent(IModule[] modules, IModule module)
    {
        // Make sure the dependent module is not already present.
        for (int i = 0; i < modules.length; i++)
        {
            if (modules[i].equals(module))
            {
                return modules;
            }
        }
        IModule[] tmp = new IModule[modules.length + 1];
        System.arraycopy(modules, 0, tmp, 0, modules.length);
        tmp[modules.length] = module;
        return tmp;
    }

    private static IModule[] removeDependent(IModule[] modules, IModule module)
    {
        IModule[] tmp = modules;

        // Make sure the dependent module is present.
        for (int i = 0; i < modules.length; i++)
        {
            if (modules[i].equals(module))
            {
                // If this is the module, then point to empty list.
                if ((modules.length - 1) == 0)
                {
                    tmp = new IModule[0];
                }
                // Otherwise, we need to do some array copying.
                else
                {
                    tmp = new IModule[modules.length - 1];
                    System.arraycopy(modules, 0, tmp, 0, i);
                    if (i < tmp.length)
                    {
                        System.arraycopy(modules, i + 1, tmp, i, tmp.length - i);
                    }
                }
                break;
            }
        }

        return tmp;
    }

    public synchronized void close()
    {
        m_content.close();
        for (int i = 0; (m_contentPath != null) && (i < m_contentPath.length); i++)
        {
            m_contentPath[i].close();
        }
        for (int i = 0; (m_fragmentContents != null) && (i < m_fragmentContents.length); i++)
        {
            m_fragmentContents[i].close();
        }
        m_classLoader = null;
    }

    public synchronized void setSecurityContext(Object securityContext)
    {
        m_protectionDomain = (ProtectionDomain) securityContext;
    }

    public synchronized Object getSecurityContext()
    {
        return m_protectionDomain;
    }

    public String toString()
    {
        return m_id;
    }

    private synchronized ModuleClassLoader getClassLoader()
    {
        if (m_classLoader == null)
        {
// TODO: REFACTOR - SecureAction fix needed.
              m_classLoader = new ModuleClassLoader(this);
//            m_classLoader = m_secureAction.createModuleClassLoader(
//                this, m_protectionDomain);
        }
        return m_classLoader;
    }

    private Object searchImports(String name, boolean isClass)
        throws ClassNotFoundException, ResourceNotFoundException
    {
        // We delegate to the module's wires to find the class or resource.
        IWire[] wires = getWires();
        for (int i = 0; (wires != null) && (i < wires.length); i++)
        {
            // If we find the class or resource, then return it.
            Object result = (isClass)
                ? (Object) wires[i].getClass(name)
                : (Object) wires[i].getResource(name);
            if (result != null)
            {
                return result;
            }
        }

        return null;
    }

    private Object searchDynamicImports(
        String name, String pkgName, boolean isClass)
        throws ClassNotFoundException, ResourceNotFoundException
    {
        // At this point, the module's imports were searched and so was the
        // the module's content. Now we make an attempt to load the
        // class/resource via a dynamic import, if possible.
        IWire wire = null;
        try
        {
            wire = m_resolver.resolveDynamicImport(this, pkgName);
        }
        catch (ResolveException ex)
        {
            // Ignore this since it is likely normal.
        }

        // If the dynamic import was successful, then this initial
        // time we must directly return the result from dynamically
        // created wire, but subsequent requests for classes/resources
        // in the associated package will be processed as part of
        // normal static imports.
        if (wire != null)
        {
            // Return the class or resource.
            return (isClass)
                ? (Object) wire.getClass(name)
                : (Object) wire.getResource(name);
        }

        // At this point, the class/resource could not be found by the bundle's
        // static or dynamic imports, nor its own content. Before we throw
        // an exception, we will try to determine if the instigator of the
        // class/resource load was a class from a bundle or not. This is necessary
        // because the specification mandates that classes on the class path
        // should be hidden (except for java.*), but it does allow for these
        // classes/resources to be exposed by the system bundle as an export.
        // However, in some situations classes on the class path make the faulty
        // assumption that they can access everything on the class path from
        // every other class loader that they come in contact with. This is
        // not true if the class loader in question is from a bundle. Thus,
        // this code tries to detect that situation. If the class
        // instigating the load request was NOT from a bundle, then we will
        // make the assumption that the caller actually wanted to use the
        // parent class loader and we will delegate to it. If the class was
        // from a bundle, then we will enforce strict class loading rules
        // for the bundle and throw an exception.

        // Get the class context to see the classes on the stack.
        Class[] classes = m_sm.getClassContext();
        // Start from 1 to skip security manager class.
        for (int i = 1; i < classes.length; i++)
        {
            // Find the first class on the call stack that is not from
            // the class loader that loaded the Felix classes or is not
            // a class loader or class itself, because we want to ignore
            // calls to ClassLoader.loadClass() and Class.forName() since
            // we are trying to find out who instigated the class load.
            // Also ignore inner classes of class loaders, since we can
            // assume they are a class loader too.

// TODO: FRAMEWORK - This check is a hack and we should see if we can think
// of another way to do it, since it won't necessarily work in all situations.
            // Since Felix uses threads for changing the start level
            // and refreshing packages, it is possible that there is no
            // module classes on the call stack; therefore, as soon as we
            // see Thread on the call stack we exit this loop. Other cases
            // where modules actually use threads are not an issue because
            // the module classes will be on the call stack before the
            // Thread class.
            if (Thread.class.equals(classes[i]))
            {
                break;
            }
            else if (isClassNotLoadedFromBundle(classes[i]))
            {
                // If the instigating class was not from a bundle,
                // then delegate to the parent class loader; otherwise,
                // break out of loop and return null.
                boolean delegate = true;
                for (ClassLoader cl = classes[i].getClassLoader(); cl != null; cl = cl.getClass().getClassLoader())
                {
                    if (ModuleClassLoader.class.isInstance(cl))
                    {
                        delegate = false;
                        break;
                    }
                }
                // Delegate to the parent class loader unless this call
                // is due to outside code calling a method on the bundle
                // interface (e.g., Bundle.loadClass()).
                if (delegate && !Bundle.class.isInstance(classes[i - 1]))
                {
                    try
                    {
                        // Return the class or resource from the parent class loader.
                        return (isClass)
                            ? (Object) this.getClass().getClassLoader().loadClass(name)
                            : (Object) this.getClass().getClassLoader().getResource(name);
                    }
                    catch (NoClassDefFoundError ex)
                    {
                        // Ignore, will return null
                    }
                }
                break;
            }
        }

        return null;
    }

    private boolean isClassNotLoadedFromBundle(Class clazz)
    {
        // If this is an inner class, try to get the enclosing class
        // because we can assume that inner classes of class loaders
        // are really just the class loader and we should ignore them.
        clazz = getEnclosingClass(clazz);
        return (this.getClass().getClassLoader() != clazz.getClassLoader())
            && !ClassLoader.class.isAssignableFrom(clazz)
            && !Class.class.equals(clazz)
            && !Proxy.class.equals(clazz);
    }

    private static Class getEnclosingClass(Class clazz)
    {
        // This code determines if the class is an inner class and if so
        // returns the enclosing class. At one point in time this code used
        // Class.getEnclosingClass() for JDKs > 1.5, but due to a bug in the
        // JDK which caused  invalid ClassCircularityErrors we had to remove it.
        int idx = clazz.getName().lastIndexOf('$');
        if (idx > 0)
        {
            ClassLoader cl = (clazz.getClassLoader() != null)
                ? clazz.getClassLoader() : ClassLoader.getSystemClassLoader();
            try
            {
                Class enclosing = cl.loadClass(clazz.getName().substring(0, idx));
                clazz = (enclosing != null) ? enclosing : clazz;
            }
            catch (Throwable t)
            {
                // Ignore all problems since we are trying to load a class
                // inside the class loader and this can lead to
                // ClassCircularityError, for example.
            }
        }

        return clazz;
    }

    private boolean shouldBootDelegate(String pkgName)
    {
        boolean result = false;

        // Only consider delegation if we have a package name, since
        // we don't want to promote the default package. The spec does
        // not take a stand on this issue.
        if (pkgName.length() > 0)
        {
            for (int i = 0; !result && (i < m_bootPkgs.length); i++)
            {
                // Check if the boot package is wildcarded.
                if (m_bootPkgWildcards[i])
                {
                    // A wildcarded boot package will be in the form "foo.",
                    // so a matching subpackage will start with "foo.", e.g.,
                    // "foo.bar".
                    if (pkgName.startsWith(m_bootPkgs[i]))
                    {
                        return true;
                    }
                    // If we have "foo." as our wildcarded boot package, then
                    // the package "foo" should be delegated too, but we don't
                    // want to delegate "foobar", so we check to make sure the
                    // package names are the same length and then perform a
                    // region match to ignore the "." on "foo.".
                    else if ((pkgName.length() == m_bootPkgs[i].length() - 1)
                        && pkgName.regionMatches(0, m_bootPkgs[i], 0, m_bootPkgs[i].length() - 1))
                    {
                        return true;
                    }
                }
                // If not wildcarded, then check for an exact match.
                else if (m_bootPkgs[i].equals(pkgName))
                {
                    return true;
                }
            }
        }

        return result;
    }

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

static String diagnoseClassLoadError(
        FelixResolver resolver, ModuleImpl module, String name)
    {
        // We will try to do some diagnostics here to help the developer
        // deal with this exception.

        // Get package name.
        String pkgName = Util.getClassPackage(name);

        // First, get the bundle ID of the module doing the class loader.
        long impId = Util.getBundleIdFromModuleId(module.getId());

        // Next, check to see if the module imports the package.
        IWire[] wires = module.getWires();
        for (int i = 0; (wires != null) && (i < wires.length); i++)
        {
            if (wires[i].getCapability().getNamespace().equals(ICapability.PACKAGE_NAMESPACE) &&
                wires[i].getCapability().getProperties().get(ICapability.PACKAGE_PROPERTY).equals(pkgName))
            {
                long expId = Util.getBundleIdFromModuleId(wires[i].getExporter().getId());

                StringBuffer sb = new StringBuffer("*** Package '");
                sb.append(pkgName);
                sb.append("' is imported by bundle ");
                sb.append(impId);
                sb.append(" from bundle ");
                sb.append(expId);
                sb.append(", but the exported package from bundle ");
                sb.append(expId);
                sb.append(" does not contain the requested class '");
                sb.append(name);
                sb.append("'. Please verify that the class name is correct in the importing bundle ");
                sb.append(impId);
                sb.append(" and/or that the exported package is correctly bundled in ");
                sb.append(expId);
                sb.append(". ***");

                return sb.toString();
            }
        }

        // Next, check to see if the package was optionally imported and
        // whether or not there is an exporter available.
        IRequirement[] reqs = module.getRequirements();
/*
* TODO: RB - Fix diagnostic message for optional imports.
        for (int i = 0; (reqs != null) && (i < reqs.length); i++)
        {
            if (reqs[i].getName().equals(pkgName) && reqs[i].isOptional())
            {
                // Try to see if there is an exporter available.
                IModule[] exporters = getResolvedExporters(reqs[i], true);
                exporters = (exporters.length == 0)
                    ? getUnresolvedExporters(reqs[i], true) : exporters;

                // An exporter might be available, but it may have attributes
                // that do not match the importer's required attributes, so
                // check that case by simply looking for an exporter of the
                // desired package without any attributes.
                if (exporters.length == 0)
                {
                    IRequirement pkgReq = new Requirement(
                        ICapability.PACKAGE_NAMESPACE, "(package=" + pkgName + ")");
                    exporters = getResolvedExporters(pkgReq, true);
                    exporters = (exporters.length == 0)
                        ? getUnresolvedExporters(pkgReq, true) : exporters;
                }

                long expId = (exporters.length == 0)
                    ? -1 : Util.getBundleIdFromModuleId(exporters[0].getId());

                StringBuffer sb = new StringBuffer("*** Class '");
                sb.append(name);
                sb.append("' was not found, but this is likely normal since package '");
                sb.append(pkgName);
                sb.append("' is optionally imported by bundle ");
                sb.append(impId);
                sb.append(".");
                if (exporters.length > 0)
                {
                    sb.append(" However, bundle ");
                    sb.append(expId);
                    if (reqs[i].isSatisfied(
                        Util.getExportPackage(exporters[0], reqs[i].getName())))
                    {
                        sb.append(" does export this package. Bundle ");
                        sb.append(expId);
                        sb.append(" must be installed before bundle ");
                        sb.append(impId);
                        sb.append(" is resolved or else the optional import will be ignored.");
                    }
                    else
                    {
                        sb.append(" does export this package with attributes that do not match.");
                    }
                }
                sb.append(" ***");

                return sb.toString();
            }
        }
*/
        // Next, check to see if the package is dynamically imported by the module.
/* TODO: RESOLVER: Need to fix this too.
        IRequirement[] dynamics = module.getDefinition().getDynamicRequirements();
        for (int dynIdx = 0; dynIdx < dynamics.length; dynIdx++)
        {
            IRequirement target = createDynamicRequirement(dynamics[dynIdx], pkgName);
            if (target != null)
            {
                // Try to see if there is an exporter available.
                PackageSource[] exporters = getResolvedCandidates(target);
                exporters = (exporters.length == 0)
                    ? getUnresolvedCandidates(target) : exporters;

                // An exporter might be available, but it may have attributes
                // that do not match the importer's required attributes, so
                // check that case by simply looking for an exporter of the
                // desired package without any attributes.
                if (exporters.length == 0)
                {
                    try
                    {
                        IRequirement pkgReq = new Requirement(
                            ICapability.PACKAGE_NAMESPACE, "(package=" + pkgName + ")");
                        exporters = getResolvedCandidates(pkgReq);
                        exporters = (exporters.length == 0)
                            ? getUnresolvedCandidates(pkgReq) : exporters;
                    }
                    catch (InvalidSyntaxException ex)
                    {
                        // This should never happen.
                    }
                }

                long expId = (exporters.length == 0)
                    ? -1 : Util.getBundleIdFromModuleId(exporters[0].m_module.getId());

                StringBuffer sb = new StringBuffer("*** Class '");
                sb.append(name);
                sb.append("' was not found, but this is likely normal since package '");
                sb.append(pkgName);
                sb.append("' is dynamically imported by bundle ");
                sb.append(impId);
                sb.append(".");
                if (exporters.length > 0)
                {
                    try
                    {
                        if (!target.isSatisfied(
                            Util.getSatisfyingCapability(exporters[0].m_module,
                                new Requirement(ICapability.PACKAGE_NAMESPACE, "(package=" + pkgName + ")"))))
                        {
                            sb.append(" However, bundle ");
                            sb.append(expId);
                            sb.append(" does export this package with attributes that do not match.");
                        }
                    }
                    catch (InvalidSyntaxException ex)
                    {
                        // This should never happen.
                    }
                }
                sb.append(" ***");

                return sb.toString();
            }
        }
*/
        IRequirement pkgReq = null;
        try
        {
            pkgReq = new Requirement(ICapability.PACKAGE_NAMESPACE, "(package=" + pkgName + ")");
        }
        catch (InvalidSyntaxException ex)
        {
            // This should never happen.
        }
        PackageSource[] exporters =
            resolver.getResolvedCandidates(pkgReq);
        exporters = (exporters.length == 0)
            ? resolver.getUnresolvedCandidates(pkgReq)
            : exporters;
        if (exporters.length > 0)
        {
            boolean classpath = false;
            try
            {
                ModuleClassLoader.class.getClassLoader().loadClass(name);
                classpath = true;
            }
            catch (NoClassDefFoundError err)
            {
                // Ignore
            }
            catch (Exception ex)
            {
                // Ignore
            }

            long expId = Util.getBundleIdFromModuleId(exporters[0].m_module.getId());

            StringBuffer sb = new StringBuffer("*** Class '");
            sb.append(name);
            sb.append("' was not found because bundle ");
            sb.append(impId);
            sb.append(" does not import '");
            sb.append(pkgName);
            sb.append("' even though bundle ");
            sb.append(expId);
            sb.append(" does export it.");
            if (classpath)
            {
                sb.append(" Additionally, the class is also available from the system class loader. There are two fixes: 1) Add an import for '");
                sb.append(pkgName);
                sb.append("' to bundle ");
                sb.append(impId);
                sb.append("; imports are necessary for each class directly touched by bundle code or indirectly touched, such as super classes if their methods are used. ");
                sb.append("2) Add package '");
                sb.append(pkgName);
                sb.append("' to the '");
                sb.append(Constants.FRAMEWORK_BOOTDELEGATION);
                sb.append("' property; a library or VM bug can cause classes to be loaded by the wrong class loader. The first approach is preferable for preserving modularity.");
            }
            else
            {
                sb.append(" To resolve this issue, add an import for '");
                sb.append(pkgName);
                sb.append("' to bundle ");
                sb.append(impId);
                sb.append(".");
            }
            sb.append(" ***");

            return sb.toString();
        }

        // Next, try to see if the class is available from the system
        // class loader.
        try
        {
            ModuleClassLoader.class.getClassLoader().loadClass(name);

            StringBuffer sb = new StringBuffer("*** Package '");
            sb.append(pkgName);
            sb.append("' is not imported by bundle ");
            sb.append(impId);
            sb.append(", nor is there any bundle that exports package '");
            sb.append(pkgName);
            sb.append("'. However, the class '");
            sb.append(name);
            sb.append("' is available from the system class loader. There are two fixes: 1) Add package '");
            sb.append(pkgName);
            sb.append("' to the '");
            sb.append(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
            sb.append("' property and modify bundle ");
            sb.append(impId);
            sb.append(" to import this package; this causes the system bundle to export class path packages. 2) Add package '");
            sb.append(pkgName);
            sb.append("' to the '");
            sb.append(Constants.FRAMEWORK_BOOTDELEGATION);
            sb.append("' property; a library or VM bug can cause classes to be loaded by the wrong class loader. The first approach is preferable for preserving modularity.");
            sb.append(" ***");

            return sb.toString();
        }
        catch (Exception ex2)
        {
        }

        // Finally, if there are no imports or exports for the package
        // and it is not available on the system class path, simply
        // log a message saying so.
        StringBuffer sb = new StringBuffer("*** Class '");
        sb.append(name);
        sb.append("' was not found. Bundle ");
        sb.append(impId);
        sb.append(" does not import package '");
        sb.append(pkgName);
        sb.append("', nor is the package exported by any other bundle or available from the system class loader.");
        sb.append(" ***");

        return sb.toString();
    }
}
