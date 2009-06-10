
package com.googlecode.lemmon;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.startlevel.StartLevel;

public class AutoActivator implements BundleActivator
{
    /**
     * The property name prefix for the launcher's auto-install property.
    **/
    public static final String AUTO_INSTALL_PROP = "felix.auto.install";
    /**
     * The property name prefix for the launcher's auto-start property.
    **/
    public static final String AUTO_START_PROP = "felix.auto.start";

    private Map m_configMap;
    private ServletContext m_servletContext;
    private BundleContext m_bundleContext;

    public AutoActivator(Map configMap, ServletContext servletContext)
    {
        m_configMap = configMap;
        m_servletContext= servletContext;
    }

    /**
     * Used to instigate auto-install and auto-start configuration
     * property processing via a custom framework activator during
     * framework startup.
     * @param context The system bundle context.
    **/
    public void start(BundleContext context) throws BundleException
    {
        m_bundleContext= context;
        installEmbeddedBundles();
        processAutoProperties();
    }

    private void installEmbeddedBundles() throws BundleException
    {
        String deployFolder= ( String ) m_configMap.get(LemmonConstants.DEPLOY_FOLDER);
        if (deployFolder == null)
            deployFolder= "WEB-INF/deploy";
        String realDeployFolder= deployFolder;
        if (!realDeployFolder.startsWith( "/" ))
            realDeployFolder= m_servletContext.getRealPath( deployFolder );
        if (realDeployFolder == null)
        {
            m_servletContext.log("WARNING: LEMMON: No bundles were automatically installed since the deploy folder was not found:"+deployFolder);
            return;
        }
        File deployFile= new File(realDeployFolder);
        File[] files= deployFile.listFiles();
        for (int i= 0; i < files.length; i++)
        {
            File file= files[i];
            String name= file.getName();
            if (name.endsWith( ".jar" ) || name.endsWith( ".zip" ))
            {
                URL url= null;
                try
                {
                    url = file.getAbsoluteFile().toURI().toURL();
                }
                catch ( MalformedURLException e )
                {
                    throw new RuntimeException(e); // not gonna happen
                }
                m_bundleContext.installBundle(url.toExternalForm(), null);
            }
        }
        
    }

    /**
     * Currently does nothing as part of framework shutdown.
     * @param context The system bundle context.
    **/
    public void stop(BundleContext context)
    {
        // Do nothing.
    }

    /**
     * <p>
     * Processes the auto-install and auto-start properties from the
     * specified configuration properties.
     * </p>
     * @throws BundleException 
     */
    private void processAutoProperties() 
    throws BundleException
    {
        // Retrieve the Start Level service, since it will be needed
        // to set the start level of the installed bundles.
        StartLevel sl = (StartLevel) m_bundleContext.getService(
            m_bundleContext.getServiceReference(org.osgi.service.startlevel.StartLevel.class.getName()));

        // Retrieve all auto-install and auto-start properties and install
        // their associated bundles. The auto-install property specifies a
        // space-delimited list of bundle URLs to be automatically installed
        // into each new profile, while the auto-start property specifies
        // bundles to be installed and started. The start level to which the
        // bundles are assigned is specified by appending a ".n" to the
        // property name, where "n" is the desired start level for the list
        // of bundles. If no start level is specified, the default start
        // level is assumed.
        for (Iterator i = m_configMap.keySet().iterator(); i.hasNext(); )
        {
            String key = ((String) i.next()).toLowerCase();

            // Ignore all keys that are not an auto property.
            if (!key.startsWith(AUTO_INSTALL_PROP) && !key.startsWith(AUTO_START_PROP))
            {
                continue;
            }

            // If the auto property does not have a start level,
            // then assume it is the default bundle start level, otherwise
            // parse the specified start level.
            int startLevel = sl.getInitialBundleStartLevel();
            if (!key.equals(AUTO_INSTALL_PROP) && !key.equals(AUTO_START_PROP))
            {
                try
                {
                    startLevel = Integer.parseInt(key.substring(key.lastIndexOf('.') + 1));
                }
                catch (NumberFormatException ex)
                {
                    System.err.println("Invalid property: " + key);
                }
            }

            // Parse and install the bundles associated with the key.
            StringTokenizer st = new StringTokenizer((String) m_configMap.get(key), "\" ", true);
            for (String location = nextLocation(st); location != null; location = nextLocation(st))
            {
                try
                {
                    Bundle b = m_bundleContext.installBundle(location, null);
                    sl.setBundleStartLevel(b, startLevel);
                }
                catch (Exception ex)
                {
                    System.err.println("Auto-properties install: " + ex);
                }
            }
        }

        // auto start bundles listed in config.properties
        HashMap installedBundles= new HashMap();
        Bundle[] bundles= m_bundleContext.getBundles(); 
        for (int i= 0; i < bundles.length; i++) 
            installedBundles.put( bundles[i].getSymbolicName(), bundles[i] );
        for (Iterator i = m_configMap.keySet().iterator(); i.hasNext(); )
        {
            String key = ((String) i.next()).toLowerCase();
            if (key.startsWith(LemmonConstants.AUTO_START_WHEN_INSTALLED))
            {
                StringTokenizer st = new StringTokenizer((String) m_configMap.get(key), " ", true);
                while (st.hasMoreTokens())
                {
                    String bundleInfo= st.nextToken();
                    String bundleId= bundleInfo;
                    String version= null;
                    int c= bundleId.indexOf( ";" );
                    if (0 < c)
                    {
                        bundleId= bundleId.substring( 0, c );
                        int v= bundleInfo.indexOf( "version=\"", c );
                        if (0 <= v)
                        {
                            int e= bundleInfo.indexOf( "\"", v+9 ); 
                            version= bundleInfo.substring( v+9, e );
                        }                        
                    }
                    
                    Bundle bundle= ( Bundle ) installedBundles.get( bundleId );
                    if (bundle != null)
                    {
                        String bVersion= ( String ) bundle.getHeaders().get(  "Bundle-Version" );
                        if (version == null || version.equals( bVersion ))
                            bundle.start();
                    }
                }
            }
        }
        
        // auto start bundles installed in future
        m_bundleContext.addBundleListener( new SynchronousBundleListener() { 
            public void bundleChanged( BundleEvent arg0 )
            {
                if (BundleEvent.INSTALLED == arg0.getType())
                {
                    Bundle bundle= arg0.getBundle();
                    try 
                    {
                        autoStartBundle( bundle );
                    }
                    catch (Throwable t)
                    {
                        m_servletContext.log( "Error starting bundle "+bundle.getSymbolicName()+" : "+t.getMessage() );
                        t.printStackTrace();
                    }
                }
            }
        } );
    }

    private void autoStartBundle( Bundle bundle ) throws BundleException
    {
        for (Iterator i = m_configMap.keySet().iterator(); i.hasNext(); )
        {
            String key = ((String) i.next()).toLowerCase();
            
            if (key.startsWith(AUTO_START_PROP))
            {
                StringTokenizer st = new StringTokenizer((String) m_configMap.get(key), "\" ", true);
                for (String location = nextLocation(st); location != null; location = nextLocation(st))
                {
                    // Installing twice just returns the same bundle.
                    try
                    {
                        Bundle b = m_bundleContext.installBundle(location, null);
                        if (b != null)
                        {
                            b.start();
                        }
                    }
                    catch (Exception ex)
                    {
                        System.err.println("Auto-properties start: " + ex);
                    }
                }
            }
            
            if (key.startsWith(LemmonConstants.AUTO_START_WHEN_INSTALLED))
            {
                StringTokenizer st = new StringTokenizer((String) m_configMap.get(key), " ", true);
                while (st.hasMoreTokens())
                {
                    String bundleInfo= st.nextToken();
                    String bundleId= bundleInfo;
                    String version= null;
                    int c= bundleId.indexOf( ";" );
                    if (0 < c)
                    {
                        bundleId= bundleId.substring( 0, c );
                        int v= bundleInfo.indexOf( "version=\"", c );
                        if (0 <= v)
                        {
                            int e= bundleInfo.indexOf( "\"", v+9 ); 
                            version= bundleInfo.substring( v+9, e );
                        }                        
                    }
                    
                    if (bundle.getSymbolicName().equals( bundleId ))
                    {
                        String bVersion= ( String ) bundle.getHeaders().get(  "Bundle-Version" );
                        if (version == null || version.equals( bVersion ))
                        {
                            bundle.start();
                        }
                    }
                }
            }
        }
    }

    private static String nextLocation(StringTokenizer st)
    {
        String retVal = null;

        if (st.countTokens() > 0)
        {
            String tokenList = "\" ";
            StringBuffer tokBuf = new StringBuffer(10);
            String tok = null;
            boolean inQuote = false;
            boolean tokStarted = false;
            boolean exit = false;
            while ((st.hasMoreTokens()) && (!exit))
            {
                tok = st.nextToken(tokenList);
                if (tok.equals("\""))
                {
                    inQuote = ! inQuote;
                    if (inQuote)
                    {
                        tokenList = "\"";
                    }
                    else
                    {
                        tokenList = "\" ";
                    }

                }
                else if (tok.equals(" "))
                {
                    if (tokStarted)
                    {
                        retVal = tokBuf.toString();
                        tokStarted=false;
                        tokBuf = new StringBuffer(10);
                        exit = true;
                    }
                }
                else
                {
                    tokStarted = true;
                    tokBuf.append(tok.trim());
                }
            }

            // Handle case where end of token stream and
            // still got data
            if ((!exit) && (tokStarted))
            {
                retVal = tokBuf.toString();
            }
        }

        return retVal;
    }
}