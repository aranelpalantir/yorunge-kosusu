# Yörünge Koşusu Tasarımı

Yatay oynanan, deneyim odaklı bir oyun. Özgün geometrik uzay dünyasında derin mavi boşlukta bölümlü sekizgen bir tünel ve küçük keşif robotları vardır. Yazılar açık renkli ve kolay okunurdur. Tünel oynanabilir ekranı kaplar, boş plakalardan yıldızlar görünür.

Büyük başparmak düğmeleri alt köşelerde, müzik ve duraklatma sağ üsttedir. Üst bilgi alanı bölüm kimliğini ve ilerlemeyi ayrı gösterir. Menü, duraklatma, başarısızlık ve bitiş ekranlarının her birinde tek bir kehribar ana düğme bulunur. Tüm metinler Türkçedir ve sistemin sans-serif yazı tipini kullanır. Tüm şekiller kodla çizilir.

Erişilebilirlik sınırı: Canvas ile çizilen oyun görseldir, tam bir ekran okuyucu gezinme yapısı sunmaz.

## Engeller
Kırmızı plakalarda onları güvenli zeminden ayıran açık renkli bir çatlak işareti vardır. Başlangıç duvarı kehribar rengidir, tünelin tüm yüzlerinden içeri doğru yükselir ve gölgeli ön ve iç yüzleri vardır; oyun başlarken robot otomatik zıplayarak aşar. Tünelin tamamını saran tehlike halkaları zıplamayı, tehlike şeritlerinin yanındaki dönen koridorlar yön değiştirmeyi gerektirir. Uzaylılar geç parkurların toparlanma bölümlerinde, gerçek deliklerin arasında boylamasına devriye gezer.

## Çöküş geri bildirimi
Geri dönüşü olmayan düşüşten önce 240 ms'lik bir iniş süresi vardır. Çöküş yalnızca ortak kenarlardan, komşu tünel yüzleri dahil, 110–170 ms arayla yayılır. Kırmızı plakalar karakterle aynı düşüş miktarını kullanır. Kopan parça görüş alanından çıkana dek görünür kalır. Kamera düşüşün bir kısmını takip eder; üst bilgi ve dokunma alanları yerinde kalır.

## Dünya ve gezinme
Menünün alt kenarında 50 numaralı bölümün seçilebilir veya kilitli olduğu, yatay kaydırılan bir şerit vardır. Turkuaz, mor, mavi, pas ve orman paletleri parkurlar arasında değişir; önizleme gerçek sonraki parkurun düzenini kullanır. Otomatik geçişlerin üzerinde kısa bir bitiş yazısı görünür. Kamera dönüşü oyuncuyu kısa bir gecikmeyle izler, böylece zıplarken yana kayma görünür.

## Çevresel anlatım
Parkurlar düzen etiketleri yerine yer adları taşır. Seyrek bakım kazımaları, 22. bölümden itibaren devriyelerde ve robotun başının arkasında görünen küçük üç çizgili işareti paylaşır. Altı kısa metin parçacığı, bölüm sonundaki sakin bölümün başında, her konumda kurulum başına bir kez görünür. Bölüm bitmeden okunabilmesi için yarı saydam koyu bir zemin üzerine yazılır; açılır pencere, toplama sayacı veya açıklayıcı başlık kullanılmaz.

## Final sahnesi
50. bölümün sonlarında tünelin ucundaki ışık büyür ve duvarlara sıcak bir renk yansıtır. Bitişte üst bilgi ve düğmeler kalkar, dokunuşlar yok sayılır. Robot 1 saniyede ışığa yürür, sırt ışığı ve anteniyle iki kez sinyal verir, ışık iki kez nabız atarak karşılık verir. Robot arkasına dönüp gülümser, ekran yaklaşık 1 saniyede sıcak beyaza döner ve bitiş ekranı bu beyazdan açılır. Sahne 3,6 saniye sürer.

## Ses kontrolleri
Sol üstteki parça seçici adlı parçalar arasında dolaşır ve sıra/toplam bilgisini gösterir. Sağ üstteki müzik ve efekt anahtarları ayrıdır. Ana menü ve duraklatma ekranında kontroller aynı yerdedir.

## Yıldızlar ve simgeler
125 sabit konumlu yıldız parlak renklerle çizilir, aralarında daha büyük parlak yıldızlar bulunur. Özgün simgeler 24 birimlik çizgi simgelerdir ve 2 birimlik yuvarlak uçlu çizgiler kullanır. Yazı ve simge grup olarak ortalanır, yazı düğme genişliğine göre küçülür. Kilitli bölüm kartlarının sağ üstünde küçük bir kilit, seçili kartta onay işareti vardır.

## Yükleme
PWA ana menüsünde üst ortada ikincil tarzda bir Yükle düğmesi bulunur; APK'da yoktur. Kendiliğinden açılır pencere gösterilmez. Tarayıcı destekliyorsa kendi penceresi açılır, desteklemiyorsa üç adımlı kısa bir panel görünür ve tek dokunuşla kapanır. Simge, kehribar tünel halkası içinde robotun başını gösterir; ana ekran kırpmaları için ayrı güvenli alanlı sürümleri vardır.

## Karakter seçimi
Karakter kartları 112 birim yüksekliğindedir; canlı robotu, adını ve kısa yetenek açıklamasını gösterir. Seçili kartta renkli çerçeve ve onay rozeti bulunur; robotu seçilince zıplar ve aralıklarla el sallar. Diğer robotlar yerinde nefes alıp göz kırpar.

## Özel tasarım
Solda canlı önizleme, sağda renk, başlık ve yetenek seçimleri bulunur. Seçimler dokunulduğu anda kaydedilir, Tamam düğmesi menüye döner. Önizleme seçilen yeteneğin koşusuyla çalışır ve her 3,2 saniyede bir o yeteneğin gerçek havada kalma süresiyle zıplar; yükseklik karta sığması için ölçeklenir. Yükseklik farkı korunur. Başlık yalnızca görünümü, yetenek fizik ve koşuyu belirler.

## Karakter animasyonu
Robotlar kodla çizilen eklemli figürlerdir: dizli bacaklar, elli kollar, gövde ve ayrı dönen bir kafa. Animasyonlar yalnızca görseldir.

- **Koşu:** Sırayla atılan adımlar, bacaklara ters sallanan kollar, gövde salınımı ve geriden gelen anten hareketi.
- **Zıplama:** Kalkışta uzama, önce yükselip sonra açılan kollar, inişte yaylı ezilme.
- **Yön değiştirme:** Gövde dönüşe yatar, kafa önden döner.
- **Yeteneğe göre koşu:**
  - Dengeli: Düzenli adımlar.
  - Yüksek zıplama: Zıplayarak koşu.
  - Hızlı koşu: Ağır adımlar, ayak tozu ve alev.
  - Süzülme: Alçak koşu, havada yana açılan kollar.
- **Başlık tipi:** Kanat başlığında havada hızlı çırpınan kanatçıklar vardır.
- **Tepkiler:**
  - Düşüş: İri gözler ve çırpınan kollar.
  - Çarpma: Saçılan parçalar, kırmızı parlama ve kısa ekran sarsıntısı.
  - Yeniden başlama: Çarpı gözler ve dönen yıldızlar.
  - Bölüm sonu: Havaya kalkan kollar ve konfeti.

## Uzaylı animasyonu
Uzaylılar dalgalanan eteklerle süzülür. Gövdeleri esner, göz kırpar, gözleriyle robotu izler ve antenleri sallanır. Uzaktaki uzaylılarda ince ayrıntılar çizilmez. Oyuncunun şeridinde yaklaşan uzaylı mercan rengine döner ve etrafında yanıp sönen bir uyarı halkası çıkar.
