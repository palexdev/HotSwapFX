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

package io.github.palexdev.hotswapfx.showcase.weather;

import io.github.palexdev.hotswapfx.core.HotSwapStrategy;
import io.github.palexdev.hotswapfx.core.annotations.HotSwappable;
import io.github.palexdev.virtualizedfx.controls.VFXScrollPane;
import io.github.palexdev.virtualizedfx.list.VFXList;
import io.github.palexdev.virtualizedfx.utils.ScrollParams;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

@HotSwappable(dependencies = {Header.class, ForecastCard.class, DetailsPane.class})
public class WeatherView extends VBox implements HotSwapStrategy {

    public WeatherView(Stage stage) {
        Header header = new Header(stage);

        var list = new VFXList<>(
            WeatherApp.data().getForecasts(),
            ForecastCard::new,
            Orientation.HORIZONTAL
        );
        VFXScrollPane vsp = list.makeScrollable();
        vsp.setMainAxis(Orientation.HORIZONTAL);
        ScrollParams.cells(1.25).bind(vsp, Orientation.HORIZONTAL);
        VBox forecast = new VBox(new Label("TODAY'S FORECAST"), vsp);
        forecast.getStyleClass().add("forecast");

        DetailsPane details = new DetailsPane();
        setVgrow(details, Priority.ALWAYS);

        getChildren().addAll(header, forecast, details);
        getStyleClass().add("weather-view");
        getStylesheets().add(WeatherApp.class.getResource("WeatherApp.css").toString());
    }

    @Override
    public Node newInstance() {
        return new WeatherView(((Stage) getScene().getWindow()));
    }
}
