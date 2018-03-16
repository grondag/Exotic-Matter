package grondag.exotic_matter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.annotation.Nullable;

public class Log
{
    @Nullable
    private static Logger log;
    
    private static void getLog()
    {
        // allow access to log during unit testing or other debug scenarios
        if(log == null)
        {
            log = LogManager.getLogger();
        }
    }
    
    public static void setLog(Logger lOG)
    {
        log = lOG;
    }

    public static void warn(String message)
    {
        getLog();
        if(log != null) log.warn(message);
    }
    
    public static void warn(String message, Object...args)
    {
        getLog();
        if(log != null) log.warn(String.format(message, args));
    }

    public static void info(String message)
    {
        getLog();
        if(log != null) log.info(message);
    }

    public static void info(String message, Object...args)
    {
        getLog();
        if(log != null) log.info(String.format(message, args));
    }
    
    public static void debug(String message)
    {
        getLog();
        if(log != null) log.debug(message);
    }
    
    public static void debug(String message, Object...args)
    {
        getLog();
        if(log != null) log.debug(String.format(message, args));
    }

    public static void error(String message)
    {
        getLog();
        if(log != null) log.error(message);
    }

    public static void error(String message, Throwable t)
    {
        getLog();
        if(log != null) log.error(message, t);
    }
    
    public static void error(String message, Object o1, Object o2)
    {
        getLog();
        if(log != null) log.error(message, o1, o2);
    }
}
