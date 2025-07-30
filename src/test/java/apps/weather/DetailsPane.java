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

import java.util.function.Function;

import apps.weather.WeatherData.Details;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.mfxresources.icon.MFXFontIcon;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class DetailsPane extends GridPane {

    public DetailsPane() {
        Label header = new Label("WEATHER'S DETAILS");
        setColumnIndex(header, 0);
        setRowIndex(header, 0);

        Card surCard = createCard("Sunrise", "fas-sun", Details::sunriseTime);
        setColumnIndex(surCard, 0);
        setRowIndex(surCard, 1);

        Card sunCard = createCard("Sunset", "fas-moon", Details::sunsetTime);
        setColumnIndex(sunCard, 1);
        setRowIndex(sunCard, 1);

        Card rainCard = createCard("Change of Rain", "fas-droplet", d -> d.chanceOfRain() + "%");
        setColumnIndex(rainCard, 2);
        setRowIndex(rainCard, 1);

        Card preCard = createCard("Pressure", "fas-compass", d -> d.pressure() + " mb");
        setColumnIndex(preCard, 3);
        setRowIndex(preCard, 1);

        Card wCard = createCard("Wind", "fas-wind", d -> d.windSpeed() + " Km/h");
        setColumnIndex(wCard, 0);
        setRowIndex(wCard, 2);

        Card uvCard = createCard("UV Index", "fas-person-rays", d -> d.uvIndex() + " of 10");
        setColumnIndex(uvCard, 1);
        setRowIndex(uvCard, 2);

        Card flCard = createCard("Feels Like", "fas-temperature-three-quarters", d -> d.feelsLikeTemperature() + "°");
        setColumnIndex(flCard, 2);
        setRowIndex(flCard, 2);

        Card visCard = createCard("Visibility", "fas-eye", d -> d.visibility() + " Km");
        setColumnIndex(visCard, 3);
        setRowIndex(visCard, 2);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            getColumnConstraints().add(cc);
        }

        for (int i = 0; i < 3; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            getRowConstraints().add(rc);
        }

        getStyleClass().add("details");
        getChildren().addAll(header, surCard, sunCard, rainCard, preCard, wCard, uvCard, flCard, visCard);
    }

    private Card createCard(String title, String icon, Function<Details, String> dataExtractor) {
        Details details = WeatherApp.data().getDetails();
        return new Card(title, dataExtractor.apply(details), icon);
    }

    static class Card extends Region {
        private final Label title;
        private final Label value;
        private final MFXFontIcon icon;

        public Card(String title, String value, String icon) {
            this.title = new Label(title);
            this.title.getStyleClass().add("title");

            this.value = new Label(value);
            this.value.getStyleClass().add("value");

            this.icon = new MFXFontIcon(icon);

            getStyleClass().add("card");
            getChildren().addAll(this.title, this.value, this.icon);
        }

        @Override
        protected double computeMinWidth(double height) {
            double labelsMax = Math.max(
                LayoutUtils.snappedBoundWidth(title),
                LayoutUtils.snappedBoundWidth(value)
            );
            return labelsMax + LayoutUtils.snappedBoundWidth(icon) + snappedLeftInset() + snappedRightInset();
        }

        @Override
        protected double computeMinHeight(double width) {
            double labelsSum = LayoutUtils.snappedBoundHeight(title) + LayoutUtils.snappedBoundHeight(value);
            return Math.max(labelsSum, LayoutUtils.snappedBoundHeight(icon)) + snappedTopInset() + snappedRightInset();
        }

        @Override
        protected void layoutChildren() {
            double w = getWidth();
            double h = getHeight();
            Insets padding = getPadding();
            layoutInArea(title, 0, 0, w, h, 0, padding, HPos.LEFT, VPos.TOP);
            layoutInArea(value, 0, 0, w, h, 0, padding, HPos.LEFT, VPos.BOTTOM);
            layoutInArea(icon, 0, 0, w, h, 0, padding, HPos.RIGHT, VPos.CENTER);
        }
    }
}
