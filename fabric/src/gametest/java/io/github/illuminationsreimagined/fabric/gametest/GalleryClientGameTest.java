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
import io.github.illuminationsreimagined.config.SpawnRate;
import io.github.illuminationsreimagined.particle.ParticleKind;
import io.github.illuminationsreimagined.particle.ParticleTracker;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.List;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Opt-in gallery capture (run with {@code -Pgallery}): visits real, generated terrain at night with default settings
 * and saves 1920x1080 screenshots of each effect, HUD hidden, to {@code fabric/build/gallery}.
 */
public class GalleryClientGameTest implements FabricClientGameTest {
    private static final int SETTLE_TICKS = 900;

    /** A camera position plus the point it looks at. */
    private record Shot(BlockPos eye, BlockPos lookAt) {
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("illuminations_reimagined.gallery")) {
            return;
        }
        Path out = Path.of(System.getProperty("illuminations_reimagined.galleryDir", "gallery"));
        context.runOnClient(client -> {
            IlluminationsConfig.resetToDefaults();
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
                    "gamerule advance_weather false", "weather clear"}) {
                world.getServer().runCommand(command);
            }

            // Year-round green fireflies (the default turns them orange in October).
            shoot(context, world, out, "fireflies_swamp", Level.OVERWORLD, ParticleKind.FIREFLY, 5, c -> c.fireflies.autumnColors = SeasonalMode.DISABLED,
                    level -> surfaceShot(level, Biomes.SWAMP, BlockPos.ZERO));
            shoot(context, world, out, "fireflies_autumn_forest", Level.OVERWORLD, ParticleKind.FIREFLY, 5, c -> c.fireflies.autumnColors = SeasonalMode.ALWAYS,
                    level -> surfaceShot(level, Biomes.FOREST, BlockPos.ZERO));
            shoot(context, world, out, "glowworms_lush_caves", Level.OVERWORLD, ParticleKind.GLOWWORM, 4, c -> {
            }, level -> caveShot(level, Biomes.LUSH_CAVES, false));
            shoot(context, world, out, "eyes_in_the_dark", Level.OVERWORLD, ParticleKind.EYES, 7, c -> {
                c.eyesInTheDark.mode = SeasonalMode.ALWAYS;
                c.eyesInTheDark.rate = SpawnRate.HIGH;
            }, level -> caveShot(level, Biomes.DRIPSTONE_CAVES, true));
            shoot(context, world, out, "plankton_deep_ocean", Level.OVERWORLD, ParticleKind.PLANKTON, 3, c -> {
            }, level -> underwaterShot(level, false));
            shoot(context, world, out, "prismarine_crystals", Level.OVERWORLD, ParticleKind.PRISMARINE_CRYSTAL, 4, c -> {
            }, level -> underwaterShot(level, true));
            shoot(context, world, out, "chorus_petals_end", Level.END, ParticleKind.CHORUS_PETAL, 5, c -> {
            }, GalleryClientGameTest::chorusShot);
            shoot(context, world, out, "will_o_wisps_soul_sand_valley", Level.NETHER, ParticleKind.WILL_O_WISP, 4, c -> {
            }, level -> netherShot(level));
        }
    }

    private interface ShotFinder {
        Shot find(ServerLevel level);
    }

    private static void shoot(ClientGameTestContext context, TestSingleplayerContext world, Path out, String name,
                              ResourceKey<Level> dimension, ParticleKind focus, int distance,
                              Consumer<IlluminationsConfig> settings, ShotFinder finder) {
        String only = System.getProperty("illuminations_reimagined.galleryShots", "");
        if (!only.isEmpty() && !java.util.List.of(only.split(",")).contains(name)) {
            return;
        }
        context.runOnClient(client -> {
            IlluminationsConfig.resetToDefaults();
            settings.accept(IlluminationsConfig.get());
        });
        Shot shot = world.getServer().computeOnServer(server -> finder.find(server.getLevel(dimension)));
        if (shot == null) {
            IlluminationsReimagined.LOGGER.info("[gallery] {}: no suitable spot found, skipped", name);
            return;
        }
        world.getServer().runCommand(String.format("execute in %s run tp @a %.1f %.1f %.1f facing %d %d %d", dimension.identifier(),
                shot.eye().getX() + 0.5, shot.eye().getY() + 0.1, shot.eye().getZ() + 0.5, shot.lookAt().getX(), shot.lookAt().getY(), shot.lookAt().getZ()));
        world.getServer().runCommand("time set midnight");
        context.waitTicks(20);
        world.getConnection().waitForChunksRender();
        context.waitTicks(SETTLE_TICKS);
        // Move in close to the densest cluster of the effect being shown, so it is actually visible.
        Vec3 target = context.computeOnClient(client -> densest(ParticleTracker.positions(focus)));
        if (target != null) {
            Vec3 eye = context.computeOnClient(client -> cameraSpot(client.level, target, distance));
            world.getServer().runCommand(String.format(java.util.Locale.ROOT, "execute in %s run tp @a %.2f %.2f %.2f facing %.2f %.2f %.2f",
                    dimension.identifier(), eye.x, eye.y - 1.62, eye.z, target.x, target.y, target.z));
            context.waitTicks(60);
        }
        String counts = context.computeOnClient(client -> ParticleTracker.describe());
        IlluminationsReimagined.LOGGER.info("[gallery] {} at {}: {}", name, shot.eye().toShortString(), counts);
        context.takeScreenshot(TestScreenshotOptions.of(name).withSize(1920, 1080).disableCounterPrefix().withDestinationDir(out));
    }

    /** The particle with the most neighbours within 4 blocks. */
    private static Vec3 densest(List<Vec3> points) {
        Vec3 best = null;
        int bestCount = -1;
        for (Vec3 p : points) {
            int count = 0;
            for (Vec3 q : points) {
                if (p.distanceToSqr(q) < 16.0) {
                    count++;
                }
            }
            if (count > bestCount) {
                bestCount = count;
                best = p;
            }
        }
        return best;
    }

    /** A camera position {@code distance} blocks from the target, preferring open air (or water) with a clear view. */
    private static Vec3 cameraSpot(ClientLevel level, Vec3 target, int distance) {
        for (int d : new int[]{distance, distance - 1, distance + 1, distance + 2, distance - 2}) {
            if (d < 2) {
                continue;
            }
            Vec3 eye = cameraSpotAt(level, target, d);
            if (eye != null) {
                return eye;
            }
        }
        return target.add(distance, 0.6, 0);
    }

    private static Vec3 cameraSpotAt(ClientLevel level, Vec3 target, int distance) {
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI / 8.0;
            Vec3 eye = target.add(Math.cos(angle) * distance, 0.6, Math.sin(angle) * distance);
            boolean open = isClearPocket(level, BlockPos.containing(eye));
            BlockHitResult hit = level.clip(new ClipContext(eye, target, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()));
            if (open && hit.getType() == HitResult.Type.MISS) {
                return eye;
            }
        }
        return null;
    }

    /** The camera block and all 26 neighbours are plain air or plain water (no kelp, leaves, moss or walls). */
    private static boolean isClearPocket(ClientLevel level, BlockPos center) {
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            BlockState state = level.getBlockState(p);
            if (!state.isAir() && !state.is(Blocks.WATER)) {
                return false;
            }
        }
        return true;
    }

    private static BlockPos locate(ServerLevel level, ResourceKey<Biome> biome, BlockPos from) {
        Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(h -> h.is(biome), from, 6400, 32, 64);
        if (found == null) {
            return null;
        }
        BlockPos pos = found.getFirst();
        // Generate the surrounding chunks first; heightmaps of unloaded chunks report the world floor.
        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) {
                level.getChunk((pos.getX() >> 4) + cx, (pos.getZ() >> 4) + cz);
            }
        }
        return pos;
    }

    /** Eye height above open ground, looking level across the biome. */
    private static Shot surfaceShot(ServerLevel level, ResourceKey<Biome> biome, BlockPos from) {
        BlockPos pos = locate(level, biome, from);
        if (pos == null) {
            return null;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) + 1;
        return new Shot(new BlockPos(pos.getX(), y, pos.getZ()), new BlockPos(pos.getX() + 20, y, pos.getZ()));
    }

    /** Somewhere inside a cave: open air with a ceiling, a floor, and (optionally) total darkness. */
    private static Shot caveShot(ServerLevel level, ResourceKey<Biome> biome, boolean dark) {
        BlockPos center = locate(level, biome, BlockPos.ZERO);
        if (center == null) {
            return null;
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int r = 0; r <= 24; r += 2) {
            for (int dx = -r; dx <= r; dx += 2) {
                for (int dz = -r; dz <= r; dz += 2) {
                    for (int dy = -12; dy <= 12; dy += 2) {
                        p.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                        if (isCaveSpot(level, p, dark)) {
                            BlockPos eye = p.immutable();
                            return new Shot(eye, eye.offset(12, dark ? 0 : 4, 0));
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isCaveSpot(ServerLevel level, BlockPos pos, boolean dark) {
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir() || level.canSeeSky(pos)) {
            return false;
        }
        if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            return false;
        }
        // A roomy chamber: open air for 6 blocks in at least one horizontal direction.
        boolean roomy = false;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            boolean clear = true;
            for (int i = 1; i <= 6 && clear; i++) {
                clear = level.getBlockState(pos.relative(d, i)).isAir();
            }
            roomy |= clear;
        }
        if (!roomy) {
            return false;
        }
        return !dark || level.getMaxLocalRawBrightness(pos) == 0;
    }

    /** Mid-water in a deep ocean, optionally with sea lanterns placed on the sea floor in view. */
    private static Shot underwaterShot(ServerLevel level, boolean withSeaLanterns) {
        BlockPos pos = locate(level, Biomes.DEEP_OCEAN, new BlockPos(withSeaLanterns ? 400 : 0, 50, 0));
        if (pos == null) {
            return null;
        }
        int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR, pos.getX(), pos.getZ());
        int eyeY = Math.min(floor + 8, level.getSeaLevel() - 6);
        BlockPos eye = new BlockPos(pos.getX(), eyeY, pos.getZ());
        BlockPos look = eye.offset(10, -3, 0);
        if (withSeaLanterns) {
            for (int i = 0; i < 5; i++) {
                int x = eye.getX() + 7 + i * 2;
                int z = eye.getZ() + (i % 2 == 0 ? -2 : 2);
                level.setBlockAndUpdate(new BlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z), z), Blocks.SEA_LANTERN.defaultBlockState());
            }
        }
        return new Shot(eye, look);
    }

    /** Near a chorus flower on the outer End islands, looking at it. */
    private static Shot chorusShot(ServerLevel level) {
        BlockPos center = locate(level, Biomes.END_HIGHLANDS, new BlockPos(1200, 64, 0));
        if (center == null) {
            return null;
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -96; dx <= 96; dx++) {
            for (int dz = -96; dz <= 96; dz++) {
                level.getChunk((center.getX() + dx) >> 4, (center.getZ() + dz) >> 4);
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, center.getX() + dx, center.getZ() + dz);
                for (int y = top; y > top - 24; y--) {
                    p.set(center.getX() + dx, y, center.getZ() + dz);
                    if (level.getBlockState(p).is(Blocks.CHORUS_FLOWER)) {
                        BlockPos flower = p.immutable();
                        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, flower.getX() - 7, flower.getZ() - 4);
                        return new Shot(new BlockPos(flower.getX() - 7, Math.max(groundY + 1, flower.getY() - 2), flower.getZ() - 4), flower);
                    }
                }
            }
        }
        // No flower nearby: grow a small chorus plant on the island surface (the real block, so the petals are genuine).
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, center.getX(), center.getZ());
        BlockPos base = new BlockPos(center.getX(), y, center.getZ());
        level.setBlockAndUpdate(base.below(), Blocks.END_STONE.defaultBlockState());
        level.setBlockAndUpdate(base, Blocks.CHORUS_PLANT.defaultBlockState());
        level.setBlockAndUpdate(base.above(), Blocks.CHORUS_PLANT.defaultBlockState());
        level.setBlockAndUpdate(base.above(2), Blocks.CHORUS_FLOWER.defaultBlockState());
        BlockPos flower = base.above(2);
        return new Shot(new BlockPos(flower.getX() - 7, y + 1, flower.getZ() - 4), flower);
    }

    /** On a Soul Sand Valley floor, looking across the valley. */
    private static Shot netherShot(ServerLevel level) {
        BlockPos pos = locate(level, Biomes.SOUL_SAND_VALLEY, BlockPos.ZERO);
        if (pos == null) {
            return null;
        }
        for (int y = 32; y < 110; y++) {
            BlockPos p = new BlockPos(pos.getX(), y, pos.getZ());
            if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir() && !level.getBlockState(p.below()).isAir()) {
                BlockPos eye = p.above();
                return new Shot(eye, eye.offset(16, -1, 0));
            }
        }
        return null;
    }
}
