package com.briochelab.app.data

data class InfoTopic(
    val title: String,
    val what: String,
    val why: String,
    val effect: String,
    val signs: String,
    val mistakes: String
)

object InfoContent {
    const val DISCLAIMER =
        "Buradaki açıklamalar genel bilgidir ve kesin reçete kuralı değildir. Uygun değerler un, reçete, ekipman ve ortam koşullarına göre değişir; kendi denemelerinizle doğrulayın."

    val topics: List<InfoTopic> = listOf(
        InfoTopic(
            "Hidrasyon",
            "Hamurdaki su miktarının un miktarına oranıdır (fırıncı yüzdesiyle ifade edilir).",
            "Hamurun kıvamını, yapışkanlığını, işlenebilirliğini ve iç dokuyu etkiler.",
            "Brioche'te yumurta, süt ve tereyağı da hamura sıvı/yağ katar; bu yüzden yalnızca eklenen suya bakmak yanıltıcı olabilir. Uygulamada iki ayrı değer gösterilir: eklenen serbest su oranı ve tahmini toplam su oranı.",
            "Hamur çok yapışkan ve toparlanmıyorsa ya da çok sıkı ve kuru kalıyorsa hidrasyonu gözden geçirin.",
            "Tahmini toplam su oranını laboratuvar ölçümü sanmak. Yağ, yumurta ve süt katıları hamurun davranışını yalnızca su oranıyla açıklanamayacak şekilde etkiler."
        ),
        InfoTopic(
            "Fırıncı yüzdesi",
            "Her malzemenin gramajının toplam un gramajına bölünüp 100 ile çarpılmasıyla bulunan orandır. Un her zaman %100'dür.",
            "Reçeteleri farklı miktarlarda büyütüp küçültmeyi ve farklı reçeteleri adil biçimde karşılaştırmayı sağlar.",
            "Örneğin 500 g un ve 100 g tereyağı varsa tereyağı %20'dir. Un miktarı değişince diğer malzemeler aynı oranı korur.",
            "Yüzdeleri iki reçete arasında yan yana koyduğunuzda hangi malzemenin değiştiği net görünür.",
            "Birden fazla un kullanırken yüzdeyi tek bir una göre hesaplamak; toplam un ağırlığı esas alınmalıdır."
        ),
        InfoTopic(
            "Gluten gelişimi",
            "Un proteinlerinin su ve yoğurma ile birleşip elastik bir ağ oluşturmasıdır.",
            "Hamurun gazı tutması, hacim ve iç doku bu ağın kalitesine bağlıdır.",
            "Brioche'te yağ ve şeker miktarı yüksek olduğu için gluten ağı daha zor gelişir; genellikle yağ, glutenin bir kısmı geliştikten sonra eklenir.",
            "Hamurun pürüzsüzleşmesi, kaseden ayrılması ve ince bir tabaka halinde gerilebilmesi gelişimi gösterir.",
            "Gelişim tamamlanmadan yağı eklemek veya hamuru aşırı ısıtacak kadar uzun yoğurmak."
        ),
        InfoTopic(
            "Pencere testi",
            "Küçük bir hamur parçasını parmaklar arasında yırtılmadan ince bir zar gibi gerip ışığa tutma kontrolüdür.",
            "Glutenin ne kadar geliştiğini ekipman gerektirmeden görmenizi sağlar.",
            "Zar ince ve dayanıklıysa gluten yeterince gelişmiş olabilir; kısa sürede yırtılıyorsa yoğurmaya devam etmek gerekebilir.",
            "Zarın inceliği, yırtılma biçimi ve hamurun toparlanması.",
            "Testi tek başına kesin ölçüt saymak; hamur sıcaklığı ve görünümüyle birlikte değerlendirin."
        ),
        InfoTopic(
            "Un protein oranı ve gluten kalitesi",
            "Unun protein yüzdesi, oluşabilecek gluten miktarının bir göstergesidir. Gluten kalitesi ise protein miktarından ayrıca dayanıklılık ve esneklik ile ilgilidir.",
            "Brioche'in hacmini, dokusunu ve yağ/şekeri taşıma kapasitesini etkiler.",
            "Aynı protein oranına sahip iki un farklı davranabilir. Un markası ve özellikleri bu yüzden her reçetede kaydedilir.",
            "Aynı reçetede un markası değişince hamurun su alma ve toparlanma davranışına bakın.",
            "Sadece etiketteki protein oranına bakıp un değiştirmek; her değişiklik ayrı bir deney değişkenidir."
        ),
        InfoTopic(
            "Hamur sıcaklığı",
            "Yoğurma bittiğinde hamurun iç sıcaklığıdır.",
            "Maya aktivitesini, fermantasyon hızını ve tereyağının hamurdaki davranışını etkiler.",
            "Çok sıcak hamurda yağ erimeye başlayıp hamurun toparlanmasını zorlaştırabilir; çok soğuk hamurda fermantasyon yavaşlar. Hedef aralık ekipmana ve reçeteye göre değişir.",
            "Yoğurma sonu ölçülen sıcaklık ve hamurun yağlı/yapışkan görünümü.",
            "Sıcaklığı ölçmeden tahmin etmek; ortam ve un sıcaklığı da sonucu değiştirir."
        ),
        InfoTopic(
            "İlk fermantasyon",
            "Yoğurmadan sonra hamurun bölünmeden önce dinlendiği ve mayanın çalıştığı aşamadır.",
            "Aroma gelişimi, hamurun gevşemesi ve gaz oluşumu bu aşamada olur.",
            "Süre; maya miktarı, hamur ve ortam sıcaklığına göre değişir. Yağlı hamurlarda fermantasyon daha yavaş olabilir.",
            "Hamurun hacmindeki artış, elastikiyeti ve yüzeyindeki gaz kabarcıkları.",
            "Yalnızca süreye bakıp hamurun görünümünü kaydetmemek."
        ),
        InfoTopic(
            "Son mayalanma",
            "Hamur parçaları şekillendirildikten sonra fırına girmeden önceki son dinlenmedir.",
            "Ekmeğin son hacmini ve fırındaki kabarmasını belirler.",
            "Yetersiz mayalanmada ekmek sıkı ve düşük kalabilir; aşırı mayalanmada fırında çökebilir veya formunu kaybedebilir.",
            "Hamura hafifçe dokunduğunuzda geri dönüş hızı, hacim artışı ve yüzey görünümü.",
            "Aynı süreyi her ortamda uygulamak; ortam sıcaklığını ve gözlemi kaydedin."
        ),
        InfoTopic(
            "Soğuk mayalandırma",
            "Hamurun buzdolabı sıcaklığında uzun süre bekletilmesidir.",
            "Hamurun işlenmesini kolaylaştırabilir, üretim planlamasında esneklik sağlar ve aroma gelişimine katkı verebilir.",
            "Tereyağı soğukta sertleştiği için şekillendirme kolaylaşabilir. Süre ve sıcaklık hamur ve reçeteye göre denenmelidir.",
            "Soğuktan çıkan hamurun sıcaklığı, hacmi ve son mayalanma süresi.",
            "Soğuk hamuru doğrudan fırına vermek; son mayalanma ihtiyacı değişir."
        ),
        InfoTopic(
            "Tangzhong",
            "Unun bir kısmının su veya sütle ısıtılıp jelatinleştirilmesiyle hazırlanan ön karışımdır.",
            "Hamurun daha fazla su tutmasına ve ekmeğin daha yumuşak kalmasına yardımcı olabileceği düşünülür.",
            "Bu bir hipotezdir: kendi reçetenizde ve prosesinizde ikinci gün yumuşaklığını artırıp artırmadığı deneyle sınanmalıdır.",
            "Ertesi gün yumuşaklık puanı, yükseklik ve hamurun yapışkanlığı.",
            "Tangzhong kullanırken toplam su ve un miktarını da hesaba katmamak; fırıncı yüzdesini güncelleyin."
        ),
        InfoTopic(
            "Ön hamur",
            "Ana hamurdan önce hazırlanan, bir miktar un, su ve mayadan oluşan fermente karışımdır (poolish, biga vb.).",
            "Aroma ve hamur yapısına katkı sağlayabilir.",
            "Ön hamurdaki un ve su toplam reçeteye dahil edilmelidir; aksi halde yüzdeler yanıltıcı olur.",
            "Ön hamurun olgunluğu, kokusu ve hacmi.",
            "Ön hamur miktarını reçetede kaydetmemek."
        ),
        InfoTopic(
            "Şeker, tuz, yumurta ve tereyağının etkileri",
            "Brioche'i sade ekmekten ayıran zengin malzemelerdir.",
            "Aroma, renk, yumuşaklık, raf ömrü ve maya aktivitesi üzerinde etkileri vardır.",
            "Şeker rengi ve yumuşaklığı artırabilir ama yüksek oranda mayayı yavaşlatabilir. Tuz aroma ve fermantasyon kontrolü sağlar. Yumurta yapı, renk ve aroma katar. Tereyağı aroma ve yumuşaklık verir; fazlası hamuru zorlaştırır.",
            "Hamurun toparlanması, mayalanma hızı ve pişmiş ekmeğin rengi ve dokusu.",
            "Birden fazla zengin malzemeyi aynı anda değiştirmek; sonucun nedenini ayırmak zorlaşır."
        ),
        InfoTopic(
            "Yoğurma sırası",
            "Malzemelerin hamura hangi sırayla ve ne zaman eklendiğidir.",
            "Özellikle yağın eklenme zamanı gluten gelişimini ve hamur sıcaklığını etkiler.",
            "Genellikle un, sıvılar, şeker ve maya birleştirilip gluten bir miktar geliştirilir, ardından yağ parça parça eklenir.",
            "Yağ eklendikten sonra hamurun ne kadar sürede toparlandığı.",
            "Yoğurma sırasını ve sürelerini kaydetmemek; deneyler karşılaştırılamaz hale gelir."
        ),
        InfoTopic(
            "Hamur gramajı ve yuvarlama",
            "Hamurun eşit parçalara bölünmesi ve yuvarlanmasıdır.",
            "Standart gramaj, tutarlı pişme ve ürün standardı için gereklidir; yuvarlama form ve hacmi etkiler.",
            "Yüzey gerilimi iyi olan yuvarlak parçalar daha düzgün kabarır.",
            "Parça gramajlarındaki sapma ve yüzeyin pürüzsüzlüğü.",
            "Hamuru gramajsız gözle bölmek; hedef ve gerçek gramajı kaydedin."
        ),
        InfoTopic(
            "Fırın sıcaklığı ve pişirme kaybı",
            "Fırının ayar sıcaklığı ile gerçek sıcaklığı farklı olabilir; pişerek kaybedilen su ekmeğin ağırlığını düşürür.",
            "Renk, kabuk, iç pişme ve ertesi gün yumuşaklık üzerinde etkilidir.",
            "Fazla pişirme nem kaybını artırarak ekmeğin daha çabuk kurumasına yol açabilir. Pişirme kaybı = (pişirme öncesi − sonrası) / öncesi.",
            "Üst/alt renk, iç sıcaklık ve pişmiş ağırlık.",
            "Fırın ayarına güvenip gerçek sıcaklığı ölçmemek."
        ),
        InfoTopic(
            "Soğutma ve ambalajlama",
            "Pişmiş ekmeğin ambalajlanmadan önce soğutulması ve saklanmasıdır.",
            "Nem dengesini, kabuk yapısını ve raf ömrünü etkiler.",
            "Sıcakken ambalajlamak nemin ambalajda yoğuşmasına neden olabilir; çok geç ambalajlamak kurumaya yol açabilir.",
            "Ambalaj içinde yoğuşma, kabuk yumuşaması ve kuruma.",
            "Soğuma süresini ve ambalaj türünü kaydetmemek."
        ),
        InfoTopic(
            "Ertesi gün yumuşaklığı",
            "Ekmeğin pişirmeden bir gün sonra yumuşaklığını koruma düzeyidir.",
            "Taze günlük üretimle satılan ürünlerde müşterinin gerçek deneyimini yansıtır.",
            "Reçete bileşimi, pişirme kaybı, soğutma ve ambalaj birlikte etkilidir.",
            "Aynı koşulda bekletilen örneklerde ilk gün ve ertesi gün puanlarının farkı.",
            "Değerlendirmeyi her seferinde farklı yöntemle yapmak."
        ),
        InfoTopic(
            "Raf ömrü ve gıda güvenliği",
            "Ürünün güvenli ve kabul edilebilir kalite ile tüketilebileceği süredir.",
            "Yumurta, süt ve tereyağı içeren ürünler hijyen ve saklama koşullarına duyarlıdır.",
            "Ticari satış için işletme kaydı, hijyen, etiketleme ve raf ömrü gereklilikleri vardır. Bu uygulama bu konuda hukuki veya mevzuata dair güvence vermez.",
            "Küf, ekşi koku, kıvam değişimi; bunlar görülürse ürünü tüketmeyin.",
            "Raf ömrünü yalnızca denemeye dayanarak belirlemek; ticari üretimde yetkili kurumlara ve uzmana danışın."
        ),
        InfoTopic(
            "Kontrollü deney tasarımı",
            "Bir seferde mümkün olduğunca az değişken değiştirerek sonuçtaki farkın nedenini ayırt etme yöntemidir.",
            "Aynı anda çok şey değiştirirseniz hangi değişikliğin sonucu etkilediğini bilemezsiniz.",
            "Temel formülü koruyup tek bir hipotezi sınayan küçük denemeler daha güvenilir sonuç verir. Tek deneme kesin kanıt değildir; tekrar gerekir.",
            "Deneyler arasında değişen parametrelerin listesi ve tekrar edilen sonuçlar.",
            "Birden çok değişkeni değiştirip sonucu tek bir değişkene bağlamak."
        )
    )
}
