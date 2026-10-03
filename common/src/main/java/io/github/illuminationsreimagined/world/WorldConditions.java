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
package io.github.illuminationsreimagined.world;

import io.github.illuminationsreimagined.config.SeasonalMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.time.Month;
import java.util.IdentityHashMap;
import java.util.Map;

/** Shared environmental checks used by spawn rules and particle behaviour. */
public final class WorldConditions {
    /** Biome → group cache. Biome instances are per-registry, so this is cleared whenever the level changes. */
    private static final Map<Biome, BiomeGroup> GROUP_CACHE = new IdentityHashMap<>();

    private WorldConditions() {
    }

    /**
     * Whether it is dark outside. Uses the level's own sky-darkening value, which follows the dimension's clock and
     * environment attributes, instead of the original hard-coded sun-angle range. Fixed-time dimensions such as the
     * Nether and End are never "night".
     */
    public static boolean isNight(Level level) {
        return level.isDarkOutside();
    }

    public static boolean isAutumn(SeasonalMode mode) {
        return mode.isActive(Month.OCTOBER);
    }

    public static boolean isHalloween(SeasonalMode mode) {
        return mode.isActive(Month.OCTOBER);
    }

    public static BiomeGroup biomeGroup(Level level, BlockPos pos) {
        Holder<Biome> holder = level.getBiome(pos);
        return GROUP_CACHE.computeIfAbsent(holder.value(), b -> BiomeGroup.classify(holder));
    }

    public static void clearCaches() {
        GROUP_CACHE.clear();
    }
}
