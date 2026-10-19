Current runtime checkpoint: 2026-10-19, 83 tests, native role jobs installed locally.

See [runtime-status.md](runtime-status.md) for current implementation, latest user decisions, live-world evidence and limits. This file below retains historical checkpoint/design text; older claims that role jobs are absent or these latest decisions are pending are superseded.

---

# Live role milestone — actual native movement verified

The local server now runs persistent Village/Mine declarations, name-tag role activation and actual WORK sessions. Role name matching is whole-name and case-insensitive; named extra roles take precedence over the current vanilla profession. This is a disclosed implementation decision made to fulfill the direct Miner activation request. A role's previous task is paused before replacement; carried items remain in the saved native inventory.

Passed native-world checks on 2026-10-19:
- Miner name assigned MINER and a native unbreakable diamond pickaxe; extra role entered native WORK without a vanilla jobsite.
- Chester could not remotely extract or remotely deliver. At physical arrival, 17 iron ingots were moved with exact conservation. Full target left source intact; already correctly stored stock was left alone.
- Real stone blocks were not removed instantly; three real stone blocks became air at diamond-pickaxe speed and all three cobblestone drops remained in NPC inventory.
- Actual naturally ticking Chester walked from (1.5,311,2.5) to (5.4127,311,2.3400), delivering all 17 iron ingots in 37 native ticks. No teleport or manual stepping drove this navigation test. Test NPC, temporary platform, clock and one-chunk test ticket were restored/removed.

The current source also adds native recipe crafting roles (Cook, Armorer, Leatherworker, Toolsmith, Weaponsmith, Fletcher, blank-map Cartographer), Mason stonecutting, Farmer surplus alongside native harvesting, real fueled furnace Cook/Smelter jobs with persistent machine reference, and nonlethal Shepherd shearing/real-bucket milking. These newer role paths need their individual native-world production checks; a build alone is not proof of their completion.

Implementation assumptions to review: chest labels currently compare literal case-sensitive registry paths, adjacent signs label both halves of a double chest; distance is squared Euclidean with coordinate tie-break. Farmer retains up to64 seeds per seed type and native breeding food quantities (12 roots /3bread). Miner keeps16 total ordinary construction stones, uses a3-high tunnel and six-face same-block ore connectivity at native tool speed. These are technical defaults, not previously recorded user choices. No new client mod, UI or custom registry items.

Remaining: machine/production role world verification, bounded unloaded-work lifecycle, Cleric brewing, Butcher safe herd workflow, Librarian's undecided random eligibility, Warrior/Archer equipment choice, and the deferred Fisherman behavior. No claim that the entire plan is finished. Two user questions about Librarian and combat gear remain pending.
# Village trading fix installed

The trade hook previously consulted only explicit ManagedVillagers assignments, which are not yet automatically populated. It now also cancels startTrading for a villager inside any registered Village declaration; unrelated outside villagers retain vanilla behavior. Union containment intentionally includes overlapping Village regions for the trade rule, without choosing a job owner there. Name tags and other interactions remain outside this cancellation point. Sign load registration now uses the ServerLevel supplied by the lifecycle event.

All 76 tests pass, including dimension/outside/overlap scope checks and native mixin loading. Installed local artifact SHA256 80A843E3731F95A8E6B1417E8F36AF32D21AEF1A0F166B19FDB29F77703044E2. World saved and backed up under backups/pre-trade-fix-20261019-171503.zip, then server restarted. Status ready on127.0.0.1:25578. The live diagnostic found zero loaded markers/NPC targets before player login, so no actual in-village click test has yet been verified for this update.
# Village live repair — 76 passing tests

Live diagnostics confirmed two separate faults: all declarations were disabled by any overlap; native 26.3 grass_block is not in BlockTags.DIRT. Both repaired. Only overlapping positions lack a unique managed scope; unambiguous perimeter columns may build. Native grass_block/mycelium are explicitly safe dry support, and ground-cover grass/ferns/snow can be cleared. Water/structures remain protected. The grass/water regression uses actual native states; all 76 tests pass.

Installed local development artifact SHA256: C0EAA2C221ADDC8B476F969DFD17CE5BE97A0C4E7683814EB4BD9DF1013F3D09. Same world/ops/offline loopback server. Latest pre-fix world backup: backups/pre-grass-fix-20261019-171200.zip. Previous wall/index claims below are superseded where conflicting. Actual placement count is logged per Village job; unloaded boundary work still skips rather than force-loading.
# Local wall update installed — 75 tests

The user explicitly requested installing the new local build. Five wall-specific tests passed (unique half-open perimeter, terrain-relative four-block height, bounded work, water/obstruction exclusion, invalidated scope and large lazy bounds). clean build passed: 75 tests, no failures/errors/skips.

Installed JAR SHA256: af5396a10d6d58936626f2b6401d5744effe380345ce59c3870f497d55f57ab6.
Server root: C:\Users\seyit\AppData\Local\Temp\minor-lord-fabric263-isolated.
The old server saved players/worlds and stopped normally. Pre-update world/settings/ops/whitelist/old JAR backup: backups/pre-wall-20261019-170340.zip (10,476,330 bytes). The new artifact was installed only on the localhost development server. Offline mode, OP and world preserved. No Dokploy deployment.

Village sign load/edit events now enqueue air-only stone_bricks walls four blocks high, following safe dry natural surface; water, entities, obstructions, uncertain roofs/leaves and unloaded columns are skipped. stone_bricks is a disclosed implementation assumption. This is conservative initial wall behavior, not completed protection integration or guaranteed full perimeter across unloaded terrain. Real world wall behavior still needs observation. Chester and other concrete role jobs remain incomplete.

Earlier checkpoint assertions below that the running JAR is the 60-test version are superseded by this installation.
# Güncel doğrulanmış checkpoint — 70 test

Bu bölüm güncel durumu belirtir. Aşağıdaki 46/60 test ve EULA/geometry/remainder bekleme bölümleri geçmiş checkpoint kayıtlarıdır; güncel onaylarla çelişen eski bekleme ifadeleri geçersizdir.

- clean build: 70 test, 0 failure/error, 0 skipped. Gerçek Fabric Loader server JUnit ve native 26.3 recipe/tag/component kullanıldı. Village sign mixin exact Loader ortamında class loading ile de doğrulandı.
- Artifact: build/libs/minor-lord-0.1.0-dev.jar; SHA-256: 7b96361cbad5a37ec64d69bf5d4572366d9b3eef30b31261cabba35e96964e8b
- Onaylanan Village/XxZ ve [cx-X,cx+X), [cz-Z,cz+Z) parser/index uygulandı; 25x25 tam 2500 blok. Loaded sign load/edit/unload olayları kullanılır; force-load/world scan yok. Çakışan köyler ve çelişen iki sign yüzü aktif scope vermez. Duvar veya mevcut yapı değiştirilmez; scope arazi sahipliği sayılmaz.
- Approved returned-container path implemented: native cake üretiminde 3 bucket korunur, bütün hedeflerin ortak kapasitesi kontrol edilir, eksik/dolu hedefte girdiler korunur. Ürünler carried buffer'a atomik dönüşür; uzak depoya teleport yok. Fiziksel rol iş döngüsü hâlâ bekler.
- Literal includes her isimde geçerli; iron_ingot exact-match değildir. İsim kaynağı/normalizasyon hâlâ açık.
- EULA yalnız C:\Users\seyit\AppData\Local\Temp\minor-lord-fabric263-isolated için kullanıcı tarafından açık onaylandı; eula=true. Sunucu 26.3/Fabric0.19.5/MinorLord0.1.0-dev ile başarıyla açıldı.
- Kullanıcının ek açık onayıyla online-mode=false yalnız 127.0.0.1:25578 üzerinde uygulandı. Önce listener/process kontrol edildi, normal server.halt(false) ile players/worlds kaydedildi ve aynı dünya ile yeniden başlatıldı. RCON/firewall/Dokploy değiştirilmedi. Whitelist açık ve yalnız umudik ekli.
- Gerçek log: 16:44:59 umudik joined the game. Status handshake Online=1. Bu önceki status-only testinden farklı olarak gerçek oyuncu katılımı kanıtıdır; bütün rol davranışlarını kanıtlamaz.
- Kullanıcının hedefi doğrulanmış OP talebiyle console op umudik çalıştı: 16:47:24 Made umudik a server operator. ops.json readback UUID76057363-b3b5-33a5-a099-228398561748 / level4. Sunucu bu Windows makinesindedir; Dokploy kurulumu yoktur.
- Çalışan sunucuda 60-test artifact SHA256 1f2a2625a2c770cc21bf4c8db005c3ffa3dff257a4dec6908f3338544bdbe746 var. Kullanıcı bağlıyken yeni 70-test JAR yüklenmedi/restart yapılmadı. Yeni Village/remainder kodunun canlı world testi henüz yapılmadı.
- Tüm somut rol işleri, automatic managed membership/name-tag assignment, inventory event wiring, fiziksel üretim/yürüme ve persistent görevler tamamlanmadı. Rol matrisi aşağıda bu yüzden 'tamamlandı' sayılmaz. Cook remainder politika sorusu çözüldü; world task'ı hâlâ bekler.
- Public repo https://github.com/umudik/minor-lord. Commit/push/deploy/DNS yok; lisans seçilmedi. Bekleyenler docs/pending-decisions.md içinde.

## Önceki checkpoint geçmişi (güncel durum yukarıdadır)
# Geliştirme durumu — doğrulanmış kapsam

## Hazırlanan temel

- Minecraft 26.3 / Java 25 / Fabric Loader 0.19.5 / Fabric API 0.163.0+26.3 / Loom 1.18.4 / Gradle 9.7.1.
- Dedicated server ortamlı mod girişi; client entrypoint, yeni oyun item/block kaydı, resource pack veya custom UI yok.
- ChestNameFilter: yalnız literal substring. Case/boşluk/namespace ve isim kaynağı henüz oyun politikası olarak seçilmedi; caller karşılaştırılabilir metni sağlar. Boş etiket wildcard olamaz.
- NearestChestTargets: yalnız uygun eşleşen hedefleri değerlendirir. Mesafe ve eligibility caller tarafından sağlanır; eşit uzaklıktaki hedefleri birlikte döndürür, tie-break uydurmaz.
- InventoryTransfers: gerçek Fabric transaction ile tam miktar aktarımı. Eksik kaynak/alan, geçersizleşen guard veya exception durumunda rollback. Çakışan fiziksel inventory kimlikleri reddedilir; double chest iki yarıyı da kimlikte belirtmelidir. Depolar Fabric transaction sözleşmesine uymalıdır.
- ChesterPlanning: etiketsiz kaynak olabilir, doğru yerdeki item bırakılır, uygun ve yeterli kapasiteli hedef yoksa kaynak değişmez. Bu salt okunur planlamadır; kaynaktan item çekmez.
- RoleEquipment: role özel şablonun kopyasına minecraft:unbreakable ekler. Şablon/player itemı/üretim output'u değiştirilmez. Henüz role otomatik ekipman verme bağlı değil.

## Çalıştırılan doğrulama

Gerçek repo: C:\Users\seyit\OneDrive\Documents\GitHub\minor-lord

JDK 25 ile gradlew.bat --no-daemon clean build başarılı.

- 60 test, 0 failure/error, 0 skipped.
- Gerçek Fabric Loader 0.19.5 server-side JUnit adapteri ve Fabric transaction uygulaması kullanıldı. Minecraft 26.3 component initializers registry test ortamında bağlandı; dünya başlatılmadı.
- Exact miktar, birden çok kaynak slotu, yetersiz kaynak/kapasite rollback'i, işlem öncesi/sonrası geçerlilik, mutation sonrası exception rollback'i, double-chest alias engeli ve Long.MAX_VALUE sınırı test edildi.
- Sabit seed ile 1.000 rastgele senaryoda kaynak/hedef toplamı ve slot sınırları korundu.
- Native ItemVariant metadata'sının karışmaması, gerçek item stack kapasitesi/damage verisinin korunması ve unbreakable kopyasının normal şablonu değiştirmemesi test edildi.
- JAR metadata: environment=server, minecraft=26.3; client entrypoint/assets bulunmuyor.

Artifact: build/libs/minor-lord-0.1.0-dev.jar

SHA-256: 1f2a2625a2c770cc21bf4c8db005c3ffa3dff257a4dec6908f3338544bdbe746

HTML rapor: build/reports/tests/test/index.html

## Henüz uygulanmayanlar / sonraki sınırlar

- Dünya/sign keşfi, kalıcı köy kaydı, inventory event bağlama, fiziksel NPC hareketi ve gerçek rol iş döngüleri henüz yok. Yönetilenlere özel trade mixin'i var; otomatik managed üyeliği veya işyeri/name tag ataması henüz bağlanmadı. WORK/lifecycle gate artık açık managed task oturumu kurulursa native WORK activity paketine bağlanıyor; somut rol iş döngüleri ve otomatik üyelik henüz yok. Foundation tamamlanmış oyun modu değildir.
- Helper çağıran katman permission/scope/loaded/reachability ve fiziksel varışı güncel doğrulamalıdır. Mesafe ölçütü, isim kaynağı/normalizasyon ve tie policy açık. Aynı server thread'inde sync işlem şart; tick'ler arasında transaction tutulmaz. Bu katman item teleportu değildir.
- Canlı dedicated server/world başlatılmadı; EULA kabul edilmedi veya ayarlanmadı. Vanilla client join testi yapılmadı.
- Dokploy projesi/world/domain/DNS/port henüz oluşturulmadı. Eksik temel ürün deploy edilmedi. Son aşama deployment hedefi karar notlarında korunuyor.
- Lisans seçilmedi; commit/push yapılmadı. Fookie veya başka projeye dokunulmadı.

## İkinci aşamada eklenen doğrulanmış ortak parçalar

- Role göre stok eşikleri: ekipman dört rolü/Leatherworker/Cartographer 5 adet, Mason gerçek stack boyutunun 5 katı; Cook'a limit eklenmedi. Cleric/Librarian gibi açık roller için limit uydurulmadı.
- ChestStock yalnız verilen kayıtlı/tabelalı envanterleri sayar, çakışan double-chest kimliklerini reddeder. Exact variant veya caller'ın item kimliği predicate'i seçilebilir; NPC taşıma/rezervasyon otomatik sayılmaz.
- ManagedVillagers üyeliği açıkça kaydedilen UUID'lerle sınırlı. Trade mixin yalnız Villager.startTrading girişini managed üyelerde engeller; diğer vanilla etkileşimleri kapatılmaz. Server-side loader'da hedef sınıfın mixin ile yüklenmesi doğrulandı; dünyada trade etkileşimi testi yapılmadı.
- NaturalProfessionRoles mevcut vanilla profession'ı okur, meslek/POI/texture kaydı yaratmaz. WorkGate vanilla WORK etkinliğini kullanıp PANIC/HIDE/REST/uykuya öncelik verir; rol/name tag önceliği veya yeni saat aralığı seçmez.
- InventoryWorkQueue yalnız değişen envantere bağımlı worker UUID'lerini kuyruğa alır ve tekrarları birleştirir. Dünya envanter eventlerine henüz bağlanmadı.
- RuntimeRecipes canlı RecipeManager koleksiyonunu okur ve gerçek placement/Ingredient alternatiflerini BatchAllocator'a verir. Tam batch sayısı için alternatifler birlikte hesaplanır; yalnız kesin gereken miktar planlanır. Bu plan tek başına özel tarifin çalıştırılabileceği kanıtı değildir; matches/assemble/remainder ve makine adapter'i gerekir.
- Testlerde gerçek 26.3 bread ve diamond_sword JSON'u ve gereken vanilla item-tag kaynakları decode edildi. 10 wheat + sugar havuzundan yalnız 9 wheat ile 3 bread batch; 5 diamond + 8 stick havuzundan 2 sword batch için 4 diamond + 2 stick planlandığı doğrulandı.
- 500 ek rastgele alternatif-ingredient senaryosu bağımsız küçük brute-force sınırıyla karşılaştırıldı; 1.000 transfer senaryosu da korunuyor.
- OutputTraits gerçek runtime tag/component bilgilerini sunar; chest isim filtresiyle karıştırmaz, açık rol çakışma politikası seçmez.
- MiningBounds tabela merkezi radius ve tabela Y-1 sınır matematiğini, ButcherPolicy en az 8 yetişkin ve yalnız kırpılmış koyun koşulunu test eder. Bunlar henüz gerçek kazı/kesim task'ına bağlanmadı.
- LiveChestAccess yalnız zaten yüklü chunk'lardaki vanilla chest'leri çözer, double-chest iki yarısını fiziksel kimliğe katar ve kapak engelini aşmaz. Exact 26.3'e derlendi; dünya/chunk testi henüz yok.

## Rol bazlı kapsam listesi

| Rol | Uygulanmış/testli temel | Henüz bekleyen oyun davranışı |
| --- | --- | --- |
| Farmer | Vanilla profession, native WORK reuse boundary ve managed-only makeBread mixin target'ı loader'da doğrulandı | Canlı dünyada ekim-hasat/depo entegrasyonu, yeniden ekim girdisi ve end-to-end bread engeli testi |
| Fisherman | Ortak üyelik/gate altyapısı | Durgun su/kuru kıyı hareketi; üretim/loot açık, görsel olta ertelenmiş |
| Shepherd | Ortak gate/transfer | Kırpma/sağma, kova akışı, süt cooldown |
| Butcher | 8 yetişkin/kırpılmış koyun predicate'i testli | Tür/sayım kapsamı ve seyrek kontrol, gerçek kesim/loot/depolama |
| Leatherworker | 5 adet stok politikası testli, ortak recipe planı | Deri kategori ölçütü/Armorer çakışması, gerçek üretim yürüyüş/makine işleri |
| Mason | 5 gerçek stack eşiği testli, canlı recipe type sorgusu | Stonecutter üretim/ürün seçimi/depolama task'ı |
| Cleric | Ortak recipe/transfer araçları | Brewing adapter'i, yakıt/girdi/çıktı işleri; limit açık |
| Librarian | Gerçek registry büyüsüyle 1 normal kitap tüketimi ve level 1 enchanted book atomic üretimi testli; output normal | Rastgele havuz/seçim/ağırlık/limit/metadata filtresi ve fiziksel rol task'ı açık |
| Cartographer | 5 adet politika testli | Yalnız boş harita üretim task'ı |
| Fletcher | 5 adet politika ve runtime ranged trait'i | Gerçek ranged üretim/scheduler; kategori çakışmaları açık |
| Weaponsmith | 5 adet, gerçek diamond sword recipe planı testli | Fiziksel kaynak/makine/çıktı iş döngüsü |
| Toolsmith | 5 adet ve runtime mining-tool trait'i | Gerçek kazma/balta/kürek/çapa üretimi |
| Armorer | 5 adet ve runtime armor trait'leri | Üretim task'ı; kategori çakışmaları açık |
| Miner | Radius/Y matematiği ve copy-only unbreakable helper testli | 10 giriş/spiral, kazma hızı/damar/zemin-kabuk/torch/yol/boşaltma/chunk task'ları |
| Cook/Cooker | Gerçek bread planı ve 3 wheat → 1 bread atomic crafting işlemi testli; tam batch/alternatif tahsisi var, limit yok | Mutfak/makine task'ları, remainder ve isim/alias kesinliği; gerçek dünya akışı henüz yok |
| Smelter | Runtime ingot trait'i ve recipe type sorgusu | Furnace/blasting ingot üretimi ve fiziksel akış |
| Chester | Hedef planlama/kapasite/aynı yerde bırakma/transaction testleri | Köy chest keşfi, event bağlama, fiziksel pickup/delivery ve save/resume |
| Warrior | Managed-only trade/gate, unbreakable copy helper | Random ekipman havuzu, dost/düşman, combat/uyku-korku önceliği |
| Archer | Managed-only trade/gate, unbreakable copy helper | Random set/bow/enchant/ok kaynağı ve combat tasarımı/uygulaması |

## Canlı aktivasyon ve deployment için somut bekleyenler

- Köy tabelasının ilk satırı ve sınırın kapsayıcı blok kuralı ile chest item-adı kaynağı/normalizasyonu henüz açık. İlk satır ve isim karşılaştırma için kullanıcıya tek soru gönderildi; yanıt bekliyor. Dünya parser'ı sessizce bir tercih seçerek etkinleştirilmedi.
- Özel recipe remainder/container davranışı, ek rol/vanilla profession önceliği ve random savaş havuzu gibi önemli oyun politikaları açık. İlgili parçalar tamamlanmış sayılmıyor.
- Dokploy'a mevcut Chrome oturumundan salt okunur erişildi; yeni Minor Lord projesi listede yoktu. Mevcut projelerde değişiklik yapılmadı. Monitoring okumasında yaklaşık 6.15/7.56 GiB RAM, 24.04/71.6 GB disk kullanımı görüldü; güvenli test kaynak bütçesi ayrıca netleşmeli.
- Yeni test world için EULA kabulü yok. Domain sağlayıcısı/sahip olunan uygun hostname ve DNS erişimi, oyun portunun boşluğu henüz doğrulanmadı. Hiçbir deployment/DNS değişikliği yapılmadı; bunlar reddedilmiş işlem değil, bekleyen önkoşullardır.
- Bu liste tüm modun tamamlandığı anlamına gelmez. Build/test başarısı yalnız yukarıda açıkça testli olarak belirtilen parçaları kapsar.
## Son doğrulanan ek aşama — 46 test

- InventoryConversions açık pozitif miktarda girdiyi tüketip bütün çıktıları tek Fabric transaction içinde üretir; eksik girdi, dolu çıktı, son anda geçersizlik ve exception güvenliği rollback sözleşmesine tabidir. Dünya makinelerini veya fiziksel taşımayı atlamaz.
- LibrarianBooks, seçilen gerçek registry enchant identity'sini daima level 1 olarak stored enchantments'a yazar; 1 normal kitap maliyetiyle atomic işlem test edildi. Lapis/emerald/XP maliyeti veya unbreakable production output eklenmedi. Rastgele uygun havuz/olasılık seçilmedi.
- StandardCrafting gerçek recipe.matches/assemble çağrılarını kullanır. Native bread tarifinde 3 wheat tüketilip 1 normal bread üretildiği ve ikinci bedava üretimin engellendiği test edildi.
- Native cake tarifinin bucket/remainder davranışı bulundu; çözülmemiş politika nedeniyle REMAINDER_POLICY_PENDING ile hiçbir girdi tüketmeden dönmesi test edildi. Remainder silinmedi veya rastgele bir depolama politikası seçilmedi.
- Gerçek repo yolundaki clean build: 46 test, 0 hata, 0 skipped. Artifact SHA-256 yukarıda güncellendi. Rol matrisi bu son aşamaya göre düzeltildi; oyun iş döngülerinin tamamlandığı iddia edilmiyor.
- Ayrı localhost smoke-test klasörü hazır: C:\Users\seyit\AppData\Local\Temp\minor-lord-fabric263-isolated. Bind 127.0.0.1:25578, ayrı world adı, RCON kapalı, eula=false. Sunucu başlatılmadı. Son EULA kabulü için kullanıcıya açık soru gönderildi; yanıt bekliyor. Bu soru Dokploy deployment veya mevcut world değişikliği değildir.
- Canlı dünya aktivasyonu için köy marker metni ve item isim/normalizasyon sorusu da yanıt bekliyor. Bu bekleyen kararlar reddedilmiş işlem değildir; hiçbir EULA veya oyun kuralı varsayılarak uygulanmadı.
## Güncel bağımsız NPC entegrasyonu — 60 test, 0 hata

- Native 26.3 Villager activity-provider noktasına managed-only WORK hook eklendi. Yalnız aynı village/role atamasına açık task kurulmuşsa devreye girer; yönetilmeyen NPC veya task'sız üyeye davranış paketi değişikliği yapılmaz.
- CORE/PANIC/HIDE/REST ve diğer activity paketleri, koşullar ve stop-memory setleri korunur. Farmer WORK paketi reuse edilir; managed Farmer makeBread noktası ayrı hook ile engellenir. Mixin hedefleri gerçek server-side Loader'da yüklenerek doğrulandı; canlı dünya/meslek testi henüz yok.
- WorkSession aynı task verisini pause/resume boyunca korur. Tamamlanan task tekrar çalışmaz. Step veya pause hatasında FAILED durumu korunur ve otomatik üretim tekrarı yapılmaz. Controller item stack sahibi değildir; gerçek item mutasyonları transaction ve kalıcı entity/makine inventory'leri üzerinden yönetilmelidir.
- ManagedWorkSessions rol veya village değişince eski task'ı çalıştırmaz, kalan task'ı sessizce üzerine yazmaz. installFor, server ve güvenli vanilla WORK koşulu arar; korku/uyku sırasında beyni yeniden kurmaz.
- TaskWalkTarget vanilla WALK_TARGET memory'sini kullanır; teleport veya navigation.stop çağırmaz. Yalnız kendi koyduğu memory nesnesini bırakır, vanilla'nın yeni korku/uyku hedefini aynı koordinatta bile silmez ve mevcut yabancı hedefi zorla devralmaz. Bu ownership sınırları unit testli; gerçek pathfinding/yürüme henüz EULA/world testi bekler.
- RecipeRemainders yalnız read-only inspection yapar. Gerçek 26.3 cake tarifinin 3 bucket döndürdüğü ve girdi stack'lerinin değişmediği test edildi. Returned-container iade önerisi docs/remainder-proposal.md içinde; uygulanmadı.
- Tam sorular ve önerilen varsayılanlar docs/pending-decisions.md içine yazıldı. Köy marker/bounds ve item-name adaptörü onay bekler; remainder politikası yalnız container tariflerini bloke eder. EULA yalnız server/world başlatma ve entegrasyon testini bloke eder. Bağımsız kod ve unit testleri bu sırada ilerletildi.
- Bütün somut rol iş döngüleri tamamlanmış değildir. Automatic village discovery/name-tag assignment, fiziksel pickup/delivery ve save/resume işleri hâlen bekler. Ek role/vanilla profession önceliği veya random savaş gear pool seçilmedi.
- Son gerçek repo clean build: 60 test, 0 failure/error, 0 skipped. Artifact hash'i üstte güncellendi. Commit/push/deploy ve EULA kabulü yapılmadı.