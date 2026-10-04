/*
 * Illuminations Reimagined - an unofficial continuation of Illuminations
 * Copyright (C) 2026 Romoslayer
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
package io.github.illuminationsreimagined.fabric.gametest;

import io.github.illuminationsreimagined.config.IlluminationsConfig;
import io.github.illuminationsreimagined.config.SpawnRate;

/**
 * Settings for scenes placed by hand: nothing spawns on its own, but effects stay on, so the placed ones live. (The
 * master switch would remove them on their first tick.)
 */
final class HandPlacedOnly {
    private HandPlacedOnly() {
    }

    static void apply(IlluminationsConfig config) {
        config.density = 0; // fireflies, glowworms, plankton, Soul Sand Valley wisps
        config.eyesInTheDark.rate = SpawnRate.DISABLED;
        config.willOWisps.fromSoulLanterns = false;
        config.chorusPetals.multiplier = 0;
        config.chorusPetals.burstOnBreak = false;
        config.prismarineCrystals.enabled = false;
        config.halloweenSpirits.fromJackOLanterns = false;
        config.halloweenSpirits.fromSkulls = false;
        config.halloweenSpirits.fromUndeadDeaths = false;
    }
}
