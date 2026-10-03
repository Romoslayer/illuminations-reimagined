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
package io.github.illuminationsreimagined.config;

/**
 * User-facing spawn frequency. The multiplier scales a per-effect base chance; the ratios (0.2 / 1 / 2.5) match the
 * original mod's per-effect spawn-rate tables.
 */
public enum SpawnRate {
    DISABLED(0.0F),
    LOW(0.2F),
    MEDIUM(1.0F),
    HIGH(2.5F);

    public final float multiplier;

    SpawnRate(float multiplier) {
        this.multiplier = multiplier;
    }
}
