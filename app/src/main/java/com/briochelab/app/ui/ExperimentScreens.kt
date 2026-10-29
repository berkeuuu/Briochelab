package com.briochelab.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.briochelab.app.calc.Fmt
import com.briochelab.app.data.ChecklistItem
import com.briochelab.app.data.Experiment
import com.briochelab.app.data.ExpSnapshot
import com.briochelab.app.data.ExperimentVariable
import com.briochelab.app.data.Fields
import com.briochelab.app.data.Ingredient
import com.briochelab.app.data.Logic
import com.briochelab.app.data.ProcessMeasurement
import com.briochelab.app.data.Recipe
import com.briochelab.app.data.RecipeVersion
import com.briochelab.app.data.SensoryEvaluation
import com.briochelab.app.data.Util
import kotlinx.coroutines.launch

private data class VarRow(val uid: Int, val label: String, val oldV: String, val newV: String)

private val variableSuggestions = listOf(
    "Un markası", "Un miktarı ve özellikleri", "Hidrasyon", "Şeker oranı", "Tereyağı oranı", "Yumurta oranı",
    "Maya miktarı", "Tuz oranı", "Tangzhong kullanımı ve oranı", "Ön hamur kullanımı", "Yoğurma süresi",
    "Yoğurma sonu hamur sıcaklığı", "İlk fermantasyon süresi", "Son mayalanma süresi", "Soğuk mayalandırma koşulları",
    "Parça gramajı", "Yuvarlama yöntemi", "Pişirme sıcaklığı ve süresi", "Soğutma ve ambalajlama koşulları"
)

// ---------------------------------------------------------------------------------------------
// Yeni deneme
// ---------------------------------------------------------------------------------------------

@Composable
fun NewExperimentScreen(draft: ExpDraft?) {
    val repo = LocalRepo.current
    val dao = repo.dao
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val exps by rememberFlow(emptyList<Experiment>()) { dao.experiments() }
    val versions by rememberFlow(emptyList<RecipeVersion>()) { dao.allVersions() }
    val recipes by rememberFlow(emptyList<Recipe>()) { dao.recipes() }
    val allIngs by rememberFlow(emptyList<Ingredient>()) { dao.allIngredients() }

    val d = draft ?: ExpDraft(planned = false)
    var name by remember { mutableStateOf(d.name) }
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var versionId by remember { mutableStateOf(d.versionId) }
    var prevId by remember { mutableStateOf(d.previousId) }
    var purpose by remember { mutableStateOf(d.purpose) }
    var problem by remember { mutableStateOf(d.problem) }
    var hypothesis by remember { mutableStateOf(d.hypothesis) }
    var expected by remember { mutableStateOf(d.expected) }
    var criteria by remember { mutableStateOf(d.successCriteria) }
    var notes by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(1) }
    var planned by remember { mutableStateOf(d.planned) }
    var showPlan by remember { mutableStateOf(false) }
    var nextCode by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var uidCounter by remember { mutableStateOf(d.variables.size) }
    val vars = remember {
        mutableStateListOf<VarRow>().also { l ->
            d.variables.forEachIndexed { idx, v -> l.add(VarRow(idx, v.label, v.oldValue, v.newValue)) }
        }
    }
    val prefill = remember { mutableStateMapOf<String, ProcessMeasurement>() }

    LaunchedEffect(Unit) { nextCode = repo.nextExperimentCode() }

    // Ayni recetenin son denemesini otomatik oner (yalnizca onceki deney secilmemisse)
    LaunchedEffect(versionId, exps.size, versions.size) {
        if (prevId == null && draft?.previousId == null && versionId != null) {
            val rid = versions.firstOrNull { it.id == versionId }?.recipeId
            if (rid != null) {
                val vids = versions.filter { it.recipeId == rid }.map { it.id }.toSet()
                val last = exps.filter { it.versionId in vids }.maxByOrNull { it.date }
                if (last != null) prevId = last.id
            }
        }
    }

    val recipeById = recipes.associateBy { it.id }
    val sortedVersions = versions.sortedWith(compareBy<RecipeVersion>({ it.recipeId }, { -it.versionNo }))
    val versionLabels = listOf("Bağımsız deney (reçetesiz)") + sortedVersions.map {
        (recipeById[it.recipeId]?.code ?: "") + " " + (recipeById[it.recipeId]?.name ?: "") + " · v" + it.versionNo
    }
    val versionSel = if (versionId == null) 0 else (sortedVersions.indexOfFirst { it.id == versionId } + 1).coerceAtLeast(0)
    val prevOptions = exps.sortedByDescending { it.date }
    val prevLabels = listOf("Önceki deney yok") + prevOptions.map { expTitle(it) }
    val prevSel = if (prevId == null) 0 else (prevOptions.indexOfFirst { it.id == prevId } + 1).coerceAtLeast(0)

    val prevSnap by produceState<ExpSnapshot?>(null, prevId) { value = prevId?.let { repo.snapshot(it) } }
    val curVersion = versions.firstOrNull { it.id == versionId }
    val curIngs = allIngs.filter { it.versionId == versionId }
    val curSnap = ExpSnapshot(
        Experiment(code = "", name = name, date = date, createdAt = 0L), curVersion, curIngs, prefill.toMap(), null,
        vars.map { ExperimentVariable(experimentId = 0, label = it.label, oldValue = it.oldV, newValue = it.newV) }, emptyList()
    )
    val changes = Logic.changes(prevSnap, curSnap)
    val changeCount = Logic.changeCount(curSnap.variables.filter { it.label.isNotBlank() }, changes)

    Page {
        CardBox(tint = BrownLight) {
            Text("Deney kodu", fontSize = 12.sp)
            Text(if (nextCode.isEmpty()) "…" else nextCode, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brown)
            Text("Kod, kaydedince otomatik verilir.", fontSize = 12.sp)
        }

        SectionTitle("A. Deneyin amacı")
        TextEdit("Deney adı", name, { name = it })
        DateField("Tarih", date, { date = it })
        DropdownField("Bağlı reçete ve sürümü", versionLabels, versionSel, { i ->
            versionId = if (i == 0) null else sortedVersions[i - 1].id
        })
        DropdownField("Bir önceki deneme (karşılaştırma için)", prevLabels, prevSel, { i ->
            prevId = if (i == 0) null else prevOptions[i - 1].id
        })
        TextEdit("Deneyin ana amacı", purpose, { purpose = it }, minLines = 2)
        TextEdit("Mevcut sorun", problem, { problem = it }, minLines = 2)
        TextEdit("Test edilen hipotez", hypothesis, { hypothesis = it }, minLines = 2)
        Hint("Hipotez kesin bir gerçek değil, sınanacak bir varsayımdır. Sonuçlar gelince doğrulanıp doğrulanmadığına bakarsın.")
        TextEdit("Beklenen sonuç", expected, { expected = it }, minLines = 2)
        TextEdit("Başarı ölçütü (neye bakarak karar vereceksin?)", criteria, { criteria = it }, minLines = 2)
        Text("Öncelik", fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        Row {
            Chip("Düşük", priority == 0) { priority = 0 }
            Chip("Orta", priority == 1) { priority = 1 }
            Chip("Yüksek", priority == 2) { priority = 2 }
        }
        TextEdit("Etiketler (virgülle ayır)", tags, { tags = it })
        TextEdit("Serbest notlar", notes, { notes = it }, minLines = 2)

        SectionTitle("B. Değiştirilen değişkenler")
        Hint("Bu denemede neyi değiştirdiğini yaz. Hepsini doldurmak zorunda değilsin; yalnızca ilgili olanları ekle.")
        for (row in vars.toList()) {
            CardBox {
                TextEdit("Değişken", row.label, { t ->
                    val idx = vars.indexOfFirst { it.uid == row.uid }
                    if (idx >= 0) vars[idx] = vars[idx].copy(label = t)
                }, stateKey = row.uid)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        TextEdit("Eski değer", row.oldV, { t ->
                            val idx = vars.indexOfFirst { it.uid == row.uid }
                            if (idx >= 0) vars[idx] = vars[idx].copy(oldV = t)
                        }, stateKey = row.uid)
                    }
                    Column(Modifier.weight(1f)) {
                        TextEdit("Yeni değer", row.newV, { t ->
                            val idx = vars.indexOfFirst { it.uid == row.uid }
                            if (idx >= 0) vars[idx] = vars[idx].copy(newV = t)
                        }, stateKey = row.uid)
                    }
                }
                TextButton(onClick = { vars.removeAll { it.uid == row.uid } }) { Text("Bu değişkeni kaldır") }
            }
        }
        DropdownField("Hızlı ekle", listOf("Seçin…") + variableSuggestions, 0, { i ->
            if (i > 0) {
                uidCounter += 1
                vars.add(VarRow(1000 + uidCounter, variableSuggestions[i - 1], "", ""))
            }
        })
        OutlinedButton(
            onClick = {
                uidCounter += 1
                vars.add(VarRow(1000 + uidCounter, "", "", ""))
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Boş değişken satırı ekle") }

        if (prevId != null) {
            SectionTitle("Önceki denemeye göre otomatik tespit edilen farklar")
            if (changes.isEmpty()) {
                Hint("Reçete ve planlanan koşullarda önceki denemeden fark bulunmadı (henüz planlanan koşul girmediysen yalnızca reçete karşılaştırılır).")
            }
            for (c in changes) {
                CalcRow(c.label, c.before + " → " + c.after)
            }
        }
        if (changeCount >= Logic.MANY_CHANGES) {
            Warn(Logic.MANY_CHANGES_WARNING + " Bilinçli olarak birden fazla değişken denemek de mümkündür.")
        }

        SectionTitle("C. Planlanan koşullar (isteğe bağlı)")
        Hint("Burada girdiklerin \"tahmin / plan\" olarak kaydedilir. Deney sırasında gerçek ölçümleri deney ekranından girersin. Boş bıraktığın alan sıfır sayılmaz.")
        OutlinedButton(onClick = { showPlan = !showPlan }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showPlan) "Planlanan koşulları gizle" else "Planlanan koşulları göster (" + prefill.size + " dolu)")
        }
        if (showPlan) {
            MeasurementEditor(
                groups = Fields.groups.filter { it != Fields.G_RESULT },
                values = prefill.toMap(),
                keyPrefix = "yeni",
                showMeasuredToggle = false,
                defaultMeasured = false
            ) { key, text, numeric, _ ->
                if (text.isBlank()) {
                    prefill.remove(key)
                } else if (numeric) {
                    val dv = Fmt.parse(text)
                    if (dv != null && dv >= 0.0) prefill[key] = ProcessMeasurement(experimentId = 0, fieldKey = key, numValue = dv, measured = false)
                } else {
                    prefill[key] = ProcessMeasurement(experimentId = 0, fieldKey = key, textValue = text.trim(), measured = false)
                }
            }
        }

        SectionTitle("Kaydet")
        Row {
            Chip("Hemen başlat (Aktif)", !planned) { planned = false }
            Chip("Plan olarak kaydet", planned) { planned = true }
        }
        if (planned) Hint("Planlanan deney taslak gibidir; hazır olunca deney ekranından başlatabilirsin.")
        Button(
            enabled = !saving,
            onClick = {
                saving = true
                val e = Experiment(
                    code = "", name = name.trim().ifBlank { "Adsız deney" }, date = date, versionId = versionId,
                    previousExperimentId = prevId, purpose = purpose, problem = problem, hypothesis = hypothesis,
                    expected = expected, successCriteria = criteria, priority = priority, notes = notes,
                    status = if (planned) "PLANNED" else "ACTIVE", createdAt = 0L, tags = tags.trim()
                )
                val vs = vars.filter { it.label.isNotBlank() }.map {
                    ExperimentVariable(experimentId = 0, label = it.label.trim(), oldValue = it.oldV, newValue = it.newV)
                }
                scope.launch {
                    val id = repo.createExperiment(e, vs, prefill.toMap())
                    nav.replace(Screen.ExperimentDetail(id))
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) { Text("Deneyi kaydet") }
        Spacer8()
    }
}

// ---------------------------------------------------------------------------------------------
// Deney geçmişi
// ---------------------------------------------------------------------------------------------

@Composable
fun HistoryScreen() {
    val repo = LocalRepo.current
    val dao = repo.dao
    val nav = LocalNav.current
    val exps by rememberFlow(emptyList<Experiment>()) { dao.experiments() }
    val versions by rememberFlow(emptyList<RecipeVersion>()) { dao.allVersions() }
    val recipes by rememberFlow(emptyList<Recipe>()) { dao.recipes() }
    val evals by rememberFlow(emptyList<SensoryEvaluation>()) { dao.allEvaluations() }
    var q by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var range by remember { mutableStateOf(0) }
    var tag by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<Long>() }

    val versionById = versions.associateBy { it.id }
    val recipeById = recipes.associateBy { it.id }
    val evalByExp = evals.associateBy { it.experimentId }
    val allTags = exps.flatMap { e -> e.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() } }.distinct().sorted()
    val now = System.currentTimeMillis()
    val day = 24L * 60L * 60L * 1000L
    val shown = exps.filter { e ->
        val v = e.versionId?.let { versionById[it] }
        val r = v?.let { recipeById[it.recipeId] }
        val text = (e.code + " " + e.name + " " + e.purpose + " " + e.problem + " " + e.hypothesis + " " + e.notes + " " + e.tags + " " + (r?.name ?: ""))
        (q.isBlank() || text.contains(q, ignoreCase = true)) &&
            (status.isEmpty() || e.status == status) &&
            (tag.isEmpty() || e.tags.split(",").map { it.trim() }.contains(tag)) &&
            when (range) {
                1 -> e.date >= now - 7 * day
                2 -> e.date >= now - 30 * day
                3 -> e.date >= java.util.Calendar.getInstance().also { it.set(java.util.Calendar.DAY_OF_YEAR, 1); it.set(java.util.Calendar.HOUR_OF_DAY, 0) }.timeInMillis
                else -> true
            }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            item {
                Spacer8()
                Button(onClick = { nav.push(Screen.NewExperiment()) }, modifier = Modifier.fillMaxWidth()) { Text("Yeni deney") }
                Spacer8()
                TextEdit("Deney ara (kod, ad, not, etiket, reçete…)", "", { q = it })
                Row(Modifier.padding(top = 4.dp)) {
                    Chip("Tümü", status.isEmpty()) { status = "" }
                    Chip("Aktif", status == "ACTIVE") { status = "ACTIVE" }
                    Chip("Planlandı", status == "PLANNED") { status = "PLANNED" }
                    Chip("Tamamlandı", status == "COMPLETED") { status = "COMPLETED" }
                }
                Row {
                    Chip("Tüm zamanlar", range == 0) { range = 0 }
                    Chip("7 gün", range == 1) { range = 1 }
                    Chip("30 gün", range == 2) { range = 2 }
                    Chip("Bu yıl", range == 3) { range = 3 }
                }
                if (allTags.isNotEmpty()) {
                    Text("Etiket", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    androidx.compose.foundation.lazy.LazyRow {
                        item { Chip("Hepsi", tag.isEmpty()) { tag = "" } }
                        items(allTags) { t -> Chip(t, tag == t) { tag = if (tag == t) "" else t } }
                    }
                }
                Hint("Karşılaştırmak için kartların sağındaki kutuları işaretle (en az 2).")
                if (exps.isEmpty()) {
                    CardBox(tint = BrownLight) {
                        Text("Henüz deney yok", fontWeight = FontWeight.Bold)
                        BodyText("\"Yeni deney\" ile ilk kaydını oluştur.")
                    }
                } else if (shown.isEmpty()) {
                    Hint("Filtreye uyan deney yok.")
                }
            }
            items(shown, key = { it.id }) { e ->
                val v = e.versionId?.let { versionById[it] }
                val r = v?.let { recipeById[it.recipeId] }
                val ev = evalByExp[e.id]
                ClickableCard(onClick = { nav.push(Screen.ExperimentDetail(e.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(expTitle(e), fontWeight = FontWeight.Bold)
                            Text(Util.date(e.date) + " · " + Util.statusLabel(e.status), fontSize = 12.sp)
                            if (r != null && v != null) Text(r.name + " · v" + v.versionNo, fontSize = 12.sp, color = Brown)
                            else Text("Bağımsız deney", fontSize = 12.sp)
                            if (e.tags.isNotBlank()) Text("Etiket: " + e.tags, fontSize = 12.sp)
                            if (ev != null) {
                                Text(
                                    "Genel " + scoreText(ev.overall) + " · İlk gün " + scoreText(ev.day1Soft) + " · Ertesi gün " + scoreText(ev.nextDaySoft),
                                    fontSize = 12.sp, color = CalcInk
                                )
                            }
                        }
                        Checkbox(
                            checked = e.id in selected,
                            onCheckedChange = { c -> if (c) selected.add(e.id) else selected.remove(e.id) }
                        )
                    }
                }
            }
            item { Spacer8(); Spacer8(); Spacer8(); Spacer8() }
        }
        if (selected.size >= 2) {
            Button(
                onClick = { nav.push(Screen.Compare(selected.toList())) },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp)
            ) { Text("Seçilenleri karşılaştır (" + selected.size + ")") }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Kontrol listesi: deney seçici
// ---------------------------------------------------------------------------------------------

@Composable
fun ChecklistPickerScreen() {
    val repo = LocalRepo.current
    val dao = repo.dao
    val nav = LocalNav.current
    val exps by rememberFlow(emptyList<Experiment>()) { dao.experiments() }
    val items by rememberFlow(emptyList<ChecklistItem>()) { dao.allChecklist() }
    Page {
        Hint("Her deneyin kendi kontrol listesi vardır. Yeni deney oluşturunca yeni bir liste açılır; eski deneylerin işaretleri değişmez. Maddeleri tamamlandı, atlandı veya ilgili değil olarak işaretleyebilir, açıklama ekleyebilirsin.")
        val ordered = exps.sortedWith(compareBy<Experiment>({ if (it.status == "ACTIVE") 0 else if (it.status == "PLANNED") 1 else 2 }, { -it.date }))
        if (ordered.isEmpty()) {
            CardBox(tint = BrownLight) {
                Text("Henüz deney yok", fontWeight = FontWeight.Bold)
                BodyText("Kontrol listesi deneyle birlikte oluşur. Önce bir deney başlat.")
                Button(onClick = { nav.push(Screen.NewExperiment()) }) { Text("Yeni deney") }
            }
        }
        for (e in ordered) {
            val mine = items.filter { it.experimentId == e.id }
            val done = mine.count { it.status == 1 }
            val handled = mine.count { it.status != 0 }
            ClickableCard(onClick = { nav.push(Screen.ExperimentDetail(e.id, 2)) }) {
                Text(expTitle(e), fontWeight = FontWeight.Bold)
                Text(Util.date(e.date) + " · " + Util.statusLabel(e.status), fontSize = 12.sp)
                Text(
                    "Tamamlanan " + done + "/" + mine.size + " · işaretlenen " + handled + "/" + mine.size,
                    fontSize = 12.sp, color = CalcInk
                )
            }
        }
    }
}
