package io.github.palexdev.hotswapfx.gradle;

import java.net.ServerSocket;

import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.tooling.GradleConnector;

public class Utils {

    public static final Logger LOGGER = Logging.getLogger(HotSwapPlugin.class);

    private Utils() {}

    public static HotReloadConnection projectConnection(Project project) {
        var conn = GradleConnector.newConnector().forProjectDirectory(project.getProjectDir()).connect();
        var cToken = GradleConnector.newCancellationTokenSource();
        return new HotReloadConnection(conn, cToken);
    }

    public static int getAvailableTcpPort(int preferredPort) {
        if (preferredPort > 0) {
            try (ServerSocket _ = new ServerSocket(preferredPort)) {
                return preferredPort;
            } catch (Exception ignored) {}
        }

        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception ex) {
            return 8765;
        }
    }
}
