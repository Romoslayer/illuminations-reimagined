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
 * A spark in a spirit's trail. Behaviour follows the original Illuminations wisp trail: it starts in the spirit's
 * trail colour and drifts by a fixed step every tick (white wisp sparks turn cyan), floats upward, shrinks, and fades
 * out after its short life.
 */
public class WispEmberParticle extends AmbientParticle {
    private final float redStep;
    private final float greenStep;
    private final float blueStep;

    public WispEmberParticle(ClientLevel level, double x, double y, double z,
                             float r, float g, float b, float redStep, float greenStep, float blueStep) {
        super(level, x, y, z, Sprites.get(Sprites.WISP_EMBER));
        this.quadSize = (0.1F + this.random.nextFloat() * 0.1F) * (0.25F + this.random.nextFloat() * 0.5F);
        this.lifetime = 10 + this.random.nextInt(10);
        this.hasPhysics = true;
        this.alpha = 1.0F;
        this.yd = 0.1;
        this.setColor(r, g, b);
        this.redStep = redStep;
        this.greenStep = greenStep;
        this.blueStep = blueStep;
    }

    @Override
    public ParticleKind kind() {
        return ParticleKind.WISP_EMBER;
    }

    @Override
    protected void tickAmbient() {
        if (this.age++ >= this.lifetime) {
            this.alpha -= 0.05F;
        }
        if (this.alpha < 0.0F || this.quadSize <= 0.0F) {
            this.remove();
            return;
        }
        this.rCol = Mth.clamp(this.rCol + this.redStep, 0.0F, 1.0F);
        this.gCol = Mth.clamp(this.gCol + this.greenStep, 0.0F, 1.0F);
        this.bCol = Mth.clamp(this.bCol + this.blueStep, 0.0F, 1.0F);
        this.yd -= 0.001;
        this.quadSize = Math.max(0.0F, this.quadSize - 0.005F);
        this.move(0.0, this.yd, 0.0);
    }
}
