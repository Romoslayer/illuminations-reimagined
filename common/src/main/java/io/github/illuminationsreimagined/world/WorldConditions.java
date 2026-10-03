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
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.time.Month;
import java.util.IdentityHashMap;
import java.util.Map;

/** Shared environmental checks used by spawn rules and particle behaviour. */
public final class WorldConditions {
    private static Level nightLevel;
    private static long nightGameTime;
    private static boolean night;
    /** Biome → group cache. Biome instances are per-registry, so this is cleared whenever the level changes. */
    private static final Map<Biome, BiomeGroup> GROUP_CACHE = new IdentityHashMap<>();

    private WorldConditions() {
    }

    /**
     * Whether it is dark outside. Uses the level's own sky-darkening value, which follows the dimension's clock and
     * environment attributes, instead of the original hard-coded sun-angle range. Fixed-time dimensions such as the
     * Nether and End are never "night".
     */
    /**
     * Night as the original mod defined it: the sun between 0.2597 and 0.7403 of its daily arc (roughly ticks 13008 to
     * 22992). In fixed-time dimensions the original read the fixed sky angle, which made the Nether always night and the
     * End always day. Cached per game tick, as reading the clock is a registry lookup.
     */
    public static boolean isNight(Level level) {
        long gameTime = level.getGameTime();
        if (level != nightLevel || gameTime != nightGameTime) {
            nightLevel = level;
            nightGameTime = gameTime;
            if (level.dimensionType().hasFixedTime()) {
                night = level.dimension() == Level.NETHER;
            } else {
                float angle = skyAngle(level.getOverworldClockTime());
                night = angle >= 0.25965086F && angle <= 0.7403491F;
            }
        }
        return night;
    }

    /** Vanilla's pre-1.21 sun angle for a time of day: 0 at noon, 0.25 at sunset, 0.5 at midnight. */
    private static float skyAngle(long timeOfDay) {
        double day = Mth.frac(timeOfDay / 24000.0 - 0.25);
        double eased = 0.5 - Math.cos(day * Math.PI) / 2.0;
        return (float) (day * 2.0 + eased) / 3.0F;
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
        nightLevel = null;
    }
}
