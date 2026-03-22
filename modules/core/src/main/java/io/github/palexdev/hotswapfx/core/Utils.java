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
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
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

    /// @return a new [PathMatcher] object that matches against the given expression.
    /// If the expression is not prefixed by either `glob:` or `regex:`, the expression is considered to be a glob pattern.
    ///
    /// @see FileSystem#getPathMatcher(String)
    public static PathMatcher toPathMatcher(String expr) {
        if (!expr.startsWith("glob:") && !expr.startsWith("regex:")) {
            expr = "glob:" + expr;
        }
        return FileSystems.getDefault().getPathMatcher(expr);
    }
}
