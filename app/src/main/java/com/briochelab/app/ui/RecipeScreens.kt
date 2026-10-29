package com.briochelab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.briochelab.app.calc.CIng
import com.briochelab.app.calc.Calc
import com.briochelab.app.calc.Cat
import com.briochelab.app.calc.Fmt
import com.briochelab.app.data.CostRecord
import com.briochelab.app.data.ExperimentPhoto
import com.briochelab.app.data.Ingredient
import com.briochelab.app.data.IngredientProperty
import com.briochelab.app.data.Logic
import com.briochelab.app.data.Recipe
import com.briochelab.app.data.RecipeVersion
import com.briochelab.app.data.Util
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------------------------
// Reçeteler listesi
// ---------------------------------------------------------------------------------------------

@Composable
fun RecipesScreen() {
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val recipes by rememberFlow(emptyList<Recipe>()) { repo.dao.recipes() }
    val versions by rememberFlow(emptyList<RecipeVersion>()) { repo.dao.allVersions() }
    var q by remember { mutableStateOf("") }
    var showNew by remember { mutableStateOf(false) }

    Page {
        Button(onClick = { showNew = true }, modifier = Modifier.fillMaxWidth()) { Text("Yeni reçete oluştur") }
        Spacer8()
        TextEdit("Reçete ara (ad, kod veya açıklama)", "", { q = it })
        Spacer8()
        val shown = recipes.filter {
            q.isBlank() || it.name.contains(q, ignoreCase = true) || it.code.contains(q, ignoreCase = true) ||
                it.description.contains(q, ignoreCase = true)
        }
        if (recipes.isEmpty()) {
            CardBox(tint = BrownLight) {
                Text("Henüz reçete yok", fontWeight = FontWeight.Bold)
                BodyText("\"Yeni reçete oluştur\" ile başla. Un, su, süt, yumurta, tereyağı, şeker, tuz ve maya satırları hazır gelir; gramajları sen girersin.")
            }
        } else if (shown.isEmpty()) {
            Hint("Aramaya uyan reçete yok.")
        }
        for (r in shown) {
            val vs = versions.filter { it.recipeId == r.id }
            ClickableCard(onClick = { nav.push(Screen.RecipeDetail(r.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text(r.code, fontSize = 12.sp, color = Brown)
                }
                Text(
                    vs.size.toString() + " sürüm" + (if (vs.isNotEmpty()) " · son: v" + (vs.maxOf { it.versionNo }) else "") +
                        " · " + Util.date(r.createdAt),
                    fontSize = 12.sp
                )
                if (r.description.isNotBlank()) Text(r.description, fontSize = 13.sp, maxLines = 2)
            }
        }
    }

    if (showNew) {
        var n by remember { mutableStateOf("") }
        var d by remember { mutableStateOf("") }
        var g by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNew = false },
            title = { Text("Yeni reçete") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    TextEdit("Reçete adı", n, { n = it })
                    TextEdit("Açıklama", d, { d = it }, minLines = 2)
                    TextEdit("Hedef", g, { g = it }, minLines = 2)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showNew = false
                    scope.launch {
                        val id = repo.createRecipe(n, d, g)
                        nav.push(Screen.RecipeDetail(id))
                    }
                }) { Text("Oluştur") }
            },
            dismissButton = { TextButton(onClick = { showNew = false }) { Text("Vazgeç") } }
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Reçete ayrıntısı: bilgiler + sürümler
// ---------------------------------------------------------------------------------------------

@Composable
fun RecipeDetailScreen(recipeId: Long) {
    val repo = LocalRepo.current
    val dao = repo.dao
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val saver = rememberAutoSaver()
    var recipe by remember(recipeId) { mutableStateOf<Recipe?>(null) }
    var loaded by remember(recipeId) { mutableStateOf(false) }
    val versions by rememberFlow(emptyList<RecipeVersion>(), recipeId) { dao.versionsOf(recipeId) }
    val allIngs by rememberFlow(emptyList<Ingredient>()) { dao.allIngredients() }
    val props by rememberFlow(emptyList<IngredientProperty>()) { dao.properties() }
    var copyFrom by remember { mutableStateOf<RecipeVersion?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(recipeId) {
        recipe = dao.getRecipe(recipeId)
        loaded = true
    }

    val r = recipe
    if (r == null) {
        Page { Text(if (loaded) "Bu reçete bulunamadı." else "Yükleniyor…") }
        return
    }
    val defaults = Logic.waterDefaults(props)

    fun upd(n: Recipe) {
        recipe = n
        saver.save { dao.updateRecipe(n) }
    }

    Page {
        CardBox {
            Text(r.code + " · oluşturulma: " + Util.date(r.createdAt), fontSize = 12.sp, color = Brown)
            TextEdit("Reçete adı", r.name, { upd(r.copy(name = it)) }, stateKey = recipeId)
            TextEdit("Açıklama", r.description, { upd(r.copy(description = it)) }, minLines = 2, stateKey = recipeId)
            TextEdit("Hedef", r.goal, { upd(r.copy(goal = it)) }, minLines = 2, stateKey = recipeId)
            TextEdit("Serbest notlar", r.notes, { upd(r.copy(notes = it)) }, minLines = 3, stateKey = recipeId)
        }

        SectionTitle("Sürümler")
        Hint("Bir reçeteyi değiştirmek yerine yeni sürüme kopyala; eski sürüm ve onunla yapılan deneyler olduğu gibi kalır.")
        for (v in versions.sortedByDescending { it.versionNo }) {
            val vi = allIngs.filter { it.versionId == v.id }
            val s = summarize(v, vi, null, defaults)
            val parent = versions.firstOrNull { it.id == v.parentVersionId }
            ClickableCard(onClick = { nav.push(Screen.VersionEdit(v.id)) }) {
                Text("Sürüm v" + v.versionNo + " · " + Util.date(v.createdAt), fontWeight = FontWeight.Bold)
                if (parent != null) Text("Kaynak: v" + parent.versionNo, fontSize = 12.sp)
                if (v.note.isNotBlank()) Text(v.note, fontSize = 13.sp, maxLines = 2)
                if (s.flour > 0.0) {
                    Text(
                        "Un " + Fmt.num(s.flour, 0) + " g · toplam " + Fmt.num(s.total, 0) + " g · serbest su " + Fmt.pct(s.freeWater, 0) +
                            " · tahmini toplam su " + Fmt.pct(s.water.pct, 0) + (if (s.water.complete) "" else " (eksik)"),
                        fontSize = 12.sp, color = CalcInk
                    )
                } else {
                    Text("Malzeme gramajları henüz girilmedi", fontSize = 12.sp)
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { copyFrom = v }, modifier = Modifier.weight(1f)) { Text("Yeni sürüme kopyala", fontSize = 12.sp) }
                    Button(
                        onClick = { nav.push(Screen.NewExperiment(ExpDraft(versionId = v.id, planned = false))) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Deney başlat", fontSize = 12.sp) }
                }
            }
        }

        Spacer8()
        OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("Reçeteyi sil") }
        Hint("Sürümleriyle deney yapılmış reçeteler, geçmiş korunsun diye silinemez.")
    }

    val cf = copyFrom
    if (cf != null) {
        var note by remember(cf.id) { mutableStateOf("") }
        var newFlour by remember(cf.id) { mutableStateOf<Double?>(null) }
        var newDough by remember(cf.id) { mutableStateOf<Double?>(null) }
        AlertDialog(
            onDismissRequest = { copyFrom = null },
            title = { Text("v" + cf.versionNo + " sürümünden yeni sürüm") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    TextEdit("Yeni sürüm notu (ne değişecek?)", note, { note = it }, minLines = 2)
                    Hint("İsteğe bağlı ölçekleme: Bir değer girersen diğer malzemeler fırıncı yüzdelerine göre yeniden hesaplanır. İkisi de boşsa gramajlar aynen kopyalanır. İkisi de doluysa un miktarı kullanılır.")
                    NumberEdit("Yeni toplam un", newFlour, { newFlour = it }, suffix = "g", stateKey = cf.id)
                    NumberEdit("Veya hedef hamur miktarı", newDough, { newDough = it }, suffix = "g", stateKey = cf.id)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val src = allIngs.filter { it.versionId == cf.id }.map { Logic.toCIng(it) }
                    val wantsScale = (newFlour != null && newFlour!! > 0.0) || (newDough != null && newDough!! > 0.0)
                    val canScale = if (newFlour != null && newFlour!! > 0.0) Calc.flourTotal(src) > 0.0 else Calc.totalWeight(src) > 0.0
                    if (wantsScale && !canScale) {
                        message = "Ölçeklemek için kaynak sürümde un (veya toplam malzeme) gramajı olmalı."
                    } else {
                        val fl = newFlour?.takeIf { it > 0.0 }
                        val dg = if (fl == null) newDough?.takeIf { it > 0.0 } else null
                        copyFrom = null
                        scope.launch {
                            val id = repo.newVersionFrom(cf.id, fl, dg, note)
                            if (id != null) nav.push(Screen.VersionEdit(id))
                        }
                    }
                }) { Text("Oluştur") }
            },
            dismissButton = { TextButton(onClick = { copyFrom = null }) { Text("Vazgeç") } }
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Reçete silinsin mi?",
            text = "Reçete ve tüm sürümleri silinir. Deneyi olan reçeteler silinmez.",
            confirmLabel = "Sil",
            onConfirm = {
                confirmDelete = false
                scope.launch {
                    if (repo.deleteRecipeIfUnused(recipeId)) nav.pop()
                    else message = "Bu reçetenin sürümleriyle deney yapılmış. Geçmişi korumak için silinemez."
                }
            },
            onDismiss = { confirmDelete = false }
        )
    }
    val m = message
    if (m != null) MessageDialog("Bilgi", m) { message = null }
}

// ---------------------------------------------------------------------------------------------
// Sürüm düzenleme: malzemeler, hesaplamalar, maliyet, fotoğraf
// ---------------------------------------------------------------------------------------------

@Composable
private fun IngredientRowEditor(
    i: Ingredient,
    flour: Double,
    editable: Boolean,
    refresh: Int,
    onChange: (Ingredient) -> Unit,
    onDelete: () -> Unit
) {
    var extra by remember(i.id) { mutableStateOf(false) }
    CardBox {
        TextEdit("Malzeme adı", i.name, { onChange(i.copy(name = it)) }, enabled = editable, stateKey = i.id)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                DropdownField(
                    "Kategori", Cat.all.map { Cat.label(it) }, Cat.all.indexOf(i.category).coerceAtLeast(0),
                    { onChange(i.copy(category = Cat.all[it])) }, editable
                )
            }
            Column(Modifier.weight(1f)) {
                NumberEdit(
                    "Gramaj", if (i.grams == 0.0) null else i.grams, { onChange(i.copy(grams = it ?: 0.0)) },
                    suffix = "g", enabled = editable, stateKey = Pair(i.id, refresh)
                )
            }
        }
        val pct = Calc.bakerPercent(i.grams, flour)
        CalcRow("Fırıncı yüzdesi", if (pct == null) "—" else Fmt.pct(pct, 1), if (flour <= 0.0) "Un gramajı girilince hesaplanır" else null)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Chip(if (extra) "Su oranı ve fiyatı gizle" else "Su oranı ve fiyat", extra) { extra = !extra }
            Spacer8()
            TextButton(onClick = onDelete, enabled = editable) { Text("Satırı sil") }
        }
        if (extra) {
            NumberEdit(
                "Bu malzemenin su oranı (boşsa tablodaki varsayılan)", i.waterPercent, { onChange(i.copy(waterPercent = it)) },
                suffix = "%", max = 100.0, enabled = editable, stateKey = i.id
            )
            NumberEdit(
                "Fiyat", i.price, { onChange(i.copy(price = it)) },
                suffix = if (i.priceUnit == "ADET") "₺/adet" else "₺/kg", enabled = editable, stateKey = i.id
            )
            Row {
                Chip("₺/kg", i.priceUnit != "ADET") { if (editable) onChange(i.copy(priceUnit = "KG")) }
                Chip("₺/adet", i.priceUnit == "ADET") { if (editable) onChange(i.copy(priceUnit = "ADET")) }
            }
            if (i.priceUnit == "ADET") {
                NumberEdit(
                    "1 adetin gramajı", i.unitGrams, { onChange(i.copy(unitGrams = it)) },
                    suffix = "g", enabled = editable, stateKey = i.id
                )
            }
        }
    }
}

@Composable
fun VersionEditScreen(versionId: Long) {
    val repo = LocalRepo.current
    val dao = repo.dao
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val saver = rememberAutoSaver()
    var ver by remember(versionId) { mutableStateOf<RecipeVersion?>(null) }
    var recipe by remember(versionId) { mutableStateOf<Recipe?>(null) }
    val rows = remember(versionId) { mutableStateListOf<Ingredient>() }
    var cost by remember(versionId) { mutableStateOf(CostRecord(versionId = versionId)) }
    var loaded by remember(versionId) { mutableStateOf(false) }
    var refresh by remember(versionId) { mutableStateOf(0) }
    val props by rememberFlow(emptyList<IngredientProperty>()) { dao.properties() }
    val expCount by rememberFlow(0, versionId) { dao.experimentCountForVersion(versionId) }
    val photos by rememberFlow(emptyList<ExperimentPhoto>(), versionId) { dao.photosOfVersion(versionId) }
    var unlocked by remember(versionId) { mutableStateOf(false) }
    var askUnlock by remember { mutableStateOf(false) }
    var deleteRow by remember { mutableStateOf<Ingredient?>(null) }
    var needPieces by remember(versionId) { mutableStateOf<Double?>(null) }
    var calcFlour by remember(versionId) { mutableStateOf<Double?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(versionId) {
        val v = dao.getVersion(versionId)
        ver = v
        recipe = v?.let { dao.getRecipe(it.recipeId) }
        rows.clear()
        rows.addAll(dao.ingredientsOfOnce(versionId))
        cost = dao.costOnce(versionId) ?: CostRecord(versionId = versionId)
        loaded = true
    }

    val v = ver
    if (v == null) {
        Page { Text(if (loaded) "Bu sürüm bulunamadı." else "Yükleniyor…") }
        return
    }
    val locked = expCount > 0
    val editable = !locked || unlocked
    val defaults = Logic.waterDefaults(props)
    val list = rows.toList()
    val s = summarize(v, list, cost, defaults)

    fun updVer(n: RecipeVersion) {
        ver = n
        saver.save { dao.updateVersion(n) }
    }

    fun updRow(n: Ingredient) {
        val idx = rows.indexOfFirst { it.id == n.id }
        if (idx >= 0) rows[idx] = n
        saver.save { dao.updateIngredient(n) }
    }

    fun updCost(n: CostRecord) {
        cost = n
        saver.save { dao.upsertCost(n) }
    }

    Page {
        Text(
            (recipe?.code ?: "") + " · " + (recipe?.name ?: "Reçete") + " · v" + v.versionNo,
            fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown
        )
        Text("Oluşturulma: " + Util.date(v.createdAt), fontSize = 12.sp)

        if (locked && !unlocked) {
            Warn("Bu sürümle " + expCount + " deney yapılmış. Geçmiş kayıtlar bozulmasın diye düzenleme kilitli. Değişiklik için \"Yeni sürüme kopyala\" önerilir.")
            OutlinedButton(onClick = { askUnlock = true }, modifier = Modifier.fillMaxWidth()) { Text("Yine de bu sürümü düzenle") }
        } else if (locked) {
            Warn("Dikkat: Bu sürümde yaptığın değişiklikler, onunla yapılmış " + expCount + " deneyin reçete bilgisini de değiştirir.")
        }

        SectionTitle("Sürüm notu")
        TextEdit("Bu sürümde ne değişti / neden?", v.note, { updVer(v.copy(note = it)) }, minLines = 2, enabled = editable, stateKey = versionId)

        // ---------------------------------------------------------------- Malzemeler
        SectionTitle("Malzemeler")
        Hint("Beyaz alanları sen girersin, mavi alanlar otomatik hesaplanır. Un gramajı değişince yüzdeler kendiliğinden güncellenir.")
        for (i in rows.toList()) {
            IngredientRowEditor(
                i = i, flour = s.flour, editable = editable, refresh = refresh,
                onChange = { updRow(it) }, onDelete = { deleteRow = i }
            )
        }
        OutlinedButton(
            enabled = editable,
            onClick = {
                scope.launch {
                    val ni = Ingredient(versionId = versionId, name = "Yeni malzeme", category = Cat.OTHER, grams = 0.0, sortOrder = rows.size)
                    val id = dao.insertIngredient(ni)
                    rows.add(ni.copy(id = id))
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Malzeme ekle") }

        // ---------------------------------------------------------------- Hesaplamalar
        SectionTitle("Otomatik hesaplamalar")
        if (s.flour <= 0.0) {
            Warn("Un gramajı 0 veya boş olduğu için yüzde ve oran hesapları yapılamıyor.")
        }
        CalcRow("Toplam un", Fmt.num(s.flour, 1) + " g", "Birden fazla un varsa referans, unların toplamıdır (%100).")
        CalcRow("Toplam malzeme ağırlığı", Fmt.num(s.total, 1) + " g")
        CalcRow(
            "Eklenen serbest su oranı", Fmt.pct(s.freeWater, 1),
            "Doğrudan \"Su\" kategorisindeki malzemelerin una oranı."
        )
        CalcRow(
            "Tahmini toplam su oranı", Fmt.pct(s.water.pct, 1),
            if (s.water.complete) "Su, süt, yumurta, tereyağı gibi malzemelerin varsayılan su oranlarıyla yapılan yaklaşık bir hesaptır; laboratuvar ölçümü değildir. Yağ, yumurta ve süt katıları hamurun davranışını yalnızca su oranıyla açıklanamayacak şekilde etkiler."
            else "Hesap EKSİK: şu malzemelerin su oranı bilinmiyor: " + s.water.missing.joinToString(", ") + ". Malzeme satırından su oranı girebilirsin."
        )
        CalcRow("Hamur miktarı (tahmini)", Fmt.num(s.dough, 1) + " g", if (s.total <= 0.0) "Malzeme gramajı girilmediği için beklenen hamur miktarı kullanıldı." else null)
        CalcRow(
            "Teorik parça sayısı",
            if (s.theoretical == null) "—" else Fmt.num(s.theoretical, 2) + " adet",
            if (s.theoretical == null) "Hedef parça gramajı girilince hesaplanır." else null
        )
        CalcRow(
            "Fire dahil tahmini üretilebilir parça",
            if (s.wholePieces == null) "—" else s.wholePieces.toString() + " adet",
            if (s.pieces.lossAssumedZero && s.wholePieces != null) "Fire girilmediği için %0 varsayıldı; bu ölçülmüş bir fire değildir." else null
        )

        // ---------------------------------------------------------------- Reçete bilgileri
        SectionTitle("Un ve maya")
        TextEdit("Un markası", v.flourBrand, { updVer(v.copy(flourBrand = it)) }, enabled = editable, stateKey = versionId)
        TextEdit("Un türü", v.flourType, { updVer(v.copy(flourType = it)) }, enabled = editable, stateKey = versionId)
        NumberEdit("Un protein oranı", v.flourProtein, { updVer(v.copy(flourProtein = it)) }, suffix = "%", max = 100.0, enabled = editable, stateKey = versionId)
        TextEdit("Unun diğer teknik özellikleri", v.flourSpecs, { updVer(v.copy(flourSpecs = it)) }, minLines = 2, enabled = editable, stateKey = versionId)
        TextEdit("Maya türü", v.yeastType, { updVer(v.copy(yeastType = it)) }, enabled = editable, stateKey = versionId)
        TextEdit("Maya markası", v.yeastBrand, { updVer(v.copy(yeastBrand = it)) }, enabled = editable, stateKey = versionId)

        SectionTitle("Yöntemler")
        TextEdit("Karıştırma ve yoğurma yöntemi", v.mixMethod, { updVer(v.copy(mixMethod = it)) }, minLines = 2, enabled = editable, stateKey = versionId)
        TextEdit("Fermantasyon yöntemi", v.fermentMethod, { updVer(v.copy(fermentMethod = it)) }, minLines = 2, enabled = editable, stateKey = versionId)
        TextEdit("Pişirme yöntemi", v.bakeMethod, { updVer(v.copy(bakeMethod = it)) }, minLines = 2, enabled = editable, stateKey = versionId)

        SectionTitle("Hedefler")
        NumberEdit("Beklenen hamur miktarı (isteğe bağlı)", v.expectedDoughG, { updVer(v.copy(expectedDoughG = it)) }, suffix = "g", enabled = editable, stateKey = versionId)
        NumberEdit("Hedef parça gramajı", v.targetPieceG, { updVer(v.copy(targetPieceG = it)) }, suffix = "g", enabled = editable, stateKey = versionId)
        NumberEdit("Tahmini / ölçülen fire oranı (boşsa %0 varsayılır)", v.lossPct, { updVer(v.copy(lossPct = it)) }, suffix = "%", max = 99.9, enabled = editable, stateKey = versionId)

        // ---------------------------------------------------------------- Üretim hesaplayıcı
        SectionTitle("Üretim hesaplayıcı (kaydedilmez)")
        Hint("Hedef adet veya yeni un miktarı gir; malzemeler bu sürümün yüzdelerine göre hesaplanır. Dilersen sonucu yeni sürüm olarak kaydedebilirsin.")
        NumberEdit("Hedef adet", needPieces, { needPieces = it }, suffix = "adet", stateKey = versionId)
        NumberEdit("Veya yeni toplam un", calcFlour, { calcFlour = it }, suffix = "g", stateKey = versionId)
        val cings: List<CIng> = list.map { Logic.toCIng(it) }
        val pieceG = v.targetPieceG
        val loss = v.lossPct ?: 0.0
        var neededDough: Double? = null
        var scaled: List<Double>? = null
        val np = needPieces
        val cf = calcFlour
        if (np != null && np > 0.0) {
            if (pieceG != null && pieceG > 0.0 && loss < 100.0) {
                neededDough = np * pieceG / (1.0 - loss / 100.0)
                scaled = Calc.scaleToDough(cings, neededDough)
            } else {
                Warn("Hedef adetten hesap için hedef parça gramajı gerekli.")
            }
        } else if (cf != null && cf > 0.0) {
            scaled = Calc.scaleToFlour(cings, cf)
        }
        if (scaled != null) {
            for ((idx, i) in list.withIndex()) {
                if (i.grams > 0.0) CalcRow(i.name, Fmt.num(scaled[idx], 1) + " g")
            }
            if (neededDough != null) {
                CalcRow("Gereken toplam hamur", Fmt.num(neededDough, 1) + " g", if (v.lossPct == null) "Fire girilmediği için %0 varsayıldı." else "Fire %" + Fmt.num(loss, 1) + " hesaba katıldı.")
            }
            Spacer8()
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val id = repo.newVersionFrom(
                            versionId,
                            if (neededDough == null) calcFlour else null,
                            neededDough,
                            "Ölçeklenmiş sürüm (" + (if (neededDough != null) "hedef " + Fmt.num(np, 0) + " adet" else "un " + Fmt.num(calcFlour, 0) + " g") + ")"
                        )
                        if (id != null) nav.push(Screen.VersionEdit(id))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Bu miktarlarla yeni sürüm oluştur") }
        }

        // ---------------------------------------------------------------- Maliyet
        SectionTitle("Maliyet (isteğe bağlı)")
        Hint("Malzeme fiyatlarını her malzeme satırındaki \"Su oranı ve fiyat\" bölümünden gir. Fiyatı girilmeyen malzeme sıfır maliyetli sayılmaz; hesap eksik gösterilir.")
        NumberEdit("Ambalaj (adet başı)", cost.packagingPerPiece, { updCost(cost.copy(packagingPerPiece = it)) }, suffix = "₺", stateKey = versionId)
        NumberEdit("Enerji gideri (bu üretim, toplam)", cost.energyTotal, { updCost(cost.copy(energyTotal = it)) }, suffix = "₺", stateKey = versionId)
        NumberEdit("İşçilik gideri (bu üretim, toplam)", cost.laborTotal, { updCost(cost.copy(laborTotal = it)) }, suffix = "₺", stateKey = versionId)
        NumberEdit("Diğer giderler (bu üretim, toplam)", cost.otherTotal, { updCost(cost.copy(otherTotal = it)) }, suffix = "₺", stateKey = versionId)
        NumberEdit("Satış fiyatı (adet başı)", cost.salePrice, { updCost(cost.copy(salePrice = it)) }, suffix = "₺", stateKey = versionId)
        val c = s.cost
        if (c.materialTotal == null) {
            Warn(
                "Toplam malzeme maliyeti hesaplanamadı. Fiyatı eksik malzemeler: " +
                    (if (c.missingPrices.isEmpty()) "malzeme gramajı girilmemiş" else c.missingPrices.joinToString(", ")) +
                    (if (c.materialPartial > 0.0) ". Bilinen kısmın toplamı: " + Fmt.tl(c.materialPartial) else "")
            )
        }
        CalcRow("Toplam malzeme maliyeti", Fmt.tl(c.materialTotal))
        CalcRow("1 kg hamurun tahmini maliyeti", Fmt.tl(c.perKgDough))
        CalcRow(
            "Adet başı tahmini malzeme maliyeti", Fmt.tl(c.perPieceMaterial),
            if (c.perPieceMaterial == null && s.wholePieces == null) "Hedef parça gramajı girilince hesaplanır." else null
        )
        CalcRow(
            "Adet başı tahmini toplam maliyet", Fmt.tl(c.perPieceTotal),
            if (c.perPieceTotal != null && c.missingExpenses.isNotEmpty()) "Girilmeyen giderler: " + c.missingExpenses.joinToString(", ") + ". Toplam maliyet bu giderler olmadan hesaplandı." else null
        )
        CalcRow("Birim brüt katkı", Fmt.tl(c.contribution), if (cost.salePrice == null) "Satış fiyatı girilince hesaplanır." else null)
        CalcRow("Tahmini marj", Fmt.pct(c.marginPct, 1))

        // ---------------------------------------------------------------- Fotoğraf
        SectionTitle("Reçete fotoğrafları")
        PhotoSection(null, versionId, photos, listOf("Hamur", "Pişmiş ekmek", "İç doku", "Diğer"))
        Spacer8()
    }

    if (askUnlock) {
        ConfirmDialog(
            title = "Kilidi aç?",
            text = "Bu sürümle deney yapılmış. Değiştirirsen eski deneylerin bağlı olduğu reçete bilgisi de değişir. Yeni sürüme kopyalamak daha güvenlidir.",
            confirmLabel = "Yine de düzenle",
            onConfirm = { unlocked = true; askUnlock = false },
            onDismiss = { askUnlock = false }
        )
    }
    val dr = deleteRow
    if (dr != null) {
        ConfirmDialog(
            title = "Malzeme satırı silinsin mi?",
            text = dr.name + " satırı bu sürümden kaldırılır.",
            confirmLabel = "Sil",
            onConfirm = {
                deleteRow = null
                rows.removeAll { it.id == dr.id }
                scope.launch { dao.deleteIngredient(dr) }
            },
            onDismiss = { deleteRow = null }
        )
    }
    val m = message
    if (m != null) MessageDialog("Bilgi", m) { message = null }
}
