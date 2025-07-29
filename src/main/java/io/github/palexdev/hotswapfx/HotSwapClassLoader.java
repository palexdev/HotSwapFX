package io.github.palexdev.hotswapfx;

import java.nio.file.Files;
import java.nio.file.Path;

/// This is responsible for reloading a class when its file has changed on the class path. The only way to do so in Java
/// is to redefine the [Class] in a new [ClassLoader] using [ClassLoader#defineClass(String, byte\[\], int, int)].<br >
///
/// @see [#reload(String, Path) ]
public class HotSwapClassLoader {

    //================================================================================
    // Constructors
    //================================================================================
    public HotSwapClassLoader() {}

    //================================================================================
    // Methods
    //================================================================================

    /// Reloads a [Class] by redefining it from the given path and the given fully qualified name.
    ///
    /// 1) Creates a new [DefiningClassLoader]
    /// 2) Reads all the bytes from the given path, [Files#readAllBytes(Path)]
    /// 3) Redefines and returns the [Class] with [DefiningClassLoader#defineClass(String, byte\[\])]
    public Class<?> reload(String className, Path path) throws Exception {
        DefiningClassLoader loader = new DefiningClassLoader();
        byte[] data = Files.readAllBytes(path);
        return loader.defineClass(className, data);
    }

    //================================================================================
    // Inner Classes
    //================================================================================
    private static class DefiningClassLoader extends ClassLoader {
        /// Exposes/delegates to [#defineClass(String, byte\[\], int, int)].
        public Class<?> defineClass(String name, byte[] data) {
            return defineClass(name, data, 0, data.length);
        }
    }
}
