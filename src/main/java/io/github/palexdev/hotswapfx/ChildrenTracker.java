package io.github.palexdev.hotswapfx;

import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.InvalidationListener;
import javafx.scene.Node;
import javafx.scene.Parent;

/// Special and naive implementation of a late [ServiceHook] that triggers the reload of a component if any of its direct
/// children's class file changes on the class path.
///
/// The mechanism is quite simple. A listener is added on the [HotSwappable#parent()]'s children list, and when it changes,
/// we gather all the children's classes in a set (wrapped in [ClassWrapper]). When the hook is notified, it checks
/// if the changed class is present among the children and in that case triggers the component's reload by calling
/// [HotSwapService#reload(String)].
///
/// This is the default behavior, but it can be customized by setting the [TrackingStrategy] via [#setTrackingStrategy(TrackingStrategy)].
/// If you need to watch for deeper parts of the scenegraph, a custom tracking strategy could be an option. However, keep
/// in mind that checking the scenegraph may be costly depending on the situation. An alternative would be to register
/// a hook for the children's classes you want to monitor and reload your component.
public class ChildrenTracker implements ServiceHook<ClassWrapper> {
    //================================================================================
    // Properties
    //================================================================================
    private final HotSwappable<?> component;
    private Set<ClassWrapper> childrenClasses;
    private InvalidationListener updater = _ -> childrenClasses = getChildrenClasses();
    private TrackingStrategy strategy = (klass, children) -> children.contains(klass);

    //================================================================================
    // Constructors
    //================================================================================
    public ChildrenTracker(HotSwappable<?> component) {
        this.component = component;
        childrenClasses = getChildrenClasses();
        component.parent().getChildrenUnmodifiable().addListener(updater);
    }

    //================================================================================
    // Methods
    //================================================================================

    /// Calls the set [TrackingStrategy] on the input [ClassWrapper] and the direct children's classes from [HotSwappable#parent()].
    ///
    /// @see #getChildrenClasses()
    @Override
    public void onEvent(ClassWrapper klass) {
        if (childrenClasses == null || childrenClasses.isEmpty()) childrenClasses = getChildrenClasses();
        if (strategy.shouldReload(klass, childrenClasses)) HotSwapService.instance().reload(component.id());
    }

    /// This is called by the [HotSwappable] component when it is reloaded. Since the [HotSwappable#parent()] instance
    /// changes, we need to remove the listener from the old listener, attach it to the new one, and re-gather the
    /// children's classes.
    ///
    /// @see #getChildrenClasses()
    protected void update(Parent oldInstance, Parent newInstance) {
        if (oldInstance != null) oldInstance.getChildrenUnmodifiable().removeListener(updater);
        newInstance.getChildrenUnmodifiable().addListener(updater);
        updater.invalidated(null);
    }

    /// Responsible for gathering all the nodes in [Parent#getChildrenUnmodifiable()] from [HotSwappable#parent()],
    /// getting their classes, wrapping them in [ClassWrappers][ClassWrapper] and collecting the results in a set.
    ///
    /// @see ClassWrapper
    protected Set<ClassWrapper> getChildrenClasses() {
        return component.parent().getChildrenUnmodifiable().stream()
            .map(Node::getClass)
            .map(ClassWrapper::wrap)
            .collect(Collectors.toSet());
    }

    /// Removes the children listener from the current parent instance in [HotSwappable#parent()].
    public void dispose() {
        component.parent().getChildrenUnmodifiable().removeListener(updater);
        updater = null;
    }

    //================================================================================
    // Getters/Setters
    //================================================================================
    public TrackingStrategy getTrackingStrategy() {
        return strategy;
    }

    /// Sets the strategy used to determine whether to trigger or not the reload of the component.
    ///
    /// Defaults to [Set#contains(Object)] if the given value is `null`
    public ChildrenTracker setTrackingStrategy(TrackingStrategy strategy) {
        this.strategy = strategy != null ? strategy : (cw, children) -> children.contains(cw);
        return this;
    }

    //================================================================================
    // Inner Classes
    //================================================================================

    /// A functional interface used by the children tracker to determine when to reload the component after a class changes
    /// on the class path. Accepts two inputs:
    /// 1) The class that changed, wrapped in a [ClassWrapper]
    /// 2) The direct children's classes of the component
    @FunctionalInterface
    public interface TrackingStrategy {
        boolean shouldReload(ClassWrapper cw, Set<ClassWrapper> childrenClasses);
    }
}
