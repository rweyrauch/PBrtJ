/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt;

import com.sampullara.cli.*;
import org.pbrt.core.Api;
import org.pbrt.core.Options;
import org.pbrt.core.Error;
import org.pbrt.core.Parser;

import java.util.List;

public class Main {

    @Argument(description = "Print this help text.")
    private static Boolean help = false;

    @Argument(alias = "n", description = "Use specified number of threads for rendering.")
    private static Integer nthreads = -1;

    @Argument(alias = "o", description = "Write the final image to the given filename.")
    private static String outfile = "";

    @Argument(description = "Automatically reduce a number of quality settings to render more quickly.")
    private static Boolean quick = false;

    @Argument(description = "Suppress all text output other than error messages.")
    private static Boolean quiet = false;

    @Argument(alias = "l", description = "Specify directory that log files should be written to.")
    private static String logdir = "";

    @Argument(description = "Print all logging messages to stderr.")
    private static Boolean logtostderr = false;

    @Argument(description = "Log messages at or above this level (0 -> INFO, 1 -> WARNING, 2 -> ERROR, 3-> FATAL). Default: 0.")
    private static Integer minloglevel = 0;

    @Argument(alias = "v", description = "Set VLOG verbosity.")
    private static Integer verbosity = 0;

    @Argument(description = "Print a reformatted version of the input file(s) to standard output. Does not render an image.")
    private static Boolean cat = false;

    @Argument(description = "Print a reformatted version of the input file(s) to standard output and convert all triangle meshes to PLY files. Does not render an image.")
    private static Boolean toply = false;

    public static void main(String[] args) {

	    final List<String> parse;
        try {
            parse = Args.parse(Main.class, args);
        } catch (IllegalArgumentException e) {
            Args.usage(Main.class);
            System.exit(1);
            return;
        }

        if (help) {
            Args.usage(Main.class);
            System.exit(0);
            return;
        }

        Options options = new Options();
        options.NumThreads = nthreads;
        options.ImageFile = outfile;
        options.QuickRender = quick;
        options.Quiet = quiet;

        options.Cat = cat;
        options.ToPly = toply;

        if (!options.Quiet && !options.Cat && !options.ToPly) {
            System.out.format("PBrtJ version 1 -- port of pbrt v3 to java. [Detected %d cores]\n", Runtime.getRuntime().availableProcessors());
            System.out.format("Copyright (c) 2017 Rick Weyrauch\n\n");
            System.out.format("pbrt source code is Copyright(c) 1998-2016\n");
            System.out.format("Matt Pharr, Greg Humphreys, and Wenzel Jakob.\n");
        }

        Api.pbrtInit(options);

        if (parse.isEmpty()) {
            // Parse scene from standard input
            Parser.ParseFile("-");
        }
        else {
            // Parse scene from input files
            for (String f : parse)
            if (!Parser.ParseFile(f)) {
                Error.Error("Couldn't open scene file \"%s\"", f);
            }
        }
        Api.pbrtCleanup();
    }
}
