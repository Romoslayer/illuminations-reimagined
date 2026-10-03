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
package io.github.illuminationsreimagined.spawn;

import io.github.illuminationsreimagined.IlluminationsReimagined;
import io.github.illuminationsreimagined.config.IlluminationsConfig;
import io.github.illuminationsreimagined.config.SpawnRate;
import io.github.illuminationsreimagined.particle.EyesParticle;
import io.github.illuminationsreimagined.particle.FireflyParticle;
import io.github.illuminationsreimagined.particle.GlowwormParticle;
import io.github.illuminationsreimagined.particle.ParticleKind;
import io.github.illuminationsreimagined.particle.ParticleTracker;
import io.github.illuminationsreimagined.particle.PlanktonParticle;
import io.github.illuminationsreimagined.particle.Sprites;
import io.github.illuminationsreimagined.particle.WillOWispParticle;
import io.github.illuminationsreimagined.world.BiomeGroup;
import io.github.illuminationsreimagined.world.WorldConditions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;

/**
 * Spawns biome-driven ambient effects (fireflies, glowworms, plankton, eyes, Soul Sand Valley wisps).
 *
 * <p>The original injected into vanilla's per-block display tick, which runs about 1,300 times per tick. Every one of
 * those samples did a biome registry lookup, and the Gaussian ±50/±25 offsets often landed in unloaded chunks or
 * outside the build height. This spawner runs once per client tick with a fixed, configurable sample budget,
 * samples a cylinder around the camera, never queries unloaded chunks, caches biome classification, and respects
 * per-effect caps.</p>
 */
public final class AmbientSpawner {
    // Base chance per sample at density 100% and SpawnRate.MEDIUM.
    private static final float FIREFLY_CHANCE = 0.004F;
    private static final float GLOWWORM_CHANCE = 0.03F;
    private static final float PLANKTON_CHANCE = 0.05F;
    private static final float EYES_CHANCE = 0.002F;
    private static final float WISP_CHANCE = 0.004F;
    private static final double EYES_MIN_PLAYER_DISTANCE = 10.0;

    private static final RandomSource RANDOM = RandomSource.create();
    private static final BlockPos.MutableBlockPos POS = new BlockPos.MutableBlockPos();
    private static final BlockPos.MutableBlockPos PROBE = new BlockPos.MutableBlockPos();
    private static WeakReference<ClientLevel> lastLevel = new WeakReference<>(null);
    /** Development aid: -Dilluminations_reimagined.debugCounts=true logs live particle counts every 10 seconds. */
    private static final boolean DEBUG_COUNTS = Boolean.getBoolean("illuminations_reimagined.debugCounts");
    private static int debugTimer;
    private static long timedNanos;
    private static int timedTicks;

    private AmbientSpawner() {
    }

    /** Runs at the end of every client tick. */
    public static void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level != lastLevel.get()) {
            // Joined, left, or changed dimension: drop everything tied to the previous level.
            WorldConditions.clearCaches();
            ParticleTracker.clear();
            lastLevel = new WeakReference<>(level);
        }
        if (level == null || minecraft.isPaused() || !level.tickRateManager().runsNormally()) {
            return;
        }
        // Mirror the particle engine, which only ticks under the same conditions.
        ParticleTracker.tick();
        if (DEBUG_COUNTS && ++debugTimer % 200 == 0) {
            IlluminationsReimagined.LOGGER.info("Live ambient particles: {}", ParticleTracker.describe());
        }

        IlluminationsConfig config = IlluminationsConfig.get();
        if (!config.enabled || config.density <= 0 || minecraft.player == null) {
            return;
        }

        Vec3 center = minecraft.gameRenderer.mainCamera().position();
        int radius = config.spawnRadius;
        int verticalRange = Math.min(radius / 2, 24);
        float density = config.densityFactor();
        boolean night = WorldConditions.isNight(level);
        long start = System.nanoTime();

        for (int i = 0; i < config.samplesPerTick; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double dist = Math.sqrt(RANDOM.nextDouble()) * radius;
            int x = Mth.floor(center.x + Math.cos(angle) * dist);
            int z = Mth.floor(center.z + Math.sin(angle) * dist);
            int y = Mth.floor(center.y) + RANDOM.nextInt(verticalRange * 2 + 1) - verticalRange;
            if (y <= level.getMinY() || y >= level.getMaxY()) {
                continue;
            }
            POS.set(x, y, z);
            if (!level.isLoaded(POS)) {
                continue;
            }
            sample(level, config, density, night, minecraft.player);
        }
        timedNanos += System.nanoTime() - start;
        timedTicks++;
    }

    /** Average time the spawner spent per tick since the last reset, in microseconds (for profiling and tests). */
    public static double averageMicrosPerTick() {
        return timedTicks == 0 ? 0.0 : timedNanos / 1000.0 / timedTicks;
    }

    public static void resetTiming() {
        timedNanos = 0;
        timedTicks = 0;
    }

    private static void sample(ClientLevel level, IlluminationsConfig config, float density, boolean night, Player player) {
        BiomeGroup group = WorldConditions.biomeGroup(level, POS);
        IlluminationsConfig.BiomeGroupSettings settings = config.biomeGroup(group);
        BlockState state = level.getBlockState(POS);

        if (settings.fireflies != SpawnRate.DISABLED && (night || config.fireflies.spawnAlways)
                && roll(FIREFLY_CHANCE, settings.fireflies, density) && ParticleTracker.hasRoom(ParticleKind.FIREFLY)) {
            trySpawnFirefly(level, config, group);
        }

        if (state.isAir()) {
            if (settings.glowworms != SpawnRate.DISABLED && roll(GLOWWORM_CHANCE, settings.glowworms, density)
                    && ParticleTracker.hasRoom(ParticleKind.GLOWWORM)) {
                trySpawnGlowworm(level);
            }
            if (WorldConditions.isHalloween(config.eyesInTheDark.mode) && roll(EYES_CHANCE, config.eyesInTheDark.rate, density)
                    && ParticleTracker.hasRoom(ParticleKind.EYES)) {
                trySpawnEyes(level, player);
            }
        } else if (group == BiomeGroup.SOUL_SAND_VALLEY && state.is(BlockTags.SOUL_FIRE_BASE_BLOCKS)) {
            if (roll(WISP_CHANCE, config.willOWisps.soulSandValleyRate, density) && ParticleTracker.hasRoom(ParticleKind.WILL_O_WISP)
                    && Sprites.isSkinAvailable(Sprites.WISP_SKIN)) {
                trySpawnValleyWisp(level);
            }
        } else if (settings.plankton != SpawnRate.DISABLED && state.getFluidState().is(FluidTags.WATER)
                && roll(PLANKTON_CHANCE, settings.plankton, density) && ParticleTracker.hasRoom(ParticleKind.PLANKTON)) {
            if (level.getMaxLocalRawBrightness(POS) <= 2) {
                ParticleTracker.spawn(new PlanktonParticle(level, POS.getX() + RANDOM.nextDouble(), POS.getY() + RANDOM.nextDouble(), POS.getZ() + RANDOM.nextDouble()));
            }
        }
    }

    private static boolean roll(float baseChance, SpawnRate rate, float density) {
        return RANDOM.nextFloat() < baseChance * rate.multiplier * density;
    }

    /**
     * Fireflies hover 0.5–3.5 blocks above the ground. By default they spawn only on the surface (under open sky,
     * below tree canopies); with {@code spawnUnderground} they may also appear above cave floors.
     */
    private static void trySpawnFirefly(ClientLevel level, IlluminationsConfig config, BiomeGroup sampledGroup) {
        int x = POS.getX();
        int z = POS.getZ();
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        double y;
        BiomeGroup group = sampledGroup;
        if (POS.getY() < surface - 3) {
            if (!config.fireflies.spawnUnderground || !level.getBlockState(POS).isAir() || !hasFloorWithin(level, 4)) {
                return;
            }
            y = POS.getY() + 0.5;
        } else {
            y = surface + 0.5 + RANDOM.nextDouble() * 3.0;
            PROBE.set(x, Mth.floor(y), z);
            if (!level.getBlockState(PROBE).isAir() || !level.canSeeSky(PROBE)) {
                return;
            }
            group = WorldConditions.biomeGroup(level, PROBE);
        }
        IlluminationsConfig.BiomeGroupSettings settings = config.biomeGroup(group);
        if (settings.fireflies == SpawnRate.DISABLED) {
            return;
        }
        ParticleTracker.spawn(FireflyParticle.create(level, x + RANDOM.nextDouble(), y, z + RANDOM.nextDouble(), settings.fireflyColorRgb()));
    }

    /** Glowworms cling to the underside of a solid ceiling, out of view of the sky. */
    private static void trySpawnGlowworm(ClientLevel level) {
        if (level.canSeeSky(POS)) {
            return;
        }
        PROBE.set(POS);
        for (int i = 0; i < 6; i++) {
            PROBE.move(Direction.UP);
            BlockState above = level.getBlockState(PROBE);
            if (above.isAir()) {
                continue;
            }
            if (above.isFaceSturdy(level, PROBE, Direction.DOWN) && above.getFluidState().isEmpty()) {
                ParticleTracker.spawn(new GlowwormParticle(level, POS.getX() + 0.15 + RANDOM.nextDouble() * 0.7, PROBE.getY(), POS.getZ() + 0.15 + RANDOM.nextDouble() * 0.7));
            }
            return;
        }
    }

    /**
     * Eyes appear only in complete darkness, near the floor, in dimensions with a sky (the original produced
     * excessive eyes in the Nether and End), and never close to the player.
     */
    private static void trySpawnEyes(ClientLevel level, Player player) {
        if (!level.dimensionType().hasSkyLight() || level.getMaxLocalRawBrightness(POS) > 0 || !hasFloorWithin(level, 2)) {
            return;
        }
        if (player.distanceToSqr(POS.getX() + 0.5, POS.getY() + 0.5, POS.getZ() + 0.5) < EYES_MIN_PLAYER_DISTANCE * EYES_MIN_PLAYER_DISTANCE) {
            return;
        }
        ParticleTracker.spawn(new EyesParticle(level, POS.getX() + 0.5, POS.getY() + 0.4 + RANDOM.nextDouble() * 0.4, POS.getZ() + 0.5));
    }

    /**
     * Valley wisps are born inside soul sand and soul soil and glide up out of it, giving off soul particles, as in
     * the original mod.
     */
    private static void trySpawnValleyWisp(ClientLevel level) {
        ParticleTracker.spawn(new WillOWispParticle(level, POS.getX() + 0.5, POS.getY() + 0.5, POS.getZ() + 0.5));
    }

    private static boolean hasFloorWithin(ClientLevel level, int depth) {
        PROBE.set(POS);
        for (int i = 0; i < depth; i++) {
            PROBE.move(Direction.DOWN);
            if (level.getBlockState(PROBE).isFaceSturdy(level, PROBE, Direction.UP)) {
                return true;
            }
        }
        return false;
    }
}
