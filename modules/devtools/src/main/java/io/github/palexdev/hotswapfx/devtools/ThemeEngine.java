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

package io.github.palexdev.hotswapfx.devtools;

import java.util.List;

import io.github.palexdev.mfxcore.utils.StringUtils;
import io.github.palexdev.mfxcore.utils.fx.PseudoClasses;
import io.github.palexdev.mfxresources.MFXResources;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.image.Image;

import static io.github.palexdev.mfxcore.observables.When.observe;

public class ThemeEngine {
    //================================================================================
    // Singleton
    //================================================================================
    private static final ThemeEngine INSTANCE = new ThemeEngine();

    public static ThemeEngine instance() {
        return INSTANCE;
    }

    //================================================================================
    // Properties
    //================================================================================

    public static final Image DARK_LOGO = new Image(DevTools.class.getResourceAsStream("icon-darkbg.png"), 40, 40, true, true);

    private final ObjectProperty<ThemeColor> theme = new SimpleObjectProperty<>(ThemeColor.BLUE);
    private final ObjectProperty<PrefColorScheme> colorScheme = new SimpleObjectProperty<>(PrefColorScheme.SYSTEM) {
        @Override
        public void set(PrefColorScheme newValue) {
            super.set(newValue != null ? newValue : PrefColorScheme.SYSTEM);
        }

        @Override
        public PrefColorScheme get() {
            if (super.get() == PrefColorScheme.SYSTEM) {
                return PrefColorScheme.valueOf(Platform.getPreferences().getColorScheme().name());
            }
            return super.get();
        }
    };
    private final List<ThemeColor> themes = List.of(ThemeColor.values());

    //================================================================================
    // Constructors
    //================================================================================

    private ThemeEngine() {
        observe(
            this::apply,
            theme, colorScheme,
            Platform.getPreferences(),
            DevTools.window().sceneProperty().flatMap(Scene::rootProperty)
        ).listen();
    }

    //================================================================================
    // Methods
    //================================================================================

    public void apply() {
        Scene scene = DevTools.window().getScene();
        PseudoClasses.setOn(scene.getRoot(), "dark", isDarkMode());
        scene.getStylesheets().setAll(getStylesheets());
    }

    private String[] getStylesheets() {
        return new String[]{
            MFXResources.load("fonts/Fonts.css"),
            MFXResources.load("sass/themes/material/md-theme.css"),
            MFXResources.load("sass/themes/material/motion/md-motion.css"),
            theme.get().load(),
            DevTools.class.getResource("DevTools.css").toString()
        };
    }

    public ObjectProperty<ThemeColor> theme() {
        return theme;
    }

    public void theme(ThemeColor theme) {
        this.theme.set(theme);
    }

    public void colorScheme(PrefColorScheme colorScheme) {
        this.colorScheme.set(colorScheme);
    }

    public ObjectProperty<PrefColorScheme> colorScheme() {
        return colorScheme;
    }

    public boolean isDarkMode() {
        return colorScheme.get() == PrefColorScheme.DARK;
    }

    public List<ThemeColor> themes() {
        return themes;
    }

    //================================================================================
    // Inner Classes
    //================================================================================

    public enum PrefColorScheme {
        SYSTEM,
        LIGHT,
        DARK
    }

    // FIXME for now these are hard coded because MaterialFX still lacks theme APIs
    public enum ThemeColor {
        BLUE("#2196F3"),
        DEEP_ORANGE("#FF5722"),
        GREEN("#4CAF50"),
        INDIGO("#3F51B5"),
        ORANGE("#FF9800"),
        PINK("#E91E63"),
        PURPLE("#6750A4"),
        TEAL("#009688"),
        YELLOW("#FFEB3B");

        final String sourceColor;

        ThemeColor(String sourceColor) {this.sourceColor = sourceColor;}

        public String load() {
            return MFXResources.loadTheme("material/md-preset-" + name().replace("_", "-").toLowerCase() + ".css");
        }

        @Override
        public String toString() {
            return StringUtils.titleCaseWord(name().replace("_", " ").toLowerCase());
        }
    }
}
