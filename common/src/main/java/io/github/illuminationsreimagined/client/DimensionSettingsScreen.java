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
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.github.illuminationsreimagined.client.ConfigOptions.key;

/**
 * One switch per dimension: effects appear only in dimensions that are on. Lists the vanilla dimensions, every
 * dimension of the world or server the player is in (so modded ones appear), and any dimension already switched off.
 */
public class DimensionSettingsScreen extends OptionsSubScreen {
    public DimensionSettingsScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options, Component.translatable(key("dimensions.title")));
    }

    @Override
    protected void addOptions() {
        IlluminationsConfig config = IlluminationsConfig.get();
        Set<Identifier> dimensions = new LinkedHashSet<>(List.of(
                Level.OVERWORLD.identifier(), Level.NETHER.identifier(), Level.END.identifier()));
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        List<Identifier> others = new ArrayList<>();
        if (connection != null) {
            connection.levels().forEach(level -> others.add(level.identifier()));
        }
        for (String id : config.disabledDimensions) {
            Identifier identifier = Identifier.tryParse(id);
            if (identifier != null) {
                others.add(identifier);
            }
        }
        others.sort(null);
        dimensions.addAll(others);

        this.list.addHeader(Component.translatable(key("dimensions.header")));
        List<OptionInstance<?>> row = new ArrayList<>();
        for (Identifier dimension : dimensions) {
            // The caption is the dimension's ID; it has no translation, so it is shown as written.
            row.add(OptionInstance.createBoolean(dimension.toString(), OptionInstance.noTooltip(),
                    config.isDimensionEnabled(dimension), enabled -> config.setDimensionEnabled(dimension, enabled)));
            if (row.size() == 2) {
                this.list.addSmall(row.toArray(OptionInstance<?>[]::new));
                row.clear();
            }
        }
        if (!row.isEmpty()) {
            this.list.addSmall(row.toArray(OptionInstance<?>[]::new));
        }
    }

    @Override
    public void removed() {
        super.removed();
        IlluminationsConfig.get().save();
    }
}
