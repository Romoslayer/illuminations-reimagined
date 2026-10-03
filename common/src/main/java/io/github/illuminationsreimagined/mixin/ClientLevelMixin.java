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
package io.github.illuminationsreimagined.mixin;

import io.github.illuminationsreimagined.spawn.AmbientSpawner;
import io.github.illuminationsreimagined.spawn.BlockAmbience;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks for ambient effects: every random display-tick sample feeds {@link BlockAmbience} (block-driven effects) and
 * {@link AmbientSpawner} (biome-driven effects), and block destruction feeds petal bursts.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "doAnimateTick", at = @At("TAIL"))
    private void illuminations_reimagined$animateTick(int xt, int yt, int zt, int r, RandomSource animateRandom,
                                                      @Nullable Block markerParticleTarget, BlockPos.MutableBlockPos pos, CallbackInfo ci) {
        ClientLevel level = (ClientLevel) (Object) this;
        BlockAmbience.onAnimateTick(level, pos, level.getBlockState(pos), animateRandom);
        AmbientSpawner.onAnimateTick(level, pos);
    }

    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"))
    private void illuminations_reimagined$destroyBlock(BlockPos pos, BlockState state, CallbackInfo ci) {
        BlockAmbience.onBlockDestroyed((ClientLevel) (Object) this, pos, state);
    }
}
