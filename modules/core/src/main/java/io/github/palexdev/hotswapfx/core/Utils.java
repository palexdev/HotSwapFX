package io.github.palexdev.hotswapfx.core;

import java.lang.reflect.Constructor;
import java.util.concurrent.CompletableFuture;

import javafx.application.Platform;
import javafx.scene.Node;

public class Utils {

    //================================================================================
    // Constructors
    //================================================================================

    private Utils() {}

    //================================================================================
    // Static Methods
    //================================================================================

    /// Creates a new instace of the given node type by either:
    /// - Calling [HotSwapStrategy#newInstance()] if it implements the interface
    /// - Using reflection to invoke the no-arg constructor
    public static Node newInstanceOf(Node node) throws ReflectiveOperationException {
        if (node instanceof HotSwapStrategy c) {
            return c.newInstance();
        }
        Class<? extends Node> klass = node.getClass();
        Constructor<? extends Node> noArg = klass.getConstructor();
        return noArg.newInstance();
    }

    /// Runs the given runnable on the FX thread and blocks the calling thread until it finishes.
    public static void waitForFX(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                runnable.run();
                future.complete(null);
            } catch (Throwable ex) {
                future.completeExceptionally(ex);
            }
        });
        future.join();
    }
}
