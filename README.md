# Yörünge Koşusu

Özgün, çevrimdışı ve yatay oynanan bir tünel koşusu. Android APK ve yüklenebilir PWA olarak üretilir. Oyun internet, hesap veya depolama izni istemez.

**[Tarayıcıda oyna](https://aranelpalantir.github.io/yorunge-kosusu/)**

## Kurulum

**Android:** `dist/Yorunge-Kosusu.apk` dosyasını cihaza kopyalayıp açın. Android isterse "Bilinmeyen uygulamaları yükle" iznini verin. APK yerel anahtarla imzalıdır, Play Store paketi değildir. Eski kurulumu silmeden güncelleyebilirsiniz; ilerleme korunur.

**PWA:** `pwa/` klasörünü HTTPS üzerinden yayınlayın ve adresi tarayıcıda açın. Ana menünün üstündeki **Yükle** düğmesi oyunu cihaza kurar. İlk açılıştan sonra çevrimdışı çalışır. İnternet varken yeni derleme bir sonraki açılışta gelir. Oyun açıkken yayınlanan derleme, oyun öne geldiğinde indirilir; oyun ana menüdeyse hemen, koşudaysa ana menüye dönünce yenilenir. İlerleme korunur. APK otomatik güncellenmez.

| Platform | Yükle düğmesi |
| --- | --- |
| Android: Chrome, Edge, Samsung Internet | Tarayıcının yükleme penceresini açar |
| PC: Chrome, Edge | Tarayıcının yükleme penceresini açar |
| iPhone ve iPad | Paylaş → Ana Ekrana Ekle adımlarını gösterir |
| Mac: Safari | Dosya → Dock'a Ekle adımlarını gösterir |
| Android: Firefox | Tarayıcı menüsündeki adımları gösterir |
| PC: Firefox | Görünmez, tarayıcı yüklemeyi desteklemiyor |

Oyun yüklü olarak açıldığında düğme görünmez.

## Oynanış

- **Sol / sağ:** Tünelin duvarlarında ilerler. Basılı tutulabilir, havada da çalışır.
- **Zıpla:** Boşluklardan, duvarlardan ve uzaylıların üzerinden geçirir. Yön ve zıplama birlikte kullanılabilir.
- **Sağ üst:** Müzik ve duraklatma. Android geri düğmesi de oyunu duraklatır.
- **Klavye:** A/D veya ok tuşları, boşluk ile zıplama.

## İçerik

- **50 bölüm:** Sırayla açılır, her parkurun düzeni farklıdır ve denemeler arasında değişmez. Hız dört bölümde bir artar. Bölüm bitince sonraki bölüm otomatik başlar.
- **Sonsuz Yörünge:** Her denemede yeni üretilen tünel, başlangıçta bir kalkan ve kaydedilen mesafe rekoru.
- **Tehlikeler:** Delikler, duvarlar, zincirleme çöken kırmızı plakalar ve geç bölümlerde devriye gezen uzaylılar. Kırmızı plaka inmeye başladıktan sonra 0,24 saniye içinde zıplayarak kurtulabilirsiniz.
- **Toplanabilirler:** Çarpan ve Yörünge Çılgınlığı sağlayan kristaller, tek hatayı affeden kalkan küresi, sıçrama plakaları ve teğet geçiş ödülü. Kristaller birikir, henüz harcanamaz.
- **Yıldızlar:** Biten bölüm 2 yıldız, tek seferde hatasız geçilen bölüm 3 yıldız verir.
- **Müzik:** Dokuz parça; müzik ve efektler ayrı ayrı açılıp kapatılır. Kaynaklar [MUSIC_CREDITS.md](MUSIC_CREDITS.md) içindedir.

## Karakterler

| Karakter | Yetenek | Hareket |
| --- | --- | --- |
| Keşif | Dengeli koşu ve zıplama | Düzenli adımlar, sallanan anten |
| Nova | %25 yüksek zıplama, %10 uzun havada kalma | Zıplayarak koşu, havada çırpınan kanatçıklar |
| Atlas | %10 hızlı koşu | Ağır adımlar, ayak tozu ve alev izi |
| Özel | Renk, başlık ve yetenek seçilir (süzülme: %15 uzun havada kalma) | Seçilen yeteneğin hareketini kullanır |

Karakterlerin koşu, zıplama, iniş, yön değiştirme, düşme, çarpma ve sevinç animasyonları vardır. Animasyonlar yalnızca görseldir; fizik ve çarpışmaları etkilemez.

## Derleme

Java 17, Android platform 35 ve Build Tools 35.0.0 gerekir. SDK `.tools/android-sdk` konumundadır.

```powershell
./build.ps1
```

Komut APK'yı `dist/Yorunge-Kosusu.apk` olarak derleyip imzasını doğrular ve `pwa/` klasörünü günceller. İmza anahtarı `.tools/prototype.jks` içindedir; aynı kurulumu veri kaybetmeden güncellemek için korunmalıdır.

Sesleri yeniden üretmek için:

```powershell
python tools/generate_audio.py
```

## Kod yapısı

Oyun kodu iki yerdedir: Android için `src/com/yorunge/runner/GameView.java`, PWA için `pwa/game.js`. Görsel ve oynanış değişiklikleri ikisine de uygulanmalıdır. Bölüm verisi ve sesler derleme sırasında PWA'ya aktarılır.

## Testler

```powershell
javac -encoding UTF-8 -cp build\classes -d build\tests tests\TrackTest.java tests\CollapseTest.java
java -cp "build\tests;build\classes" TrackTest
java -cp "build\tests;build\classes" CollapseTest
python tests\audio_test.py
```

`tests/android/CampaignTest.java` gerçek oyun ekranıyla çalışan Android testidir ve emülatör veya cihaz gerektirir.

## Diğer belgeler

- [PRODUCT.md](PRODUCT.md): Ürün tanımı ve oyun kuralları
- [DESIGN.md](DESIGN.md): Görsel tasarım ve animasyon kuralları
- [MUSIC_CREDITS.md](MUSIC_CREDITS.md): Müzik kaynakları
