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
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;

/**
 * Tiny bioluminescent specks drifting in dark water.
 *
 * <p>The original pushed plankton upward whenever it left water and its "ground" search only found the water block it
 * was already in. Plankton now random-walks inside water and fades away as soon as it is no longer submerged.</p>
 */
public class PlanktonParticle extends AmbientParticle {
    private static final float PULSE_STEP = 0.01F;

    private float brightness;
    private float targetBrightness;
    private boolean fadingOut;

    public PlanktonParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, Sprites.get(Sprites.PLANKTON));
        this.quadSize = 0.025F + this.random.nextFloat() * 0.025F;
        this.lifetime = 400 + this.random.nextInt(801);
        this.hasPhysics = true;
        this.alpha = 0.0F;
        this.setColor(0.1F, 0.55F + this.random.nextFloat() * 0.35F, 1.0F);
    }

    @Override
    public ParticleKind kind() {
        return ParticleKind.PLANKTON;
    }

    @Override
    protected void tickAmbient() {
        if (this.age++ >= this.lifetime || (this.age % 5 == 0 && !this.inWater())) {
            this.fadingOut = true;
        }

        if (this.fadingOut) {
            this.brightness -= 0.04F;
            if (this.brightness <= 0.0F) {
                this.remove();
                return;
            }
        } else {
            if (Math.abs(this.brightness - this.targetBrightness) < PULSE_STEP) {
                this.targetBrightness = 0.2F + this.random.nextFloat() * 0.8F;
            }
            this.brightness += Mth.clamp(this.targetBrightness - this.brightness, -PULSE_STEP, PULSE_STEP);
        }
        this.alpha = Mth.clamp(this.brightness, 0.0F, 1.0F);

        this.xd = this.xd * 0.95 + (this.random.nextFloat() - 0.5F) * 0.0015;
        this.yd = this.yd * 0.95 + (this.random.nextFloat() - 0.5F) * 0.0010;
        this.zd = this.zd * 0.95 + (this.random.nextFloat() - 0.5F) * 0.0015;
        this.move(this.xd, this.yd, this.zd);
    }

    private boolean inWater() {
        return this.level.getFluidState(this.at(this.x, this.y, this.z)).is(FluidTags.WATER);
    }
}
