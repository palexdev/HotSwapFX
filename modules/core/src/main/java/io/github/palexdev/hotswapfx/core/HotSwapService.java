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

import java.util.List;
import java.util.Set;

import javafx.scene.Node;
import javafx.scene.Parent;
import org.tinylog.Logger;

/// Core class that represents the hotswap service. Responsible for instantiating new nodes and swapping them in
/// the scenegraph.
///
/// @see HotSwapStrategy
public class HotSwapService {

    //================================================================================
    // Singleton
    //================================================================================

    private static final HotSwapService INSTANCE = new HotSwapService();

    public static HotSwapService instance() {
        return INSTANCE;
    }

    //================================================================================
    // Properties
    //================================================================================

    private final HotSwapRegistry registry = new HotSwapRegistry();

    //================================================================================
    // Constructors
    //================================================================================

    private HotSwapService() {}

    //================================================================================
    // Methods
    //================================================================================

    public void swapNodes(Class<?> klass) {
        if (!Node.class.isAssignableFrom(klass)) {
            Logger.trace("Class {} is not a Node, skipping...", klass.getName());
            return;
        }

        Logger.debug("Swapping instances of: {}", klass.getName());
        List<Node> instances = registry.getInstances(klass);
        Logger.trace("Found {} instances", instances.size());
        for (Node node : instances) {
            try {
                Parent parent = node.getParent();
                Node newNode = Utils.newInstanceOf(node);
                Logger.debug("Instantiated new node: {}, replacing...", newNode);

                // Try 1: use strategy if available
                if (node instanceof HotSwapStrategy strategy && strategy.swapInScenegraph(newNode, parent)) {
                    registry.unregister(node);
                    continue;
                }
                // Try 2: replacing in the parent container...
                if (HotSwapStrategy.swapInParent(node, newNode, parent)) {
                    registry.unregister(node);
                    continue;
                }
                // Try 3: maybe it's the root of some Scene...
                if (HotSwapStrategy.swapInScene(node, newNode, node.getScene())) {
                    registry.unregister(node);
                    continue;
                }

                throw new HotSwapException("Node is either detached from JavaFX Scenegraph or new one could not be attached");
            } catch (Exception ex) {
                Logger.error(ex, "Could not replace node: {}", node);
            }
        }
    }

    public void register(Node node) {
        registry.register(node);
    }

    public Set<Class<?>> dependenciesOf(Class<?> klass) {
        return registry.dependenciesOf(klass);
    }

    // TODO add hook mechanisms
}
