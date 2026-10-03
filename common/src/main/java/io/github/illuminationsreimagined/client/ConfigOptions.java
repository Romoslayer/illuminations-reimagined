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
package io.github.illuminationsreimagined.client;

import com.mojang.serialization.Codec;
import io.github.illuminationsreimagined.IlluminationsReimagined;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/** Small factory helpers that turn config fields into vanilla {@link OptionInstance} widgets. */
final class ConfigOptions {
    private ConfigOptions() {
    }

    static String key(String path) {
        return "config." + IlluminationsReimagined.MOD_ID + "." + path;
    }

    static <T> OptionInstance.TooltipSupplier<T> tooltip(String path) {
        return OptionInstance.cachedConstantTooltip(Component.translatable(key(path) + ".tooltip"));
    }

    static OptionInstance<Boolean> bool(String path, boolean value, Consumer<Boolean> setter) {
        return OptionInstance.createBoolean(key(path), tooltip(path), value, setter::accept);
    }

    static OptionInstance<Integer> slider(String path, int min, int max, int value, IntFunction<Component> label, Consumer<Integer> setter) {
        return new OptionInstance<>(key(path), tooltip(path),
                (caption, v) -> Options.genericValueLabel(caption, label.apply(v)),
                new OptionInstance.IntRange(min, max), Math.clamp(value, min, max), setter::accept);
    }

    /** A cycle button over an enum; value names are translated as {@code config.<modid>.value.<name>}. */
    static <E extends Enum<E>> OptionInstance<E> cycle(String path, Class<E> type, E value, Consumer<E> setter) {
        E[] constants = type.getEnumConstants();
        Codec<E> codec = Codec.STRING.xmap(name -> Enum.valueOf(type, name), Enum::name);
        return new OptionInstance<>(key(path), tooltip(path),
                // Cycle buttons prepend the caption themselves, so the label is just the value.
                (caption, v) -> Component.translatable(key("value." + v.name().toLowerCase(Locale.ROOT))),
                new OptionInstance.Enum<>(List.of(constants), codec), value, setter::accept);
    }

    static Component percent(int value) {
        return Component.literal(value + "%");
    }

    static Component number(int value) {
        return Component.literal(Integer.toString(value));
    }
}

