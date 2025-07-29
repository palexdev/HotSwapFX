/*
 * Copyright (C) 2025 Parisi Alessandro - alessandro.parisi406@gmail.com
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

package apps;

import io.github.palexdev.hotswapfx.HotSwapService;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class TestApp extends Application {

    public static void main(String[] args) {
        System.setProperty("HOTSWAPFX", "true");
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        HotSwapService.instance().start();

        View view = new View();
        HotSwapService.instance().register(view);

        stage.setScene(new Scene(view, 400, 400));
        stage.setTitle("HotSwapFX");
        stage.show();
    }

    @Override
    public void stop() {
        HotSwapService.instance().dispose();
    }

    private void addSecondary(View view) {
        View view2 = new View();
        view2.setMaxSize(100.0, 100.0);
        view2.setBackground(Background.fill(Color.RED));
        StackPane.setAlignment(view2, Pos.TOP_LEFT);
        view.getChildren().add(view2);
    }
}
