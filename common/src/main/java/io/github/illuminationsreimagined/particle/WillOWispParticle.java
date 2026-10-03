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
package io.github.illuminationsreimagined.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A will o' wisp: a soul-flame that darts between random points, leaving a ribbon of sparks that start white and
 * turn cyan. It rises out of soul sand and soul soil and can glide through them, giving off soul particles while
 * inside. Rendered as a small 3D head ({@link SpiritModel}).
 */
public class WillOWispParticle extends WanderingSpiritParticle {
    private static final float[] TRAIL_COLOR = {1.0F, 1.0F, 1.0F};
    private static final float[] TRAIL_STEP = {-0.1F, -0.01F, 0.0F};

    public WillOWispParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, Sprites.WISP_SKIN, TRAIL_COLOR, TRAIL_STEP);
    }

    @Override
    public ParticleKind kind() {
        return ParticleKind.WILL_O_WISP;
    }

    @Override
    protected int retargetInterval() {
        return 20;
    }

    @Override
    protected boolean passesThroughSoulBlocks() {
        return true;
    }

    @Override
    protected BlockState deathBlock() {
        return Blocks.SOUL_SAND.defaultBlockState();
    }

    @Override
    protected SoundEvent ambientSound() {
        return SoundEvents.SOUL_ESCAPE.value();
    }

    @Override
    protected float ambientPitch() {
        return 1.5F;
    }

    @Override
    protected int ambientSoundChance() {
        return 20;
    }

    @Override
    protected SoundEvent[] deathSounds() {
        return new SoundEvent[]{SoundEvents.SOUL_ESCAPE.value(), SoundEvents.SOUL_SAND_BREAK};
    }

    @Override
    protected float[] deathPitches() {
        return new float[]{1.5F, 1.0F};
    }
}
