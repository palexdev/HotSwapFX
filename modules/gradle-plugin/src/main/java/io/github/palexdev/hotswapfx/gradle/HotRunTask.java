package io.github.palexdev.hotswapfx.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/// Dummy task to execute the `run` task in hot swap mode.
public class HotRunTask extends DefaultTask {

    //================================================================================
    // Methods
    //================================================================================

    @TaskAction
    public void hotRun() {}
}
