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
import java.util.Objects;

import static io.github.palexdev.hotswapfx.ServiceLogger.logger;

/// Because the service reloads classes from the classpath, two classes with the same fully qualified name will be different.<br >
/// This wrapper solves this issue by wrapping a class' fully qualified name and the related class.<br >
/// The latter is initially `null`, and it's loaded only when requested by the service, [#reload(Path)].
///
/// The benefit of this change is that when a reload request arrives from the [ClassPathWatcher], the [HotSwapService]
/// can check whether a class is registered before reloading it from the disk, which is a costly operation. Especially
/// considering that many classes may have changed, but only a few may be registered on the service.<br >
/// In short, this allows implementing lazy reload of class files.
public class ClassWrapper {
    //================================================================================
    // Static Properties
    //================================================================================
    private static final HotSwapClassLoader classLoader = new HotSwapClassLoader();

    //================================================================================
    // Properties
    //================================================================================
    private final String name;
    private Class<?> klass;

    //================================================================================
    // Constructors
    //================================================================================
    public ClassWrapper(String name) {
        this.name = name;
    }

    /// Creates a new `ClassWrapper` instance with the fully qualified name of the given class.
    public static ClassWrapper wrap(Class<?> klass) {
        return new ClassWrapper(klass.getName());
    }

    //================================================================================
    // Methods
    //================================================================================

    /// Reloads and returns a class from the given path.<br >
    /// There are no checks here, the given file is expected to be the class associated with the fully qualified name
    /// wrapped by this object. This is ensured by the [HotSwapService].
    ///
    /// If the reload fails, an error is logged and `null` is returned.
    ///
    /// @see HotSwapClassLoader
    protected Class<?> reload(Path path) {
        try {
            klass = classLoader.reload(name, path);
            return klass;
        } catch (Exception ex) {
            logger().error(ex, "Failed to reload class {} at path: {}", name, path);
            return null;
        }
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ClassWrapper that = (ClassWrapper) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    //================================================================================
    // Getters
    //================================================================================
    public String name() {
        return name;
    }

    public String simpleName() {
        return name.substring(name.lastIndexOf('.') + 1);
    }

    public Class<?> klass() {
        return klass;
    }
}