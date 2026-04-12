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

package io.github.palexdev.hotswapfx.runtime;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.github.palexdev.hotswapfx.orchestration.message.ReloadRequest;
import org.tinylog.Logger;

/// An alternative file watch service for those who don't want/cannot use the Gradle plugin.
///
/// This system is similar to the one used in older versions of _HotSwapFX._ It's based on polling, periodically scanning
/// a preset of directories to check for changes.
///
/// By default, the observer directories are the one from `System.getProperty("java.class.path")`, can be changed by
/// modifying the [#watchDirs()] set.
///
/// By default, it checks for changes every `1000ms`, this can be changed by setting the system property `hotswapfx.pollRate`.
///
/// Other than the increased number of I/O operations (which are not to be neglected!), the major downside is that
/// a polling system makes it fairly difficult to determine what changes and how (add/remove/modify), so:
/// 1. Change types will always be `null`
/// 2. Checking if a file did change involves reading its full content as a byte array and comparing that from the last
/// snapshot. Before doing such a comparison, which is costly as you might guess, it also checks the file's timestamp and size.
///
/// _**Note:** to enable this service instead of the default one, you must pass the following argument to the agent:
/// `legacyWatchService=true`._
public class LegacyWatchService {
    //================================================================================
    // Singleton
    //================================================================================
    private static final LegacyWatchService instance = new LegacyWatchService();

    public static LegacyWatchService instance() {
        return instance;
    }

    //================================================================================
    // Constructors
    //================================================================================

    private LegacyWatchService() {
        dirs.addAll(classpath());
    }

    //================================================================================
    // Properties
    //================================================================================

    private static Set<Path> classpath;
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(
        Thread.ofVirtual().factory()
    );

    private Future<?> task;
    private final Set<Path> dirs = new HashSet<>();
    private Map<Path, FileInfo> snapshot;
    private Consumer<ReloadRequest> onReloadRequest;

    //================================================================================
    // Methods
    //================================================================================

    public void start() {
        task = SCHEDULER.scheduleWithFixedDelay(
            this::scanCompareReload,
            0,
            Integer.getInteger("hotswapfx.pollRate", 1000),
            TimeUnit.MILLISECONDS
        );
    }

    public void stop() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }
    }

    private void scanCompareReload() {
        // 1. Collect all files
        Map<Path, FileInfo> collected = dirs.stream()
            .map(p -> p.toFile().listFiles())
            .filter(files -> Objects.nonNull(files) && files.length > 0)
            .flatMap(Arrays::stream)
            .filter(File::isFile)
            .collect(Collectors.toMap(
                File::toPath,
                FileInfo::new
            ));
        if (snapshot == null) {
            snapshot = collected;
            return;
        }
        if (!Objects.equals(collected.keySet(), snapshot.keySet()))
            Logger.debug("Collected files {}: ", collected);

        // 2. Compare against last snapshot
        ReloadRequest.Changes changed = new ReloadRequest.Changes();
        for (Map.Entry<Path, FileInfo> entry : collected.entrySet()) {
            Path path = entry.getKey();
            FileInfo info = entry.getValue();
            FileInfo last = snapshot.get(path);
            if (!Objects.equals(info, last)) changed.put(path, null);
        }

        // 3. Issue reload request
        if (!changed.isEmpty() && onReloadRequest != null) {
            Logger.debug("Issuing reload request for: {}", changed);
            onReloadRequest.accept(new ReloadRequest(changed));
        }
        snapshot = collected;
    }

    // package-private this should be set by the agent alone
    void onReloadRequest(Consumer<ReloadRequest> callback) {
        this.onReloadRequest = callback;
    }

    private static Set<Path> classpath() {
        if (classpath == null) {
            Set<Path> tmp = new HashSet<>();
            String[] entries = System.getProperty("java.class.path").split(File.pathSeparator);
            for (String entry : entries) {
                Path path = Path.of(entry);
                if (Files.isDirectory(path)) {
                    try (var stream = Files.walk(path)) {
                        stream.filter(Files::isDirectory).forEach(tmp::add);
                    } catch (IOException ex) {
                        Logger.error(ex, "Failed to walk classpath");
                    }
                }
            }
            classpath = tmp;
            Logger.info("Collected classpath: \n{}", String.join("\n", classpath.stream().map(Path::toString).toArray(String[]::new)));
        }
        return Collections.unmodifiableSet(classpath);
    }

    public Set<Path> watchDirs() {
        return dirs;
    }

    //================================================================================
    // Inner Classes
    //================================================================================
    record FileInfo(long lastModified, long size, byte[] content) {
        public FileInfo(File file) {
            byte[] content = null;
            try {
                content = Files.readAllBytes(file.toPath());
            } catch (IOException ex) {
                Logger.error(ex, "Failed to read file");
            }
            this(file.lastModified(), file.length(), content);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            FileInfo fileInfo = (FileInfo) o;
            return size() == fileInfo.size() && lastModified() == fileInfo.lastModified() && Objects.deepEquals(content(), fileInfo.content());
        }

        @Override
        public int hashCode() {
            return Objects.hash(lastModified(), size(), Arrays.hashCode(content()));
        }
    }
}
