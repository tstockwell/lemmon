package com.googlecode.lemmon;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Logging {

	static abstract public class Task 
	{
		abstract public void log(Logger logger);
	}
	public static Logger getLogger()
	{
		return getLogger(1);
	}
	private static Logger getLogger(int depth)
	{
		Throwable throwable= new Throwable();
		StackTraceElement[] stackTraceElements= throwable.getStackTrace();
		Logger logger= null;
		if (stackTraceElements != null && 1 <= stackTraceElements.length)
		{
			String packageName= stackTraceElements[depth].getClassName();
			int i= packageName.lastIndexOf('.');
			if (0 < i)
				packageName= packageName.substring(0, i);
			logger= Logger.getLogger(packageName);
		}
		if (logger == null)
			logger= Logger.getLogger(Logging.class.getPackage().getName());
		return logger;
	}
	
    static public void log(Level level, String msg) {
    	Logger logger= getLogger(2);
    	logger.log(level, msg);
    }
	
    static public void log(Level level, String msg, Throwable t) {
    	Logger logger= getLogger(2);
    	logger.log(level, msg, t);
    }
	
    static public void severe(String msg) {
    	Logger logger= getLogger(2);
    	logger.log(Level.SEVERE, msg);
    }
	
    static public void severe(String msg, Throwable t) {
    	Logger logger= getLogger(2);
    	logger.log(Level.SEVERE, msg, t);
    }
	
    static public void warning(String msg) {
    	Logger logger= getLogger(2);
    	logger.log(Level.WARNING, msg);
    }
	
    static public void warning(String msg, Throwable t) {
    	Logger logger= getLogger(2);
    	logger.log(Level.WARNING, msg, t);
    }
	public static void info(String msg) {
    	Logger logger= getLogger(2);
    	logger.log(Level.INFO, msg);
	}
    public static void info(String msg, Throwable t) {
        Logger logger= getLogger(2);
        logger.log(Level.INFO, msg, t);
    }
    public static void debug(String msg) {
        Logger logger= getLogger(2);
        logger.log(Level.FINE, msg);
    }
    public static void debug(String msg, Throwable t) {
        Logger logger= getLogger(2);
        logger.log(Level.FINE, msg, t);
    }
	public static void fine(String msg)
	{
    	Logger logger= getLogger(2);
    	logger.log(Level.FINE, msg);
	}
    public static void fine(String msg, Throwable t)
    {
        Logger logger= getLogger(2);
        logger.log(Level.FINE, msg, t);
    }
	/**
	 * For performing an intensive logging task.
	 * The task is only invoked if the specified logging level is enabled. 
	 */
	public static void fine(Task task)
	{
    	Logger logger= getLogger(2);
    	if (logger.isLoggable(Level.FINE)) {
    		task.log(logger);
    	}
	}

}
