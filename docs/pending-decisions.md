Current runtime checkpoint: 2026-10-19, 83 tests, native role jobs installed locally.

See [runtime-status.md](runtime-status.md) for current implementation, latest user decisions, live-world evidence and limits. This file below retains historical checkpoint/design text; older claims that role jobs are absent or these latest decisions are pending are superseded.

---

# Bekleyen kararlar — güncel durum

Onaylananlar tekrar sorulmayacak:

- Village ilk satır, XxZ ikinci satır. Merkez-X dahil / merkez+X hariç; 25x25 tam 50×50 blok. Marker parser, loaded sign index ve çakışma güvenliği derlendi/test edildi. Bu onay mevcut yapıları değiştirme veya alan sahipliği yetkisi vermez.
- Literal substring/includes her zaman geçerli; iron_ingot da bu metni içeren bütün isimlerle eşleşir. Exact-match istisnası yok.
- Gerçek recipe remainder/kap/kova çıktıları silinmez; güncel en yakın eşleşen uygun sandığa fiziksel olarak iade edilir. Hedef yok/doluysa girdi tüketmeden bekleme. Native cake/3 bucket ve rollback/no-dupe testleri mevcut.
- Yalnız localhost test klasörü için EULA açıkça kabul edildi. Sunucu başlatıldı ve gerçek umudik katılımı doğrulandı.
- Sonraki açık onayla yalnız 127.0.0.1:25578 test sunucusu offline-mode yapıldı ve umudik OP oldu. Dokploy/auth/firewall değişikliği yapılmadı.

Hâlen açık:

1. Sandık isim kaynağı/normalizasyon: registry path mi localized name mi; case, boşluk/underscore, namespace işlemleri. Includes onayı bunları seçmedi. Bu canlı item-label adaptörünü bloke eder; ortak substring/transaction kodunu bloke etmez.
2. Ek rol tag / vanilla profession önceliği, Cook/Cooker kesin alias, random Warrior/Archer ekipman havuzu ve ilgili combat kuralları.
3. Mesafe metriği/eşit uzaklık seçimi, kapsam/koruma/yerleşik yapılar ve duvar su/arazi davranışındaki açık noktalar. Güvenlik için belirsiz/çakışan Village alanı otomatik iş başlatmaz; blok değişikliği yok.
4. Rol matrisindeki diğer açık politikalar ve somut yürüme/makine/kalıcı görev uygulamaları. Bunlar tam modun tamamlandığı anlamına gelmez.
5. Dokploy kaynak bütçesi, yeni ayrı proje/port/domain/DNS ve deployment önkoşulları. Hiçbir deployment yapılmadı; localhost adresi Dokploy adresi değildir.

Bu bekleyenler reddedilmiş işlem değildir. Bağımsız kodlama sürer; onaylanan geometrinin parser/index kısmı artık aktiftir, çalışan test sunucusunda hâlâ önceki doğrulanmış JAR bulunmaktadır.