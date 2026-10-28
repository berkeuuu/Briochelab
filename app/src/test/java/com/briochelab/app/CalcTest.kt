package com.briochelab.app

import com.briochelab.app.calc.CIng
import com.briochelab.app.calc.Calc
import com.briochelab.app.calc.Cat
import com.briochelab.app.calc.ExtraCosts
import com.briochelab.app.calc.Fmt
import com.briochelab.app.data.BackupCodec
import com.briochelab.app.data.BackupData
import com.briochelab.app.data.BackupException
import com.briochelab.app.data.Experiment
import com.briochelab.app.data.ExpSnapshot
import com.briochelab.app.data.Ingredient
import com.briochelab.app.data.Logic
import com.briochelab.app.data.ProcessMeasurement
import com.briochelab.app.data.Recipe
import com.briochelab.app.data.RecipeVersion
import com.briochelab.app.data.SensoryEvaluation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CalcTest {
    private fun ing(name: String, cat: String, g: Double) = CIng(name, cat, g)

    // 1
    @Test fun sekerYuzdesi() {
        val p = Calc.bakerPercent(100.0, 500.0)
        assertEquals(20.0, p!!, 1e-9)
    }

    // 2
    @Test fun dogrudanSuOrani() {
        val l = listOf(ing("Un", Cat.FLOUR, 500.0), ing("Su", Cat.WATER, 300.0))
        assertEquals(60.0, Calc.freeWaterPercent(l)!!, 1e-9)
    }

    // 3
    @Test fun tahminiToplamSu() {
        val l = listOf(
            ing("Un", Cat.FLOUR, 500.0), ing("Su", Cat.WATER, 100.0), ing("Süt", Cat.MILK, 100.0),
            ing("Yumurta", Cat.EGG, 150.0), ing("Tereyağı", Cat.BUTTER, 100.0)
        )
        val w = Calc.estimatedWater(l, Cat.defaultWater)
        assertTrue(w.complete)
        assertEquals(63.1, w.pct!!, 1e-6)
        assertTrue(w.pct!! in 63.0..64.0)
    }

    @Test fun bilinmeyenSuOraniEksikIsaretlenir() {
        val l = listOf(ing("Un", Cat.FLOUR, 500.0), CIng("Bal", Cat.OTHER, 20.0))
        val w = Calc.estimatedWater(l, Cat.defaultWater)
        assertFalse(w.complete)
        assertEquals(listOf("Bal"), w.missing)
    }

    // 4
    @Test fun gramajDegisinceYuzdeVeToplamGuncellenir() {
        val a = listOf(ing("Un", Cat.FLOUR, 500.0), ing("Şeker", Cat.SUGAR, 50.0))
        val b = listOf(ing("Un", Cat.FLOUR, 500.0), ing("Şeker", Cat.SUGAR, 75.0))
        assertEquals(550.0, Calc.totalWeight(a), 1e-9)
        assertEquals(575.0, Calc.totalWeight(b), 1e-9)
        assertEquals(10.0, Calc.bakerPercent(50.0, 500.0)!!, 1e-9)
        assertEquals(15.0, Calc.bakerPercent(75.0, 500.0)!!, 1e-9)
    }

    @Test fun unDegisinceDigerleriOrantili() {
        val l = listOf(ing("Un", Cat.FLOUR, 500.0), ing("Şeker", Cat.SUGAR, 50.0), ing("Tuz", Cat.SALT, 10.0))
        val s = Calc.scaleToFlour(l, 1000.0)!!
        assertEquals(listOf(1000.0, 100.0, 20.0), s)
    }

    @Test fun hedefHamurOlcekleme() {
        val l = listOf(ing("Un", Cat.FLOUR, 500.0), ing("Su", Cat.WATER, 500.0))
        val s = Calc.scaleToDough(l, 2000.0)!!
        assertEquals(listOf(1000.0, 1000.0), s)
    }

    // 5
    @Test fun teorikParcaSayisi() {
        assertEquals(10.0, Calc.theoreticalPieces(1000.0, 100.0)!!, 1e-9)
        assertEquals(8.0, Calc.theoreticalPieces(1000.0, 125.0)!!, 1e-9)
        assertEquals(8, Calc.wholePieces(Calc.theoreticalPieces(1000.0, 120.0)))
    }

    @Test fun fireIleParcaSayisi() {
        val r = Calc.piecesWithLoss(1000.0, 10.0, 100.0)
        assertEquals(9.0, r.value!!, 1e-9)
        assertFalse(r.lossAssumedZero)
        val z = Calc.piecesWithLoss(1000.0, null, 100.0)
        assertEquals(10.0, z.value!!, 1e-9)
        assertTrue(z.lossAssumedZero)
    }

    // 6
    @Test fun sifirUnSifiraBolmeYapmaz() {
        assertNull(Calc.bakerPercent(100.0, 0.0))
        val l = listOf(ing("Un", Cat.FLOUR, 0.0), ing("Su", Cat.WATER, 100.0))
        assertNull(Calc.freeWaterPercent(l))
        assertNull(Calc.estimatedWater(l, Cat.defaultWater).pct)
        assertNull(Calc.scaleToFlour(l, 500.0))
        assertNull(Calc.theoreticalPieces(1000.0, 0.0))
    }

    // 7
    @Test fun negatifVeGecersizGirdi() {
        assertNull(Calc.bakerPercent(-5.0, 500.0))
        assertNull(Calc.bakerPercent(5.0, -500.0))
        assertNotNull(Fmt.errorFor("-3"))
        assertNotNull(Fmt.errorFor("abc"))
        assertNull(Fmt.errorFor(""))
        assertNotNull(Fmt.errorFor("120", 100.0))
        assertNull(Calc.piecesWithLoss(1000.0, 100.0, 100.0).value)
        assertNull(Calc.piecesWithLoss(1000.0, -1.0, 100.0).value)
        val l = listOf(ing("Un", Cat.FLOUR, 500.0), ing("Hatalı", Cat.SUGAR, -10.0))
        assertEquals(500.0, Calc.totalWeight(l), 1e-9)
    }

    // 8
    @Test fun bosOlcumSifirOlmaz() {
        assertNull(Fmt.parse(""))
        assertNull(Fmt.parse("   "))
        assertNull(Fmt.parse(null))
        assertEquals("", Fmt.edit(null))
        assertEquals("—", Fmt.num(null))
    }

    @Test fun maliyetEksikFiyatSifirSayilmaz() {
        val l = listOf(
            CIng("Un", Cat.FLOUR, 1000.0, price = 30.0),
            CIng("Tereyağı", Cat.BUTTER, 500.0)
        )
        val c = Calc.cost(l, 10.0, ExtraCosts())
        assertNull(c.materialTotal)
        assertEquals(30.0, c.materialPartial, 1e-9)
        assertEquals(listOf("Tereyağı"), c.missingPrices)
    }

    @Test fun maliyetHesabi() {
        val l = listOf(
            CIng("Un", Cat.FLOUR, 1000.0, price = 30.0),
            CIng("Yumurta", Cat.EGG, 200.0, price = 5.0, priceUnit = "ADET", unitGrams = 50.0)
        )
        val c = Calc.cost(l, 10.0, ExtraCosts(packagingPerPiece = 1.0, energyTotal = 10.0, laborTotal = 20.0, otherTotal = 0.0, salePrice = 20.0))
        assertEquals(50.0, c.materialTotal!!, 1e-9)
        assertEquals(5.0, c.perPieceMaterial!!, 1e-9)
        assertEquals(50.0 / 1.2, c.perKgDough!!, 1e-6)
        // (50 + 10 + 20 + 0) / 10 + 1 = 9
        assertEquals(9.0, c.perPieceTotal!!, 1e-9)
        assertEquals(11.0, c.contribution!!, 1e-9)
        assertEquals(55.0, c.marginPct!!, 1e-9)
        assertTrue(c.missingExpenses.isEmpty())
    }

    // 14
    @Test fun turkceSayiBicimi() {
        assertEquals("1.234,5", Fmt.num(1234.5, 2))
        assertEquals("63,1", Fmt.num(63.1, 1))
        assertEquals("%20", Fmt.pct(20.0, 1))
        assertEquals(1.5, Fmt.parse("1,5")!!, 1e-12)
        assertEquals(1.5, Fmt.parse("1.5")!!, 1e-12)
        assertEquals(1234.5, Fmt.parse("1.234,5")!!, 1e-12)
        assertEquals("0,5", Fmt.edit(0.5))
        assertEquals("500", Fmt.edit(500.0))
    }

    // 12
    @Test fun degisenParametrelerDogruBulunur() {
        val v1 = RecipeVersion(id = 1, recipeId = 1, versionNo = 1, createdAt = 0, flourBrand = "A")
        val v2 = RecipeVersion(id = 2, recipeId = 1, versionNo = 2, createdAt = 0, flourBrand = "B")
        val ingA = listOf(
            Ingredient(1, 1, "Un", Cat.FLOUR, 500.0), Ingredient(2, 1, "Şeker", Cat.SUGAR, 50.0)
        )
        val ingB = listOf(
            Ingredient(3, 2, "Un", Cat.FLOUR, 500.0), Ingredient(4, 2, "Şeker", Cat.SUGAR, 75.0)
        )
        val e1 = Experiment(id = 1, code = "BL-2026-001", name = "a", date = 0, versionId = 1, createdAt = 0)
        val e2 = Experiment(id = 2, code = "BL-2026-002", name = "b", date = 0, versionId = 2, createdAt = 0)
        val m1 = mapOf("kneadMin" to ProcessMeasurement(experimentId = 1, fieldKey = "kneadMin", numValue = 10.0))
        val m2 = mapOf("kneadMin" to ProcessMeasurement(experimentId = 2, fieldKey = "kneadMin", numValue = 12.0))
        val a = ExpSnapshot(e1, v1, ingA, m1, null, emptyList(), emptyList())
        val b = ExpSnapshot(e2, v2, ingB, m2, null, emptyList(), emptyList())
        val ch = Logic.changes(a, b)
        val labels = ch.map { it.label }
        assertTrue(labels.contains("Reçete sürümü"))
        assertTrue(labels.contains("Un markası"))
        assertTrue(labels.any { it.startsWith("Şeker") })
        assertTrue(labels.contains("Yoğurma süresi"))
        val sugar = ch.first { it.label.startsWith("Şeker") }
        assertEquals("%10", sugar.before)
        assertEquals("%15", sugar.after)
        assertTrue(Logic.changes(null, b).isEmpty())
    }

    // 13
    @Test fun yedekGeriYuklemeKayitlariVeIliskileriKorur() {
        val data = BackupData(
            recipes = listOf(Recipe(1, "R-001", "Brioche Ç", "açıklama ğüşiöç", "hedef", "", 1000)),
            versions = listOf(
                RecipeVersion(1, 1, 1, null, 1000, flourBrand = "İpek", flourProtein = 12.5, targetPieceG = 90.0),
                RecipeVersion(2, 1, 2, 1, 2000, expectedDoughG = null)
            ),
            ingredients = listOf(Ingredient(1, 1, "Un", Cat.FLOUR, 500.5, null, 30.0, "KG", null, 0)),
            experiments = listOf(Experiment(1, "BL-2026-001", "Deneme", 5000, 2, null, createdAt = 5000)),
            measurements = listOf(
                ProcessMeasurement(1, 1, "doughTemp", 25.5, "", true),
                ProcessMeasurement(2, 1, "mixer", null, "Spiral", false)
            ),
            evaluations = listOf(SensoryEvaluation(1, 1, softness = 8, height = null, goalMet = 1, freeText = "güzel", updatedAt = 9))
        )
        val json = BackupCodec.encode(data, 123L, "1.0.0")
        val back = BackupCodec.decode(json)
        assertEquals(data.recipes, back.recipes)
        assertEquals(data.versions, back.versions)
        assertEquals(data.ingredients, back.ingredients)
        assertEquals(data.experiments, back.experiments)
        assertEquals(data.measurements, back.measurements)
        assertEquals(data.evaluations, back.evaluations)
        assertEquals(2L, back.experiments[0].versionId)
        assertEquals(1L, back.versions[1].parentVersionId)
        assertNull(back.measurements[1].numValue)
        assertFalse(back.measurements[1].measured)
    }

    @Test fun bozukYedekHataVerir() {
        try {
            BackupCodec.decode("bu json degil")
            fail("hata bekleniyordu")
        } catch (e: BackupException) {
            assertTrue(e.message!!.isNotEmpty())
        }
        try {
            BackupCodec.decode("{\"app\":\"Baska\",\"schema\":1}")
            fail("hata bekleniyordu")
        } catch (e: BackupException) {
            assertTrue(e.message!!.contains("BriocheLab"))
        }
        try {
            BackupCodec.decode("{\"app\":\"BriocheLab\",\"schema\":99}")
            fail("hata bekleniyordu")
        } catch (e: BackupException) {
            assertTrue(e.message!!.contains("güncelle"))
        }
    }
}
