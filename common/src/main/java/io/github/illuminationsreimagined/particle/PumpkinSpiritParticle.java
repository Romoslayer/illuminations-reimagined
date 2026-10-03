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
 * A pumpkin spirit: a glowing jack o'lantern face that bursts out of a jack o'lantern on Halloween nights and darts
 * around leaving a trail of sparks that run from yellow to orange to red. Rendered as a small 3D head ({@link SpiritModel}).
 */
public class PumpkinSpiritParticle extends WanderingSpiritParticle {
    private static final float[] TRAIL_COLOR = {1.0F, 0.95F, 0.0F};
    private static final float[] TRAIL_STEP = {0.0F, -0.03F, 0.0F};

    public PumpkinSpiritParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, Sprites.PUMPKIN_SPIRIT_SKIN, TRAIL_COLOR, TRAIL_STEP);
    }

    @Override
    public ParticleKind kind() {
        return ParticleKind.PUMPKIN_SPIRIT;
    }

    @Override
    protected int retargetInterval() {
        return 100;
    }

    @Override
    protected boolean burstOnSpawn() {
        return true;
    }

    @Override
    protected BlockState deathBlock() {
        return Blocks.JACK_O_LANTERN.defaultBlockState();
    }

    @Override
    protected SoundEvent ambientSound() {
        return SoundEvents.VEX_AMBIENT;
    }

    @Override
    protected float ambientPitch() {
        return 0.8F;
    }

    @Override
    protected int ambientSoundChance() {
        return 100;
    }

    @Override
    protected SoundEvent[] deathSounds() {
        return new SoundEvent[]{SoundEvents.VEX_DEATH, SoundEvents.WOOD_BREAK};
    }

    @Override
    protected float[] deathPitches() {
        return new float[]{0.8F, 1.0F};
    }
}
