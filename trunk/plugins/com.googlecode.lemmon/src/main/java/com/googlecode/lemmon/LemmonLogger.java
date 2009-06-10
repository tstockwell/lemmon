package com.googlecode.lemmon;

import org.apache.felix.framework.Logger;
import org.osgi.framework.ServiceReference;

public class LemmonLogger
extends Logger
{

    protected void doLog( ServiceReference sr, int level, String msg, Throwable throwable )
    {
        String s = (sr == null) ? null : "SvcRef " + sr;
        s = (s == null) ? msg : s + " " + msg;
        s = (throwable == null) ? s : s + " (" + throwable + ")";
        switch (level)
        {
            case LOG_DEBUG:
                Logging.fine( s, throwable );
                break;
            case LOG_ERROR:
                Logging.severe( s, throwable );
                break;
            case LOG_INFO:
                Logging.info( s, throwable );
                break;
            case LOG_WARNING:
                Logging.warning( s, throwable );
                break;
            default:
                Logging.info( s, throwable );
        }
    }

}
