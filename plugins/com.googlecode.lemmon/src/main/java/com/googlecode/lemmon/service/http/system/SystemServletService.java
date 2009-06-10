package com.googlecode.lemmon.service.http.system;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

/**
 * A service provided by OSGi framework implementations that are embedded inside 
 * a Servlet container.
 * This service provides a way for another bundle that implements the OSGi HTTP 
 * service to get hooked up with the containing servlet container. 
 * 
 * @author Ted Stockwell
 */
public interface SystemServletService
{
    /**
     * When this method is invoked the OSGi framework will call the 
     * Servlet.init method on the given servlet and pass the system 
     * ServletConfig object. 
     */
    void registerSystemServletDelegate(Servlet systemDelegate) throws ServletException;
    void unregisterSystemServletDelegate(Servlet systemDelegate);
}
