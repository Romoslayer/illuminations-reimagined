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
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tiny glowing specks floating almost motionless in dark water, blinking slowly. Ported from the original mod's plankton.
 *
 * <p>Fixes: the original pushed plankton upward whenever the block below was not water, so plankton that left the
 * water (or spawned in a puddle) rose into the sky forever; it is now only pushed up while still submerged. Its search
 * for water below also used {@code 0} as "not found".</p>
 */
public class PlanktonParticle extends AmbientParticle {
    private static final float BLINK_STEP = 0.01F;
    private static final int WATER_SEARCH_DEPTH = 20;

    private float glow;
    private float nextGlowGoal;
    private double targetX;
    private double targetY;
    private double targetZ;
    private boolean hasTarget;
    private int retargetCooldown;

    public PlanktonParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, Sprites.get(Sprites.PLANKTON));
        this.quadSize *= 0.05F + this.random.nextFloat() * 0.05F;
        this.lifetime = 400 + this.random.nextInt(801);
        this.hasPhysics = true;
        this.alpha = 0.0F;
        this.setColor(0.0F, 0.25F + this.random.nextFloat() * 0.25F, 1.0F);
    }

    @Override
    public ParticleKind kind() {
        return ParticleKind.PLANKTON;
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

        this.retargetCooldown -= 10;
        double dx = this.targetX - this.x;
        double dy = this.targetY - this.y;
        double dz = this.targetZ - this.z;
        if (!this.hasTarget || (this.level.getGameTime() % 20 == 0 && (dx * dx + dy * dy + dz * dz < 9.0 || this.retargetCooldown <= 0))) {
            this.selectBlockTarget();
            dx = this.targetX - this.x;
            dy = this.targetY - this.y;
            dz = this.targetZ - this.z;
        }
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double scale = length > 1.0E-6 ? 0.001 / length : 0.0;
        this.xd = 0.9 * this.xd + 0.1 * dx * scale;
        this.zd = 0.9 * this.zd + 0.1 * dz * scale;
        boolean waterBelow = this.level.getFluidState(this.at(this.x, this.y - 0.1, this.z)).is(FluidTags.WATER);
        if (!waterBelow && this.level.getFluidState(this.at(this.x, this.y, this.z)).is(FluidTags.WATER)) {
            this.yd = 0.05;
        } else {
            this.yd = 0.9 * this.yd + 0.1 * dy * scale;
        }

        BlockPos.MutableBlockPos targetBlock = this.at(this.targetX, this.targetY + 0.5, this.targetZ);
        if (!targetBlock.equals(BlockPos.containing(this.x, this.y, this.z))) {
            this.moveSteered(this.xd, this.yd, this.zd);
        }
    }

    private void selectBlockTarget() {
        double waterLevel = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < WATER_SEARCH_DEPTH; i++) {
            if (this.level.getFluidState(this.at(this.x, this.y - i, this.z)).is(FluidTags.WATER)) {
                waterLevel = this.y - i;
                break;
            }
        }
        this.targetX = this.x + this.random.nextGaussian() * 10.0;
        this.targetY = Math.max(this.y + this.random.nextGaussian() * 2.0, waterLevel);
        this.targetZ = this.z + this.random.nextGaussian() * 10.0;
        BlockPos.MutableBlockPos target = this.at(this.targetX, this.targetY, this.targetZ);
        BlockState state = this.level.getBlockState(target);
        if (state.isCollisionShapeFullBlock(this.level, target) && state.isRedstoneConductor(this.level, target)) {
            this.targetY += 1.0;
        }
        this.hasTarget = true;
        this.retargetCooldown = this.random.nextInt(100);
    }
}
