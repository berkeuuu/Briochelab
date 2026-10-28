package com.briochelab.app.calc

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Malzeme kategorileri. Kategori, varsayilan su orani ve "serbest su" hesabi icin kullanilir. */
object Cat {
    const val FLOUR = "UN"
    const val WATER = "SU"
    const val MILK = "SUT"
    const val EGG = "YUMURTA"
    const val BUTTER = "TEREYAGI"
    const val SUGAR = "SEKER"
    const val SALT = "TUZ"
    const val YEAST = "MAYA"
    const val OTHER = "DIGER"

    val all: List<String> = listOf(FLOUR, WATER, MILK, EGG, BUTTER, SUGAR, SALT, YEAST, OTHER)

    fun label(c: String): String = when (c) {
        FLOUR -> "Un"
        WATER -> "Su"
        MILK -> "Süt"
        EGG -> "Yumurta"
        BUTTER -> "Tereyağı"
        SUGAR -> "Şeker"
        SALT -> "Tuz"
        YEAST -> "Maya"
        else -> "Diğer"
    }

    /** Baslangic varsayimlari (gercek analiz sonucu degildir, Ayarlar'dan degistirilebilir). null = bilinmiyor. */
    val defaultWater: Map<String, Double?> = mapOf(
        FLOUR to 0.0,
        WATER to 100.0,
        MILK to 87.0,
        EGG to 75.0,
        BUTTER to 16.0,
        SUGAR to 0.0,
        SALT to 0.0,
        YEAST to 0.0,
        OTHER to null
    )
}

/** Hesaplama motorunun girdisi: veritabanindan bagimsiz, test edilebilir malzeme satiri. */
data class CIng(
    val name: String,
    val category: String,
    val grams: Double,
    val waterPercent: Double? = null,
    val price: Double? = null,
    val priceUnit: String = "KG",
    val unitGrams: Double? = null
)

data class WaterEstimate(
    val pct: Double?,
    val waterG: Double,
    val complete: Boolean,
    val missing: List<String>
)

data class PieceResult(val value: Double?, val lossAssumedZero: Boolean)

data class ExtraCosts(
    val packagingPerPiece: Double? = null,
    val energyTotal: Double? = null,
    val laborTotal: Double? = null,
    val otherTotal: Double? = null,
    val salePrice: Double? = null
)

data class CostResult(
    val materialTotal: Double?,
    val materialPartial: Double,
    val missingPrices: List<String>,
    val perKgDough: Double?,
    val perPieceMaterial: Double?,
    val perPieceTotal: Double?,
    val missingExpenses: List<String>,
    val contribution: Double?,
    val marginPct: Double?
)

object Calc {
    private val MC = MathContext.DECIMAL64

    private fun bd(d: Double): BigDecimal = BigDecimal.valueOf(d)

    fun valid(g: Double?): Boolean = g != null && !g.isNaN() && !g.isInfinite() && g >= 0.0

    fun round(x: Double, scale: Int): Double =
        bd(x).setScale(scale, RoundingMode.HALF_UP).toDouble()

    fun flourTotal(ings: List<CIng>): Double {
        var s = BigDecimal.ZERO
        for (i in ings) if (i.category == Cat.FLOUR && valid(i.grams)) s = s.add(bd(i.grams))
        return s.toDouble()
    }

    fun totalWeight(ings: List<CIng>): Double {
        var s = BigDecimal.ZERO
        for (i in ings) if (valid(i.grams)) s = s.add(bd(i.grams))
        return s.toDouble()
    }

    /** Firinci yuzdesi = malzeme / toplam un x 100. Un sifirsa veya girdi gecersizse null. */
    fun bakerPercent(grams: Double, flour: Double): Double? {
        if (!valid(grams) || !valid(flour) || flour <= 0.0) return null
        return bd(grams).multiply(BigDecimal(100)).divide(bd(flour), MC).toDouble()
    }

    /** A) Dogrudan su olarak girilen malzemelerin una orani. */
    fun freeWaterPercent(ings: List<CIng>): Double? {
        val flour = flourTotal(ings)
        if (flour <= 0.0) return null
        var s = BigDecimal.ZERO
        for (i in ings) if (i.category == Cat.WATER && valid(i.grams)) s = s.add(bd(i.grams))
        return s.multiply(BigDecimal(100)).divide(bd(flour), MC).toDouble()
    }

    /** B) Tahmini toplam su orani. Su orani bilinmeyen malzeme varsa complete=false olur. */
    fun estimatedWater(ings: List<CIng>, defaults: Map<String, Double?>): WaterEstimate {
        val flour = flourTotal(ings)
        var sum = BigDecimal.ZERO
        val missing = ArrayList<String>()
        for (i in ings) {
            if (!valid(i.grams) || i.grams <= 0.0) continue
            val p = i.waterPercent ?: defaults[i.category]
            if (p == null || p.isNaN() || p < 0.0 || p > 100.0) {
                missing.add(i.name)
            } else {
                sum = sum.add(bd(i.grams).multiply(bd(p)).divide(BigDecimal(100), MC))
            }
        }
        val pct = if (flour > 0.0) sum.multiply(BigDecimal(100)).divide(bd(flour), MC).toDouble() else null
        return WaterEstimate(pct, sum.toDouble(), missing.isEmpty(), missing)
    }

    /** Teorik parca sayisi = toplam hamur / parca gramaji. */
    fun theoreticalPieces(doughG: Double, pieceG: Double?): Double? {
        if (!valid(doughG) || pieceG == null || !valid(pieceG) || pieceG <= 0.0) return null
        return bd(doughG).divide(bd(pieceG), MC).toDouble()
    }

    /** Fire hesaba katilan parca sayisi. Fire bilinmiyorsa sifir varsayilir ve bu isaretlenir. */
    fun piecesWithLoss(doughG: Double, lossPct: Double?, pieceG: Double?): PieceResult {
        if (!valid(doughG) || pieceG == null || !valid(pieceG) || pieceG <= 0.0) return PieceResult(null, lossPct == null)
        val loss = lossPct ?: 0.0
        if (loss.isNaN() || loss < 0.0 || loss >= 100.0) return PieceResult(null, false)
        val keep = BigDecimal.ONE.subtract(bd(loss).divide(BigDecimal(100), MC))
        val v = bd(doughG).multiply(keep).divide(bd(pieceG), MC).toDouble()
        return PieceResult(v, lossPct == null)
    }

    fun wholePieces(x: Double?): Int? = if (x == null) null else Math.floor(x + 1e-9).toInt()

    /** Toplam un degisince tum malzemeler ayni oranda yeniden hesaplanir. Gecersizse null. */
    fun scaleToFlour(ings: List<CIng>, newFlour: Double): List<Double>? {
        val old = flourTotal(ings)
        if (old <= 0.0 || !valid(newFlour) || newFlour <= 0.0) return null
        val factor = bd(newFlour).divide(bd(old), MC)
        return ings.map { if (valid(it.grams)) bd(it.grams).multiply(factor).setScale(2, RoundingMode.HALF_UP).toDouble() else it.grams }
    }

    /** Hedef hamur miktarina gore olcekleme. */
    fun scaleToDough(ings: List<CIng>, targetDough: Double): List<Double>? {
        val old = totalWeight(ings)
        if (old <= 0.0 || !valid(targetDough) || targetDough <= 0.0) return null
        val factor = bd(targetDough).divide(bd(old), MC)
        return ings.map { if (valid(it.grams)) bd(it.grams).multiply(factor).setScale(2, RoundingMode.HALF_UP).toDouble() else it.grams }
    }

    fun lineCost(i: CIng): Double? {
        val p = i.price ?: return null
        if (!valid(i.grams) || p.isNaN() || p < 0.0) return null
        return if (i.priceUnit == "ADET") {
            val u = i.unitGrams
            if (u == null || !valid(u) || u <= 0.0) null
            else bd(i.grams).divide(bd(u), MC).multiply(bd(p)).toDouble()
        } else {
            bd(i.grams).divide(BigDecimal(1000), MC).multiply(bd(p)).toDouble()
        }
    }

    /**
     * Maliyet. Fiyati bilinmeyen malzeme sifir maliyetli sayilmaz: materialTotal null olur,
     * materialPartial yalnizca bilinen kisimlarin toplamidir.
     */
    fun cost(ings: List<CIng>, pieces: Double?, extra: ExtraCosts): CostResult {
        var sum = BigDecimal.ZERO
        val missing = ArrayList<String>()
        for (i in ings) {
            if (!valid(i.grams) || i.grams <= 0.0) continue
            val c = lineCost(i)
            if (c == null) missing.add(i.name) else sum = sum.add(bd(c))
        }
        val total: Double? = if (missing.isEmpty() && ings.any { valid(it.grams) && it.grams > 0.0 }) sum.toDouble() else null
        val doughKg = totalWeight(ings) / 1000.0
        val perKg = if (total != null && doughKg > 0.0) bd(total).divide(bd(doughKg), MC).toDouble() else null
        val pcs = if (pieces != null && pieces > 0.0) pieces else null
        val perPieceMat = if (total != null && pcs != null) bd(total).divide(bd(pcs), MC).toDouble() else null

        val missingExp = ArrayList<String>()
        if (extra.packagingPerPiece == null) missingExp.add("Ambalaj")
        if (extra.energyTotal == null) missingExp.add("Enerji")
        if (extra.laborTotal == null) missingExp.add("İşçilik")
        if (extra.otherTotal == null) missingExp.add("Diğer giderler")

        var perPieceTotal: Double? = null
        if (total != null && pcs != null) {
            var t = bd(total)
            t = t.add(bd(extra.energyTotal ?: 0.0)).add(bd(extra.laborTotal ?: 0.0)).add(bd(extra.otherTotal ?: 0.0))
            perPieceTotal = t.divide(bd(pcs), MC).add(bd(extra.packagingPerPiece ?: 0.0)).toDouble()
        }
        var contribution: Double? = null
        var margin: Double? = null
        val sp = extra.salePrice
        if (perPieceTotal != null && sp != null && sp > 0.0) {
            contribution = bd(sp).subtract(bd(perPieceTotal)).toDouble()
            margin = bd(contribution).multiply(BigDecimal(100)).divide(bd(sp), MC).toDouble()
        }
        return CostResult(total, sum.toDouble(), missing, perKg, perPieceMat, perPieceTotal, missingExp, contribution, margin)
    }
}

/** Turkiye yerel ayarina uygun sayi bicimlendirme ve ayristirma. */
object Fmt {
    private val symbols: DecimalFormatSymbols = DecimalFormatSymbols(Locale("tr", "TR"))

    fun num(d: Double?, maxDecimals: Int = 1): String {
        if (d == null || d.isNaN() || d.isInfinite()) return "—"
        val pattern = "#,##0" + (if (maxDecimals > 0) "." + "#".repeat(maxDecimals) else "")
        return DecimalFormat(pattern, symbols).format(d)
    }

    fun pct(d: Double?, maxDecimals: Int = 1): String = if (d == null) "—" else "%" + num(d, maxDecimals)

    fun tl(d: Double?): String = if (d == null) "—" else num(d, 2) + " ₺"

    /** Kullanici girdisini sayiya cevirir. "1,5" ve "1.5" kabul edilir. Bos veya gecersizse null. */
    fun parse(s: String?): Double? {
        if (s == null) return null
        var t = s.trim().replace(" ", "")
        if (t.isEmpty()) return null
        t = if (t.contains(',')) t.replace(".", "").replace(',', '.') else t
        val v = t.toDoubleOrNull() ?: return null
        if (v.isNaN() || v.isInfinite()) return null
        return v
    }

    /** Duzenleme kutusu icin: gereksiz sifirlar olmadan, virgullu. */
    fun edit(d: Double?): String {
        if (d == null) return ""
        val s = BigDecimal.valueOf(d).stripTrailingZeros().toPlainString()
        return s.replace('.', ',')
    }

    /** Negatif veya gecersiz girdi icin hata metni; sorun yoksa null. Bos girdi hata degildir. */
    fun errorFor(text: String, max: Double? = null): String? {
        if (text.isBlank()) return null
        val v = parse(text) ?: return "Geçerli bir sayı girin"
        if (v < 0.0) return "Negatif olamaz"
        if (max != null && v > max) return "En fazla " + num(max, 0) + " olabilir"
        return null
    }
}
