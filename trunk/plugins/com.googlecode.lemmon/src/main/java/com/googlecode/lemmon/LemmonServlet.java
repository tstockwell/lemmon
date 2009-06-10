package com.googlecode.lemmon;

import java.io.IOException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.framework.Lemmon;

/**
 * This servlet embeds the Felix OSGi framework.
 * This servlet also implements the OSGi HTTP service so that  
 * all servlets that are deployed from OSGi bundles are made available
 * to the outside world via this servlet.
 * 
 * Initialization parameters defined in web.xml will be passed to the 
 * underlying Felix instance.
 *  
 * @author Ted Stockwell <emorning@yahoo.com>
 */
public class LemmonServlet
extends GenericServlet
{
	private static final long serialVersionUID = 1L;
	private Lemmon _framework= null;
	
	
	public void init(ServletConfig config) 
	throws ServletException
	{
        Logging.debug("Starting Lemmon servlet");
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
			    Logging.severe("Error starting Lemmon", e);
				throw new ServletException(e);
			}
		}
        Logging.debug("Successfully started Lemmon servlet");
	}

	public void service(ServletRequest request, ServletResponse response) 
	throws ServletException, IOException
	{
	    SystemServletActivator.service((HttpServletRequest)request, (HttpServletResponse)response);
	}

}
