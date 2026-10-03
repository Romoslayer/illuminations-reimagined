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
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A glowworm hanging from a cave ceiling, pulsing slowly. When its supporting block disappears it drops and fades.
 *
 * <p>Changes from the original: no per-tick {@code new Random()}, the ceiling search respects the modern build
 * height, support is checked every few ticks instead of every tick, and a falling glowworm stops at the floor
 * rather than sliding through blocks.</p>
 */
public class GlowwormParticle extends AmbientParticle {
    private static final float PULSE_STEP = 0.008F;

    private float brightness;
    private float targetBrightness;
    private boolean falling;
    private boolean fadingOut;
    private final double anchorX;
    private final double anchorZ;
    private final float swayPhase;

    /**
     * @param ceilingBottomY the Y coordinate of the underside of the supporting block
     */
    public GlowwormParticle(ClientLevel level, double x, double ceilingBottomY, double z) {
        super(level, x, ceilingBottomY - 0.06, z, Sprites.get(Sprites.GLOWWORM));
        this.quadSize = 0.06F + this.random.nextFloat() * 0.05F;
        this.lifetime = 1200 + this.random.nextInt(2401);
        this.hasPhysics = false;
        this.alpha = 0.0F;
        this.anchorX = x;
        this.anchorZ = z;
        this.swayPhase = this.random.nextFloat() * Mth.TWO_PI;
        this.setColor(0.25F + this.random.nextFloat() * 0.15F, 0.85F + this.random.nextFloat() * 0.15F, 1.0F);
    }

    @Override
    public ParticleKind kind() {
        return ParticleKind.GLOWWORM;
    }

    @Override
    protected void tickAmbient() {
        if (this.age++ >= this.lifetime) {
            this.fadingOut = true;
        }

        if (!this.falling && this.age % 10 == 0 && !this.hasSupport()) {
            this.falling = true;
            this.fadingOut = true;
            this.hasPhysics = true;
        }

        if (this.fadingOut) {
            this.brightness -= this.falling ? 0.03F : PULSE_STEP;
            if (this.brightness <= 0.0F) {
                this.remove();
                return;
            }
        } else {
            if (Math.abs(this.brightness - this.targetBrightness) < PULSE_STEP) {
                this.targetBrightness = 0.35F + this.random.nextFloat() * 0.65F;
            }
            this.brightness += Mth.clamp(this.targetBrightness - this.brightness, -PULSE_STEP, PULSE_STEP);
        }
        this.alpha = Mth.clamp(this.brightness, 0.0F, 1.0F);

        if (this.falling) {
            this.yd = Math.max(this.yd - 0.04, -0.6);
            this.move(0.0, this.yd, 0.0);
            if (this.onGround) {
                this.yd = 0.0;
            }
        } else {
            // Gentle sway around the anchor point; position is set directly but stays within a few hundredths of a block.
            float t = (this.age + this.swayPhase * 20.0F) * 0.03F;
            this.setPos(this.anchorX + Mth.sin(t) * 0.015, this.y, this.anchorZ + Mth.cos(t * 0.7F) * 0.015);
        }
    }

    private boolean hasSupport() {
        BlockState above = this.level.getBlockState(this.at(this.x, this.y + 0.2, this.z));
        return above.isFaceSturdy(this.level, this.scratchPos, Direction.DOWN);
    }
}
