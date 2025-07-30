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

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import apps.Resources;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.tinylog.Logger;

public class WeatherData {
    private String city;
    private TodayData todayData;
    private Details details;
    private final ObservableList<Forecast> forecasts = FXCollections.observableArrayList();

    public void load() {
        forecasts.clear();
        try {
            Properties props = new Properties();
            props.load(Resources.getStream("weather/data.properties"));

            city = props.getProperty("city");

            // Load summary
            double todayMin = Double.parseDouble(props.getProperty("today.min.temperature"));
            double todayMax = Double.parseDouble(props.getProperty("today.max.temperature"));
            WeatherCondition todayCondition = WeatherCondition.valueOf(props.getProperty("today.condition"));
            todayData = new TodayData(todayMin, todayMax, todayCondition);

            // Load forecasts
            for (int i = 0; i < 24; i++) {
                String base = "forecast." + i;
                try {
                    String hour = props.getProperty(base + ".hour");
                    WeatherCondition condition = WeatherCondition.valueOf(
                        props.getProperty(base + ".condition")
                    );
                    double temperature = Double.parseDouble(
                        props.getProperty(base + ".temperature")
                    );
                    Forecast forecast = new Forecast(hour, condition, temperature);
                    forecasts.add(forecast);
                } catch (Exception ex) {
                    throw new IOException("Failed to read forecast for hour: " + i, ex);
                }
            }

            // Load details
            String sunrise = props.getProperty("sunrise");
            String sunset = props.getProperty("sunset");
            double rainChance = Double.parseDouble(props.getProperty("rain.chance"));
            double pressure = Double.parseDouble(props.getProperty("pressure"));
            double windSpeed = Double.parseDouble(props.getProperty("wind.speed"));
            int uv = Integer.parseInt(props.getProperty("uv.index"));
            double feelsLike = Double.parseDouble(props.getProperty("feels.like"));
            double visibility = Double.parseDouble(props.getProperty("visibility"));
            details = new Details(
                sunrise, sunset, rainChance, pressure,
                windSpeed, uv, feelsLike, visibility
            );
        } catch (IOException ex) {
            Logger.error(ex);
        }
    }

    public String getCity() {
        return Optional.ofNullable(city).orElse("Dummy");
    }

    public TodayData todaySummary() {
        return Optional.ofNullable(todayData)
            .orElse(TodayData.NULL);
    }

    public Details getDetails() {
        return Optional.ofNullable(details)
            .orElse(Details.NULL);
    }

    public ObservableList<Forecast> getForecasts() {
        return FXCollections.unmodifiableObservableList(forecasts);
    }

    //================================================================================
    // Inner Classes
    //================================================================================
    public record Forecast(
        String hour,
        WeatherCondition condition,
        double temperature
    ) {}

    public record TodayData(
        double minTemperature,
        double maxTemperature,
        WeatherCondition condition
    ) {
        private static final TodayData NULL = new TodayData(0.0, 0.0, WeatherCondition.SUN);
    }

    public record Details(
        String sunriseTime,
        String sunsetTime,
        double chanceOfRain,
        double pressure,
        double windSpeed,
        int uvIndex,
        double feelsLikeTemperature,
        double visibility
    ) {
        private static final Details NULL = new Details(
            "", "", 0, 0,
            0, 0, 0, 0
        );
    }

    public enum WeatherCondition {
        LIGHT_RAIN("rain3.png"),
        RAIN("rain.png"),
        HEAVY_RAIN("rain2.png"),

        SNOW("snow.png"),
        SNOW_RAIN("snow-rain.png"),
        SNOW_CLOUD("snow-cloud.png"),

        SUN("sun.png"),
        SUN_CLOUD("sun-cloud.png"),
        SUN_RAIN("sun-rain.png"),

        THUNDERSTORM("thunderstorm.png"),
        THUNDERSTORM_RAIN("thunderstorm-rain.png"),
        ;

        private final String icon;

        WeatherCondition(String icon) {this.icon = icon;}

        public String getIconName() {return icon;}

        public String getIconPath() {return "weather/assets/" + getIconName();}
    }
}
