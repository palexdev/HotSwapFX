module HotSwapFX {
    requires java.management;
    requires javafx.graphics;
    requires org.jooq.joor;
    requires directory.watcher;
    requires org.tinylog.api;

    exports io.github.palexdev.hotswapfx;
}