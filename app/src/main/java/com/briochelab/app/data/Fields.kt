package com.briochelab.app.data

/** Bir deneyde kaydedilebilen olcum / kosul alani. */
data class MField(
    val key: String,
    val label: String,
    val unit: String,
    val group: String,
    val numeric: Boolean = true
)

object Fields {
    const val G_ENV = "Ortam"
    const val G_MIX = "Karıştırma ve yoğurma"
    const val G_BULK = "İlk fermantasyon"
    const val G_SHAPE = "Bölme ve şekillendirme"
    const val G_PROOF = "Son mayalanma"
    const val G_BAKE = "Pişirme"
    const val G_COOL = "Soğutma ve ambalaj"
    const val G_RESULT = "Sonuç ölçümleri"

    val groups = listOf(G_ENV, G_MIX, G_BULK, G_SHAPE, G_PROOF, G_BAKE, G_COOL, G_RESULT)

    val all: List<MField> = listOf(
        MField("ambientTemp", "Ortam sıcaklığı", "°C", G_ENV),
        MField("humidity", "Ortam nemi", "%", G_ENV),

        MField("mixer", "Mikser", "", G_MIX, false),
        MField("mixSetting", "Karıştırma ayarı", "", G_MIX, false),
        MField("kneadMin", "Yoğurma süresi", "dk", G_MIX),
        MField("doughTemp", "Yoğurma sonu hamur sıcaklığı", "°C", G_MIX),
        MField("butterStage", "Tereyağı ekleme aşaması", "", G_MIX, false),
        MField("tangzhongPct", "Tangzhong oranı (una göre)", "%", G_MIX),
        MField("prefermentNote", "Ön hamur kullanımı", "", G_MIX, false),

        MField("bulkMin", "İlk fermantasyon süresi", "dk", G_BULK),
        MField("bulkRisePct", "Fermantasyon sonu hacim artışı", "%", G_BULK),
        MField("bulkNote", "Gözlenen hacim / görünüm", "", G_BULK, false),
        MField("coldNote", "Soğuk mayalandırma koşulları", "", G_BULK, false),

        MField("pieceG", "Gerçek parça gramajı (ortalama)", "g", G_SHAPE),
        MField("roundMethod", "Yuvarlama yöntemi", "", G_SHAPE, false),
        MField("trayLayout", "Tepsi / kalıp düzeni", "", G_SHAPE, false),

        MField("proofMin", "Son mayalanma süresi", "dk", G_PROOF),
        MField("proofTemp", "Son mayalanma ortam sıcaklığı", "°C", G_PROOF),
        MField("proofNote", "Hacim ve yüzey görünümü", "", G_PROOF, false),

        MField("ovenType", "Fırın tipi", "", G_BAKE, false),
        MField("ovenSetC", "Fırın ayar sıcaklığı", "°C", G_BAKE),
        MField("ovenMeasC", "Ölçülen gerçek fırın sıcaklığı", "°C", G_BAKE),
        MField("bakeMin", "Pişirme süresi", "dk", G_BAKE),
        MField("weightBefore", "Pişirme öncesi ağırlık (parça)", "g", G_BAKE),
        MField("weightAfter", "Pişirme sonrası ağırlık (parça)", "g", G_BAKE),

        MField("coolMin", "Soğutma süresi", "dk", G_COOL),
        MField("packaging", "Ambalaj türü", "", G_COOL, false),
        MField("storage", "Saklama koşulları", "", G_COOL, false),

        MField("producedCount", "Gerçek üretim adedi", "adet", G_RESULT),
        MField("lossActual", "Gerçek fire", "%", G_RESULT),
        MField("bakedWeight", "Pişmiş ekmek ağırlığı", "g", G_RESULT),
        MField("diameterCm", "Çap", "cm", G_RESULT),
        MField("heightCm", "Yükseklik", "cm", G_RESULT),
        MField("volumeMl", "Hacim (ölçülebiliyorsa)", "ml", G_RESULT)
    )

    private val byKey: Map<String, MField> = all.associateBy { it.key }

    fun get(key: String): MField? = byKey[key]

    fun label(key: String): String = byKey[key]?.label ?: key
}

/** Degerlendirme olcutleri (1-10). */
data class Criterion(val key: String, val label: String)

object Criteria {
    val all: List<Criterion> = listOf(
        Criterion("softness", "Yumuşaklık"),
        Criterion("crumb", "Lifli iç doku"),
        Criterion("aroma", "Aroma"),
        Criterion("volume", "Hacim"),
        Criterion("height", "Yükseklik"),
        Criterion("roundness", "Yuvarlak form"),
        Criterion("color", "Yüzey rengi"),
        Criterion("slicing", "Dilimleme kalitesi"),
        Criterion("sauce", "Sos dayanımı"),
        Criterion("day1Soft", "İlk gün yumuşaklık"),
        Criterion("nextDaySoft", "Ertesi gün yumuşaklık"),
        Criterion("overall", "Genel kalite")
    )

    fun label(key: String): String = all.firstOrNull { it.key == key }?.label ?: key
}

fun SensoryEvaluation.score(key: String): Int? = when (key) {
    "softness" -> softness
    "crumb" -> crumb
    "aroma" -> aroma
    "volume" -> volume
    "height" -> height
    "roundness" -> roundness
    "color" -> color
    "slicing" -> slicing
    "sauce" -> sauce
    "day1Soft" -> day1Soft
    "nextDaySoft" -> nextDaySoft
    "overall" -> overall
    else -> null
}

fun SensoryEvaluation.withScore(key: String, v: Int?): SensoryEvaluation = when (key) {
    "softness" -> copy(softness = v)
    "crumb" -> copy(crumb = v)
    "aroma" -> copy(aroma = v)
    "volume" -> copy(volume = v)
    "height" -> copy(height = v)
    "roundness" -> copy(roundness = v)
    "color" -> copy(color = v)
    "slicing" -> copy(slicing = v)
    "sauce" -> copy(sauce = v)
    "day1Soft" -> copy(day1Soft = v)
    "nextDaySoft" -> copy(nextDaySoft = v)
    "overall" -> copy(overall = v)
    else -> this
}

object Stages {
    val names: List<String> = listOf(
        "Genel",
        "Deneme öncesi",
        "Karıştırma ve yoğurma",
        "İlk fermantasyon",
        "Bölme ve şekillendirme",
        "Son mayalanma",
        "Pişirme",
        "Soğutma ve ambalajlama",
        "Ertesi gün ve sonraki değerlendirmeler"
    )

    /** stage numarasi: 0 genel, 1..8 asamalar. */
    fun name(stage: Int): String = names.getOrElse(stage) { "Genel" }

    val photoSlots: List<String> = listOf(
        "Yoğurma sonrası hamur",
        "İlk fermantasyon sonrası",
        "Şekillendirilmiş hamur",
        "Fırın öncesi",
        "Fırından çıkış",
        "Kesilmiş ekmeğin iç dokusu",
        "Ertesi gün ekmek",
        "Diğer"
    )

    val checklist: Map<Int, List<String>> = mapOf(
        1 to listOf(
            "Deney amacı ve hipotezi belirlendi.",
            "Kullanılacak reçete ve sürümü seçildi.",
            "Malzemeler tartıldı.",
            "Un markası ve özellikleri kaydedildi.",
            "Maya türü ve miktarı kontrol edildi.",
            "Terazi ve termometre hazırlandı.",
            "Deneyde değiştirilecek parametreler belirlendi.",
            "Gerekli tepsi, kalıp ve ekipman hazırlandı."
        ),
        2 to listOf(
            "Malzemeler planlanan sırayla eklendi.",
            "Karıştırma ve yoğurma süreleri kaydedildi.",
            "Tereyağı ekleme aşaması kaydedildi.",
            "Hamurun gluten gelişimi değerlendirildi.",
            "Hamurun sıcaklığı ölçüldü.",
            "Hamurun görünümü ve yapışkanlığı kaydedildi."
        ),
        3 to listOf(
            "Başlangıç saati ve hamur sıcaklığı kaydedildi.",
            "Fermantasyon süresi kaydedildi.",
            "Fermantasyon sonundaki hamur hacmi veya hacim değişimi kaydedildi.",
            "Hamurun görünümü ve elastikiyeti değerlendirildi."
        ),
        4 to listOf(
            "Her parçanın hedef gramajı belirlendi.",
            "Gerçek parça gramajları kontrol edildi.",
            "Yuvarlama yöntemi kaydedildi.",
            "Hamur yüzey gerilimi ve şekli değerlendirildi.",
            "Tepsi veya kalıp düzeni kaydedildi."
        ),
        5 to listOf(
            "Başlangıç saati kaydedildi.",
            "Ortam sıcaklığı kaydedildi.",
            "Son mayalanma süresi kaydedildi.",
            "Hacim ve yüzey görünümü değerlendirildi.",
            "Fırına verme kararı ve gözlenen hamur durumu kaydedildi."
        ),
        6 to listOf(
            "Fırın önceden ısıtıldı.",
            "Fırın sıcaklığı ve pişirme süresi kaydedildi.",
            "Üst renk ve yüzey görünümü değerlendirildi.",
            "Pişmiş ekmek gramajı ölçüldü.",
            "Yükseklik ve çap ölçüldü.",
            "Gerekliyse iç pişme değerlendirmesi kaydedildi."
        ),
        7 to listOf(
            "Soğutma süresi kaydedildi.",
            "Ambalaj öncesi ekmek durumu kontrol edildi.",
            "Ambalaj türü kaydedildi.",
            "Saklama koşulları kaydedildi.",
            "İlk gün değerlendirmesi yapıldı."
        ),
        8 to listOf(
            "Ertesi gün yumuşaklık değerlendirildi.",
            "İç doku ve liflenme değerlendirildi.",
            "Aroma değerlendirildi.",
            "Dilimleme kalitesi değerlendirildi.",
            "Burger performansı değerlendirildi.",
            "Şekil ve yükseklik kontrol edildi.",
            "Sonuçlar fotoğraflandı.",
            "Deneyin başarılı olup olmadığı değerlendirildi.",
            "Sonraki deneme için karar yazıldı."
        )
    )

    fun statusLabel(s: Int): String = when (s) {
        1 -> "Tamamlandı"
        2 -> "Atlandı"
        3 -> "İlgili değil"
        else -> "Bekliyor"
    }
}
