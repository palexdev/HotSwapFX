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

package io.github.palexdev.hotswapfx.gradle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;

import io.github.palexdev.hotswapfx.orchestration.message.ReloadRequest;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;
import org.gradle.internal.FileUtils;
import org.gradle.work.DisableCachingByDefault;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

@DisableCachingByDefault
abstract class HotReloadTask extends DefaultTask {

    //================================================================================
    // Properties
    //================================================================================

    @Incremental
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getClasspath();

    @OutputFile
    public abstract RegularFileProperty getTimestampFile();

    //================================================================================
    // Constructors
    //================================================================================

    @Inject
    public HotReloadTask() {}

    //================================================================================
    // Methods
    //================================================================================

    @TaskAction
    void takeSnapshot(InputChanges changes) {
        ReloadRequest.Changes requestChanges = new ReloadRequest.Changes();
        changes.getFileChanges(getClasspath()).forEach(change -> {
            if (!FileUtils.hasExtension(change.getFile(), ".class")) return;
            ReloadRequest.ChangeType type = switch (change.getChangeType()) {
                case ADDED -> ReloadRequest.ChangeType.ADD;
                case MODIFIED -> ReloadRequest.ChangeType.UPDATE;
                case REMOVED -> ReloadRequest.ChangeType.REMOVE;
            };
            requestChanges.put(change.getFile().toPath(), type);
        });

        if (canSend()) {
            Utils.LOGGER.lifecycle("Sending reload request...");
            try (var client = HotSwapPlugin.context().client()) {
                client.send(new ReloadRequest(requestChanges));
            }
        }
        writeTimestamp();
    }

    private void writeTimestamp() {
        try {
            Files.writeString(
                getTimestampFile().get().getAsFile().toPath(),
                String.valueOf(System.nanoTime()),
                CREATE, TRUNCATE_EXISTING
            );
        } catch (Exception ex) {
            Utils.LOGGER.error("Failed to write timestamp file", ex);
        }
    }

    private boolean canSend() {
        long now = System.nanoTime();
        long last = Long.MAX_VALUE;
        Path file = getTimestampFile().get().getAsFile().toPath();
        if (!Files.isRegularFile(file)) return false;

        try {
            last = Long.parseLong(Files.readString(file));
        } catch (IOException ex) {
            Utils.LOGGER.error("Failed to read timestamp file", ex);
        }
        return now > last;
    }
}
