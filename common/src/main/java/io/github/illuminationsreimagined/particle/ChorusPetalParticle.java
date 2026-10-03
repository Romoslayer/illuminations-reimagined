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
 * A chorus-flower petal twirling down through the End air, shifting from pale lilac toward deep purple.
 *
 * <p>The original always blew petals toward negative X/Z. Each petal now gets its own gentle drift direction, and
 * petals settle on the ground or on water before fading.</p>
 */
public class ChorusPetalParticle extends AmbientParticle {
    private final float spin;
    private final double driftX;
    private final double driftZ;
    private boolean fadingOut;

    /**
     * @param burst true for petals thrown out when a flower breaks (start visible, carry the given velocity)
     */
    public ChorusPetalParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, boolean burst) {
        super(level, x, y, z, Sprites.get(Sprites.CHORUS_PETAL[level.getRandom().nextInt(Sprites.CHORUS_PETAL.length)]));
        this.quadSize = 0.08F + this.random.nextFloat() * 0.08F;
        this.lifetime = 40 + this.random.nextInt(80);
        this.hasPhysics = true;
        this.alpha = burst ? 1.0F : 0.0F;
        this.xd = xd;
        this.yd = yd - 0.02 - this.random.nextFloat() * 0.02;
        this.zd = zd;
        float angle = this.random.nextFloat() * Mth.TWO_PI;
        this.driftX = Mth.cos(angle) * 0.004;
        this.driftZ = Mth.sin(angle) * 0.004;
        this.spin = (this.random.nextFloat() - 0.5F) * 0.25F;
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.setColor(0.95F, 0.88F, 1.0F);
    }

    @Override
    public ParticleKind kind() {
        return ParticleKind.CHORUS_PETAL;
    }

    @Override
    protected void tickAmbient() {
        if (this.age++ >= this.lifetime) {
            this.fadingOut = true;
        }
        if (this.fadingOut) {
            this.alpha -= 0.05F;
            if (this.alpha <= 0.0F) {
                this.remove();
                return;
            }
        } else {
            this.alpha = Math.min(1.0F, this.alpha + 0.1F);
        }

        // Shift toward purple as the petal ages.
        this.gCol = Math.max(0.45F, this.gCol * 0.992F);
        this.rCol = Math.max(0.7F, this.rCol * 0.997F);

        boolean resting = this.onGround || this.level.getFluidState(this.at(this.x, this.y, this.z)).is(FluidTags.WATER);
        if (resting) {
            this.xd = 0.0;
            this.yd = 0.0;
            this.zd = 0.0;
            this.fadingOut = true;
            return;
        }

        this.xd = this.xd * 0.96 + this.driftX + Mth.sin(this.age * 0.2F + this.spin * 10.0F) * 0.003;
        this.yd = Math.max(this.yd * 0.98 - 0.002, -0.08);
        this.zd = this.zd * 0.96 + this.driftZ;
        this.roll += this.spin;
        this.move(this.xd, this.yd, this.zd);
    }
}
