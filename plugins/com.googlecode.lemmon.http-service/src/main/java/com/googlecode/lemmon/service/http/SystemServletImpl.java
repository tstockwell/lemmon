package com.googlecode.lemmon.service.http;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

public class SystemServletImpl
extends GenericServlet
{
    private static final long serialVersionUID = 1L;
    
    static class ResourceContext
    {
        public ResourceContext(String name2, HttpContext context)
        {
            name= name2;
            httpContext= context;
        }
        final String name;
        final HttpContext httpContext;
    }
    
    
    
    private HashMap _servletsByAlias= new HashMap();
    private HashMap _resourcesByAlias= new HashMap();
    private ServletConfig m_systemConfig; 
    
    
    public void init( ServletConfig config ) throws ServletException
    {
        super.init( config );
        m_systemConfig= config;
    }

    public void service( ServletRequest request, ServletResponse response ) 
    throws ServletException, IOException
    {
        String alias= ((HttpServletRequest)request).getPathInfo();
        if (alias.length() <= 0)
            alias= "/";
        Servlet servlet= null;
        String servletAlias= null;
        for (Iterator i= _servletsByAlias.keySet().iterator(); i.hasNext();)
        {
            String registeredAlias= (String)i.next();
            if (alias.startsWith(registeredAlias))
            {
                if (servletAlias == null || servletAlias.length() < registeredAlias.length())
                {
                    servletAlias= registeredAlias;
                    servlet= (Servlet)_servletsByAlias.get(registeredAlias);
                }
            }
        }
        
        if (servlet == null)
            throw new ServletException("No servlet registered for path:"+alias);
        
        servlet.service(request, response);
    }


    public void unregister(String alias)
    {
        _servletsByAlias.remove(alias);
        _resourcesByAlias.remove(alias);
    }

    public void registerServlet(Bundle _bundle, final String alias, Servlet servlet, final Dictionary initparams, HttpContext context) 
    throws ServletException
    {
        ServletConfig servletConfig= new ServletConfigImpl(this, alias, initparams, context); 
        servlet.init(servletConfig);
        _servletsByAlias.put(alias, servlet);
    }

    public void registerResources(Bundle _bundle, String alias, String name, HttpContext context)
    {
        _resourcesByAlias.put(alias, new ResourceContext(name, context));
    }

    public ServletContext getContext(String path)
    {
        String servletAlias= null;
        for (Iterator i= _servletsByAlias.keySet().iterator(); i.hasNext();)
        {
            String registeredAlias= (String)i.next();
            if (path.startsWith(registeredAlias))
                if (servletAlias == null || servletAlias.length() < registeredAlias.length())
                    servletAlias= registeredAlias;
        }
        if (servletAlias == null)
            return m_systemConfig.getServletContext();
        
        Servlet servlet= (Servlet)_servletsByAlias.get(servletAlias);
        ServletContext context= servlet.getServletConfig().getServletContext();
        return context;
    }

    public Enumeration getServlets()
    {
        return Collections.enumeration(_servletsByAlias.values());
    }

    public Enumeration getServletNames()
    {
        return Collections.enumeration(_servletsByAlias.keySet());
    }

    public Servlet getServlet(String path)
    {
        String servletAlias= null;
        for (Iterator i= _servletsByAlias.keySet().iterator(); i.hasNext();)
        {
            String registeredAlias= (String)i.next();
            if (path.startsWith(registeredAlias))
                if (servletAlias == null || servletAlias.length() < registeredAlias.length())
                    servletAlias= registeredAlias;
        }
        if (servletAlias == null)
            return null;
        
        return (Servlet)_servletsByAlias.get(servletAlias);
    }

}
