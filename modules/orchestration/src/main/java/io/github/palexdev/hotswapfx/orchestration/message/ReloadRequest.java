/*
 * Copyright (C) 2026 Parisi Alessandro - alessandro.parisi406@gmail.com
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

package io.github.palexdev.hotswapfx.orchestration.message;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/// A message that can be sent to the server to signal which files changed, so that the service can reload and or swap
/// registered classes.
public record ReloadRequest(Changes changes) implements Message {

    //================================================================================
    // Inner Classes
    //================================================================================

    public enum ChangeType {
        ADD,
        REMOVE,
        UPDATE
    }

    public static class Changes extends HashMap<Path, ChangeType> {
        public Changes() {}

        public Changes(Map<? extends Path, ? extends ChangeType> m) {super(m);}
    }
}
