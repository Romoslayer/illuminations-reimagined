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
package io.github.illuminationsreimagined.fabric;

import io.github.illuminationsreimagined.IlluminationsReimagined;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleGroupRegistry;
import io.github.illuminationsreimagined.particle.SpiritParticleGroup;

public final class IlluminationsReimaginedFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        IlluminationsReimagined.init();
        ClientTickEvents.END_CLIENT_TICK.register(IlluminationsReimagined::onClientTick);
        ParticleGroupRegistry.register(SpiritParticleGroup.RENDER_TYPE, SpiritParticleGroup::new);
    }
}
