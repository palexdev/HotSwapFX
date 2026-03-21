package io.github.palexdev.hotswapfx.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

/// Dummy task to execute the `run` task in hot swap mode.
@DisableCachingByDefault
public class HotRunTask extends DefaultTask {

    //================================================================================
    // Methods
    //================================================================================

    @TaskAction
    public void hotRun() {}
}
