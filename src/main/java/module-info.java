module HotSwapFX {
    requires java.management;
    requires transitive javafx.graphics;
    requires org.jooq.joor;
    requires org.tinylog.api;

    exports io.github.palexdev.hotswapfx;
}