package io.github.palexdev.hotswapfx.gradle;

import org.gradle.tooling.CancellationToken;
import org.gradle.tooling.CancellationTokenSource;
import org.gradle.tooling.ProjectConnection;

public record HotReloadConnection(ProjectConnection conn, CancellationTokenSource tokenSource) {

    //================================================================================
    // Methods
    //================================================================================

    public CancellationToken token() {
        return tokenSource.token();
    }

    public void close() {
        tokenSource.cancel();
        conn.close();
    }
}
