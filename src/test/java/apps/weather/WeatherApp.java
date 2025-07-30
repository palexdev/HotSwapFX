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

package apps.weather;

import io.github.palexdev.hotswapfx.ChildrenTracker;
import io.github.palexdev.hotswapfx.HotSwapService;
import io.github.palexdev.hotswapfx.HotSwappable;
import io.github.palexdev.mfxresources.MFXResources;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class WeatherApp extends Application {
    private static final WeatherData data = new WeatherData();

    public static void main(String[] args) {
        System.setProperty(HotSwapService.SYSTEM_FLAG, "true");
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        data.load();
        WeatherView view = new WeatherView(stage);

        HotSwapService.instance().register(view)
            .setInstantiator(_ -> new WeatherView(stage))
            .monitorChildren(ChildrenTracker::new);

        HotSwapService.instance().earlyHook(e -> {
            if (e.path().toString().contains("weather"))
                HotSwapService.instance().reload("weather-view");
        });

        Scene scene = new Scene(view, 800, 600);
        scene.getStylesheets().addAll(
            MFXResources.load("fonts/Fonts.css"),
            MFXResources.load("sass/themes/material/md-preset-blue.css"),
            MFXResources.load("sass/themes/material/md-theme.css")
        );
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.setTitle("Weather App");
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.show();

        HotSwapService.instance().start();
    }

    public static WeatherData data() {
        return data;
    }
}
