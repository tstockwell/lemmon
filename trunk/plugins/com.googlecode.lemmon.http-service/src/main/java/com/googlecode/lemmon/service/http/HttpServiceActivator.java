package com.googlecode.lemmon.service.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.ServletException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.googlecode.lemmon.service.http.system.SystemServletService;


public class HttpServiceActivator 
implements BundleActivator 
{
	
    private Map _servicesByBundle= Collections.synchronizedMap(new HashMap());
    private ServiceRegistration _registration;
    private ServiceReference m_systemServletReference;
    private SystemServletService m_systemServletService;
    private SystemServletImpl m_systemServletImpl= new SystemServletImpl();
    
    public void start(BundleContext bundlecontext)
    throws BundleException
    {
        m_systemServletReference= bundlecontext.getServiceReference(SystemServletService.class.getName());
        if (m_systemServletReference == null)
            throw new BundleException("Cannot start HTTP service, no system servlet service is available");
        m_systemServletService= ( SystemServletService ) bundlecontext.getService( m_systemServletReference );
        
        try 
        {
            m_systemServletService.registerSystemServletDelegate( m_systemServletImpl );
        }
        catch (Throwable x)
        {
            throw new BundleException("Failed to start HTTP service", x);
        }

        /*
        register a service factory
        */
        ServiceFactory servicefactory = new ServiceFactory() {
            public Object getService(Bundle bundle, ServiceRegistration serviceregistration) 
            {
                HttpServiceImpl httpserviceimpl = (HttpServiceImpl) _servicesByBundle.get(bundle);
                if (httpserviceimpl == null)
                {
                	httpserviceimpl= new HttpServiceImpl(m_systemServletImpl, bundle);
                	_servicesByBundle.put(bundle, httpserviceimpl);
                }
                return httpserviceimpl;
            }

            public void ungetService(Bundle bundle, ServiceRegistration serviceregistration, Object obj) {
                HttpServiceImpl httpserviceimpl = (HttpServiceImpl)_servicesByBundle.remove(bundle);
                if(httpserviceimpl != null)
                    httpserviceimpl.unregisterAll();
            }
        };
        Hashtable hashtable = new Hashtable();
        hashtable.put("Description", "The OSGI HTTP Service");
        _registration= bundlecontext.registerService("org.osgi.service.http.HttpService", servicefactory, hashtable);
        bundlecontext.addBundleListener(
            new BundleListener() {
                public void bundleChanged(BundleEvent bundleevent) {
                    if(bundleevent.getType() == Bundle.STOPPING) {
                        HttpServiceImpl httpserviceimpl = (HttpServiceImpl)_servicesByBundle.remove(bundleevent.getBundle());
                        if(httpserviceimpl != null)
                            httpserviceimpl.unregisterAll();
                    }
                }
            }
        );
    }

    public void stop(BundleContext bundlecontext)
    throws BundleException
    {
        try { m_systemServletService.unregisterSystemServletDelegate( m_systemServletImpl ); } catch (Throwable t) { }
        
    	try { _registration.unregister(); } catch (Throwable t) { }
    	
        try { bundlecontext.ungetService(m_systemServletReference); } catch (Throwable t) { }
    }
};
