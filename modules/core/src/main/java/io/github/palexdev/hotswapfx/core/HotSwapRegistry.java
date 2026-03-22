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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;

import io.github.palexdev.hotswapfx.core.annotations.HotSwappable;
import javafx.scene.Node;

import static java.util.Optional.ofNullable;

// TODO limit the user of strings in favor of Class<?>

/// Registry that keeps track of all the nodes objects which type is marked by [HotSwappable], as well as their dependencies.
///
/// Uses [WeakReferences][WeakReference] and a [ReferenceQueue] to avoid memory leaks.
class HotSwapRegistry {

    //================================================================================
    // Properties
    //================================================================================

    private final Map<Class<?>, Set<TrackedRef>> registry = new HashMap<>();
    private final Map<Class<?>, Set<Class<?>>> reverseDeps = new HashMap<>();
    private final ReferenceQueue<Node> refQueue = new ReferenceQueue<>();

    //================================================================================
    // Methods
    //================================================================================

    /// Adds the given node to the tracked references.
    ///
    /// If the type was not registered before, a new entry is created.
    ///
    /// If the type marked by [HotSwappable] has dependencies, those are also registered.
    public void register(Node node) {
        purgeStale();
        Class<? extends Node> klass = node.getClass();
        registry.computeIfAbsent(klass, _ -> new HashSet<>())
            .add(new TrackedRef(node, refQueue));
        ofNullable(klass.getAnnotation(HotSwappable.class))
            .map(HotSwappable::dependencies)
            .ifPresent(deps -> {
                for (Class<?> dep : deps) {
                    reverseDeps.computeIfAbsent(dep, _ -> new HashSet<>())
                        .add(klass);
                }
            });
    }

    /// @return all the registered dependencies for the given class
    public Set<Class<?>> dependenciesOf(Class<?> klass) {
        Set<Class<?>> deps = new LinkedHashSet<>();
        collectOwners(klass, deps);
        return deps;
    }

    private void collectOwners(Class<?> klass, Set<Class<?>> visited) {
        Set<Class<?>> owners = reverseDeps.getOrDefault(klass, Collections.emptySet());
        for (Class<?> owner : owners) {
            if (visited.add(owner)) {
                collectOwners(owner, visited);
            }
        }
    }

    /// Removes the given node from the tracked references.
    ///
    /// This is crucial when swapping a node in the scenegraph because the old object removed from it must not be tracked anymore.
    /// The new node is registered automatically.
    public void unregister(Node node) {
        var refs = registry.get(node.getClass());
        if (refs != null) refs.removeIf(r -> r.get() == node);
    }

    /// @return all the tracked instances for the given class
    public List<Node> getInstances(Class<?> klass) {
        purgeStale();
        var refs = registry.getOrDefault(klass, Collections.emptySet());
        return refs.stream()
            .map(TrackedRef::get)
            .filter(Objects::nonNull)
            .toList();
    }

    /// Removes GCed references from the registry.
    private void purgeStale() {
        TrackedRef unalive;
        while ((unalive = (TrackedRef) refQueue.poll()) != null) {
            var refs = registry.get(unalive.theClass);
            if (refs != null) refs.remove(unalive);
        }
    }

    //================================================================================
    // Inner Classes
    //================================================================================

    static class TrackedRef extends WeakReference<Node> {
        final Class<?> theClass;

        TrackedRef(Node node, ReferenceQueue<Node> queue) {
            super(node, queue);
            this.theClass = node.getClass();
        }
    }
}
