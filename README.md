# masa-saati

Yatay tam ekran dijital masa saati. Büyük 24 saat biçimli saat saniyeleriyle güncellenir; Türkçe tarih saatin altında görünür.

- Kenar alanlarına dokununca parlaklık veya renk paneli açılır.
- Dokunma kesildiğinde paneller ve kenar işaretleri 1 saniye içinde kaybolur.
- Yazı renk panelinde ana renkler dahil 35 renk vardır.
- Ad imza yalnızca renk paletinin en altında görünür.
- Krem kabartma uygulama simgesinde 23:59 yazar.

GitHub Actions derlemesi, indirilebilir saat.apk dosyasını saat-apk adlı artifact olarak yayımlar.

Sürüm APK’sı `download/saat.apk` konumundadır. Kaynak değiştiğinde Actions önce `saat-unsigned` çıktısını oluşturur. Bu paketi sabit yayın anahtarıyla çevrimdışı imzalayıp aynı konuma koyun. İş akışı sertifikayı ve paket içeriğini yeni derlemeyle karşılaştırdıktan sonra `saat-apk` çıktısını yayımlar ve Android 15 üzerinde açılış testi yapar.
