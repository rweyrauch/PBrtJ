/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {

    private static String searchDirectory;

    public static boolean IsAbsolutePath(String filename) {
        Path path = Paths.get(filename);
        return path.isAbsolute();
    }

    public static String AbsolutePath(String filename) {
        Path path = Paths.get(filename);
        return path.toAbsolutePath().toString();
    }

    public static String ResolveFilename(String filename) {
        if (searchDirectory.isEmpty() || filename.isEmpty()) {
            return filename;
        }
        else if (IsAbsolutePath(filename)) {
            return filename;
        }
        else {
            Path path = Paths.get(searchDirectory, filename);
            return path.toString();
        }
    }

    public static String DirectoryContaining(String filename) {
        Path path = Paths.get(filename);
        return path.getParent().toString();
    }
    public static void SetSearchDirectory(String dirname) {
        searchDirectory = dirname;
    }

    public static boolean HasExtension(String value, String ending) {
        return value.endsWith(ending);
    }

}