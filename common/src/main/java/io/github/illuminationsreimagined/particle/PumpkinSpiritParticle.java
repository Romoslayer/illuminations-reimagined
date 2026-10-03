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

/**
 * A pumpkin spirit: a small glowing jack o'lantern face that slips out of a jack o'lantern on Halloween nights,
 * bobs around mischievously and trails orange embers. Its sprites are painted in final colours (not tinted).
 */
public class PumpkinSpiritParticle extends WanderingSpiritParticle {
    public PumpkinSpiritParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, Sprites.PUMPKIN_SPIRIT, 4, 1.0F);
        this.quadSize = 0.2F + this.random.nextFloat() * 0.06F;
        this.lifetime = 300 + this.random.nextInt(300);
    }

    @Override
    public ParticleKind kind() {
        return ParticleKind.PUMPKIN_SPIRIT;
    }

    @Override
    protected double steering() {
        return 0.004;
    }

    @Override
    protected void tickAppearance() {
        if (!this.fadingOut && this.age % 4 == 0) {
            this.shedEmber(1.0F, 0.55F + this.random.nextFloat() * 0.2F, 0.1F);
        }
    }
}
