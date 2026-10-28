@file:OptIn(ExperimentalMaterial3Api::class)

package com.briochelab.app.ui

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.briochelab.app.calc.Fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

val Brown = Color(0xFF7A4A1E)
val BrownLight = Color(0xFFF3E6D6)
val CalcTint = Color(0xFFE4F0FB)
val CalcInk = Color(0xFF0F4C81)
val WarnTint = Color(0xFFFFF1D6)
val BadTint = Color(0xFFFCE4E1)

@Composable
fun BriocheTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Brown,
            onPrimary = Color.White,
            primaryContainer = BrownLight,
            onPrimaryContainer = Color(0xFF3A2208),
            secondary = Color(0xFF8A6D3B),
            surface = Color(0xFFFFFBF7),
            background = Color(0xFFFFFBF7)
        ),
        content = content
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Brown,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
    )
}

@Composable
fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B6B6B), modifier = Modifier.padding(vertical = 2.dp))
}

@Composable
fun CardBox(modifier: Modifier = Modifier, tint: Color? = null, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = tint ?: Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(12.dp)) { content() }
    }
}

/** Otomatik hesaplanan degerler mavi zeminle, "Hesaplanan" etiketiyle gosterilir. */
@Composable
fun CalcRow(label: String, value: String, note: String? = null) {
    Box(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(8.dp)).background(CalcTint).padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
                Text(value, fontWeight = FontWeight.Bold, color = CalcInk, fontSize = 16.sp)
            }
            Text("Hesaplanan değer", fontSize = 10.sp, color = CalcInk)
            if (note != null) Text(note, fontSize = 12.sp, color = Color(0xFF444444))
        }
    }
}

@Composable
fun Warn(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(8.dp)).background(WarnTint).padding(10.dp)) {
        Text(text, fontSize = 13.sp, color = Color(0xFF6B4A00))
    }
}

@Composable
fun Chip(text: String, selected: Boolean = false, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(text, fontSize = 13.sp) }, modifier = Modifier.padding(end = 6.dp))
}

@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String = "Evet", onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

@Composable
fun MessageDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tamam") } }
    )
}

/** Yazdikca kaydeden sayi alani. Bos birakilirsa null gonderilir (sifir kaydedilmez). */
@Composable
fun NumberEdit(
    label: String,
    initial: Double?,
    onValue: (Double?) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String = "",
    max: Double? = null,
    enabled: Boolean = true,
    stateKey: Any? = null
) {
    var t by remember(stateKey) { mutableStateOf(Fmt.edit(initial)) }
    val err = Fmt.errorFor(t, max)
    val sup: (@Composable () -> Unit)? = if (err != null) {
        { Text(err) }
    } else null
    OutlinedTextField(
        value = t,
        onValueChange = {
            t = it
            if (Fmt.errorFor(it, max) == null) onValue(Fmt.parse(it))
        },
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = err != null,
        supportingText = sup,
        suffix = if (suffix.isNotEmpty()) {
            { Text(suffix) }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun TextEdit(
    label: String,
    initial: String,
    onValue: (String) -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    enabled: Boolean = true,
    stateKey: Any? = null
) {
    var t by remember(stateKey) { mutableStateOf(initial) }
    OutlinedTextField(
        value = t,
        onValueChange = {
            t = it
            onValue(it)
        },
        label = { Text(label) },
        minLines = minLines,
        singleLine = minLines == 1,
        enabled = enabled,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun DropdownField(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit, enabled: Boolean = true) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, fontSize = 12.sp, color = Color(0xFF6B6B6B))
        Box {
            OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(options.getOrElse(selected) { "Seçin" }, maxLines = 1)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEachIndexed { idx, o ->
                    DropdownMenuItem(text = { Text(o) }, onClick = { open = false; onSelect(idx) })
                }
            }
        }
    }
}

@Composable
fun DateField(label: String, millis: Long, onChange: (Long) -> Unit, enabled: Boolean = true) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, fontSize = 12.sp, color = Color(0xFF6B6B6B))
        OutlinedButton(
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val c = Calendar.getInstance()
                c.timeInMillis = millis
                DatePickerDialog(ctx, { _, y, m, d ->
                    val n = Calendar.getInstance()
                    n.set(y, m, d, 12, 0, 0)
                    onChange(n.timeInMillis)
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
            }
        ) { Text(com.briochelab.app.data.Util.date(millis)) }
    }
}

/** 1-10 puan secici. Secili puana tekrar basinca puan silinir (bos kalir). */
@Composable
fun ScoreRow(label: String, value: Int?, onChange: (Int?) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row {
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(if (value == null) "—" else "$value/10", fontWeight = FontWeight.Bold, color = Brown)
        }
        LazyRow {
            items((1..10).toList()) { n ->
                FilterChip(
                    selected = value == n,
                    onClick = { onChange(if (value == n) null else n) },
                    label = { Text(n.toString()) },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
    }
}

fun loadBitmap(f: File, maxSide: Int): Bitmap? {
    if (!f.exists()) return null
    val bounds = BitmapFactory.Options()
    bounds.inJustDecodeBounds = true
    BitmapFactory.decodeFile(f.absolutePath, bounds)
    var sample = 1
    while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) sample *= 2
    val opts = BitmapFactory.Options()
    opts.inSampleSize = sample
    val bmp = BitmapFactory.decodeFile(f.absolutePath, opts) ?: return null
    return try {
        val exif = ExifInterface(f.absolutePath)
        val rot = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rot == 0f) bmp else {
            val m = Matrix()
            m.postRotate(rot)
            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
    } catch (e: Exception) {
        bmp
    }
}

@Composable
fun PhotoThumb(file: File, size: Int = 96, modifier: Modifier = Modifier) {
    val bmp by produceState<Bitmap?>(null, file.absolutePath) {
        value = withContext(Dispatchers.IO) { loadBitmap(file, 400) }
    }
    val b = bmp
    if (b != null) {
        Image(
            bitmap = b.asImageBitmap(),
            contentDescription = "Fotoğraf",
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size.dp).clip(RoundedCornerShape(8.dp))
        )
    } else {
        Box(modifier.size(size.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEEEEEE)), contentAlignment = Alignment.Center) {
            Text("…", color = Color.Gray)
        }
    }
}

@Composable
fun Spacer8() {
    Spacer(Modifier.height(8.dp))
}

@Composable
fun ClickableCard(onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color? = null, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = tint ?: Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(12.dp)) { content() }
    }
}

fun Modifier.w(dp: Int): Modifier = this.width(dp.dp)
