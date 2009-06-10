package com.googlecode.lemmon.service.http;


import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;


public class HttpContextImpl implements HttpContext
{
    private Bundle _bundle;


    public HttpContextImpl( Bundle bundle )
    {
        _bundle = bundle;
    }


    public String getMimeType( String name )
    {
        return null;
    }

    public URL getResource( String name )
    {
        if ( name.startsWith( "/" ) )
            name = name.substring( 1 );

        return _bundle.getResource( name );
    }

    public boolean handleSecurity( HttpServletRequest request, HttpServletResponse response )
    {
        return true;  
    }
}