package com.briochelab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.briochelab.app.calc.Calc
import com.briochelab.app.calc.Cat
import com.briochelab.app.calc.Fmt
import com.briochelab.app.data.Advisor
import com.briochelab.app.data.Criteria
import com.briochelab.app.data.ExpSnapshot
import com.briochelab.app.data.Experiment
import com.briochelab.app.data.ExperimentNote
import com.briochelab.app.data.ExperimentPhoto
import com.briochelab.app.data.Fields
import com.briochelab.app.data.Logic
import com.briochelab.app.data.Stages
import com.briochelab.app.data.Util
import com.briochelab.app.data.score
import kotlinx.coroutines.flow.first

private class CmpItem(
    val snap: ExpSnapshot,
    val sum: VSummary?,
    val notes: List<ExperimentNote>,
    val photos: List<ExperimentPhoto>
)

/** section = true ise yalnizca baslik satiridir. */
private class CmpRow(val label: String, val values: List<String>, val section: Boolean = false, val highlight: Boolean = true)

private val cmpCritical = listOf(
    "doughTemp" to "Hamur sıcaklığı",
    "bakeMin" to "Pişirme süresi",
    "ovenSetC" to "Fırın sıcaklığı",
    "bakedWeight" to "Pişmiş ağırlık"
)

private fun mText(s: ExpSnapshot, key: String): String {
    val f = Fields.get(key) ?: return "—"
    val m = s.measurements[key] ?: return "—"
    val v: String = if (f.numeric) {
        val n = m.numValue ?: return "—"
        Fmt.num(n, 2) + (if (f.unit.isNotEmpty()) " " + f.unit else "")
    } else {
        if (m.textValue.isBlank()) return "—"
        m.textValue
    }
    return v + (if (m.measured) "" else " (tahmin)")
}

@Composable
fun CompareScreen(ids: List<Long>) {
    val repo = LocalRepo.current
    val dao = repo.dao
    val nav = LocalNav.current
    val exps by rememberFlow(emptyList<Experiment>()) { dao.experiments() }
    val sel = remember { mutableStateListOf<Long>() }
    val data by produceState<List<CmpItem>?>(null, ids) {
        if (ids.size < 2) {
            value = null
        } else {
            val defaults = Logic.waterDefaults(dao.propertiesOnce())
            val out = ArrayList<CmpItem>()
            for (id in ids) {
                val s = repo.snapshot(id) ?: continue
                val cost = s.exp.versionId?.let { dao.costOnce(it) }
                val sum = if (s.version != null) summarize(s.version, s.ingredients, cost, defaults) else null
                val notes = dao.notesOf(id).first()
                val photos = dao.photosOfExperiment(id).first()
                out.add(CmpItem(s, sum, notes, photos))
            }
            value = out
        }
    }

    if (ids.size < 2) {
        Page {
            Hint("Karşılaştırmak istediğin iki veya daha fazla deneyi seç.")
            if (exps.size < 2) {
                CardBox(tint = BrownLight) {
                    Text("Karşılaştırma için en az 2 deney gerekir", fontWeight = FontWeight.Bold)
                    BodyText("Önce deneyler oluştur; sonra buradan veya Deney Geçmişi ekranından seçerek yan yana görebilirsin.")
                }
            }
            for (e in exps) {
                ClickableCard(onClick = { if (e.id in sel) sel.remove(e.id) else sel.add(e.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(expTitle(e), fontWeight = FontWeight.Bold)
                            Text(Util.date(e.date) + " · " + Util.statusLabel(e.status), fontSize = 12.sp)
                        }
                        Checkbox(checked = e.id in sel, onCheckedChange = { c -> if (c) sel.add(e.id) else sel.remove(e.id) })
                    }
                }
            }
            Button(
                enabled = sel.size >= 2,
                onClick = { nav.replace(Screen.Compare(sel.toList())) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) { Text("Karşılaştır (" + sel.size + ")") }
        }
        return
    }

    val items = data
    if (items == null) {
        Page { Text("Yükleniyor…") }
        return
    }
    if (items.size < 2) {
        Page { Text("Seçilen deneylerden en az ikisi bulunamadı (silinmiş olabilir).") }
        return
    }

    val rows = ArrayList<CmpRow>()
    rows.add(CmpRow("Genel", emptyList(), section = true))
    rows.add(CmpRow("Ad", items.map { it.snap.exp.name }, highlight = false))
    rows.add(CmpRow("Tarih", items.map { Util.date(it.snap.exp.date) }, highlight = false))
    rows.add(CmpRow("Durum", items.map { Util.statusLabel(it.snap.exp.status) }, highlight = false))
    rows.add(CmpRow("Reçete sürümü", items.map { if (it.snap.version == null) "Bağımsız" else "v" + it.snap.version.versionNo + " (" + Util.date(it.snap.version.createdAt) + ")" }))
    rows.add(CmpRow("Un markası / türü", items.map { x -> val v = x.snap.version; if (v == null || (v.flourBrand + v.flourType).isBlank()) "—" else (v.flourBrand + " " + v.flourType).trim() }))
    rows.add(CmpRow("Un protein oranı", items.map { x -> x.snap.version?.flourProtein?.let { Fmt.pct(it, 1) } ?: "—" }))
    rows.add(CmpRow("Maya", items.map { x -> val v = x.snap.version; if (v == null || (v.yeastType + v.yeastBrand).isBlank()) "—" else (v.yeastType + " " + v.yeastBrand).trim() }))

    rows.add(CmpRow("Malzemeler ve fırıncı yüzdeleri (hesaplanan)", emptyList(), section = true))
    rows.add(CmpRow("Toplam un", items.map { x -> if (x.sum == null || x.sum.flour <= 0.0) "—" else Fmt.num(x.sum.flour, 1) + " g" }))
    for (cat in Cat.all) {
        if (cat == Cat.FLOUR) continue
        val vals = items.map { x ->
            val g = x.snap.ingredients.filter { i -> i.category == cat }.sumOf { i -> i.grams }
            val flour = x.sum?.flour ?: 0.0
            if (g <= 0.0) "—" else Fmt.num(g, 1) + " g" + (Calc.bakerPercent(g, flour)?.let { p -> " · " + Fmt.pct(p, 1) } ?: "")
        }
        if (vals.any { it != "—" }) rows.add(CmpRow(Cat.label(cat), vals))
    }
    rows.add(CmpRow("Serbest su oranı", items.map { x -> if (x.sum?.freeWater == null) "—" else Fmt.pct(x.sum.freeWater, 1) }))
    rows.add(CmpRow("Tahmini toplam su oranı", items.map { x -> if (x.sum?.water?.pct == null) "—" else Fmt.pct(x.sum.water.pct, 1) + (if (x.sum.water.complete) "" else " (eksik)") }))

    rows.add(CmpRow("Süreç ve koşullar", emptyList(), section = true))
    for (f in Fields.all) {
        if (f.group == Fields.G_RESULT) continue
        val vals = items.map { x -> mText(x.snap, f.key) }
        if (vals.any { it != "—" }) rows.add(CmpRow(f.label, vals))
    }
    rows.add(CmpRow("Pişirme kaybı (hesaplanan)", items.map { x ->
        val wb = x.snap.num("weightBefore")
        val wa = x.snap.num("weightAfter")
        if (wb != null && wa != null && wb > 0.0) Fmt.pct((wb - wa) / wb * 100.0, 1) else "—"
    }))

    rows.add(CmpRow("Hamur ve üretim", emptyList(), section = true))
    rows.add(CmpRow("Hedef parça gramajı", items.map { x -> x.snap.version?.targetPieceG?.let { Fmt.num(it, 1) + " g" } ?: "—" }))
    rows.add(CmpRow("Teorik parça sayısı", items.map { x -> x.sum?.theoretical?.let { Fmt.num(it, 1) } ?: "—" }))

    rows.add(CmpRow("Sonuç ölçümleri", emptyList(), section = true))
    for (f in Fields.all) {
        if (f.group != Fields.G_RESULT) continue
        rows.add(CmpRow(f.label, items.map { x -> mText(x.snap, f.key) }))
    }

    rows.add(CmpRow("Puanlar (1–10)", emptyList(), section = true))
    for (cr in Criteria.all) {
        rows.add(CmpRow(cr.label, items.map { x -> scoreText(x.snap.eval?.score(cr.key)) }))
    }
    rows.add(CmpRow("Başarı ölçütüne ulaşıldı mı?", items.map { x ->
        when (x.snap.eval?.goalMet) {
            1 -> "Evet"
            0 -> "Hayır"
            else -> "—"
        }
    }))

    rows.add(CmpRow("Maliyet (hesaplanan)", emptyList(), section = true))
    rows.add(CmpRow("Adet başı malzeme maliyeti", items.map { x -> if (x.sum?.cost?.perPieceMaterial == null) "—" else Fmt.tl(x.sum.cost.perPieceMaterial) }))
    rows.add(CmpRow("Adet başı toplam maliyet", items.map { x -> if (x.sum?.cost?.perPieceTotal == null) "—" else Fmt.tl(x.sum.cost.perPieceTotal) }))

    rows.add(CmpRow("Manuel gözlemler", emptyList(), section = true))
    rows.add(CmpRow("Notlar", items.map { x ->
        if (x.notes.isEmpty()) "—" else x.notes.size.toString() + " not\n" + x.notes.take(3).joinToString("\n") { n ->
            "• " + (if (n.title.isNotBlank()) n.title + ": " else "") + n.body.take(70) + (if (n.body.length > 70) "…" else "")
        }
    }, highlight = false))
    rows.add(CmpRow("Hipotez", items.map { x -> x.snap.exp.hypothesis.ifBlank { "—" } }, highlight = false))
    rows.add(CmpRow("Serbest değerlendirme", items.map { x -> x.snap.eval?.freeText?.ifBlank { "—" } ?: "—" }, highlight = false))
    rows.add(CmpRow("Beklenmeyen sonuçlar", items.map { x -> x.snap.eval?.failures?.ifBlank { "—" } ?: "—" }, highlight = false))

    rows.add(CmpRow("Eksik veriler", emptyList(), section = true))
    rows.add(CmpRow("Eksikler", items.map { x ->
        val miss = ArrayList<String>()
        for ((k, lab) in cmpCritical) if (!x.snap.hasValue(k)) miss.add(lab)
        val ev = x.snap.eval
        if (ev == null) miss.add("Değerlendirme yok") else {
            val n = Criteria.all.count { ev.score(it.key) == null }
            if (n > 0) miss.add(n.toString() + " puan boş")
        }
        if (x.snap.version == null) miss.add("Reçete bağlı değil")
        if (miss.isEmpty()) "Kritik eksik yok" else miss.joinToString(", ")
    }, highlight = false))

    val labW = 150.dp
    val colW = 140.dp
    val totalW = labW + colW * items.size

    Page {
        Hint("Sarı zeminli satırlarda denemeler arasında fark var. \"—\" o deneyde değer girilmediğini gösterir (sıfır anlamına gelmez). \"(tahmin)\" ölçülmemiş, planlanan değerdir.")
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            Row {
                Box(Modifier.width(labW))
                for (x in items) {
                    Box(Modifier.width(colW).padding(6.dp)) {
                        Column {
                            Text(x.snap.exp.code, fontWeight = FontWeight.Bold, color = Brown, fontSize = 13.sp)
                        }
                    }
                }
            }
            for (r in rows) {
                if (r.section) {
                    Box(Modifier.width(totalW).background(BrownLight).padding(6.dp)) {
                        Text(r.label, fontWeight = FontWeight.Bold, color = Brown, fontSize = 13.sp)
                    }
                } else {
                    val differs = r.highlight && r.values.distinct().size > 1
                    Row {
                        Box(Modifier.width(labW).padding(6.dp)) { Text(r.label, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                        for (v in r.values) {
                            Box(Modifier.width(colW).background(if (differs) WarnTint else Color.Transparent).padding(6.dp)) {
                                Text(v, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            Box(Modifier.width(totalW).background(BrownLight).padding(6.dp)) {
                Text("Fotoğraflar (aşamaya göre ilk fotoğraf)", fontWeight = FontWeight.Bold, color = Brown, fontSize = 13.sp)
            }
            for (slot in Stages.photoSlots) {
                if (items.none { x -> x.photos.any { it.slot == slot } }) continue
                Row {
                    Box(Modifier.width(labW).padding(6.dp)) { Text(slot, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                    for (x in items) {
                        val p = x.photos.firstOrNull { it.slot == slot }
                        Box(Modifier.width(colW).padding(6.dp)) {
                            if (p == null) Text("—", fontSize = 12.sp) else PhotoThumb(repo.photoFile(p.fileName), 120)
                        }
                    }
                }
            }
        }
        Warn("Fotoğrafları karşılaştırırken aynı açıdan, ışıkta ve mesafeden çekilmediyse görünür fark yanıltıcı olabilir. Uygulama fotoğraftan çap, hacim veya yumuşaklık ölçmez.")

        SectionTitle("Değişen parametreler ve temkinli yorum")
        for (i in 0 until items.size - 1) {
            val a = items[i].snap
            val b = items[i + 1].snap
            val ch = Logic.changes(a, b)
            val count = Logic.changeCount(b.variables, ch)
            CardBox {
                Text(a.exp.code + " → " + b.exp.code, fontWeight = FontWeight.Bold, color = Brown)
                if (ch.isEmpty() && b.variables.isEmpty()) {
                    Text("Otomatik tespit edilen fark yok.", fontSize = 13.sp)
                }
                for (c in ch) CalcRow(c.label, c.before + " → " + c.after)
                if (b.variables.isNotEmpty()) {
                    Text("İkinci deneyde belirtilen değişkenler: " + b.variables.joinToString(", ") { it.label }, fontSize = 13.sp)
                }
                if (count >= Logic.MANY_CHANGES) Warn(Logic.MANY_CHANGES_WARNING)
                Text(Advisor.cautiousComparison(a, b, count), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Spacer8()
    }
}
