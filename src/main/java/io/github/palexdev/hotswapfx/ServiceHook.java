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
