module HotSwapFX.DevTools {
    requires transitive HotSwapFX.Orchestration;
    requires transitive javafx.controls;
    requires mfx.core;
    requires mfx.components;
    requires mfx.resources;
    requires org.tinylog.api;
    requires org.tinylog.impl;

    exports io.github.palexdev.hotswapfx.devtools;
}