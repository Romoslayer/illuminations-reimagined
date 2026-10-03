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

import com.mojang.datafixers.util.Pair;
import io.github.illuminationsreimagined.IlluminationsReimagined;
import io.github.illuminationsreimagined.config.IlluminationsConfig;
import io.github.illuminationsreimagined.particle.ParticleTracker;
import io.github.illuminationsreimagined.spawn.AmbientSpawner;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/**
 * Opt-in survey of real, generated terrain with default settings (run with {@code -Psurvey}). Visits one example of
 * each important biome at midnight and logs live particle counts, spawner cost and a screenshot, for tuning densities.
 * It makes no assertions about exact numbers.
 */
public class TerrainSurveyClientGameTest implements FabricClientGameTest {
    private record Stop(String name, ResourceKey<Level> dimension, ResourceKey<Biome> biome, BlockPos searchFrom, boolean surface) {
    }

    private static final List<Stop> STOPS = List.of(
            new Stop("forest", Level.OVERWORLD, Biomes.FOREST, BlockPos.ZERO, true),
            new Stop("swamp", Level.OVERWORLD, Biomes.SWAMP, BlockPos.ZERO, true),
            new Stop("plains", Level.OVERWORLD, Biomes.PLAINS, BlockPos.ZERO, true),
            new Stop("river", Level.OVERWORLD, Biomes.RIVER, BlockPos.ZERO, true),
            new Stop("lush_caves", Level.OVERWORLD, Biomes.LUSH_CAVES, BlockPos.ZERO, false),
            new Stop("deep_ocean", Level.OVERWORLD, Biomes.DEEP_OCEAN, BlockPos.ZERO, false),
            new Stop("soul_sand_valley", Level.NETHER, Biomes.SOUL_SAND_VALLEY, BlockPos.ZERO, false),
            new Stop("end_highlands", Level.END, Biomes.END_HIGHLANDS, new BlockPos(1200, 64, 0), true));

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("illuminations_reimagined.survey")) {
            return;
        }
        context.runOnClient(client -> IlluminationsConfig.resetToDefaults());

        try (TestSingleplayerContext world = context.worldBuilder()
                .setUseConsistentSettings(false)
                .adjustSettings(s -> {
                    s.setSeed("illuminations");
                    s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                })
                .create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode spectator @a");
            world.getServer().runCommand("difficulty peaceful");
            world.getServer().runCommand("gamerule advance_time false");
            world.getServer().runCommand("gamerule advance_weather false");
            world.getServer().runCommand("weather clear");

            String only = System.getProperty("illuminations_reimagined.surveyStops", "");
            for (Stop stop : STOPS) {
                if (!only.isEmpty() && !List.of(only.split(",")).contains(stop.name())) {
                    continue;
                }
                BlockPos target = world.getServer().computeOnServer(server -> locate(server.getLevel(stop.dimension()), stop));
                if (target == null) {
                    IlluminationsReimagined.LOGGER.info("[survey] {}: biome not found nearby, skipped", stop.name());
                    continue;
                }
                world.getServer().runCommand(String.format("execute in %s run tp @a %d %d %d 0 15",
                        stop.dimension().identifier(), target.getX(), target.getY(), target.getZ()));
                world.getServer().runCommand("time set midnight");
                context.waitTicks(20);
                world.getConnection().waitForChunksRender();
                context.runOnClient(client -> AmbientSpawner.resetTiming());
                context.waitTicks(600);
                String counts = context.computeOnClient(client -> ParticleTracker.describe());
                double micros = context.computeOnClient(client -> AmbientSpawner.averageMicrosPerTick());
                IlluminationsReimagined.LOGGER.info("[survey] {} at {}: {} | spawner {} µs/tick",
                        stop.name(), target.toShortString(), counts, String.format("%.1f", micros));
                context.takeScreenshot("survey_" + stop.name());
            }
        }
    }

    /** Finds the biome and picks a sensible camera position: just above the ground for surface stops. */
    private static BlockPos locate(ServerLevel level, Stop stop) {
        Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(h -> h.is(stop.biome()), stop.searchFrom(), 6400, 32, 64);
        if (found == null) {
            return null;
        }
        BlockPos pos = found.getFirst();
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        if (stop.surface()) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
            return new BlockPos(pos.getX(), y + 2, pos.getZ());
        }
        if (stop.dimension() == Level.NETHER) {
            // Soul Sand Valley floors are low; search upward from the floor for open air.
            for (int y = 32; y < 110; y++) {
                BlockPos p = new BlockPos(pos.getX(), y, pos.getZ());
                if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir() && !level.getBlockState(p.below()).isAir()) {
                    return p.above();
                }
            }
        }
        return pos;
    }
}
