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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

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
        if (this.age % 20 == 0 && this.isFarFromCamera()) {
            // The player moved away (teleport, fast travel): drop it instead of ticking it and counting it against the cap.
            this.remove();
            return;
        }
        this.tickAmbient();
    }

    private boolean isFarFromCamera() {
        double limit = IlluminationsConfig.get().spawnRadius * 1.5 + 16.0;
        Vec3 camera = Minecraft.getInstance().gameRenderer.mainCamera().position();
        double dx = this.x - camera.x;
        double dz = this.z - camera.z;
        return dx * dx + dz * dz > limit * limit;
    }

    /** Per-tick behaviour. Previous-position bookkeeping has already been done. */
    protected abstract void tickAmbient();

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
