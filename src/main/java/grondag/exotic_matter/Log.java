package grondag.exotic_matter;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Log
{
    @Nullable
    private static Logger log;
    
    @Nullable
    private static Logger getLog()
    {
        // allow access to log during unit testing or other debug scenarios
        if(log == null)
        {
            log = LogManager.getLogger();
        }
        return log;
    }
    
    public static void setLog(Logger lOG)
    {
        log = lOG;
    }

    public static void warn(String message)
    {
        Logger log = getLog();
        if(log != null) log.warn(message);
    }
    
    public static void warn(String message, Object...args)
    {
    		Logger log = getLog();
        if(log != null) log.warn(String.format(message, args));
    }

    public static void info(String message)
    {
		Logger log = getLog();
        if(log != null) log.info(message);
    }

    public static void info(String message, Object...args)
    {
		Logger log = getLog();
        if(log != null) log.info(String.format(message, args));
    }
    
    public static void debug(String message)
    {
		Logger log = getLog();
        if(log != null) log.debug(message);
    }
    
    public static void debug(String message, Object...args)
    {
		Logger log = getLog();
        if(log != null) log.debug(String.format(message, args));
    }

    public static void error(String message)
    {
		Logger log = getLog();
        if(log != null) log.error(message);
    }

    public static void error(String message, Throwable t)
    {
		Logger log = getLog();
        if(log != null) log.error(message, t);
    }
    
    public static void error(String message, Object o1, Object o2)
    {
		Logger log = getLog();
        if(log != null) log.error(message, o1, o2);
    }
}
