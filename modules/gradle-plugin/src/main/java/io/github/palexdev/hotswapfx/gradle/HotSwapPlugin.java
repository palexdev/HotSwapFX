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

import io.github.palexdev.hotswapfx.orchestration.HotSwapParticipant;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.compile.JavaCompile;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/// ### _Premise_
/// I'm not super happy with this work. Gradle APIs are great in many ways and garbage in some others. Inherently, I'm
/// not in the mood to document the whole thing in every corner of its complexity. So, what I'm gonna do is document the
/// big picture, how this works, and why in the first place.
///
/// ### _Why_
/// The legacy version of `HotSwapFX` used a polling file system watch service to check for changed files and trigger the
/// reload/swap mechanism. It underwent several refactors but never really got bulletproof.<br >
/// When I saw [compose-hot-reload](https://blog.jetbrains.com/kotlin/2026/01/the-journey-to-compose-hot-reload-1-0-0/),
/// I thought that integrating with the build system directly to detect changes was a great idea, and so I started rewriting
/// the project.
///
/// On the surface, this is a much better and much more stable approach. But little did I know about the dirt under the rug.
///
/// ### **How it works and flow**
///
/// To detect file changes, we use Gradle's continuous builds and incremental tasks features. Unfortunately, continuous
/// builds are blocking, and so is the `run` task of the application plugin. So, the only way to make it work is to
/// start a new Gradle daemon and delegate the continuous build to that ([WatchServiceTask]).<br >
///
/// The continuous build runs the `hotReload` task every time a files changes on the classpath, this depends on `compileJava`.
/// When changes are detected a `ReloadRequest` is sent to the agent server, which in turn will start the hot swap mechanism.<br >
///
/// To make things super easy for the user, the plugin requires zero configuration, unless you want to customize things,
/// of course. So, the plugin registers a new task `hotRun` which is finalized by the application's `run` task. To enable
/// the hot swap mechanism, you must start the application through this new task because it configures the `run` task
/// by adding the Java agent as an argument for you.
///
/// Compose hot reload really likes making love to your disk. I tried so hard, like so very hard to avoid writing garbage
/// to the disk, but unfortunately, I could not find a better solution. Still, I write the bare minimum:
/// - A `.hotswapfx` file containing the timestamp in nanoseconds of the last detected changes. The [HotReloadTask] being
/// incremental (I think) **requires** some kind of output. So, to make Gradle shut up, this is the most significant output
/// I could find, it's still useless garbage though, we don't need it, we don't use it.
/// - A `.hotswapfx-port` file containing the port used by the server to listen to reload requests. Every time a continuous
/// runs, it's like the entire plugin state is lost. There is no obvious way (and maybe not at all) in the Gradle API to
/// persist some state across runs without fucking writing to the disk. I therefore declare defeat to this garbage design.
///
/// ```
///                  │
///                  │
///                  │
///                  │
///        ┌─────────▼──────────┐                              ┌──────────────────────────┐
///        │       hotRun       │  configures & finalized by   │            run           │
///        │ (entry point task) │─────────────────────────────►│ (adds Java agent to app) │
///        │                    │                              │                          │
///        └────────────────────┘                              └──────────────────────────┘
///                  │
///                  │ depends on
///                  │
///                  ▼
/// ┌─────────────────────────────────┐
/// │      hotReloadWatchService      │  ┌─►─┐
/// │ (continuous mode, separate JVM) │──│   │
/// │                                 │  └─◄─┘
/// └─────────────────────────────────┘
///                  │
///                  │ triggers (on changes)
///                  │
///                  ▼
///           ┌─────────────┐
///           │             │
///           │  hotReload  │
///           │             │
///           └─────────────┘
///                  │
///                  │ depends on
///                  │
///                  ▼
///           ┌─────────────┐
///           │             │
///           │ compileJava │
///           │             │
///           └─────────────┘
/// ```
///
/// _It just works_
public class HotSwapPlugin implements Plugin<Project> {

    //================================================================================
    // Properties
    //================================================================================

    public static Context context;

    //================================================================================
    // Methods
    //================================================================================

    @Override
    public void apply(Project project) {
        context = new Context(project);
        project.getExtensions().add("hotSwapFx", settings());

        // Watch service
        project.getTasks().register("hotReloadWatchService", WatchServiceTask.class)
            .configure(t -> t.setGroup("application"));

        // Classpath snapshot task
        project.getTasks().register("hotReload", HotReloadTask.class).configure(t -> {
            t.setGroup("application");
            t.setDescription("Takes a snapshot of the classpath and communicated changes to the hot swap agent");
            t.getClasspath().from(
                project.getTasks().named("compileJava", JavaCompile.class)
                    .flatMap(AbstractCompile::getDestinationDirectory)
            );
            t.getTimestampFile().set(
                project.getLayout().getProjectDirectory().file(".hotswapfx")
            );
            t.dependsOn("compileJava");
        });

        // Hot run task
        project.getTasks().register("hotRun", HotRunTask.class).configure(t -> {
            t.setGroup("application");
            t.setDescription("Runs the application in HotSwap mode");
            t.dependsOn("hotReloadWatchService");

            // Configure run task
            t.doFirst(_ ->
                project.getTasks().named("run", JavaExec.class).configure(rt -> {
                    rt.jvmArgs("-javaagent:%s=port=%d".formatted(
                        context.settings.agentPath(),
                        context.settings.agentPort()
                    ), "-XX:+AllowEnhancedClassRedefinition");
                    rt.doLast(_ -> context.dispose());
                })
            );
            t.finalizedBy("run");
        });
    }

    public static Context context() {
        return context;
    }

    public static Settings settings() {
        return context.settings;
    }

    //================================================================================
    // Inner Classes
    //================================================================================

    public static class Settings {
        private final Project project;
        String agentPath = null;
        int agentPort = -1;
        boolean verbose = false;

        public Settings(Project project) {
            this.project = project;
        }

        public String agentPath() {
            if (agentPath != null) {
                if (!Files.isRegularFile(Path.of(agentPath))) {
                    Utils.LOGGER.warn("Agent path is not a regular file: {}", agentPath);
                }
                return agentPath;
            }

            Utils.LOGGER.debug("Agent path not set, trying to resolve it...");
            var classpath = project.getConfigurations().getByName("runtimeClasspath");
            if (!classpath.isCanBeResolved()) {
                Utils.LOGGER.error(
                    """
                        Cannot resolve agent Jar from runtime classpath config
                        Please set it manually via the 'agentPath' property"""
                );
                return null;
            }

            var artifacts = classpath.getIncoming().artifactView(v -> {
                v.setLenient(true);
                v.attributes(attrs ->
                    attrs.attribute(
                        ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                        ArtifactTypeDefinition.JAR_TYPE
                    ));
            }).getArtifacts();
            return artifacts.getArtifacts().stream()
                .filter(a -> {
                    if (a.getId().getComponentIdentifier() instanceof ModuleComponentIdentifier modId) {
                        return modId.getGroup().equals("io.github.palexdev") &&
                               modId.getModule().equals("hotswapfx-runtime");
                    }
                    return false;
                })
                .map(a -> a.getFile().toPath().toString())
                .findFirst()
                .orElseGet(() -> {
                    Utils.LOGGER.error("Cannot find agent Jar in runtime classpath");
                    return null;
                });
        }

        public int agentPort() {
            Path portFile = project.getLayout().getProjectDirectory().file(".hotswapfx-port").getAsFile().toPath();
            // if set by the user, write for IPC and return
            if (agentPort > 0) {
                writePort(portFile, agentPort);
                return agentPort;
            }

            // if the file exists, it's either a previous run not cleaned or IPC
            if (Files.isRegularFile(portFile)) {
                try {
                    return Integer.parseInt(new String(Files.readAllBytes(portFile)));
                } catch (Exception ex) {
                    Utils.LOGGER.error("Failed to read port file", ex);
                }
            }

            // get an available port, write and return
            try {
                int port = Utils.getAvailableTcpPort(-1);
                writePort(portFile, port);
                return port;
            } catch (Exception ex) {
                Utils.LOGGER.error("Failed to write port file", ex);
            }

            throw new RuntimeException("Failed to find a free port for the HotSwap agent");
        }

        private void writePort(Path portFile, int port) {
            try {
                Files.writeString(portFile, String.valueOf(port), CREATE, TRUNCATE_EXISTING);
            } catch (IOException ex) {
                Utils.LOGGER.error("Failed to write port file", ex);
            }
        }
    }

    public static class Context {
        private final Project project;
        private final Settings settings;
        private HotReloadConnection hotReloadConnection;


        public Context(Project project) {
            this.project = project;
            this.settings = new Settings(project);
        }

        public HotReloadConnection hotReloadConnection() {
            if (hotReloadConnection == null) {
                hotReloadConnection = Utils.projectConnection(project);
            }
            return hotReloadConnection;
        }

        public HotSwapParticipant client() {
            return new HotSwapParticipant(settings.agentPort());
        }

        public void dispose() {
            if (hotReloadConnection != null) hotReloadConnection.close();

            // Clean up files
            var hotswapFile = project.getLayout().getProjectDirectory().file(".hotswapfx").getAsFile().toPath();
            var portFile = project.getLayout().getProjectDirectory().file(".hotswapfx-port").getAsFile().toPath();
            try {
                Files.deleteIfExists(portFile);
                Files.deleteIfExists(hotswapFile);
            } catch (IOException ex) {
                Utils.LOGGER.error("Failed files cleanup", ex);
            }
        }
    }
}
