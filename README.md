# Minor Lord

Minecraft **26.3**, Fabric, Java **25**. Entirely server-side: vanilla clients need no mod or resource pack.

Village automation is implemented and installed on the local development server. This is a development build; [runtime status](docs/runtime-status.md) distinguishes tested behavior, implementation defaults and remaining limitations. Older checkpoint documents retain design history.

## Local use

Connect from this Windows computer to **127.0.0.1:25578** with a vanilla 26.3 client.

1. Put `village` on a vanilla sign's first line and `60x60` on the second. Values are half extents: this defines 120 by 120 blocks around the sign. `25x25` defines 50 by 50. Editing the sign updates the declaration.
2. Four-block-high stone-brick boundary walls build gradually on safe loaded terrain. Structures, fluids, living entities and ambiguous overlap positions are skipped. Village declarations survive chunk unload/restart and are not land-protection claims.
3. Ordinary professions claim their workstation natively and work at that claimed jobsite. Extra jobs use whole name tags normalized to lowercase: `miner`, `chester`, `cook`/`cooker`, `smelter`, `warrior`, `archer`. Marker headers and role names accept all letter cases, so Mine/MINE/mine and Miner/MINER/miner are equivalent. Naming, entity load/spawn, adulthood and actual profession changes queue one role check at tick end; repeated events coalesce. There is no periodic villager role scan. Replacing WORK preserves the existing brain, bed and safety/rest activities. Extra roles are not vanilla professions: a villager named for one drops its vanilla profession and workplace, keeps its bed, and cannot take a profession while the name stays. A managed villager belongs permanently to the village it was first assigned in (the one it stood in); walking away, unloads, restarts and renaming elsewhere never move it, and only removing that village's sign frees it. It only uses its own village's chests and workstations. A new Miner outside village bounds joins the uniquely nearest village in the same dimension; the home determines storage, not mining radius. Each Miner binds to the Mine sign nearest to it when it first starts and keeps it across deliveries, unloads and restarts; it never switches to another village's mine, and only a broken/rewritten sign releases the binding. Jobs operate during vanilla WORK, yielding to fear, hiding, rest and sleep.
4. A vanilla sign attached to a chest labels it (a wall sign on its face, a standing sign on top or a hanging sign under it); a sign attached to anything else, even if it touches the chest, does not. Each nonblank line is a substring of the item registry path, written as players like: case does not matter and spaces/hyphens mean `_` (`Copper` and `Raw Copper` both take `raw_copper`): `iron_ingot`, `wheat`, `bread`, `book`, `enchanted_book`, `coal`, `raw_iron`, `potion`, `blaze_powder`, etc. There are no semantic `Food`/`Fuel` aliases or wildcard syntax. `stone` also accepts `cobblestone`, slabs and other paths containing `stone`. When several labelled chests accept an item, the most specific label wins (`leather_boots` over `leather` for leather boots); distance only decides between equally specific labels, and a broader chest is used only when the specific one is full or unavailable.
5. Put real materials/fuel/returned-container destinations in the village and keep workstations close to the worker/chests. Workers walk to collect inputs, visit workstations and deliver results. Missing/full destinations wait; moving results remain in the villager's saved inventory. Chester sorts unlabelled source chests, moves an item out of a `garbage` chest once a chest labelled for it exists, moves items from a broad chest to a newer more specific one (`copper` to `raw_copper`), and wakes on actual item additions or removals (a removal frees room in a full destination). Only changed chests are compared at tick end; unchanged/rearrangement/rolled-back changes do not wake it. Initial availability and filter-sign changes also allow replanning. Unopened natural loot chests are not unpacked by indexing.
6. For Miner, put `mine` on a sign's first line and a positive maximum **radius** on line two. The sign must be inside the Miner's village; the radius may reach beyond it. Mining starts one block below the sign, digs a corridor at least two wide and three high outward for ten blocks, then expands in a spiral with two solid blocks between rings, plus a straight two-wide spoke along the mine direction through every ring. Trips home and back use the spoke, and progress survives restarts. It seals before it digs: every open neighbor outside the corridor (cave air, water, lava, torches, grass) is closed with stone before a block is broken, and mined ore holes are filled again. Walking back it breaks anything that now blocks the tunnel. It never breaks chests, signs, other block entities or unbreakable blocks: it walks through signs and skips a cell blocked by a chest. A full inventory sends it to deliver. It uses a plain unbreakable diamond pickaxe at native mining speed, collects actual drops and uses carried ordinary stone for tunnel support. Before opening any block it fills every loose water/lava within two blocks with carried stone (liquid that flowed into the tunnel included), waits at unloaded terrain and never force-loads a radius-sized area.

## Implemented roles

| Role | Work |
|---|---|
| Miner / Chester | Native-speed tunnel/ore mining and real chest sorting/delivery |
| Cook / Smelter | Actual crafting/cooking and ingot furnace recipes, real fuel and native timing |
| Armorer / Toolsmith / Weaponsmith | Actual crafting and smithing, consuming real templates and upgrade materials |
| Leatherworker / Fletcher / Cartographer | Real leather-input products, ranged gear/arrows, and blank maps |
| Mason | Actual stonecutter recipes |
| Librarian | One normal book becomes one uniformly selected registered non-curse enchantment book, level one, at a lectern |
| Cleric | Actual brewing recipes, real bottles/reagents/fuel and native brewing time |
| Farmer | Native farming retained; surplus delivery, seed/root/breeding reserves, managed bread crafting disabled |
| Shepherd | Nonlethal native shearing and milk using a real bucket |
| Butcher | Cow/pig/chicken/rabbit and previously sheared sheep; protects named animals, retains at least eight adults per species; at most one cull per worker per five minutes |
| Warrior / Archer | Random vanilla weapons/armor with compatible enchantments; hostile targets only, arrows also cannot hit players/passive mobs |
| Fisherman | Walks to dry shore beside loaded still water and fishes with the vanilla fishing loot table (no bobber entity, so no treasure); catch goes to `cod`/`salmon`/etc. or `garbage` chests |

Created role tools/armor use the native unbreakable component. Native trade previews cannot replace or clear worker equipment. Existing gear is retained, except a Miner swaps a held non-pickaxe into carried inventory (or drops the remainder if full) and equips its pickaxe. Crafted products remain normal items. Archer uses real arrows and currently a bow; Warrior uses swords/axes. Current armor pools are leather/chain/iron/gold/diamond/netherite.

Start thresholds are five items per product for equipment/leather/fletcher/cartographer producers, and five actual max stacks for Mason. These are not reservations or hard output caps. Cook and roles without an agreed threshold have no invented limit.

## Build and verification

With JDK 25:

```powershell
.\gradlew.bat build
```

Main artifact: `build/libs/minor-lord-0.1.0-dev.jar`. Gradle 9.7.1, Loom 1.18.4, Loader 0.19.5 and Fabric API 0.163.0+26.3 are pinned. No license has been selected.

The build passes 84 tests using actual Minecraft/Fabric recipe/tag/component and transaction APIs. Separate local world probes verified real mining beyond village bounds, outside-village naming/profession release, addition-only events, transfers, crafting, stonecutting, furnace/brewing timing, librarian/smithing, combat guards, livestock and shore behavior. Naturally ticking Chester navigation is checked separately. Probes restore temporary fixtures, are built separately by `localIntegrationJar`, and must **never** be installed in `mods`.

Only the local Windows server is installed. There is no Dokploy deployment, automatic Git commit/push, client content, RCON or firewall change. See runtime status for testing limits.
