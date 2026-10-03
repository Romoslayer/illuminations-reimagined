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
 * A firefly: a blinking light that wanders within four blocks of the ground and is drawn to nearby light. Ported from
 * the original mod's firefly.
 *
 * <p>Bugs in the original that this fixes, with the behaviour otherwise kept:</p>
 * <ul>
 *   <li>Fading out (at daybreak or old age) never finished: on reaching zero the blink picked a new brightness, so
 *       fireflies lived forever. They now fade out and are removed.</li>
 *   <li>Light attraction <em>teleported</em> the particle onto the light source on every other retarget, and the
 *       light search always returned a block, even an unlit one or one behind a wall. Fireflies now fly toward the
 *       light, and only toward lit blocks they can see.</li>
 *   <li>The ground search used {@code 0} as its "not found" value, so fireflies more than 20 blocks up dived toward
 *       y = 0..4, far below the ground since 1.18. They now descend 16 to 20 blocks at a time until they find it.</li>
 *   <li>Until the first retarget, fireflies flew toward the world origin.</li>
 *   <li>Several allocations per tick and per rendered frame were removed.</li>
 * </ul>
 */
public class FireflyParticle extends AmbientParticle {
    private static final float BLINK_STEP = 0.05F;
    private static final int GROUND_SEARCH_DEPTH = 20;
    private static final int MAX_HEIGHT = 4;

    private final TextureAtlasSprite coreSprite;
    private float nextAlphaGoal;
    private float coreAlpha;
    private boolean fadingOut;

    private double targetX;
    private double targetY;
    private double targetZ;
    private boolean hasTarget;
    private int retargetCooldown;
    private BlockPos lightTarget;

    public FireflyParticle(ClientLevel level, double x, double y, double z, int rgb) {
        super(level, x, y, z, Sprites.get(Sprites.FIREFLY_GLOW));
        this.coreSprite = Sprites.get(Sprites.FIREFLY_CORE);
        this.quadSize *= 0.25F + this.random.nextFloat() * 0.5F;
        this.lifetime = 400 + this.random.nextInt(801);
        this.hasPhysics = true;
        this.alpha = 0.0F;
        this.setColor(ARGB.redFloat(rgb), ARGB.greenFloat(rgb), ARGB.blueFloat(rgb));
    }

    /** Creates a firefly coloured for its surroundings, honouring the rainbow and autumn options. */
    public static FireflyParticle create(ClientLevel level, double x, double y, double z, int biomeColor) {
        IlluminationsConfig.Fireflies cfg = IlluminationsConfig.get().fireflies;
        if (cfg.rainbow) {
            return new FireflyParticle(level, x, y, z, Color.HSBtoRGB(level.getRandom().nextFloat(), 1.0F, 1.0F));
        }
        int rgb = WorldConditions.isAutumn(cfg.autumnColors) ? 0xFF9A2E : biomeColor;
        // Shift the hue by up to 15 degrees either way.
        float[] hsb = Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
        hsb[0] += (level.getRandom().nextFloat() - 0.5F) * 30.0F / 360.0F;
        return new FireflyParticle(level, x, y, z, Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
    }

    @Override
    public ParticleKind kind() {
        return ParticleKind.FIREFLY;
    }

    @Override
    protected boolean glows() {
        return true;
    }

    @Override
    protected void tickAmbient() {
        IlluminationsConfig.Fireflies cfg = IlluminationsConfig.get().fireflies;
        boolean daylight = !cfg.spawnAlways && !this.level.dimensionType().hasFixedTime() && !WorldConditions.isNight(this.level);
        if (daylight || this.age++ >= this.lifetime) {
            this.fadingOut = true;
        }
        this.tickBlink(cfg);
        if (this.fadingOut && this.alpha <= 0.0F) {
            this.remove();
            return;
        }

        // The original counted this down by 10 whenever the firefly had barely moved, which (as it compared the
        // position with itself) was every tick.
        this.retargetCooldown -= 10;
        double dx = this.targetX - this.x;
        double dy = this.targetY - this.y;
        double dz = this.targetZ - this.z;
        if (!this.hasTarget || (this.level.getGameTime() % 20 == 0 && (dx * dx + dy * dy + dz * dz < 9.0 || this.retargetCooldown <= 0))) {
            this.selectBlockTarget(cfg);
            dx = this.targetX - this.x;
            dy = this.targetY - this.y;
            dz = this.targetZ - this.z;
        }

        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double scale = length > 1.0E-6 ? 0.1 / length : 0.0;
        this.xd = 0.9 * this.xd + 0.1 * dx * scale;
        this.zd = 0.9 * this.zd + 0.1 * dz * scale;
        // Standing on something: hop up off it.
        BlockState below = this.level.getBlockState(this.at(this.x, this.y - 0.1, this.z));
        if (!below.getBlock().isPossibleToRespawnInThis(below)) {
            this.yd = 0.05;
        } else {
            this.yd = 0.9 * this.yd + 0.1 * dy * scale;
        }

        // Hover once inside the target's block, until the next retarget.
        BlockPos.MutableBlockPos targetBlock = this.at(this.targetX, this.targetY + 0.5, this.targetZ);
        if (!targetBlock.equals(BlockPos.containing(this.x, this.y, this.z))) {
            this.moveSteered(this.xd, this.yd, this.zd);
        }
    }

    private void tickBlink(IlluminationsConfig.Fireflies cfg) {
        if (this.fadingOut) {
            this.alpha = Math.max(0.0F, this.alpha - BLINK_STEP);
        } else if (this.alpha > this.nextAlphaGoal - BLINK_STEP && this.alpha < this.nextAlphaGoal + BLINK_STEP) {
            this.nextAlphaGoal = this.random.nextFloat();
        } else if (this.nextAlphaGoal > this.alpha) {
            this.alpha = Math.min(this.alpha + BLINK_STEP, 1.0F);
        } else {
            this.alpha = Math.max(this.alpha - BLINK_STEP, 0.0F);
        }
        this.coreAlpha = this.alpha * cfg.coreBrightness / 100.0F;
    }

    private void selectBlockTarget(IlluminationsConfig.Fireflies cfg) {
        if (this.lightTarget == null) {
            double ground = this.findGround();
            this.targetX = this.x + this.random.nextGaussian() * 10.0;
            this.targetY = Math.min(Math.max(this.y + this.random.nextGaussian() * 2.0, ground), ground + MAX_HEIGHT);
            this.targetZ = this.z + this.random.nextGaussian() * 10.0;
            BlockPos.MutableBlockPos target = this.at(this.targetX, this.targetY, this.targetZ);
            BlockState state = this.level.getBlockState(target);
            if (state.isCollisionShapeFullBlock(this.level, target) && state.isRedstoneConductor(this.level, target)) {
                this.targetY += 1.0;
            }
            if (cfg.lightAttraction) {
                this.lightTarget = this.findMostLitBlockAround();
            }
        } else {
            this.targetX = this.lightTarget.getX() + this.random.nextGaussian();
            this.targetY = this.lightTarget.getY() + this.random.nextGaussian();
            this.targetZ = this.lightTarget.getZ() + this.random.nextGaussian();
            if (this.level.getBrightness(LightLayer.BLOCK, this.lightTarget.above()) > 0 && !this.level.isBrightOutside()) {
                this.lightTarget = this.findMostLitBlockAround();
            } else {
                this.lightTarget = null;
            }
        }
        this.hasTarget = true;
        this.retargetCooldown = this.random.nextInt(100);
    }

    /** Height of the first solid block (or liquid) within 20 blocks below; without one, 20 blocks down. */
    private double findGround() {
        for (int i = 0; i < GROUND_SEARCH_DEPTH; i++) {
            BlockState state = this.level.getBlockState(this.at(this.x, this.y - i, this.z));
            if (!state.getBlock().isPossibleToRespawnInThis(state)) {
                return this.y - i;
            }
        }
        return this.y - GROUND_SEARCH_DEPTH;
    }

    /**
     * The block with the most block light among the 27 around the firefly and 15 random ones nearby, or null when none
     * of them is lit or the firefly cannot see it.
     */
    private BlockPos findMostLitBlockAround() {
        BlockPos.MutableBlockPos pos = this.scratchPos;
        int bestLight = 0;
        int bestX = 0;
        int bestY = 0;
        int bestZ = 0;
        for (int i = 0; i < 27 + 15; i++) {
            if (i < 27) {
                pos.set(this.x + i % 3 - 1, this.y + i / 3 % 3 - 1, this.z + i / 9 - 1);
            } else {
                pos.set(this.x + this.random.nextGaussian() * 10.0, this.y + this.random.nextGaussian() * 10.0, this.z + this.random.nextGaussian() * 10.0);
            }
            int light = this.level.getBrightness(LightLayer.BLOCK, pos);
            if (light > bestLight) {
                bestLight = light;
                bestX = pos.getX();
                bestY = pos.getY();
                bestZ = pos.getZ();
            }
        }
        if (bestLight == 0) {
            return null;
        }
        Vec3 to = new Vec3(bestX + 0.5, bestY + 0.5, bestZ + 0.5);
        BlockHitResult hit = this.level.clip(new ClipContext(new Vec3(this.x, this.y, this.z), to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (hit.getType() != HitResult.Type.MISS && !hit.getBlockPos().equals(new BlockPos(bestX, bestY, bestZ))) {
            return null;
        }
        return new BlockPos(bestX, bestY, bestZ);
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
        // White centre.
        if (this.coreAlpha > 0.0F) {
            state.add(this.getLayer(), x, y, z, rotation.x, rotation.y, rotation.z, rotation.w, size,
                    this.coreSprite.getU0(), this.coreSprite.getU1(), this.coreSprite.getV0(), this.coreSprite.getV1(),
                    ARGB.colorFromFloat(this.coreAlpha, 1.0F, 1.0F, 1.0F), light);
        }
    }
}
