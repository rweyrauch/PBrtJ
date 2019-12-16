/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */
package org.pbrt.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PBrtTLogger {

    private static final Logger logger = LogManager.getFormatterLogger("Pbrt");

    public synchronized static void Trace(String format, Object... args) {
        if (Pbrt.options.Quiet) return;
        logger.trace(format, args);
    }

    public synchronized static void Info(String format, Object... args) {
        if (Pbrt.options.Quiet) return;
        logger.info(format, args);
    }
 
    public synchronized static void Warning(String format, Object... args) {
        if (Pbrt.options.Quiet) return;
        logger.warn(format, args);
    }

    public synchronized static void Error(String format, Object... args) {
        logger.error(format, args);
    }

}