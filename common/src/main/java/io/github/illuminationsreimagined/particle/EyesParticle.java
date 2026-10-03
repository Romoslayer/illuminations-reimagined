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
import io.github.illuminationsreimagined.config.SeasonalMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * A pair of glowing eyes watching from absolute darkness (a seasonal Halloween effect by default). Ported from the
 * original mod's eyes.
 *
 * <p>The eyes open over three ticks and close over three when their time is up, when light reaches them or when a
 * player comes within {@value #VANISH_DISTANCE} blocks. They vanish at once if the camera has night vision or the
 * effect is turned off. In the original those last checks ran inside the render method; they now run in the tick.</p>
 */
public class EyesParticle extends AmbientParticle {
    public static final double VANISH_DISTANCE = 5.0;
    private static final int FRAMES = 4;
    private final TextureAtlasSprite[] frames = new TextureAtlasSprite[FRAMES];

    public EyesParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, Sprites.get(Sprites.EYES[0]));
        for (int i = 0; i < FRAMES; i++) {
            this.frames[i] = Sprites.get(Sprites.EYES[i]);
        }
        this.quadSize *= 1.0F + this.random.nextFloat();
        this.lifetime = 400 + this.random.nextInt(801);
        this.hasPhysics = true;
        this.alpha = 1.0F;
    }

    @Override
    public ParticleKind kind() {
        return ParticleKind.EYES;
    }

    @Override
    protected void tickAmbient() {
        if (this.shouldDisappear()) {
            this.remove();
            return;
        }
        if (this.age++ < this.lifetime) {
            this.setSprite(this.frames[Math.min(this.age, FRAMES - 1)]);
        } else {
            int closing = this.age - this.lifetime;
            if (closing >= FRAMES - 1) {
                this.remove();
                return;
            }
            this.setSprite(this.frames[FRAMES - 2 - closing]);
        }
        if (this.lifetime > this.age && (this.level.getMaxLocalRawBrightness(this.at(this.x, this.y, this.z)) > 0
                || this.level.getNearestPlayer(this.x, this.y, this.z, VANISH_DISTANCE, false) != null)) {
            // Start closing.
            this.lifetime = this.age;
        }
    }

    private boolean shouldDisappear() {
        if (IlluminationsConfig.get().eyesInTheDark.mode == SeasonalMode.DISABLED) {
            return true;
        }
        Entity camera = Minecraft.getInstance().getCameraEntity();
        return camera instanceof LivingEntity living && living.hasEffect(MobEffects.NIGHT_VISION);
    }
}
