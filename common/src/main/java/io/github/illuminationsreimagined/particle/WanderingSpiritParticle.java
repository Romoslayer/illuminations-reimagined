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
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Shared behaviour for floating spirits (will o' wisps, pumpkin spirits, poltergeists), ported from the movement and
 * trail logic of the original Illuminations will o' wisp:
 *
 * <ul>
 *   <li>each spirit picks a cruising speed between 0.1 and 1.0 blocks per tick and steers toward random targets about
 *       ten blocks away in any direction, re-rolling speed with every new target;</li>
 *   <li>while moving it sheds a trail of sparks, more the faster it flies, whose colour drifts over their short life;</li>
 *   <li>after 30-60 seconds it bursts into sparks and block fragments with a sound; one that spends over a second
 *       inside a block after reaching open air simply vanishes.</li>
 * </ul>
 *
 * <p>Fixes over the original: movement no longer latches to a stop after a blocked vertical move (it froze spirits that
 * touched the ground), the model is centred in the hitbox instead of on its bottom edge (resting spirits sank halfway
 * into the block), the first target is chosen at once instead of the world origin, a blocked axis loses its speed on
 * Z as well as X, retarget cooldowns are never negative, and the head turns smoothly (the original interpolated it by
 * the vertical offset instead of the frame time).</p>
 */
public abstract class WanderingSpiritParticle extends AmbientParticle {
    /** Hitbox edge. The model is drawn centred in the hitbox, so it never sinks into the floor it rests on. */
    private static final float SIZE = 0.25F;
    private static final float HALF_SIZE = SIZE / 2.0F;

    private final Identifier skin;
    private float yaw;
    private float yawO;
    private float pitch;
    private float pitchO;
    private final float trailRed;
    private final float trailGreen;
    private final float trailBlue;
    private final float trailRedStep;
    private final float trailGreenStep;
    private final float trailBlueStep;

    protected float speed;
    private double targetX;
    private double targetY;
    private double targetZ;
    private boolean hasTarget;
    private int retargetCooldown;
    private int ticksInBlock = -1;
    private double lastStartX = Double.NaN;
    private double lastStartY;
    private double lastStartZ;

    /**
     * @param trailColor     starting colour of trail sparks (r, g, b)
     * @param trailColorStep per-tick change of the sparks' colour (r, g, b)
     */
    protected WanderingSpiritParticle(ClientLevel level, double x, double y, double z, Identifier skin,
                                      float[] trailColor, float[] trailColorStep) {
        // The quad sprite is unused: spirits are drawn as 3D models by SpiritParticleGroup.
        super(level, x, y, z, Sprites.get(Sprites.WISP_EMBER));
        this.skin = skin;
        this.trailRed = trailColor[0];
        this.trailGreen = trailColor[1];
        this.trailBlue = trailColor[2];
        this.trailRedStep = trailColorStep[0];
        this.trailGreenStep = trailColorStep[1];
        this.trailBlueStep = trailColorStep[2];
        this.hasPhysics = true;
        this.friction = 1.0F;
        this.gravity = 0.0F;
        this.lifetime = 600 + this.random.nextInt(600);
        this.speed = rollSpeed();
        this.alpha = 1.0F;
        this.quadSize = 0.25F;
        this.setSize(SIZE, SIZE);
    }

    @Override
    public ParticleRenderType getGroup() {
        return SpiritParticleGroup.RENDER_TYPE;
    }

    public Identifier skin() {
        return this.skin;
    }

    Vec3 renderPosition(float partialTick) {
        return new Vec3(Mth.lerp(partialTick, this.xo, this.x), Mth.lerp(partialTick, this.yo, this.y) + HALF_SIZE, Mth.lerp(partialTick, this.zo, this.z));
    }

    float renderYaw(float partialTick) {
        return SpiritParticleGroup.lerpDegrees(partialTick, this.yawO, this.yaw);
    }

    float renderPitch(float partialTick) {
        return Mth.lerp(partialTick, this.pitchO, this.pitch);
    }

    float renderAlpha() {
        return this.alpha * this.modelOpacity();
    }

    /** Drawn with the glowing (emissive) render type; the original drew poltergeists plain. */
    boolean glowingModel() {
        return true;
    }

    /** Opacity of the model; the original drew poltergeists at half opacity. */
    protected float modelOpacity() {
        return 1.0F;
    }

    private float rollSpeed() {
        return 0.1F + Math.max(0.0F, this.random.nextFloat() - 0.1F);
    }

    /** Ticks between checks for a new target (the original used 20 for wisps and 100 for Halloween spirits). */
    protected abstract int retargetInterval();

    /** A trail of 10 × speed sparks per tick (wisps, pumpkin spirits), or a single spark per tick (poltergeists). */
    protected boolean continuousTrail() {
        return true;
    }

    /** Spirits that appear with a burst of sparks during their first five ticks. */
    protected boolean burstOnSpawn() {
        return false;
    }

    /** Wisps glide through soul sand and soul soil. */
    protected boolean passesThroughSoulBlocks() {
        return false;
    }

    protected abstract BlockState deathBlock();

    protected abstract SoundEvent ambientSound();

    protected abstract float ambientPitch();

    /** One in this many ticks plays the ambient sound. */
    protected abstract int ambientSoundChance();

    protected abstract SoundEvent[] deathSounds();

    protected abstract float[] deathPitches();

    @Override
    protected void tickAmbient() {
        // Did not move at all last tick (reached its target's block, or stuck): choose somewhere else.
        if (this.x == this.lastStartX && this.y == this.lastStartY && this.z == this.lastStartZ) {
            this.pickTarget();
        }
        this.lastStartX = this.x;
        this.lastStartY = this.y;
        this.lastStartZ = this.z;
        if (this.burstOnSpawn() && this.age < 5) {
            for (int i = 0; i < 25; i++) {
                this.spawnSpark();
            }
        }
        if (this.age++ >= this.lifetime) {
            this.die();
            return;
        }

        double dx = this.targetX - this.x;
        double dy = this.targetY - this.y;
        double dz = this.targetZ - this.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        // The original counted this down by 10 whenever the spirit had barely moved, which (as it compared the position
        // with itself) was every tick.
        this.retargetCooldown -= 10;
        if (!this.hasTarget || (this.level.getGameTime() % this.retargetInterval() == 0 && (distSq < 9.0 || this.retargetCooldown <= 0))) {
            this.pickTarget();
            dx = this.targetX - this.x;
            dy = this.targetY - this.y;
            dz = this.targetZ - this.z;
            distSq = dx * dx + dy * dy + dz * dz;
        }

        if (distSq > 1.0E-6) {
            double scale = this.speed / Math.sqrt(distSq);
            this.xd = 0.9 * this.xd + 0.1 * dx * scale;
            this.yd = 0.9 * this.yd + 0.1 * dy * scale;
            this.zd = 0.9 * this.zd + 0.1 * dz * scale;
        }

        // Turn to face the direction of travel.
        this.yawO = this.yaw;
        this.pitchO = this.pitch;
        double horizontal = Math.sqrt(this.xd * this.xd + this.zd * this.zd);
        if (horizontal > 1.0E-4 || Math.abs(this.yd) > 1.0E-4) {
            this.yaw = (float) (Mth.atan2(this.xd, this.zd) * Mth.RAD_TO_DEG);
            this.pitch = (float) (Mth.atan2(this.yd, horizontal) * Mth.RAD_TO_DEG);
        }

        if (this.continuousTrail()) {
            boolean inSoulBlock = this.passesThroughSoulBlocks() && this.level.getBlockState(this.at(this.x, this.y + HALF_SIZE, this.z)).is(BlockTags.SOUL_FIRE_BASE_BLOCKS);
            for (int i = 0; i < 10 * this.speed; i++) {
                if (inSoulBlock) {
                    this.level.addParticle(ParticleTypes.SOUL, this.x + this.random.nextGaussian() / 10, this.y + HALF_SIZE + this.random.nextGaussian() / 10,
                            this.z + this.random.nextGaussian() / 10, this.random.nextGaussian() / 20, this.random.nextGaussian() / 20, this.random.nextGaussian() / 20);
                } else {
                    this.spawnSpark();
                }
            }
        } else {
            this.spawnSpark();
        }

        // Pause once inside the target's block; the next tick picks a new target.
        if (!this.at(this.targetX, this.targetY + 0.5, this.targetZ).equals(BlockPos.containing(this.x, this.y, this.z))) {
            this.moveSpirit(this.xd, this.yd, this.zd);
        }

        if (this.random.nextInt(this.ambientSoundChance()) == 0) {
            this.level.playLocalSound(this.x, this.y, this.z, this.ambientSound(), SoundSource.AMBIENT, 1.0F, this.ambientPitch(), true);
        }

        // Inside a block for over a second after having been out in the air: vanish (the count only starts once the
        // spirit has left the block it spawned in).
        if (!this.level.getBlockState(this.at(this.x, this.y + HALF_SIZE, this.z)).isAir()) {
            if (this.ticksInBlock > -1) {
                this.ticksInBlock++;
            }
        } else {
            this.ticksInBlock = 0;
        }
        if (this.ticksInBlock > 25) {
            this.remove();
        }
    }

    /**
     * Aims about ten blocks away in any direction. A target inside a full block is kept but retried at the next
     * check, without re-rolling the speed, as in the original.
     */
    private void pickTarget() {
        this.targetX = this.x + this.random.nextGaussian() * 10;
        this.targetY = this.y + this.random.nextGaussian() * 10;
        this.targetZ = this.z + this.random.nextGaussian() * 10;
        this.hasTarget = true;
        BlockPos.MutableBlockPos target = this.at(this.targetX, this.targetY, this.targetZ);
        BlockState state = this.level.getBlockState(target);
        if (state.isCollisionShapeFullBlock(this.level, target) && !(this.passesThroughSoulBlocks() && state.is(BlockTags.SOUL_FIRE_BASE_BLOCKS))) {
            this.retargetCooldown = 0;
            return;
        }
        this.speed = rollSpeed();
        this.retargetCooldown = this.random.nextInt((int) (100 / this.speed));
    }

    /** Collides with blocks, except that wisps glide straight into soul sand and soul soil. */
    private void moveSpirit(double dx, double dy, double dz) {
        if (this.passesThroughSoulBlocks()
                && this.level.getBlockState(this.at(this.x + dx, this.y + dy, this.z + dz)).is(BlockTags.SOUL_FIRE_BASE_BLOCKS)) {
            this.setPos(this.x + dx, this.y + dy, this.z + dz);
            return;
        }
        this.moveSteered(dx, dy, dz);
    }

    private void die() {
        for (int i = 0; i < 25; i++) {
            this.spawnSpark();
            this.level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, this.deathBlock()),
                    this.x + this.random.nextGaussian() / 10, this.y + HALF_SIZE + this.random.nextGaussian() / 10, this.z + this.random.nextGaussian() / 10,
                    this.random.nextGaussian() / 20, this.random.nextGaussian() / 20, this.random.nextGaussian() / 20);
        }
        SoundEvent[] sounds = this.deathSounds();
        float[] pitches = this.deathPitches();
        for (int i = 0; i < sounds.length; i++) {
            this.level.playLocalSound(this.x, this.y, this.z, sounds[i], SoundSource.AMBIENT, 1.0F, pitches[i], true);
        }
        this.remove();
    }

    /** One trail spark. Like the original's, sparks ignore distance and the particles setting. */
    private void spawnSpark() {
        ParticleTracker.spawn(new WispEmberParticle(this.level,
                this.x + this.random.nextGaussian() / 15, this.y + HALF_SIZE + this.random.nextGaussian() / 15, this.z + this.random.nextGaussian() / 15,
                this.trailRed, this.trailGreen, this.trailBlue, this.trailRedStep, this.trailGreenStep, this.trailBlueStep));
    }
}
