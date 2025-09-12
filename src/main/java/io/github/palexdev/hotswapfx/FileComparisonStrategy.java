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

import io.github.palexdev.hotswapfx.ClassPathWatcher.FileAttributes;

/// Interface to let the user choose which strategy to use when comparing two files by their main attributes, [FileAttributes].
public interface FileComparisonStrategy {

    /// @return whether the attributes of `file1` are equal to those of `file2`.
    boolean equals(FileAttributes f1, FileAttributes f2);

    default FileComparisonStrategy compose(FileComparisonStrategy other) {
        return (f1, f2) -> equals(f1, f2) && other.equals(f1, f2);
    }

    /// This strategy compares only the last modified date and the size.
    FileComparisonStrategy DATE_SIZE = (f1, f2) ->
        f1.lastModified() == f2.lastModified() && f1.size() == f2.size();

    /// This strategy compares only the hashes (file contents).
    ///
    /// @see FileHasher
    FileComparisonStrategy HASH = ((f1, f2) -> FileHasher.equals(f1.hash(), f2.hash()));
}
