package com.briochelab.app.data

import com.briochelab.app.calc.CIng
import com.briochelab.app.calc.Calc
import com.briochelab.app.calc.Fmt

/** Bir deneyin karsilastirma / oneri icin ihtiyac duydugu tum verisi. */
data class ExpSnapshot(
    val exp: Experiment,
    val version: RecipeVersion?,
    val ingredients: List<Ingredient>,
    val measurements: Map<String, ProcessMeasurement>,
    val eval: SensoryEvaluation?,
    val variables: List<ExperimentVariable>,
    val checklist: List<ChecklistItem>
) {
    fun num(key: String): Double? = measurements[key]?.numValue
    fun text(key: String): String = measurements[key]?.textValue ?: ""
    fun hasValue(key: String): Boolean {
        val m = measurements[key] ?: return false
        return m.numValue != null || m.textValue.isNotBlank()
    }
}

/** Iki deney arasinda gorulen tek bir fark. */
data class Change(val label: String, val before: String, val after: String)

object Logic {
    fun toCIng(i: Ingredient): CIng =
        CIng(i.name, i.category, i.grams, i.waterPercent, i.price, i.priceUnit, i.unitGrams)

    fun waterDefaults(props: List<IngredientProperty>): Map<String, Double?> {
        val m = LinkedHashMap<String, Double?>(com.briochelab.app.calc.Cat.defaultWater)
        for (p in props) m[p.category] = p.waterPercent
        return m
    }

    private fun measureText(s: ExpSnapshot, f: MField): String {
        val m = s.measurements[f.key] ?: return ""
        return if (f.numeric) {
            if (m.numValue == null) "" else Fmt.num(m.numValue, 2) + (if (f.unit.isNotEmpty()) " " + f.unit else "")
        } else m.textValue
    }

    /** Onceki deneye gore otomatik tespit edilen degisiklikler (recete, malzeme yuzdeleri, olcumler). */
    fun changes(prev: ExpSnapshot?, cur: ExpSnapshot): List<Change> {
        if (prev == null) return emptyList()
        val out = ArrayList<Change>()

        val pv = prev.version
        val cv = cur.version
        if (pv != null && cv != null && pv.id != cv.id) {
            out.add(Change("Reçete sürümü", "v" + pv.versionNo, "v" + cv.versionNo))
        }
        if (pv != null && cv != null) {
            fun txt(label: String, a: String, b: String) {
                if (a.trim() != b.trim()) out.add(Change(label, a.ifBlank { "—" }, b.ifBlank { "—" }))
            }
            txt("Un markası", pv.flourBrand, cv.flourBrand)
            txt("Un türü", pv.flourType, cv.flourType)
            txt("Maya", pv.yeastType + " " + pv.yeastBrand, cv.yeastType + " " + cv.yeastBrand)
            txt("Karıştırma yöntemi", pv.mixMethod, cv.mixMethod)
            txt("Fermantasyon yöntemi", pv.fermentMethod, cv.fermentMethod)
            txt("Pişirme yöntemi", pv.bakeMethod, cv.bakeMethod)
            if (pv.targetPieceG != cv.targetPieceG) {
                out.add(Change("Hedef parça gramajı", Fmt.num(pv.targetPieceG, 1), Fmt.num(cv.targetPieceG, 1)))
            }
        }
        // Malzeme yuzdeleri (kategori bazinda toplam)
        val pf = Calc.flourTotal(prev.ingredients.map { Logic.toCIng(it) })
        val cf = Calc.flourTotal(cur.ingredients.map { Logic.toCIng(it) })
        if (prev.ingredients.isNotEmpty() && cur.ingredients.isNotEmpty() && pf > 0.0 && cf > 0.0) {
            for (cat in com.briochelab.app.calc.Cat.all) {
                if (cat == com.briochelab.app.calc.Cat.FLOUR) continue
                val a = prev.ingredients.filter { it.category == cat }.sumOf { it.grams }
                val b = cur.ingredients.filter { it.category == cat }.sumOf { it.grams }
                val pa = Calc.bakerPercent(a, pf)
                val pb = Calc.bakerPercent(b, cf)
                if (pa != null && pb != null && Math.abs(pa - pb) > 0.05) {
                    out.add(Change(com.briochelab.app.calc.Cat.label(cat) + " (fırıncı %)", Fmt.pct(pa, 1), Fmt.pct(pb, 1)))
                }
            }
        }
        // Surec olcumleri (sonuc olcumleri degisken sayilmaz)
        for (f in Fields.all) {
            if (f.group == Fields.G_RESULT || f.group == Fields.G_ENV) continue
            val a = measureText(prev, f)
            val b = measureText(cur, f)
            if (a.isNotEmpty() && b.isNotEmpty() && a != b) out.add(Change(f.label, a, b))
        }
        return out
    }

    const val MANY_CHANGES = 4

    /** Kullanici tarafindan bildirilen degiskenler ile otomatik bulunanlarin birlesik sayisi. */
    fun changeCount(declared: List<ExperimentVariable>, auto: List<Change>): Int {
        val labels = HashSet<String>()
        for (v in declared) labels.add(v.label.trim().lowercase())
        for (c in auto) labels.add(c.label.trim().lowercase())
        return labels.size
    }

    const val MANY_CHANGES_WARNING =
        "Sonuçtaki farkın hangi değişkenden kaynaklandığını ayırmak zorlaşabilir."
}

/** Yerel kurallarla uretilen, aciklanabilir oneri. */
data class Advice(
    val key: String,
    val title: String,
    val observation: String,
    val explanations: List<String>,
    val support: List<String>,
    val missing: List<String>,
    val nextStep: String,
    val howToMeasure: String
)

object Advisor {
    private val critical = listOf(
        "doughTemp" to "Yoğurma sonu hamur sıcaklığı",
        "bakeMin" to "Pişirme süresi",
        "ovenSetC" to "Fırın ayar sıcaklığı",
        "bakedWeight" to "Pişmiş ekmek ağırlığı"
    )

    fun advise(cur: ExpSnapshot, prev: ExpSnapshot?, changes: List<Change>): List<Advice> {
        val out = ArrayList<Advice>()
        val ev = cur.eval
        val missingCritical = critical.filter { !cur.hasValue(it.first) }.map { it.second }

        // Durum 3: cok degisken
        val cc = Logic.changeCount(cur.variables, changes)
        if (cc >= Logic.MANY_CHANGES) {
            out.add(
                Advice(
                    key = "cok_degisken",
                    title = "Birden fazla değişken aynı anda değişmiş",
                    observation = "Bu denemede $cc farklı parametre değişmiş görünüyor.",
                    explanations = listOf(Logic.MANY_CHANGES_WARNING),
                    support = changes.take(6).map { it.label + ": " + it.before + " → " + it.after } +
                        cur.variables.map { it.label },
                    missing = emptyList(),
                    nextStep = "Sonraki denemede yalnızca en şüpheli 1–2 değişkeni değiştirip diğerlerini sabit tutmayı düşünebilirsiniz. Bilinçli olarak çok değişken denemek de mümkündür.",
                    howToMeasure = "Aynı puan ve ölçüm alanlarını (yükseklik, yumuşaklık, ertesi gün) iki denemede karşılaştırın."
                )
            )
        }

        // Durum 4: hamur sicakligi kaydedilmedi
        if (!cur.hasValue("doughTemp")) {
            out.add(
                Advice(
                    key = "hamur_sicakligi_yok",
                    title = "Hamur sıcaklığı kaydedilmemiş",
                    observation = "Yoğurma sonu hamur sıcaklığı bu denemede boş.",
                    explanations = listOf("Hamur sıcaklığı fermantasyon hızını ve tereyağlı hamurun toparlanmasını etkileyebilir; kayıt yoksa farkın nedeni değerlendirilemez."),
                    support = emptyList(),
                    missing = listOf("Yoğurma sonu hamur sıcaklığı"),
                    nextStep = "Bir sonraki denemede yoğurma biter bitmez hamurun ortasından sıcaklık ölçüp kaydedin.",
                    howToMeasure = "Prob termometreyle hamurun ortasından, aynı noktada ve aynı anda ölçün."
                )
            )
        }

        if (ev != null) {
            // Durum 1: hamur basik / yukseklik dusuk
            val h = ev.height
            val r = ev.roundness
            if ((h != null && h <= 4) || (r != null && r <= 4)) {
                val sup = ArrayList<String>()
                if (h != null) sup.add("Yükseklik puanı: $h/10")
                if (r != null) sup.add("Yuvarlak form puanı: $r/10")
                cur.num("heightCm")?.let { sup.add("Ölçülen yükseklik: " + Fmt.num(it, 1) + " cm") }
                val miss = ArrayList<String>()
                if (!cur.hasValue("doughTemp")) miss.add("Hamur sıcaklığı")
                if (!cur.hasValue("proofMin")) miss.add("Son mayalanma süresi")
                if (!cur.hasValue("roundMethod")) miss.add("Yuvarlama yöntemi")
                out.add(
                    Advice(
                        key = "basik",
                        title = "Ekmek basık / form zayıf kalmış olabilir",
                        observation = "Yükseklik veya form puanı düşük.",
                        explanations = listOf(
                            "Gluten gelişimi yetersiz olabilir.",
                            "Şekillendirme sırasında yüzey gerilimi yetersiz kalmış olabilir.",
                            "Son mayalanma kısa/uzun kalmış olabilir.",
                            "Hamur sıcaklığı hedefin dışında olabilir."
                        ),
                        support = sup,
                        missing = miss,
                        nextStep = "Bu dört başlığı (gluten gelişimi, şekillendirme, son mayalanma, hamur sıcaklığı) kayıtlarınızla gözden geçirin. Tek bir nedeni kesin kabul etmeyin; bir seferde birini değiştirerek deneyin.",
                        howToMeasure = "Pişmiş ekmek yüksekliğini ve çapını aynı yerden cetvelle ölçüp yükseklik/çap oranını önceki denemeyle karşılaştırın."
                    )
                )
            }

            // Durum 2: ikinci gun sertlesti
            val d1 = ev.day1Soft
            val d2 = ev.nextDaySoft
            if ((d2 != null && d2 <= 4) || (d1 != null && d2 != null && d1 - d2 >= 3)) {
                val sup = ArrayList<String>()
                if (d1 != null) sup.add("İlk gün yumuşaklık: $d1/10")
                if (d2 != null) sup.add("Ertesi gün yumuşaklık: $d2/10")
                val wb = cur.num("weightBefore")
                val wa = cur.num("weightAfter")
                if (wb != null && wa != null && wb > 0.0) {
                    sup.add("Pişirme kaybı: " + Fmt.pct((wb - wa) / wb * 100.0, 1))
                }
                val miss = ArrayList<String>()
                if (wb == null || wa == null) miss.add("Pişirme öncesi/sonrası ağırlık")
                if (!cur.hasValue("coolMin")) miss.add("Soğutma süresi")
                if (!cur.hasValue("packaging")) miss.add("Ambalaj türü")
                out.add(
                    Advice(
                        key = "ikinci_gun_sert",
                        title = "Ertesi gün yumuşaklık düşmüş",
                        observation = "Ertesi gün yumuşaklık puanı düşük veya ilk güne göre belirgin biçimde azalmış.",
                        explanations = listOf(
                            "Pişirme kaybı yüksek olmuş olabilir (fazla pişme).",
                            "Reçete bileşimi (yağ, şeker, yumurta, hidrasyon) etkili olabilir.",
                            "Soğutma ve ambalajlama koşulları nem kaybını artırmış olabilir."
                        ),
                        support = sup,
                        missing = miss,
                        nextStep = "İlk gün ve ertesi gün sonuçlarını, pişirme kaybını, reçete bileşimini ve soğutma/ambalaj koşullarını önceki denemelerle yan yana karşılaştırın.",
                        howToMeasure = "Aynı saatte, aynı basınç ve aynı dilim kalınlığıyla yumuşaklığı puanlayın; ambalaj türünü sabit tutun."
                    )
                )
            }

            // Durum 6: celiskili sonuclar
            val pe = prev?.eval
            if (pe != null) {
                val ups = ArrayList<String>()
                val downs = ArrayList<String>()
                for (c in Criteria.all) {
                    val a = pe.score(c.key)
                    val b = ev.score(c.key)
                    if (a != null && b != null) {
                        if (b - a >= 2) ups.add(c.label + " " + a + " → " + b)
                        if (a - b >= 2) downs.add(c.label + " " + a + " → " + b)
                    }
                }
                if (ups.isNotEmpty() && downs.isNotEmpty()) {
                    out.add(
                        Advice(
                            key = "celiski",
                            title = "Sonuçlar çelişkili: bazıları iyileşmiş, bazıları gerilemiş",
                            observation = "Önceki denemeye göre bazı ölçütler artmış, bazıları azalmış.",
                            explanations = listOf("Tek bir genel puan bu farkı gizleyebilir; iki yönü ayrı değerlendirin."),
                            support = listOf("İyileşen: " + ups.joinToString("; "), "Gerileyen: " + downs.joinToString("; ")),
                            missing = emptyList(),
                            nextStep = "Gerileyen ölçütü hedefleyen küçük bir deneme planlayın ve iyileşen kısmı koruyan temel formülü değiştirmeyin.",
                            howToMeasure = "Her iki ölçütü de aynı yöntemle ölçüp sonraki denemede yine ayrı ayrı karşılaştırın."
                        )
                    )
                }
            }

            // Durum 5: basarili
            val o = ev.overall
            if ((o != null && o >= 8) || ev.goalMet == 1) {
                out.add(
                    Advice(
                        key = "basarili",
                        title = "Deney başarılı görünüyor",
                        observation = "Genel kalite yüksek veya başarı ölçütüne ulaşıldığı işaretlenmiş.",
                        explanations = listOf("Tek denemedeki başarı, tekrarlanabilirliği göstermez."),
                        support = listOfNotNull(o?.let { "Genel kalite: $it/10" }, if (ev.goalMet == 1) "Başarı ölçütüne ulaşıldı" else null),
                        missing = missingCritical,
                        nextStep = "Reçeteyi doğrudan değiştirmek yerine bu denemeyi yeni bir reçete sürümü olarak kaydedin ve aynı koşullarla kontrollü bir tekrar denemesi yapın.",
                        howToMeasure = "Tekrar denemesinde aynı puanların ve ölçümlerin benzer çıkıp çıkmadığına bakın."
                    )
                )
            }
        }

        // Durum 7: eksik kritik kayitlar
        if (missingCritical.isNotEmpty()) {
            out.add(
                Advice(
                    key = "eksik_kayit",
                    title = "Eksik kritik kayıtlar var",
                    observation = "Bazı önemli ölçümler boş bırakılmış.",
                    explanations = listOf("Eksik kayıt, sonuçların nedenini yorumlamayı zorlaştırır. Deney geçersiz sayılmaz."),
                    support = emptyList(),
                    missing = missingCritical,
                    nextStep = "Hatırlıyorsanız bu değerleri tamamlayın; hatırlamıyorsanız boş bırakın (tahmin yazmak yerine).",
                    howToMeasure = "Bir sonraki denemede bu değerleri ilgili aşamada hemen kaydedin."
                )
            )
        }

        // Yetersiz veri
        val scored = if (ev == null) 0 else Criteria.all.count { ev.score(it.key) != null }
        val measuredCount = cur.measurements.values.count { it.numValue != null || it.textValue.isNotBlank() }
        if (scored < 3 || measuredCount < 5) {
            out.add(
                Advice(
                    key = "yetersiz_veri",
                    title = "Bu denemeden henüz kesin sonuç çıkarılamaz",
                    observation = "Puan ve ölçüm sayısı az (puan: $scored, ölçüm: $measuredCount).",
                    explanations = listOf("Yorum yapabilmek için en az birkaç puan ve süreç ölçümü gerekir."),
                    support = emptyList(),
                    missing = emptyList(),
                    nextStep = "Değerlendirme ekranından en azından yumuşaklık, yükseklik, form ve ertesi gün puanlarını girin.",
                    howToMeasure = "Puanlamayı her denemede aynı kriterlerle yapın."
                )
            )
        }
        return out
    }

    /** Iki deney karsilastirilirken temkinli yorum. */
    fun cautiousComparison(a: ExpSnapshot, b: ExpSnapshot, changeCount: Int): String {
        val ea = a.eval
        val eb = b.eval
        if (ea == null || eb == null) return "Karşılaştırma için iki denemenin de değerlendirmesi gerekir."
        val better = ArrayList<String>()
        for (c in Criteria.all) {
            val x = ea.score(c.key)
            val y = eb.score(c.key)
            if (x != null && y != null && y > x) better.add(c.label)
        }
        val base = if (better.isEmpty()) "İkinci denemede daha yüksek puanlı bir ölçüt gözlenmedi."
        else "İkinci denemede şu ölçütlerde daha yüksek puan gözlendi: " + better.joinToString(", ") + "."
        return base + " Bu, farkın belirli bir değişkenden kaynaklandığını kanıtlamaz; diğer koşulların etkisi ayrıca değerlendirilmelidir." +
            (if (changeCount >= Logic.MANY_CHANGES) " " + Logic.MANY_CHANGES_WARNING else "")
    }
}
