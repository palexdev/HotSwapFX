/*
 * Copyright (C) 2025 Parisi Alessandro - alessandro.parisi406@gmail.com
 * This file is part of HotSwapFX (https://github.com/palexdev/HotSwapFX)
 *
 * HotSwapFX is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * HotSwapFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with HotSwapFX. If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.palexdev.hotswapfx;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.tinylog.Logger;

/// A bunch of miscellaneous utilities for `HotSwapFX`.
public class Utils {
    //================================================================================
    // Static Properties
    //================================================================================
    private static List<Path> classpath;

    //================================================================================
    // Constructors
    //================================================================================
    private Utils() {}

    //================================================================================
    // Static Methods
    //================================================================================

    /// Given the path to a class file, tries to convert it to a valid fully qualified class name by removing the extension
    /// and replacing the file separators with a dot.
    public static String getClassName(Path path) {
        String ts = path.toString();
        ts = ts.substring(0, ts.length() - 6);
        return ts.replace(FileSystems.getDefault().getSeparator(), ".");
    }

    /// @return the value of the system property `java.class.path` split by colon, so an array with each individual path
    /// separated
    public static String[] getClassPath() {
        return System.getProperty("java.class.path").split(";");
    }

    /// Converts the return value of [#getClassPath()] to a list of [Paths][Path], additionally rejects all files that
    /// are not directories.
    public static List<Path> getClassPathDirectories() {
        if (classpath == null) {
            classpath = Arrays.stream(getClassPath())
                .map(Path::of)
                .filter(Files::isDirectory)
                .toList();
        }
        return classpath;
    }

    /// @return whether the `HotSwapFX` is enabled by looking at the value of the `HOTSWAP` system property (`false` if absent).
    public static boolean isServiceEnabled() {
        try {
            return Boolean.parseBoolean(System.getProperty(HotSwapService.SYSTEM_FLAG, "false"));
        } catch (Exception ex) {
            Logger.error(ex, "Failed to parse {}} system property", HotSwapService.SYSTEM_FLAG);
            return false;
        }
    }

    /// A trick to check whether a debugger is attached to the current running Java process. As far as I know, the hot swap
    /// is possible only when in debug mode. So, we use this trick to inform the user that he probably forgot to debug the app.
    public static boolean isDebuggerPresent() {
        // Get ahold of the Java Runtime Environment (JRE) management interface
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

        // Get the command line arguments that we were originally passed in
        Set<String> args = new HashSet<>(runtime.getInputArguments());

        // Check if the Java Debug Wire Protocol (JDWP) agent is used.
        // One of the items might contain something like "-agentlib:jdwp=transport=dt_socket,address=9009,server=y,suspend=n"
        // We're looking for the string "jdwp".
        return args.toString().contains("jdwp");
    }
}
