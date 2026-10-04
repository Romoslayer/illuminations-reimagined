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
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Base class for every particle spawned by the mod.
 *
 * <p>Particles are constructed directly and handed to the particle engine through {@link ParticleTracker#spawn},
 * so nothing is added to the (synced) particle-type registry. This keeps the mod purely client-side on both
 * loaders and on vanilla servers.</p>
 */
public abstract class AmbientParticle extends SingleQuadParticle {
    /** Reused scratch position for block queries during ticking; never handed out. */
    protected final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();
    /** Vanilla skips collision for moves this long or longer (squared), and so do we. */
    private static final double MAX_COLLISION_DISTANCE_SQ = 100.0 * 100.0;
    /**
     * Horizontal distance beyond which a particle is dropped (the player teleported or travelled away). Spawns reach about
     * 180 blocks out (a 50-block Gaussian around vanilla's 32-block display-tick area), so this sits just beyond that.
     */
    private static final double FAR_FROM_CAMERA = 192.0;
    private long lastTrackedTick;

    protected AmbientParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
    }

    public abstract ParticleKind kind();

    final void markTracked(long tick) {
        this.lastTrackedTick = tick;
    }

    final long lastTrackedTick() {
        return this.lastTrackedTick;
    }

    @Override
    public void tick() {
        this.lastTrackedTick = ParticleTracker.currentTick();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;
        if (!ParticleTracker.effectsEnabled(this.level)) {
            // Effects were switched off (globally or for this dimension): vanish quietly, without trails, bursts or sounds.
            this.remove();
            return;
        }
        if (this.age % 20 == 0 && this.isFarFromCamera()) {
            // The player moved away (teleport, fast travel): drop it instead of ticking it and counting it against the cap.
            this.remove();
            return;
        }
        this.tickAmbient();
    }

    private boolean isFarFromCamera() {
        Vec3 camera = Minecraft.getInstance().gameRenderer.mainCamera().position();
        double dx = this.x - camera.x;
        double dz = this.z - camera.z;
        return dx * dx + dz * dz > FAR_FROM_CAMERA * FAR_FROM_CAMERA;
    }

    /** Per-tick behaviour. Previous-position bookkeeping has already been done. */
    protected abstract void tickAmbient();

    /** Light-giving particles are drawn by {@link GlowParticleGroup}, so shader packs make them glow. */
    protected boolean glows() {
        return false;
    }

    @Override
    public ParticleRenderType getGroup() {
        return this.glows() ? GlowParticleGroup.RENDER_TYPE : super.getGroup();
    }

    /**
     * {@link #move} without vanilla's collision latch. Vanilla stops a particle for good the first time a vertical
     * move is fully blocked, so anything that flies under its own power froze (and spirits spun on the spot) as soon as
     * it brushed a floor or ceiling. Particles that steer themselves use this instead.
     */
    protected void moveSteered(double dx, double dy, double dz) {
        double wantedX = dx;
        double wantedY = dy;
        double wantedZ = dz;
        if (this.hasPhysics && (dx != 0.0 || dy != 0.0 || dz != 0.0) && dx * dx + dy * dy + dz * dz < MAX_COLLISION_DISTANCE_SQ) {
            Vec3 allowed = Entity.collideBoundingBox((Entity) null, new Vec3(dx, dy, dz), this.getBoundingBox(), this.level, List.of());
            dx = allowed.x;
            dy = allowed.y;
            dz = allowed.z;
        }
        if (dx != 0.0 || dy != 0.0 || dz != 0.0) {
            this.setBoundingBox(this.getBoundingBox().move(dx, dy, dz));
            this.setLocationFromBoundingbox();
        }
        this.onGround = wantedY != dy && wantedY < 0.0;
        if (wantedX != dx) {
            this.xd = 0.0;
        }
        if (wantedZ != dz) {
            this.zd = 0.0;
        }
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    /** Ambient lights glow regardless of the surrounding light level. */
    @Override
    protected int getLightCoords(float partialTickTime) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    protected BlockPos.MutableBlockPos at(double x, double y, double z) {
        return this.scratchPos.set(x, y, z);
    }

    /** Current position (used by tests and debugging tools). */
    public Vec3 currentPosition() {
        return new Vec3(this.x, this.y, this.z);
    }

    protected boolean isRemoved() {
        return this.removed;
    }
}
