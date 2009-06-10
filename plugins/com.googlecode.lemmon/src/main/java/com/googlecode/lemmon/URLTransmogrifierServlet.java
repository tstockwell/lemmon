package com.googlecode.lemmon;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.framework.Lemmon;

/**
 * This servlet, and associated utility methods, provide a way to create 
 * working URLs in the Google App Engine environment that have special handling 
 * requirements.
 * 
 * The rational is this:
 * Many types of applications use special protocols for accessing resources.
 * For instance, OSGi frameworks use the bundle: protocol for accessing resources 
 * inside bundles.
 * However, the Google App Engine disables Java's support for special URLs.
 * In the GAE environment only the standard URL protocols may be used.
 * 
 * So, this class provides a way to take an URL like bundle:1/some.file.txt and
 * 'transmogrify' the URL into a valid http URL, something like 
 * http://localhost/transmogrify/bundle/1/some.file.txt.
 * Special URL handlers may be plugged into this servlet and this servlet will 
 * map http requests to the appropriate URL handler.
 * 
 * This solves two problems:
 * 1) In GAE you cannot create an URL instance for bundle:1/some.file.txt since  
 * you will get a MalformedURLException because Java does not understand 
 * the bundle: protocol.
 * 
 * 2) the resulting URL is a valid URL that can be opened by third-party Java 
 * components.
 * 
 * @author Ted Stockwell <emorning@yahoo.com>
 */
public class URLTransmogrifierServlet
extends GenericServlet
{
	private static final long serialVersionUID = 1L;
	private Lemmon _framework= null;
	
	
	public void init(ServletConfig config) 
	throws ServletException
	{
		super.init(config);
		if (_framework == null)
		{
			try
			{
				_framework= new Lemmon(config);
				_framework.start();
			} 
			catch (Throwable e)
			{
				config.getServletContext().log("Error starting Lemmon", e);
				throw new ServletException(e);
			}
		}
	}

	public void service(ServletRequest request, ServletResponse response) 
	throws ServletException, IOException
	{
	    SystemServletActivator.service((HttpServletRequest)request, (HttpServletResponse)response);
	}

}
