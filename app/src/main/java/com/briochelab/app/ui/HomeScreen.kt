package com.briochelab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.briochelab.app.calc.Fmt
import com.briochelab.app.data.Experiment
import com.briochelab.app.data.Criteria
import com.briochelab.app.data.Logic
import com.briochelab.app.data.Util
import com.briochelab.app.data.score

@Composable
fun HomeScreen() {
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val dao = repo.dao
    val exps by rememberFlow(emptyList<com.briochelab.app.data.Experiment>()) { dao.experiments() }
    val evals by rememberFlow(emptyList<com.briochelab.app.data.SensoryEvaluation>()) { dao.allEvaluations() }
    val recipes by rememberFlow(emptyList<com.briochelab.app.data.Recipe>()) { dao.recipes() }
    val versions by rememberFlow(emptyList<com.briochelab.app.data.RecipeVersion>()) { dao.allVersions() }
    val ings by rememberFlow(emptyList<com.briochelab.app.data.Ingredient>()) { dao.allIngredients() }
    val costs by rememberFlow(emptyList<com.briochelab.app.data.CostRecord>()) { dao.allCosts() }

    val versionById = versions.associateBy { it.id }
    val recipeById = recipes.associateBy { it.id }
    val evalByExp = evals.associateBy { it.experimentId }

    val total = exps.size
    val completed = exps.count { it.status == "COMPLETED" }
    val active = exps.filter { it.status == "ACTIVE" }
    val planned = exps.filter { it.status == "PLANNED" }.sortedBy { it.date }
    val lastCreated = exps.maxByOrNull { it.createdAt }

    // En iyi recete: yalnizca "Genel kalite" puani ortalamasina gore
    val byRecipe = HashMap<Long, MutableList<Int>>()
    for (e in exps) {
        val vid = e.versionId ?: continue
        val v = versionById[vid] ?: continue
        val o = evalByExp[e.id]?.overall ?: continue
        byRecipe.getOrPut(v.recipeId) { mutableListOf() }.add(o)
    }
    val bestRecipe = byRecipe.maxByOrNull { (it.value.sum().toDouble() / it.value.size) }

    // En yuksek ertesi gun yumusaklik
    var bestNext: Pair<Experiment, Int>? = null
    for (e in exps) {
        val s = evalByExp[e.id]?.nextDaySoft ?: continue
        val cur = bestNext
        if (cur == null || s > cur.second) bestNext = Pair(e, s)
    }

    // En iyi form ve yukseklik (iki puan birlikte)
    var bestForm: Triple<Experiment, Int, Int>? = null
    for (e in exps) {
        val ev = evalByExp[e.id] ?: continue
        val h = ev.height ?: continue
        val r = ev.roundness ?: continue
        val cur = bestForm
        if (cur == null || h + r > cur.second + cur.third) bestForm = Triple(e, h, r)
    }

    // Son kaydedilen birim maliyet (malzeme)
    var lastCost: Pair<Experiment, Double>? = null
    for (e in exps.sortedByDescending { it.date }) {
        val vid = e.versionId ?: continue
        val v = versionById[vid] ?: continue
        val vi = ings.filter { it.versionId == vid }
        if (vi.isEmpty()) continue
        val s = summarize(v, vi, costs.firstOrNull { it.versionId == vid }, emptyMap())
        val pp = s.cost.perPieceMaterial
        if (pp != null) {
            lastCost = Pair(e, pp)
            break
        }
    }

    // Eksik degerlendirmesi olan deneyler
    val needEval = exps.filter { e ->
        e.status != "PLANNED" && run {
            val ev = evalByExp[e.id]
            ev == null || Criteria.all.count { ev.score(it.key) != null } < 3
        }
    }

    Page {
        if (exps.isEmpty() && recipes.isEmpty()) {
            CardBox(tint = BrownLight) {
                Text("Hoş geldin!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brown)
                BodyText("Burası brioche deney defterin. Önce bir reçete oluştur, sonra o reçeteyle yeni bir deneme başlat. Her denemede neyi değiştirdiğini, ne ölçtüğünü ve sonucu kaydedersin; uygulama önceki denemelerle karşılaştırmana yardım eder.")
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { nav.push(Screen.Recipes) }, modifier = Modifier.weight(1f)) { Text("İlk reçeteyi oluştur") }
                    OutlinedButton(onClick = { nav.push(Screen.NewExperiment()) }, modifier = Modifier.weight(1f)) { Text("Reçetesiz deney") }
                }
            }
        }

        SectionTitle("Genel Bakış")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) { StatCard("Toplam deney", total.toString()) }
            Column(Modifier.weight(1f)) { StatCard("Tamamlanan", completed.toString()) }
            Column(Modifier.weight(1f)) { StatCard("Aktif", active.size.toString()) }
        }

        if (active.isNotEmpty()) {
            Text("Aktif deneyler", fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 6.dp))
            for (e in active.take(5)) {
                ClickableCard(onClick = { nav.push(Screen.ExperimentDetail(e.id)) }) {
                    Text(expTitle(e), fontWeight = FontWeight.Bold)
                    Text(Util.date(e.date), fontSize = 12.sp)
                }
            }
        }

        StatCard(
            "Son oluşturulan deney",
            if (lastCreated == null) "Henüz deney yok" else expTitle(lastCreated),
            lastCreated?.let { Util.dateTime(it.createdAt) },
            onClick = lastCreated?.let { e -> { nav.push(Screen.ExperimentDetail(e.id)) } }
        )

        val np = planned.firstOrNull()
        StatCard(
            "Sıradaki planlanmış deney",
            if (np == null) "Planlanmış deney yok" else expTitle(np),
            np?.let { Util.date(it.date) },
            onClick = np?.let { e -> { nav.push(Screen.ExperimentDetail(e.id)) } }
        )

        val br = bestRecipe
        if (br != null) {
            val r = recipeById[br.key]
            val avgScore = br.value.sum().toDouble() / br.value.size
            StatCard(
                "En iyi değerlendirilen reçete",
                (r?.name ?: "Reçete") + " · " + Fmt.num(avgScore, 1) + "/10",
                "Ölçüt: yalnızca \"Genel kalite\" puanı ortalaması (" + br.value.size + " puanlı deney). Maliyet, ertesi gün yumuşaklığı ve tekrarlanabilirlik hesaba katılmaz; bu nedenle ticari açıdan en iyi reçete anlamına gelmez.",
                onClick = { nav.push(Screen.RecipeDetail(br.key)) }
            )
        } else {
            StatCard("En iyi değerlendirilen reçete", "Henüz veri yok", "Deneylere \"Genel kalite\" puanı girildikçe burada görünür.")
        }

        val bn = bestNext
        StatCard(
            "En yüksek ertesi gün yumuşaklık puanı",
            if (bn == null) "Henüz veri yok" else bn.second.toString() + "/10",
            bn?.let { expTitle(it.first) },
            onClick = bn?.let { p -> { nav.push(Screen.ExperimentDetail(p.first.id)) } }
        )

        val bf = bestForm
        StatCard(
            "En iyi form ve yükseklik sonucu",
            if (bf == null) "Henüz veri yok" else "Yükseklik " + bf.second + "/10 · Yuvarlak form " + bf.third + "/10",
            bf?.let { expTitle(it.first) },
            onClick = bf?.let { p -> { nav.push(Screen.ExperimentDetail(p.first.id)) } }
        )

        val lc = lastCost
        StatCard(
            "Son kaydedilen birim maliyet (yalnızca malzeme)",
            if (lc == null) "Hesaplanamadı" else Fmt.tl(lc.second) + " / adet",
            if (lc == null) "Reçetede tüm malzeme fiyatları ve parça gramajı girildiğinde hesaplanır." else expTitle(lc.first) + " · ambalaj, enerji, işçilik dahil değildir"
        )

        SectionTitle("Eksik değerlendirmesi olan deneyler (" + needEval.size + ")")
        if (needEval.isEmpty()) {
            Hint(if (exps.isEmpty()) "Henüz deney yok." else "Eksik değerlendirme yok.")
        } else {
            for (e in needEval.take(5)) {
                ClickableCard(onClick = { nav.push(Screen.ExperimentDetail(e.id, 5)) }) {
                    Text(expTitle(e), fontWeight = FontWeight.Medium)
                    Text("Değerlendirme eksik veya en az 3 puan girilmemiş", fontSize = 12.sp)
                }
            }
        }

        SectionTitle("Bölümler")
        val tiles = listOf(
            Triple<String, String, () -> Unit>("Yeni Deneme", "Yeni deney kaydı başlat", { nav.push(Screen.NewExperiment()) }),
            Triple<String, String, () -> Unit>("Reçetelerim", "Reçeteler, sürümler, hesaplar", { nav.push(Screen.Recipes) }),
            Triple<String, String, () -> Unit>("Deney Geçmişi", "Tüm denemeler, arama, filtre", { nav.push(Screen.History) }),
            Triple<String, String, () -> Unit>("Karşılaştırma", "İki veya daha fazla deneyi yan yana gör", { nav.push(Screen.Compare()) }),
            Triple<String, String, () -> Unit>("Deney Kontrol Listesi", "Aşama aşama işaretle", { nav.push(Screen.ChecklistPicker) }),
            Triple<String, String, () -> Unit>("Bilgi Merkezi", "Kısa teknik açıklamalar", { nav.push(Screen.Info) }),
            Triple<String, String, () -> Unit>("Yedekleme ve Ayarlar", "JSON/CSV yedek, geri yükleme", { nav.push(Screen.Settings) })
        )
        for (t in tiles) {
            ClickableCard(onClick = t.third) {
                Text(t.first, fontWeight = FontWeight.Bold, color = Brown)
                Text(t.second, fontSize = 12.sp)
            }
        }
        Hint("Kayıtlar yalnızca bu telefonda saklanır. Uygulama silinirse veya telefon bozulursa kaybolabilir; \"Yedekleme ve Ayarlar\"dan düzenli yedek alın.")
    }
}
