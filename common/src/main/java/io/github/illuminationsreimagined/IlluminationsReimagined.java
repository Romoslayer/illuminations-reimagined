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
package io.github.illuminationsreimagined;

import io.github.illuminationsreimagined.config.IlluminationsConfig;
import io.github.illuminationsreimagined.spawn.AmbientSpawner;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loader-independent entry point. The loader modules call {@link #init()} once and {@link #onClientTick} every tick. */
public final class IlluminationsReimagined {
    public static final String MOD_ID = "illuminations_reimagined";
    public static final Logger LOGGER = LoggerFactory.getLogger("Illuminations Reimagined");

    private IlluminationsReimagined() {
    }

    public static void init() {
        IlluminationsConfig.load();
    }

    public static void onClientTick(Minecraft minecraft) {
        AmbientSpawner.tick(minecraft);
    }
}
