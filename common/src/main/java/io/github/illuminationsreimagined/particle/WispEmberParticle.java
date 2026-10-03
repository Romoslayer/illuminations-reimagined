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

/** A faint ember left behind by a will o' wisp; rises a little and fades out. */
public class WispEmberParticle extends AmbientParticle {
    private final float startSize;

    public WispEmberParticle(ClientLevel level, double x, double y, double z, float r, float g, float b) {
        super(level, x, y, z, Sprites.get(Sprites.WISP_EMBER));
        this.startSize = 0.05F + this.random.nextFloat() * 0.04F;
        this.quadSize = this.startSize;
        this.lifetime = 15 + this.random.nextInt(15);
        this.hasPhysics = false;
        this.alpha = 0.7F;
        this.yd = 0.008 + this.random.nextFloat() * 0.006;
        this.setColor(r, g, b);
    }

    @Override
    public ParticleKind kind() {
        return ParticleKind.WISP_EMBER;
    }

    @Override
    protected void tickAmbient() {
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        float progress = (float) this.age / this.lifetime;
        this.alpha = 0.7F * (1.0F - progress);
        this.quadSize = this.startSize * (1.0F - progress * 0.6F);
        this.move(this.xd, this.yd, this.zd);
    }
}
