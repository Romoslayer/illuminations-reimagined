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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * A pair of glowing eyes watching from absolute darkness (a seasonal Halloween effect by default).
 *
 * <p>The eyes open over a few frames, blink occasionally, and close when light reaches them, when the player comes
 * close, or when the player has night vision. In the original those night-vision/config checks ran inside the render
 * method; they now run in the tick.</p>
 */
public class EyesParticle extends AmbientParticle {
    public static final double VANISH_DISTANCE = 5.0;
    private static final int FRAMES = 4;

    private final TextureAtlasSprite[] frames = new TextureAtlasSprite[FRAMES];
    /** 0 = closed, FRAMES - 1 = fully open. */
    private int openness;
    private boolean closing;
    private int blinkTimer;

    public EyesParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, Sprites.get(Sprites.EYES[0]));
        for (int i = 0; i < FRAMES; i++) {
            this.frames[i] = Sprites.get(Sprites.EYES[i]);
        }
        this.quadSize = 0.2F + this.random.nextFloat() * 0.12F;
        this.lifetime = 400 + this.random.nextInt(801);
        this.hasPhysics = false;
        this.alpha = 1.0F;
        this.blinkTimer = 60 + this.random.nextInt(200);
    }

    @Override
    public ParticleKind kind() {
        return ParticleKind.EYES;
    }

    @Override
    protected void tickAmbient() {
        if (this.age++ >= this.lifetime || this.shouldVanish()) {
            this.closing = true;
        }

        if (this.closing) {
            if (this.openness == 0) {
                this.remove();
                return;
            }
            this.openness--;
        } else if (this.blinkTimer-- <= 0) {
            // A quick blink: snap shut, then reopen over the next frames.
            this.openness = 0;
            this.blinkTimer = 80 + this.random.nextInt(240);
        } else if (this.openness < FRAMES - 1 && this.age % 2 == 0) {
            this.openness++;
        }
        this.setSprite(this.frames[this.openness]);
    }

    private boolean shouldVanish() {
        if (!WorldConditions.isHalloween(IlluminationsConfig.get().eyesInTheDark.mode)) {
            return true;
        }
        if (this.age % 5 != 0) {
            return false;
        }
        if (this.level.getMaxLocalRawBrightness(this.at(this.x, this.y, this.z)) > 0) {
            return true;
        }
        Player player = Minecraft.getInstance().player;
        return player != null
                && (player.hasEffect(MobEffects.NIGHT_VISION)
                || player.distanceToSqr(this.x, this.y, this.z) < VANISH_DISTANCE * VANISH_DISTANCE);
    }
}
