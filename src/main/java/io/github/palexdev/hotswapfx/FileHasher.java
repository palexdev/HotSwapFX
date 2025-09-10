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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/// Utility class to hash and compare files using the [Murmur3F] algorithm.
public class FileHasher {

    //================================================================================
    // Constructors
    //================================================================================
    private FileHasher() {}

    //================================================================================
    // Static Methods
    //================================================================================

    /// @return whether the two given paths are equal by using [FileHasher#hash(Path)] and [FileHasher#equals(byte[], byte[])].
    public static boolean equals(Path p1, Path p2) {
        if (p1 == null || p2 == null) {
            return false;
        }
        try {
            byte[] h1 = hash(p1);
            byte[] h2 = hash(p2);
            return equals(h1, h2);
        } catch (Exception ex) {
            return false;
        }
    }

    /// @return whether the two given byte arrays are equal by using [Arrays#mismatch(byte[], byte[])].
    public static boolean equals(byte[] h1, byte[] h2) {
        if (h1 == null || h2 == null) {
            return false;
        }
        return Arrays.mismatch(h1, h2) == -1;
    }

    /// @return the generated Murmur3F hash for the given file, or `null` if an error occurred.
    public static byte[] hash(Path path) {
        try {
            Murmur3F m = new Murmur3F();
            ByteBuffer bb = ByteBuffer.wrap(Files.readAllBytes(path));
            m.update(bb);
            return m.getValueBytesBigEndian();
        } catch (IOException ex) {
            return null;
        }
    }
}
