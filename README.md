# MeshengerTR Windows - P2P Emergency Network & Disaster Beacon

**MeshengerTR Windows**, hücresel şebekeler ve internet altyapısı çöktüğünde bile yerel Wi-Fi, Ethernet ve Mesh ağları üzerinden kesintisiz P2P acil durum iletişimi sağlayan **Compose Multiplatform (Kotlin Desktop)** uygulamasıdır.

Bu uygulama **ISO 22324 Uluslararası Akıllı Acil Durum Standartları** ile tam uyumlu olup Android sürümü (`meshenger-android`) ile çift yönlü doğrudan iletişim kurabilir.

---

## 🌟 Temel Özellikler

- **📡 Afet Kipi (Disaster Beacon Network)**:
  - Yerel ağdaki tüm Android ve Windows cihazlarına 3 saniyede bir UDP port 19451 üzerinden doğrudan acil durum sinyali yayını.
  - Sinyal Durum Seçenekleri:
    - 🔴 `SOS / RED ALERT` (`#DC2626`)
    - 🔵 `MEDICAL ASSISTANCE` (`#2563EB`)
    - 🟢 `STATUS OK / SAFE` (`#16A34A`)
  - Medikal not ve konum bilgisi ekleme desteği.

- **🎙️ Enkaz Dinleme & Ses Yükseltici (Rubble Audio Processor)**:
  - **Kaynak & Algoritma:** Ozan Sarıer'in açık kaynak [enkazdinlemeuygulamasi](https://github.com/ozansarier/enkazdinlemeuygulamasi) projesinin ses işleme algoritması temel alınarak entegre edilmiştir.
  - **Canlı Ses Akışı:** 16kHz PCM mikrofon yakalama ve hoparlör aktarımı.
  - **300Hz Yüksek Geçiren Filtre (High-Pass Filter):** 300Hz altı jeneratör ve rüzgar uğultularını keser; insan sesi ve enkaz tıkırtılarına odaklanır.
  - **Dinamik Kazanç Yükseltme:** `3x`, `5x`, `10x (MAX)` amplifikasyon artırımı (Yumuşak Kırpma / Soft Clipping Korumalı).
  - **Canlı Genlik & Tepe Uyarısı:** RMS hesaplamasıyla çalışan canlı ses çubuğu ve %65 üzeri ani ses uyarısı (`⚠️ YÜKSEK SES / TIKIRTI ALGILANDI!`).

- **🔊 Akustik Siren & 🔦 SOS Strobe Flaş**:
  - Arama kurtarma ekiplerinin yön tayinini kolaylaştırmak için **3.5 kHz akustik düdük sesi**.
  - Görsel arama kurtarma sinyali için Strobe Flaş.

- **👥 P2P Mesh Ağ Keşfi**:
  - Merkezi bir sunucuya ihtiyaç duymadan doğrudan cihazlar arası peer discovery.

---

## 🚀 Çalıştırma ve Paketleme

### Geliştirici Modunda Çalıştırma
```bash
.\gradlew.bat run
```

### Windows Çalıştırılabilir Paket (.exe / .msi) Oluşturma
```bash
.\gradlew.bat packageApp
```
Paketler `build/compose/binaries/main/msi/` ve `build/compose/binaries/main/exe/` dizinlerinde oluşturulacaktır.

---

## ⚖️ Yasal Bildirim, Beta Test ve Sorumluluk Reddi (Legal Disclaimer)

> [!CAUTION]
> **⚠️ BETA / TEST AŞAMASI UYARISI:**
> Bu uygulama henüz deneysel (Beta / Test) aşamasında açık kaynaklı bir yazılımdır. Arama-kurtarma ve acil durum senaryolarında ek/yardımcı bir araç olarak tasarlanmış olup, resmi acil durum kanallarının (AFAD, 112 Acil Çağrı vb.) veya profesyonel arama-kurtarma teçhizatlarının yerini asla alamaz.

> [!WARNING]
> **⚖️ YASAL SORUMLULUK REDDİ (DISCLAIMER OF LIABILITY):**
> Yazılım "olduğu gibi" (AS IS) ve "mevcut haliyle" herhangi bir açık veya zımnı garanti verilmeksizin sunulmaktadır. Geliştiriciler, katkıda bulunanlar ve lisansörler; uygulamanın kesintisiz çalışması, sinyal iletim garantisi, enkaz altı ses tespiti doğruluğu, veri kaybı, cihaz hasarı veya kullanım sırasında oluşabilecek doğrudan/dolaylı hiçbir can ve mal kaybından veya zarardan yasal olarak sorumlu tutulamaz. Kullanıcı uygulamayı tüm riskleri kabul ederek kendi sorumluluğunda çalıştırır.

---

## 📄 Lisans
GNU General Public License v3.0 (GPL-3.0)
