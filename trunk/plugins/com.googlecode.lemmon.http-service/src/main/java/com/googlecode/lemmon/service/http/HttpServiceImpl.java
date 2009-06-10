package com.googlecode.lemmon.service.http;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

public class HttpServiceImpl
implements HttpService
{
	private Bundle _bundle;
	private ArrayList _registeredAliases= new ArrayList();
	private SystemServletImpl m_systemServletService;

	public HttpServiceImpl(SystemServletImpl systemServletService, Bundle bundle)
	{
		m_systemServletService= systemServletService;
		_bundle= bundle;
	}

	public HttpContext createDefaultHttpContext()
	{
		return new HttpContextImpl(_bundle);
	}

	/**
	 * Registers resources into the URI namespace.
	 * The alias is the name in the URI namespace of the Http Service at which the registration will be mapped. 
	 * An alias must begin with slash ('/') and must not end with slash ('/'), with the exception that an alias of the form "/" is used to denote the root alias. 
	 * The name parameter must also not end with slash ('/'). 
	 * See the specification text for details on how HTTP requests are mapped to servlet and resource registrations.
	 * 
	 * For example, suppose the resource name /tmp is registered to the alias /files. A request for /files/foo.txt will map to the resource name /tmp/foo.txt.
	 * 
	 * httpservice.registerResources("/files", "/tmp", context);
	 * The Http Service will call the HttpContext argument to map resource names to URLs and MIME types and to handle security for requests. If the HttpContext argument is null, a default HttpContext is used (see createDefaultHttpContext()).
	 * 
	 */
	public void registerResources(String alias, String name, HttpContext context) 
	throws NamespaceException
	{
	    m_systemServletService.registerResources(_bundle, alias, name, context);
		_registeredAliases.add(alias);
	}

	/**
	 * Registers a servlet into the URI namespace.
	 * The alias is the name in the URI namespace of the Http Service at which the registration will be mapped.
	 * 
	 * An alias must begin with slash ('/') and must not end with slash ('/'), with the exception that an alias of the form "/" is used to denote the root alias. See the specification text for details on how HTTP requests are mapped to servlet and resource registrations.
	 *  
	 * The Http Service will call the servlet's init method before returning.
	 * 
	 * httpService.registerServlet("/myservlet", servlet, initparams, context);
	 * Servlets registered with the same HttpContext object will share the same ServletContext. The Http Service will call the context argument to support the ServletContext methods getResource,getResourceAsStream and getMimeType, and to handle security for requests. If the context argument is null, a default HttpContext object is used (see createDefaultHttpContext()).
	 * 
	 * @param alias name in the URI namespace at which the servlet is registered
	 * @param servlet the servlet object to register
	 * @param initparams initialization arguments for the servlet or null if there are none. This argument is used by the servlet's ServletConfig object.
	 * @param context the HttpContext object for the registered servlet, or null if a default HttpContext is to be created and used.
	 * 
	 * @throws NamespaceException if the registration fails because the alias is already in use.
	 * @throws javax.servlet.ServletException  if the servlet's init method throws an exception, or the given servlet object has already been registered at a different alias.
	 * @throws java.lang.IllegalArgumentException if any of the arguments are invalid
	 */
	public void registerServlet(String alias, Servlet servlet, Dictionary initparams, HttpContext context) 
	throws ServletException, NamespaceException
	{
	    m_systemServletService.registerServlet(_bundle, alias, servlet, initparams, context);
        ServletConfig servletConfig= new ServletConfigImpl(m_systemServletService, alias, initparams, context); 
        servlet.init(servletConfig);
		_registeredAliases.add(alias);
	}

	public void unregister(String alias)
	{
		if (_registeredAliases.contains(alias))
		{
			_registeredAliases.remove(alias);
			m_systemServletService.unregister(alias);
		}
	}

	public void unregisterAll()
	{
		for (Iterator i= _registeredAliases.iterator(); i.hasNext();)
			unregister((String)i.next());
	}

}
