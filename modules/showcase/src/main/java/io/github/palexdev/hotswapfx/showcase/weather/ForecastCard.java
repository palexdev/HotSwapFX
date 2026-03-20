package io.github.palexdev.hotswapfx.showcase.weather;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import io.github.palexdev.imcache.transforms.Pad;
import io.github.palexdev.mfxcore.controls.MFXSkinBase;
import io.github.palexdev.mfxcore.controls.MFXStyleable;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.mfxcore.utils.fx.SwingFXUtils;
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
    public Supplier<MFXSkinBase<? extends Node>> defaultSkinFactory() {
        return () -> new ForecastCardSkin(this);
    }

    @Override
    public List<String> defaultStyleClasses() {
        return MFXStyleable.styleClasses("forecast-card");
    }

    static class ForecastCardSkin extends MFXSkinBase<ForecastCard> {
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

            try (InputStream is = WeatherApp.class.getResourceAsStream(forecast.condition().getIconPath())) {
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
