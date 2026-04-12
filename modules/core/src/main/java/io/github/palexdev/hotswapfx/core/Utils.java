/*
 * Copyright (C) 2026 Parisi Alessandro - alessandro.parisi406@gmail.com
 * This file is part of HotSwapFX (https://github.com/palexdev/HotSwapFX)
 *
 * HotSwapFX is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * HotSwapFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with HotSwapFX. If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.palexdev.hotswapfx.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import io.github.palexdev.hotswapfx.core.annotations.Factory;
import javafx.application.Platform;
import javafx.scene.Node;
import org.tinylog.Logger;

public class Utils {

    //================================================================================
    // Constructors
    //================================================================================

    private Utils() {}

    //================================================================================
    // Static Methods
    //================================================================================

    /// Creates a new instance of the given node type by either:
    /// - Retrieving and invoking the first method annotated with [Factory] on the given node's class
    /// - Invoking the no-args constructor
    public static Node newInstanceOf(Node node) throws ReflectiveOperationException {
        Class<? extends Node> klass = node.getClass();
        Method factory = Arrays.stream(klass.getDeclaredMethods())
            .filter(m -> m.isAnnotationPresent(Factory.class))
            .findFirst()
            .orElse(null);
        if (factory != null) {
            factory.setAccessible(true);
            return (Node) factory.invoke(node);
        }

        Constructor<? extends Node> noArg = klass.getConstructor();
        return noArg.newInstance();
    }

    /// Runs the given runnable on the FX thread and blocks the calling thread until it finishes.
    public static void waitForFX(ThrowingRunnable runnable) {
        waitForFxAndGet((ThrowingSupplier<Void>) () -> {
            try {
                runnable.run();
                return null;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }, null);
    }

    /// Runs the given supplier on the FX thread and blocks the calling thread until it finishes.
    public static <T> T waitForFxAndGet(ThrowingSupplier<T> supplier, T defaultValue) {
        if (Platform.isFxApplicationThread()) {
            try {
                return supplier.get();
            } catch (Exception ex) {
                Logger.error(ex);
                return defaultValue;
            }
        }

        CompletableFuture<T> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable ex) {
                future.completeExceptionally(ex);
            }
        });
        return future.join();
    }

    /// @return a new [PathMatcher] object that matches against the given expression.
    /// If the expression is not prefixed by either `glob:` or `regex:`, the expression is considered to be a glob pattern.
    /// @see FileSystem#getPathMatcher(String)
    public static PathMatcher toPathMatcher(String expr) {
        if (!expr.startsWith("glob:") && !expr.startsWith("regex:")) {
            expr = "glob:" + expr;
        }
        return FileSystems.getDefault().getPathMatcher(expr);
    }

    //================================================================================
    // Inner Classes
    //================================================================================

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
