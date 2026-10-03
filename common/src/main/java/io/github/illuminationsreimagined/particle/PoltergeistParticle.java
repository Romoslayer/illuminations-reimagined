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
import net.minecraft.util.Mth;

/**
 * A poltergeist: a translucent spectral skull that rises from skeleton skulls (and sometimes from undead that die at
 * night) on Halloween, darting about erratically while it slowly pulses in and out of sight. Final-colour sprites.
 */
public class PoltergeistParticle extends WanderingSpiritParticle {
    private final float pulsePhase;

    public PoltergeistParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, Sprites.POLTERGEIST, 3, 0.85F);
        this.quadSize = 0.22F + this.random.nextFloat() * 0.06F;
        this.lifetime = 200 + this.random.nextInt(300);
        this.pulsePhase = this.random.nextFloat() * Mth.TWO_PI;
    }

    @Override
    public ParticleKind kind() {
        return ParticleKind.POLTERGEIST;
    }

    @Override
    protected double wanderRange() {
        return 8.0;
    }

    @Override
    protected double steering() {
        return 0.008;
    }

    @Override
    protected double maxSpeed() {
        return 0.12;
    }

    @Override
    protected int retargetDelay() {
        return 15;
    }

    @Override
    protected void tickAppearance() {
        // Fade between faint and solid so it seems to flicker in and out of the world.
        float pulse = 0.65F + 0.35F * Mth.sin(this.age * 0.15F + this.pulsePhase);
        this.rCol = pulse;
        this.gCol = pulse;
        this.bCol = Math.min(1.0F, pulse + 0.1F);
    }
}
