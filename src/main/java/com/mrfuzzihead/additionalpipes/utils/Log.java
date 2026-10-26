package com.mrfuzzihead.additionalpipes.utils;

import org.apache.logging.log4j.Level;

import com.mrfuzzihead.additionalpipes.APConfiguration;
import com.mrfuzzihead.additionalpipes.AdditionalPipes;

import cpw.mods.fml.common.FMLLog;

public class Log {

    public static void log(Level logLevel, Object object) {
        FMLLog.log(AdditionalPipes.NAME, logLevel, String.valueOf(object));
    }

    public static void all(Object object) {
        log(Level.ALL, object);
    }

    public static void debug(Object object) {
        if (APConfiguration.enableDebugLog) {
            log(Level.INFO, object);
        }
    }

    /**
     * Formatting is deferred until debug logging is enabled, so disabled debug calls do not allocate strings.
     */
    public static void debug(String format, Object... args) {
        if (APConfiguration.enableDebugLog) {
            log(Level.INFO, String.format(format, args));
        }
    }

    public static void error(Object object) {
        log(Level.ERROR, object);
    }

    public static void error(String message, Throwable throwable) {
        if (throwable == null) {
            log(Level.ERROR, message);
        } else {
            FMLLog.log(AdditionalPipes.NAME, Level.ERROR, throwable, String.valueOf(message));
        }
    }

    public static void unexpected(Object object) {
        log(Level.ERROR, "...what? " + object);
    }

    public static void fatal(Object object) {
        log(Level.FATAL, object);
    }

    public static void info(Object object) {
        log(Level.INFO, object);
    }

    public static void off(Object object) {
        log(Level.OFF, object);
    }

    public static void trace(Object object) {
        log(Level.TRACE, object);
    }

    public static void warn(Object object) {
        log(Level.WARN, object);
    }
}
