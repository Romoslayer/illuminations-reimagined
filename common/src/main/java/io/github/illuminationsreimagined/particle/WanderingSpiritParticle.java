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
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Shared behaviour for floating spirits (will o' wisps, pumpkin spirits, poltergeists): an animated billboard that
 * fades in, wanders toward randomly chosen nearby points, optionally sheds embers, and fades out.
 */
public abstract class WanderingSpiritParticle extends AmbientParticle {
    private final TextureAtlasSprite[] frames;
    private final int ticksPerFrame;
    private final float maxAlpha;
    private double targetX;
    private double targetY;
    private double targetZ;
    private int retargetCooldown;
    protected boolean fadingOut;

    protected WanderingSpiritParticle(ClientLevel level, double x, double y, double z, Identifier[] frameIds, int ticksPerFrame, float maxAlpha) {
        super(level, x, y, z, Sprites.get(frameIds[0]));
        this.frames = new TextureAtlasSprite[frameIds.length];
        for (int i = 0; i < frameIds.length; i++) {
            this.frames[i] = Sprites.get(frameIds[i]);
        }
        this.ticksPerFrame = ticksPerFrame;
        this.maxAlpha = maxAlpha;
        this.hasPhysics = true;
        this.alpha = 0.0F;
        this.targetX = x;
        this.targetY = y + 1.0;
        this.targetZ = z;
    }

    /** How far a new wander target may be, horizontally and vertically. */
    protected double wanderRange() {
        return 6.0;
    }

    /** Steering strength per tick; higher feels more erratic. */
    protected double steering() {
        return 0.003;
    }

    protected double maxSpeed() {
        return 0.05;
    }

    /** Ticks between new wander targets (minimum; a random extra is added). */
    protected int retargetDelay() {
        return 40;
    }

    /** Called every tick after movement; colour, flicker, trails. */
    protected void tickAppearance() {
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
            this.alpha = Math.min(this.maxAlpha, this.alpha + 0.03F);
        }

        this.setSprite(this.frames[(this.age / this.ticksPerFrame) % this.frames.length]);

        if (--this.retargetCooldown <= 0) {
            double range = this.wanderRange();
            this.targetX = this.x + (this.random.nextDouble() - 0.5) * range;
            this.targetY = this.y + (this.random.nextDouble() - 0.35) * range / 3.0;
            this.targetZ = this.z + (this.random.nextDouble() - 0.5) * range;
            this.retargetCooldown = this.retargetDelay() + this.random.nextInt(this.retargetDelay() + 20);
        }
        double dx = this.targetX - this.x;
        double dy = this.targetY - this.y;
        double dz = this.targetZ - this.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > 0.05) {
            double steer = this.steering();
            this.xd += dx / dist * steer;
            this.yd += dy / dist * steer;
            this.zd += dz / dist * steer;
        }
        double max = this.maxSpeed();
        this.xd = Mth.clamp(this.xd * 0.95, -max, max);
        this.yd = Mth.clamp(this.yd * 0.95, -max, max);
        this.zd = Mth.clamp(this.zd * 0.95, -max, max);
        this.move(this.xd, this.yd, this.zd);

        this.tickAppearance();
    }

    protected void shedEmber(float r, float g, float b) {
        ParticleTracker.spawn(new WispEmberParticle(this.level,
                this.x + (this.random.nextFloat() - 0.5F) * 0.1,
                this.y + (this.random.nextFloat() - 0.5F) * 0.1,
                this.z + (this.random.nextFloat() - 0.5F) * 0.1,
                r, g, b));
    }
}
