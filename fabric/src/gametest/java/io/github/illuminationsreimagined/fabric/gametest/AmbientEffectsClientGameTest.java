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
package io.github.illuminationsreimagined.fabric.gametest;

import io.github.illuminationsreimagined.IlluminationsReimagined;
import io.github.illuminationsreimagined.config.IlluminationsConfig;
import io.github.illuminationsreimagined.config.SeasonalMode;
import io.github.illuminationsreimagined.config.SpawnRate;
import io.github.illuminationsreimagined.particle.ParticleKind;
import io.github.illuminationsreimagined.particle.ParticleTracker;
import io.github.illuminationsreimagined.world.BiomeGroup;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;

import java.util.EnumMap;
import java.util.Map;

/**
 * End-to-end check of every ambient effect in a real client: spawning, block-driven effects, day/night fade-out
 * and tracker cleanup after leaving the world.
 */
public class AmbientEffectsClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        // Make every effect frequent so the test is fast and deterministic enough.
        context.runOnClient(client -> {
            IlluminationsConfig config = IlluminationsConfig.get();
            config.density = 400;
            config.spawnRadius = 24;
            config.samplesPerTick = 256;
            config.biomeGroup(BiomeGroup.PLAINS).plankton = SpawnRate.HIGH;
            config.biomeGroup(BiomeGroup.PLAINS).fireflies = SpawnRate.HIGH;
            config.biomeGroup(BiomeGroup.PLAINS).glowworms = SpawnRate.HIGH;
            config.eyesInTheDark.mode = SeasonalMode.ALWAYS;
            config.eyesInTheDark.rate = SpawnRate.HIGH;
        });

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            BlockPos p = context.computeOnClient(client -> client.player.blockPosition());
            int x = p.getX(), y = p.getY(), z = p.getZ();
            IlluminationsReimagined.LOGGER.info("[gametest] player at {}", p);

            world.getServer().runCommand("time set midnight");
            world.getServer().runCommand("gamerule advance_time false");
            world.getServer().runCommand("gamerule advance_weather false");

            // Sealed, unlit stone room for glowworms and eyes (12-22 blocks away, so beyond the eyes' minimum distance).
            fill(world, x + 12, y - 1, z - 6, x + 22, y + 6, z + 6, "stone hollow");
            // Chorus flower on end stone.
            world.getServer().runCommand(String.format("setblock %d %d %d end_stone", x - 6, y - 1, z + 4));
            world.getServer().runCommand(String.format("setblock %d %d %d chorus_flower", x - 6, y, z + 4));
            // Covered (dark) water pool with a sea lantern at one end.
            fill(world, x - 20, y - 1, z - 8, x - 8, y + 6, z + 8, "stone hollow");
            fill(world, x - 19, y, z - 7, x - 9, y + 5, z + 7, "water");
            world.getServer().runCommand(String.format("setblock %d %d %d sea_lantern", x - 10, y + 2, z));
            // Soul lantern.
            world.getServer().runCommand(String.format("setblock %d %d %d soul_lantern", x - 4, y, z - 6));
            // Face the chorus flower / pool.
            world.getServer().runCommand(String.format("tp @p %d %d %d 90 10", x, y, z));

            context.waitTicks(400);
            Map<ParticleKind, Integer> night = counts(context);
            IlluminationsReimagined.LOGGER.info("[gametest] counts after 400 night ticks: {}", night);
            context.takeScreenshot("illuminations_night_west");
            world.getServer().runCommand(String.format("tp @p %d %d %d -90 0", x, y, z));
            context.waitTicks(5);
            context.takeScreenshot("illuminations_night_east");

            require(night.get(ParticleKind.FIREFLY) > 0, "fireflies spawn at night in plains");
            require(night.get(ParticleKind.GLOWWORM) > 0, "glowworms spawn under a ceiling");
            require(night.get(ParticleKind.EYES) > 0, "eyes spawn in total darkness");
            require(night.get(ParticleKind.CHORUS_PETAL) > 0, "chorus flower sheds petals");
            require(night.get(ParticleKind.PRISMARINE_CRYSTAL) > 0, "sea lantern spawns crystals");
            require(night.get(ParticleKind.PLANKTON) > 0, "plankton spawns in dark water");
            for (ParticleKind kind : ParticleKind.values()) {
                int cap = context.computeOnClient(c -> kind.cap(IlluminationsConfig.get()));
                require(night.get(kind) <= cap, kind + " respects its cap (" + night.get(kind) + " <= " + cap + ")");
            }

            // Breaking the chorus flower bursts petals.
            int petalsBefore = context.computeOnClient(c -> ParticleTracker.count(ParticleKind.CHORUS_PETAL));
            world.getServer().runCommand(String.format("setblock %d %d %d air destroy", x - 6, y, z + 4));
            context.waitTicks(3);
            int petalsAfter = context.computeOnClient(c -> ParticleTracker.count(ParticleKind.CHORUS_PETAL));
            IlluminationsReimagined.LOGGER.info("[gametest] petals before/after break: {} / {}", petalsBefore, petalsAfter);
            require(petalsAfter > petalsBefore, "breaking a chorus flower bursts petals");

            // Daylight: fireflies fade out.
            world.getServer().runCommand("time set noon");
            context.waitTicks(200);
            int firefliesByDay = context.computeOnClient(c -> ParticleTracker.count(ParticleKind.FIREFLY));
            IlluminationsReimagined.LOGGER.info("[gametest] fireflies 200 ticks after noon: {}", firefliesByDay);
            require(firefliesByDay == 0, "fireflies disappear during the day");
        }

        // After leaving the world, the tracker must not keep stale particles.
        context.waitTicks(5);
        Map<ParticleKind, Integer> after = counts(context);
        IlluminationsReimagined.LOGGER.info("[gametest] counts after leaving world: {}", after);
        require(after.values().stream().allMatch(c -> c == 0), "tracker is empty after leaving the world");
    }

    private static void fill(TestSingleplayerContext world, int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        world.getServer().runCommand(String.format("fill %d %d %d %d %d %d %s", x1, y1, z1, x2, y2, z2, block));
    }

    private static Map<ParticleKind, Integer> counts(ClientGameTestContext context) {
        return context.computeOnClient(c -> {
            Map<ParticleKind, Integer> map = new EnumMap<>(ParticleKind.class);
            for (ParticleKind kind : ParticleKind.values()) {
                map.put(kind, ParticleTracker.count(kind));
            }
            return map;
        });
    }

    private static void require(boolean condition, String what) {
        if (!condition) {
            throw new AssertionError("Expected: " + what);
        }
        IlluminationsReimagined.LOGGER.info("[gametest] OK: {}", what);
    }
}
