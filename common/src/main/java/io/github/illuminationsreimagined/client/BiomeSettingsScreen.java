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
import io.github.illuminationsreimagined.config.SpawnRate;
import io.github.illuminationsreimagined.world.BiomeGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import static io.github.illuminationsreimagined.client.ConfigOptions.cycle;
import static io.github.illuminationsreimagined.client.ConfigOptions.key;

/**
 * Per-biome-group spawn rates for fireflies, glowworms and plankton. Firefly colours stay in the JSON file
 * ({@code biomeGroups.<group>.fireflyColor}) because a colour picker is not worth a dependency.
 */
public class BiomeSettingsScreen extends OptionsSubScreen {
    public BiomeSettingsScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options, Component.translatable(key("biomes.title")));
    }

    @Override
    protected void addOptions() {
        IlluminationsConfig config = IlluminationsConfig.get();
        for (BiomeGroup group : BiomeGroup.values()) {
            IlluminationsConfig.BiomeGroupSettings s = config.biomeGroup(group);
            this.list.addHeader(Component.translatable(key("group." + group.key())));
            this.list.addSmall(
                    cycle("biome.fireflies", SpawnRate.class, s.fireflies, v -> s.fireflies = v),
                    cycle("biome.glowworms", SpawnRate.class, s.glowworms, v -> s.glowworms = v));
            this.list.addSmall(
                    cycle("biome.plankton", SpawnRate.class, s.plankton, v -> s.plankton = v));
        }
    }

    @Override
    public void removed() {
        super.removed();
        IlluminationsConfig.get().save();
    }
}
