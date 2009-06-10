package com.googlecode.lemmon.service.http;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.osgi.service.http.HttpContext;

public class ServletContextImpl implements ServletContext
{
    private SystemServletImpl m_servletImpl;
    private ServletConfig m_servletConfig;
    private ServletContext m_servletContext;
	HttpContext _httpContext;
	
	HashMap _attributes= new HashMap();
	HashMap _initParameters= new HashMap();

	public ServletContextImpl(SystemServletImpl systemServlet, Dictionary initparams, HttpContext context)
	{
	    m_servletImpl= systemServlet;
		_httpContext= context;
		
        m_servletConfig= m_servletImpl.getServletConfig();
		m_servletContext= m_servletConfig.getServletContext();
		for (Enumeration e= m_servletContext.getAttributeNames(); e.hasMoreElements();)
		{
			String key= (String)e.nextElement();
			_attributes.put(key, m_servletContext.getAttribute(key));
		}
		
		
		for (Enumeration e= m_servletConfig.getInitParameterNames(); e.hasMoreElements();)
		{
			String key= (String)e.nextElement();
			_initParameters.put(key, m_servletConfig.getInitParameter(key));
		}
	
		if (initparams != null)
		{
	        for (Enumeration e= initparams.keys(); e.hasMoreElements();)
	        {
	            String key= (String)e.nextElement();
	            _initParameters.put(key, initparams.get(key));
	        }
		}
	}

	public Object getAttribute(String arg0)
	{
		return _attributes;
	}

	public Enumeration getAttributeNames()
	{
		return Collections.enumeration(_attributes.keySet());
	}

	public ServletContext getContext(String arg0)
	{
		return m_servletImpl.getContext(arg0);
	}

	public String getContextPath()
	{
		return m_servletContext.getContextPath();
	}

	public String getInitParameter(String arg0)
	{
		return (String)_initParameters.get(arg0);
	}

	public Enumeration getInitParameterNames()
	{
		return Collections.enumeration(_initParameters.keySet());
	}

	public int getMajorVersion()
	{
		return m_servletContext.getMajorVersion();
	}

	public String getMimeType(String arg0)
	{
		return m_servletContext.getMimeType(arg0);
	}

	public int getMinorVersion()
	{
		return m_servletContext.getMinorVersion();
	}

	public RequestDispatcher getNamedDispatcher(String arg0)
	{
		return m_servletContext.getNamedDispatcher(arg0);
	}

	public String getRealPath(String arg0)
	{
		return m_servletContext.getRealPath(arg0);
	}

	public RequestDispatcher getRequestDispatcher(String arg0)
	{
		return m_servletContext.getRequestDispatcher(arg0);
	}

	public URL getResource(String arg0) throws MalformedURLException
	{
		return m_servletContext.getResource(arg0);
	}

	public InputStream getResourceAsStream(String arg0)
	{
		return m_servletContext.getResourceAsStream(arg0);
	}

	public Set getResourcePaths(String arg0)
	{
		return m_servletContext.getResourcePaths(arg0);
	}

	public String getServerInfo()
	{
		return m_servletContext.getServerInfo();
	}

	public Servlet getServlet(String arg0) throws ServletException
	{
		return m_servletImpl.getServlet(arg0);
	}

	public String getServletContextName()
	{
		return m_servletContext.getServletContextName();
	}

	public Enumeration getServletNames()
	{
		return m_servletImpl.getServletNames();
	}

	public Enumeration getServlets()
	{
		return m_servletImpl.getServlets();
	}

	public void log(String arg0)
	{
	    m_servletContext.log(arg0);
	}

	public void log(Exception arg0, String arg1)
	{
	    m_servletContext.log(arg1, arg0);
	}

	public void log(String arg0, Throwable arg1)
	{
	    m_servletContext.log(arg0, arg1);
	}

	public void removeAttribute(String arg0)
	{
		_attributes.remove(arg0);
	}

	public void setAttribute(String arg0, Object arg1)
	{
		_attributes.put(arg0, arg1);
	}

}
