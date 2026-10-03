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
package io.github.illuminationsreimagined.spawn;

import io.github.illuminationsreimagined.config.IlluminationsConfig;
import io.github.illuminationsreimagined.particle.ParticleKind;
import io.github.illuminationsreimagined.particle.ParticleTracker;
import io.github.illuminationsreimagined.particle.PoltergeistParticle;
import io.github.illuminationsreimagined.particle.PumpkinSpiritParticle;
import io.github.illuminationsreimagined.particle.Sprites;
import io.github.illuminationsreimagined.world.WorldConditions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Halloween spirits: pumpkin spirits from jack o'lanterns, poltergeists from skeleton skulls and from undead that die
 * at night. Only active at night while the Halloween setting is on, and dormant until their textures exist.
 */
public final class HalloweenSpirits {
    private HalloweenSpirits() {
    }

    private static boolean active(ClientLevel level, IlluminationsConfig config) {
        return config.enabled && WorldConditions.isNight(level) && WorldConditions.isHalloween(config.halloweenSpirits.mode);
    }

    static void fromJackOLantern(ClientLevel level, BlockPos pos, RandomSource random, IlluminationsConfig config) {
        if (config.halloweenSpirits.fromJackOLanterns && random.nextInt(100) == 0 && active(level, config)
                && ParticleTracker.hasRoom(ParticleKind.PUMPKIN_SPIRIT) && Sprites.isAvailable(Sprites.PUMPKIN_SPIRIT[0])) {
            ParticleTracker.spawn(new PumpkinSpiritParticle(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        }
    }

    static void fromSkull(ClientLevel level, BlockPos pos, RandomSource random, IlluminationsConfig config) {
        if (config.halloweenSpirits.fromSkulls && random.nextInt(100) == 0 && active(level, config)) {
            spawnPoltergeist(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }
    }

    /** Called on the client when an entity's death animation starts. */
    public static void onEntityDeath(LivingEntity entity) {
        if (!(entity.level() instanceof ClientLevel level)) {
            return;
        }
        IlluminationsConfig config = IlluminationsConfig.get();
        if (config.halloweenSpirits.fromUndeadDeaths && entity.typeHolder().is(EntityTypeTags.UNDEAD)
                && level.getRandom().nextInt(5) == 0 && active(level, config)
                && spawnPoltergeist(level, entity.getX(), entity.getEyeY(), entity.getZ())) {
            level.playLocalSound(entity.getX(), entity.getEyeY(), entity.getZ(), SoundEvents.VEX_CHARGE, SoundSource.AMBIENT, 1.0F, 0.8F, false);
        }
    }

    private static boolean spawnPoltergeist(ClientLevel level, double x, double y, double z) {
        if (!ParticleTracker.hasRoom(ParticleKind.POLTERGEIST) || !Sprites.isAvailable(Sprites.POLTERGEIST[0])) {
            return false;
        }
        return ParticleTracker.spawn(new PoltergeistParticle(level, x, y, z));
    }
}
