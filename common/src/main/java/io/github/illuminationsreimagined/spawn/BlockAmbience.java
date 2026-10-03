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

import io.github.illuminationsreimagined.config.IlluminationsConfig;
import io.github.illuminationsreimagined.particle.ChorusPetalParticle;
import io.github.illuminationsreimagined.particle.ParticleKind;
import io.github.illuminationsreimagined.particle.ParticleTracker;
import io.github.illuminationsreimagined.particle.PrismarineCrystalParticle;
import io.github.illuminationsreimagined.particle.Sprites;
import io.github.illuminationsreimagined.particle.WillOWispParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Effects that emanate from specific blocks: chorus petals, prismarine crystals and soul-lantern wisps.
 *
 * <p>Called from vanilla's random display tick through a single mixin on {@code ClientLevel}. This replaces the
 * original's chain of mixins that inherited from another mixin and overrode its handler, a pattern that silently
 * broke chorus petals on 1.19.2.</p>
 */
public final class BlockAmbience {
    private static final BlockPos.MutableBlockPos PROBE = new BlockPos.MutableBlockPos();

    private BlockAmbience() {
    }

    public static void onAnimateTick(ClientLevel level, BlockPos pos, BlockState state, RandomSource random) {
        IlluminationsConfig config = IlluminationsConfig.get();
        if (!config.enabled) {
            return;
        }
        if (state.is(Blocks.CHORUS_FLOWER)) {
            spawnChorusPetals(level, pos, state, random, config);
        } else if (state.is(Blocks.SEA_LANTERN)) {
            spawnPrismarineCrystals(level, pos, random, config);
        } else if (state.is(Blocks.SOUL_LANTERN)) {
            if (config.willOWisps.fromSoulLanterns && random.nextInt(100) == 0 && ParticleTracker.hasRoom(ParticleKind.WILL_O_WISP)
                    && Sprites.isSkinAvailable(Sprites.WISP_SKIN)) {
                ParticleTracker.spawn(new WillOWispParticle(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            }
        } else if (state.is(Blocks.JACK_O_LANTERN)) {
            HalloweenSpirits.fromJackOLantern(level, pos, random, config);
        } else if (state.is(Blocks.SKELETON_SKULL) || state.is(Blocks.SKELETON_WALL_SKULL)) {
            HalloweenSpirits.fromSkull(level, pos, random, config);
        }
    }

    /** Petal burst when a chorus flower is broken (younger flowers burst more). */
    public static void onBlockDestroyed(ClientLevel level, BlockPos pos, BlockState state) {
        IlluminationsConfig config = IlluminationsConfig.get();
        if (!config.enabled || !config.chorusPetals.burstOnBreak || !state.is(Blocks.CHORUS_FLOWER)) {
            return;
        }
        RandomSource random = level.getRandom();
        int count = (ChorusFlowerBlock.DEAD_AGE + 1 - state.getValue(ChorusFlowerBlock.AGE)) * 6 * Math.max(1, config.chorusPetals.multiplier);
        for (int i = 0; i < count && ParticleTracker.hasRoom(ParticleKind.CHORUS_PETAL); i++) {
            ParticleTracker.spawn(new ChorusPetalParticle(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    random.nextGaussian() * 0.08, random.nextGaussian() * 0.08 + 0.05, random.nextGaussian() * 0.08, true));
        }
    }

    private static void spawnChorusPetals(ClientLevel level, BlockPos pos, BlockState state, RandomSource random, IlluminationsConfig config) {
        int count = (ChorusFlowerBlock.DEAD_AGE + 1 - state.getValue(ChorusFlowerBlock.AGE)) * config.chorusPetals.multiplier;
        for (int i = 0; i < count && ParticleTracker.hasRoom(ParticleKind.CHORUS_PETAL); i++) {
            double x = pos.getX() + 0.5 + random.nextGaussian() * 3.0;
            double y = pos.getY() + 0.5 + random.nextGaussian() * 2.0;
            double z = pos.getZ() + 0.5 + random.nextGaussian() * 3.0;
            PROBE.set(x, y, z);
            if (level.isLoaded(PROBE) && level.getBlockState(PROBE).isAir()) {
                ParticleTracker.spawn(new ChorusPetalParticle(level, x, y, z, 0.0, 0.0, 0.0, false));
            }
        }
    }

    /** Crystals appear in the water around a sea lantern, more often in the dimmer water further out. */
    private static void spawnPrismarineCrystals(ClientLevel level, BlockPos pos, RandomSource random, IlluminationsConfig config) {
        if (!config.prismarineCrystals.enabled) {
            return;
        }
        for (int i = 0; i < 6 && ParticleTracker.hasRoom(ParticleKind.PRISMARINE_CRYSTAL); i++) {
            PROBE.set(Mth.floor(pos.getX() + 0.5 + random.nextGaussian() * 8.0),
                    Mth.floor(pos.getY() + 0.5 + random.nextGaussian() * 8.0),
                    Mth.floor(pos.getZ() + 0.5 + random.nextGaussian() * 8.0));
            if (!level.isLoaded(PROBE) || !level.getFluidState(PROBE).is(FluidTags.WATER)) {
                continue;
            }
            if (random.nextInt(1 + level.getBrightness(LightLayer.BLOCK, PROBE)) == 0) {
                ParticleTracker.spawn(new PrismarineCrystalParticle(level,
                        PROBE.getX() + random.nextDouble(), PROBE.getY() + random.nextDouble(), PROBE.getZ() + random.nextDouble()));
            }
        }
    }
}
