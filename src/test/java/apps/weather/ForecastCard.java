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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import apps.Resources;
import io.github.palexdev.imcache.transforms.Pad;
import io.github.palexdev.mfxcore.controls.MFXStyleable;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.mfxcore.utils.fx.SwingFXUtils;
import io.github.palexdev.virtualizedfx.cells.CellBaseBehavior;
import io.github.palexdev.virtualizedfx.cells.VFXCellBase;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.tinylog.Logger;

public class ForecastCard extends VFXCellBase<WeatherData.Forecast> {

    public ForecastCard(WeatherData.Forecast item) {
        super(item);
    }

    @Override
    public Supplier<SkinBase<?, ?>> defaultSkinProvider() {
        return () -> new ForecastCardSkin(this);
    }

    @Override
    public List<String> defaultStyleClasses() {
        return MFXStyleable.styleClasses("forecast-card");
    }

    static class ForecastCardSkin extends SkinBase<ForecastCard, CellBaseBehavior<WeatherData.Forecast>> {
        private final Label hour;
        private final ImageView icon;
        private final Label temperature;

        protected double GAP = 8.0;

        public ForecastCardSkin(ForecastCard card) {
            super(card);

            hour = new Label();
            hour.getStyleClass().add("hour");

            icon = new ImageView();

            temperature = new Label();
            temperature.getStyleClass().add("temperature");

            addListeners();
            getChildren().setAll(hour, icon, temperature);
        }

        protected void addListeners() {
            ForecastCard card = getSkinnable();
            listeners(
                When.onInvalidated(card.itemProperty())
                    .then(_ -> update())
                    .executeNow()
            );
        }

        protected void update() {
            WeatherData.Forecast forecast = getSkinnable().getItem();
            hour.setText(forecast.hour());
            temperature.setText(forecast.temperature() + "°");

            try (InputStream is = Resources.getStream(forecast.condition().getIconPath())) {
                BufferedImage bimg = ImageIO.read(is);
                bimg = new Pad(256.0, 256.0, new Color(0, 0, 0, 0)).transform(bimg);
                Image fxImage = SwingFXUtils.toFXImage(bimg, null);
                icon.setImage(fxImage);
            } catch (IOException ex) {
                Logger.error(ex, "Failed to load weather conditions icon for forecast: " + forecast);
            }
        }

        @Override
        protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
            return getChildren().stream()
                       .mapToDouble(LayoutUtils::snappedBoundWidth)
                       .max()
                       .orElse(0.0) + leftInset + rightInset;
        }

        @Override
        protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
            double totalGap = (getChildren().size() - 1) * GAP;
            return getChildren().stream()
                       .mapToDouble(LayoutUtils::snappedBoundHeight)
                       .sum() + totalGap + topInset + bottomInset;
        }

        @Override
        protected void layoutChildren(double x, double y, double w, double h) {
            double advance = 0;
            for (Node child : getChildren()) {
                layoutInArea(child, x, y + advance, w, h, 0, HPos.CENTER, VPos.TOP);
                advance += child.getLayoutBounds().getHeight() + GAP;
            }
        }
    }
}
