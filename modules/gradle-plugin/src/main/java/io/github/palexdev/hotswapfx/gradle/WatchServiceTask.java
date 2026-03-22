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

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.ResultHandler;

abstract class WatchServiceTask extends DefaultTask {

    //================================================================================
    // Constructors
    //================================================================================

    @Inject
    public WatchServiceTask() {}

    //================================================================================
    // Methods
    //================================================================================

    @SuppressWarnings("resource")
    @TaskAction
    void start() {
        if (HotSwapPlugin.settings().legacyWatchService) return;
        Utils.LOGGER.lifecycle("Starting watch service...");
        var conn = HotSwapPlugin.context().hotReloadConnection();
        Thread.ofVirtual().name("HotReload Watcher Service").start(() -> {
                var launcher = conn.conn().newBuild()
                    .forTasks("hotReload")
                    .withCancellationToken(conn.token())
                    .withArguments("--continuous");
                if (HotSwapPlugin.settings().verbose) {
                    launcher.setStandardOutput(System.out);
                    launcher.setStandardError(System.err);
                }
                launcher.run(new NoOpHandler());
            }
        );
    }

    //================================================================================
    // Inner Classes
    //================================================================================

    //@formatter:off
    private static class NoOpHandler implements ResultHandler<Void> {
        @Override public void onComplete(Void result) {}
        @Override public void onFailure(GradleConnectionException failure) {
            Utils.LOGGER.error("Gradle connection failure", failure);
        }
    }
    //@formatter:on
}
