package io.github.palexdev.hotswapfx;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import io.github.palexdev.watcher.DirectoryChangeEvent;
import io.github.palexdev.watcher.DirectoryWatcher;

/// This is responsible for watching for changes among the classes on the class path (which is given by [Utils#getClassPathDirectories()]).
/// For this I use a personal fork of the amazing work done [here](https://github.com/gmethvin/directory-watcher), which
/// is basically a convenience facade over the Java's disastrous [WatchService] API.
///
/// Whenever a change is detected, it is notified through a [Consumer] that can be specified by the user. In the case
/// of [HotSwapService], the action reloads the changed classes.
public class ClassPathWatcher {
    //================================================================================
    // Properties
    //================================================================================
    private final DirectoryWatcher watcher;
    private Consumer<DirectoryChangeEvent> onEvent = _ -> {};

    //================================================================================
    // Constructors
    //================================================================================
    public ClassPathWatcher() {
        watcher = buildWatcher();
    }

    //================================================================================
    // Methods
    //================================================================================

    /// Starts the watcher asynchronously on a virtual thread.
    public void start() {
        if (watcher != null)
            watcher.watchAsync(Executors.newVirtualThreadPerTaskExecutor());
    }

    /// Tries to stop the watcher.
    public void stop() {
        if (watcher != null && !watcher.isClosed()) {
            try {
                watcher.close();
            } catch (IOException ex) {
                HotSwapService.logger().error(ex, "Failed to close class path watcher");
            }
        }
    }

    /// Calls the specified [#getOnEvent()] [Consumer] with the given event.
    protected void onEvent(DirectoryChangeEvent e) {
        onEvent.accept(e);
    }

    /// Creates the [DirectoryWatcher] which observes for changes on the class path and notifies through [#onEvent(DirectoryChangeEvent)].
    protected DirectoryWatcher buildWatcher() {
        try {
            List<Path> paths = Utils.getClassPathDirectories();
            return DirectoryWatcher.builder()
                .paths(paths)
                .listener(this::onEvent)
                .fileHashing(false)
                .build();
        } catch (IOException ex) {
            HotSwapService.logger().error(ex, "Failed to create class path watcher");
            return null;
        }
    }

    //================================================================================
    // Getters/Setters
    //================================================================================
    public DirectoryWatcher getWatcher() {
        return watcher;
    }

    public Consumer<DirectoryChangeEvent> getOnEvent() {
        return onEvent;
    }

    public void setOnEvent(Consumer<DirectoryChangeEvent> onEvent) {
        this.onEvent = onEvent != null ? onEvent : _ -> {};
    }
}
