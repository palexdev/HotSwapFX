module HotSwapFX.Showcase {
    requires HotSwapFX.Core;
    requires javafx.controls;
    requires javafx.graphics;
    requires mfx.core;
    requires mfx.resources;
    requires ImCache;
    requires org.tinylog.api;
    requires org.tinylog.impl;
    requires VirtualizedFX;

    exports io.github.palexdev.hotswapfx.showcase.weather;
}