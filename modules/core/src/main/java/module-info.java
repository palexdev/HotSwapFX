module HotSwapFX.Core {
    requires transitive javafx.graphics;

    requires org.tinylog.api;
    requires org.tinylog.impl;

    exports io.github.palexdev.hotswapfx.core;
    exports io.github.palexdev.hotswapfx.core.annotations;
}