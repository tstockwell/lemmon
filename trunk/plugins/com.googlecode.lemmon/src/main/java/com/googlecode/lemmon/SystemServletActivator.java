
package com.googlecode.lemmon;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

import com.googlecode.lemmon.service.http.system.SystemServletService;

public class SystemServletActivator 
implements BundleActivator
{
    private ServletConfig m_servletConfig;
    private static Servlet m_servletDelegate;
    private SystemServletServiceImpl m_servletServiceImpl= new SystemServletServiceImpl();
    private ServiceRegistration _registration;
    
    class SystemServletServiceImpl 
    implements SystemServletService
    {
        public void registerSystemServletDelegate( Servlet systemDelegate ) 
        throws ServletException
        {
            if (m_servletDelegate != null)
                throw new RuntimeException("A system servlet is already registered");
            m_servletDelegate= systemDelegate;
            m_servletDelegate.init( m_servletConfig );
            
        }
        
        public void unregisterSystemServletDelegate( Servlet systemDelegate )
        {
            if (systemDelegate == m_servletDelegate)
                m_servletDelegate= null;
        }
    }
    
    

    public SystemServletActivator(Map configMap, ServletConfig servletContext)
    {
        m_servletConfig= servletContext;
    }

    /**
     * Used to instigate auto-install and auto-start configuration
     * property processing via a custom framework activator during
     * framework startup.
     * @param context The system bundle context.
    **/
    public void start(BundleContext ctx) throws BundleException
    {
        Hashtable hashtable = new Hashtable();
        hashtable.put("Description", "The Lemmon System Servlet Hook");
        _registration= ctx.registerService(SystemServletService.class.getName(), m_servletServiceImpl, hashtable);
    }

    /**
     * Currently does nothing as part of framework shutdown.
     * @param context The system bundle context.
    **/
    public void stop(BundleContext context)
    {
        try { _registration.unregister(); } catch (Throwable t) { }
    }

    public static void service( HttpServletRequest request, HttpServletResponse response ) 
    throws ServletException, IOException
    {
        if (m_servletDelegate != null)
            m_servletDelegate.service( request, response );
        
    }
}