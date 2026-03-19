module HotSwapFX.Runtime {
    requires HotSwapFX.Core;
    requires HotSwapFX.Orchestration;
    requires javafx.graphics;
    requires java.instrument;
    requires net.bytebuddy;
    requires org.tinylog.api;
    requires org.tinylog.impl;

    exports io.github.palexdev.hotswapfx.runtime;
}