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
import java.nio.file.Path;
import java.util.*;

import io.github.palexdev.hotswapfx.core.annotations.HotSwappable;
import javafx.scene.Node;
import org.tinylog.Logger;

import static java.util.Optional.ofNullable;

/// Registry that keeps track of all the nodes objects which type is marked by [HotSwappable], as well as their dependencies.
///
/// Uses [WeakReferences][WeakReference] and a [ReferenceQueue] to avoid memory leaks.
class HotSwapRegistry {

    //================================================================================
    // Properties
    //================================================================================

    private final Map<Class<?>, Set<TrackedRef>> registry = new HashMap<>();
    private final ReferenceQueue<Node> refQueue = new ReferenceQueue<>();

    private final Map<Class<?>, Set<Class<?>>> deps = new HashMap<>();
    private final Map<Class<?>, ServiceHook<?>> resHooks = new HashMap<>();

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

        // Register dependencies
        HotSwappable annotation = klass.getAnnotation(HotSwappable.class);
        ofNullable(annotation).map(HotSwappable::dependencies)
            .filter(deps -> deps.length != 0)
            .ifPresentOrElse(
                deps -> this.deps.put(klass, Set.of(deps)),
                () -> deps.remove(klass)
            );

        // Resources hook
        ServiceHook<Path> resHook = ofNullable(annotation).map(HotSwappable::resources)
            .filter(res -> !res.isBlank())
            .map(Utils::toPathMatcher)
            .map(matcher -> (ServiceHook<Path>) p -> {
                if (matcher.matches(p.getFileName())) {
                    Logger.info("Reloading class {} on matched pattern: {}", klass, annotation.resources());
                    HotSwapService.instance().swapNodes(klass);
                }
            })
            .orElse(null);
        if (resHooks.containsKey(klass)) { // unregister previous hook
            HotSwapService.instance().removeHook(resHooks.get(klass));
        }
        if (resHook != null) { // register new hook
            HotSwapService.instance().earlyHook(resHook);
            resHooks.put(klass, resHook);
        }

    }

    /// @return all the dependencies of the given class
    public Set<Class<?>> dependenciesOf(Class<?> klass) {
        return deps.getOrDefault(klass, Collections.emptySet());
    }

    /// @return all the classes which depend on the given one
    public Set<Class<?>> dependsOn(Class<?> klass) {
        Set<Class<?>> deps = new HashSet<>();
        this.deps.forEach((k, v) -> {
            if (v.contains(klass)) deps.add(k);
        });
        return deps;
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

    /// @return the currently tracked classes which may be reloaded at some point
    public Set<Class<?>> trackedClasses() {
        return Collections.unmodifiableSet(registry.keySet());
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
