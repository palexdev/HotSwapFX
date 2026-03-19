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

    private final Map<String, Set<TrackedRef>> registry = new HashMap<>();
    private final Map<String, Set<String>> reverseDeps = new HashMap<>();
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
        String className = node.getClass().getName();
        registry.computeIfAbsent(className, k -> new HashSet<>())
            .add(new TrackedRef(node, refQueue));
        ofNullable(node.getClass().getAnnotation(HotSwappable.class))
            .map(HotSwappable::dependencies)
            .ifPresent(deps -> {
                for (Class<?> dep : deps) {
                    reverseDeps.computeIfAbsent(dep.getName(), k -> new HashSet<>())
                        .add(className);
                }
            });
    }

    /// @return all the registered dependencies for the given class
    public Set<String> dependenciesOf(Class<?> klass) {
        Set<String> deps = new LinkedHashSet<>();
        collectOwners(klass.getName(), deps);
        return deps;
    }

    private void collectOwners(String className, Set<String> visited) {
        Set<String> owners = reverseDeps.getOrDefault(className, Collections.emptySet());
        for (String owner : owners) {
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
        var refs = registry.get(node.getClass().getName());
        if (refs != null) refs.removeIf(r -> r.get() == node);
    }

    /// @return all the tracked instances for the given class
    public List<Node> getInstances(Class<?> klass) {
        purgeStale();
        var refs = registry.getOrDefault(klass.getName(), Collections.emptySet());
        return refs.stream()
            .map(TrackedRef::get)
            .filter(Objects::nonNull)
            .toList();
    }

    /// Removes GCed references from the registry.
    private void purgeStale() {
        TrackedRef unalive;
        while ((unalive = (TrackedRef) refQueue.poll()) != null) {
            var refs = registry.get(unalive.className);
            if (refs != null) refs.remove(unalive);
        }
    }

    //================================================================================
    // Inner Classes
    //================================================================================

    static class TrackedRef extends WeakReference<Node> {
        final String className;
        TrackedRef(Node node, ReferenceQueue<Node> queue) {
            super(node, queue);
            this.className = node.getClass().getName();
        }
    }
}
