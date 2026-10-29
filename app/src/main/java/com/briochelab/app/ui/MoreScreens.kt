@file:OptIn(ExperimentalMaterial3Api::class)

package com.briochelab.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.briochelab.app.calc.Cat
import com.briochelab.app.calc.Fmt
import com.briochelab.app.data.BackupCodec
import com.briochelab.app.data.BackupData
import com.briochelab.app.data.BackupException
import com.briochelab.app.data.IngredientProperty
import com.briochelab.app.data.InfoContent
import com.briochelab.app.data.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------------------------
// Bilgi Merkezi
// ---------------------------------------------------------------------------------------------

@Composable
fun InfoScreen() {
    var open by remember { mutableStateOf<Int?>(null) }
    Page {
        Warn(InfoContent.DISCLAIMER)
        InfoContent.topics.forEachIndexed { idx, t ->
            val isOpen = open == idx
            ClickableCard(onClick = { open = if (isOpen) null else idx }) {
                Text(t.title, fontWeight = FontWeight.Bold, color = Brown, fontSize = 16.sp)
                if (isOpen) {
                    InfoBlock("1. Nedir?", t.what)
                    InfoBlock("2. Neden önemlidir?", t.why)
                    InfoBlock("3. Brioche'ı nasıl etkiler?", t.effect)
                    InfoBlock("4. Hangi belirtilere bakmalıyım?", t.signs)
                    InfoBlock("5. Hangi hatalardan kaçınmalıyım?", t.mistakes)
                } else {
                    Text("Açmak için dokun", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun InfoBlock(title: String, body: String) {
    Text(title, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
    BodyText(body)
}

// ---------------------------------------------------------------------------------------------
// Yedekleme ve Ayarlar
// ---------------------------------------------------------------------------------------------

private fun stamp(): String = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())

@Composable
fun SettingsScreen() {
    val repo = LocalRepo.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val props by rememberFlow(emptyList<IngredientProperty>()) { repo.dao.properties() }
    val backups by rememberFlow(emptyList<com.briochelab.app.data.BackupMetadata>()) { repo.dao.backups() }

    var message by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pending by remember { mutableStateOf<BackupData?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun say(title: String, text: String) {
        message = Pair(title, text)
    }

    val saveJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                try {
                    val text = repo.exportJson()
                    withContext(Dispatchers.IO) {
                        ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                    }
                    say("Yedek alındı", "JSON yedeği kaydedildi. Fotoğraflar bu dosyaya dahil değildir; onları \"Fotoğrafları yedekle\" ile ayrıca alın.")
                } catch (e: Exception) {
                    say("Yedek alınamadı", e.message ?: "Bilinmeyen hata")
                }
                busy = false
            }
        }
    }
    val saveCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                try {
                    val text = repo.exportCsv()
                    withContext(Dispatchers.IO) {
                        ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                    }
                    say("CSV hazır", "Deney ve ölçüm verileri CSV olarak kaydedildi (noktalı virgülle ayrılmış, Excel'de açılır).")
                } catch (e: Exception) {
                    say("CSV alınamadı", e.message ?: "Bilinmeyen hata")
                }
                busy = false
            }
        }
    }
    val savePhotos = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                try {
                    val n = ctx.contentResolver.openOutputStream(uri, "wt")!!.use { repo.zipPhotos(it) }
                    say("Fotoğraflar yedeklendi", "$n fotoğraf ZIP dosyasına kaydedildi.")
                } catch (e: Exception) {
                    say("Fotoğraf yedeği alınamadı", e.message ?: "Bilinmeyen hata")
                }
                busy = false
            }
        }
    }
    val openJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                try {
                    val text = withContext(Dispatchers.IO) {
                        ctx.contentResolver.openInputStream(uri)!!.use { String(it.readBytes(), Charsets.UTF_8) }
                    }
                    pending = BackupCodec.decode(text)
                } catch (e: BackupException) {
                    say("Yedek okunamadı", e.message ?: "Geçersiz yedek")
                } catch (e: Exception) {
                    say("Yedek okunamadı", "Dosya açılamadı: " + (e.message ?: "bilinmeyen hata"))
                }
                busy = false
            }
        }
    }
    val openPhotos = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                try {
                    val n = ctx.contentResolver.openInputStream(uri)!!.use { repo.unzipPhotos(it) }
                    say("Fotoğraflar geri yüklendi", "$n yeni fotoğraf eklendi (aynı adlı olanlar atlandı).")
                } catch (e: Exception) {
                    say("Fotoğraflar okunamadı", e.message ?: "Geçersiz ZIP")
                }
                busy = false
            }
        }
    }

    Page {
        Warn("Kayıtlar yalnızca bu telefonda saklanır. Uygulamayı silerseniz veya telefon bozulursa kaybolabilir. Düzenli yedek alıp dosyayı telefon dışında (Drive, bilgisayar, e-posta) saklayın.")
        if (busy) Hint("İşlem yapılıyor…")

        SectionTitle("Yedek al")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { saveJson.launch("BriocheLab_yedek_" + stamp() + ".json") }, modifier = Modifier.weight(1f)) { Text("JSON yedek") }
            OutlinedButton(onClick = { savePhotos.launch("BriocheLab_fotograflar_" + stamp() + ".zip") }, modifier = Modifier.weight(1f)) { Text("Fotoğrafları yedekle") }
        }
        OutlinedButton(onClick = { saveCsv.launch("BriocheLab_deneyler_" + stamp() + ".csv") }, modifier = Modifier.fillMaxWidth()) {
            Text("CSV olarak dışa aktar")
        }
        Hint("JSON yedeği tüm reçete, deney, not ve puanları içerir; fotoğraf dosyaları ayrı ZIP olarak yedeklenir.")

        SectionTitle("Geri yükle")
        Button(onClick = { openJson.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) {
            Text("JSON yedeğinden geri yükle")
        }
        OutlinedButton(onClick = { openPhotos.launch(arrayOf("application/zip", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) {
            Text("Fotoğraf ZIP'inden geri yükle")
        }
        Hint("Dosya seçildikten sonra içeriği size gösterilir; onaylamadan hiçbir şey değişmez. Geri yüklemeden önce mevcut verinin otomatik bir kopyası uygulama içine yazılır.")

        SectionTitle("Malzeme su oranı varsayımları")
        Hint("Tahmini toplam su oranı bu değerlerle hesaplanır. Bunlar gerçek analiz sonucu değildir; elinizdeki ürün bilgisine göre değiştirebilirsiniz. Boş bırakırsanız o kategori için hesap \"eksik\" sayılır.")
        for (c in Cat.all) {
            if (c == Cat.FLOUR) continue
            val cur = props.firstOrNull { it.category == c }
            NumberEdit(
                label = Cat.label(c) + " su oranı",
                initial = cur?.waterPercent,
                onValue = { v -> scope.launch { repo.dao.upsertProperty(IngredientProperty(c, v)) } },
                suffix = "%",
                max = 100.0,
                stateKey = c
            )
        }
        OutlinedButton(
            onClick = {
                scope.launch {
                    for (c in Cat.all) repo.dao.upsertProperty(IngredientProperty(c, Cat.defaultWater[c]))
                    say("Varsayılanlar", "Su oranları başlangıç varsayımlarına döndürüldü. Alanları görmek için ekrandan çıkıp tekrar girin.")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Başlangıç varsayımlarına dön") }

        SectionTitle("Yedek geçmişi")
        if (backups.isEmpty()) Hint("Henüz yedek alınmadı.") else {
            for (b in backups.take(10)) {
                Text(Util.dateTime(b.createdAt) + " · " + b.kind + " · " + b.itemCount + " kayıt", fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp))
            }
        }

        SectionTitle("Hakkında")
        BodyText("BriocheLab " + com.briochelab.app.data.Repo.APP_VERSION + " — tüm veriler internet gerektirmeden telefonda saklanır.")
        Hint("Hesaplanan değerler mavi zeminle gösterilir. Hesaplamalar tahminidir; ölçülmemiş değerler kayıt edilmez.")
    }

    val p = pending
    if (p != null) {
        val lines = BackupCodec.summaryLines(p).joinToString("\n")
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Yedek içeriği") },
            text = {
                androidx.compose.foundation.layout.Column {
                    Text(lines)
                    Hint("\"Birleştir\": yedekteki yeni reçete ve deneyleri ekler, aynı kodlular atlanır. \"Tümünün yerine koy\": mevcut tüm veri silinip yedekle değiştirilir.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    scope.launch {
                        busy = true
                        try {
                            val r = repo.restoreMerge(p)
                            say("Birleştirildi", r.added.toString() + " kayıt eklendi, " + r.skipped + " kayıt zaten vardı (atlandı).")
                        } catch (e: Exception) {
                            say("Geri yükleme başarısız", e.message ?: "Bilinmeyen hata")
                        }
                        busy = false
                    }
                }) { Text("Birleştir") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pending = null }) { Text("Vazgeç") }
                    TextButton(onClick = {
                        pending = null
                        scope.launch {
                            busy = true
                            try {
                                val r = repo.restoreReplace(p)
                                say("Geri yüklendi", r.added.toString() + " kayıt yüklendi. Önceki veri uygulama içinde \"geri_yukleme_oncesi_yedek.json\" olarak saklandı.")
                            } catch (e: Exception) {
                                say("Geri yükleme başarısız", e.message ?: "Bilinmeyen hata")
                            }
                            busy = false
                        }
                    }) { Text("Tümünün yerine koy") }
                }
            }
        )
    }

    val m = message
    if (m != null) MessageDialog(m.first, m.second) { message = null }
}
