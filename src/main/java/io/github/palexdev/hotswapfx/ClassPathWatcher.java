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

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.StructuredTaskScope.Subtask;
import java.util.function.BiConsumer;

import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

/// This core class is responsible for watching the class path directories given by [Utils#getClassPathDirectories()]
/// and any extra path registered on [HotSwapServiceSettings#EXTRA_WATCH_PATHS].<br >
/// The watch mechanism is based on polling which runs at a fixed delay
/// (from each task, see [ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)]).<br >
///
/// The watcher visits every file and directory recursively starting from the aforementioned root paths. Each is handled
/// by a separate virtual thread. During the visit, all class files are collected, and it stores their [attributes][FileAttributes]
/// in a map. When the stored value is different from the current one, the path is marked as modified and added
/// to a collection. When all virtual threads have finished, all the modified paths are collected in a single collection,
/// and the reload is sent as a batch to [HotSwapService#reload(Path[])].
///
/// @since 24.2.0 Previously, the service was using a facade over the [WatchService] API. However, that works like crap.
/// Even with file hashing, events were sometimes randomly lost. And that led to registered nodes to not be reloaded and
/// swapper. I grew tired of that bullshit, and so I implemented this polling mechanism. It's much, much more reliable, but
/// it could be slightly more heavy on performance.
/// (Though the [WatchService] API also kinda relies on polling, so it also may not')
public class ClassPathWatcher {
    //================================================================================
    // Static Properties
    //================================================================================
    private static final TaggedLogger LOGGER = Logger.tag("ClassPathWatcher");

    //================================================================================
    // Properties
    //================================================================================
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(t -> {
        Thread thread = new Thread(t);
        thread.setName("HotSwapFX-ClasspathWatcher");
        thread.setDaemon(true);
        return thread;
    });
    private Future<?> task;

    private final Set<Path> watchPaths = new HashSet<>();
    private final Map<Path, FileAttributes> lastAttributes = new ConcurrentHashMap<>();

    //================================================================================
    // Constructors
    //================================================================================
    public ClassPathWatcher() {
        List<Path> directories = Utils.getClassPathDirectories();
        if (directories.isEmpty()) {
            LOGGER.warn("""
                No class path directories found to watch.
                If your project is modular you'll have to manually add the directories to the watch list.
                You can do so by calling ClassPathWatcher.addExtraWatchPath(Path)
                """
            );
        }
        watchPaths.addAll(directories);
        watchPaths.addAll(HotSwapServiceSettings.EXTRA_WATCH_PATHS);
    }

    //================================================================================
    // Methods
    //================================================================================

    /// Starts the watcher asynchronously on a daemon thread.<br >
    /// The polling executes by default at delays specified by [HotSwapServiceSettings#POLL_RATE].
    public void start() {
        task = executor.scheduleWithFixedDelay(
            this::scan,
            0,
            HotSwapServiceSettings.POLL_RATE,
            TimeUnit.MILLISECONDS
        );
    }

    /// Stop the watcher by stopping the poll task immediately.
    public void stop() {
        if (task != null) task.cancel(true);
        executor.shutdown();
        lastAttributes.clear();
        watchPaths.clear();
    }

    /// Scans the class path directories for modified files.<br >
    /// If any are found, they are added to a batch and sent to [HotSwapService#reload(Path[])].
    ///
    /// Each directory scan is assigned to a different virtual thread.
    ///
    /// @see #walk(Path)
    private void scan() {
        lastAttributes.keySet().removeIf(p -> !Files.exists(p));

        LOGGER.trace("Scanning...");
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            List<Subtask<Set<Path>>> subtasks = watchPaths.stream()
                .map(p -> (Callable<Set<Path>>) () -> walk(p))
                .map(scope::fork)
                .toList();

            scope.join().throwIfFailed();

            Path[] allModified = subtasks.stream()
                .flatMap(s -> s.get().stream())
                .distinct()
                .toArray(Path[]::new);
            LOGGER.trace("End of scan, found {} modified files", allModified.length);
            if (allModified.length > 0) {
                HotSwapService.instance().reload(allModified);
            }
        } catch (Exception ex) {
            LOGGER.error(ex, "Failed to scan classpath for modified files");
        }
    }

    /// Walks down the given `path`, collects and returns all class files for which [file attributes][FileAttributes]
    /// have changed since the last scan.
    private Set<Path> walk(Path path) throws IOException {
        if (!Files.isDirectory(path)) return Collections.emptySet();
        Set<Path> modified = new HashSet<>();
        Files.walkFileTree(path, new FileWalker((p, a) -> {
            if (p.toString().endsWith(".class")) {
                long lastMillis = a.lastModifiedTime().toMillis();
                long size = a.size();
                byte[] hash = FileHasher.hash(p);
                FileAttributes newAttr = new FileAttributes(lastMillis, size, hash);
                FileAttributes prevAttr = lastAttributes.put(p, newAttr);
                if (prevAttr != null && (!Objects.equals(prevAttr, newAttr))) {
                    LOGGER.debug("File modified: {}, adding to batch", p);
                    modified.add(p);
                }
            }
        }));
        return modified;
    }

    //================================================================================
    // Inner Classes
    //================================================================================

    /// Wrapper for three file attributes:
    /// - the last modified timestamp
    /// - the size
    /// - the hash
    ///
    /// It's used by the `ClassPathWatcher` to determine if a file has changed since the last scan.<br >
    /// Hashes are compared as a last resort if the date and size fail to identify a change.
    ///
    /// @see FileHasher#equals(byte[], byte[])
    record FileAttributes(
        long lastModified,
        long size,
        byte[] hash
    ) {
        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            FileAttributes that = (FileAttributes) o;
            return size == that.size &&
                   lastModified == that.lastModified &&
                   (FileHasher.equals(that.hash(), hash()));
        }

        @Override
        public int hashCode() {
            return Objects.hash(lastModified, size, Arrays.hashCode(hash));
        }
    }

    static class FileWalker implements FileVisitor<Path> {
        private final BiConsumer<Path, BasicFileAttributes> onFile;

        FileWalker(BiConsumer<Path, BasicFileAttributes> onFile) {this.onFile = onFile;}

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            onFile.accept(file, attrs);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            LOGGER.error(exc, "Failed to visit file: {}", file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            if (exc != null) LOGGER.error(exc, "Failed to visit directory: {}", dir);
            return FileVisitResult.CONTINUE;
        }
    }
}
