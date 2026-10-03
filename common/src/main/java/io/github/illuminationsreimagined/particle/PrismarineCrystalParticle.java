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
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * A small crystal flake drifting slowly through the water around a sea lantern, twirling as it sinks and lying flat
 * once it settles. Out of the water it drops quickly. Ported from the original mod's prismarine crystal.
 */
public class PrismarineCrystalParticle extends AmbientParticle {
    /** Our crystal art fills more of its frame than the original's, so it is drawn smaller to keep the same size. */
    private static final float ART_SCALE = 0.65F;
    private static final Quaternionf LYING_FLAT = new Quaternionf().rotationX(Mth.HALF_PI);

    private final float rotationFactor;
    private final float groundOffset;

    public PrismarineCrystalParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, Sprites.get(Sprites.PRISMARINE_CRYSTAL[level.getRandom().nextInt(Sprites.PRISMARINE_CRYSTAL.length)]));
        this.quadSize *= (1.0F + this.random.nextFloat()) * ART_SCALE;
        this.lifetime = 400 + this.random.nextInt(801);
        this.hasPhysics = true;
        this.alpha = 0.0F;
        this.xd = this.random.nextFloat() * 0.01;
        this.yd = -this.random.nextFloat() * 0.01;
        this.zd = this.random.nextFloat() * 0.01;
        this.groundOffset = this.random.nextFloat() / 100.0F + 0.001F;
        this.rotationFactor = (this.random.nextFloat() - 0.5F) * 0.01F;
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.setColor(1.0F, 1.0F, 1.0F);
    }

    @Override
    public ParticleKind kind() {
        return ParticleKind.PRISMARINE_CRYSTAL;
    }

    @Override
    protected void tickAmbient() {
        if (this.age++ < this.lifetime) {
            this.alpha = Math.min(1.0F, this.alpha + 0.01F);
        }
        boolean inWater = this.level.getFluidState(this.at(this.x, this.y, this.z)).is(FluidTags.WATER);
        this.move(this.xd, this.yd, this.zd);
        if (!inWater) {
            this.xd *= 0.9;
            this.yd = -0.9;
            this.zd *= 0.9;
        }
        if (this.age >= this.lifetime) {
            this.alpha = Math.max(0.0F, this.alpha - 0.01F);
            if (this.alpha <= 0.0F) {
                this.remove();
                return;
            }
        }
        this.rCol = 0.8F + Mth.sin(this.age / 100.0F) * 0.2F;
        if (this.onGround) {
            this.xd = 0.0;
            this.yd = 0.0;
            this.zd = 0.0;
        }
        if (this.yd != 0.0) {
            // The original turned by this many degrees per tick.
            this.roll += Mth.DEG_TO_RAD * (float) (Math.PI * Math.sin(this.rotationFactor * this.age) / 2.0);
        }
    }

    @Override
    protected void extractRotatedQuad(QuadParticleRenderState state, Quaternionf rotation, float x, float y, float z, float partialTickTime) {
        super.extractRotatedQuad(state, this.onGround ? LYING_FLAT : rotation, x, y + this.groundOffset, z, partialTickTime);
    }
}
