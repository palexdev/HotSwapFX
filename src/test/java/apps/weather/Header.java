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
import java.io.InputStream;
import java.util.Objects;

import javax.imageio.ImageIO;

import apps.Resources;
import apps.weather.WeatherData.WeatherCondition;
import io.github.palexdev.imcache.transforms.Pad;
import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.builders.bindings.DoubleBindingBuilder;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.fx.StageUtils;
import io.github.palexdev.mfxcore.utils.fx.SwingFXUtils;
import io.github.palexdev.mfxresources.icon.MFXFontIcon;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.tinylog.Logger;

public class Header extends GridPane {
    private Size stageSizes = Size.zero();

    public Header(Stage stage) {
        // First Row
        Label title = new Label("Weather App");
        title.setGraphic(loadLogo());
        title.setTranslateX(-16.0);
        setColumnIndex(title, 0);

        Region separator = new Region();
        setColumnIndex(separator, 1);

        MFXFontIcon minIcon = new MFXFontIcon("fas-square-minus");
        minIcon.setOnMouseClicked(_ -> stage.setIconified(true));
        minIcon.getStyleClass().add("min");

        MFXFontIcon maxIcon = new MFXFontIcon("fas-square-plus");
        maxIcon.setOnMouseClicked(_ -> maximize(stage));
        maxIcon.getStyleClass().add("max");

        MFXFontIcon exitIcon = new MFXFontIcon("fas-square-xmark");
        exitIcon.setOnMouseClicked(_ -> stage.close());
        exitIcon.getStyleClass().add("exit");

        HBox semaphore = new HBox(minIcon, maxIcon, exitIcon);
        semaphore.getStyleClass().add("semaphore");
        setColumnIndex(semaphore, 2);

        // Second Row
        Label city = new Label(WeatherApp.data().getCity());
        city.getStyleClass().add("city");
        setColumnIndex(city, 0);
        setRowIndex(city, 1);

        Label min = new Label(WeatherApp.data().todaySummary().minTemperature() + "°");
        min.getStyleClass().add("min");

        Label max = new Label(WeatherApp.data().todaySummary().maxTemperature() + "°");
        max.getStyleClass().add("max");

        HBox minMax = new HBox(max, min);
        minMax.setMaxWidth(USE_PREF_SIZE);
        minMax.getStyleClass().add("minMax");
        setColumnIndex(minMax, 0);
        setRowIndex(minMax, 2);

        ImageView weatherIcon = loadWeatherIcon();
        StackPane wrapper = new StackPane(weatherIcon);
        wrapper.getStyleClass().add("wrapper-region");
        wrapper.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        setColumnIndex(wrapper, 0);
        setRowIndex(wrapper, 1);
        setRowSpan(wrapper, REMAINING);

        wrapper.translateXProperty().bind(DoubleBindingBuilder.build()
            .setMapper(() -> Math.max(
                city.getLayoutBounds().getMaxX(),
                minMax.getLayoutBounds().getMaxX()) + getHgap()
            )
            .addSources(city.layoutBoundsProperty())
            .addSources(minMax.layoutBoundsProperty())
            .addSources(wrapper.layoutBoundsProperty())
            .addSources(hgapProperty())
            .get()
        );

        When.onInvalidated(stage.sceneProperty().flatMap(Scene::rootProperty))
            .condition(Objects::nonNull)
            .then(r -> {
                StageUtils.makeDraggable(stage, separator);
                StageUtils.makeResizable(stage, (Region) r);
            })
            .oneShot()
            .listen();

        for (int i = 0; i < 3; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(i == 1 ? Priority.ALWAYS : Priority.SOMETIMES);
            getColumnConstraints().add(cc);
        }

        getStyleClass().add("header");
        getChildren().addAll(title, separator, semaphore, city, minMax, wrapper);
    }

    private ImageView loadLogo() {
        return new ImageView(loadImage("weather/assets/logo.png"));
    }

    private ImageView loadWeatherIcon() {
        WeatherCondition condition = WeatherApp.data().todaySummary().condition();
        return new ImageView(loadImage(condition.getIconPath()));
    }

    private Image loadImage(String name) {
        // Pad at 256px and downscale later in CSS for better image quality
        try (InputStream is = Resources.getStream(name)) {
            BufferedImage bimg = ImageIO.read(is);
            bimg = new Pad(256.0, 256.0, new Color(0, 0, 0, 0)).transform(bimg);
            return SwingFXUtils.toFXImage(bimg, null);
        } catch (Exception ex) {
            Logger.error(ex, "Failed to load logo!");
            return null;
        }
    }

    private void maximize(Stage stage) {
        if (stage.isMaximized()) {
            stage.setMaximized(false);
            stage.setWidth(stageSizes.width());
            stage.setHeight(stageSizes.height());
            stage.centerOnScreen();
        } else {
            stageSizes = new Size(stage.getWidth(), stage.getHeight());
            stage.setMaximized(true);
        }
    }
}
