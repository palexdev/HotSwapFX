module HotSwapFX.Runtime {
    requires transitive HotSwapFX.Core;
    requires transitive HotSwapFX.Orchestration;
    requires transitive javafx.graphics;
    requires java.instrument;
    requires net.bytebuddy;
    requires org.tinylog.api;
    requires org.tinylog.impl;

    exports io.github.palexdev.hotswapfx.runtime;
}