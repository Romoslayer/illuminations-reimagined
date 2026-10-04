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
package io.github.illuminationsreimagined.neoforge;

import io.github.illuminationsreimagined.IlluminationsReimagined;
import io.github.illuminationsreimagined.client.IlluminationsConfigScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleGroupsEvent;
import io.github.illuminationsreimagined.particle.GlowParticleGroup;
import io.github.illuminationsreimagined.particle.SpiritParticleGroup;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

@Mod(value = IlluminationsReimagined.MOD_ID, dist = Dist.CLIENT)
public final class IlluminationsReimaginedNeoForge {
    public IlluminationsReimaginedNeoForge(IEventBus modBus, ModContainer container) {
        IlluminationsReimagined.init();
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new IlluminationsConfigScreen(parent));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> IlluminationsReimagined.onClientTick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener((TagsUpdatedEvent.ClientPacketReceived event) -> IlluminationsReimagined.onClientTagsUpdated());
        modBus.addListener((RegisterParticleGroupsEvent event) -> {
            event.register(SpiritParticleGroup.RENDER_TYPE, SpiritParticleGroup::new);
            event.register(GlowParticleGroup.RENDER_TYPE, GlowParticleGroup::new);
        });
    }
}
