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

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;
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
 *   <li>it dies after 30-60 seconds, or after being stuck inside a solid block, bursting into sparks and block
 *       fragments with a sound.</li>
 * </ul>
 *
 * <p>Differences from the original: the head is an animated billboard instead of a textured 3D model rendered through a
 * private buffer; a spirit no longer stops dead when it reaches its target's block (it picks a new target instead); and
 * retarget cooldowns can no longer go negative.</p>
 */
public abstract class WanderingSpiritParticle extends AmbientParticle {
    /** Trail sparks only spawn within this distance of the camera, like vanilla's own particle culling. */
    private static final double TRAIL_RANGE_SQ = 32.0 * 32.0;

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
    private int ticksInSolid;
    private int ticksBlocked;
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
        this.alpha = 0.0F;
        this.quadSize = 0.25F;
        this.setSize(0.2F, 0.2F);
    }

    @Override
    public ParticleRenderType getGroup() {
        return SpiritParticleGroup.RENDER_TYPE;
    }

    public Identifier skin() {
        return this.skin;
    }

    Vec3 renderPosition(float partialTick) {
        return new Vec3(Mth.lerp(partialTick, this.xo, this.x), Mth.lerp(partialTick, this.yo, this.y), Mth.lerp(partialTick, this.zo, this.z));
    }

    float renderYaw(float partialTick) {
        return SpiritParticleGroup.lerpDegrees(partialTick, this.yawO, this.yaw);
    }

    float renderPitch(float partialTick) {
        return Mth.lerp(partialTick, this.pitchO, this.pitch);
    }

    float renderAlpha() {
        return this.alpha;
    }

    private float rollSpeed() {
        return 0.1F + Math.max(0.0F, this.random.nextFloat() - 0.1F);
    }

    /** Ticks between checks for a new target (the original used 20 for wisps and 100 for Halloween spirits). */
    protected abstract int retargetInterval();

    /** Continuous trail while flying (wisps, pumpkin spirits) or only bursts (poltergeists). */
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
        // Did not move at all last tick (stuck): choose somewhere else.
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
        this.alpha = Math.min(1.0F, this.alpha + 0.1F);

        double dx = this.targetX - this.x;
        double dy = this.targetY - this.y;
        double dz = this.targetZ - this.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        // The original effectively counted this down by 10 per tick, so retargets come every 0.5-5 seconds.
        this.retargetCooldown -= 10;
        if (!this.hasTarget || distSq < 9.0 || (this.age % this.retargetInterval() == 0 && this.retargetCooldown <= 0)) {
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
            boolean inSoulBlock = this.passesThroughSoulBlocks() && this.level.getBlockState(this.at(this.x, this.y, this.z)).is(BlockTags.SOUL_FIRE_BASE_BLOCKS);
            for (int i = 0; i < 10 * this.speed; i++) {
                if (inSoulBlock) {
                    this.level.addParticle(ParticleTypes.SOUL, this.x + this.random.nextGaussian() / 10, this.y + this.random.nextGaussian() / 10,
                            this.z + this.random.nextGaussian() / 10, this.random.nextGaussian() / 20, this.random.nextGaussian() / 20, this.random.nextGaussian() / 20);
                } else {
                    this.spawnSpark();
                }
            }
        }

        this.moveSpirit(this.xd, this.yd, this.zd);

        if (this.random.nextInt(this.ambientSoundChance()) == 0) {
            this.level.playLocalSound(this.x, this.y, this.z, this.ambientSound(), SoundSource.AMBIENT, 1.0F, this.ambientPitch(), true);
        }

        // Trapped inside a block for over a second (despite drifting out freely): vanish in a burst.
        this.ticksInSolid = this.isInsideSolid() ? this.ticksInSolid + 1 : 0;
        if (this.ticksInSolid > 25) {
            this.die();
        }
    }

    private void pickTarget() {
        // Like the original, aim about ten blocks away in any direction; unlike it, keep trying until the target is not
        // inside a solid block, instead of flying on toward the old target for up to five seconds.
        for (int attempt = 0; attempt < 8; attempt++) {
            this.targetX = this.x + this.random.nextGaussian() * 10;
            this.targetY = this.y + this.random.nextGaussian() * 10;
            this.targetZ = this.z + this.random.nextGaussian() * 10;
            BlockState state = this.level.getBlockState(this.at(this.targetX, this.targetY, this.targetZ));
            boolean blocked = !state.getCollisionShape(this.level, this.scratchPos).isEmpty()
                    && !(this.passesThroughSoulBlocks() && state.is(BlockTags.SOUL_FIRE_BASE_BLOCKS));
            if (!blocked) {
                break;
            }
            if (attempt == 7) {
                // Hemmed in: rise.
                this.targetX = this.x;
                this.targetY = this.y + 4.0;
                this.targetZ = this.z;
            }
        }
        this.hasTarget = true;
        this.ticksBlocked = 0;
        this.speed = rollSpeed();
        this.retargetCooldown = this.random.nextInt((int) (100 / this.speed));
    }

    /** Inside a block with collision (soul sand and soul soil do not count for spirits that glide through them). */
    private boolean isInsideSolid() {
        BlockState state = this.level.getBlockState(this.at(this.x, this.y, this.z));
        if (this.passesThroughSoulBlocks() && state.is(BlockTags.SOUL_FIRE_BASE_BLOCKS)) {
            return false;
        }
        return !state.getCollisionShape(this.level, this.scratchPos).isEmpty();
    }

    private void moveSpirit(double dx, double dy, double dz) {
        boolean intoSoul = this.passesThroughSoulBlocks()
                && this.level.getBlockState(this.at(this.x + dx, this.y + dy, this.z + dz)).is(BlockTags.SOUL_FIRE_BASE_BLOCKS);
        if (intoSoul || this.isInsideSolid()) {
            // Collision cannot push a particle out of a block it is already inside, so drift freely (and upward) until
            // clear. This is what kept spirits stuck in jack o'lanterns, skulls and walls.
            this.setPos(this.x + dx, this.y + dy + (intoSoul ? 0.0 : 0.05), this.z + dz);
            return;
        }
        double beforeX = dx;
        double beforeZ = dz;
        this.move(dx, dy, dz);
        // Mostly blocked for half a second (pressed against a wall or ceiling): choose somewhere else.
        double wanted = dx * dx + dy * dy + dz * dz;
        double moved = (this.x - this.xo) * (this.x - this.xo) + (this.y - this.yo) * (this.y - this.yo) + (this.z - this.zo) * (this.z - this.zo);
        this.ticksBlocked = wanted > 1.0E-4 && moved < wanted * 0.1 ? this.ticksBlocked + 1 : 0;
        if (this.ticksBlocked > 10) {
            this.pickTarget();
        }
        // Like the original, a blocked horizontal axis loses its speed.
        if (Math.abs(this.x - this.xo - beforeX) > 1.0E-7) {
            this.xd = 0.0;
        }
        if (Math.abs(this.z - this.zo - beforeZ) > 1.0E-7) {
            this.zd = 0.0;
        }
    }

    private void die() {
        for (int i = 0; i < 25; i++) {
            this.spawnSpark();
            this.level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, this.deathBlock()),
                    this.x + this.random.nextGaussian() / 10, this.y + this.random.nextGaussian() / 10, this.z + this.random.nextGaussian() / 10,
                    this.random.nextGaussian() / 20, this.random.nextGaussian() / 20, this.random.nextGaussian() / 20);
        }
        SoundEvent[] sounds = this.deathSounds();
        float[] pitches = this.deathPitches();
        for (int i = 0; i < sounds.length; i++) {
            this.level.playLocalSound(this.x, this.y, this.z, sounds[i], SoundSource.AMBIENT, 1.0F, pitches[i], true);
        }
        this.remove();
    }

    /** One trail spark, honouring the particle setting and vanilla's 32-block particle range. */
    private void spawnSpark() {
        Minecraft minecraft = Minecraft.getInstance();
        ParticleStatus status = minecraft.options.particles().get();
        if (status == ParticleStatus.MINIMAL && this.random.nextInt(10) != 0
                || status == ParticleStatus.DECREASED && this.random.nextInt(3) == 0) {
            return;
        }
        Vec3 camera = minecraft.gameRenderer.mainCamera().position();
        if (camera.distanceToSqr(this.x, this.y, this.z) > TRAIL_RANGE_SQ) {
            return;
        }
        ParticleTracker.spawn(new WispEmberParticle(this.level,
                this.x + this.random.nextGaussian() / 15, this.y + this.random.nextGaussian() / 15, this.z + this.random.nextGaussian() / 15,
                this.trailRed, this.trailGreen, this.trailBlue, this.trailRedStep, this.trailGreenStep, this.trailBlueStep));
    }
}
