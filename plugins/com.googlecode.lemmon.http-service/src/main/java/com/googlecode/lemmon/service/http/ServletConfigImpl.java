package com.googlecode.lemmon.service.http;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.osgi.service.http.HttpContext;

public class ServletConfigImpl implements ServletConfig
{

	String _alias;
	HttpContext _context;
    private SystemServletImpl m_systemServletService;

	ServletContext _servletContext;
	HashMap _initParameters= new HashMap();
	
	public ServletConfigImpl(SystemServletImpl systemServletService, String name, Dictionary initparams, HttpContext context)	
	{
	    m_systemServletService= systemServletService;
		_alias= name;
		_context= context;
		_servletContext= new ServletContextImpl(m_systemServletService, initparams, context);
		
		final ServletConfig config= m_systemServletService.getServletConfig();
		for (Enumeration e= config.getInitParameterNames(); e.hasMoreElements();)
		{
			String key= (String)e.nextElement();
			_initParameters.put(key, config.getInitParameter(key));
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

    public String getInitParameter(String arg0)
	{		
		return (String)_initParameters.get(arg0);
	}

	public Enumeration getInitParameterNames()
	{
		return Collections.enumeration(_initParameters.keySet());
	}

	public ServletContext getServletContext()
	{
		return _servletContext;
	}

	public String getServletName()
	{
		return _alias;
	}


}
