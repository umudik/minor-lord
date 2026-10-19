Current runtime checkpoint: 2026-10-19, 83 tests, native role jobs installed locally.

See [runtime-status.md](runtime-status.md) for current implementation, latest user decisions, live-world evidence and limits. This file below retains historical checkpoint/design text; older claims that role jobs are absent or these latest decisions are pending are superseded.

---

# Onaylanan recipe remainder / returned container davranışı

Kullanıcı boş kova/kap gibi gerçek recipe remainder çıktılarının kayıpsız iadesini onayladı. Önceki onay bekleme durumu geçersizdir.

- Gerçek matches/assemble/getRemainingItems sonuçları kullanılır; native cake tam üç bucket döndürür.
- Normal ürün ve tüm remainder miktarları için güncel eşleşen hedef ve ortak kapasite birlikte, rollback edilen Fabric transaction simülasyonuyla kontrol edilir. Hedef yok/doluysa hiçbir girdi tüketilmez.
- Gerçek üretim NPC'nin geçici carried output buffer'ına yapılır. Uzak sandığa teleport yok; worker fiziksel yürüyüp teslimde güncel en yakın eşleşen hedefi tekrar çözmelidir. Kapasite rezervasyonu yoktur.
- Bütün exact input tüketimi ve bütün ürün/remainder çıktıları tek transaction içinde ya tamamlanır ya geri alınır. Silme/drop veya final ürüne dönüştürme yok.
- Tek hedefli eski overload kapları yanlış ürün sandığına göndermemek için REMAINDER_TARGET_REQUIRED verir; açık dönüş hedefli craftOneWithReturns onaylanan davranışı uygular.
- Testler gerçek native cake girdileri, üç bucket korunumu, geçici buffer, ardından exact delivery, tekrar aktarımda dupe engeli, eksik/dolu hedefte girdi korunumu ve iki ürünün ortak slot kapasitesini kapsar.

Çok tick süren fiziksel makine/worker persistence hâlâ uygulanmalıdır. Hedef üretimden sonra değişirse itemlar güvenli taşıma durumunda kalır; silinmez. Bu ilke onaylıdır; somut save/resume ve dünya iş döngüleri henüz tamamlanmamıştır.