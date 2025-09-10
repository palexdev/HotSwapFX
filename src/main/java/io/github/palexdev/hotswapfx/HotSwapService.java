/*
 * Copyright (C) 2025 Parisi Alessandro - alessandro.parisi406@gmail.com
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

package io.github.palexdev.hotswapfx;

import java.nio.file.Path;
import java.util.*;

import io.github.palexdev.hotswapfx.ServiceHook.HookType;
import javafx.scene.Node;
import javafx.scene.Parent;

import static io.github.palexdev.hotswapfx.ServiceLogger.logger;

/// The core of this project, a singleton service that watches the class path for changes (using [ClassPathWatcher]) and
/// reloads the registered components as needed.
///
/// There is much going on here, but before discussing details, let's explore the architecture and the core mechanics.
///
/// ### Concept & Architecture<br >
/// The general idea is to allow developers reloading their JavaFX views without the need to recompile and rerun the
/// entire project, but rather recompile just the necessary classes and swap them at runtime.<br >
/// Technically, Java offers all the tools necessary for the task, and it's not even that hard to implement, but the issue
/// relies on the sheer number of possible use cases and edge cases that may arise. Think about it, a scenegraph is no
/// joke. It could be a simple toy app or a fully fledged app with hundreds of nodes and heavy nesting.
///
/// To keep things simple and reliable, it's **highly recommended** to reload big portions of a UI rather than independent
/// components. For example, you should not reload a single button, even if it's the only class that changed, but rather
/// the pane/container in which it is laid out. And this is somewhat enforced by this service as you are allowed to register
/// [Parent] nodes only.<br >
/// That said, the service is very configurable as you're going to read below.
///
/// The service can be started by calling the [#start()] method, but only if the system property `HOTSWAPFX` is set to true.
/// This is a convenient way to disable the mechanism in production.<br >
/// You can stop the service by calling [#stop()] and restart it later if you need it again. If instead you don't need the
/// service anymore, you should call [#dispose()].
///
/// Components are registered on this service with a unique id. To allow watching multiple instances of the same component,
/// each class is associated with a [Set] of ids. When a class changes and the related components need to be reloaded,
/// the service looks it up in a [Map], retrieves all the corresponding ids and for each of them retrieves the component
/// and starts the reload process. Overview of the two maps:
/// - `[Class] -> [Set<String>]`
/// - `[String] -> [HotSwappable]`
///
/// The service offers several ways to register a component:
/// 1) [#register(Parent)]: convenient if you don't need much customization
/// 2) [#register(String, Parent)]: similar to the previous, but it's registered with your id of choice
/// 3) [#register(HotSwappable)]: offers maximum flexibility
///
/// And just as many ways to unregister components:
/// 1) [#unregister(HotSwappable, boolean)] the most flexible as it also allows unregistering all components of the same class
/// 2) [#unregister(String)]: almost as convenient as the first but more error-prone
///
/// Components cannot be unregistered by the [Parent] instance because that's mutable. Only [HotSwappable] remains the same
/// while the internal state is updated.
///
/// The service also allows you to register two types of **hooks** into the reload process:
/// 1) [HookType#ON_FILE]: these are also called _early hooks_ because they are notified as soon as a file
/// on the class path changes, bypassing all checks of the service. They can be useful, for example, if you need to
/// reload something after a resource/assets has changed, [#earlyHook(ServiceHook)].
/// 2) [HookType#ON_CLASS]: these are also called _late hooks_ as they are notified only when a class file changes and
/// after the reloaded class passes some checks (for example, if it is a JavaFX node). They can be useful if you want
/// to reload a component when one of its subcomponents/children changes. Such a mechanism is already implemented in
/// [HotSwappable] through the [ChildrenTracker] class.
///
/// ### Details & Important Notes
///
/// 1) Java's [ClassLoaders][ClassLoader] cache the class they load. For this project this is not good because by reloading
/// an already defined class, there's the risk of not viewing any change or desynchronization. The service uses a new
/// class loader every time a class needs to be reloaded, see [HotSwapClassLoader].
/// 2) The class path watching system is based on polling. By default the poll rate is set to 1 second, but it can be changed
/// by setting the [#POLL_RATE] variable.
/// 3) The service seems to work only if running in debug mode. I don't really know why, since we're technically not
/// relying on some internal hot swap mechanism. We watch the class path, and when something changes, we straight up reload
/// it reading its bytes and defining a new class. That said, I found it very convenient when used in
/// [IntelliJ](https://www.jetbrains.com/idea/) with [this plugin](https://plugins.jetbrains.com/plugin/14832-single-hotswap).
public class HotSwapService {
    //================================================================================
    // Static Members
    //================================================================================
    public static final String SYSTEM_FLAG = "HOTSWAPFX";

    /// This value determines the delay between each scan task of [ClassPathWatcher].<br >
    /// Values are in milliseconds, by default, it's 1000 ms.
    ///
    /// It takes effect only when the service is started. If you change this while it's already running, it won't have any effect!
    public static final long POLL_RATE = 1000;

    private static final HotSwapService INSTANCE = new HotSwapService();

    public static HotSwapService instance() {
        return INSTANCE;
    }

    //================================================================================
    // Properties
    //================================================================================
    private ClassPathWatcher watcher;

    private final Map<ClassWrapper, Set<String>> idsMap = new HashMap<>();
    private final Map<String, HotSwappable<? extends Parent>> componentsMap = new HashMap<>();

    private final Map<HookType, Set<ServiceHook<?>>> hooks = new EnumMap<>(HookType.class);

    //================================================================================
    // Constructors
    //================================================================================
    private HotSwapService() {}

    //================================================================================
    // Methods
    //================================================================================

    /// Starts the service if it's not already running and the `HOTSWAPFX` system property is set to `true`.<br >
    /// Creates the [ClassPathWatcher] starts it.
    ///
    /// Issues a warning if the Java process is not running in debug mode.
    public void start() {
        if (isRunning()) {
            logger().warn("The HotSwapService is already running.");
            return;
        }

        if (!Utils.isServiceEnabled()) {
            logger().warn("Cannot start HotSwapService as it is disabled.\nSet the {} system property to 'true' to enable it", SYSTEM_FLAG);
            return;
        }

        if (!Utils.isDebuggerPresent()) {
            logger().warn(
                "HotSwapService is about to start but it seems like the JVM is not running in debug mode. " +
                "HotSwap functionality may not work."
            );
        }

        watcher = new ClassPathWatcher();
        watcher.start();
    }

    /// Stops the service if it's running by stopping the [ClassPathWatcher].
    public void stop() {
        if (!isRunning()) return;
        if (watcher != null) {
            watcher.stop();
            watcher = null;
        }
    }

    /// Stops the service if it's running and additionally removes any registered components. Use this instead of
    /// [#stop()] if you don't plan on using the service later anymore.
    public void dispose() {
        if (!isRunning()) return;
        if (watcher != null) watcher.stop();
        idsMap.clear();
        componentsMap.values().forEach(HotSwappable::dispose);
        componentsMap.clear();
    }

    public boolean isRunning() {
        return watcher != null;
    }

    // Register/Unregister

    /// Registers the given component by its [HotSwappable#id()], issuing an error if the same id is already registered.
    public void register(HotSwappable<? extends Parent> component) {
        Set<String> ids = idsMap.computeIfAbsent(ClassWrapper.wrap(component.parent().getClass()), _ -> new HashSet<>());
        if (!ids.add(component.id())) {
            logger().error("Component with ID: {} is already registered", component.id());
            return;
        }

        componentsMap.put(component.id(), component);
        logger().debug("Registered component: {})", component);
    }

    /// Convenience method to register a component by an id and the initial [Parent] instance that needs to be swapped.
    ///
    /// @return the registered [HotSwappable] to allow configuring it with fluent API
    public <P extends Parent> HotSwappable<P> register(String id, P parent) {
        HotSwappable<P> component = new HotSwappable<>(id, parent);
        register(component);
        return component;
    }

    /// Convenience method to register a component given only the initial [Parent] instance that needs to be swapped.<br >
    /// The id is automatically generated as follows:
    /// 1) Tries to use [Parent#idProperty()] if available
    /// 2) Otherwise tries to use [Parent#getStyleClass()] is not empty. All the style classes are joined into a single
    /// string delimited by a dot
    /// 3) Otherwise generates a random id with [UUID#randomUUID()]
    ///
    /// **Note:** if the component ends up being registered either by the node's id or style classes, and you change
    /// them, it should still work as the ids are wrapped and never changed in [HotSwappable].
    public <P extends Parent> HotSwappable<P> register(P parent) {
        String id;
        if (parent.getId() != null && !parent.getId().isBlank()) {
            id = parent.getId();
        } else if (!parent.getStyleClass().isEmpty()) {
            id = String.join(".", parent.getStyleClass());
        } else {
            id = UUID.randomUUID().toString();
        }

        HotSwappable<P> component = new HotSwappable<>(id, parent);
        register(component);
        return component;
    }

    /// Unregisters a component with the given id, and if one is found, calls [HotSwappable#dispose()].
    public void unregister(String id) {
        HotSwappable<? extends Parent> component = componentsMap.remove(id);
        if (component != null) component.dispose();
    }

    /// Unregister one or more components:
    /// 1) If the `full` flag is `true`, then it uses the component's [Parent] class to remove all components of that class
    /// 2) If the `full` flag is `false`, then it delegates to [#unregister(String)] with [HotSwappable#id()]
    public void unregister(HotSwappable<? extends Parent> component, boolean full) {
        ClassWrapper klass = ClassWrapper.wrap(component.parent().getClass());
        if (full) {
            Set<String> ids = idsMap.remove(klass);
            ids.forEach(id -> componentsMap.remove(id).dispose());
            logger().debug("Unregistered all components of class: {})", klass);
            return;
        }

        unregister(component.id());
    }

    // Reload

    /// Reloads the component with the given id. Issues an error if no component is found.
    ///
    /// Instantiates a new node and then starts the reload process, see [HotSwappable] and [HotSwappable#onReload(Parent)].
    public void reload(String id) {
        HotSwappable<? extends Parent> component = componentsMap.get(id);
        if (component == null) {
            logger().error("No component registered with ID: {}", id);
            return;
        }
        component.reload();
    }

    /// Runs [#reload(Path)] on all the given paths.
    protected void reload(Path... paths) {
        for (Path path : paths) {
            reload(path);
        }
    }

    /// This core method is responsible for reloading all the registered components associated to the given path.
    ///
    /// Here are all the phases of this reload process:
    /// 1) Notifies early hooks
    /// 2) Filters for '.class' files (excludes module-info.class)
    /// 3) Converts the file path to a fully qualified name with [Utils#getClassName(Path)]
    /// 4) Checks if there are any registered components associated to the class
    /// 5) Reloads the class by calling [ClassWrapper#reload(Path)]
    /// 6) Proceeds only if the class is a JavaFX [Node]
    /// 7) Notifies late hooks
    /// 8) On all the found ids calls [#reload(String)]
    protected void reload(Path path) {
        logger().trace("Notifying early hooks...");
        notifyHooks(HookType.ON_FILE, path);

        String sPath = path.toString();
        if (!sPath.endsWith(".class") || sPath.endsWith("module-info.class")) return;

        String className = Utils.getClassName(path);
        if (className == null) {
            logger().error("Failed to get class name from path: {}", path);
            return;
        }

        ClassWrapper classWrapper = new ClassWrapper(className);
        Set<String> ids = idsMap.get(classWrapper);
        if (ids == null || ids.isEmpty()) {
            logger().info("No registered components found for class: {}", className);
            return;
        }

        Class<?> klass = classWrapper.reload(path);
        if (klass == null) return;

        if (!Node.class.isAssignableFrom(klass)) {
            logger().info("Class {} is not a Node, skipping...", klass);
            return;
        }

        logger().trace("Notifying class hooks...");
        notifyHooks(HookType.ON_CLASS, classWrapper);

        ids.forEach(this::reload);
    }

    // Hooks

    /// Registers an early hook into the [#reload(Path)] process.
    public void earlyHook(ServiceHook<Path> hook) {
        hooks.computeIfAbsent(HookType.ON_FILE, _ -> new LinkedHashSet<>()).add(hook);
    }

    /// Registers a late hook into the [#reload(Path)] process.
    public void lateHook(ServiceHook<ClassWrapper> hook) {
        hooks.computeIfAbsent(HookType.ON_CLASS, _ -> new LinkedHashSet<>()).add(hook);
    }

    /// Unregisters the given hook from the [#reload(Path)] process and calls [ServiceHook#dispose()]
    public void removeHook(ServiceHook<?> hook) {
        hooks.values().forEach(set -> set.remove(hook));
        hook.dispose();
    }

    /// Notifies all registered hooks of the given type, with the given data.
    @SuppressWarnings("unchecked")
    private <D> void notifyHooks(HookType type, D data) {
        Set<ServiceHook<?>> hooks = this.hooks.get(type);
        if (hooks == null || hooks.isEmpty()) return;
        for (ServiceHook<?> hook : hooks) {
            ((ServiceHook<D>) hook).onEvent(data);
        }
    }
}
