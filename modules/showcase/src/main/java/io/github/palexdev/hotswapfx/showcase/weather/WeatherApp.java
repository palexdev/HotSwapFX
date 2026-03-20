package io.github.palexdev.hotswapfx.showcase.weather;

import io.github.palexdev.mfxresources.MFXResources;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class WeatherApp extends Application {
    private static final WeatherData data = new WeatherData();

    static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        data.load();
        WeatherView view = new WeatherView(stage);
        Scene scene = new Scene(view);
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
    }

    public static WeatherData data() {
        return data;
    }
}
