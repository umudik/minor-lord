Current runtime checkpoint: 2026-10-19, 83 tests, native role jobs installed locally.

See [runtime-status.md](runtime-status.md) for current implementation, latest user decisions, live-world evidence and limits. This file below retains historical checkpoint/design text; older claims that role jobs are absent or these latest decisions are pending are superseded.

---

# Son kullanıcı onayları — önceki açık soruları geçersiz kılan güncel kurallar

- Village / XxZ: 25x25 toplam50×50; yarı-açık [cx-X,cx+X), [cz-Z,cz+Z) blok sınırı onaylandı. Loaded vanilla sign index eklendi. Koruma/yerleşik yapıları overwrite etme yetkisi bu karardan çıkarılmadı.
- Chest filters her zaman literal includes. iron_ingot da içeren isimleri kapsar; exact-match istisnası yok. Case/boşluk/namespace ve registry/display-name seçimi henüz onaylanmadı.
- Gerçek returned-container/remainder çıktıları silinmez, güncel en yakın matching uygun chest'e fiziksel iade edilir. Hedef yok/doluysa üretim girdi tüketmeden bekler. Gerçek cake/3bucket, rollback ve no-dupe testleri eklendi. Önceki remainder-policy-pending sorusu kapanmıştır.
- Minecraft EULA yalnız belirtilmiş localhost izole test klasörü için açıkça kabul edildi. Gerçek server açıldı ve umudik katıldı.
- Ayrı açık risk/onay ve exact target sonrasında yalnız localhost25578 offline-mode yapıldı; umudik adına yalnız burada OP verildi. Başka server/Dokploy auth/deployment ayarı değişmedi. Bu server kullanıcının Windows bilgisayarındadır; Dokploy'a kurulmuş değildir.

Aşağıdaki eski checkpoint/onay bekleme ifadeleri tarihsel kayıttır; güncel durum implementation-status.md ve pending-decisions.md başındadır.
# Minor Lord — Karar notları

Bu dosya konuşmada netleşen kararları ve henüz açık konuları kaydeder. Açık konular uygulama şartı değildir.

## Netleşen kararlar

### Platform ve istemci

- Hedef Minecraft sürümü: 26.3. Mod Fabric üzerinde geliştirilecek. Fabric tercihi yeniden açıkça onaylandı; Paper kullanılmayacak.
- Mod tamamen sunucu tarafında çalışacak. Vanilla istemcilerin mod kurması veya başka bir değişiklik yapması gerekmeyecek.
- Custom UI, menü, dashboard veya inventory screen hiç olmayacak. Ayarlar vanilla tabela/name tag/workstation üzerinden yapılacak; yönetilen köylü trade ekranları kapalı olacak.

### Köy tanımı ve sınırlar

- Vanilla tabela köyü tanımlayan işaret olacak. Tabelada köy işareti ve sonraki satırda boyut bulunacak.
- Tabela köyün merkezi olacak.
- Boyut ifadesinde ilk sayı X, ikinci sayı Z ekseni için kullanılacak. Y sabit bir düzlem olmayacak.
- Son karara göre 25x25, merkezden X ve Z eksenlerinde her iki yöne 25 uzaklık anlamına geliyor. Kullanıcı bunu toplam 50x50 olarak ifade ediyor. Bu karar önceki toplam genişlik yorumunun yerini alır; kapsayıcı blok sayısı henüz kararlaştırılmadı.
- Kara yüzeyindeki sınır duvarı araziyi takip edecek ve 4 blok yüksek olacak.
- Köy sınırında yüzey su ise o kesime duvar konmayacak; sınırın o bölümü açık kalacak. Su üstünü doldurma, köprü veya drenaj yapılmayacak. Kullanıcı genel su dedi; bu istisna source/flowing ayrımıyla daraltılmayacak.
- Duvar için taş tuğla benzeri vanilla bir blok kullanılacak; kesin blok kimliği açık.

### Köylü rolleri

- Son rol atama tasarımı çift mekanizma: vanilla meslekler doğal workstation/jobsite ve profession davranışıyla edinilecek; vanilla mesleklerde name tag zorunluluğu olmayacak. Görünüşün meslekle tutarlı olması hedef.
- Ek roller name tag ile atanacak. Miner tek ek rol değil; Cook, Smelter ve Chester da ek roller. Warrior/Archer da ek roller; Warrior'ın aşağıdaki temel ekipman/saldırı kararları onaylandı, kalan ayrıntıları ve Archer tasarımı açık.
- Miner için miner, Warrior için warrior ve ayrıntısız Archer için archer anahtarları önceki rol adları olarak korunur. Aşçı rolünün Cook/Cooker kesin adı açık; önceki küçük/büyük harf duyarsızlığı kararı ek rol mekanizmasında korunur. Vanilla meslekleri tag ile atama şartı yok.
- Ek rol tag'ının mevcut vanilla profession ile öncelik/çakışması, görünüş ve işyeri ilişkisi henüz açık; otomatik bir öncelik veya dönüşüm seçilmeyecek.
- Mine tabelası kazı marker'ı olarak korunur; otomatik meslek atayan jobsite olarak onaylanmadı. Yeni custom profession/texture talebi yok; buna yönelik araştırma durduruldu.
- Warrior rolünün kılıç kullanması isteniyor.
- İlk odak madenci ve yemek pişiren rol olacak.
- Mevcut çiftçi davranışı korunacak.
- Ayrı bir aşçı rolü olacak. Vanilla kasabın amacı korunacak; kasap rolüne yemek pişirme ekleme önerisi reddedildi.
- Ek aşçı rolü için önce Cook adı seçilmişti; kullanıcı son olarak Cooker dedi. Cook/Cooker alias ve kesin normalize ad henüz açık. Ek rol name tag eşleşmesinde önceki küçük/büyük harf duyarsızlığı kararı geçerli.

### Cook üretimi ve sandıklar

- Cook, uygun etiketli kaynak sandıkların içerik değişimlerine dayalı, olay güdümlü tetiklemeyle çalışacak. Sürekli pahalı tarama olmayacak.
- Üretim yalnız fırında pişirmeyle sınırlı olmayacak; crafting tarifleriyle de yenebilir gıda üretilecek. Yakındaki crafting table, furnace ve smoker uygun şekilde kullanılacak.
- Tarif girdileri birden fazla uygun etiketli sandıktan alınabilecek; örneğin sugar girdisi eşleşen sandıktan sağlanabilecek. Tek bir Raw Food kaynağı zorunluluğu yok.
- Pişiricide mevcut yakıt önce kullanılacak. Yakıt yoksa ilgili üretim bekleyecek. Yeni isim filtresiyle yakıt sandığı erişiminin nasıl sınırlandırılacağı açık; Fuel bir semantik alias kabul edilmeyecek, izinsiz kaynak alma varsayılmayacak.
- Üretici çıktısı, item adına uyan etiketli sandıklar arasından en yakınına gönderilecek. Food/Raw Food/Cooked Food özel gıda kategorileri değildir.
- Üretici envanterinde stok veya boşuna bekleyen malzeme olmayacak; kısmi toplama sırasında geçici taşıma envanteri mümkün. Çıktı/iade için uygun hedef yoksa veya doluysa fallback davranışı açık; item silme, teleport veya zemine atma varsayılmayacak.
- Girdi → üretim → çıktı mantığı başka üretim rollerine de uygulanacak; her rolün tam şartnamesi ayrıca konuşulacak.

### Cook için dinamik tarif keşfi ve kaynak toplama

- Food/item/recipe listeleri statik olarak tutulmayacak; keşif dinamik olacak.
- Fabric modunun eriştiği Minecraft çalışma zamanı registry, component ve recipe manager verileri kullanılacak. Besin özelliğine sahip çıktılar, canlı sunucunun tarifleri ve ingredient alternatifleri bu verilerden keşfedilecek. Bu mekanizmaların tamamı Fabric API'ye aitmiş gibi kabul edilmeyecek.
- Her tüketilebilir item Cook'un hedefi değildir. Potion gibi besin olmayan tüketilebilir itemlar kapsam dışında olacak.
- Buğday ve şeker gibi yenmeyen tarif girdileri tariflerden çıkarılacak; yalnız doğrudan yenebilen girdilerle sınırlı bir liste kullanılmayacak.
- Önce kaynakların, erişim izinlerinin ve işleme gereksinimlerinin gerçekten karşılandığı üretilebilir tarif belirlenecek. Ardından yalnız gerekli miktarda malzeme alınacak.
- Tüm şekeri toplama veya envanteri malzemeyle doldurma davranışı olmayacak. Üretim mümkün değilse spekülatif malzeme toplanmayacak veya tutulmayacak.
- Dinamik tarif keşfi ile sandık filtreleri ayrı konulardır. Üreticiler tarif girdilerini birden fazla uygun etiketli sandıktan, yalnız gereken miktarda toplayacak; erişim izinleri kendiliğinden genişlemeyecek. Yakıtın kesin kaynak/izin kuralı yeni filtre tasarımıyla ayrıca netleşecek.

### Sandık etiketleri: yalnız isim filtresi

- Sandık etiketleri yalnız item adına uygulanan includes/substring filtresidir; semantik kategori anlamı yoktur. Yeni sürüm/mod itemları da dinamik isim eşleşmesinin hedefidir; vanilla-client uyumluluğu ayrıca değerlendirilir.
- food etiketi, adında food bulunmayan yenebilir itemı kabul etmez. Food ve Raw Food besin kategorisi değildir.
- ingot, iron ve melon örnekleri sırasıyla adında bu metinler bulunan itemları eşleştirir. Daha uzun etiket de aynı isim filtresidir; önceki genel/tam item çözümlemesi ayrımı artık aktif kural değildir.
- Çıktı için eşleşen sandıklar arasından en yakın olan seçilecek. Özel etiketin genel etikete önceliği yoktur.
- Rolün hedef çıktı grubu runtime besin/tag/component verilerinden sınıflandırılmaya devam edecek. Bu sınıflandırma sandık isim filtrelerinden ayrıdır; Cook'un besin üretmesi food adlı sandığın bütün besinleri kabul etmesini sağlamaz.

### Ortak yardımcı fonksiyonlar ve onay sırası

- Kullanıcı ortak util fonksiyonlarını açıkça istedi. Item/etiket eşleştirme, uygun ve yakın sandık bulma, üretim planlama, tarif/görev geçişi ve yeniden planlama aynı kuralları roller arasında paylaşacak.
- Bundan sonra köylü görevleri ve lifecycle akışları tek tek onaylanacak; bir rol netleşip onaylanınca sonrakine geçilecek. Bir rolün anlatılması bütün lifecycle ayrıntılarının onaylandığı anlamına gelmez.

### Farmer: onaylanan lifecycle ve açlık kapsamı

- Farmer lifecycle'ı tasarım olarak onaylandı: vanilla ekim/hasat davranışı korunacak; yeniden ekim için gereken girdiler ayrılacak; fazlası ortak kuralla en yakın isimle eşleşen sandığa bırakılacak.
- Farmer'ın ekmek crafting'i kapatılacak. Gıda üretimi Cook'a ait olacak. Kullanıcı bunu açıkça onaylayıp sonraki role geçilmesini istedi.
- Bu maddeler onaylı tasarımdır; kodda uygulanmış değildir. Konuşmada geçen kapattık ifadesi gerçek bir kod değişikliği anlamına gelmez. Teknik entegrasyonun yapılabilirliği henüz uygulama/derleme ile doğrulanmadı; Farmer davranışını bütünüyle yeniden yazma kararı yok.
- Özel (custom) açlık sistemi eklenmeyecek. Hunger meter, 3 yemek eşiği ve açlığa bağlı yiyecek arama döngüsü aktif kapsamda değil.
- Vanilla köylünün mevcut internal food inventory ve breeding davranışlarını kaldırma kararı yok.

### Fisherman: onaylanan kıyı hareketi ve ertelenen görsel olta

- Fisherman durgun su bulacak; tek blok su da olabilir. Güvenli kuru kıyıya gidecek, akıntıya girip sürüklenmeyecek. Kullanıcı bu hareketi onayladı.
- Kullanıcı yalnız basit native destek varsa görsel oltayla balıkçılığı istiyor. Çok basit değilse yapılmamasını istedi. Exact 26.3 kontrolünde native player-owner kısıtı görüldüğü için yalnız görsel/native olta davranışı ilk kapsamdan ertelendi; onaylanan kıyıya gitme hareketi kapsamda.
- Özel renderer, fake-player, particle veya display simülasyonu tasarlanmayacak/araştırılmayacak. Kendiliğinden loot spawn alternatifi seçilmedi. Kıyıya gittikten sonra suda hangi üretim/loot davranışının olacağı henüz açık; simülasyon onayı yok.
- Görsel olta ertelemesi öncesi konuşulan fishing lifecycle ve araç/loot yönü aşağıdaki tarihçede korundu; onaylanan yeni kıyı hareketinden ayrı tutulacak. Kullanıcının diğer role geçmeyi onaylaması otomatik loot alternatifine onay sayılmayacak. Üretim/loot ayrıntıları hâlen açık; kod uygulanmadı.

### Shepherd: onaylanan görevler

- Shepherd koyunları öldürmeden makasla kırpacak ve inek sağacak; iki işi birlikte yapacak. Kullanıcı ikisini de yapmasını onaylayıp sonraki role geçilmesini istedi. Bu işler arasında belirlenmiş öncelik yok.
- Boş kovayı uygun isim filtresiyle eşleşen sandıktan alacak. Süt kovası ve yünü ortak en yakın eşleşen sandık kuralıyla bırakacak.
- Makas/kova kapasitesi, süt cooldown/üretim limiti ve diğer araç ayrıntıları açık. Meslek aracı olan makas son genel karara göre bitmeyecek/kırılmayacak; kova ve süt girdileri bu karar nedeniyle sınırsız sayılmaz. Sonsuz süt üretimi varsayılmayacak.
- Bunlar onaylı tasarım kararlarıdır; kod uygulanmadı.

### Butcher: onaylanan görevler

- Butcher her hayvan türünde en az 8 yetişkin bırakacak; yalnız bu sayının üzerindeki fazlayı kesecek. Önceki asistanın 2 yetişkin önerisi kullanıcı tarafından 8 olarak değiştirildi; 2 aktif kural değildir.
- Et ve yan ürünleri ortak kuralla en yakın eşleşen sandığa bırakacak. Kullanıcı yan ürünleri de onayladı; wool konuşmada verilen örnek. Bu örnek kesin uygun hayvan türü veya özel drop listesi tanımı değildir.
- Pişirme Cook'a ait kalacak.
- Uygun hayvan türleri listesi, hayvan sayımının kapsamı/sınırı ve breeding davranışı henüz açık; yeni davranış varsayılmayacak.
- Bunlar onaylı tasarım kararlarıdır; kod uygulanmadı.

### Shepherd / Butcher: onaylanan basit koordinasyon

- Katı global sıra veya saat düzeni olmayacak. Shepherd kırpılabilir koyunu kırpacak.
- Butcher seyrek kontrol yapacak; kesin interval açık. 20 dakika kullanıcı fikriydi, kesinleşmiş süre değil.
- Butcher tür başına en az 8 yetişkin bırakma kuralını koruyacak. Koyunda yalnız önceden kırpılmış uygun yetişkinler kesim için seçilecek; böyle koyun yoksa o tur koyun kesilmeyecek, diğer türler değerlendirilebilecek.
- Toplanan bütün et ve yan ürünler ortak en yakın eşleşen sandık kuralıyla boşaltılacak.

### Leatherworker: onaylanan üretim yönü

- Kullanıcı Leatherworker'ın deriyle ilgili yapabildiği her şeyi yapmasını istedi. Kapsam yalnız leather armor ile sınırlanmayacak; gerçek tarif ve erişilebilir kaynaklarla üretilebilir deriyle ilgili çıktılar hedeflenecek.
- Ortak dinamik tarif keşfi, üretilebilirlik kontrolü, kesin miktar toplama, plan/geçiş/yeniden planlama ve en yakın eşleşen sandık kuralları uygulanacak.
- Güncel Minecraft 26.3 sunucusunun gerçek tarifleri esas alınacak. Vanilla trade'de bir itemın bulunması üretim tarifi sayılmaz; tarifi olmayan item bedava yaratılmayacak.
- Deri kategorisinin tam runtime ölçütü, Armorer ile kategori çakışması, boyalı ürünler ve recipe remainder ayrıntıları açık.
- Leatherworker, Cook gibi ortak gerçek-tarif üretim mekanizmasını kullanacak; kapsamı deriyle ilgili üretilebilir çıktılar olacak.
- Leatherworker için son sandık stok eşiği ürün başına 5 adet. Sandık stoku 5 veya üzerindeyse ilgili ürün için yeni üretim başlatılmayacak; bu basit durdurma eşiği kesin üst sınır değildir. Fletcher/Weaponsmith/Toolsmith/Armorer için de 5 adet; Cook/gıda için limit yok.
- Diamond horse armor için gerçek tarif bulunduğu iddia edilmiyor. Genel kural gerçek recipe yoksa üretmemek; Leatherworker kapsamı deriyle ilgili ürünler.
- Bunlar onaylı tasarım kararlarıdır; kod uygulanmadı.

### Mason: onaylanan üretim ve stok eşiği

- Mason canlı sunucunun stonecutter tariflerinden uygun taş parçaları üretecek; ortak gerçek-tarif üretim mekanizması uygulanacak.
- Girdiler birden fazla uygun etiketli sandıktan toplanabilecek; çıktılar ortak en yakın eşleşen sandık kuralıyla bırakılacak.
- Son stok eşiği ürün başına 5 stack; 5 adet değil. Standart 64'lük stack için bu 320 item eder. Eşik hesabında itemın gerçek maksimum stack miktarı esas alınacak; bütün itemların 64 stack olduğu varsayılmayacak.
- Eşik her ayrı çıktı itemının köydeki kayıtlı/tabelalı sandıklardaki toplamına uygulanacak; tüm ürünlerin ortak toplamı için 5 stack değildir. Bir itemın sandık stoku 5 stack veya üzerindeyse Mason o ürünü atlayıp stoğu eksik başka üretilebilir ürüne geçecek. Bütün hedef ürünler kendi eşiklerine ulaşınca duracak. Kullanıcı bu akışı onayladı.
- Bu stok değeri yeni üretimi durdurma eşiğidir; bütün durumlarda kesin tavan garantisi değildir. Ortak tam batch büyüklüğüyle eşik etkileşimi ayrıca netleşecek.
- Fletcher/Weaponsmith/Toolsmith/Armorer için 5 adet, Leatherworker için 5 adet ve Cook için limitsiz üretim ayrımı korunacak; kaynak/çıktı kısıtları geçerli.
- Bunlar onaylı tasarım kararlarıdır; kod uygulanmadı.

### Cleric: onaylanan brewing rolü

- Cleric iksir standında gerçek brewing tarifleriyle çalışacak; gerekli malzemeleri toplayıp iksir üretecek ve çıktıları ortak en yakın eşleşen sandığa bırakacak.
- Ortak kaynak/erişim ve üretim planlama kuralları geçerli. Cleric'in stok limiti henüz açık; başka rolün eşiği kendiliğinden uygulanmayacak.
- Bunlar onaylı tasarım kararlarıdır; kod uygulanmadı.

### Librarian: onaylanan özel büyülü kitap işi

- Kullanıcı Librarian'ın normal kitap alıp büyü basmasını kesin olarak istiyor. Tek girdi maliyeti normal kitap; lapis, emerald veya XP ek maliyeti olmayacak.
- Canlı enchantment registry üzerinden dinamik keşif hedefleniyor; büyüler için tek tek statik katalog tutulmayacak.
- Her üretimde 1 normal kitap tüketilecek. Dinamik uygun enchant listesinden rastgele büyü seçilecek ve daima level 1 büyülü kitap üretilecek.
- Level yükseltme ve kitap birleştirme oyuncunun elle yapacağı iş olacak; Librarian kitap birleştirmeyecek veya büyü seviyesini yükseltmeyecek.
- Kullanıcı mending'in zor gelmesi fikrini dile getirdi; özel ağırlık veya oran belirlenmedi. Uniform dağılım ya da özel rarity/ağırlık kuralı varsayılmayacak.
- Standart Minecraft enchantment registry'sine düzgün kaydedilmiş mod büyüleri de kapsama girecek. Vanilla filtre ifadesi bu standart mod kayıtlarını dışlama anlamına gelmez.
- Standart registry'den ayrı özel kitap/enchant sistemleri için uyumluluk adapteri yapılmayacak; kullanıcı bunu açıkça reddetti.
- Tüm tek seviyeli büyüler havuzda olacak; Mending, Silk Touch ve Infinity yalnız örneklerdir, kapsam bunlarla sınırlı değil. Çok seviyeli büyüler de yalnız level 1 olarak üretilecek.
- Her enchant identity havuzda bir kayıt olarak yer alacak; seçim rastgele olacak. Bu, uniform olasılık veya belirli ağırlık onayı değildir; oran/ağırlık ayrıntıları henüz ayrı netleşmedi. Normal kitap tek maliyet olarak kalacak.
- Curse için özel dışlama onayı yok; curse/special ayrımının kesin ele alınışı açık. Tek kitapta çoklu enchant, stok limiti ve item metadata'sına göre hedef sandık eşleşmesi de açık.
- Bu, açıkça onaylanan özel enchant işidir. Genel olarak tarifi olmayan vanilla trade itemlarını otomatik/bedava yaratmama kuralı diğer üretimlerde geçerli kalır; o kural Librarian'ın bu özel normal-kitap → büyülü-kitap işini reddetmek için uygulanmayacak. Ek maliyet uydurulmayacak.
- Bunlar onaylı tasarım kararlarıdır; kod uygulanmadı.

### Cartographer: onaylanan kapsam ve stok eşiği

- Cartographer yalnız boş harita üretecek. Harita kopyalama ve keşif haritası üretimi olmayacak; kapsam artık kesin.
- Ürün stok eşiği 5 adet. Ortak sandık sayımı ve üretim durdurma eşiği kuralları uygulanacak; sert üst sınır garantisi değildir.
- Vanilla köylü trade ekranı ile cartography table ayrı mekanizmalardır; bu rolün onaylanan işi yalnız boş harita üretimidir.

### Miner: Mine tabelası, kazı ve depolama döngüsü

- Kullanıcı Mine tabelasını seçtiği konuma koyacak. Kazının alt sırası Mine tabela bloğunun Y seviyesinden 1 aşağıda olacak. Geçiş yüksekliği açık. Destek bloğunun Y'si ölçüt olarak kesinleştirilmedi; -53/-54 konuşmasındaki karışıklıktan ek formül çıkarılmayacak.
- Önce Mine tabelasından 10 blok düz giriş yapılacak; ardından yukarıdan spiral görünen, tabeladaki maksimum yarıçapa kadar dışa genişleyen spiral kazı başlayacak. İki blok aralık korunacak. Düz ana koridor/yan kol ağı tasarımı reddedilmişti; son onaylanan 10 blok giriş bu ağ tasarımı değildir.
- Kullanıcı kesin matematik/voxel yöntemini teknik tasarıma bıraktı. Önceki Mine tabela merkezli dairesel/radyal yön ile 10 blok giriş ve spiral merkezi tutarlı çözülecek; giriş/dönüş yolu kesilmeyecek. Asistanın spiral merkezini giriş sonu sayması kullanıcı tarafından ayrıca onaylanmadı; kesin merkez/lead-in bağlantısı teknik plan olarak açık.
- Miner'ın fiziksel olarak ileri yürümesi zorunlu. Her cevher bloğuna ayrı yürüme veya kazı animasyonu gerekmiyor.
- Mine tabelasının alt satırında sayısal maksimum yarıçap belirtilecek. 1000 yazılırsa 1000 blok radius demektir; path length veya diameter değildir. Önceki sınırsız genişleme yerine bu yarıçap sınırı geçerli.
- Mining köy boyutundan bağımsız, kendi tabela yarıçapı içinde genişleyebilecek. Tabela merkezi önceki intent olarak korunur; 10 blok giriş, spiral origin ve radius center geometrisinin tutarlılığı teknik tasarımda açık. Giriş sonu sessizce radius merkezi sayılmayacak.
- Eksik/geçersiz sayı ve maksimum sayı validasyonu ile chunk stratejisi açık. 1000 yarıçapındaki bütün alanı force-load etme onayı yok. Mining kapsamı başka rollere genel yetki veya chunk yükleme politikası sağlamaz.
- Miner düşmemek için uygun yapı bloklarıyla boşlukta zemin/köprü oluşturacak. Uygun blok yoksa bekleyecek; boşluğa düşmeyecek.
- Tüm kazı drop'ları toplanacak. Seçmeli drop toplama veya item silme uygulanmayacak.
- Cevher damarı bulununca bağlı cevherlerin tümünü çıkarma onaylandı: gerçek bloklar dünyadan kaldırılacak, loot doğrudan envantere alınacak; Miner her cevhere fiziksel olarak gitmeyecek.
- Bağlılık tanımı, damar sınırı, loot kapasitesi ve çıkarma temposu açık. Instant-damar anlatımı son tool-speed isteğiyle uzlaştırılacak; sınırsız tek-tick damar çıkarma vaat edilmeyecek.
- Damar boşluğunun mevcut veya gelecekte yürünecek spiral içi boş kalacak. Ortak kazı planına göre floor/sides/ceiling açıklıkları envanterdeki uygun taşlarla kapatılacak; yürüyüş yolu doldurulmayacak, tüm mağara doldurulmayacak.
- Envanter dolunca uygun eşleşen sandıkları fiziksel olarak teker teker gezecek; taşıdığı itemları güncel en yakın substring filtreli hedeflere boşaltacak, ardından Mine tabelasına geri dönecek. Boşaltırken az miktar güvenli yapı bloğu yanında kalacak; bunlar boşlukları köprülemek ve dönüş yolu için kullanılacak. Kullanıcı sıradan item değil, yerleştirilebilir blok olması gerektiğini vurguladı. Bu onaylı kazı/taşıma ve güvenlik stoğu üreticilerin spekülatif malzeme stoklamasından ayrıdır.
- Güvenlik bloğu miktarı, uygun blok ölçütü ve yerçekimli blokları dışlama gibi teknik seçimler henüz açık.
- Dönüş/iniş yolunu hafızaya kaydetme önerisi kullanıcı tarafından reddedildi. Her seferinde güncel dünyadan yol bulunacak ve geçerliliği kontrol edilecek; oyuncu madeni kapatmış olabilir. Yürünmüş blokları kayda alıp koruma yaklaşımı onaylı değil.
- Dönüş yolunu kırmama gereksinimi var. Bunun güncel dünyaya göre güvenli yol koruma algoritması henüz açık; kalıcı yürünmüş-blok kayıt sistemi varsayılmayacak.
- Kazma görseli güzel bulunuyor ama işlev önemli; kazmanın elde gösterilmesi zorunlu değil.
- Mağaraya çıkınca tüm mağara doldurulmayacak; yürünebilir tünelin kabuğu oluşturulacak. Bir alt zemin ile sağ/sol ve üst açıklıklar uygun envanter bloklarıyla kapatılacak, tünelin içi yürünebilir kalacak. Köylü yaklaşınca veya 1 adım önceden kapatma yapılabilir.
- Yapı blokları envanterden tüketilecek; uygun blok yoksa Miner bekleyecek. Yapı malzemesi sınırsız değil; sarf malzemesi olarak yalnız torch açıkça sınırsız onaylandı.
- Kazı/taş koyma animasyonu veya özel efekt gerekmiyor. Hedef blok doğrudan kaldırılabilir; bu sıfır süreli bütün-blok kazı garantisi değil. Drop/loot semantiği ve sade diamond kazmanın normal araç-blok hız etkisi korunacak; Efficiency yok, harvest sınırları açık.
- Kullanıcının diamond blokları patlatabilir ifadesi bu bağlamda anlık kırmayı anlatıyor; TNT veya alan etkili (AoE) patlama yetkisi değildir.
- Düşman saldırısından güvenlik hedefi var; dokunulmazlık garantisi yok.
- Miner için sınırsız torch kaynağı kullanıcı tarafından açıkça istendi. Kazdıkça ilerleyen tünele torch yerleştirip iyi aydınlatacak. Bu sarf malzemeleri kuralına özel istisna; diğer kaynaklar bu nedenle sınırsız olmayacak.
- Işık ful ifadesi her blokta 15 ışık garantisi olarak yorumlanmayacak. Torch aralığı, ışık eşiği ve yerleştirme kuralları henüz açık.
- Dönüşte deterministik desen fonksiyonu canlı dünya blokları üzerinde yeniden çalıştırılacak. Zaten hava olan/kazılmış yerler geçilecek; kullanıcı desene sonradan blok koyduysa yeniden kırılacak. Bu karar tüm alanı sıfırdan sürekli tarama tasarımı anlamına gelmez.
- Dönüşte yerde kalmış dropları toplaması isteniyor. Vanilla despawn'dan korunma garantisi veya bunu sağlayan ek mekanizma onaylanmadı.
- Miner kendi kalıcı/kırılmaz, sade diamond kazmasına sahip olacak; oyuncunun kazma vermesi gerekmeyecek. Efficiency olmayacak. Kullanıcı yavaş yavaş kazmasını istedi: normal araç-blok hızına göre çalışacak; her blok için sıfır süre/instant kazı varsayılmayacak. Animasyon zorunlu değil. Harvest ve bedrock gibi sınırlar açık; her bloğu kazabileceği varsayılmayacak.
- Geçiş yüksekliği, desenin tam voxel/ilerleme formülü, lav/su davranışı, korumalı bloklar ve chunk politikası açık. Dairesel desen chunk yüklenme sorununu kendiliğinden çözmez. Eşleşen depolama hedefi yoksa/doluysa fallback davranışı da açık.
- Bunlar tasarım kararlarıdır; kod uygulanmadı.

### Kalıcı meslek araçları ve sarf girdileri

- Fisherman oltası ve genel meslek araçları bitmeyecek/kırılmayacak. Bu araç kararı Fisherman'ın görsel/native olta davranışını kapsam içine geri almaz.
- Meslek ekipmanı olan armor/weapon/tool için seçilen yöntem native minecraft:unbreakable item data component. Kullanıcı Unbreaking enchant ile bu component arasındaki fark açıklanınca açıkça “Tamam tamam öyle yapalım” dedi. Cron, Mending+XP veya per-tick repair önerileri seçilmedi. Random gear pool hâlen açık; component kararı havuzu belirlemez.
- Kalıcı araç üretim girdisi olarak tüketilmez. Kova, coal ve diğer sarf girdilerinin sınırsız olduğu sonucu çıkarılmayacak; bunlara ilişkin kaynak/üretim kuralları ayrı kalır. Miner için kullanıcının açıkça istediği sınırsız torch bu genel kurala özel istisnadır.
- Genel araçların tier, hız, harvest ve enchant özellikleri kendiliğinden belirlenmiş sayılmayacak. Miner için son özel karar sade diamond kazma, Efficiency yok; kazı hızı normal araç-blok özelliklerine bağlı.

### Warrior: onaylanan temel ekipman ve saldırı

- Warrior'ın kendi zırhı/ekipmanı olacak; silahını sandıktan almayacak ve düşman görünce saldıracak. Son istekte kılıç dahil savaş ekipmanlarının random olması gündeme geldi. Önceki sabit diamond sword tercihiyle random isteğinin ilişkisi netleştirilecek; kesin random malzeme/enchant havuzu seçilmeyecek.
- Kullanıcı native minecraft:unbreakable item data component yöntemini açıkça onayladı. Meslek ekipmanı olan zırh/silah/araçlar bu component ile dayanıklılık kaybından korunacak. Zırh materyali ve random kapsamı açık; full diamond armor varsayılmayacak. Bu tasarım onayıdır; component henüz kodla uygulanmadı.
- Düşman tanımı, oyuncular için dost/düşman ayrımı, algılama/çatışma menzili ve gece uyku/korku davranışlarıyla savaşın önceliği açık. Oyunculara otomatik saldırganlık eklenmeyecek.
- Ortak vanilla lifecycle güvenlik öncelikleri korunur; yeni özel korkusuz combat istisnası onaylanmadı. Bu temel Warrior kararı diğer ek rollerin kaldırılması anlamına gelmez.
- Cook, Smelter ve Chester kararları korunuyor. Konuşmada geçen sildim ifadesi gerçek dosya/kod değişikliği veya bu rolleri silme kararı olarak alınmayacak.
- Üreticiler için mevcut eşikler değişmeyecek: dört ekipman rolü ve Leatherworker 5 adet, Mason 5 stack, Cartographer 5 adet; Cook limitsiz. Her role global 5 veya Mason'a 5 adet yorumu yapılmayacak. Açık limitler kendiliğinden belirlenmeyecek.
- Bunlar onaylı tasarım kararlarıdır; kod uygulanmadı.

### Warrior / Archer: random ekipman ve Archer yönü

- Archer ek rol olarak name tag ile atanacak ve bow kullanacak. Önce full leather set istendi; sonraki her şeyleri random olsun isteğinin bu sabitlikle ilişkisi henüz netleşmedi.
- Kullanıcı kılıç dahil ekipmanın random olmasını istedi. Bunun random malzeme mi, enchant mı veya ikisi mi olduğu ve uygun havuz açık; sessizce bir dağılım/level seçilmeyecek.
- Bow için önce Infinity, sonra normal/no-enchant, ardından random konuşuldu. Infinity kesin aktif karar değil. Ok kaynağı ve tüketimi açık.
- Bütün savaş ekipmanı ve zırhın kırılmaması için native minecraft:unbreakable item data component yöntemi onaylandı; kodda henüz uygulanmadı. Combat menzili, dost/düşman ve vanilla korku/uyku ile öncelik açık kalıyor.

### Bütün yönetilen köylüler için gündüz/gece lifecycle

- Bütün yönetilen köylüler bu görevleri gündüz yapacak; kullanıcı ifadesiyle sadece sabah çalışacaklar. Gece köye dönüp uyumaya çalışacaklar.
- Bu ortak kural köy dışında mining yapan Miner için de geçerli.
- Gece uyumaya çalışmak, kesin yatağa ulaşma veya uyku başarısı garantisi değildir.
- Kesin daytime tick penceresi, aktif işin güvenli ara verilmesi/bitirilmesi, dönüşü önceden başlatma, yatak yok/ulaşılamaz fallback ve ertesi gün resume ayrıntıları henüz açık.
- Teleport veya kaynak kaybettiren anlık iptal varsayılmayacak. Ortak üretim/kaynak iadesi ve güvenli yol kuralları bu lifecycle ile tutarlı tasarlanacak.
- Ortak invariant: köylünün normal davranışı ezilmeyecek. Köylü korkarsa vanilla davranışıyla kaçmaya çalışacak; özel işine devam etmeye zorlanmayacak.
- Custom görevler vanilla korku, kaçış ve uyku/güvenlik önceliklerine tabi olacak. Gündüz çalışma/gece uyumaya çalışma kararı vanilla lifecycle'ı koruma hedefidir; bağımsız yeni sleep veya hunger sistemi değildir.
- Vanilla WORK bölümü yönetilen köylüde belirlenen rol işiyle override edilecek; korku/kaçış/gece uyku korunacak. Farmer ekim/hasat davranışını reuse etme hedefi bu kuralla korunur.
- Korku/kaçış/uyku nedeniyle işe ara verme ve yeniden planlama item kaybettirmeyecek; bunun teknik yöntemi henüz açık.
- Warrior'ın temel ekipman/saldırı yönü onaylandı; korku/uyku ile combat önceliği ve Archer tasarımı açık. Bu roller için özel korkusuz combat istisnası eklenmeyecek.

### Bütün üretim rollerinin ortak ilkeleri

- Dinamik registry/özellik/tarif keşfi ve statik item/recipe kataloğu tutmama ilkesi yalnız Cook için değil, bütün üretim rolleri için geçerli. Zırh ve silah üretimi de bu kapsama dahil.
- Her rol hedef çıktı kategorisini belirleyecek; ortak üretim planlama ve kaynak kuralları uygulanacak. Cook'un besin çıktısı koşulu kendi rolünün hedefidir, diğer rollerin çıktı kategorisini değiştirmez.
- Bütün üretim rollerinde önce kaynak, erişim ve işleme koşullarıyla gerçekten üretilebilir tarif belirlenecek; ardından yalnız gereken miktar alınacak. Üretim mümkün değilse spekülatif malzeme toplanmayacak veya tutulmayacak.
- Kaynak ve çıktı kısıtları devam eder. Kaynak toplama birden fazla uygun etiketli sandıktan yapılabilir; yakıt erişiminin yeni isim filtresiyle kesin sınırları açık. Etiketsiz veya izinsiz sandıklardan kaynak alma yetkisi varsayılmayacak.
- Üreticiler envanterlerinde stok bekletmeyecek. Köy kapsamındaki birden çok uygun etiketli sandıktaki girdiler global değerlendirilecek; köylü fiziksel olarak sandıklara gidip gerekli malzemeleri toplayacak, sonra ilgili makinesine/çalışma istasyonuna gidip üretim yapacak. Kısmi toplama sırasında geçici taşıma envanteri mümkün; stok/boşuna bekletme hedef değil.
- Sandıktan item alınması veya sandığa item eklenmesi gibi içerik değişimlerinde ilgili üreticiler olay bazlı değerlendirilecek; sürekli tarama olmayacak. Her olayda bütün köylüleri taramak zorunlu bir tasarım olarak kabul edilmeyecek.

### Ortak üretim planı, iade ve tarif batch seçimi

- İlgili envanter değişimi olayında aktif plan yeniden kontrol edilecek. Girdiler ve koşullar hâlâ yeterliyse devam edilecek; yetersizse ilgili üretim iptal edilecek veya duracak ve önceden toplanan girdiler geri bırakılacak.
- İade orijinal kaynak sandığına bağlı olmayacak. İade anında item adına uyan güncel en yakın sandık seçilecek; eski sandık kırılmış veya yeni bir sandık eklenmiş olabilir. Ürün bırakma ve iade aynı en yakın eşleşen hedef kuralını kullanacak.
- Sandık oluşturulması, kırılması veya etiket değişimi hedef geçerliliğinin yeniden değerlendirilmesini tetikleyecek.
- Aynı anda bir aktif tarif olacak. Seçilen tarif için mevcut kaynak ve koşullarla üretilebilen tam batch sayısı azami yapılmaya çalışılacak: 10 üretilebiliyorsa 10, yalnız 1 üretilebiliyorsa 1.
- Seçilen tarifin malzemesi kalmayınca başka uygulanabilir tarife geçilecek. Tüm tarif olanakları değerlendirilecek; zorunlu round robin/fair scheduling yok. Önceki her tariften bir tane sırayla üretme yorumu geçerli değil.
- Yalnız planlanan tam batch için gereken kesin miktar alınacak; spekülatif artan girdi toplanmayacak.
- Kullanıcı diğer rollere geçmeden önce bu ortak mekanizmayı netleştirmek istiyor.

### Üretim rolleri ve ticaret

- Toolsmith, Armorer ve Weaponsmith kapsamda olacak ve mümkün olduğunca dinamik çalışacak. Gerçek çalışma zamanı tag/component verileri rolün çıktı sınıflandırmasında, canlı tarifler ve alternatif girdiler ortak planlamada kullanılacak.
- Bütün yönetilen rollerde vanilla trade ekranı kapatılacak; yalnız üreticilerle sınırlı değil. Yönetilmeyen dünyadaki bütün köylüler global olarak patchlenmeyecek.
- Weaponsmith metal-only değildir; diamond sword dahil uygun gerçek üretilebilir silah tarifleri kapsamda, malzeme sınırlaması yok. Fletcher'ın ranged alanı ayrı kalacak.
- Toolsmith için uygun gerçek kazma, balta, kürek ve çapa tarifleri teyit edildi. Rol çıktı kategorileri arasındaki çakışmaların teknik ayrımı henüz açık.
- Trading level yeni bir üretim gate'i olmayacak. Önceden onaylanmış rollerin kararları tekrar onaya açılmayacak.
- Küçük statik fallback eşlemesi ancak dinamik çözüm mümkün değilse düşünülebilir; henüz gerekli olduğu kabul edilmedi ve uygulanmasına karar verilmedi.
- Sandık kaynaklarını tüketerek gerçek üretim yapmaları eklenecek. Her rolün tam üretim/tarif şartnamesi henüz belirlenmedi.
- Ticaret ekranı bütün yönetilen rollerde kapalı; yönetilmeyen köylüler bu kapsamda değil.

### Smelter

- Smelter için crafting veya alaşım birleştirme olmayacak.
- Çıktısı ingot kategorisinde olan furnace/smelting ve blasting tariflerini seçme yönü kabul edildi.
- Girdinin adında raw bulunması tek başına ölçüt olmayacak; çiğ et gibi girdiler bu nedenle yanlış kapsama alınmayacak.

### Fletcher / Weaponsmith / Toolsmith / Armorer stok eşiği

- Fletcher, Weaponsmith, Toolsmith ve Armorer için durdurma eşiği ürün başına 5 adet. Kullanıcı önceki 10 adedi bu dört rol konuşulurken 5 olarak değiştirdi; sandık stoku 5 veya üzerindeyse ilgili item için yeni üretim başlatılmayacak. Başka roller kendiliğinden bu kapsama alınmayacak.
- Son çalışma yorumu: kullanıcı “Hepsini de çek” dedi. Ardından sesli açıklama önceden sınırlı bütün roller için 5 değerini (Mason 5 stack; Leatherworker ve dört ekipman rolü 5 adet; Cook limitsiz) belirtti; kullanıcı “Devam et kanki” dedi. Bu son yorum geçerli; yeni veya limitsiz bir role otomatik 5 eklenmeyecek.
- Cook/gıda için stok limiti yok. Kaynak ve çıktı kısıtları devam edecek.
- Köy içindeki kayıtlı/tabelalı sandıkların içindeki gerçek item miktarları toplanacak. Rastgele etiketsiz sandıklar sayılmayacak. Sandıkları sayma ifadesi sandık adedi değil, item toplamı anlamında.
- Köylü sayım yapabilmek için sandıkların yanına yürümek zorunda olmayacak. Olay güdümlü/cache yaklaşımı tercihi korunacak.
- Üretimdeki veya rezerve edilmiş itemlar ve köylü envanterindekiler stok sayımına dahil edilmeyecek. Kullanıcı bunları saymayı açıkça reddetti; yalnız sandık içeriği sayılacak.
- 5 bir üretim öncesi durma eşiğidir; kesinlikle aşılamayan üst sınır garantisi değildir. Kullanıcı yalnız sandık içeriğine dayalı sayımın eşzamanlı üretimde aşılabileceğini kabul etti. Birden fazla çıktı veren tarifler veya Chester taşırken geçici eksik sayım da aşım olasılığı doğurabilir. Rezervasyon/taşınan item sayımı yapılmayacak; gizli reservation veya karmaşık ek hesap sistemi eklenmeyecek.
- Unload edilmiş sandıkların sayımı henüz açık. Diğer rollerin stok kuralları ileride ayrı ayrı görüşülecek.

### Meslekler arası iş bölümü

- Fletcher kendi ok/yay/arbalet alanında ortak ranged üretim mekanizmasıyla çalışacak; ürün başına son stok eşiği 5 adet. Weaponsmith ile iş bölümü geçerli; Weaponsmith/Armorer/Toolsmith kararları yeniden açılmadı. Kesin item kategori çakışmalarının çözümü henüz açık.
- Fisherman için durgun su bulup güvenli kuru kıyıya gitme hareketi onaylandı. Görsel/native oltayla balıkçılık basitlik şartıyla ilk kapsamdan ertelendi. Suda üretim/loot davranışı açık; otomatik loot üretimi kararı yok. Pişirme görevi Cook'ta kalıyor.
- Diğer meslekler için asistanın saydığı ayrıntılar öneridir; onaylı uygulama kararı değildir. Kullanıcı rolleri tek tek ve yavaş konuşmak istiyor.

### Chester

- Yeni rol: Chester. Yakındaki sandıklardaki itemları eşleşen etiketli sandıklara taşıyan bir düzenleyici olacak.
- Chester köy alanındaki bütün sandıklara bakacak; etiketsiz sandıklar kaynak olabilir. İlgili envanter değişim eventiyle yanlış yerdeki itemı en yakın isim-substring eşleşen uygun sandığa fiziksel taşıyacak.
- Zaten doğru sandıktaki item sırf daha yakın hedef var diye taşınmayacak. Uygun hedef veya boş yer yoksa item kaynağında kalacak; son konuşmada güvenli temel davranış olarak onaylandı.
- Transfer öncesi ve sonrasında güncel hedef/izin doğrulaması ile kapasite kontrolü yapılacak; item kaybı/çoğaltma olmayacak. Sıra, tam yarıçap, fiziksel iş kuyruğu ve yarış ayrıntıları açık.

## İlk kod aşaması — geliştirme kapsamı

- Kullanıcı Minor Lord implementasyonuna başlamayı açıkça istedi. Fookie'ye özel küçük yaklaşım onayı kuralı bu ayrı repo için kapsam değildir; Fookie dosyalarına dokunulmayacak.
- İlk aşama Fabric 26.3 server-only scaffold, ortak isim-substring filtresi, en yakın uygun hedef sorgusu, transaction tabanlı güvenli exact transfer ve Chester'ın salt okunur hedef planlama temelidir. Köy kaydı/sign parser, olay bağlama, fiziksel yürüme ve role AI sonraki aşamalardır; ilk temel tamamlanmış mod diye sunulmayacak.
- Çözülmemiş isim kaynağı/normalizasyon, mesafe ölçütü ve tie-break politikası yardımcı fonksiyonlara dışarıdan verilir; gizli oyun kuralı seçilmez. Aktarım aynı server thread'inde, tek anlık transaction ile yapılır; yürüyüşler/tick'ler boyunca transaction tutulmaz.
- Role özel equipment helper, kaynak şablonu değiştirmeden kopyaya native unbreakable ekler. Oyuncu gear'ı ve üretim output'ları normal kalır; bütün itemlara global component eklenmez.
- Lisans seçimi, GitHub commit/push, gerçek dünya kurulumu veya EULA kabulü yapılmayacak. İzole build/test sonuçları ayrıca kaydedilecek.
- Kullanıcının son aşama hedefi Dokploy'da ayrı proje ve ayrı test world üzerinde Fabric 26.3 deployment ve mevcut sahip olunan domain altında test hostudur. Temel henüz tamamlanmamışken deploy yok; yeni domain/plan satınalma, ilgisiz uygulama/world/DNS üzerine yazma yok. Minecraft TCP/DNS/port, EULA ve gerekli ağ onayları hedef incelendiğinde ayrıca doğrulanacak. Vanilla client join yapılmadan yapılmış sayılmayacak.

## Teknik kısıt

- Bir mod itemının sunucunun item registry'sinde bulunması, vanilla istemciyle uyumlu olduğunu garanti etmez. Tamamen server-side ve vanilla-client gereksinimi korunarak bu uyumluluk ayrıca değerlendirilmelidir.

## Teknik yapılabilirlik kanıtı — Minecraft 26.3

Bu bölüm tasarım kararı veya çalışan mod testi değildir. Aşağıdaki semboller exact 26.3 artifact/API kaynaklarında doğrulandı; 26.2 dokümanı kanıt olarak kullanılmadı.

### Kaynak ve sürüm doğrulaması

- Mojang [resmî manifestindeki](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json) 26.3 kaydından [26.3 metadata](https://piston-meta.mojang.com/v1/packages/11f1aefe473eefba0af62e3286c33b692807b2eb/26.3.json) alındı. [Resmî server.jar](https://piston-data.mojang.com/v1/objects/33680f5f2ac32864d6d7cf5e56a705fdb3e05f4c/server.jar) indirildi; SHA-1 değeri 33680f5f2ac32864d6d7cf5e56a705fdb3e05f4c ile eşleşti. İçindeki META-INF/versions/26.3/server-26.3.jar sınıfları javap ile incelendi; sunucu çalıştırılmadı.
- [Fabric API 0.163.0+26.3 POM](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.163.0+26.3/fabric-api-0.163.0+26.3.pom), fabric-convention-tags-v2 sürümünü 4.10.3+6b5b47fd5d olarak bildiriyor. [Bu modülün kaynak JAR'ı](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-convention-tags-v2/4.10.3+6b5b47fd5d/fabric-convention-tags-v2-4.10.3+6b5b47fd5d-sources.jar) ve [26.3 ConventionalItemTags Javadoc](https://maven.fabricmc.net/docs/fabric-api-0.163.0+26.3/net/fabricmc/fabric/api/tag/convention/v2/ConventionalItemTags.html) incelendi.

### Doğrulanan exact semboller

- Minecraft net.minecraft.core.component.DataComponents: FOOD (FoodProperties), CONSUMABLE, TOOL, WEAPON ve EQUIPPABLE. Besin sınıflandırması için FOOD mevcut; CONSUMABLE tek başına besin demek değildir. EQUIPPABLE tek başına bütün armor kategorisini tanımlamaz.
- Fabric net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags: TOOLS, MINING_TOOL_TOOLS, MELEE_WEAPON_TOOLS, RANGED_WEAPON_TOOLS, ARMORS, HUMANOID_ARMORS, INGOTS ve IRON_INGOTS. Kaynaktaki kayıt yolları sırasıyla tools, tools/mining_tool, tools/melee_weapon, tools/ranged_weapon, armors, armors/humanoid, ingots ve ingots/iron. ARMORS oyuncuya uymayan zırhları da kapsayabilir; HUMANOID_ARMORS ayrıca mevcut. TOOLS ile WEAPON kapsamı rol seçiminde aynı kabul edilmemeli.
- Minecraft net.minecraft.tags.ItemTags içinde HEAD_ARMOR, CHEST_ARMOR, LEG_ARMOR, FOOT_ARMOR, SWORDS, AXES, PICKAXES, SHOVELS, HOES ve MELEE_WEAPON_ENCHANTABLE da mevcut. Enchantable tag'ler tüm üretim kategorilerinin eşdeğeri sayılmadı.
- MinecraftServer.getRecipeManager(); RecipeManager.getRecipes(), getRecipeFor(RecipeType, input, Level); Recipe.getType(), matches(input, Level), assemble(input), placementInfo(); PlacementInfo.ingredients(); Ingredient.items(), test(ItemStack), acceptsItem(Holder<Item>) doğrulandı. Bunlar Minecraft API'leridir; tümü Fabric API olarak adlandırılmamalı.
- RecipeType.SMELTING, BLASTING, SMOKING, CRAFTING ve SMITHING mevcut. Smelter için SMELTING/BLASTING tür filtresi ve çıktı ingot kategorisi birlikte değerlendirilebilir. SingleItemRecipe.input(), assemble(SingleRecipeInput) ve AbstractCookingRecipe.cookingTime() mevcut. SingleItemRecipe.result() protected; genel, public bir getResultItem varsayılmadı.

### Kanıtın sınırı ve açık teknik işler

- Bu semboller dinamik sınıflandırma ve canlı tarif incelemesi için teknik temel sağlar; bütün custom mod item/tariflerin otomatik çalışacağını kanıtlamaz. Mod itemları ilgili tag/component bilgilerini sağlamayabilir; registry kaydı tek başına rol kategorisini veya vanilla-client uyumluluğunu garanti etmez.
- Recipe.placementInfo() ve ingredient alternatifleri her özel recipe türünün tam makine, enerji, dünya durumu, ingredient/component ve yan ürün kurallarını tek başına açıklamayabilir. Çıktı girdiye bağlı olabilir; Recipe.matches/assemble ile gerçek aday girdinin değerlendirilmesi ve gerekirse özel recipe adaptasyonu gerekir. Özel türleri destekleme kapsamı henüz kararlaştırılmadı.
- Tag/component birleşiminden tam Toolsmith/Armorer/Weaponsmith/Fletcher ürün kapsamı çıkarma, kategori çakışmaları ve ilk/sonraki uygulanabilir tarifin seçim ayrıntıları açık. Tam batch azamileştirme yönü onaylandı; stok eşiğiyle etkileşimi ayrıca netleşecek. Bu dört rol için onaylı 5 adet eşiği sandık sayımına dayalı yeni üretimi durdurma kuralıdır; kesin tavan değildir. Statik fallback gerekliliği gösterilmedi. Uygulama veya derleme testi yapılmadı.

### Dar Fisherman görsel yapılabilirlik kontrolü — 26.3

- Aynı [resmî 26.3 metadata](https://piston-meta.mojang.com/v1/packages/11f1aefe473eefba0af62e3286c33b692807b2eb/26.3.json) içindeki [client.jar](https://piston-data.mojang.com/v1/objects/e877b6a07acd633fb3bb475002175cec036e7b87/client.jar) indirildi; SHA-1 e877b6a07acd633fb3bb475002175cec036e7b87 doğrulandı. Daha önce doğrulanmış resmî server-26.3.jar ile birlikte javap -p -c kullanılarak salt okunur incelendi; oyun/prototip çalıştırılmadı.
- FishingRodItem.use(Level, Player, InteractionHand) oyuncu alıyor. FishingHook'un atış constructor'ı FishingHook(Player, Level, int, int). Genel EntityType/Level constructor'ının varlığı villager-owner desteği anlamına gelmiyor.
- FishingHook.setOwner(Entity) genel görünse de getPlayerOwner() yalnız owner instanceof Player ise Player döndürüyor, aksi hâlde null. tick() null player-owner için discard() çağırıyor. retrieve(ItemStack) de player-owner gerektiriyor.
- Vanilla istemcide FishingHook.recreateFromPacket(...) geçerli player-owner yoksa hata kaydedip discard() çağırıyor. FishingHookRenderer.shouldRender(...) getPlayerOwner() null ise false döndürüyor; extractRenderState(...) ve getPlayerHandPos(Player, ...) çizgi başlangıcını oyuncudan hesaplıyor.
- Sonuç: gerçek villager'a doğrudan native bobber/olta çizgisi bağlamak basit server-only destek değildir. Sadece sunucudaki owner kontrolünü değiştirmek vanilla istemcinin renderer/owner kısıtını değiştirmez. Sahte oyuncu veya client mod varsayılmadı.
- VillagerRenderer içinde CrossedArmsItemLayer bulunması elde item görseli desteğidir; tam olta atma/bobber/çizgi davranışını kanıtlamaz. Native item/entity görseli ile tam balıkçılık aynı şey sayılmadı. Kullanıcı basit değilse istemediğinden alternatif görsel simülasyon/prototip tasarımına devam edilmedi.

### Dar vanilla meslek edinme kontrolü — 26.3

- Daha önce SHA-1 doğrulanan resmî 26.3 server/client artifact'leri salt okunur incelendi. AcquirePoi.create(...), AssignProfessionFromJobSite.create(), VillagerProfession.heldJobSite()/acquirableJobSite() ve VillagerData.withProfession(...) mevcut. AssignProfessionFromJobSite, kayıtlı vanilla meslekler arasından jobsite eşleşmesini bulup setVillagerData(...) ve refreshBrain(...) çağırıyor.
- POI bootstrap'ta blast furnace → Armorer, smoker → Butcher, barrel → Fisherman gibi mevcut ilişkiler görüldü. Dolayısıyla aynı vanilla işyerini ek rol için yeniden kullanmak mevcut meslekle çakışma/ayrıştırma kararı gerektirir; böyle bir eşleme seçilmedi.
- Vanilla profession kimliğini/görünüşünü koruyup sunucuda özel iş davranışı bağlama yönü bu giriş noktalarıyla tutarlıdır; uygulama/derleme testi yapılmadı. VillagerProfessionLayer görünüşü profession kimliğinin istemcideki kaynaklarından alıyor; yeni texture/profession vanilla istemcide kendiliğinden var sayılmayacak.
- Son kullanıcı tercihi vanilla mesleklerde doğal edinme, ek rollerde name tag olduğu için yeni profession/POI/network/texture çözümü araştırmasına devam edilmedi. Yeni client mod veya kaynak paketi gereksinimi seçilmedi. Miner'ın mevcut vanilla profession ile önceliği açık; Mine tabelası rol atayıcı yapılmadı.

### Native UNBREAKABLE kontrolü — exact 26.3

- Daha önce resmî metadata'dan alınmış, SHA-1 doğrulanmış [26.3 server.jar](https://piston-data.mojang.com/v1/objects/33680f5f2ac32864d6d7cf5e56a705fdb3e05f4c/server.jar) içindeki sınıflar javap -p -c ile salt okunur incelendi.
- DataComponents.UNBREAKABLE exact tipi DataComponentType<Unit>. ItemStack.isDamageableItem(), MAX_DAMAGE ve DAMAGE bulunmasına ek olarak UNBREAKABLE'ın bulunmamasını şart koşuyor; UNBREAKABLE varsa false dönüyor.
- ItemStack.processDurabilityChange(int, ServerLevel, ServerPlayer), isDamageableItem() false ise hemen 0 dönüyor. Native component bu standart hasar yolunda gerçek dayanıklılık kaybını engelliyor; yalnız tooltip adı değildir.
- Unbreaking enchant farklı: aynı artifact'teki data/minecraft/enchantment/unbreaking.json, minecraft:item_damage altında minecraft:remove_binomial olasılıklı azaltma etkisi tanımlıyor. Bu enchant native UNBREAKABLE ile aynı garanti değil. Yalnız itema Kırılmaz ismi yazılması da component uygulamak değildir.
- Bu kanıt vanilla standart durability yoluna ilişkindir; itemı komut/mod ile değiştirme veya özel hasar kodlarını evrensel olarak engelleme iddiası değildir. Son kullanıcı kararı bu native component yöntemini onaylıyor. Component hiçbir ekipmana uygulanmadı, sürekli tamir tick döngüsü yazılmadı; mod testi yapılmadı. Dar exact 26.3 doğrulaması tamamlandı.

## Açık konular

- Tabeladaki köy işaretinin kesin metni ve boyut yazımının kesin biçimi.
- Sınırların dahil edilme biçimi ve kapsayıcı blok sayısı: merkezden iki yöne 25 uzaklık için kesin blok koordinatı/kapsama kuralı.
- Duvarın kesin vanilla blok kimliği.
- Arazi yüzeyi belirlenirken ağaç ve yaprağın nasıl ele alınacağı açık. Su yüzeyindeki sınır kesimine duvar koymama kararı onaylandı.
- Kapıların yerleri ve davranışı; mevcut yapıların nasıl korunacağı.
- Ek rol name tag anahtarlarında alt dize eşleşmesi, birden fazla anahtar varsa öncelik ve mevcut vanilla profession ile çakışma kuralları açık. Küçük/büyük harf duyarsızlığı önceki ek rol kararı olarak korunur; vanilla mesleklerde tag gerekmiyor.
- İlk aktif tarifin ve birden fazla uygulanabilir sonraki tarif arasından seçimin ayrıntıları açık. Son karar tek aktif tarifte tam batch sayısını azamileştirme ve malzeme kalmayınca başka uygulanabilir tarife geçme; zorunlu round robin/fair scheduling yok. Sandık/çalışma istasyonu arama yarıçapı ve içerik değişimlerinin tam olay kapsamı açık.
- Plan yetersizleşirse toplama sonrası iade davranışı ve güncel en yakın eşleşen hedef kuralı onaylandı. Uygun hedef hiç yoksa veya doluysa iade/çıktı fallback davranışı, eşzamanlı kaynak yarışının teknik çözümü ve reload/cache invalidasyonu ayrıntıları açık.
- Gerçek recipe remainder/container çıktıları (örneğin kek tarifindeki kova) henüz konuşulmadı. Yalnız final product hedefiyle bu teknik ayrımın nasıl ele alınacağı açık; remainder silme veya yok sayma varsayılmayacak.
- Stok eşiğine tabi roller için tam batch büyüklüğüyle kendi stok eşiğinin etkileşimi açık. Sessizce yeni adet tavanı veya ek rezervasyon sayımı eklenmeyecek.
- Önceki Fuel-only kaynak kuralının yalnız isim filtresiyle nasıl bağdaştırılacağı ve yakıtın kesin kaynak izinleri açık. Fuel magic alias olarak onaylanmış sayılmayacak; izinsiz kaynak alma da varsayılmayacak.
- Kömür dışındaki yakıt türleri henüz kararlaştırılmadı.
- Sandık etiketlerinde tam namespace desteği önerilen teknik yön; henüz kesinleşmiş karar değil. Boşluk/underscore/case normalizasyonu ve namespace belirsizliğinde hangi itemın tercih edileceği açık.
- İsim filtresinin normalize registry path üzerinde mi yoksa localized display name üzerinde mi çalışacağı açık. Case, boşluk/underscore ve namespace kuralları kesinleşmedi. *iron* wildcard sözdizimi onaylanmadı. En yakın sandık için uzaklık ölçümü ve eşit uzaklıkta seçim açık.
- Chester için kesin iş yarıçapı, yürüyüş/iş kuyruğu ve döngü/rekabet çözümleri açık. Köydeki bütün sandıkların kaynak olabilmesi, doğru yerdeki itemın bırakılması ve hedef/yer yoksa kaynaktan alınmaması onaylandı. En yakın eşleşen hedef kuralı geçerli.
- Bütün yönetilen rollerin trade ekranı kapalı olacak; kapsam dışında kalan köylülerin trade davranışı değiştirilmeyecek.
- Geçici taşımanın ayrıntıları ve hedef yok/dolu fallback davranışı, diğer üretim rollerinin tarif ve davranış ayrıntıları, ticaretin kalan kapsamı, dostluk ve büyük köylerin aktifliği ileride konuşulacak. Yukarıda onaylanan üretim, çıktı ve ticaret ekranı kuralları geçerli.

## Geçersizleşen kararlar — tarihçe (2026-10-19)

Bu bölüm yürürlükteki kuralları tanımlamaz; sonraki kullanıcı kararlarıyla değişen eski yorumları korur.

- Önceki Food/Raw Food semantik girdi/çıktı kategorileri ve ayrı Raw Food → Food sandığı zorunluluğu kaldırıldı. Etiketler artık yalnız isim substring filtresi; girdiler birden fazla uygun sandıktan alınabilir.
- Önceki geniş kategori ile exact item çözümlemesi ayrımı ve iron ingot özel hedefinin ingot genel hedefine önceliği kaldırıldı. En yakın eşleşen çıktı sandığı seçilir.
- Önceki yalnız Fuel tabelalı sandıktan kömür alma kuralı yeni isim filtresiyle yeniden netleştirilecek açık konuya taşındı. Bu tarihçe Fuel alias veya başka sandıklardan serbest yakıt alma izni değildir.
- Food sandığı yokken Cook envanterinde bekletme kararı daha önce kaldırıldı; üretici envanterinde bekleyen stok yok. Eksik/dolu çıktı hedefi davranışı açık.
- 10 eşiğini genel ürün kategorilerine yayma yorumu daraltıldı: mevcut rol listesi yalnız Fletcher, Weaponsmith, Toolsmith ve Armorer. Cook/gıda için 10 limiti yok; diğer roller ileride konuşulacak.

- Her tariften bir tane sırayla üretme yorumu kaldırıldı. Tek aktif tarifte üretilebilir tam batch sayısı azamileştirilir; malzeme kalmayınca başka uygulanabilir tarife geçilir. Zorunlu round robin/fair scheduling yok.

- Açlık sistemi konuşulurken Farmer için bütün sandıklarda tabela/isim filtresi olmadan yiyecek arama, yiyecek varsa yürüme ve 3 yemek kalınca arama eşiği not edilmişti. 3 yemek birimi kesinleştirilmemişti; 3 ikon = 6 puan yorumu onaylanmamıştı. Kullanıcı daha sonra özel açlık sistemi eklememeye karar verdi; bu eşik ve arama döngüsü tamamen aktif kapsamdan çıktı. Önceki tanımlı Food/Cook sandığı yorumu da onaylı kural değildi.

- Farmer lifecycle'ının henüz onaylanmadığı ve depo entegrasyonunun yalnız öneri olduğu önceki durum son kullanıcı onayıyla değişti. Vanilla ekim/hasat, yeniden ekim girdisini ayırma ve fazlayı sandığa bırakma tasarımı onaylandı; ekmek crafting'ini kapatma kararı alındı. Kod henüz yapılmadı.

### Fisherman: erteleme öncesi onaylanan yön

- Fisherman uygun suya gider, oltayla balık tutar; envanteri dolunca item başına en yakın uygun isim/tabela filtreli sandıkları sırayla gezerek boşaltır, sonra balığa döner. Kullanıcı bu döngüyü açıkça onayladı. Bu fishing biriktirme lifecycle'ı üreticilerdeki spekülatif stok tutmama kuralından ayrıdır.
- Gerçek/vanilla benzeri fishing ve balık dışı normal fishing loot da istenmişti; statik yalnız balık drop listesi veya yalnız balık spawn yaklaşımı seçilmedi. Pişirme Cook'a aittir.
- Sabit olta ekipmanı yönü konuşuldu; Fisherman başka araç/silah tutmayacak. Eldeki ekipman kısıtı loot taşımadan ayrıdır. Sonsuz dayanıklılık mı yenilenen olta mı açık; bedava item farm kararı yok.
- Normal loot koşullarını koruma yaklaşımı konuşuldu; tam süre/oran/enchant kuralları, araç dayanıklılığı ve hedef yok/dolu fallback açık kaldı.
- Çok basit native destek yoksa görsel balıkçılık yapılmasın kararıyla yalnız görsel/native olta davranışı ilk kapsamdan ertelendi. Sonraki onayla durgun su bulup güvenli kuru kıyıya gitme hareketi kapsamda; suda üretim/loot davranışı hâlen açık. Kod yapılmadı.

- Leatherworker için önceki 5 adet stok eşiği önerisi kesinleşmedi; kullanıcı son olarak ürün başına 3 adet seçti.

- Fletcher/Weaponsmith/Toolsmith/Armorer için önceki ürün başına 10 adet kararı son kullanıcı düzeltmesiyle 5 adet oldu. Mason 10 stack ve Leatherworker 3 adet ayrı kararları korundu. Asistanın diamond sword tarifi yok ifadesi yanlış bulundu ve düzeltildi; Weaponsmith için metal-only sınırlaması yok.

- Son limit görüşmesinde kullanıcı “Hepsini de çek”, açıklama sonrasında “Devam et kanki” dedi. Önceki Mason 10 stack ve Leatherworker 3 adet değerleri geçersizleşti; aktif çalışma yorumu Mason 5 stack, Leatherworker 5 adet, Fletcher/Weaponsmith/Toolsmith/Armorer 5 adet, Cook limitsiz. Yeni limitsiz role otomatik eşik eklenmez.

- Cartographer için boş harita üretme/kopyalamanın henüz onaysız olduğu önceki durum değişti: son onay yalnız boş harita üretimi ve 5 adet eşik. Kopyalama ve keşif haritası yok.

- Miner için önceki tabela Y katmanında başlama anlatımı son bir-alt kuralıyla netleşti: kazı alt sırası Mine tabela bloğunun Y seviyesinden 1 aşağıda. Seçmeli kazı drop'u toplama/item silme düşüncesi geçersiz; tüm kazı drop'ları toplanacak.

- Miner için düz ana koridor/yan kol önerisi reddedildi; son karar Mine merkezli, zamanla yarıçapı artan dairesel/radyal desen ve iki blok aralık. Köy dışı mining kapsamı son kararla izinli; chunk politikası hâlen açık. Araç dayanıklılığına ilişkin eski belirsizlik son kalıcı/kırılmaz meslek aracı kararıyla kapandı; tier/hız/harvest sınırları açık kaldı.

- Miner dönüş/iniş yolunu hafızaya kaydetme önerisi reddedildi. Son karar güncel dünyadan her seferinde yol bulma/geçerlilik kontrolü; yürünmüş-blok kayıt koruması onaylanmadı.

- Miner mağara kapatma kapsamının açık olduğu önceki durum netleşti: tüm mağara doldurulmaz; yürünebilir tünelin bir alt zemini, sağ/sol ve üst açıklıkları envanterdeki uygun bloklarla kapatılır.

- Miner deseninin önceki genel dairesel/radyal anlatımı son kararla 10 blok düz giriş ve ardından dışa genişleyen spiral olarak netleşti. İki blok aralık korunur; kesin merkez/voxel bağlantısı teknik plana bırakıldı. Pickaxe tier'ın açık olduğu eski durum değişti: diamond ve Efficiency seçildi, Efficiency seviyesi hâlen açık. Sıfır süreli her-blok/tek-tick sınırsız damar çıkarma yorumu onaylı değildir.

- Miner için Efficiency isteği son kullanıcı kararıyla kaldırıldı: “Dümdüz diamond kazma verelim ... yavaş yavaş kazsın” ve “gerek yok”. Aktif araç sade/kendinden/kırılmaz diamond kazma, normal araç-blok hızıdır. Sonsuz genişleme yorumu da Mine alt satırındaki sayısal maksimum yarıçapla sınırlandı; 1000 radius, yol uzunluğu veya çap değildir.

- Name tag ile rol atamayı tamamen kaldırma yönü ara karardı; son kullanıcı düzeltmesiyle geçersizleşti. Aktif ayrım vanilla roller için doğal jobsite/profession, ek roller için name tag. Kullanıcı ek rolü yalnız Miner sandığını ifade etti; Cook/Smelter/Chester da ek rollerdir, Warrior/Archer ayrıntısızdır. Blanket kaldırma uygulanmadı; kod yok.

- Warrior sabit diamond sword ve Archer full leather/Infinity gibi önceki tercihler son random ekipman isteğiyle yeniden netleştirilecek. Infinity kesin aktif karar değildir; random malzeme/enchant havuzu seçilmedi. Cook adı son Cooker ifadesiyle alias/normalize ad açısından açık kaldı.

- Native UNBREAKABLE yöntemine ilk itiraz, Unbreaking enchant ile item data component farkı açıklandıktan sonra son açık onayla aşıldı. Aktif yöntem armor/weapon/tool için minecraft:unbreakable component; cron/Mending+XP/per-tick repair seçilmedi. Kod henüz uygulanmadı.

## Yorumlama notu

Sözlü aritmetik kaymaları (örneğin 100x50 için 150 alan denmesi) bir hesaplama veya uygulama kuralı olarak alınmayacak. Belirsiz noktalar bu dosyada karar gibi gösterilmeyecek.