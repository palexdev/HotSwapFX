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
