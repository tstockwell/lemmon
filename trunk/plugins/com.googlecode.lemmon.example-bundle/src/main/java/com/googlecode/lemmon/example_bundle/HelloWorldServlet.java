package com.googlecode.lemmon.example_bundle;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Hello world!
 *
 */
public class HelloWorldServlet 
extends GenericServlet
{
    private static final long serialVersionUID = 1L;
    
    private String _servletName= "unknown";

    public HelloWorldServlet(String name)
	{
		_servletName= name;
	}
    public HelloWorldServlet()
	{
	}

	public void service( ServletRequest request, ServletResponse response) 
    throws ServletException, IOException
    {
        Writer writer= response.getWriter();
        writer.write( "<html><body><h1>Hello World from "+_servletName+"</h1></body></html>" );
        writer.flush();
    }
}
