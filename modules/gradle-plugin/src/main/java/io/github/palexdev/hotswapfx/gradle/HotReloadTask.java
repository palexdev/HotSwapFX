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
