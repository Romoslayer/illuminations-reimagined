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
 * A glowworm hanging from a cave ceiling, blinking slowly. When the block above it disappears it drops and fades.
 * Ported from the original mod's glowworm.
 *
 * <p>Changes from the original: no per-tick {@code new Random()}, and the ceiling search (done by the spawner) respects
 * the modern build height. The original also steered glowworms along the ceiling, but toward a target height of 0,
 * which left them all but motionless; they now simply stay put.</p>
 */
public class GlowwormParticle extends AmbientParticle {
    private static final float BLINK_STEP = 0.01F;

    private float glow;
    private float nextGlowGoal;
    private boolean onCeiling = true;

    /** @param y the hanging height, just below the ceiling block */
    public GlowwormParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, Sprites.get(Sprites.GLOWWORM));
        this.quadSize *= 0.25F + this.random.nextFloat() * 0.5F;
        this.lifetime = 1200 + this.random.nextInt(2401);
        this.hasPhysics = true;
        this.alpha = 0.0F;
        this.setColor(0.0F, 0.75F + this.random.nextFloat() * 0.25F, 1.0F);
    }

    @Override
    public ParticleKind kind() {
        return ParticleKind.GLOWWORM;
    }

    @Override
    protected boolean glows() {
        return true;
    }

    @Override
    protected void tickAmbient() {
        boolean fadingOut = this.age++ >= this.lifetime;
        if (fadingOut && this.glow < 0.0F) {
            this.remove();
            return;
        }
        if (this.onCeiling && this.level.getBlockState(this.at(this.x, this.y + 0.5, this.z)).isAir()) {
            this.onCeiling = false;
        }
        if (!this.onCeiling) {
            this.yd -= 0.1;
            this.lifetime = 0;
        }

        if (fadingOut) {
            this.glow -= BLINK_STEP;
        } else if (this.glow > this.nextGlowGoal - BLINK_STEP && this.glow < this.nextGlowGoal + BLINK_STEP) {
            this.nextGlowGoal = this.random.nextFloat();
        } else if (this.nextGlowGoal > this.glow) {
            this.glow += BLINK_STEP;
        } else {
            this.glow -= BLINK_STEP;
        }
        this.alpha = Math.max(0.0F, Math.min(1.0F, this.glow));

        if (this.yd != 0.0) {
            this.move(0.0, this.yd, 0.0);
        }
    }
}
