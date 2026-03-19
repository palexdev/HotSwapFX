package io.github.palexdev.hotswapfx.core;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import org.tinylog.Logger;

/// Interface that lets users customize how a node is going to be swapped in the scenegraph (optional API).
///
/// 1. Nodes are instantiated calling the no-arg constructor by default. Override [#newInstance()] to define a custom
/// instantiation strategy.
/// 2. By default, the hotswap service attempts at replacing the old node in the scenegraph in the following ways:
///     - If the node's parent is not `null` and is of type [Pane] (because it exposes the children list) it replaces
///       the old node with the new one at the same index
///     - If the node is the root of a [Scene] it replaces the old node with the new one in the scene
///
/// These two mechanisms are implemented as defaults by [#swapInParent(Object, Node, Object)] and [#swapInScene(Object, Node, Object)]
///
/// If you want to customize this behavior, override the [#swapInScenegraph(Node, Object)] method. The `parent` parameter
/// is _guaranteed_ to be either a [Parent] or a [Scene].
///
/// _Note: swapping must be done on the FX thread and ideally the calling thread should wait for the swap to happen!
/// Default implementations already do this with [Utils#waitForFX(Runnable)]_
public interface HotSwapStrategy {
    Node newInstance();

    default boolean swapInScenegraph(Node newNode, Object parent) {
        return swapInParent(this, newNode, parent) || swapInScene(this, newNode, parent);
    }

    static boolean swapInParent(Object oldNode, Node newNode, Object parent) {
        if (oldNode instanceof Node && parent instanceof Pane pane) {
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

    static boolean swapInScene(Object oldNode, Node newNode, Object parent) {
        if (oldNode instanceof Node &&
            parent instanceof Scene scene &&
            scene.getRoot() == oldNode
        ) {
            Utils.waitForFX(() -> {
                Platform.runLater(() -> scene.setRoot(((Parent) newNode)));
                Logger.debug("Node {} replaced in Scene", oldNode);
            });
            return true;
        }
        return false;
    }
}
