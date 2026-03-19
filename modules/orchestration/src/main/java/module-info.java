module HotSwapFX.Orchestration {
    requires com.esotericsoftware.kryo;
    requires io.github.palexdev.kryonet;
    requires com.esotericsoftware.minlog;
    requires org.tinylog.api;
    requires org.tinylog.impl;

    exports io.github.palexdev.hotswapfx.orchestration;
    exports io.github.palexdev.hotswapfx.orchestration.message;
}