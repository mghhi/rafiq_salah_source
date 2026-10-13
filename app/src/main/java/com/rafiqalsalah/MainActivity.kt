package com.rafiqalsalah

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.rafiqalsalah.data.*
import com.rafiqalsalah.domain.PrayerLogic
import com.rafiqalsalah.domain.PrayerStatus
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Ink = Color(0xFF203B35)
private val Green = Color(0xFF477A68)
private val Pale = Color(0xFFF3F7F3)
private val Gold = Color(0xFFE5B65B)

class MainActivity : ComponentActivity() {
    private lateinit var dao: RafiqDao
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dao = AppDatabase.get(this).dao()
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Green, secondary = Gold, background = Pale, surface = Color.White)) { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { RafiqApp(dao) } } }
    }
}

@Composable
private fun RafiqApp(dao: RafiqDao) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val dateKey = selectedDate.toString()
    val day by dao.observeDay(dateKey).collectAsState(initial = null)
    val prayers by dao.observePrayers(dateKey).collectAsState(initial = emptyList())
    val rawatib by dao.observeRawatib(dateKey).collectAsState(initial = emptyList())
    val settings by dao.observeSettings().collectAsState(initial = AppSettings())
    val currentDay = day ?: DailyRecord(date = dateKey)
    val normalizedPrayers = (0..4).map { id -> prayers.firstOrNull { it.prayerId == id } ?: PrayerEntry(dateKey, id) }
    val evaluation = PrayerLogic.evaluate(normalizedPrayers)
    val points = PrayerLogic.totalPoints(currentDay, normalizedPrayers, rawatib)
    val percent = PrayerLogic.completionPercent(points, settings?.dailyGoal ?: 500)
    val scope = rememberCoroutineScope()

    LaunchedEffect(dateKey) {
        if (dao.getDay(dateKey) == null) dao.saveDay(DailyRecord(dateKey))
        (0..4).forEach { id -> if (dao.getPrayers(dateKey).none { it.prayerId == id }) dao.savePrayer(PrayerEntry(dateKey, id)) }
        if (dao.getSettings() == null) dao.saveSettings(AppSettings())
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Pale).padding(horizontal = 16.dp), contentPadding = PaddingValues(top = 18.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("رفيق الصلاة", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("خطوة ثابتة كل يوم", color = Green, fontSize = 14.sp)
                }
                Text("☾", fontSize = 34.sp, color = Green)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Ink), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (selectedDate == LocalDate.now()) "أهلًا بيك، يوم جديد مبارك" else "مراجعة يوم سابق", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(selectedDate.format(DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale("ar"))), color = Color.White.copy(alpha = .8f), fontSize = 13.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { selectedDate = selectedDate.minusDays(1) }) { Text("السابق", color = Color.White) }
                            TextButton(onClick = { selectedDate = selectedDate.plusDays(1) }) { Text("التالي", color = Color.White) }
                        }
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$percent%", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.width(8.dp)); Text("إنجاز اليوم", color = Color.White.copy(alpha = .8f), modifier = Modifier.padding(bottom = 7.dp))
                    }
                    LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp), color = Gold, trackColor = Color.White.copy(alpha = .2f))
                    Text(if (percent >= 100) "اكتمل هدفك اليومي، تقبّل الله منك" else if (percent >= 60) "أداء طيب، كمّل على قد ما تقدر" else "كل خطوة صغيرة بتفرق، كمّل يومك", color = Color.White, fontSize = 14.sp)
                }
            }
        }
        item {
            SectionTitle("تقييم الفروض الخمسة", "مستقل عن بقية العبادات")
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(evaluation.title, color = if (evaluation.qadaNames.isNotEmpty()) Color(0xFF9A4A30) else Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(evaluation.description, color = Color(0xFF52615B), fontSize = 14.sp, lineHeight = 21.sp)
                    if (evaluation.isComplete) Text("هذا تقييم شخصي لتنظيم العادات، وليس حكمًا شرعيًا أو مقياسًا للثواب.", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }
        item { SectionTitle("الصلوات المفروضة", "اختار الحالة الصحيحة، وتقدر تعدّلها في أي وقت") }
        items(normalizedPrayers, key = { it.prayerId }) { prayer ->
            var expanded by remember(prayer.status) { mutableStateOf(false) }
            val statusLabel = when (prayer.status) {
                PrayerStatus.CONGREGATION -> "صليت جماعة"
                PrayerStatus.SOLO -> "صليت منفردًا"
                PrayerStatus.QADA -> "صليت قضاء"
                else -> "لم تُسجّل بعد"
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(PrayerLogic.prayerNames[prayer.prayerId], fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        Text(statusLabel, fontSize = 13.sp, color = if (prayer.status == PrayerStatus.QADA) Color(0xFF9A4A30) else Green)
                    }
                    Box {
                        OutlinedButton(onClick = { expanded = true }) { Text("تعديل") }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("لم تُسجّل بعد" to PrayerStatus.UNRECORDED, "صليت جماعة" to PrayerStatus.CONGREGATION, "صليت منفردًا" to PrayerStatus.SOLO, "صليت قضاء" to PrayerStatus.QADA).forEach { (label, value) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = { expanded = false; scope.launch { dao.savePrayer(prayer.copy(status = value)) } })
                            }
                        }
                    }
                }
            }
        }
        item { SectionTitle("السنن والعبادات", "سجّل اللي أتممته اليوم") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("قيام الليل والوتر", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    CounterRow("ركعات قيام الليل", currentDay.qiyamRakat, { n -> scope.launch { dao.saveDay(currentDay.copy(qiyamRakat = n.coerceAtLeast(0), updatedAt = System.currentTimeMillis())) } })
                    ToggleRow("صلاة الوتر", currentDay.witrDone) { v -> scope.launch { dao.saveDay(currentDay.copy(witrDone = v, updatedAt = System.currentTimeMillis())) } }
                    Divider(color = Pale)
                    Text("سنن إضافية", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    ToggleRow("صلاة الضحى", currentDay.duhaDone) { v -> scope.launch { dao.saveDay(currentDay.copy(duhaDone = v, updatedAt = System.currentTimeMillis())) } }
                    ToggleRow("السنة القبلية للعصر", currentDay.asrSunnahDone) { v -> scope.launch { dao.saveDay(currentDay.copy(asrSunnahDone = v, updatedAt = System.currentTimeMillis())) } }
                    CounterRow("ركعات النوافل الأخرى", currentDay.naflRakat, { n -> scope.launch { dao.saveDay(currentDay.copy(naflRakat = n.coerceAtLeast(0), updatedAt = System.currentTimeMillis())) } })
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("السنن الرواتب", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    val rawatibNames = listOf("السنة القبلية للفجر", "السنة القبلية للظهر", "السنة البعدية للظهر", "السنة البعدية للمغرب", "السنة البعدية للعشاء")
                    rawatibNames.forEachIndexed { id, name -> ToggleRow(name, rawatib.any { it.sunnahId == id && it.done }) { v -> scope.launch { dao.saveRawatib(RawatibEntry(dateKey, id, v)) } } }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("القرآن الكريم", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    CounterRow("صفحات قرأتها", currentDay.quranPages, step = 1, onChange = { n -> scope.launch { dao.saveDay(currentDay.copy(quranPages = n.coerceAtLeast(0), updatedAt = System.currentTimeMillis())) } })
                    CounterRow("الأجزاء المقروءة", currentDay.quranJuz, step = 1, onChange = { n -> scope.launch { dao.saveDay(currentDay.copy(quranJuz = n.coerceAtLeast(0), updatedAt = System.currentTimeMillis())) } })
                    Text("هدف الصفحات اليومي: ${settings?.quranPageGoal ?: 5} صفحة", color = Green, fontSize = 13.sp)
                    LinearProgressIndicator(progress = { ((currentDay.quranPages.toFloat() / (settings?.quranPageGoal ?: 5).coerceAtLeast(1)).coerceAtMost(1f)) }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الأذكار اليومية", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    ToggleRow("أذكار الصباح", currentDay.morningAdhkar) { v -> scope.launch { dao.saveDay(currentDay.copy(morningAdhkar = v)) } }
                    ToggleRow("أذكار المساء", currentDay.eveningAdhkar) { v -> scope.launch { dao.saveDay(currentDay.copy(eveningAdhkar = v)) } }
                    ToggleRow("أذكار بعد الصلاة", currentDay.afterPrayerAdhkar) { v -> scope.launch { dao.saveDay(currentDay.copy(afterPrayerAdhkar = v)) } }
                    ToggleRow("أذكار النوم", currentDay.sleepAdhkar) { v -> scope.launch { dao.saveDay(currentDay.copy(sleepAdhkar = v)) } }
                    Text("نصوص الأذكار والعبادات الشخصية والإحصائيات والنسخ الاحتياطي ستُستكمل في المرحلة التالية.", color = Color.Gray, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
        item {
            OutlinedButton(onClick = { /* Daily values are already persisted after every edit. */ }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)) {
                Text("ملخص اليوم — البيانات محفوظة تلقائيًا", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("بياناتك محفوظة محليًا على هذا الهاتف. لا توجد إشعارات أو مزامنة سحابية.", modifier = Modifier.fillMaxWidth().padding(top = 8.dp), color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(subtitle, fontSize = 12.sp, color = Color(0xFF68766F))
    }
}

@Composable private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = onChange)
    }
}

@Composable private fun CounterRow(label: String, value: Int, step: Int = 2, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange(value - step) }, enabled = value > 0) { Icon(Icons.Default.Remove, contentDescription = "تقليل") }
        Text(value.toString(), color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        IconButton(onClick = { onChange(value + step) }) { Icon(Icons.Default.Add, contentDescription = "إضافة") }
    }
}
