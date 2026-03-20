package io.github.palexdev.hotswapfx.showcase.weather;

import java.io.IOException;
import java.util.Properties;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.tinylog.Logger;

import static java.util.Optional.ofNullable;

public class WeatherData {
    private String city;
    private TodayData todayData;
    private Details details;
    private final ObservableList<Forecast> forecasts = FXCollections.observableArrayList();

    public void load() {
        forecasts.clear();
        try {
            Properties props = new Properties();
            props.load(WeatherApp.class.getResourceAsStream("data.properties"));

            // Load city
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
            }
        } catch (IOException ex) {
            Logger.error(ex, "Failed to load weather data");
        }
    }

    public String getCity() {
        return ofNullable(city).orElse("Unknown");
    }

    public TodayData todaySummary() {
        return ofNullable(todayData).orElse(TodayData.NULL);
    }

    public Details getDetails() {
        return ofNullable(details).orElse(Details.NULL);
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

        public String getIconPath() {return "assets/" + getIconName();}
    }
}
