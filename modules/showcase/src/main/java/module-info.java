module HotSwapFX.Showcase {
    requires HotSwapFX.Core;
    requires javafx.controls;
    requires javafx.graphics;
    requires mfx.core;
    requires mfx.components;
    requires mfx.resources;
    requires VirtualizedFX;
    requires ImCache;
    requires fr.brouillard.oss.cssfx;
    requires org.tinylog.api;
    requires org.tinylog.impl;

    exports io.github.palexdev.hotswapfx.showcase.weather;
}