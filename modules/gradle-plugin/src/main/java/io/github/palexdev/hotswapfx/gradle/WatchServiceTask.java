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
