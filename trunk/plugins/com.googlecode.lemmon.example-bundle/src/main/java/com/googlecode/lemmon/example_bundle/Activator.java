package com.googlecode.lemmon.example_bundle;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

public class Activator
implements BundleActivator
{
    ServiceReference m_reference;

    public void start( final BundleContext ctx ) throws Exception
    {
        new ServiceTracker(ctx, HttpService.class.getName(), null)
        {
            public Object addingService( ServiceReference reference )
            {
                HttpService httpService= (HttpService)super.addingService( reference );
                registerServlets( httpService );
                return httpService;
            }
        }.open();
    }
    
    void registerServlets(HttpService httpService) 
    {
        try
        {
            httpService.registerServlet("/", new HelloWorldServlet("1of2"), null, null);
            httpService.registerServlet("/2of2", new HelloWorldServlet("2of2"), null, null);
        }
        catch ( Throwable e )
        {
            e.printStackTrace();
        }
    }

    public void stop( BundleContext ctx ) throws Exception
    {
        if (m_reference != null)
            ctx.ungetService(m_reference);
    }
    
    

}
