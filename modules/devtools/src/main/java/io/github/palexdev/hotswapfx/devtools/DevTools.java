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

package io.github.palexdev.hotswapfx.devtools;

import java.util.Set;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.tinylog.Logger;

import static io.github.palexdev.mfxcore.observables.When.observe;

public class DevTools extends Application {
    //================================================================================
    // Properties
    //================================================================================

    private static Set<String> args;
    private static Stage window;
    private static int port = -1;

    //================================================================================
    // Methods
    //================================================================================

    static void main(String[] args) {
        DevTools.args = Set.of(args);
        boolean fxAlreadyRunning = false;
        try {
            Platform.runLater(() -> {});
        } catch (Exception ex) {
            fxAlreadyRunning = true;
        }
        if (!fxAlreadyRunning) {
            launch(args);
        } else {
            Platform.startup(() -> {
                window = new Stage();
                start();
            });
        }
    }

    private static void start() {
        if (port() < 0) throw new IllegalStateException("DevTools cannot work without knowing server port");
        Logger.debug("DevTools will use port {} for communication...", port);

        DevToolsView view = new DevToolsView();
        Scene scene = new Scene(view);
        scene.setFill(Color.TRANSPARENT);
        window.setScene(scene);
        window.setTitle("HotSwapFX DevTools");
        window.initStyle(StageStyle.TRANSPARENT);
        window.setAlwaysOnTop(true);
        ThemeEngine.instance().apply();
        window.sizeToScene();
        window.show();

        // close dev tools if it's the only remaining window
        if (autoClose()) {
            observe(() -> {
                ObservableList<Window> all = Window.getWindows();
                if (all.isEmpty() || (all.size() == 1 && all.getFirst() == window))
                    Platform.exit();
            }, Window.getWindows()).listen();
        }
    }

    @Override
    public void start(Stage stage) {
        if (args == null) args = Set.of(getParameters().getRaw().toArray(String[]::new));
        DevTools.window = stage;
        start();
    }

    public static Stage window() {
        return window;
    }

    public static int port() {
        if (port < 0) {
            port = args.stream()
                .filter(a -> a.startsWith("--port"))
                .findFirst()
                .map(a -> {
                    String[] keyVal = a.split("=");
                    if (keyVal.length != 2) {
                        throw new IllegalArgumentException("Invalid port argument: " + a + "\nExpected format: --port=PORT");
                    }
                    return Integer.parseInt(keyVal[1]);
                })
                .orElse(8765);
        }
        return port;
    }

    public static boolean autoClose() {
        return args.stream().anyMatch(a -> a.startsWith("--autoclose"));
    }
}

