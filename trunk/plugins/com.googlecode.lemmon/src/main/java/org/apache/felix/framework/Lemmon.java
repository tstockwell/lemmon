package org.apache.felix.framework;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.FelixConstants;

import com.googlecode.lemmon.AutoActivator;
import com.googlecode.lemmon.LemmonLogger;
import com.googlecode.lemmon.SystemServletActivator;

public class Lemmon
extends Felix
{
	
	ServletConfig _servletConfig;
	private boolean m_securityEnabled= true;
	
	public Lemmon(ServletConfig config) 
	throws ServletException, IOException
	{
		super(createConfigurationMap(config));
		
		Object startService= getConfig().get( FelixConstants.SYSTEMBUNDLE_START_SERVICE );
		if (startService instanceof LemmonStartLevelService)
		    ((LemmonStartLevelService)startService).setFramework(this);
        
		_servletConfig= config;
	}
	
	public boolean isSecurityEnabled() 
	{
	    return m_securityEnabled;
	}
	
    private static Map createConfigurationMap(ServletConfig config) 
	throws ServletException, IOException
	{
        HashMap configMap= new HashMap();
        
	    // load config properties from conf folder
        ServletContext servletContext= config.getServletContext();
	    String confPath= servletContext.getRealPath( "WEB-INF/conf" );
	    File confFile= new File(new File(confPath), "config.properties");
	    if (!confFile.exists())
	        throw new ServletException("Felix configuration file not found.  Configuration file is expected at WEB-INF/conf/config.properties");
	    Properties  properties= new Properties();
        InputStream in= new BufferedInputStream(new FileInputStream(confFile));	    
        properties.load( in );
        try { in.close(); } catch (Throwable t) { }
        configMap.putAll( properties );
	    
		// add servlet config properties
		for (Enumeration e= config.getInitParameterNames(); e.hasMoreElements();)
		{
			String name= (String)e.nextElement();			
			configMap.put(name, config.getInitParameter(name));
		}
		
        // add host activator;
        List list = new ArrayList();
        list.add( new SystemServletActivator(configMap, config) );
        list.add( new AutoActivator(configMap, servletContext) );
        configMap.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, list);
        
        // tell Felix to use this logger
        Logger logger= new LemmonLogger();
        configMap.put( FelixConstants.LOG_LOGGER_PROP, logger );
        
        // tell Felix to use GAE bundle cache
        try 
        {
            BundleCache bundleCache= new BundleCache(logger, configMap);
            configMap.put( FelixConstants.SYSTEMBUNDLE_BUNDLE_CACHE, bundleCache );
        }
        catch (Exception e)
        {
            throw new ServletException(e);
        }
        
        // tell Felix to use custom start service       
        LemmonStartLevelService startLevelService= new LemmonStartLevelService();
        configMap.put( FelixConstants.SYSTEMBUNDLE_START_SERVICE, startLevelService );
        
		return configMap;
	}
}
