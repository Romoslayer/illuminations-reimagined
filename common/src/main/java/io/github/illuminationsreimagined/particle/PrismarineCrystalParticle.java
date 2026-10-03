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

/** A small crystal flake drifting and slowly sinking through the water around a sea lantern. */
public class PrismarineCrystalParticle extends AmbientParticle {
    private final float spin;
    private final float colorPhase;
    private boolean fadingOut;

    public PrismarineCrystalParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, Sprites.get(Sprites.PRISMARINE_CRYSTAL[level.getRandom().nextInt(Sprites.PRISMARINE_CRYSTAL.length)]));
        this.quadSize = 0.06F + this.random.nextFloat() * 0.06F;
        this.lifetime = 400 + this.random.nextInt(801);
        this.hasPhysics = true;
        this.alpha = 0.0F;
        this.xd = (this.random.nextFloat() - 0.5F) * 0.01;
        this.yd = -this.random.nextFloat() * 0.004;
        this.zd = (this.random.nextFloat() - 0.5F) * 0.01;
        this.spin = (this.random.nextFloat() - 0.5F) * 0.05F;
        this.colorPhase = this.random.nextFloat() * Mth.TWO_PI;
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
    }

    @Override
    public ParticleKind kind() {
        return ParticleKind.PRISMARINE_CRYSTAL;
    }

    @Override
    protected void tickAmbient() {
        boolean inWater = this.level.getFluidState(this.at(this.x, this.y, this.z)).is(FluidTags.WATER);
        if (this.age++ >= this.lifetime || !inWater) {
            this.fadingOut = true;
        }
        if (this.fadingOut) {
            this.alpha -= inWater ? 0.01F : 0.08F;
            if (this.alpha <= 0.0F) {
                this.remove();
                return;
            }
        } else {
            this.alpha = Math.min(0.9F, this.alpha + 0.01F);
        }

        // Cycle softly between white, cyan and sea-green.
        float t = this.age * 0.015F + this.colorPhase;
        this.rCol = 0.55F + 0.35F * Mth.sin(t);
        this.gCol = 0.9F + 0.1F * Mth.sin(t * 0.7F);
        this.bCol = 0.8F + 0.2F * Mth.cos(t);

        if (this.onGround) {
            this.xd = 0.0;
            this.yd = 0.0;
            this.zd = 0.0;
            return;
        }
        this.xd *= 0.98;
        this.zd *= 0.98;
        this.yd = Math.max(this.yd - 0.0002, -0.01);
        this.roll += this.spin;
        this.move(this.xd, this.yd, this.zd);
    }
}
