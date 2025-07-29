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

/// A functional interface allowing users to hook into the reload process of [HotSwapService].
///
/// @see HookType
@FunctionalInterface
public interface ServiceHook<D> {
    void onEvent(D data);

    default void dispose() {}

    //================================================================================
    // Inner Classes
    //================================================================================

    enum HookType {
        /// Marks early hooks, they work on files. Notified when a file on the class path changes.
        ON_FILE,

        /// Marks late hooks, they work on classes. Notified when a class is about to be reloaded.
        ON_CLASS
    }
}
