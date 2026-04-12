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

import io.github.palexdev.hotswapfx.devtools.ThemeEngine.PrefColorScheme;
import io.github.palexdev.hotswapfx.orchestration.HotSwapParticipant;
import io.github.palexdev.hotswapfx.orchestration.message.ProcessPendingReloads;
import io.github.palexdev.hotswapfx.orchestration.message.Message;
import io.github.palexdev.hotswapfx.orchestration.message.ToggleAutoReload;
import io.github.palexdev.mfxcomponents.controls.MFXIconButton;
import io.github.palexdev.mfxcomponents.variants.ButtonVariants;
import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.enums.SelectionMode;
import io.github.palexdev.mfxcore.popups.MFXPopups;
import io.github.palexdev.mfxcore.popups.MFXTooltip;
import io.github.palexdev.mfxcore.popups.menu.MFXCheckMenuItem;
import io.github.palexdev.mfxcore.popups.menu.MFXMenu;
import io.github.palexdev.mfxcore.popups.menu.MenuBuilder;
import io.github.palexdev.mfxcore.selection.SelectionGroup;
import io.github.palexdev.mfxcore.utils.fx.AnchorHandlers;
import io.github.palexdev.mfxcore.utils.fx.PseudoClasses;
import io.github.palexdev.mfxcore.utils.fx.StageUtils;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import static io.github.palexdev.mfxcore.observables.When.observe;
import static io.github.palexdev.mfxcore.popups.menu.MFXCheckMenuItem.checkMenuItem;
import static io.github.palexdev.mfxcore.popups.menu.MFXMenuItem.menuItem;

public class DevToolsView extends StackPane {

    //================================================================================
    // Constructors
    //================================================================================

    {
        getStyleClass().add("devtools-view");
        getChildren().setAll(new Content());
    }

    //================================================================================
    // Inner Classes
    //================================================================================

    static class Content extends VBox {
        {
            // Logo
            ImageView logo = new ImageView(ThemeEngine.DARK_LOGO);
            logo.setMouseTransparent(true);
            StackPane logoW = new StackPane(logo);
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(logoW.widthProperty());
            clip.heightProperty().bind(logoW.heightProperty());
            clip.setArcWidth(24);
            clip.setArcHeight(24);
            logoW.setClip(clip);
            StageUtils.makeDraggable(DevTools.window(), logoW);

            // Reload btns
            var reloadBtn = new MFXIconButton()
                .setShape(ButtonVariants.ShapeVariant.SQUARED)
                .setIcon("fas-arrow-rotate-right");
            reloadBtn.setOnAction(_ -> sendMessage(new ProcessPendingReloads()));
            installTooltip(reloadBtn, "Reload");

            var autoReloadBtn = new MFXIconButton.MFXToggleIconButton()
                .setStyle(ButtonVariants.StyleVariant.TONAL)
                .setShape(ButtonVariants.ShapeVariant.SQUARED)
                .setIcon("fas-arrows-spin");
            autoReloadBtn.setSelected(true);
            autoReloadBtn.onSelectionChanged(v -> sendMessage(new ToggleAutoReload(v)));
            installTooltip(autoReloadBtn, "Enable/Disable Auto-Reload");

            // Settings btn
            var settingsBtn = new MFXIconButton()
                .setShape(ButtonVariants.ShapeVariant.SQUARED)
                .setIcon("fas-gear");
            new SettingsMenu(settingsBtn).install();

            // Close btn
            var closeBtn = new MFXIconButton()
                .setIcon("fas-arrow-right-from-bracket")
                .setShape(ButtonVariants.ShapeVariant.SQUARED);
            closeBtn.setOnAction(_ -> Platform.exit());

            getStyleClass().add("content");
            getChildren().addAll(logoW, reloadBtn, autoReloadBtn, settingsBtn, closeBtn);
        }

        private void sendMessage(Message message) {
            try (HotSwapParticipant client = new HotSwapParticipant(DevTools.port())) {
                client.send(message);
            }
        }

        private void installTooltip(Node owner, String text) {
            Label label = new Label(text);
            MFXTooltip tp = MFXPopups.tooltip(cfg -> cfg
                    .offset(Position.of(-4, 0))
                ).setContent(label)
                .show(owner, AnchorHandlers.Placement.Outside.CENTER_LEFT);
            observe(() -> PseudoClasses.setOn(tp.getRoot(), "dark", ThemeEngine.instance().isDarkMode()), ThemeEngine.instance().colorScheme())
                .executeNow()
                .listen();
        }
    }

    static class SettingsMenu {
        private final Node owner;
        private final MFXMenu menu;

        SettingsMenu(Node owner) {
            this.owner = owner;
            menu = MFXPopups.menu(cfg -> cfg
                    .triggerButton(MouseButton.PRIMARY)
                    .enableKeyTrigger(true)
                    .placement(AnchorHandlers.Placement.Outside.CENTER_RIGHT)
                    .offset(Position.of(8, 0))
                    .styleableParent(owner))
                .addMenuItems(items())
                .setSubMenuFactory(items -> {
                    MFXMenu sub = new MFXMenu(items);
                    sub.configure(cfg -> cfg
                        .placement(AnchorHandlers.Placement.placement(Pos.TOP_RIGHT, AnchorHandlers.Direction.AFTER, AnchorHandlers.Direction.AFTER))
                        .offset(Position.of(4.0, 0))
                    );
                    observe(sub::updateStylesheets, ThemeEngine.instance().theme()).listen();
                    observe(() -> PseudoClasses.setOn(sub.getRoot(), "dark", ThemeEngine.instance().isDarkMode()), ThemeEngine.instance().colorScheme())
                        .executeNow()
                        .listen();
                    return sub;
                }).get();
            observe(menu::updateStylesheets, ThemeEngine.instance().theme()).listen();
            observe(() -> PseudoClasses.setOn(menu.getRoot(), "dark", ThemeEngine.instance().isDarkMode()), ThemeEngine.instance().colorScheme())
                .executeNow()
                .listen();
        }

        private MenuBuilder[] items() {
            SelectionGroup themesGroup = new SelectionGroup(SelectionMode.SINGLE, true);
            SelectionGroup schemesGroup = new SelectionGroup(SelectionMode.SINGLE, true);
            return new MenuBuilder[]{
                checkMenuItem("Always on Top").config(
                    it -> ((MFXCheckMenuItem) it).selectedProperty().bind(DevTools.window().alwaysOnTopProperty())
                ).action(() -> DevTools.window().setAlwaysOnTop(!DevTools.window().isAlwaysOnTop())),
                menuItem("Color Scheme").subItems(
                    checkMenuItem("System").onSelected(() -> ThemeEngine.instance().colorScheme(PrefColorScheme.SYSTEM)).group(schemesGroup),
                    checkMenuItem("Light").onSelected(() -> ThemeEngine.instance().colorScheme(PrefColorScheme.LIGHT)).group(schemesGroup),
                    checkMenuItem("Dark").onSelected(() -> ThemeEngine.instance().colorScheme(PrefColorScheme.DARK)).group(schemesGroup)
                ),
                menuItem("Themes").subItems(
                    ThemeEngine.instance().themes().stream()
                        .map(t -> checkMenuItem(t.toString())
                            .group(themesGroup)
                            .onSelectionChanged(_ -> ThemeEngine.instance().theme(t))
                        )
                        .toArray(MenuBuilder[]::new)
                )
            };
        }

        public void install() {
            menu.install(owner);
        }
    }
}
