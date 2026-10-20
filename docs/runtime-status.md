# Runtime checkpoint — 2026-10-19

## Current event activation and lowercase naming update

- Explicit names and marker headers normalize to lowercase: `miner`, `chester`, `cook`/`cooker`, `smelter`, `warrior`, `archer`, `village`, `mine`. Following explicit clarification, uppercase/mixed-case role names and marker headers are accepted. No migration or NPC-specific repair was added.
- Name changes, entity spawn/load, adulthood and actual native profession changes enqueue a single coalesced role check. No per-second villager role scan remains. Village declaration changes wake relevant assigned/waiting entities.
- Only WORK behaviors are replaced on the existing brain. Native CORE/REST/PANIC/HIDE and bed memory remain. Babies wait until adulthood; removing the extra-role name restores native WORK.
- Native ShowTradesToPlayer can replace or clear MAINHAND independently of the trading UI. Managed/explicit-role villagers now cannot start that preview or have it overwrite/clear their working equipment. A currently running native preview is stopped before a newly assigned worker receives real equipment, so preview items are not preserved as inventory items.
- A held sword was another concrete mining blocker: the previous rule gave a pickaxe only to empty hands, while mining correctly refused stone without a suitable tool. On Miner assignment, a non-pickaxe moves to carried inventory, spilling only an unaccepted remainder, before the pickaxe is equipped. Existing pickaxes are retained.
- Gradle build: 84 tests, zero failures/errors. Native lifecycle checks pass: duplicate events keep the same session, babies wait, adulthood starts work, rename during REST retains brain/bed/REST, name removal restores native work.
- At 21:31:44–21:31:49 host log time the new event system passed all controlled native-world checks plus fresh natural NPC navigation: Chester delivered 17 iron in 53 ticks; a fresh former farmer named `miner` outside its village walked to the mine and retained three real cobblestone in 50 ticks, with no manual task ticks or actor teleport. Fixtures were restored.
- A valid `mine` sign plus positive numeric radius remains necessary; `village` alone does not specify an excavation start. The user selected radius 64; their existing lowercase sign at (218,62,546) was updated and verified registered, facing east, first tunnel cell (219,61,546). No new excavation location was invented.

- Final installed production JAR SHA-256: `e7d13eebcb5f37ea685491838add1faddd403ffb9a60adeba50a6dac34daffb9`. Built and installed JARs match. At 21:37:25–21:37:26 the final build passed controlled native regressions: sword preserved once, pickaxe equipped, native preview cannot display a sword or clear the pickaxe, lifecycle, transfers, real stone mining, recipes, livestock and combat. The asynchronous navigation rerun skipped because the player was online; no clock changes were made. Prior 21:31 natural navigation evidence covers the same lifecycle behavior before the independent equipment-preview fix.
- Read-only diagnosis at 21:37:26: day tick 1427 (native IDLE), only the secondary lowercase `village` at (148,66,422) active; main sign (225,64,522) is `Village` and inactive under the requested lowercase-only parser. Active miners are assigned to the secondary village, which has no indexed village chests; no mine declarations exist. Several Mine signs have a blank second line, including lowercase `mine` at (218,62,546). Last radius-selection question is pending; no arbitrary radius/location was applied.
- An initial normalization attempt was denied by automatic approval review due to the earlier lowercase-only request and did not execute. The user then explicitly approved Mine/MINE/mine normalization; the authorized retry succeeded. The denial is resolved.
- Latest installed production SHA-256 is `e6e2f6fc1cdcc85908225c839406ea7c5078b4422797b8b0bb5fed92d2ad680b`. At 21:41:32, both actual villages and the radius-64 mine were registered, and all three loaded named miners had WORK sessions with diamond pickaxes. Earlier strict-case diagnostics and hashes above are historical.

## 2026-10-19 evening fixes

- Root cause of natural professions never working in the test arena: the arena spawner forced professions and took POI tickets by hand. Vanilla validation erased the JOB_SITE memory without releasing the ticket, so every workstation stayed occupied with no owner. The spawner now creates unemployed villagers that claim stations natively. The arena waits until all its chunks tick with entities loaded (player nearby) before clearing, so stale workers cannot survive a rebuild. The test server's view/simulation distance is 8 (was 3, which left half the 100x100 arena unticked).
- Mod fix: furnace/brewing-stand claims leaked when a villager left the world without dying (discarded, converted, changed dimension), locking that machine forever. Claims are now released on any permanent removal; a job whose machine is in another dimension is dropped.
- Mod fix: natural professions use their own claimed JOB_SITE as station first; the 4-block search around chests/worker remains for stationless extra roles.
- Mod change (user decision): the most specific matching chest label wins over a closer broad one; distance orders equally specific labels.
- Mod change (user decision): Chester also wakes on item removals, so a full destination that gets room is retried.
- Mod change (user decision): a Miner binds persistently to the Mine sign nearest to it at first start. Previously it re-picked the nearest loaded sign whenever its site unloaded (e.g. while delivering), drifting to other villages' mines.
- Mod fix (user decision): Miner no longer waits forever at water/lava. Before opening any block it displaces every loose liquid within two blocks of it (including liquid that flowed into tunnel cells, on or off the path) with real carried stone, one block per tick, never in the cells it occupies; floor, ceiling and side openings filled with liquid are sealed like air ones. Waterlogged solid blocks are left alone.
- Fisherman now fishes: at dry shore beside still water, holding its rod, it waits a vanilla lure time (100-600 ticks), rolls the vanilla fishing loot table once (no bobber entity, so no open-water treasure) and carries the catch to a matching or `garbage` chest. Catch without a destination stays in its inventory; a full inventory stops fishing.
- The arena Miner room is too small for the spiral (it starts at radius 10 after the 10-block lead-in); spiral geometry is covered by unit tests and needs a real-terrain mine.
- Mod change (user decision): village membership is permanent. Renaming a villager inside another village no longer re-homes it (no villager stealing); only a removed home village frees it.
- Mod fix: deliveries no longer stall on the first carried stack. Workers skip stacks with no matching or `garbage` chest (they stay carried) and put the rest away; Miner/Librarian/Shepherd/Farmer/crafting leftovers also use the `garbage` chest. A Miner whose inventory is full of only unsortable stacks waits instead of shuttling.
- Mod change (user decision): Mine signs only count inside the Miner's own village (radius may extend beyond it). The corridor is two wide and three high; the spiral pitch is four so two solid blocks stay between rings. Walking back out the Miner follows the tunnel cell by cell and breaks anything blocking it; drops that do not fit are left on the tunnel floor.
- Mod fix: a Miner whose tunnel hit a chest, sign or other block entity (or bedrock) waited forever at that cell, and with a full inventory never went to deliver (delivery only started when a drop failed to fit). Protected blocks are never broken: walk-through ones (signs) count as open, solid ones make the column blocked and it is skipped. A full inventory now starts a delivery trip by itself.
- Arena: no bedrock anywhere; the whole arena underground (y50-61) is real stone with scattered ores, so the Miner (radius 40) can spiral under the arena.
- Mod change (user decision): the corridor is never narrower than two blocks (a 2x2 outward brush on the spiral, both corners at diagonal steps; one shared cell list drives digging, sealing and walking back). Seal first, then dig: before any block is broken, every neighbor outside the corridor that is open (air, loose liquid, torch, grass) is closed with real stone; after a step any remaining opening in the walls, floor or ceiling is closed; holes left by mined ore veins are filled again. The tunnel mouth beside the sign is never sealed. Non-solid blocks in wall positions are broken with their drops before the stone goes in.
- Mod change (user decision): besides the spiral, a straight two-wide spoke runs along the mine direction from the sign through every ring (dug each time the spiral crosses that direction). Walking home the Miner backs along the spiral only to its last crossing, then goes straight down the spoke; after delivering it walks straight out along the spoke and resumes at that crossing. Progress is stored on the villager, so this survives restarts.
- Measured (user decision: keep vanilla): villagers that walk past the simulation distance freeze like every vanilla entity (all observed Miner stalls were at the edge chunk with an unticked walk target) and resume when the player comes back. No chunk tickets are added.
- Code cleanup: unused StandardCrafting, ChesterPlanning, ChestStock and InventoryWorkQueue removed with their tests; chest ranking lives only in NearestChestTargets, which VillageInventories.nearest now uses.
- Mod change (user decision): Chester moves an item from a matching chest to a strictly more specific one (early broad labels feed later specific ones; never back, so no bouncing).
- Mod fix: only Miner was barred from vanilla professions; a villager named smelter/cook/chester/warrior/archer could claim a nearby workstation, change look and steal the job site. Every extra role now drops and cannot take a vanilla profession.
- Mod fix: chest labels leaked between neighbors because every sign touching a chest counted (a coal chest also took the gravel label of the chest beside it). Only a sign attached to the chest labels it now.
- Mod change (user decision): chest labels are case-insensitive with spaces/hyphens as `_`. Chester moves items out of a `garbage` chest once a chest labelled for them exists (e.g. coal after adding a `coal` chest).
- Mod fix: a role handoff no longer blocks the new job on carried items that have no destination (e.g. a Miner's swapped sword); such stacks stay in the villager's saved inventory.

Earlier sections below are historical checkpoints; this section supersedes their naming/event behavior.

## Latest Miner / Chester fix

- User requested Chester to wake on item insertion, Miner to work beyond village limits/new outside tags, and Miner to lose its active vanilla profession. Implemented and installed.
- Chester uses a separate addition-event revision. Only changed chests are compared once at tick end; unchanged, removal-only, stack rearrangement and aborted transfers do not fire the insertion event. Initial availability and changed filter configuration also permit replanning. Active hauling completes normally. Generic production inventory revisions remain independent.
- Chest-load callbacks only register/queue positions. They never read inventory in a chunk-load callback, and pending natural loot tables are not unpacked by the event index. Transaction close callbacks are handled without requesting an unsafe current transaction while it is closing. Both issues were caught in live-world verification and fixed.
- A new Miner name triggers role selection immediately. Outside the village it binds to the uniquely nearest same-dimension village; mining still follows the independent Mine sign/radius. Vanilla profession becomes NONE and job/potential jobsite tickets/memories are released, while bed memory and normal safety/rest activities stay. Native workplace acquisition and profession reassignment are blocked while the name remains Miner. Unnamed villagers are unaffected.
- Build: **84 tests, zero failures/errors**. Live checks at 19:41:53 host log time pass outside-village naming, prior-profession removal/reassignment guard, bed/WORK preservation, actual excavation outside home bounds, addition-only/rollback behavior, and unopened native loot-table preservation. Prior production/livestock/combat checks also pass again.
- Fresh NPCs with real Village/Mine signs passed natural ticking at 19:50:09–19:50:11 host log time: Chester physically delivered 17 iron in 35 ticks; a freshly spawned farmer named Miner outside the village walked from (8.5,299,3.5) to (8.5,299,6.2489), became profession NONE, mined real stone and retained three cobblestone in 29 ticks. No manual task stepping or teleporting was used in these tests. NPCs, signs, walls, clock and chunk-force state were restored afterward.
- Earlier asynchronous navigation fixtures lacked a physical Village sign, so normal chunk validation removed their synthetic scope. The fixture was corrected to use real signs; no runtime exception or special-case NPC repair was added for that test failure. Source selection now prefers nearby source chests.
- User explicitly requested clean first-spawn behavior rather than existing-NPC migration/repair code. No UUID-specific correction, legacy migration scan or entity deletion was added. Diagnostic/probe code is a separate source set excluded from the main JAR. The final server is restarted normally without injected probe code.
- Installed JAR matches build, SHA-256 `2a50b588442ebf960701b26fcba4c049c87e1517819ec8c6544536ca61621035`.
- Live diagnosis of the user's named miners showed WORK sessions with diamond pickaxes, but **no registered Mine marker**; another newly named Miner outside bounds lacked a home before this fix. A Mine sign must specify the intended start/depth and numeric radius. Village extent alone does not choose an excavation site. No arbitrary user-world Mine sign was placed.

The older checkpoint below is history; its earlier build hash/test count are superseded above.

This file supersedes older implementation/pending checkpoint claims. Source is local and uncommitted. The public repository is https://github.com/umudik/minor-lord; no license has been selected and no code has been pushed.

## Latest user decisions

- Librarian selects uniformly from every registered enchantment except curses; level one, one plain normal book consumed.
- Warrior/Archer use vanilla weapons/armor and compatible random enchantments, and attack hostile mobs only.
- Butcher handles cows, pigs, chickens, rabbits and previously sheared sheep. Named animals are protected. At least eight adults per species remain, and each worker culls at most one animal per five minutes. Cooldown persists.

## Current runtime

Minecraft 26.3 / Loader 0.19.5 / Fabric API 0.163.0+26.3 / JDK 25.0.4.101. Main mod 0.1.0-dev is installed in the existing local test world's server, listening only on 127.0.0.1:25578 with previously approved EULA/offline whitelist/OP settings. Normal halt/save was used for updates. User umudik has joined with a vanilla client. No Dokploy or Fookie changes.

Installed main JAR equals the built artifact. SHA-256: `ba1d7e89dc1dca65b8d04c215698ebd6081075c97283999f594dbcc5135d34ea`. Final status handshake confirms 26.3 and one real player online. The status probe itself does not log in a player. Production JAR contains no local probe classes.

Village/Mine declarations use native SavedData. Entity home, pending librarian choice, furnace/brewing references and Butcher cooldown use persistent server-only attachments. Materials/results use the villager's actual saved inventory. Tasks reconstruct after load; leftover crafting inputs return through matching delivery before replanning. Villager death drops carried stock and releases machine claims; native machine contents remain real items even when a worker dies.

CHUNK_LOAD only enqueues positions. Block reads run later on the server tick, avoiding a verified chunk-future reentrancy deadlock. Walls inspect at most eight columns per tick and never force-load terrain.

## Verification evidence

- Gradle build: **83 tests, zero failures/errors**.
- Native world checks at **19:17:27–19:17:28 host log time** passed:
  - Extra-role assignment, native WORK, no remote transfer, 17 iron conserved, full target leaves source intact.
  - Three real stone blocks mined at native pickaxe speed; all three cobblestone retained.
  - Nine wheat become three bread; one unused wheat retained.
  - Four stone become eight actual recipe-matching slabs. Literal `stone_slab` also accepts `cobblestone_slab` in this runtime.
  - Raw iron/coal fed to a furnace, native 200-tick processing, one ingot delivered.
  - One plain book consumed at a lectern, one registered non-curse level-one enchanted book delivered.
  - Diamond sword/netherite ingot/upgrade template consumed, one normal netherite sword delivered.
  - Water bottle/nether wart/blaze powder used, native brewing timer, awkward potion delivered. Shared fuel/reagent quantities counted jointly.
  - Random gear supported and enchantments pairwise compatible; melee damages hostile; native arrow consumes real ammunition and cannot hit cows, villagers or online player.
  - One cow death retains eight adults and named cow; cooldown prevents another cull; exact native loot conserved. Unshorn sheep rejected, sheared sheep eligible.
  - Nonlethal shearing, wool delivery, real-bucket milk with live cow.
  - One source-water block accepted and dry-shore native walk target chosen, without fabricated fishing loot.
- Temporary fixtures restored in finally blocks.
- Prior naturally ticking Chester test passed at 18:44:42: 37 ticks of real walking and 17 iron delivered. Latest navigation rerun skipped because a real player was online; no clock/forced-chunk changes were made.

These controlled checks do not exhaustively cover full spiral traversal, every recipe, every natural-profession navigation path, restart during every machine phase, or long-running multiplayer interaction.

## Defaults and remaining limits

- Chest names: registry-path substrings, normalized (case-insensitive, spaces/hyphens as `_`; user decision 2026-10-20); all adjacent sign sides/double-chest halves contribute. Label overlap is intentional. The longest matching label wins (user decision 2026-10-19: `leather_boots` beats a closer `leather`); squared Euclidean distance and coordinate tie-breaks order equally specific labels. These are implementation defaults, not newly attributed user decisions.
- Extra tags override professions; Cook/Cooker aliases accepted. Existing gear retained. Warrior pool: wooden/stone/iron/gold/diamond/netherite swords/axes. Archer: bow. Armor: leather/chain/iron/gold/diamond/netherite per slot. Native compatible random enchanting excludes curses.
- Vanilla fear/rest/sleep takes priority, including combat. A warrior can flee when PANIC/HIDE activates; always-fighting guard behavior would be a separate change.
- Natural professions still require native adult WORK/jobsite state. Stations are searched within four horizontal/two vertical blocks around worker and village chests.
- Native WALK_TARGET refuses to replace foreign movement/safety targets. No teleport. Missing routes/targets wait; custom unreachable-path recovery is not implemented.
- Work uses loaded terrain. Bounded Miner chunk tickets remain undecided; a distant Mine sign may need its chunk loaded when reacquiring a job. No radius-wide force-load.
- Miner uses three-block clearance, plain diamond speed, ordinary construction stone, and a shared 16-block support reserve on unload. Fluids/block entities/unsafe blocks wait. No walked-path map.
- Fisherman searches loaded surface still water, walks to dry shore and fishes with the vanilla fishing loot table (see 2026-10-19 evening fixes). No bobber entity.
- Butcher rolls native loot once, jointly checks targets/carried capacity, and reuses exactly that roll in the same synchronous native death. MOB_DROPS=false disables culling.
- Protection-plugin integration, deployment and always-loaded distant work are outside this local checkpoint.

See [README](../README.md) for use. Earlier files preserve history; stale statements that no role jobs exist are superseded here.
