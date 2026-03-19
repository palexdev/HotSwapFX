package io.github.palexdev.hotswapfx.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Annotation to mark a certain type as part of the hot swap mechanism.
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HotSwappable {

    /// When any of the dependencies specified by this change, the marked type is also reloaded by the service.
    Class<?>[] dependencies();
}
