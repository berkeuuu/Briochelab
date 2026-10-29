@file:OptIn(ExperimentalMaterial3Api::class)

package com.briochelab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.briochelab.app.calc.Fmt
import com.briochelab.app.data.Advice
import com.briochelab.app.data.Advisor
import com.briochelab.app.data.Change
import com.briochelab.app.data.ChecklistItem
import com.briochelab.app.data.Criteria
import com.briochelab.app.data.ExpSnapshot
import com.briochelab.app.data.Experiment
import com.briochelab.app.data.ExperimentNote
import com.briochelab.app.data.ExperimentPhoto
import com.briochelab.app.data.ExperimentRecommendation
import com.briochelab.app.data.ExperimentVariable
import com.briochelab.app.data.Fields
import com.briochelab.app.data.Ingredient
import com.briochelab.app.data.IngredientProperty
import com.briochelab.app.data.Logic
import com.briochelab.app.data.ProcessMeasurement
import com.briochelab.app.data.Recipe
import com.briochelab.app.data.RecipeVersion
import com.briochelab.app.data.SensoryEvaluation
import com.briochelab.app.data.Stages
import com.briochelab.app.data.Util
import com.briochelab.app.data.score
import com.briochelab.app.data.withScore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private class Ctx(
    val repo: com.briochelab.app.data.Repo,
    val nav: Nav,
    val saver: AutoSaver,
    val scope: CoroutineScope,
    val id: Long,
    val exp: Experiment,
    val upd: (Experiment) -> Unit,
    val version: RecipeVersion?,
    val recipe: Recipe?,
    val ings: List<Ingredient>,
    val defaults: Map<String, Double?>,
    val msMap: Map<String, ProcessMeasurement>,
    val snap: ExpSnapshot,
    val prev: ExpSnapshot?,
    val changes: List<Change>,
    val declared: List<ExperimentVariable>,
    val allExps: List<Experiment>
)

private val tabNames = listOf("Genel", "Koşullar", "Kontrol listesi", "Notlar", "Fotoğraflar", "Değerlendirme", "Öneriler")

private val criticalFields = listOf(
    "doughTemp" to "Yoğurma sonu hamur sıcaklığı",
    "bakeMin" to "Pişirme süresi",
    "ovenSetC" to "Fırın ayar sıcaklığı",
    "bakedWeight" to "Pişmiş ekmek ağırlığı"
)

@Composable
fun ExperimentDetailScreen(id: Long, tab: Int) {
    val repo = LocalRepo.current
    val dao = repo.dao
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val saver = rememberAutoSaver()
    var tabIdx by rememberSaveable(id) { mutableStateOf(tab) }
    var cur by remember(id) { mutableStateOf<Experiment?>(null) }
    var loaded by remember(id) { mutableStateOf(false) }
    var localEv by remember(id) { mutableStateOf<SensoryEvaluation?>(null) }
    var evLoaded by remember(id) { mutableStateOf(false) }
    var askComplete by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        cur = dao.getExperiment(id)
        loaded = true
        localEv = dao.evaluationOf(id).first()
        evLoaded = true
    }

    val vid = cur?.versionId
    val version by rememberFlow<RecipeVersion?>(null, vid) { if (vid == null) flowOf<RecipeVersion?>(null) else dao.version(vid) }
    val ings by rememberFlow(emptyList<Ingredient>(), vid) { if (vid == null) flowOf(emptyList<Ingredient>()) else dao.ingredientsOf(vid) }
    val msList by rememberFlow<List<ProcessMeasurement>?>(null, id) { dao.measurementsOf(id) }
    val items by rememberFlow<List<ChecklistItem>?>(null, id) { dao.checklistOf(id) }
    val notes by rememberFlow<List<ExperimentNote>?>(null, id) { dao.notesOf(id) }
    val photos by rememberFlow(emptyList<ExperimentPhoto>(), id) { dao.photosOfExperiment(id) }
    val declared by rememberFlow<List<ExperimentVariable>?>(null, id) { dao.variablesOf(id) }
    val recs by rememberFlow<List<ExperimentRecommendation>?>(null, id) { dao.recsOf(id) }
    val allExps by rememberFlow(emptyList<Experiment>()) { dao.experiments() }
    val props by rememberFlow(emptyList<IngredientProperty>()) { dao.properties() }
    val prevId = cur?.previousExperimentId
    val prevSnap by produceState<ExpSnapshot?>(null, prevId) { value = if (prevId == null) null else repo.snapshot(prevId) }
    val recipe by produceState<Recipe?>(null, version?.recipeId) { value = version?.let { dao.getRecipe(it.recipeId) } }

    val e = cur
    if (e == null) {
        Page { Text(if (loaded) "Bu deney bulunamadı." else "Yükleniyor…") }
        return
    }

    val msMap = (msList ?: emptyList()).associateBy { it.fieldKey }
    val snap = ExpSnapshot(e, version, ings, msMap, localEv, declared ?: emptyList(), items ?: emptyList())
    val changes = Logic.changes(prevSnap, snap)
    val defaults = Logic.waterDefaults(props)
    val upd: (Experiment) -> Unit = { n ->
        cur = n
        saver.save { dao.updateExperiment(n) }
    }
    val ctx = Ctx(
        repo, nav, saver, scope, id, e, upd, version, recipe, ings, defaults, msMap, snap, prevSnap, changes,
        declared ?: emptyList(), allExps
    )
    val missingCritical = criticalFields.filter { !snap.hasValue(it.first) }.map { it.second }
    val pending = (items ?: emptyList()).count { it.status == 0 }

    Column(Modifier.fillMaxSize()) {
        CardBoxHeader(e)
        ScrollableTabRow(selectedTabIndex = tabIdx, edgePadding = 0.dp) {
            tabNames.forEachIndexed { i, n ->
                Tab(selected = tabIdx == i, onClick = { tabIdx = i }, text = { Text(n, fontSize = 13.sp) })
            }
        }
        Box(Modifier.weight(1f)) {
            when (tabIdx) {
                0 -> TabGeneral(ctx, { askComplete = true }, { askDelete = true })
                1 -> TabConditions(ctx, msList != null)
                2 -> TabChecklist(ctx, items, missingCritical)
                3 -> TabNotes(ctx, notes)
                4 -> Page { PhotoSection(id, null, photos, Stages.photoSlots) }
                5 -> TabEvaluation(
                    ctx, evLoaded, localEv, msList != null,
                    { n ->
                        localEv = n
                        saver.save { repo.saveEvaluation(n) }
                    }
                )
                else -> TabAdvice(ctx, recs, evLoaded)
            }
        }
    }

    if (askComplete) {
        AlertDialog(
            onDismissRequest = { askComplete = false },
            title = { Text("Deney tamamlansın mı?") },
            text = {
                Column {
                    Text("Tamamlayınca değerlendirme ekranı açılır. Notların, kontrol listen ve ölçümlerin korunur; deneyi istersen yeniden açabilirsin.")
                    if (pending > 0) Text("\n• Kontrol listesinde işaretlenmemiş " + pending + " madde var (tamamlamayı engellemez).")
                    if (missingCritical.isNotEmpty()) {
                        Text("\n• Şu önemli kayıtlar boş: " + missingCritical.joinToString(", ") + ". Bu eksikler sonuçların güvenilirliğini etkileyebilir; eski kayıt geçersiz sayılmaz.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    askComplete = false
                    upd(e.copy(status = "COMPLETED", completedAt = System.currentTimeMillis()))
                    tabIdx = 5
                }) { Text("Tamamla") }
            },
            dismissButton = { TextButton(onClick = { askComplete = false }) { Text("Vazgeç") } }
        )
    }
    if (askDelete) {
        ConfirmDialog(
            title = "Deney silinsin mi?",
            text = "Bu deneyin tüm ölçümleri, notları, kontrol listesi ve fotoğrafları silinir. Bu işlem geri alınamaz. Sonraki denemeler bu deneyle bağlantısını kaybeder.",
            confirmLabel = "Sil",
            onConfirm = {
                askDelete = false
                scope.launch {
                    for (p in photos) {
                        dao.deletePhoto(p)
                        repo.photoFile(p.fileName).delete()
                    }
                    dao.deleteExperiment(e)
                    nav.pop()
                }
            },
            onDismiss = { askDelete = false }
        )
    }
}

@Composable
private fun CardBoxHeader(e: Experiment) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Column {
            Text(e.code + " · " + e.name, fontWeight = FontWeight.Bold, color = Brown, fontSize = 15.sp, maxLines = 1)
            Text(Util.statusLabel(e.status) + " · " + Util.date(e.date), fontSize = 12.sp)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Genel
// ---------------------------------------------------------------------------------------------

@Composable
private fun TabGeneral(c: Ctx, onComplete: () -> Unit, onDelete: () -> Unit) {
    val e = c.exp
    val dao = c.repo.dao
    var vLabel by remember(c.id) { mutableStateOf("") }
    var vOld by remember(c.id) { mutableStateOf("") }
    var vNew by remember(c.id) { mutableStateOf("") }
    var formKey by remember(c.id) { mutableStateOf(0) }

    Page {
        CardBox(tint = BrownLight) {
            Text("Durum: " + Util.statusLabel(e.status), fontWeight = FontWeight.Bold)
            if (e.completedAt != null) Text("Tamamlanma: " + Util.dateTime(e.completedAt), fontSize = 12.sp)
            when (e.status) {
                "PLANNED" -> Button(onClick = { c.upd(e.copy(status = "ACTIVE")) }, modifier = Modifier.fillMaxWidth()) { Text("Denemeyi başlat") }
                "ACTIVE" -> Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) { Text("Deneyi tamamla") }
                else -> OutlinedButton(onClick = { c.upd(e.copy(status = "ACTIVE", completedAt = null)) }, modifier = Modifier.fillMaxWidth()) { Text("Yeniden aç") }
            }
        }

        SectionTitle("Reçete")
        val v = c.version
        if (v == null) {
            Hint("Bu deney bir reçeteye bağlı değil (bağımsız deney).")
        } else {
            val s = summarize(v, c.ings, null, c.defaults)
            Text((c.recipe?.code ?: "") + " · " + (c.recipe?.name ?: "Reçete") + " · v" + v.versionNo, fontWeight = FontWeight.Bold)
            if (c.ings.isNotEmpty() && s.flour > 0.0) {
                CardBox(tint = CalcTint) {
                    for (i in c.ings) {
                        if (i.grams > 0.0) {
                            val p = com.briochelab.app.calc.Calc.bakerPercent(i.grams, s.flour)
                            Text(i.name + ": " + Fmt.num(i.grams, 1) + " g · " + Fmt.pct(p, 1), fontSize = 13.sp, color = CalcInk)
                        }
                    }
                    Text("Serbest su: " + Fmt.pct(s.freeWater, 1) + " · Tahmini toplam su: " + Fmt.pct(s.water.pct, 1) + (if (s.water.complete) "" else " (eksik)"), fontSize = 12.sp, color = CalcInk)
                    Text("Hesaplanan değerler", fontSize = 10.sp, color = CalcInk)
                }
            }
            OutlinedButton(onClick = { c.nav.push(Screen.VersionEdit(v.id)) }, modifier = Modifier.fillMaxWidth()) { Text("Reçete sürümünü aç") }
        }

        SectionTitle("Deney bilgileri")
        TextEdit("Deney adı", e.name, { c.upd(e.copy(name = it)) }, stateKey = c.id)
        DateField("Tarih", e.date, { c.upd(e.copy(date = it)) })
        TextEdit("Deneyin ana amacı", e.purpose, { c.upd(e.copy(purpose = it)) }, minLines = 2, stateKey = c.id)
        TextEdit("Mevcut sorun", e.problem, { c.upd(e.copy(problem = it)) }, minLines = 2, stateKey = c.id)
        TextEdit("Test edilen hipotez", e.hypothesis, { c.upd(e.copy(hypothesis = it)) }, minLines = 2, stateKey = c.id)
        Hint("Hipotez kanıtlanmış bir bilgi değil, sınanan bir varsayımdır.")
        TextEdit("Beklenen sonuç", e.expected, { c.upd(e.copy(expected = it)) }, minLines = 2, stateKey = c.id)
        TextEdit("Başarı ölçütü", e.successCriteria, { c.upd(e.copy(successCriteria = it)) }, minLines = 2, stateKey = c.id)
        Text("Öncelik", fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        Row {
            Chip("Düşük", e.priority == 0) { c.upd(e.copy(priority = 0)) }
            Chip("Orta", e.priority == 1) { c.upd(e.copy(priority = 1)) }
            Chip("Yüksek", e.priority == 2) { c.upd(e.copy(priority = 2)) }
        }
        TextEdit("Etiketler (virgülle ayır)", e.tags, { c.upd(e.copy(tags = it)) }, stateKey = c.id)
        TextEdit("Serbest notlar", e.notes, { c.upd(e.copy(notes = it)) }, minLines = 3, stateKey = c.id)

        SectionTitle("Önceki deneme")
        val others = c.allExps.filter { it.id != c.id }.sortedByDescending { it.date }
        val labels = listOf("Önceki deney yok") + others.map { expTitle(it) }
        val sel = if (e.previousExperimentId == null) 0 else (others.indexOfFirst { it.id == e.previousExperimentId } + 1).coerceAtLeast(0)
        DropdownField("Karşılaştırma için önceki deneme", labels, sel, { i ->
            c.upd(e.copy(previousExperimentId = if (i == 0) null else others[i - 1].id))
        })
        if (c.prev != null) {
            OutlinedButton(onClick = { c.nav.push(Screen.ExperimentDetail(c.prev.exp.id)) }, modifier = Modifier.fillMaxWidth()) {
                Text("Önceki denemeyi aç: " + c.prev.exp.code)
            }
        }

        SectionTitle("Değiştirilen değişkenler (senin belirttiklerin)")
        if (c.declared.isEmpty()) Hint("Henüz değişken eklenmedi.")
        for (d in c.declared) {
            CardBox {
                Text(d.label, fontWeight = FontWeight.Bold)
                if (d.oldValue.isNotBlank() || d.newValue.isNotBlank()) Text((d.oldValue.ifBlank { "—" }) + " → " + d.newValue.ifBlank { "—" }, fontSize = 13.sp)
                TextButton(onClick = { c.scope.launch { dao.deleteVariable(d) } }) { Text("Kaldır") }
            }
        }
        TextEdit("Değişken adı", vLabel, { vLabel = it }, stateKey = formKey)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) { TextEdit("Eski değer", vOld, { vOld = it }, stateKey = formKey) }
            Column(Modifier.weight(1f)) { TextEdit("Yeni değer", vNew, { vNew = it }, stateKey = formKey) }
        }
        OutlinedButton(
            enabled = vLabel.isNotBlank(),
            onClick = {
                val row = ExperimentVariable(experimentId = c.id, label = vLabel.trim(), oldValue = vOld, newValue = vNew)
                c.scope.launch { dao.insertVariable(row) }
                vLabel = ""; vOld = ""; vNew = ""; formKey += 1
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Değişkeni ekle") }

        if (c.prev != null) {
            SectionTitle("Önceki denemeye göre otomatik tespit edilen farklar")
            if (c.changes.isEmpty()) Hint("Reçete ve ölçümlerde fark bulunamadı (ya da karşılaştırılacak değer girilmemiş).")
            for (ch in c.changes) CalcRow(ch.label, ch.before + " → " + ch.after)
        }
        if (Logic.changeCount(c.declared, c.changes) >= Logic.MANY_CHANGES) {
            Warn(Logic.MANY_CHANGES_WARNING + " Bilinçli olarak birden fazla değişken denemek de mümkündür.")
        }

        Spacer8()
        OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("Deneyi sil") }
        Spacer8()
    }
}

// ---------------------------------------------------------------------------------------------
// Koşullar ve ölçümler
// ---------------------------------------------------------------------------------------------

@Composable
private fun TabConditions(c: Ctx, ready: Boolean) {
    Page {
        if (!ready) {
            Text("Yükleniyor…")
            return@Page
        }
        Hint("Ölçmediğin alanları boş bırak; boş alan sıfır olarak kaydedilmez. Tahmin ettiğin veya planladığın değerleri \"Tahmin / plan\" olarak işaretle; ölçülen değerler \"Ölçüldü\" olarak kalır.")
        MeasurementEditor(
            groups = Fields.groups.filter { it != Fields.G_RESULT },
            values = c.msMap,
            keyPrefix = c.id,
            showMeasuredToggle = true,
            defaultMeasured = true
        ) { key, text, numeric, measured ->
            c.saver.save { c.repo.setMeasurement(c.id, key, text, numeric, measured) }
        }
        SectionTitle("Hesaplanan")
        val wb = c.msMap["weightBefore"]?.numValue
        val wa = c.msMap["weightAfter"]?.numValue
        if (wb != null && wa != null && wb > 0.0) {
            CalcRow("Pişirme kaybı (parça)", Fmt.pct((wb - wa) / wb * 100.0, 1), "Pişirme öncesi ve sonrası ağırlıktan hesaplandı.")
        } else {
            Hint("Pişirme kaybı, pişirme öncesi ve sonrası ağırlık girilince hesaplanır.")
        }
        Spacer8()
    }
}

// ---------------------------------------------------------------------------------------------
// Kontrol listesi
// ---------------------------------------------------------------------------------------------

@Composable
private fun ChecklistRow(item: ChecklistItem, onSave: (ChecklistItem) -> Unit) {
    var local by remember(item.id) { mutableStateOf(item) }
    var showComment by remember(item.id) { mutableStateOf(item.comment.isNotBlank()) }
    val tint: Color? = when (local.status) {
        1 -> Color(0xFFE8F5E9)
        2 -> WarnTint
        3 -> Color(0xFFEEEEEE)
        else -> null
    }
    CardBox(tint = tint) {
        Text(local.text, fontSize = 14.sp)
        LazyRow {
            items(listOf(1, 2, 3, 0)) { s ->
                Chip(Stages.statusLabel(s), local.status == s) {
                    val ns = if (local.status == s) 0 else s
                    local = local.copy(status = ns)
                    onSave(local)
                }
            }
        }
        if (showComment) {
            TextEdit("Açıklama", local.comment, { t ->
                local = local.copy(comment = t)
                onSave(local)
            }, stateKey = item.id)
        } else {
            TextButton(onClick = { showComment = true }) { Text("Açıklama ekle") }
        }
    }
}

@Composable
private fun TabChecklist(c: Ctx, items: List<ChecklistItem>?, missingCritical: List<String>) {
    Page {
        if (items == null) {
            Text("Yükleniyor…")
            return@Page
        }
        val done = items.count { it.status == 1 }
        val handled = items.count { it.status != 0 }
        CalcRow("İlerleme", "$done/${items.size} tamamlandı", "İşaretlenen (tamamlandı, atlandı veya ilgili değil): $handled/${items.size}")
        Hint("Tamamlanmayan maddeler deneyin tamamlanmasını engellemez. Bu liste yalnızca bu deneye aittir; diğer deneylerin işaretleri etkilenmez.")
        if (missingCritical.isNotEmpty()) {
            Warn("Şu önemli kayıtlar henüz boş: " + missingCritical.joinToString(", ") + ". Bunlar eksik kalırsa sonuçların güvenilirliği etkilenebilir; yine de deney geçersiz sayılmaz.")
        }
        for (stage in 1..8) {
            val list = items.filter { it.stage == stage }
            if (list.isEmpty()) continue
            val d = list.count { it.status == 1 }
            SectionTitle("Aşama " + stage + ": " + Stages.name(stage) + " (" + d + "/" + list.size + ")")
            for (it in list) {
                ChecklistRow(it) { n -> c.saver.save { c.repo.dao.updateChecklistItem(n) } }
            }
        }
        Spacer8()
    }
}

// ---------------------------------------------------------------------------------------------
// Notlar
// ---------------------------------------------------------------------------------------------

@Composable
private fun TabNotes(c: Ctx, notes: List<ExperimentNote>?) {
    val dao = c.repo.dao
    var stage by remember(c.id) { mutableStateOf(0) }
    var title by remember(c.id) { mutableStateOf("") }
    var tags by remember(c.id) { mutableStateOf("") }
    var body by remember(c.id) { mutableStateOf("") }
    var formKey by remember(c.id) { mutableStateOf(0) }
    var q by remember(c.id) { mutableStateOf("") }
    var editing by remember { mutableStateOf<ExperimentNote?>(null) }
    var deleting by remember { mutableStateOf<ExperimentNote?>(null) }

    Page {
        SectionTitle("Yeni not")
        DropdownField("Aşama", Stages.names, stage, { stage = it })
        TextEdit("Başlık (isteğe bağlı)", title, { title = it }, stateKey = formKey)
        TextEdit("Not", body, { body = it }, minLines = 4, stateKey = formKey)
        TextEdit("Etiketler (virgülle ayır, isteğe bağlı)", tags, { tags = it }, stateKey = formKey)
        Button(
            enabled = body.isNotBlank(),
            onClick = {
                val n = ExperimentNote(experimentId = c.id, stage = stage, title = title, tags = tags, body = body, createdAt = System.currentTimeMillis())
                c.scope.launch { dao.insertNote(n) }
                title = ""; tags = ""; body = ""; formKey += 1
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Notu kaydet") }
        Hint("Notların tarih ve saatle, yazdığın gibi saklanır. Uygulama notlarını değiştirmez; sistem önerileri ayrı bir sekmededir.")

        SectionTitle("Notlar" + (if (notes != null) " (" + notes.size + ")" else ""))
        if (notes == null) {
            Text("Yükleniyor…")
        } else {
            if (notes.isEmpty()) Hint("Bu deney için henüz not yok.")
            if (notes.isNotEmpty()) TextEdit("Notlarda ara", "", { q = it })
            val shown = notes.filter {
                q.isBlank() || it.title.contains(q, true) || it.body.contains(q, true) || it.tags.contains(q, true)
            }
            for (n in shown) {
                CardBox {
                    Text(Stages.name(n.stage) + " · " + Util.dateTime(n.createdAt), fontSize = 12.sp, color = Brown)
                    if (n.updatedAt != null) Text("Güncellendi: " + Util.dateTime(n.updatedAt), fontSize = 11.sp, color = Color(0xFF6B6B6B))
                    if (n.title.isNotBlank()) Text(n.title, fontWeight = FontWeight.Bold)
                    Text(n.body)
                    if (n.tags.isNotBlank()) Text("Etiket: " + n.tags, fontSize = 12.sp)
                    Row {
                        TextButton(onClick = { editing = n }) { Text("Düzenle") }
                        TextButton(onClick = { deleting = n }) { Text("Sil") }
                    }
                }
            }
        }
        Spacer8()
    }

    val ed = editing
    if (ed != null) {
        var t by remember(ed.id) { mutableStateOf(ed.title) }
        var b by remember(ed.id) { mutableStateOf(ed.body) }
        var tg by remember(ed.id) { mutableStateOf(ed.tags) }
        var st by remember(ed.id) { mutableStateOf(ed.stage) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Notu düzenle") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    DropdownField("Aşama", Stages.names, st, { st = it })
                    TextEdit("Başlık", t, { t = it })
                    TextEdit("Not", b, { b = it }, minLines = 4)
                    TextEdit("Etiketler", tg, { tg = it })
                }
            },
            confirmButton = {
                TextButton(enabled = b.isNotBlank(), onClick = {
                    val n = ed.copy(title = t, body = b, tags = tg, stage = st, updatedAt = System.currentTimeMillis())
                    editing = null
                    c.scope.launch { dao.updateNote(n) }
                }) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Vazgeç") } }
        )
    }
    val del = deleting
    if (del != null) {
        ConfirmDialog(
            title = "Not silinsin mi?",
            text = "Bu not kalıcı olarak silinir.",
            confirmLabel = "Sil",
            onConfirm = {
                deleting = null
                c.scope.launch { dao.deleteNote(del) }
            },
            onDismiss = { deleting = null }
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Değerlendirme
// ---------------------------------------------------------------------------------------------

@Composable
private fun TabEvaluation(c: Ctx, evLoaded: Boolean, saved: SensoryEvaluation?, msReady: Boolean, onChange: (SensoryEvaluation) -> Unit) {
    Page {
        if (!evLoaded || !msReady) {
            Text("Yükleniyor…")
            return@Page
        }
        val ev = saved ?: SensoryEvaluation(experimentId = c.id, updatedAt = 0L)
        if (c.exp.successCriteria.isNotBlank()) {
            CardBox(tint = BrownLight) {
                Text("Başarı ölçütün", fontSize = 12.sp)
                Text(c.exp.successCriteria)
            }
        }
        Hint("Puanlar isteğe bağlıdır. Ölçmediğin ya da değerlendirmediğin özelliği boş bırak; seçili puana tekrar basarsan puan silinir.")

        SectionTitle("Puanlar (1–10)")
        for (cr in Criteria.all) {
            ScoreRow(cr.label, ev.score(cr.key)) { v -> onChange(ev.withScore(cr.key, v)) }
        }

        MeasurementEditor(
            groups = listOf(Fields.G_RESULT),
            values = c.msMap,
            keyPrefix = c.id,
            showMeasuredToggle = true,
            defaultMeasured = true
        ) { key, text, numeric, measured ->
            c.saver.save { c.repo.setMeasurement(c.id, key, text, numeric, measured) }
        }

        SectionTitle("Başarı ölçütü")
        Row {
            Chip("Belirsiz", ev.goalMet == null) { onChange(ev.copy(goalMet = null)) }
            Chip("Ulaşıldı", ev.goalMet == 1) { onChange(ev.copy(goalMet = 1)) }
            Chip("Ulaşılamadı", ev.goalMet == 0) { onChange(ev.copy(goalMet = 0)) }
        }
        TextEdit("Serbest değerlendirme", ev.freeText, { onChange(ev.copy(freeText = it)) }, minLines = 3, stateKey = c.id)
        TextEdit("Başarısızlık veya beklenmeyen sonuçlar", ev.failures, { onChange(ev.copy(failures = it)) }, minLines = 3, stateKey = c.id)
        TextEdit("Bir sonraki deney için önerin", ev.nextSuggestion, { onChange(ev.copy(nextSuggestion = it)) }, minLines = 3, stateKey = c.id)

        SectionTitle("Sonuç özeti")
        val scored = Criteria.all.filter { ev.score(it.key) != null }
        val avgScore = avg(scored.mapNotNull { ev.score(it.key) })
        CalcRow("Puanlanan ölçüt", scored.size.toString() + "/" + Criteria.all.size, if (avgScore != null) "Puanların ortalaması: " + Fmt.num(avgScore, 1) + " — ortalama tek başına yeterli değildir, aşağıdaki ayrıntıya bakın." else "Henüz puan girilmedi.")

        val pe = c.prev?.eval
        if (c.prev != null && pe != null) {
            val up = ArrayList<String>()
            val down = ArrayList<String>()
            val same = ArrayList<String>()
            for (cr in Criteria.all) {
                val a = pe.score(cr.key)
                val b = ev.score(cr.key)
                if (a != null && b != null) {
                    val t = cr.label + " " + a + " → " + b
                    if (b > a) up.add(t) else if (b < a) down.add(t) else same.add(t)
                }
            }
            Text("Önceki denemeye (" + c.prev.exp.code + ") göre", fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 6.dp))
            CalcRow("İyileşen", if (up.isEmpty()) "—" else up.size.toString() + " ölçüt", if (up.isEmpty()) null else up.joinToString("; "))
            CalcRow("Gerileyen", if (down.isEmpty()) "—" else down.size.toString() + " ölçüt", if (down.isEmpty()) null else down.joinToString("; "))
            CalcRow("Değişmeyen", if (same.isEmpty()) "—" else same.size.toString() + " ölçüt", if (same.isEmpty()) null else same.joinToString("; "))
            if (up.isNotEmpty() && down.isNotEmpty()) {
                Warn("Sonuçlar çelişkili: bazı ölçütler iyileşmiş, bazıları gerilemiş. Genel puan bunu gizleyebilir; ikisini ayrı değerlendir.")
            }
        } else {
            Hint("Önceki deneme seçilmediği veya değerlendirilmediği için iyileşme/gerileme karşılaştırması yapılamadı.")
        }

        val missing = ArrayList<String>()
        for (cr in Criteria.all) if (ev.score(cr.key) == null) missing.add(cr.label + " puanı")
        for (f in Fields.all.filter { it.group == Fields.G_RESULT }) if (!c.snap.hasValue(f.key)) missing.add(f.label)
        if (missing.isNotEmpty()) {
            Warn("Eksik: " + missing.joinToString(", ") + ". Eksik olması deneyi geçersiz yapmaz; yalnızca yorum yaparken dikkatli olmak gerekir.")
        }
        Spacer8()
    }
}

// ---------------------------------------------------------------------------------------------
// Öneriler
// ---------------------------------------------------------------------------------------------

private fun buildDraft(c: Ctx, advices: List<Advice>, recs: List<ExperimentRecommendation>): ExpDraft {
    val e = c.exp
    val ev = c.snap.eval
    val byKey = recs.associateBy { it.ruleKey }
    val meta = setOf("yetersiz_veri", "eksik_kayit", "hamur_sicakligi_yok", "cok_degisken")
    val accepted = advices.filter { (byKey[it.key]?.status ?: 0) == 1 || (byKey[it.key]?.status ?: 0) == 3 }
    val problemParts = ArrayList<String>()
    for (a in accepted) if (a.key !in meta) problemParts.add(a.observation)
    if (problemParts.isEmpty() && e.problem.isNotBlank()) problemParts.add(e.problem)
    if (ev != null && ev.failures.isNotBlank()) problemParts.add(ev.failures)
    val plans = ArrayList<String>()
    val vars = ArrayList<ExperimentVariable>()
    for (a in accepted) {
        val r = byKey[a.key]
        if (r != null && r.status == 3 && r.userPlan.isNotBlank()) {
            plans.add(r.userPlan)
            vars.add(ExperimentVariable(experimentId = 0, label = "Planlanan değişiklik", newValue = r.userPlan))
        } else if (a.key !in meta) {
            vars.add(ExperimentVariable(experimentId = 0, label = a.title, newValue = a.nextStep))
        }
    }
    val hypothesis = when {
        plans.isNotEmpty() -> plans.joinToString("\n")
        ev != null && ev.nextSuggestion.isNotBlank() -> ev.nextSuggestion
        else -> ""
    }
    return ExpDraft(
        name = "Sonraki deneme (" + e.code + " sonrası)",
        purpose = "Önceki denemeye (" + e.code + ") göre küçük ve kontrollü bir iyileştirme denemesi. Temel formülü gereksiz yere değiştirme.",
        problem = problemParts.joinToString("\n"),
        hypothesis = hypothesis,
        expected = "",
        successCriteria = e.successCriteria.ifBlank { "Genel kalite, ertesi gün yumuşaklık, yükseklik ve maliyetin önceki denemeyle karşılaştırılması" },
        versionId = e.versionId,
        previousId = e.id,
        variables = vars,
        planned = true
    )
}

@Composable
private fun TabAdvice(c: Ctx, recs: List<ExperimentRecommendation>?, evLoaded: Boolean) {
    Page {
        if (recs == null || !evLoaded) {
            Text("Yükleniyor…")
            return@Page
        }
        Hint("Bu öneriler yalnızca bu denemede kaydettiğin verilere dayanan basit kurallardır; kesin neden belirtmez. Kabul edebilir, reddedebilir veya kendi planını yazabilirsin. Uygulama senin onayın olmadan reçeteyi değiştirmez ve deney silmez.")
        val advices = Advisor.advise(c.snap, c.prev, c.changes)
        val byKey = recs.associateBy { it.ruleKey }
        for (a in advices) {
            val r = byKey[a.key]
            val st = r?.status ?: 0
            CardBox(tint = if (a.key == "yetersiz_veri") WarnTint else null) {
                Text(a.title, fontWeight = FontWeight.Bold, color = Brown)
                Text("Gözlem", fontSize = 11.sp, color = Color(0xFF6B6B6B), modifier = Modifier.padding(top = 4.dp))
                Text(a.observation, fontSize = 14.sp)
                if (a.explanations.isNotEmpty()) {
                    Text("Olası açıklamalar", fontSize = 11.sp, color = Color(0xFF6B6B6B), modifier = Modifier.padding(top = 4.dp))
                    for (x in a.explanations) Text("• $x", fontSize = 14.sp)
                }
                if (a.support.isNotEmpty()) {
                    Text("Destekleyen veriler", fontSize = 11.sp, color = Color(0xFF6B6B6B), modifier = Modifier.padding(top = 4.dp))
                    for (x in a.support) Text("• $x", fontSize = 14.sp)
                }
                if (a.missing.isNotEmpty()) {
                    Text("Eksik bilgi", fontSize = 11.sp, color = Color(0xFF6B6B6B), modifier = Modifier.padding(top = 4.dp))
                    for (x in a.missing) Text("• $x", fontSize = 14.sp)
                }
                Text("Önerilen sonraki adım", fontSize = 11.sp, color = Color(0xFF6B6B6B), modifier = Modifier.padding(top = 4.dp))
                Text(a.nextStep, fontSize = 14.sp)
                Text("Sonucun nasıl ölçüleceği", fontSize = 11.sp, color = Color(0xFF6B6B6B), modifier = Modifier.padding(top = 4.dp))
                Text(a.howToMeasure, fontSize = 14.sp)
                Row(Modifier.padding(top = 6.dp)) {
                    Chip("Kabul", st == 1) { c.saver.save { c.repo.decideRecommendation(c.id, a.key, if (st == 1) 0 else 1, r?.userPlan ?: "") } }
                    Chip("Reddet", st == 2) { c.saver.save { c.repo.decideRecommendation(c.id, a.key, if (st == 2) 0 else 2, r?.userPlan ?: "") } }
                    Chip("Kendi planım", st == 3) { c.saver.save { c.repo.decideRecommendation(c.id, a.key, if (st == 3) 0 else 3, r?.userPlan ?: "") } }
                }
                if (st == 3) {
                    TextEdit("Kendi planın", r?.userPlan ?: "", { t ->
                        c.saver.save { c.repo.decideRecommendation(c.id, a.key, 3, t) }
                    }, minLines = 2, stateKey = a.key)
                }
                if (a.key == "basarili" && c.exp.versionId != null) {
                    OutlinedButton(
                        onClick = {
                            val vId = c.exp.versionId!!
                            c.scope.launch {
                                val nid = c.repo.newVersionFrom(vId, null, null, "Başarılı deneme " + c.exp.code + " sonrası kaydedilen sürüm")
                                if (nid != null) c.nav.push(Screen.VersionEdit(nid))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Bu reçeteyi yeni sürüm olarak kaydet") }
                }
            }
        }

        SectionTitle("Sonraki deneyi planla")
        Hint("Kabul ettiğin veya kendi planını yazdığın öneriler yeni deney taslağına taşınır. Taslak sen kaydedene kadar oluşmaz.")
        Button(
            onClick = { c.nav.push(Screen.NewExperiment(buildDraft(c, advices, recs))) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Sonraki deney taslağını oluştur") }
        Spacer8()
    }
}
