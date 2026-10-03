# Changelog

## 0.1.0 — unreleased (Minecraft 26.2 / 26.3, Fabric + NeoForge)

First version of Illuminations Reimagined, an unofficial continuation of Illuminations 1.10.2 (Ladysnake, GPL-3.0-or-later).

### Removed
- All donor cosmetics: auras, overheads (crowns, horns, halos, tiaras, wreaths), pets (pride hearts, Jacko,
  lanterns, player wisps), the "jeb" rainbow shader and the Satin integration.
- Cosmetics dashboard integration and the HTTP fetch of donor data, which ran every time a player entity loaded.
- Self-updater, bundled `illuminations-uninstaller.jar`, and the update, donation and greeting screens and toasts.
- Override of Minecraft's `particle.fsh` shader; Canvas material maps; built-in "lowerres"/"pixelaccurate" packs.
- Unused embers effect; unreferenced `lib/javax.*.jar` binaries.
- All original artwork (All Rights Reserved), replaced by new generated textures.

### Changed
- Ported to Minecraft 26.2 and 26.3 (unobfuscated, Mojang names) with a shared codebase for Fabric and NeoForge.
- New mod ID `illuminations_reimagined`, package `io.github.illuminationsreimagined`, name and icon.
- Config is now `config/illuminations_reimagined.json`: typed, validated and self-healing.
- Biome rules use biome tags (vanilla and `c:` conventions) instead of `Biome.Category` and pre-1.18 biome IDs.
- Particles are created directly instead of being registered as particle types.
- Will o' wisps are animated billboards with ember trails instead of entity models.

### Fixed
- Fireflies teleporting onto light sources (the light search never returned "none").
- Fireflies freezing mid-air (movement was skipped inside the target block, and targets only changed on ticks divisible by 20).
- Fireflies diving to y≈0 when far above the ground (0 was used as "not found").
- Negative retarget cooldowns (`nextInt() % 100`).
- Light attraction through walls (light targets now need line of sight).
- Spawning in unloaded chunks and outside the build height.
- Excessive per-tick work (about 1,300 biome registry lookups per tick) and per-frame allocations.
- No upper bound on particle counts. Every effect now has a configurable cap, and counts survive world and dimension changes, disconnects and resource reloads.
- Glowworms allocating `new Random()` every tick, searching for ceilings only up to y=255, and never appearing in modern noise caves.
- Plankton being pushed upward out of water.
- Eyes in the dark appearing in large numbers in the Nether and End, and running game logic in render code.
- Will o' wisps from Soul Sand Valley spawning inside soul sand blocks.
- Chorus petals always blowing in the same direction, and the petal hook breaking.
