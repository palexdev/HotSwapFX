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

import java.util.Objects;

/// A simple wrapper for a [Class]. Convenient for this project to compare two classes. Because we reload the changed
/// classes by redefining them in a new class loader (see [HotSwapService] and [HotSwapClassLoader]), we can't use
/// the equal sign nor the equals method when comparing two classes because they end up being different, even though
/// they are not. The solution is to compare their fully qualified names, and so the [#equals(Object)] method is overridden
/// to do so. The [#hashCode()] method is also overridden to use the fully qualified name instead.
public record ClassWrapper(Class<?> klass) {
    //================================================================================
    // Constructors
    //================================================================================
    public ClassWrapper {
        if (klass == null) throw new NullPointerException("Class cannot be null");
    }

    public static ClassWrapper wrap(Class<?> klass) {
        return new ClassWrapper(klass);
    }

    //================================================================================
    // Methods
    //================================================================================

    /// Wraps the given [Class] and runs [#equals(Object)] on it.
    public boolean equals(Class<?> klass) {
        return this.equals(wrap(klass));
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    /// {@inheritDoc}
    ///
    /// Overridden to compare the classes' fully qualified names.
    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        ClassWrapper that = (ClassWrapper) object;
        return klass.getName().equals(that.klass.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(klass.getName());
    }
}
