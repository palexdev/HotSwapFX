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

package io.github.palexdev.hotswapfx.core;

import java.util.LinkedHashSet;

/// A functional interface allowing users to hook into the reload process of [HotSwapService].
///
/// @see HookType
@FunctionalInterface
public interface ServiceHook<T> {

    void onEvent(T data);

    default void dispose() {}

    //================================================================================
    // Inner Classes
    //================================================================================

    class Hooks extends LinkedHashSet<ServiceHook<?>> {}

    enum HookType {
        /// Marks early hooks, they work on files. Notified when a file on the class path changes.
        ///
        /// _Note that the notification (to the hook) arrives before any actual operation on the file. So, for example,
        /// for classes the reload may fail, but the hook is still notified of the event._
        ON_FILE,

        /// Marks late hooks, they work on classes. Notified when a class is about to be reloaded.
        ///
        /// _Note that there is no guarantee the class will actually be reloaded for several reasons. For example, the
        /// class may not be a JavaFX Node, or the swap process may fail at some point_
        ON_CLASS
    }
}
