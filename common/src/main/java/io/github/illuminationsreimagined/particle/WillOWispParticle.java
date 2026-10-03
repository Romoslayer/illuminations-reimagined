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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;

/**
 * A will o' wisp: a flickering soul-flame that wanders slowly and sheds faint embers.
 *
 * <p>The original rendered a textured 3D entity model from inside the particle renderer using its own immediate-mode
 * buffer (incompatible with batching and with Sodium/Iris). The wisp is now an animated billboard plus trailing
 * ember particles, rendered entirely through the vanilla particle pipeline.</p>
 */
public class WillOWispParticle extends AmbientParticle {
    private final TextureAtlasSprite[] frames = new TextureAtlasSprite[Sprites.WISP.length];
    private double targetX;
    private double targetY;
    private double targetZ;
    private int retargetCooldown;
    private boolean fadingOut;

    public WillOWispParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, Sprites.get(Sprites.WISP[0]));
        for (int i = 0; i < this.frames.length; i++) {
            this.frames[i] = Sprites.get(Sprites.WISP[i]);
        }
        this.quadSize = 0.22F + this.random.nextFloat() * 0.08F;
        this.lifetime = 300 + this.random.nextInt(400);
        this.hasPhysics = true;
        this.alpha = 0.0F;
        this.setColor(0.55F, 0.95F, 1.0F);
        this.targetX = x;
        this.targetY = y + 1.0;
        this.targetZ = z;
    }

    @Override
    public ParticleKind kind() {
        return ParticleKind.WILL_O_WISP;
    }

    @Override
    protected void tickAmbient() {
        if (this.age++ >= this.lifetime) {
            this.fadingOut = true;
        }
        if (this.fadingOut) {
            this.alpha -= 0.03F;
            if (this.alpha <= 0.0F) {
                this.remove();
                return;
            }
        } else {
            this.alpha = Math.min(0.95F, this.alpha + 0.03F);
        }

        this.setSprite(this.frames[(this.age / 3) % this.frames.length]);
        // Flicker.
        float flicker = 0.85F + this.random.nextFloat() * 0.15F;
        this.rCol = 0.5F * flicker;
        this.gCol = 0.95F * flicker;

        if (--this.retargetCooldown <= 0) {
            this.targetX = this.x + (this.random.nextDouble() - 0.5) * 6.0;
            this.targetY = this.y + (this.random.nextDouble() - 0.35) * 2.0;
            this.targetZ = this.z + (this.random.nextDouble() - 0.5) * 6.0;
            this.retargetCooldown = 40 + this.random.nextInt(60);
        }
        double dx = this.targetX - this.x;
        double dy = this.targetY - this.y;
        double dz = this.targetZ - this.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > 0.05) {
            this.xd += dx / dist * 0.003;
            this.yd += dy / dist * 0.003;
            this.zd += dz / dist * 0.003;
        }
        this.xd = Mth.clamp(this.xd * 0.95, -0.05, 0.05);
        this.yd = Mth.clamp(this.yd * 0.95, -0.05, 0.05);
        this.zd = Mth.clamp(this.zd * 0.95, -0.05, 0.05);
        this.move(this.xd, this.yd, this.zd);

        if (!this.fadingOut && this.age % 3 == 0) {
            ParticleTracker.spawn(new WispEmberParticle(this.level,
                    this.x + (this.random.nextFloat() - 0.5F) * 0.1,
                    this.y + (this.random.nextFloat() - 0.5F) * 0.1,
                    this.z + (this.random.nextFloat() - 0.5F) * 0.1,
                    this.rCol, this.gCol, this.bCol));
        }
    }
}
