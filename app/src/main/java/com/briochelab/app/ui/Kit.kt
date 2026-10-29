@file:OptIn(ExperimentalMaterial3Api::class)

package com.briochelab.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.briochelab.app.calc.CIng
import com.briochelab.app.calc.Calc
import com.briochelab.app.calc.CostResult
import com.briochelab.app.calc.ExtraCosts
import com.briochelab.app.calc.Fmt
import com.briochelab.app.calc.PieceResult
import com.briochelab.app.calc.WaterEstimate
import com.briochelab.app.data.CostRecord
import com.briochelab.app.data.Experiment
import com.briochelab.app.data.ExperimentPhoto
import com.briochelab.app.data.Fields
import com.briochelab.app.data.Ingredient
import com.briochelab.app.data.Logic
import com.briochelab.app.data.ProcessMeasurement
import com.briochelab.app.data.RecipeVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** Room her cagrida yeni bir Flow urettigi icin akis bir kez hatirlanir. */
@Composable
fun <T> rememberFlow(initial: T, key: Any? = null, make: () -> Flow<T>): State<T> {
    val f = remember(key) { make() }
    return f.collectAsState(initial = initial)
}

/** Kaydirilabilir sayfa govdesi. */
@Composable
fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        content = content
    )
}

/**
 * Yazdikca kaydetme icin: kayitlar sirayla yapilir, ekran kapansa bile yarida kesilmez.
 */
class AutoSaver(private val scope: CoroutineScope) {
    private val mutex = Mutex()
    fun save(block: suspend () -> Unit) {
        scope.launch(start = CoroutineStart.ATOMIC) {
            withContext(NonCancellable) {
                mutex.withLock {
                    try {
                        block()
                    } catch (e: Exception) {
                        // tek bir kayit hatasi uygulamayi kapatmasin
                    }
                }
            }
        }
    }
}

@Composable
fun rememberAutoSaver(): AutoSaver {
    val scope = rememberCoroutineScope()
    return remember { AutoSaver(scope) }
}

/** Bir recete surumunun otomatik hesaplanan ozeti. */
data class VSummary(
    val flour: Double,
    val total: Double,
    val dough: Double,
    val freeWater: Double?,
    val water: WaterEstimate,
    val theoretical: Double?,
    val pieces: PieceResult,
    val wholePieces: Int?,
    val cost: CostResult
)

fun summarize(v: RecipeVersion?, ings: List<Ingredient>, cost: CostRecord?, defaults: Map<String, Double?>): VSummary {
    val c: List<CIng> = ings.map { Logic.toCIng(it) }
    val flour = Calc.flourTotal(c)
    val total = Calc.totalWeight(c)
    val dough = if (total > 0.0) total else (v?.expectedDoughG ?: 0.0)
    val piece = v?.targetPieceG
    val theo = Calc.theoreticalPieces(dough, piece)
    val pr = Calc.piecesWithLoss(dough, v?.lossPct, piece)
    val whole = Calc.wholePieces(pr.value)
    val extra = ExtraCosts(cost?.packagingPerPiece, cost?.energyTotal, cost?.laborTotal, cost?.otherTotal, cost?.salePrice)
    val cr = Calc.cost(c, whole?.toDouble(), extra)
    return VSummary(
        flour, total, dough, Calc.freeWaterPercent(c), Calc.estimatedWater(c, defaults),
        theo, pr, whole, cr
    )
}

fun expTitle(e: Experiment): String = e.code + " · " + e.name

@Composable
fun StatCard(title: String, value: String, note: String? = null, onClick: (() -> Unit)? = null) {
    val body: @Composable () -> Unit = {
        Text(title, fontSize = 12.sp, color = Color(0xFF6B6B6B))
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Brown)
        if (note != null) Text(note, fontSize = 12.sp, color = Color(0xFF555555))
    }
    if (onClick != null) ClickableCard(onClick = onClick) { body() } else CardBox { body() }
}

/** Fotograf ekleme (kamera / galeri), slot bazli listeleme, buyutme ve silme. */
@Composable
fun PhotoSection(expId: Long?, versionId: Long?, photos: List<ExperimentPhoto>, slots: List<String>) {
    val repo = LocalRepo.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var slotIdx by rememberSaveable { mutableStateOf(0) }
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }
    var viewing by remember { mutableStateOf<ExperimentPhoto?>(null) }
    var deleting by remember { mutableStateOf<ExperimentPhoto?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    val slot = slots.getOrElse(slotIdx) { slots.first() }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                if (!repo.importPhoto(uri, expId, versionId, slot)) msg = "Fotoğraf eklenemedi."
            }
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val p = pendingPath
        pendingPath = null
        if (p != null) {
            val f = File(p)
            if (ok) {
                scope.launch { repo.registerPhoto(f, expId, versionId, slot) }
            } else {
                f.delete()
            }
        }
    }

    DropdownField("Fotoğraf türü / aşama", slots, slotIdx, { slotIdx = it })
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = {
                try {
                    val f = repo.newPhotoFile()
                    pendingPath = f.absolutePath
                    val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
                    camera.launch(uri)
                } catch (e: Exception) {
                    pendingPath = null
                    msg = "Kamera açılamadı. Galeriden seçmeyi deneyin."
                }
            },
            modifier = Modifier.weight(1f)
        ) { Text("Kamera") }
        OutlinedButton(
            onClick = {
                try {
                    pick.launch("image/*")
                } catch (e: Exception) {
                    msg = "Galeri açılamadı."
                }
            },
            modifier = Modifier.weight(1f)
        ) { Text("Galeriden seç") }
    }
    Hint("Denemeleri karşılaştırmak için fotoğrafları mümkünse aynı açıdan, aynı ışıkta ve aynı mesafeden çekin; aksi halde görsel karşılaştırma yanıltıcı olabilir. Uygulama fotoğraftan çap, hacim veya yumuşaklık ölçmez.")

    if (photos.isEmpty()) {
        Hint("Henüz fotoğraf eklenmedi.")
    }
    val known = slots.toSet()
    for (s in slots) {
        val list = photos.filter { it.slot == s }
        if (list.isEmpty()) continue
        Text(s + " (" + list.size + ")", fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        LazyRow {
            items(list, key = { it.id }) { p ->
                PhotoThumb(repo.photoFile(p.fileName), 96, Modifier.padding(end = 6.dp).clickable { viewing = p })
            }
        }
    }
    val others = photos.filter { it.slot !in known }
    if (others.isNotEmpty()) {
        Text("Diğer (" + others.size + ")", fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        LazyRow {
            items(others, key = { it.id }) { p ->
                PhotoThumb(repo.photoFile(p.fileName), 96, Modifier.padding(end = 6.dp).clickable { viewing = p })
            }
        }
    }

    val v = viewing
    if (v != null) {
        AlertDialog(
            onDismissRequest = { viewing = null },
            confirmButton = { TextButton(onClick = { viewing = null }) { Text("Kapat") } },
            dismissButton = { TextButton(onClick = { deleting = v; viewing = null }) { Text("Sil") } },
            title = { Text(v.slot.ifBlank { "Fotoğraf" }) },
            text = {
                Column {
                    PhotoThumb(repo.photoFile(v.fileName), 280)
                    Text(com.briochelab.app.data.Util.dateTime(v.createdAt), fontSize = 12.sp)
                }
            }
        )
    }
    val d = deleting
    if (d != null) {
        ConfirmDialog(
            title = "Fotoğraf silinsin mi?",
            text = "Bu fotoğraf kalıcı olarak silinir.",
            confirmLabel = "Sil",
            onConfirm = {
                deleting = null
                scope.launch {
                    repo.dao.deletePhoto(d)
                    repo.photoFile(d.fileName).delete()
                }
            },
            onDismiss = { deleting = null }
        )
    }
    val m = msg
    if (m != null) MessageDialog("Fotoğraf", m) { msg = null }
}

/**
 * Surec / kosul olcumleri formu. Bos birakilan alan kaydedilmez (sifir sayilmaz).
 * showMeasuredToggle true ise kayitli degerin "olculdu" mu "tahmin / plan" mi oldugu secilebilir.
 */
@Composable
fun MeasurementEditor(
    groups: List<String>,
    values: Map<String, ProcessMeasurement>,
    keyPrefix: Any,
    showMeasuredToggle: Boolean,
    defaultMeasured: Boolean,
    onChange: (key: String, text: String, numeric: Boolean, measured: Boolean) -> Unit
) {
    for (g in groups) {
        val fs = Fields.all.filter { it.group == g }
        if (fs.isEmpty()) continue
        SectionTitle(g)
        for (f in fs) {
            val m = values[f.key]
            val measured = m?.measured ?: defaultMeasured
            val unit = f.unit
            if (f.numeric) {
                val max: Double? = if (f.key == "humidity" || f.key == "lossActual") 100.0 else null
                NumberEdit(
                    label = f.label,
                    initial = m?.numValue,
                    onValue = { onChange(f.key, Fmt.edit(it), true, measured) },
                    suffix = unit,
                    max = max,
                    stateKey = Pair(keyPrefix, f.key)
                )
            } else {
                TextEdit(
                    label = f.label,
                    initial = m?.textValue ?: "",
                    onValue = { onChange(f.key, it, false, measured) },
                    stateKey = Pair(keyPrefix, f.key)
                )
            }
            if (showMeasuredToggle && m != null) {
                val has = m.numValue != null || m.textValue.isNotBlank()
                if (has) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Chip("Ölçüldü", measured) {
                            onChange(f.key, if (f.numeric) Fmt.edit(m.numValue) else m.textValue, f.numeric, true)
                        }
                        Chip("Tahmin / plan", !measured) {
                            onChange(f.key, if (f.numeric) Fmt.edit(m.numValue) else m.textValue, f.numeric, false)
                        }
                    }
                }
            }
        }
    }
}

fun scoreText(v: Int?): String = if (v == null) "—" else "$v/10"

fun avg(l: List<Int>): Double? = if (l.isEmpty()) null else l.sum().toDouble() / l.size

@Composable
fun BodyText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
}
