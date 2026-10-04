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
import io.github.illuminationsreimagined.config.SeasonalMode;
import io.github.illuminationsreimagined.particle.FireflyParticle;
import io.github.illuminationsreimagined.particle.ParticleTracker;
import io.github.illuminationsreimagined.world.WorldConditions;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Opt-in firefly gallery shots (run with {@code -PfireflyShot}): visits a few overworld biomes at night on the gallery
 * seed, faces the most open view at eye level and fills it with fireflies, then saves a short sequence of 1920x1080
 * screenshots per biome to {@code fabric/build/gallery/fireflies}.
 */
public class FireflyShotClientGameTest implements FabricClientGameTest {
    private static final List<ResourceKey<Biome>> BIOMES = List.of(Biomes.SWAMP, Biomes.FLOWER_FOREST, Biomes.MEADOW,
            Biomes.CHERRY_GROVE, Biomes.BIRCH_FOREST, Biomes.PLAINS);
    private static final int FIREFLIES = 110;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("illuminations_reimagined.fireflyShot")) {
            return;
        }
        Path out = Path.of(System.getProperty("illuminations_reimagined.galleryDir", "gallery")).resolve("fireflies");
        context.runOnClient(client -> {
            IlluminationsConfig.resetToDefaults();
            IlluminationsConfig config = IlluminationsConfig.get();
            HandPlacedOnly.apply(config); // only the fireflies placed below
            config.fireflies.autumnColors = SeasonalMode.DISABLED; // year-round biome colours
            config.fireflies.maxCount = 400;
            if (!client.gui.hud.isHidden()) {
                client.gui.hud.toggle();
            }
        });

        try (TestSingleplayerContext world = context.worldBuilder()
                .setUseConsistentSettings(false)
                .adjustSettings(s -> {
                    s.setSeed("illuminations");
                    s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                })
                .create()) {
            world.getConnection().waitForChunksRender();
            for (String command : new String[]{"gamemode spectator @a", "difficulty peaceful", "gamerule advance_time false",
                    "gamerule advance_weather false", "weather clear", "time set midnight"}) {
                world.getServer().runCommand(command);
            }

            for (ResourceKey<Biome> biome : BIOMES) {
                String name = biome.identifier().getPath();
                BlockPos spot = world.getServer().computeOnServer(server -> locate(server.overworld(), biome));
                if (spot == null) {
                    IlluminationsReimagined.LOGGER.info("[fireflies] {}: biome not found, skipped", name);
                    continue;
                }
                world.getServer().runCommand(String.format("tp @a %d %d %d", spot.getX(), spot.getY(), spot.getZ()));
                context.waitTicks(20);
                world.getConnection().waitForChunksRender();

                float yaw = context.computeOnClient(client -> openestYaw(client.level, eye(spot)));
                world.getServer().runCommand(String.format(Locale.ROOT, "tp @a %.1f %d %.1f %.1f 4", spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, yaw));
                context.waitTicks(10);
                world.getConnection().waitForChunksRender();

                context.runOnClient(client -> spawnInView(client.level, eye(spot), yaw));
                for (int frame = 0; frame < 4; frame++) {
                    context.waitTicks(frame == 0 ? 50 : 25);
                    String counts = context.computeOnClient(client -> ParticleTracker.describe());
                    IlluminationsReimagined.LOGGER.info("[fireflies] {} frame {} at {} yaw {}: {}", name, frame, spot.toShortString(), yaw, counts);
                    context.takeScreenshot(TestScreenshotOptions.of(name + "_" + frame).withSize(1920, 1080)
                            .disableCounterPrefix().withDestinationDir(out));
                }
            }
        }
    }

    private static Vec3 eye(BlockPos feet) {
        return new Vec3(feet.getX() + 0.5, feet.getY() + 1.62, feet.getZ() + 0.5);
    }

    /** A dry surface spot in the biome, with chunks generated around it. */
    private static BlockPos locate(ServerLevel level, ResourceKey<Biome> biome) {
        Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(h -> h.is(biome), new BlockPos(0, 80, 0), 6400, 32, 64);
        if (found == null) {
            return null;
        }
        BlockPos center = found.getFirst();
        for (int cx = -2; cx <= 2; cx++) {
            for (int cz = -2; cz <= 2; cz++) {
                level.getChunk((center.getX() >> 4) + cx, (center.getZ() >> 4) + cz);
            }
        }
        // Prefer standing on land rather than in a pond.
        for (int r = 0; r <= 12; r += 2) {
            for (int dx = -r; dx <= r; dx += 2) {
                for (int dz = -r; dz <= r; dz += 2) {
                    int x = center.getX() + dx;
                    int z = center.getZ() + dz;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos feet = new BlockPos(x, y, z);
                    if (level.getFluidState(feet.below()).isEmpty() && level.getBlockState(feet.above()).isAir()) {
                        return feet;
                    }
                }
            }
        }
        return new BlockPos(center.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.getX(), center.getZ()), center.getZ());
    }

    /** The yaw whose view at eye height is least blocked within 32 blocks (a fan of rays around each heading). */
    private static float openestYaw(ClientLevel level, Vec3 eye) {
        float bestYaw = 0;
        double bestScore = -1;
        for (int i = 0; i < 16; i++) {
            float yaw = i * 22.5F;
            double score = 0;
            for (int spread = -30; spread <= 30; spread += 10) {
                Vec3 dir = direction(yaw + spread);
                Vec3 end = eye.add(dir.scale(32));
                Vec3 hit = level.clip(new ClipContext(eye, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty())).getLocation();
                score += hit.distanceTo(eye);
            }
            if (score > bestScore) {
                bestScore = score;
                bestYaw = yaw;
            }
        }
        return bestYaw;
    }

    private static Vec3 direction(float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
    }

    /** Fireflies scattered over the ground in front of the camera, denser near it, each in its own biome colour. */
    private static void spawnInView(ClientLevel level, Vec3 eye, float yaw) {
        RandomSource r = level.getRandom();
        int placed = 0;
        for (int attempt = 0; attempt < FIREFLIES * 4 && placed < FIREFLIES; attempt++) {
            double distance = 2.5 + Math.pow(r.nextDouble(), 1.6) * 22.0;
            Vec3 dir = direction(yaw + (r.nextFloat() - 0.5F) * 90.0F);
            double x = eye.x + dir.x * distance;
            double z = eye.z + dir.z * distance;
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
            double y = ground + 0.4 + r.nextDouble() * 3.0;
            BlockPos pos = BlockPos.containing(x, y, z);
            if (!level.getBlockState(pos).isAir()) {
                continue;
            }
            int rgb = IlluminationsConfig.get().biomeGroup(WorldConditions.biomeGroup(level, pos)).fireflyColorRgb();
            FireflyParticle firefly = FireflyParticle.create(level, x, y, z, rgb);
            ParticleTracker.spawn(firefly);
            placed++;
        }
    }
}
