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
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.ref.WeakReference;

/**
 * Spawns the biome-driven ambient effects (fireflies, glowworms, plankton, eyes and Soul Sand Valley wisps) the way the
 * original mod did: on every one of vanilla's random display-tick samples around the player (about 1,300 per tick), it
 * offsets the sampled position by a Gaussian of 50 / 25 / 50 blocks and rolls each effect's chance there.
 *
 * <p>Fixes over the original: positions in unloaded chunks or outside the build height are skipped, every effect has a
 * count cap, biome lookups are cached, and glowworms also appear in modern noise caves (which are filled with plain air
 * rather than cave air).</p>
 */
public final class AmbientSpawner {
    // Spawn chance per sample at MEDIUM and 100% density: the original's per-effect spawn-rate tables.
    private static final float FIREFLY_CHANCE = 0.0001F;
    private static final float GLOWWORM_CHANCE = 0.0002F;
    private static final float PLANKTON_CHANCE = 0.001F;
    private static final float EYES_CHANCE = 0.0001F;
    private static final float WISP_CHANCE = 0.0001F;
    private static final double EYES_VANISH_DISTANCE = EyesParticle.VANISH_DISTANCE;

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
        timedTicks++;
        if (DEBUG_COUNTS && ++debugTimer % 200 == 0) {
            IlluminationsReimagined.LOGGER.info("Live ambient particles: {}", ParticleTracker.describe());
        }
    }

    /** Average time the spawner spent per tick since the last reset, in microseconds (for profiling and tests). */
    public static double averageMicrosPerTick() {
        return timedTicks == 0 ? 0.0 : timedNanos / 1000.0 / timedTicks;
    }

    public static void resetTiming() {
        timedNanos = 0;
        timedTicks = 0;
    }

    /** Called for each of vanilla's random display-tick samples, with the sampled position. */
    public static void onAnimateTick(ClientLevel level, BlockPos sample) {
        IlluminationsConfig config = IlluminationsConfig.get();
        if (!config.enabled || !config.isDimensionEnabled(level)) {
            return;
        }
        long start = System.nanoTime();
        POS.set(Mth.floor(sample.getX() + RANDOM.nextGaussian() * 50.0),
                Mth.floor(sample.getY() + RANDOM.nextGaussian() * 25.0),
                Mth.floor(sample.getZ() + RANDOM.nextGaussian() * 50.0));
        if (POS.getY() >= level.getMinY() && POS.getY() < level.getMaxY() && level.isLoaded(POS)) {
            spawnAt(level, config);
        }
        timedNanos += System.nanoTime() - start;
    }

    private static void spawnAt(ClientLevel level, IlluminationsConfig config) {
        BlockState state = level.getBlockState(POS);
        // Every effect needs air, water or soul sand/soil here; skip the biome lookup for anything else (most samples).
        if (!state.isAir() && !state.getFluidState().is(FluidTags.WATER) && !state.is(BlockTags.SOUL_FIRE_BASE_BLOCKS)) {
            return;
        }
        BiomeGroup group = WorldConditions.biomeGroup(level, POS);
        IlluminationsConfig.BiomeGroupSettings settings = config.biomeGroup(group);
        float density = config.densityFactor();

        if (roll(FIREFLY_CHANCE, settings.fireflies, density) && isFireflySpot(level, config, state)
                && ParticleTracker.hasRoom(ParticleKind.FIREFLY)) {
            ParticleTracker.spawn(FireflyParticle.create(level, POS.getX(), POS.getY(), POS.getZ(), settings.fireflyColorRgb()));
        }
        if (roll(GLOWWORM_CHANCE, settings.glowworms, density) && isCaveAir(level, state)
                && ParticleTracker.hasRoom(ParticleKind.GLOWWORM)) {
            spawnGlowworm(level);
        }
        if (roll(PLANKTON_CHANCE, settings.plankton, density) && state.getFluidState().is(FluidTags.WATER)
                && level.getMaxLocalRawBrightness(POS) < 2 && ParticleTracker.hasRoom(ParticleKind.PLANKTON)) {
            ParticleTracker.spawn(new PlanktonParticle(level, POS.getX(), POS.getY(), POS.getZ()));
        }
        if (group == BiomeGroup.SOUL_SAND_VALLEY && state.is(BlockTags.SOUL_FIRE_BASE_BLOCKS)
                && roll(WISP_CHANCE, config.willOWisps.soulSandValleyRate, density)
                && ParticleTracker.hasRoom(ParticleKind.WILL_O_WISP) && Sprites.isSkinAvailable(Sprites.WISP_SKIN)) {
            ParticleTracker.spawn(new WillOWispParticle(level, POS.getX(), POS.getY(), POS.getZ()));
        }
        // Eyes ignore the density setting, as in the original.
        if (WorldConditions.isHalloween(config.eyesInTheDark.mode) && RANDOM.nextFloat() <= EYES_CHANCE * config.eyesInTheDark.rate.multiplier
                && isEyesSpot(level, state) && ParticleTracker.hasRoom(ParticleKind.EYES)) {
            ParticleTracker.spawn(new EyesParticle(level, POS.getX() + 0.5, POS.getY() + 0.5, POS.getZ() + 0.5));
        }
    }

    private static boolean roll(float baseChance, SpawnRate rate, float density) {
        float chance = baseChance * rate.multiplier;
        return chance > 0.0F && RANDOM.nextFloat() <= chance * density;
    }

    /** Open air at night under the sky; in dimensions with a fixed time, any air. */
    private static boolean isFireflySpot(ClientLevel level, IlluminationsConfig config, BlockState state) {
        if (level.dimensionType().hasFixedTime()) {
            return state.is(Blocks.AIR) || state.is(Blocks.VOID_AIR);
        }
        return state.is(Blocks.AIR)
                && (config.fireflies.spawnAlways || WorldConditions.isNight(level))
                && (config.fireflies.spawnUnderground || level.canSeeSky(POS));
    }

    /** Cave air, or plain air out of sight of the sky (modern noise caves are not filled with cave air). */
    private static boolean isCaveAir(ClientLevel level, BlockState state) {
        return state.is(Blocks.CAVE_AIR) || (state.is(Blocks.AIR) && !level.canSeeSky(POS));
    }

    /** Glowworms climb straight up from the sampled spot to the first block above and hang just under it. */
    private static void spawnGlowworm(ClientLevel level) {
        PROBE.set(POS);
        while (level.getBlockState(PROBE).isAir()) {
            PROBE.move(0, 1, 0);
            if (PROBE.getY() >= level.getMaxY()) {
                return;
            }
        }
        ParticleTracker.spawn(new GlowwormParticle(level, POS.getX(), PROBE.getY() - 0.025, POS.getZ()));
    }

    /** Pitch-dark air in the Overworld, with nobody within the eyes' vanishing distance. */
    private static boolean isEyesSpot(ClientLevel level, BlockState state) {
        return (state.is(Blocks.AIR) || state.is(Blocks.CAVE_AIR))
                && level.dimension() == Level.OVERWORLD
                && level.getMaxLocalRawBrightness(POS) <= 0
                && level.getNearestPlayer(POS.getX(), POS.getY(), POS.getZ(), EYES_VANISH_DISTANCE, false) == null;
    }
}
