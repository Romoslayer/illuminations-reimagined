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
- All original artwork (All Rights Reserved), replaced by new original textures and a new icon.

### Changed
- Ported to Minecraft 26.2 and 26.3 (unobfuscated, Mojang names) with a shared codebase for Fabric and NeoForge.
- New mod ID `illuminations_reimagined`, package `io.github.illuminationsreimagined`, name and icon.
- Config is now `config/illuminations_reimagined.json`: typed, validated and self-healing.
- In-game settings screen built from vanilla option widgets, with a per-biome sub-screen and a reset button. Opens from Mod Menu (optional) on Fabric and from the Mods screen on NeoForge.
- Biome rules use biome tags (vanilla and `c:` conventions) instead of `Biome.Category` and pre-1.18 biome IDs.
- Particles are created directly instead of being registered as particle types.
- Every effect keeps the original's behaviour: where and how often it spawns, its size, colours, blinking, movement and lifetime, the default per-biome settings and the night window. Will o' wisps, pumpkin spirits and poltergeists are 3D heads that face their direction of travel, as in the original. Poltergeists from undead deaths use the client's death event.
- New options, with defaults that match the original: autumn firefly colours (off), light attraction for fireflies, separate switches for each spirit source, petal bursts and prismarine crystals, separate seasonal settings for the eyes and the spirits, a count cap per effect, and a switch per dimension (all on; modded dimensions are listed while you are in them). Spirits stay dormant until their textures exist.
- Particles left far behind the player (teleports, fast travel) are removed instead of ticking until they expire.
- With shader packs, the light effects (fireflies, glowworms, plankton, eyes, prismarine crystals and spirit trail sparks) are drawn like other glowing things, so packs such as Complementary give them bloom. Without shaders they look exactly as before.

### Fixed
- Fireflies never dying: when they faded out, the blink picked a new brightness, so they lived forever.
- Fireflies teleporting onto light sources, and being drawn to unlit blocks and to lights behind walls.
- Fireflies diving to y≈0 when more than 20 blocks above the ground (0 was used as "not found"), and flying toward the world origin before their first retarget (spirits too).
- Spawning in unloaded chunks and outside the build height.
- Excessive per-tick work (about 1,300 biome registry lookups per tick) and per-frame allocations.
- No upper bound on particle counts. Every effect now has a configurable cap, and counts survive world and dimension changes, disconnects and resource reloads.
- Glowworms allocating `new Random()` every tick, searching for ceilings only up to y=255, and never appearing in modern noise caves.
- Plankton that left the water (or spawned in a puddle) rising into the sky forever.
- Eyes in the dark running game logic in render code.
- The chorus petal hook breaking.
- Poltergeists from undead deaths never appearing (the original spawned them on the server, where particles do nothing).
- Spirits, fireflies and plankton freezing for good after brushing a floor or ceiling (vanilla's particle collision stops a particle permanently once a vertical move is blocked); stuck spirits spun on the spot. Spirit models are now centred in their hitbox, so they no longer sink halfway into the block they touch.
