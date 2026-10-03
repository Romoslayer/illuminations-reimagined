/*
 * Illuminations Reimagined - an unofficial continuation of Illuminations
 * Copyright (C) 2026 Romoslayer
 * Portions derived from Illuminations, Copyright (C) 2021 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.illuminationsreimagined.client;

import io.github.illuminationsreimagined.config.IlluminationsConfig;
import io.github.illuminationsreimagined.config.SeasonalMode;
import io.github.illuminationsreimagined.config.SpawnRate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import static io.github.illuminationsreimagined.client.ConfigOptions.bool;
import static io.github.illuminationsreimagined.client.ConfigOptions.cycle;
import static io.github.illuminationsreimagined.client.ConfigOptions.key;
import static io.github.illuminationsreimagined.client.ConfigOptions.slider;

/**
 * The in-game settings screen, built from vanilla option widgets so it looks and behaves like Minecraft's own menus.
 * Opened from Mod Menu (Fabric) or the mod list's Config button (NeoForge). Changes apply immediately and are saved to
 * {@code config/illuminations_reimagined.json} when the screen closes.
 */
public class IlluminationsConfigScreen extends OptionsSubScreen {
    public IlluminationsConfigScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options, Component.translatable(key("title")));
    }

    @Override
    protected void addOptions() {
        IlluminationsConfig c = IlluminationsConfig.get();

        this.list.addHeader(Component.translatable(key("section.general")));
        this.list.addSmall(
                bool("enabled", c.enabled, v -> c.enabled = v),
                slider("density", 0, 40, c.density / 10, v -> ConfigOptions.percent(v * 10), v -> c.density = v * 10));
        this.list.addSmall(
                slider("spawnRadius", 16, 96, c.spawnRadius, ConfigOptions::blocks, v -> c.spawnRadius = v),
                slider("samplesPerTick", 8, 256, c.samplesPerTick, ConfigOptions::number, v -> c.samplesPerTick = v));
        this.list.addBig(Button.builder(Component.translatable(key("biomes")),
                b -> this.minecraft.gui.setScreen(new BiomeSettingsScreen(this))).build());

        IlluminationsConfig.Fireflies ff = c.fireflies;
        this.list.addHeader(Component.translatable(key("section.fireflies")));
        this.list.addSmall(
                bool("fireflies.spawnAlways", ff.spawnAlways, v -> ff.spawnAlways = v),
                bool("fireflies.spawnUnderground", ff.spawnUnderground, v -> ff.spawnUnderground = v));
        this.list.addSmall(
                bool("fireflies.lightAttraction", ff.lightAttraction, v -> ff.lightAttraction = v),
                bool("fireflies.rainbow", ff.rainbow, v -> ff.rainbow = v));
        this.list.addSmall(
                cycle("fireflies.autumnColors", SeasonalMode.class, ff.autumnColors, v -> ff.autumnColors = v),
                slider("fireflies.coreBrightness", 0, 100, ff.coreBrightness, ConfigOptions::percent, v -> ff.coreBrightness = v));
        this.list.addSmall(
                slider("maxCount", 0, 500, ff.maxCount, ConfigOptions::number, v -> ff.maxCount = v));

        this.list.addHeader(Component.translatable(key("section.glowwormsPlankton")));
        this.list.addSmall(
                slider("glowworms.maxCount", 0, 500, c.glowworms.maxCount, ConfigOptions::number, v -> c.glowworms.maxCount = v),
                slider("plankton.maxCount", 0, 500, c.plankton.maxCount, ConfigOptions::number, v -> c.plankton.maxCount = v));

        IlluminationsConfig.Eyes eyes = c.eyesInTheDark;
        this.list.addHeader(Component.translatable(key("section.eyes")));
        this.list.addSmall(
                cycle("eyes.mode", SeasonalMode.class, eyes.mode, v -> eyes.mode = v),
                cycle("eyes.rate", SpawnRate.class, eyes.rate, v -> eyes.rate = v));
        this.list.addSmall(
                slider("maxCount", 0, 50, eyes.maxCount, ConfigOptions::number, v -> eyes.maxCount = v));

        IlluminationsConfig.WillOWisps wisps = c.willOWisps;
        this.list.addHeader(Component.translatable(key("section.wisps")));
        this.list.addSmall(
                cycle("wisps.soulSandValleyRate", SpawnRate.class, wisps.soulSandValleyRate, v -> wisps.soulSandValleyRate = v),
                bool("wisps.fromSoulLanterns", wisps.fromSoulLanterns, v -> wisps.fromSoulLanterns = v));
        this.list.addSmall(
                slider("maxCount", 0, 50, wisps.maxCount, ConfigOptions::number, v -> wisps.maxCount = v));

        IlluminationsConfig.ChorusPetals petals = c.chorusPetals;
        this.list.addHeader(Component.translatable(key("section.chorus")));
        this.list.addSmall(
                slider("chorus.multiplier", 0, 10, petals.multiplier, v -> Component.literal("×" + v), v -> petals.multiplier = v),
                bool("chorus.burstOnBreak", petals.burstOnBreak, v -> petals.burstOnBreak = v));
        this.list.addSmall(
                bool("prismarine.enabled", c.prismarineCrystals.enabled, v -> c.prismarineCrystals.enabled = v),
                slider("prismarine.maxCount", 0, 500, c.prismarineCrystals.maxCount, ConfigOptions::number, v -> c.prismarineCrystals.maxCount = v));
    }

    @Override
    protected void addFooter() {
        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(Component.translatable(key("reset")), b -> {
            IlluminationsConfig.resetToDefaults();
            this.minecraft.gui.setScreen(new IlluminationsConfigScreen(this.lastScreen));
        }).width(150).build());
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose()).width(150).build());
    }

    @Override
    public void removed() {
        super.removed();
        IlluminationsConfig.get().sanitize();
        IlluminationsConfig.get().save();
    }
}
