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

package io.github.palexdev.hotswapfx.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.palexdev.hotswapfx.core.Utils;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import org.tinylog.Logger;

/// Annotation to be used on a method to tell the hot swap system how to replace an old node/view with the new instance
/// after reloading.
///
/// _The method annotated by this must have one arg of type [Node] (or compatible subclasses. For example, a `FooView`
/// will receive a `FooView` object), which is the new instance of the node to be swapped in!_
///
/// Defaults are implemented in [Default].
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SwapStrategy {


    /// By default, the hotswap service attempts at replacing the old node in the scenegraph in the following ways:
    /// - If the node's parent is not `null` and is of type [Pane] (because it exposes the children list) it replaces
    ///   the old node with the new one at the same index
    /// - If the node is the root of a [Scene], it replaces the old node with the new one in the scene
    ///
    /// The two attempts are combined into one by [#swapInScenegraph(Node, Node)]
    ///
    /// _Note: swapping must be done on the FX thread and ideally the calling thread should wait for the swap to happen!
    /// Default implementations already do this with [Utils#waitForFX(Runnable)]_
    interface Default {
        static boolean swapInScenegraph(Node oldNode, Node newNode) {
            return swapInParent(oldNode, newNode, oldNode.getParent()) || swapInScene(oldNode, newNode, oldNode.getScene());
        }

        static boolean swapInParent(Node oldNode, Node newNode, Node parent) {
            if (oldNode != null && parent instanceof Pane pane) {
                ObservableList<Node> children = pane.getChildren();
                Utils.waitForFX(() -> {
                    int idx = children.indexOf(oldNode);
                    if (idx >= 0) children.set(idx, newNode);
                    Logger.debug("Node {} replaced in parent container", oldNode);
                });
                return true;
            }
            return false;
        }

        static boolean swapInScene(Node oldNode, Node newNode, Scene scene) {
            if (oldNode != null &&
                scene != null &&
                scene.getRoot() == oldNode
            ) {
                Utils.waitForFX(() -> {
                    scene.setRoot(((Parent) newNode));
                    Logger.debug("Node {} replaced in Scene", oldNode);
                });
                return true;
            }
            return false;
        }
    }
}
