package com.briochelab.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.briochelab.app.calc.Calc
import com.briochelab.app.calc.Cat
import com.briochelab.app.calc.Fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object Util {
    private val tr = Locale("tr", "TR")
    fun date(ms: Long): String = SimpleDateFormat("dd.MM.yyyy", tr).format(Date(ms))
    fun dateTime(ms: Long): String = SimpleDateFormat("dd.MM.yyyy HH:mm", tr).format(Date(ms))
    fun statusLabel(s: String): String = when (s) {
        "PLANNED" -> "Planlandı"
        "COMPLETED" -> "Tamamlandı"
        else -> "Aktif"
    }
}

data class RestoreResult(val added: Int, val skipped: Int)

class Repo(private val ctx: Context, val db: AppDb) {
    val dao: BDao = db.dao()

    val photosDir: File
        get() {
            val d = File(ctx.filesDir, "photos")
            if (!d.exists()) d.mkdirs()
            return d
        }

    suspend fun ensureDefaults() {
        val existing = dao.propertiesOnce().map { it.category }.toSet()
        for (c in Cat.all) {
            if (c !in existing) dao.upsertProperty(IngredientProperty(c, Cat.defaultWater[c]))
        }
    }

    // ------------------------------------------------------------------ Receteler

    suspend fun createRecipe(name: String, description: String, goal: String): Long = db.withTransaction {
        val count = dao.dumpRecipes().size
        val now = System.currentTimeMillis()
        val rid = dao.insertRecipe(
            Recipe(code = "R-" + (count + 1).toString().padStart(3, '0'), name = name.ifBlank { "Yeni reçete" },
                description = description, goal = goal, createdAt = now)
        )
        val vid = dao.insertVersion(RecipeVersion(recipeId = rid, versionNo = 1, createdAt = now))
        val starters = listOf(
            "Un" to Cat.FLOUR, "Su" to Cat.WATER, "Süt" to Cat.MILK, "Yumurta" to Cat.EGG,
            "Tereyağı" to Cat.BUTTER, "Şeker" to Cat.SUGAR, "Tuz" to Cat.SALT, "Maya" to Cat.YEAST
        )
        var order = 0
        for ((n, c) in starters) {
            dao.insertIngredient(Ingredient(versionId = vid, name = n, category = c, grams = 0.0, sortOrder = order++))
        }
        rid
    }

    suspend fun versionLocked(versionId: Long): Boolean = dao.experimentCountForVersionOnce(versionId) > 0

    /** Mevcut surumu yeni surum olarak kopyalar. Eski surum degismez. Olcekleme istege baglidir. */
    suspend fun newVersionFrom(versionId: Long, newFlour: Double?, targetDough: Double?, note: String): Long? = db.withTransaction {
        val src = dao.getVersion(versionId) ?: return@withTransaction null
        val ings = dao.ingredientsOfOnce(versionId)
        val all = dao.versionsOfOnce(src.recipeId)
        val nextNo = (all.maxOfOrNull { it.versionNo } ?: 0) + 1
        val cings = ings.map { Logic.toCIng(it) }
        val scaled: List<Double>? = when {
            newFlour != null -> Calc.scaleToFlour(cings, newFlour)
            targetDough != null -> Calc.scaleToDough(cings, targetDough)
            else -> null
        }
        val newDough = if (targetDough != null) targetDough else if (scaled != null) Calc.totalWeight(
            cings.mapIndexed { idx, c -> c.copy(grams = scaled[idx]) }
        ) else src.expectedDoughG
        val vid = dao.insertVersion(
            src.copy(id = 0, versionNo = nextNo, parentVersionId = src.id, createdAt = System.currentTimeMillis(),
                note = note, expectedDoughG = newDough)
        )
        ings.forEachIndexed { idx, i ->
            dao.insertIngredient(i.copy(id = 0, versionId = vid, grams = scaled?.get(idx) ?: i.grams))
        }
        dao.costOnce(versionId)?.let { dao.upsertCost(it.copy(id = 0, versionId = vid)) }
        vid
    }

    suspend fun deleteRecipeIfUnused(recipeId: Long): Boolean {
        val r = dao.getRecipe(recipeId) ?: return false
        for (v in dao.versionsOfOnce(recipeId)) {
            if (dao.experimentCountForVersionOnce(v.id) > 0) return false
        }
        dao.deleteRecipe(r)
        return true
    }

    // ------------------------------------------------------------------ Deneyler

    suspend fun nextExperimentCode(): String {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val prefix = "BL-$year-"
        var max = 0
        for (c in dao.codesWithPrefix(prefix)) {
            val n = c.removePrefix(prefix).toIntOrNull()
            if (n != null && n > max) max = n
        }
        return prefix + (max + 1).toString().padStart(3, '0')
    }

    suspend fun createExperiment(e: Experiment, variables: List<ExperimentVariable>, prefill: Map<String, ProcessMeasurement>): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val id = dao.insertExperiment(e.copy(id = 0, code = nextExperimentCode(), createdAt = now))
        val items = ArrayList<ChecklistItem>()
        for ((stage, texts) in Stages.checklist) {
            texts.forEachIndexed { idx, t -> items.add(ChecklistItem(experimentId = id, stage = stage, text = t, sortOrder = idx)) }
        }
        dao.insertChecklistItems(items)
        for (v in variables) dao.insertVariable(v.copy(id = 0, experimentId = id))
        for (m in prefill.values) dao.upsertMeasurement(m.copy(id = 0, experimentId = id))
        id
    }

    suspend fun setMeasurement(expId: Long, key: String, text: String, numeric: Boolean, measured: Boolean) {
        if (text.isBlank()) {
            dao.deleteMeasurement(expId, key)
            return
        }
        if (numeric) {
            val v = Fmt.parse(text)
            if (v == null || v < 0.0) return
            dao.upsertMeasurement(ProcessMeasurement(experimentId = expId, fieldKey = key, numValue = v, measured = measured))
        } else {
            dao.upsertMeasurement(ProcessMeasurement(experimentId = expId, fieldKey = key, textValue = text.trim(), measured = measured))
        }
    }

    suspend fun saveEvaluation(e: SensoryEvaluation) {
        val existing = dao.dumpEvaluations().firstOrNull { it.experimentId == e.experimentId }
        dao.upsertEvaluation(e.copy(id = existing?.id ?: 0, updatedAt = System.currentTimeMillis()))
    }

    suspend fun decideRecommendation(expId: Long, ruleKey: String, status: Int, plan: String) {
        val existing = dao.dumpRecs().firstOrNull { it.experimentId == expId && it.ruleKey == ruleKey }
        dao.upsertRec(
            ExperimentRecommendation(id = existing?.id ?: 0, experimentId = expId, ruleKey = ruleKey,
                status = status, userPlan = plan, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun snapshot(expId: Long): ExpSnapshot? {
        val e = dao.getExperiment(expId) ?: return null
        val v = e.versionId?.let { dao.getVersion(it) }
        val ings = e.versionId?.let { dao.ingredientsOfOnce(it) } ?: emptyList()
        val ms = dao.measurementsOfOnce(expId).associateBy { it.fieldKey }
        val ev = dao.dumpEvaluations().firstOrNull { it.experimentId == expId }
        val vars = dao.dumpVariables().filter { it.experimentId == expId }
        val cl = dao.dumpChecklist().filter { it.experimentId == expId }
        return ExpSnapshot(e, v, ings, ms, ev, vars, cl)
    }

    // ------------------------------------------------------------------ Fotograflar

    fun newPhotoFile(): File = File(photosDir, "p_" + System.currentTimeMillis() + ".jpg")

    fun photoFile(name: String): File = File(photosDir, File(name).name)

    suspend fun importPhoto(uri: Uri, expId: Long?, versionId: Long?, slot: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val f = newPhotoFile()
            val input = ctx.contentResolver.openInputStream(uri) ?: return@withContext false
            input.use { ins -> f.outputStream().use { out -> ins.copyTo(out) } }
            dao.insertPhoto(ExperimentPhoto(experimentId = expId, versionId = versionId, slot = slot, fileName = f.name, createdAt = System.currentTimeMillis()))
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun registerPhoto(file: File, expId: Long?, versionId: Long?, slot: String) {
        dao.insertPhoto(ExperimentPhoto(experimentId = expId, versionId = versionId, slot = slot, fileName = file.name, createdAt = System.currentTimeMillis()))
    }

    suspend fun zipPhotos(out: OutputStream): Int = withContext(Dispatchers.IO) {
        var n = 0
        ZipOutputStream(out).use { zip ->
            for (f in photosDir.listFiles() ?: emptyArray()) {
                if (!f.isFile) continue
                zip.putNextEntry(ZipEntry("photos/" + f.name))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                n++
            }
        }
        n
    }

    suspend fun unzipPhotos(input: InputStream): Int = withContext(Dispatchers.IO) {
        var n = 0
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = File(entry.name).name
                    if (name.isNotEmpty()) {
                        val target = File(photosDir, name)
                        if (!target.exists()) {
                            target.outputStream().use { zip.copyTo(it) }
                            n++
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        n
    }

    // ------------------------------------------------------------------ Yedekleme

    suspend fun dumpAll(): BackupData = BackupData(
        dao.dumpRecipes(), dao.dumpVersions(), dao.dumpIngredients(), dao.dumpProperties(), dao.dumpCosts(),
        dao.dumpExperiments(), dao.dumpVariables(), dao.dumpMeasurements(), dao.dumpChecklist(), dao.dumpNotes(),
        dao.dumpPhotos(), dao.dumpEvaluations(), dao.dumpRecs()
    )

    suspend fun exportJson(kind: String = "JSON"): String {
        val d = dumpAll()
        val now = System.currentTimeMillis()
        dao.insertBackupMeta(BackupMetadata(createdAt = now, kind = kind, appVersion = APP_VERSION, schemaVersion = BackupCodec.SCHEMA, itemCount = d.itemCount()))
        return BackupCodec.encode(d, now, APP_VERSION)
    }

    /** Geri yuklemeden once mevcut verinin uygulama icinde otomatik kopyasini yazar. */
    suspend fun writeSafetyBackup(): File = withContext(Dispatchers.IO) {
        val f = File(ctx.filesDir, "geri_yukleme_oncesi_yedek.json")
        f.writeText(BackupCodec.encode(dumpAll(), System.currentTimeMillis(), APP_VERSION), Charsets.UTF_8)
        f
    }

    /** Tum mevcut verinin yerine yedegi koyar. */
    suspend fun restoreReplace(d: BackupData): RestoreResult {
        writeSafetyBackup()
        db.withTransaction {
            dao.clearRecipes()
            dao.clearExperiments()
            dao.clearPhotos()
            dao.clearProperties()
            dao.putRecipes(d.recipes)
            dao.putVersions(d.versions)
            dao.putIngredients(d.ingredients)
            dao.putProperties(d.properties)
            dao.putCosts(d.costs)
            dao.putExperiments(d.experiments)
            dao.putVariables(d.variables)
            dao.putMeasurements(d.measurements)
            dao.putChecklist(d.checklist)
            dao.putNotes(d.notes)
            dao.putPhotos(d.photos)
            dao.putEvaluations(d.evaluations)
            dao.putRecs(d.recs)
        }
        ensureDefaults()
        return RestoreResult(d.itemCount(), 0)
    }

    /** Mevcut verilere ekler. Ayni kodlu recete/deney zaten varsa atlanir (yinelenen kayit olusmaz). */
    suspend fun restoreMerge(d: BackupData): RestoreResult {
        writeSafetyBackup()
        var added = 0
        var skipped = 0
        db.withTransaction {
            val existingRecipeCodes = dao.dumpRecipes().map { it.code }.toSet()
            val existingExpCodes = dao.dumpExperiments().map { it.code }.toSet()
            val recipeMap = HashMap<Long, Long>()
            for (r in d.recipes) {
                if (r.code in existingRecipeCodes) { skipped++; continue }
                recipeMap[r.id] = dao.insertRecipe(r.copy(id = 0)); added++
            }
            val versionMap = HashMap<Long, Long>()
            for (v in d.versions.sortedBy { it.id }) {
                val rid = recipeMap[v.recipeId] ?: continue
                val parent = v.parentVersionId?.let { versionMap[it] }
                versionMap[v.id] = dao.insertVersion(v.copy(id = 0, recipeId = rid, parentVersionId = parent)); added++
            }
            for (i in d.ingredients) {
                val vid = versionMap[i.versionId] ?: continue
                dao.insertIngredient(i.copy(id = 0, versionId = vid)); added++
            }
            for (c in d.costs) {
                val vid = versionMap[c.versionId] ?: continue
                dao.upsertCost(c.copy(id = 0, versionId = vid)); added++
            }
            val expMap = HashMap<Long, Long>()
            for (e in d.experiments.sortedBy { it.id }) {
                if (e.code in existingExpCodes) { skipped++; continue }
                val vid = e.versionId?.let { versionMap[it] }
                val prev = e.previousExperimentId?.let { expMap[it] }
                expMap[e.id] = dao.insertExperiment(e.copy(id = 0, versionId = vid, previousExperimentId = prev)); added++
            }
            for (x in d.variables) { val id = expMap[x.experimentId] ?: continue; dao.insertVariable(x.copy(id = 0, experimentId = id)); added++ }
            for (x in d.measurements) { val id = expMap[x.experimentId] ?: continue; dao.upsertMeasurement(x.copy(id = 0, experimentId = id)); added++ }
            for (x in d.notes) { val id = expMap[x.experimentId] ?: continue; dao.insertNote(x.copy(id = 0, experimentId = id)); added++ }
            val items = ArrayList<ChecklistItem>()
            for (x in d.checklist) { val id = expMap[x.experimentId] ?: continue; items.add(x.copy(id = 0, experimentId = id)); added++ }
            dao.insertChecklistItems(items)
            for (x in d.evaluations) { val id = expMap[x.experimentId] ?: continue; dao.upsertEvaluation(x.copy(id = 0, experimentId = id)); added++ }
            for (x in d.recs) { val id = expMap[x.experimentId] ?: continue; dao.upsertRec(x.copy(id = 0, experimentId = id)); added++ }
            for (x in d.photos) {
                val eid = x.experimentId?.let { expMap[it] }
                val vid = x.versionId?.let { versionMap[it] }
                if (eid == null && vid == null) continue
                dao.insertPhoto(x.copy(id = 0, experimentId = eid, versionId = vid)); added++
            }
        }
        return RestoreResult(added, skipped)
    }

    suspend fun exportCsv(): String {
        val exps = dao.dumpExperiments().sortedBy { it.date }
        val versions = dao.dumpVersions().associateBy { it.id }
        val recipes = dao.dumpRecipes().associateBy { it.id }
        val ms = dao.dumpMeasurements().groupBy { it.experimentId }
        val evs = dao.dumpEvaluations().associateBy { it.experimentId }
        val sb = StringBuilder()
        sb.append('﻿')
        fun esc(s: String): String {
            val t = s.replace("\"", "\"\"").replace("\n", " ").replace("\r", " ")
            return "\"" + t + "\""
        }
        val header = ArrayList<String>()
        header.addAll(listOf("Kod", "Deney adı", "Tarih", "Durum", "Reçete", "Sürüm"))
        for (f in Fields.all) header.add(f.label + (if (f.unit.isNotEmpty()) " (" + f.unit + ")" else "") + " [ölçüm durumu]")
        header.add("Pişirme kaybı % (hesaplanan)")
        for (c in Criteria.all) header.add(c.label + " (1-10)")
        header.add("Başarı ölçütüne ulaşıldı")
        sb.append(header.joinToString(";") { esc(it) }).append("\r\n")
        for (e in exps) {
            val v = e.versionId?.let { versions[it] }
            val r = v?.let { recipes[it.recipeId] }
            val mm = (ms[e.id] ?: emptyList()).associateBy { it.fieldKey }
            val row = ArrayList<String>()
            row.add(e.code); row.add(e.name); row.add(Util.date(e.date)); row.add(Util.statusLabel(e.status))
            row.add(r?.name ?: ""); row.add(if (v != null) "v" + v.versionNo else "")
            for (f in Fields.all) {
                val m = mm[f.key]
                if (m == null) row.add("") else {
                    val value = if (f.numeric) Fmt.num(m.numValue, 3) else m.textValue
                    row.add(value + (if (m.measured) "" else " (tahmin)"))
                }
            }
            val wb = mm["weightBefore"]?.numValue
            val wa = mm["weightAfter"]?.numValue
            row.add(if (wb != null && wa != null && wb > 0.0) Fmt.num((wb - wa) / wb * 100.0, 2) else "")
            val ev = evs[e.id]
            for (c in Criteria.all) row.add(ev?.score(c.key)?.toString() ?: "")
            val goal = ev?.goalMet
            row.add(
                when (goal) {
                    1 -> "Evet"
                    0 -> "Hayır"
                    else -> ""
                }
            )
            sb.append(row.joinToString(";") { esc(it) }).append("\r\n")
        }
        return sb.toString()
    }

    companion object {
        const val APP_VERSION = "1.0.0"
    }
}
