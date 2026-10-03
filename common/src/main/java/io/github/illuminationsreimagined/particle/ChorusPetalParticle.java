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
 * A chorus-flower petal blown down and away on the End wind, twirling as it falls and darkening from white to purple.
 * Ported from the original mod's chorus petal.
 */
public class ChorusPetalParticle extends AmbientParticle {
    private final float rotationFactor;

    /** Petals given no initial velocity fade in; those thrown out by a breaking flower appear at once. */
    public ChorusPetalParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z, Sprites.get(Sprites.CHORUS_PETAL[level.getRandom().nextInt(Sprites.CHORUS_PETAL.length)]));
        this.quadSize *= 1.0F + this.random.nextFloat();
        this.lifetime = 30 + this.random.nextInt(60);
        this.hasPhysics = true;
        this.alpha = xd == 0.0 && yd == 0.0 && zd == 0.0 ? 0.0F : 1.0F;
        this.yd = yd - 0.15 - this.random.nextFloat() / 10.0;
        this.xd = xd - 0.05 - this.random.nextFloat() / 10.0;
        this.zd = zd - 0.05 - this.random.nextFloat() / 10.0;
        this.rotationFactor = (this.random.nextFloat() - 0.5F) * 0.01F;
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.setColor(1.0F, 1.0F, 1.0F);
    }

    @Override
    public ParticleKind kind() {
        return ParticleKind.CHORUS_PETAL;
    }

    @Override
    protected void tickAmbient() {
        if (this.age++ < this.lifetime) {
            this.alpha = Math.min(1.0F, this.alpha + 0.1F);
        }
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.99;
        this.yd *= 0.99;
        this.zd *= 0.99;
        this.gCol *= 0.98F;
        // The original drew red as max(green, 0.3), so the petal darkens toward purple as green fades.
        this.rCol = Math.max(this.gCol, 0.3F);
        if (this.age >= this.lifetime) {
            this.alpha = Math.max(0.0F, this.alpha - 0.1F);
            if (this.alpha <= 0.0F) {
                this.remove();
                return;
            }
        }
        if (this.onGround || this.level.getFluidState(this.at(this.x, this.y, this.z)).is(FluidTags.WATER)) {
            this.xd = 0.0;
            this.yd = 0.0;
            this.zd = 0.0;
        }
        if (this.yd != 0.0) {
            // The original turned by this many degrees per tick.
            this.roll += Mth.DEG_TO_RAD * (float) (Math.PI * Math.sin(this.rotationFactor * this.age) / 2.0);
        }
    }
}
