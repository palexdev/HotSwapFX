package io.github.palexdev.hotswapfx;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import org.joor.Reflect;

/// Wrapper class that represents a hot swappable component at runtime identified by a unique [#id()], see [HotSwapService].
/// It also wraps the generic [Parent] instance that needs to be swapped and undergoes four phases during the reload:
/// 1) The reload is triggered by [HotSwapService#reload(String)] and calls [#reload()].
/// 2) The first step is to instantiate the new node that will replace the old one by using the component's specified instantiator.
/// By default, every component uses [#DEFAULT_INSTANTIATOR]. If you want higher performance or need a specific constructor,
/// you can easily set a different instantiation function, [#setInstantiator(Function)].
/// 3) The next step is to clone the state of the old node to the new one. This is optional, the default cloner does
/// nothing. For example, say your view has a combo box, and you selected a specific item for testing, you can restore
/// that choice by setting the state cloner function as needed, [#setStateCloner(BiConsumer)].
/// 4) Finally, the component can be swapped in the scenegraph. This is done by yet another function which by default
/// is [#DEFAULT_RELOAD_ACTION] and can be changed through [#setOnReload(BiConsumer)]. At this point, the parent instance
/// wrapped in this is also updated with the new instance.
///
/// ### Children Tracking
///
/// Initially, the idea was as simple as allowing monitoring only specific container classes. Like I said in [HotSwapService],
/// ideally, you should reload big portions of a UI rather than single small components. However, that would mean that
/// if a subcomponent of a view changes, you would not be able to use this system and need to restart the whole app.<br >
/// So, at that point, the project evolved to allow for what they later became [ServiceHooks][ServiceHook]. The idea is
/// that if any child of a view changes, you are able to tell the service to reload the entire parent. [HotSwappable] already
/// integrates this mechanism through the [ChildrenTracker] hook. You can enable/disable it via [#monitorChildren(Function)],
/// it is disabled by default.
///
/// Updating the child tracker is the final and optional phase of the reload process. Since the parent instance is updated
/// here, the tracker may need to be updated too.
@SuppressWarnings({"rawtypes", "unchecked"})
public class HotSwappable<P extends Parent> {
    //================================================================================
    // Static Properties
    //================================================================================
    public static final BiConsumer DEFAULT_RELOAD_ACTION = (o, n) -> {
        if (o == null || n == null) {
            throw new IllegalStateException("Cannot reload component because either the old or new instance is null");
        }

        Parent parent = ((Parent) o).getParent();
        // Try replacing in parent container...
        if (parent instanceof Pane pane) {
            HotSwapService.logger().debug("Replacing component in parent container: {}", pane);
            ObservableList<Node> children = pane.getChildren();
            int idx = children.indexOf(o);
            if (idx >= 0) Platform.runLater(() -> children.set(idx, ((Parent) n)));
            return;
        }

        // Maybe it's the root of some Scene...
        Platform.runLater(() ->
            Optional.ofNullable(((Parent) o).getScene())
                .filter(s -> s.getRoot() == o)
                .ifPresent(s -> {
                    HotSwapService.logger().debug("Replacing root of scene: {}", s);
                    s.setRoot(((Parent) n));
                }));
    };

    public static final Function DEFAULT_INSTANTIATOR = p -> {
        try {
            Class<? extends Parent> klass = ((Parent) p).getClass();
            return Reflect.onClass(klass).create().get();
        } catch (Exception ex) {
            HotSwapService.logger().error(ex, "Failed to instantiate class: {}", p.getClass());
            return null;
        }
    };

    //================================================================================
    // Properties
    //================================================================================
    private final String id;
    private P parent;
    private BiConsumer<P, P> onReload = DEFAULT_RELOAD_ACTION;
    private BiConsumer<P, P> stateCloner = (_, _) -> {};
    private Function<P, P> instantiator;

    private ChildrenTracker tracker;

    //================================================================================
    // Constructors
    //================================================================================
    public HotSwappable(String id, P parent) {
        if (id == null) throw new NullPointerException("ID cannot be null");
        if (parent == null) throw new NullPointerException("Parent cannot be null");

        this.id = id;
        this.parent = parent;
        this.instantiator = DEFAULT_INSTANTIATOR;
    }

    //================================================================================
    // Methods
    //================================================================================

    /// Convenience shortcut to [HotSwapService#register(HotSwappable)].
    public void register() {
        HotSwapService.instance().register(this);
    }

    /// Core method that runs the reload process as requested by [HotSwapService#reload(String)].<br >
    /// Phases:
    /// 1) Instantiate the new node
    /// 2) Clone the state from the old one to the new one
    /// 3) Replace the node into the scenegraph according to the specified action, [#getOnReload()]
    /// 4) Update the [ChildrenTracker] if enabled
    protected void reload() {
        try {
            Parent oldInstance = parent;
            P newInstance = instantiate();

            HotSwapService.logger().debug("Cloning state of component: {}", id);
            stateCloner.accept(parent, newInstance);

            HotSwapService.logger().debug("Reloading component: {}", id);
            onReload.accept(parent, newInstance);
            this.parent = newInstance;

            if (tracker != null) tracker.update(oldInstance, newInstance);
        } catch (Exception ex) {
            HotSwapService.logger().error(ex, "Failed to reload component: {}", id);
        }
    }

    /// Uses the specified [#getInstantiator()] to create a new instance of the wrapped parent.
    protected P instantiate() {
        HotSwapService.logger().debug("Instantiating component: {}", id);
        return instantiator.apply(parent);
    }

    /// Disposes the component, called when unregistered from the service. Removes the children tracker hook if it was
    /// enabled.
    protected void dispose() {
        if (tracker != null) {
            HotSwapService.instance().removeHook(tracker);
            tracker = null;
        }
    }

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        HotSwappable that = (HotSwappable) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "HotSwappable[" +
               "id=" + id + ", " +
               "parent=" + parent + ']';
    }

    //================================================================================
    // Getters/Setters
    //================================================================================

    /// @return the unique string identifying this component
    public String id() {
        return id;
    }

    /// @return the wrapped parent instance
    public P parent() {
        return parent;
    }

    public BiConsumer<P, P> getOnReload() {
        return onReload;
    }

    /// Sets the action to perform when the node should be replaced in the scenegraph by the new instance.
    ///
    /// Defaults to [#DEFAULT_RELOAD_ACTION] if the given value is `null`.
    public HotSwappable<P> setOnReload(BiConsumer<P, P> onReload) {
        this.onReload = onReload != null ? onReload : DEFAULT_RELOAD_ACTION;
        return this;
    }

    public BiConsumer<P, P> getStateCloner() {
        return stateCloner;
    }

    /// Sets the action to perform to clone the state from the old node to the new one.
    ///
    /// Defaults to an empty action if the given value is `null`.
    public HotSwappable<P> setStateCloner(BiConsumer<P, P> stateCloner) {
        this.stateCloner = stateCloner != null ? stateCloner : (_, _) -> {};
        return this;
    }

    public Function<P, P> getInstantiator() {
        return instantiator;
    }

    /// Sets the function responsible for creating a new instance of the wrapped node.
    ///
    /// Defaults to [#DEFAULT_INSTANTIATOR] if the given value is `null`.
    public HotSwappable<P> setInstantiator(Function<P, P> instantiator) {
        this.instantiator = instantiator != null ? instantiator : DEFAULT_INSTANTIATOR;
        return this;
    }

    /// @return whether the [ChildrenTracker] of this component is active.
    public boolean isMonitoringChildren() {
        return tracker != null;
    }

    /// Enables children tracking by creating a [ChildrenTracker] as specified by the given function. The hook is automatically
    /// registered using [HotSwapService#lateHook(ServiceHook)].
    public HotSwappable<P> monitorChildren(Function<HotSwappable<P>, ChildrenTracker> trackerFn) {
        if (tracker != null) HotSwapService.instance().removeHook(tracker);
        if (trackerFn != null && (tracker = trackerFn.apply(this)) != null) {
            HotSwapService.instance().lateHook(tracker);
        }
        return this;
    }
}
