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
