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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/// Settings for the [HotSwapService].
public class HotSwapServiceSettings {
    //================================================================================
    // Static Properties
    //================================================================================

    /// This value determines the delay between each scan task of [ClassPathWatcher].<br >
    /// Values are in milliseconds, by default, it's 3000 ms.
    ///
    /// It takes effect only when the service is started. If you change this while it's already running, it won't have any effect!
    public static long POLL_RATE = 3000;

    /// Additional paths for the [ClassPathWatcher] to periodically scan.
    ///
    /// This allows for more flexibility and to cover edge cases that would not be feasible to auto-detect here.<br >
    /// For example:
    /// - Modular projects that do not have a class path. In such cases you have to manually specify the path/s where your
    /// class files reside
    /// - Different build systems. This project is tasted in a Gradle environment because that's what I use. Other systems
    /// should be supported too but are untested.
    public static final Set<Path> EXTRA_WATCH_PATHS = new HashSet<>();

    /// Default is [FileComparisonStrategy#HASH]
    ///
    /// @see FileComparisonStrategy
    public static FileComparisonStrategy FILE_COMPARISON_STRATEGY = FileComparisonStrategy.HASH;

    public static void addExtraWatchPaths(Path... paths) {
        Collections.addAll(EXTRA_WATCH_PATHS, paths);
    }

    public static void addExtraWatchPaths(String... paths) {
        addExtraWatchPaths(Arrays.stream(paths)
            .map(Path::of)
            .toArray(Path[]::new));
    }

    public static void setFileComparisonStrategy(FileComparisonStrategy strategy) {
        FILE_COMPARISON_STRATEGY = strategy;
    }
}
