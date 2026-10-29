# BriocheLab — Kurulum ve Kullanım

## Telefona kurulum (Android)
1. Telefonda şu sayfayı aç: https://github.com/berkeuuu/briochelab/releases/tag/latest
2. **BriocheLab.apk** dosyasına dokunup indir.
3. İndirilen dosyaya dokun. "Bilinmeyen kaynaklardan kurulum" izni isterse **İzin ver** de.
4. Kur ve uygulamayı aç. İnternet gerekmez.

Uygulamayı güncellemek için yeni APK'yı aynı şekilde üstüne kur; kayıtların silinmez (aynı imza anahtarı kullanılır).

## Yedekleme
- Uygulama içinde **Yedekleme ve Ayarlar** bölümünden **JSON yedek** al ve dosyayı Drive/e-posta/bilgisayara kaydet.
- Fotoğraflar JSON'a dahil değildir; **Fotoğrafları yedekle** ile ayrı ZIP olarak al.
- Geri yüklerken önce yedeğin içeriği gösterilir, onaylamadan hiçbir şey değişmez.
- Uygulamayı silersen veya telefon bozulursa yalnızca telefondaki kayıtlar kaybolur; bu yüzden düzenli yedek al.

## APK nasıl üretilir
Depoya her gönderimde GitHub Actions ("APK derle" iş akışı) APK'yı otomatik derler ve yukarıdaki sürüm sayfasına yükler.
Elle başlatmak için: GitHub → Actions → "APK derle" → Run workflow.
