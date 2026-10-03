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

import io.github.illuminationsreimagined.config.IlluminationsConfig;
import io.github.illuminationsreimagined.world.WorldConditions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Quaternionf;

import java.awt.Color;

/**
 * A firefly: a softly blinking light that wanders a few blocks above the ground at night.
 *
 * <p>Rewritten rather than ported. Bugs in the original implementation that this fixes:</p>
 * <ul>
 *   <li>Light attraction <em>teleported</em> the particle onto the light source on every other retarget, because
 *       the light search never returned "nothing found". Attraction now only steers, and only toward real light
 *       (block light ≥ {@value #MIN_ATTRACTING_LIGHT}) in line of sight.</li>
 *   <li>Movement stopped completely once the particle was inside its target's block, and targets only changed on
 *       world ticks divisible by 20, so fireflies froze in mid-air. Steering is now continuous.</li>
 *   <li>The ground search used {@code 0} as its "not found" value, which is a valid height since 1.18, and
 *       fireflies far above the ground dived toward y = 0..4.</li>
 *   <li>{@code random.nextInt() % 100} produced negative cooldowns, causing retargets every tick.</li>
 *   <li>Several allocations per tick and per rendered frame were removed.</li>
 * </ul>
 */
public class FireflyParticle extends AmbientParticle {
    private static final int MIN_ATTRACTING_LIGHT = 8;
    private static final double MAX_SPEED = 0.06;
    private static final double STEER = 0.006;
    private static final int GROUND_SEARCH_DEPTH = 12;
    private static final float FADE_STEP = 0.04F;

    private final TextureAtlasSprite coreSprite;
    private final float maxHeightAboveGround;
    private float brightness;
    private float targetBrightness;
    private float coreAlpha;
    private boolean fadingOut;

    private double targetX;
    private double targetY;
    private double targetZ;
    private int retargetCooldown;
    private int lightSearchCooldown;
    private boolean attractedToLight;
    private double lightX;
    private double lightY;
    private double lightZ;

    public FireflyParticle(ClientLevel level, double x, double y, double z, int rgb) {
        super(level, x, y, z, Sprites.get(Sprites.FIREFLY_GLOW));
        this.coreSprite = Sprites.get(Sprites.FIREFLY_CORE);
        this.quadSize = 0.12F + this.random.nextFloat() * 0.1F;
        this.lifetime = 400 + this.random.nextInt(801);
        this.hasPhysics = true;
        this.friction = 1.0F;
        this.gravity = 0.0F;
        this.maxHeightAboveGround = 2.0F + this.random.nextFloat() * 3.0F;
        this.alpha = 0.0F;
        this.setColor(ARGB.redFloat(rgb), ARGB.greenFloat(rgb), ARGB.blueFloat(rgb));
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.retargetCooldown = 0;
        this.lightSearchCooldown = 20 + this.random.nextInt(40);
    }

    /** Creates a firefly coloured for its surroundings, honouring the rainbow and autumn options. */
    public static FireflyParticle create(ClientLevel level, double x, double y, double z, int biomeColor) {
        IlluminationsConfig.Fireflies cfg = IlluminationsConfig.get().fireflies;
        int rgb = biomeColor;
        float hueShift = (level.getRandom().nextFloat() - 0.5F) * (40.0F / 360.0F);
        if (cfg.rainbow) {
            rgb = Color.HSBtoRGB(level.getRandom().nextFloat(), 0.85F, 1.0F);
            hueShift = 0.0F;
        } else if (WorldConditions.isAutumn(cfg.autumnColors)) {
            rgb = 0xFF9A2E;
        }
        float[] hsb = Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
        rgb = Color.HSBtoRGB(hsb[0] + hueShift, hsb[1], hsb[2]);
        return new FireflyParticle(level, x, y, z, rgb);
    }

    @Override
    public ParticleKind kind() {
        return ParticleKind.FIREFLY;
    }

    @Override
    protected void tickAmbient() {
        IlluminationsConfig.Fireflies cfg = IlluminationsConfig.get().fireflies;

        if (this.age++ >= this.lifetime
                || (!cfg.spawnAlways && !WorldConditions.isNight(this.level))
                || !this.level.isLoaded(this.at(this.x, this.y, this.z))) {
            this.fadingOut = true;
        }

        this.tickBlink();
        if (this.fadingOut && this.brightness <= 0.0F) {
            this.remove();
            return;
        }

        if (cfg.lightAttraction && --this.lightSearchCooldown <= 0) {
            this.lightSearchCooldown = 40 + this.random.nextInt(40);
            this.attractedToLight = this.findLightTarget();
        }

        double dx = this.targetX - this.x;
        double dy = this.targetY - this.y;
        double dz = this.targetZ - this.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        double arriveRadius = this.attractedToLight ? 2.25 : 1.0;
        if (--this.retargetCooldown <= 0 || distSq < arriveRadius) {
            if (this.attractedToLight) {
                this.orbitLightTarget();
            } else {
                this.pickWanderTarget();
            }
            dx = this.targetX - this.x;
            dy = this.targetY - this.y;
            dz = this.targetZ - this.z;
            distSq = dx * dx + dy * dy + dz * dz;
        }

        if (distSq > 1.0E-6) {
            double inv = 1.0 / Math.sqrt(distSq);
            this.xd += dx * inv * STEER;
            this.yd += dy * inv * STEER * 0.6;
            this.zd += dz * inv * STEER;
        }
        // A little drift so motion never looks mechanical.
        this.xd += (this.random.nextFloat() - 0.5F) * 0.002;
        this.yd += (this.random.nextFloat() - 0.5F) * 0.002;
        this.zd += (this.random.nextFloat() - 0.5F) * 0.002;

        double speedSq = this.xd * this.xd + this.yd * this.yd + this.zd * this.zd;
        if (speedSq > MAX_SPEED * MAX_SPEED) {
            double scale = MAX_SPEED / Math.sqrt(speedSq);
            this.xd *= scale;
            this.yd *= scale;
            this.zd *= scale;
        }
        this.xd *= 0.96;
        this.yd *= 0.96;
        this.zd *= 0.96;

        double beforeX = this.x;
        double beforeZ = this.z;
        this.move(this.xd, this.yd, this.zd);
        // Blocked by a wall: choose somewhere else soon instead of pushing against it forever.
        if (Math.abs(this.x - beforeX) < 1.0E-4 && Math.abs(this.z - beforeZ) < 1.0E-4 && (Math.abs(this.xd) > 0.01 || Math.abs(this.zd) > 0.01)) {
            this.retargetCooldown = Math.min(this.retargetCooldown, 5);
            this.attractedToLight = false;
        }
    }

    private void tickBlink() {
        float coreFactor = IlluminationsConfig.get().fireflies.coreBrightness / 100.0F;
        if (this.fadingOut) {
            this.brightness = Math.max(0.0F, this.brightness - FADE_STEP);
        } else {
            if (Math.abs(this.brightness - this.targetBrightness) < FADE_STEP) {
                // Mostly-lit with occasional dim phases reads as "blinking" without strobing.
                this.targetBrightness = this.random.nextFloat() < 0.25F ? this.random.nextFloat() * 0.2F : 0.5F + this.random.nextFloat() * 0.5F;
            }
            this.brightness += Mth.clamp(this.targetBrightness - this.brightness, -FADE_STEP, FADE_STEP);
        }
        this.alpha = this.brightness;
        this.coreAlpha = this.brightness * coreFactor;
    }

    private void pickWanderTarget() {
        double ground = this.findGroundBelow();
        this.targetX = this.x + (this.random.nextDouble() - 0.5) * 12.0;
        this.targetZ = this.z + (this.random.nextDouble() - 0.5) * 12.0;
        double desired = this.y + (this.random.nextDouble() - 0.5) * 3.0;
        this.targetY = Mth.clamp(desired, ground + 0.5, ground + this.maxHeightAboveGround);
        BlockState state = this.level.getBlockState(this.at(this.targetX, this.targetY, this.targetZ));
        if (state.isCollisionShapeFullBlock(this.level, this.scratchPos)) {
            this.targetY += 1.0;
        }
        this.retargetCooldown = 40 + this.random.nextInt(80);
    }

    /** Height of the first collidable surface below the particle, or the particle's own height if none is close. */
    private double findGroundBelow() {
        BlockPos.MutableBlockPos pos = this.at(this.x, this.y, this.z);
        int minY = this.level.getMinY();
        for (int i = 0; i < GROUND_SEARCH_DEPTH && pos.getY() > minY; i++) {
            pos.move(0, -1, 0);
            BlockState state = this.level.getBlockState(pos);
            if (!state.getCollisionShape(this.level, pos).isEmpty() || !state.getFluidState().isEmpty()) {
                return pos.getY() + 1.0;
            }
        }
        // Nothing nearby (high over a ravine or the void): stay at the current height instead of diving.
        return this.y - 1.0;
    }

    /**
     * Samples a handful of nearby positions for block light and keeps the brightest one that the firefly can see.
     * Returns false when there is no worthwhile light, which ends any previous attraction.
     */
    private boolean findLightTarget() {
        int best = MIN_ATTRACTING_LIGHT - 1;
        double bestX = 0;
        double bestY = 0;
        double bestZ = 0;
        for (int i = 0; i < 8; i++) {
            double sx = this.x + (this.random.nextDouble() - 0.5) * 16.0;
            double sy = this.y + (this.random.nextDouble() - 0.5) * 6.0;
            double sz = this.z + (this.random.nextDouble() - 0.5) * 16.0;
            BlockPos.MutableBlockPos pos = this.at(sx, sy, sz);
            if (!this.level.isLoaded(pos)) {
                continue;
            }
            int light = this.level.getBrightness(LightLayer.BLOCK, pos);
            if (light > best) {
                best = light;
                bestX = pos.getX() + 0.5;
                bestY = pos.getY() + 0.5;
                bestZ = pos.getZ() + 0.5;
            }
        }
        if (best < MIN_ATTRACTING_LIGHT) {
            return false;
        }
        Vec3 from = new Vec3(this.x, this.y, this.z);
        Vec3 to = new Vec3(bestX, bestY, bestZ);
        BlockHitResult hit = this.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (hit.getType() != HitResult.Type.MISS && hit.getLocation().distanceToSqr(to) > 2.25) {
            return false;
        }
        this.lightX = bestX;
        this.lightY = bestY;
        this.lightZ = bestZ;
        this.orbitLightTarget();
        return true;
    }

    private void orbitLightTarget() {
        double angle = this.random.nextDouble() * Math.PI * 2.0;
        double radius = 0.8 + this.random.nextDouble() * 1.2;
        this.targetX = this.lightX + Math.cos(angle) * radius;
        this.targetY = this.lightY + (this.random.nextDouble() - 0.3) * 1.5;
        this.targetZ = this.lightZ + Math.sin(angle) * radius;
        this.retargetCooldown = 20 + this.random.nextInt(30);
    }

    @Override
    protected void extractRotatedQuad(QuadParticleRenderState state, Quaternionf rotation, float x, float y, float z, float partialTickTime) {
        if (this.alpha <= 0.0F) {
            return;
        }
        float size = this.getQuadSize(partialTickTime);
        int light = this.getLightCoords(partialTickTime);
        // Coloured halo.
        state.add(this.getLayer(), x, y, z, rotation.x, rotation.y, rotation.z, rotation.w, size,
                this.sprite.getU0(), this.sprite.getU1(), this.sprite.getV0(), this.sprite.getV1(),
                ARGB.colorFromFloat(this.alpha, this.rCol, this.gCol, this.bCol), light);
        // Bright core.
        if (this.coreAlpha > 0.0F) {
            state.add(this.getLayer(), x, y, z, rotation.x, rotation.y, rotation.z, rotation.w, size,
                    this.coreSprite.getU0(), this.coreSprite.getU1(), this.coreSprite.getV0(), this.coreSprite.getV1(),
                    ARGB.colorFromFloat(this.coreAlpha, 1.0F, 1.0F, 1.0F), light);
        }
    }
}
