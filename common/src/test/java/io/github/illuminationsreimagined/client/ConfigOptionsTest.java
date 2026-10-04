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
package io.github.illuminationsreimagined.client;

import io.github.illuminationsreimagined.config.IlluminationsConfig;
import net.minecraft.client.OptionInstance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The settings screen must show a cap the config file accepts, not quietly lower it. */
class ConfigOptionsTest {
    @Test
    void sliderShowsTheHighestAcceptedCap() {
        for (int max : new int[]{IlluminationsConfig.MAX_EYES, IlluminationsConfig.MAX_WISPS, IlluminationsConfig.MAX_HALLOWEEN_SPIRITS,
                IlluminationsConfig.MAX_CHORUS_PETALS}) {
            int[] written = {-1};
            OptionInstance<Integer> slider = ConfigOptions.slider("maxCount", 0, max, max, ConfigOptions::number, v -> written[0] = v);
            assertEquals(max, slider.get());
            assertEquals(-1, written[0], "showing the value does not write it back");
        }
    }
}
