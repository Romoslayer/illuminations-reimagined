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
package io.github.illuminationsreimagined.fabric.gametest;

import io.github.illuminationsreimagined.IlluminationsReimagined;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Opt-in shader setup for compatibility runs ({@code -Pcompat=iris -PshaderPack=<zip>}): copies the pack into Iris's
 * shader pack folder and turns it on before the other tests run. The test client's game directory is reset between
 * runs, so this has to happen from inside the game. Iris is reached by reflection so the tests build without it.
 */
public class ShaderPackClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        String pack = System.getProperty("illuminations_reimagined.shaderPack", "");
        if (pack.isEmpty()) {
            return;
        }
        if (!FabricLoader.getInstance().isModLoaded("iris")) {
            throw new AssertionError("-PshaderPack needs Iris: add -Pcompat=iris");
        }
        String loaded = context.computeOnClient(client -> {
            try {
                Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
                Path source = Path.of(pack);
                Path directory = (Path) iris.getMethod("getShaderpacksDirectory").invoke(null);
                Files.createDirectories(directory);
                Files.copy(source, directory.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                Object config = iris.getMethod("getIrisConfig").invoke(null);
                config.getClass().getMethod("setShaderPackName", String.class).invoke(config, source.getFileName().toString());
                config.getClass().getMethod("setShadersEnabled", boolean.class).invoke(config, true);
                config.getClass().getMethod("save").invoke(config);
                iris.getMethod("reload").invoke(null);
                Method currentPack = iris.getMethod("getCurrentPack");
                return ((java.util.Optional<?>) currentPack.invoke(null)).isPresent() ? source.getFileName().toString() : null;
            } catch (ReflectiveOperationException | java.io.IOException e) {
                throw new RuntimeException("Could not load the shader pack " + pack, e);
            }
        });
        if (loaded == null) {
            throw new AssertionError("Iris did not load the shader pack " + pack);
        }
        IlluminationsReimagined.LOGGER.info("[shaders] running with shader pack {}", loaded);
        context.waitTicks(20);
    }
}
