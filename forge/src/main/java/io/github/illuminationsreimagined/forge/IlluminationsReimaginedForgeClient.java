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
package io.github.illuminationsreimagined.forge;

import io.github.illuminationsreimagined.IlluminationsReimagined;
import io.github.illuminationsreimagined.client.IlluminationsConfigScreen;
import io.github.illuminationsreimagined.particle.GlowParticleGroup;
import io.github.illuminationsreimagined.particle.SpiritParticleGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Client setup, called from {@link IlluminationsReimaginedForge} on the physical client only. */
final class IlluminationsReimaginedForgeClient {
    private IlluminationsReimaginedForgeClient() {
    }

    static void init(FMLJavaModLoadingContext context) {
        IlluminationsReimagined.init();
        context.getContainer().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(parent -> new IlluminationsConfigScreen(parent)));
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> IlluminationsReimagined.onClientTick(Minecraft.getInstance()));
        // Also fires on the integrated server's thread for its own tags; only the client's copy matters here.
        TagsUpdatedEvent.BUS.addListener(event -> {
            if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED) {
                IlluminationsReimagined.onClientTagsUpdated();
            }
        });
        // Forge's ParticleEngine patch: custom particle groups for the spirits' 3D heads and the glowing particles.
        ParticleEngine.registerParticleGroup(SpiritParticleGroup.RENDER_TYPE, SpiritParticleGroup::new);
        ParticleEngine.registerParticleGroup(GlowParticleGroup.RENDER_TYPE, GlowParticleGroup::new);
    }
}
