package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.disease.Disease
import com.example.data.disease.DiseaseDb
import com.example.data.disease.RiskLevel
import com.example.data.model.Batch
import com.example.data.model.DailyRecord
import com.example.ui.theme.TextGray
import com.example.data.model.MedsRecord
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FarmViewModel
import com.example.ui.viewmodel.KPIReport
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val viewModel: FarmViewModel = viewModel()
    val context = LocalContext.current

    // Observe DB States
    val batches by viewModel.allBatches.collectAsState()
    val selectedBatch by viewModel.selectedBatch.collectAsState()
    val dailyRecords by viewModel.dailyRecords.collectAsState()
    val diagnosisRecords by viewModel.diagnosisRecords.collectAsState()
    val medsRecords by viewModel.medsRecords.collectAsState()

    // Splash screen state
    var showSplash by remember { mutableStateOf(true) }

    // Navigation state: 0 = Batches, 1 = Daily Log, 2 = KPIs, 3 = AI Vet, 4 = Vaccination, 5 = Reports
    var selectedTab by remember { mutableIntStateOf(0) }

    // Dialog state
    var showCreateBatchDialog by remember { mutableStateOf(false) }

    if (showSplash) {
        SplashScreen { showSplash = false }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A), // Slate 900
                            Color(0xFF1E293B), // Slate 800
                            Color(0xFF090D16)  // Deep slate
                        )
                    )
                )
        ) {
            Scaffold(
                topBar = {
                    OptimusTopBar(
                        selectedBatch = selectedBatch,
                        onChangeBatch = { selectedTab = 0 }
                    )
                },
                bottomBar = {
                    OptimusBottomBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                },
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                when (selectedTab) {
                    0 -> BatchesScreen(
                        batches = batches,
                        selectedBatch = selectedBatch,
                        onSelectBatch = { viewModel.selectBatch(it) },
                        onDeleteBatch = { viewModel.deleteBatch(it) },
                        onCloseBatch = { viewModel.closeBatch(it) },
                        onAddBatchClick = { showCreateBatchDialog = true },
                        viewModel = viewModel
                    )
                    1 -> DailyLogScreen(
                        selectedBatch = selectedBatch,
                        records = dailyRecords,
                        onAddRecord = { date, age, mort, feed, water, temp, hum, amm, vent, weight ->
                            viewModel.addDailyRecord(date, age, mort, feed, water, temp, hum, amm, vent, weight)
                        },
                        onDeleteRecord = { viewModel.deleteDailyRecord(it) }
                    )
                    2 -> KpiAnalysisScreen(
                        selectedBatch = selectedBatch,
                        records = dailyRecords,
                        viewModel = viewModel
                    )
                    3 -> VetDiagnosisScreen(
                        selectedBatch = selectedBatch,
                        viewModel = viewModel
                    )
                    4 -> VaccinationScreen(
                        selectedBatch = selectedBatch,
                        meds = medsRecords,
                        onToggleMeds = { viewModel.toggleMedsCompleted(it) },
                        onAddMeds = { type, name, dosage, date, notes ->
                            viewModel.addMedsRecord(type, name, dosage, date, notes)
                        },
                        onDeleteMeds = { viewModel.deleteMedsRecord(it) }
                    )
                    5 -> ReportsScreen(
                        selectedBatch = selectedBatch,
                        viewModel = viewModel,
                        context = context
                    )
                }
            }
        }
    }
}

    if (showCreateBatchDialog) {
        CreateBatchDialog(
            onDismiss = { showCreateBatchDialog = false },
            onConfirm = { num, count, source, breed, type ->
                viewModel.createBatch(num, count, source, breed, type, System.currentTimeMillis())
                showCreateBatchDialog = false
            }
        )
    }
}

// --- VISUAL SPLASH SCREEN ---
@Composable
fun SplashScreen(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF090D16))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.lmar_vet_logo),
                contentDescription = "Lmar Vet Logo",
                modifier = Modifier
                    .size(170.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(2.dp, Color(0xFF10B981), RoundedCornerShape(32.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Lmar Vet ProFarm",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "POULTRY SERVICES",
                fontSize = 12.sp,
                letterSpacing = 4.sp,
                color = Color(0xFF10B981),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(
                modifier = Modifier.width(100.dp),
                thickness = 2.dp,
                color = Color(0xFF10B981)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "المستشار العلمي والمطور البيطري للبرنامج:",
                fontSize = 13.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "د. ضيف الله الحسني",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFABE2C),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(60.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0D6E3F),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp)
                    .testTag("get_started_btn")
            ) {
                Text(text = "البدء والتحليل ➔", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- CUSTOM M3 TOP APP BAR ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimusTopBar(selectedBatch: Batch?, onChangeBatch: () -> Unit) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.lmar_vet_logo),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "لومار فيت برو فارم",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Lmar Vet ProFarm",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        },
        actions = {
            if (selectedBatch != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x2210B981)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .border(1.dp, Color(0x3310B981), RoundedCornerShape(24.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF10B981), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "دفعة: ${selectedBatch.batchNumber}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
                IconButton(onClick = onChangeBatch) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "تغيير الدفعة",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

// --- OPTIMUS BOTTOM BAR ---
@Composable
fun OptimusBottomBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = Color(0xD90F172A), // Slate 900 Glassy
        tonalElevation = 0.dp,
        modifier = Modifier.border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x12FFFFFF), Color(0x02FFFFFF))
            ),
            shape = androidx.compose.ui.graphics.RectangleShape
        )
    ) {
        val items = listOf(
            NavigationItem("القطيع", Icons.Default.Home),
            NavigationItem("اليوميات", Icons.Default.Edit),
            NavigationItem("تحليل الأداء", Icons.Default.PlayArrow),
            NavigationItem("التشخيص AI", Icons.Default.Warning),
            NavigationItem("التحصينات", Icons.Default.DateRange),
            NavigationItem("التقارير", Icons.Default.List)
        )

        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = { 
                    Icon(
                        imageVector = item.icon, 
                        contentDescription = item.label, 
                        modifier = Modifier.size(24.dp)
                    ) 
                },
                label = { Text(text = item.label, fontSize = 9.sp, fontWeight = FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF10B981),
                    selectedTextColor = Color(0xFF10B981),
                    unselectedIconColor = Color(0xFF64748B),
                    unselectedTextColor = Color(0xFF64748B),
                    indicatorColor = Color(0x1A10B981)
                )
            )
        }
    }
}

data class NavigationItem(val label: String, val icon: ImageVector)

// --- SCREEN 1: BATCHES SCREEN ---
@Composable
fun BatchesScreen(
    batches: List<Batch>,
    selectedBatch: Batch?,
    onSelectBatch: (Batch) -> Unit,
    onDeleteBatch: (Batch) -> Unit,
    onCloseBatch: (Batch) -> Unit,
    onAddBatchClick: () -> Unit,
    viewModel: FarmViewModel
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "قائمة القطعان والدفعات الحالية",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
            Text(
                text = "اختر الدفعة النشطة لإدخال البيانات اليومية ومتابعة الأداء والتحصينات بكل عنبر مغلق.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                textAlign = TextAlign.Right
            )

            // Dynamic Stats Panel
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "إجمالي الدورات", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text(text = batches.size.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "الدورات النشطة", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text(text = batches.filter { !it.isClosed }.size.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "معايير النظام", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text(text = "مغلق محكم", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            if (batches.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا يوجد دفعات مسجلة حالياً.", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                        Text("انقر على زر + لبدء دورة جديدة.", color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
                    }
                }
            } else {
                batches.forEach { batch ->
                    val isSelected = selectedBatch?.id == batch.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onSelectBatch(batch) }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0x14FFFFFF),
                                shape = RoundedCornerShape(24.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (batch.isClosed) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color.LightGray),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "مغلقة",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.DarkGray,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "نشطة",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF065F46),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (isSelected) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                        ) {
                                            Text(
                                                "قيد العرض",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "دفعة: ${batch.batchNumber}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "${batch.initialChickCount} طير", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = "سلالة: ${batch.breed}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                                Text(text = "المصدر: ${batch.chickSource}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text(text = "بدء: ${sdf.format(Date(batch.startDate))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                if (!batch.isClosed) {
                                    OutlinedButton(
                                        onClick = { onCloseBatch(batch) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text("إغلاق الدورة", fontSize = 11.sp)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { onDeleteBatch(batch) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("حذف الدورة", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddBatchClick,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_batch_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة دفعة")
        }
    }
}

// --- DIALOG FOR CREATING NEW BATCH ---
@Composable
fun CreateBatchDialog(onDismiss: () -> Unit, onConfirm: (String, Int, String, String, String) -> Unit) {
    var num by remember { mutableStateOf("") }
    var countStr by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("Ross 308") }
    var systemType by remember { mutableStateOf("CLOSED") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "بدء دورة تربية جديدة 🐥",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
                Text(
                    text = "سيقوم البرنامج بجدولة التحصينات والجرعات الدوائية الوقائية الموصى بها تلقائياً للدورة الجديدة المفرزة.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    textAlign = TextAlign.Right
                )

                OutlinedTextField(
                    value = num,
                    onValueChange = { num = it },
                    label = { Text("رقم الدفعة المميز") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("dialog_batch_no"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true
                )

                OutlinedTextField(
                    value = countStr,
                    onValueChange = { countStr = it },
                    label = { Text("عدد الكتاكيت عند الاستلام") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("dialog_batch_count"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("مصدر الكتاكيت (الشركة)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    singleLine = true
                )

                Text("سلالة الدير اللحم:", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Ross 308", "Cobb 500", "Arbor Acres").forEach { b ->
                        FilterChip(
                            selected = breed == b,
                            onClick = { breed = b },
                            label = { Text(b) }
                        )
                    }
                }

                Text("نوع نظام رعاية العنبر:", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { systemType = "CLOSED" }) {
                        RadioButton(selected = systemType == "CLOSED", onClick = { systemType = "CLOSED" })
                        Text("نظام مغلق محكم (Closed)", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { systemType = "OPEN" }) {
                        RadioButton(selected = systemType == "OPEN", onClick = { systemType = "OPEN" })
                        Text("نظام مفتوح", fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.padding(end = 8.dp)) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            val count = countStr.toIntOrNull() ?: 10000
                            onConfirm(num.ifEmpty { "غير مسمى" }, count, source.ifEmpty { "غير محدد" }, breed, systemType)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        enabled = num.isNotEmpty()
                    ) {
                        Text("حفظ والبدء")
                    }
                }
            }
        }
    }
}

// --- SCREEN 2: DAILY LOG SHEET ---
@Composable
fun DailyLogScreen(
    selectedBatch: Batch?,
    records: List<DailyRecord>,
    onAddRecord: (Long, Int, Int, Double, Double, Double, Double, Double, Double, Double) -> Unit,
    onDeleteRecord: (Int) -> Unit
) {
    if (selectedBatch == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(16.dp))
                Text("الرجاء تحديد دفعة نشطة أولاً.", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("توجه لتبويب (القطيع) لإنشاء أو تحديد دورة تربية.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
        return
    }

    var ageVal by remember { mutableStateOf("") }
    var mortVal by remember { mutableStateOf("") }
    var feedVal by remember { mutableStateOf("") }
    var waterVal by remember { mutableStateOf("") }
    var tempVal by remember { mutableStateOf("") }
    var humVal by remember { mutableStateOf("") }
    var ammVal by remember { mutableStateOf("") }
    var ventVal by remember { mutableStateOf("") }
    var weightVal by remember { mutableStateOf("") }

    // Prepopulate expected age based on start date
    LaunchedEffect(records, selectedBatch) {
        val calculatedAge = if (records.isNotEmpty()) {
            records.last().ageInDays + 1
        } else {
            val diff = System.currentTimeMillis() - selectedBatch.startDate
            ((diff / (24 * 60 * 60 * 1000L)).toInt() + 1).coerceAtLeast(1)
        }
        ageVal = calculatedAge.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = "تسجيل السجل اليومي للعنبر المغلق 📝",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "أدخل بيانات الاستهلاك والمناخ اليومية بدقة لقياس الكفاءة والتحويل.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Form Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = ageVal,
                        onValueChange = { ageVal = it },
                        label = { Text("عمر الطير (يوم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).padding(end = 6.dp).testTag("input_age")
                    )
                    OutlinedTextField(
                        value = mortVal,
                        onValueChange = { mortVal = it },
                        label = { Text("عدد النافق (رأس)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).padding(start = 6.dp).testTag("input_mortality")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = feedVal,
                        onValueChange = { feedVal = it },
                        label = { Text("العلف المستهلك (كجم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).padding(end = 6.dp).testTag("input_feed")
                    )
                    OutlinedTextField(
                        value = waterVal,
                        onValueChange = { waterVal = it },
                        label = { Text("الماء المستهلك (لتر)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).padding(start = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = tempVal,
                        onValueChange = { tempVal = it },
                        label = { Text("درجة الحرارة (°م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).padding(end = 6.dp)
                    )
                    OutlinedTextField(
                        value = humVal,
                        onValueChange = { humVal = it },
                        label = { Text("الرطوبة النسبية (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).padding(start = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = ammVal,
                        onValueChange = { ammVal = it },
                        label = { Text("الأمونيا (PPM)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).padding(end = 6.dp)
                    )
                    OutlinedTextField(
                        value = ventVal,
                        onValueChange = { ventVal = it },
                        label = { Text("معدل التهوية (m³/h)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).padding(start = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = weightVal,
                    onValueChange = { weightVal = it },
                    label = { Text("متوسط وزن الطير الحالي (جرام)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("input_weight")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val age = ageVal.toIntOrNull() ?: 1
                        val mort = mortVal.toIntOrNull() ?: 0
                        val feed = feedVal.toDoubleOrNull() ?: 0.0
                        val water = waterVal.toDoubleOrNull() ?: 0.0
                        val temp = tempVal.toDoubleOrNull() ?: 28.0
                        val hum = humVal.toDoubleOrNull() ?: 60.0
                        val amm = ammVal.toDoubleOrNull() ?: 5.0
                        val vent = ventVal.toDoubleOrNull() ?: 1000.0
                        val weight = weightVal.toDoubleOrNull() ?: 0.0

                        onAddRecord(System.currentTimeMillis(), age, mort, feed, water, temp, hum, amm, vent, weight)

                        // Clear inputs
                        mortVal = ""
                        feedVal = ""
                        waterVal = ""
                        tempVal = ""
                        humVal = ""
                        ammVal = ""
                        ventVal = ""
                        weightVal = ""
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_daily_record"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("حفظ سجل اليوم بمدخلات دقيقة", fontWeight = FontWeight.Bold)
                }
            }
        }

        // List of entered logs
        Text(
            text = "تاريخ السجلات الموثقة بالدورة",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        if (records.isEmpty()) {
            Text(
                "أدخل سجل اليوم الأول لبناء سجل الأداء وتحاليل FCR.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            records.sortedByDescending { it.date }.forEach { rec ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onDeleteRecord(rec.id) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "عمر ${rec.ageInDays} أيام", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(rec.date)),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "النافق: ${rec.mortalityCount} | علف: ${rec.feedConsumptionKg} كجم | وزن: ${rec.averageWeightGrams} جرام",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "الحرارة: ${rec.temperatureCelsius}°م | رطوبة: ${rec.humidityPercent}% | أمونيا: ${rec.ammoniaLevelPpm}ppm",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// --- SCREEN 3: KPI & PERFORMANCE ANALYSIS SCREEN ---
@Composable
fun KpiAnalysisScreen(selectedBatch: Batch?, records: List<DailyRecord>, viewModel: FarmViewModel) {
    if (selectedBatch == null || records.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(16.dp))
                Text("لم تسجل أي بيانات يومية بعد لحساب مؤشرات الأداء.", fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("من فضلك أدخل سجل يومي لبدء المحلل الفني للقطيع.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
            }
        }
        return
    }

    val kpis = viewModel.calculateKPIs()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = "لوحة قياس الأداء والتحليلات الفنية (Aviagen KPIs) 📊",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "مؤشرات الكفاءة الحقلية المحسوبة رقمياً بموجب مقاييس التربية المغلقة العالمية.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // EPI Card - Big Hero
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "مؤشر الكفاءة الإنتاجية الكلي (EPEF / EPI)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format("%.1f", kpis.epi),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // EPI Interpretation
                val interpretation = when {
                    kpis.epi >= 350.0 -> "كفاءة إنتاجية ممتازة ومثالية جداً! تهوية ورعاية متزنة!"
                    kpis.epi >= 300.0 -> "كفاءة جيدة جداً، استمر بضبط التهوية والحرارة وتفقد النافق."
                    else -> "كفاءة متوسطة أو متدنية. ابحث عن مسببات تراجع FCR وعالجها مع د. ضيف الله."
                }
                Text(
                    text = interpretation,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Primary KPIs row
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Card(
                modifier = Modifier.weight(1f).padding(end = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("معدل التحويل (FCR)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Text(String.format("%.2f", kpis.fcr), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (kpis.fcr > 1.8) Color.Red else Color(0xFF0D6E3F))
                    Text("معيار: 1.50 مثالي", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
            Card(
                modifier = Modifier.weight(1f).padding(start = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("النافق الإجمالي", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Text("${String.format("%.2f", kpis.mortalityPercent)}%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (kpis.mortalityPercent > 5.0) Color.Red else Color(0xFF10B981))
                    Text("${kpis.totalMortality} من ${kpis.initialChicks} كتكوت", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Card(
                modifier = Modifier.weight(1.0f).padding(end = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("النمو اليومي (ADG)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Text("${String.format("%.1f", kpis.adg)} جم", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("متوسط زيادة وزن طائر", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
            Card(
                modifier = Modifier.weight(1.0f).padding(start = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("كثافة التحميل", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Text("${String.format("%.1f", kpis.stockingDensity)} رأس/م²", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Text("المساحة الافتراضية: 500م²", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        // Custom ventilation and climate warns
        Text("تنبيهات حالة بيئة العنبر المغلق:", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        
        // Temperature check
        val tempColor = if (kpis.latestTemp > 31.0 || kpis.latestTemp < 19.0) Color(0xFFEF4444) else Color(0xFF10B981)
        val tempAdvice = if (kpis.latestTemp > 31.0) "تحذير: حرارة العنبر مرتفعة جداً! شغل خلايا التبريد ومراوح الشفط فوراً." else "درجة حرارة ممتازة ومثالية للعنبر."
        ClimateAlertCard(title = "أحدث درجة حرارة: ${kpis.latestTemp}°م", advice = tempAdvice, indicatorColor = tempColor)

        // Ammonia check
        val ammColor = if (kpis.latestAmmonia > 15.0) Color(0xFFEF4444) else Color(0xFF10B981)
        val ammAdvice = if (kpis.latestAmmonia > 15.0) "تحذير: غاز الأمونيا مرتفع وخانق (${kpis.latestAmmonia} PPM)! خطر الإصابة بالكولي وبكتيريا الإي كولاي. ضاعف معدل التهوية." else "مستوى غاز الأمونيا آمن وممتاز دون خطورة."
        ClimateAlertCard(title = "مستوى غاز الأمونيا: ${kpis.latestAmmonia} PPM", advice = ammAdvice, indicatorColor = ammColor)

        // Humidity check
        val humColor = if (kpis.latestHumidity > 70.0 || kpis.latestHumidity < 40.0) Color(0xFFFABE2C) else Color(0xFF10B981)
        val humAdvice = if (kpis.latestHumidity > 70.0) "الرطوبة مرتفعة (${kpis.latestHumidity}%). قد تفور الكوكسيديا بالفرشة. قلل التبريد وزد التهوية." else "الرطوبة النسبية آمنة للقطيع."
        ClimateAlertCard(title = "مستوى الرطوبة النسبية: ${kpis.latestHumidity}%", advice = humAdvice, indicatorColor = humColor)

        // Pseudo Graph Visualization (Weight Gain Progress)
        Spacer(modifier = Modifier.height(16.dp))
        Text("مؤشر تطور الأوزان بمعدلات الدورة 📈", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                records.takeLast(7).forEach { rec ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "يوم ${rec.ageInDays}", fontSize = 11.sp, modifier = Modifier.width(45.dp))
                        
                        // Render progress bar mimicking the weight gain relative to top standard (e.g., target 2500g)
                        val maxTargetWeight = 2500.0
                        val progressFraction = (rec.averageWeightGrams / maxTargetWeight).coerceIn(0.0, 1.0).toFloat()
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressFraction)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFF10B981))
                                        )
                                    )
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "${rec.averageWeightGrams} جرام", fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.width(70.dp))
                    }
                }
            }
        }

        // --- NEW FEATURE: ADVANCED EPIDEMIOLOGICAL DISEASE PREDICTION ---
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "مجمّع التنبؤ الاستباقي والأوبئة المستقبلية (Epidemiological AI Predictor) 🔮",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "توقع إحصائي مبني على خوارزميات بيطرية تدرس ترابط السلالة والعمر بالفوارق المفاجئة للحرارة، استهلاك المياه، والأمونيا.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val predictionResults by viewModel.predictionResults.collectAsState()
        val aiPredictionReport by viewModel.aiPredictionReport.collectAsState()
        val isAiPredicting by viewModel.isAiPredicting.collectAsState()

        if (predictionResults.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x13FFFFFF)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "لا توجد بيانات كافية للتنبؤ بالمستقبل. أدخل المزيد من السجلات اليومية (الحرارة، الرطوبة، الأمونيا، الأكل والماء).",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            predictionResults.forEach { result ->
                val cardBorderColor = when (result.riskLevel) {
                    RiskLevel.HIGH -> Color(0x33EF4444)
                    RiskLevel.MEDIUM -> Color(0x33F59E0B)
                    RiskLevel.LOW -> Color(0x15FFFFFF)
                }
                val riskBadgeColor = when (result.riskLevel) {
                    RiskLevel.HIGH -> Color(0xFFEF4444)
                    RiskLevel.MEDIUM -> Color(0xFFF59E0B)
                    RiskLevel.LOW -> Color(0xFF94A3B8)
                }
                val riskBadgeText = when (result.riskLevel) {
                    RiskLevel.HIGH -> "خطر مرتفع ⚠️"
                    RiskLevel.MEDIUM -> "خطر متوسط ⚠️"
                    RiskLevel.LOW -> "خطر منخفض ✓"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .border(1.dp, cardBorderColor, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = when (result.riskLevel) {
                            RiskLevel.HIGH -> Color(0x11EF4444)
                            RiskLevel.MEDIUM -> Color(0x0CF59E0B)
                            RiskLevel.LOW -> Color(0x05FFFFFF)
                        }
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = riskBadgeColor.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text(
                                    text = "$riskBadgeText (${result.probability}%)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = riskBadgeColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = result.disease.nameAr,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Right
                            )
                        }

                        if (result.riskFactors.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "مسببات ومحفزات الخطر المستكشفة بالأرقام:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            result.riskFactors.forEach { factor ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = factor,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(riskBadgeColor, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Veterinary Prevention Deep Dive button
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x1410B981)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(1.dp, Color(0x3310B981), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "دفاع الأمن الحيوي الذكي (إرشاد د. ضيف الله) 🛡️",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF10B981)
                    )
                    Text(
                        text = "توليد مصفوفة وقائية عاجلة صممت خصيصاً على معايير دورتك الحالية وتنبؤات الخطر لصد الوباء وتفادي تراجع FCR.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    Button(
                        onClick = { viewModel.getAIPredictionPlan() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        enabled = !isAiPredicting,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isAiPredicting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري توليد الدفاع الذكي الاستباقي...", fontSize = 11.sp)
                        } else {
                            Text("اصنع خطة وقائية استباقية بالذكاء الاصطناعي 🔮", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    aiPredictionReport?.let { report ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x0CFFFFFF)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(20.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "التقرير التنبؤي التوليدي للتصدي المبكر:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFABE2C),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = report,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.95f),
                                    textAlign = TextAlign.Right,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun ClimateAlertCard(title: String, advice: String, indicatorColor: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(indicatorColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = advice, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

// --- SCREEN 4: INTELLIGENT VET DIAGNOSIS SHEET ---
@Composable
fun VetDiagnosisScreen(selectedBatch: Batch?, viewModel: FarmViewModel) {
    val selectedSymptoms by viewModel.selectedSymptoms.collectAsState()
    val results by viewModel.diagnosisResults.collectAsState()
    val aiResponse by viewModel.aiVetResponse.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    var showQueryDialog by remember { mutableStateOf(false) }
    var userCustomQuery by remember { mutableStateOf("") }

    if (selectedBatch == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(16.dp))
                Text("يرجى تحديد دفعة أولاً لقياس أعمار ومؤشرات الطيور.", fontSize = 14.sp)
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = "مساعد التشخيص البيطري الذكي 🩺",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "اختر الأعراض الظاهرة والتشريحية على الطيور لمعرفة المرض المحتمل، بروتوكولاته، والوقاية الحيوية.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Symptoms Selector Section Flow
        Text(
            text = "حدد جميع الأعراض الملاحظة بالحظيرة المعزولة:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Flow-like symptom items as togglable chips
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(24.dp))
                .background(Color(0x0FFFFFFF), RoundedCornerShape(24.dp))
                .padding(12.dp)
        ) {
            Column {
                // Grouping them beautifully
                val chunkedSymptoms = DiseaseDb.symptomsPoolAr.chunked(3)
                chunkedSymptoms.forEach { rowSymptoms ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowSymptoms.forEach { symptom ->
                            val isSelected = selectedSymptoms.contains(symptom)
                            Card(
                                onClick = { viewModel.toggleSymptom(symptom) },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0x13FFFFFF)
                                ),
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .weight(1f)
                                    .border(1.dp, if (isSelected) Color.Transparent else Color(0x18FFFFFF), RoundedCornerShape(20.dp))
                            ) {
                                Text(
                                    text = symptom,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(
                onClick = { viewModel.clearSymptoms() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("تصفية الأعراض", fontSize = 11.sp)
            }

            Text(
                text = "الأعراض المختارة: ${selectedSymptoms.size}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "النتائج والتشخيص المحتمل (التوافق الرياضي):",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (results.isEmpty()) {
            Text(
                "رصد مفرز: الرجاء النقر فوق بعض الأعراض كالإسهال الأبيض أو التواء الرقبة لتحديد الاحتمال.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                textAlign = TextAlign.Center
            )
        } else {
            results.forEach { (disease, prob) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFECEB)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "$prob%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = disease.nameAr,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Right
                            )
                        }
                        Text(
                            text = "النوع: ${disease.typeAr} | الشريحة العمرية: ${disease.ageGroupAr}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Text(text = "عوامل الخطر / المولدات:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(text = disease.causesAr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 8.dp))

                        Text(text = "البروتوكول العلاجي الدوائي والطبيعي المقترح:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(text = disease.treatmentAr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 8.dp))

                        Text(text = "إجراءات الأمن الحيوي للوقاية (الأنظمة المغلقة):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(text = disease.preventionAr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Button(
                                onClick = { viewModel.saveDiagnosisResult(disease.nameAr, prob) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("حفظ التشريج بسجل اليوم", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // EXTRA PRESTIGE FIELD: THE GEMINI AI VET CONSULTATION CARD
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x223B82F6)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillModifier()
                    .padding(bottom = 24.dp)
                    .border(1.dp, Color(0x333B82F6), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFFFABE2C), modifier = Modifier.size(28.dp))
                        Text(
                            "استشارة طبيب الذكاء الاصطناعي (Gemini) 🤖",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "يقوم النموذج بتحليل الأعراض المحددة، وعمر دجاج الدورة لإنشاء بروتوكول تبريد وعزل مخصص للأمن الحيوي بإرشاد د. ضيف الله.",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { showQueryDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFABE2C), contentColor = Color(0xFF0C2B4E))
                        ) {
                            Text("اسأل سؤال خاص ✎", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.consultAiVet() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            enabled = !isAiLoading
                        ) {
                            if (isAiLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                            } else {
                                Text("تحليل القطيع الفوري ⚡", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Answer Container
                    if (aiResponse != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "تقرير الاستشارة البيطرية الذكية:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFABE2C),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = aiResponse ?: "",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showQueryDialog) {
        Dialog(onDismissRequest = { showQueryDialog = false }) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                    Text("اسأل الطبيب البيطري الذكي سؤال مخصص:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = userCustomQuery,
                        onValueChange = { userCustomQuery = it },
                        label = { Text("مثال: ما تأثير غاز الأمونيا على التهوية السقفية؟") },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showQueryDialog = false }) {
                            Text("رجوع")
                        }
                        Button(
                            onClick = {
                                viewModel.consultAiVet(userCustomQuery)
                                showQueryDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("إرسال بذكاء")
                        }
                    }
                }
            }
        }
    }
}

fun Modifier.fillModifier() = this.fillMaxWidth()

// --- SCREEN 5: VACCINATIONS & DRUGS TIMELINE SCREEN ---
@Composable
fun VaccinationScreen(
    selectedBatch: Batch?,
    meds: List<MedsRecord>,
    onToggleMeds: (MedsRecord) -> Unit,
    onAddMeds: (String, String, String, Long, String) -> Unit,
    onDeleteMeds: (Int) -> Unit
) {
    if (selectedBatch == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(16.dp))
                Text("يرجى تحديد دفعة أولى لتتبع جدول تحصيناتها الدقيق.", fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        }
        return
    }

    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = "برامج التحصينات والجرعات الوقائية للقطيع 💉",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "تم بناء هذا الجدول الوقائي لخدمتك تلقائياً وفقاً لتوجيهات د. ضيف الله الحسني لقطع دجاج اللحم بالفلاحة المغلقة.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("+ إضافة لقاح/دواء مخصص", fontSize = 11.sp)
            }

            Text(
                text = "إجمالي اللقاحات: ${meds.size}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Timeline listing
        if (meds.isEmpty()) {
            Text(
                "أضف لقاح أو تمتع بالبرنامج التلقائي للدورة.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                textAlign = TextAlign.Center
            )
        } else {
            meds.sortedBy { it.date }.forEach { item ->
                val remainingDays = ((item.date - System.currentTimeMillis()) / (24 * 60 * 60 * 1000L)).toInt()
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.isCompleted) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Action checkbox
                        Checkbox(
                            checked = item.isCompleted,
                            onCheckedChange = { onToggleMeds(item) },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981))
                        )

                        // Column info
                        Column(
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (item.isCompleted) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "تم التحصين",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF065F46),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    val badgeColor = if (remainingDays < 0) Color(0xFFFEE2E2) else Color(0xFFFFEDD5)
                                    val badgeTextColor = if (remainingDays < 0) Color(0xFF991B1B) else Color(0xFF9A3412)
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = badgeColor),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (remainingDays < 0) "متأخر" else "قادم بعد $remainingDays يوم",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeTextColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (item.type == "VACCINE") "تحصين 💉" else "جرعة دواء 💊",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.type == "VACCINE") Color(0xFF0D6E3F) else Color(0xFF0C2B4E)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Right
                            )
                            Text(
                                text = "الجرعة: ${item.dosage}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Right
                            )
                            Text(
                                text = "ملاحظة: ${item.notes}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Right
                            )
                            
                            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                            Text(
                                text = "التاريخ المخطط: ${sdf.format(Date(item.date))}",
                                fontSize = 9.sp,
                                color = TextGray
                            )

                            if (item.isCompleted && item.actualDate != null) {
                                Text(
                                    text = "تمت الرعاية في: ${sdf.format(Date(item.actualDate))}",
                                    fontSize = 9.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(onClick = { onDeleteMeds(item.id) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف اللقاح", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showAddDialog) {
        var medName by remember { mutableStateOf("") }
        var medDosage by remember { mutableStateOf("") }
        var medNotes by remember { mutableStateOf("") }
        var medType by remember { mutableStateOf("VACCINE") }
        var daysInFuture by remember { mutableStateOf("1") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                    Text("إضافة تحصين أو دواء مخصص للدفعة:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { medType = "VACCINE" }) {
                            RadioButton(selected = medType == "VACCINE", onClick = { medType = "VACCINE" })
                            Text("تحصين لقاح", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { medType = "MEDICINE" }) {
                            RadioButton(selected = medType == "MEDICINE", onClick = { medType = "MEDICINE" })
                            Text("علاج طبيعي/دوائي", fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = medName,
                        onValueChange = { medName = it },
                        label = { Text("الاسم (مثال: نيوكاسل لاسوتة)") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = medDosage,
                        onValueChange = { medDosage = it },
                        label = { Text("الجرعة وطريقة الإعطاء") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = daysInFuture,
                        onValueChange = { daysInFuture = it },
                        label = { Text("تخطيط الإعطاء بعد م كم يوم من اليوم؟") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = medNotes,
                        onValueChange = { medNotes = it },
                        label = { Text("ملاحظات إرشادية للمربين بالعنابر") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("إلغاء")
                        }
                        Button(
                            onClick = {
                                val dayCount = daysInFuture.toLongOrNull() ?: 1L
                                val dateMs = System.currentTimeMillis() + (dayCount * 24 * 60 * 60 * 1000L)
                                onAddMeds(medType, medName, medDosage, dateMs, medNotes)
                                showAddDialog = false
                            },
                            enabled = medName.isNotEmpty()
                        ) {
                            Text("حفظ وجدولة")
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN 6: REPORTS & DEVELOPER SECTION ---
@Composable
fun ReportsScreen(selectedBatch: Batch?, viewModel: FarmViewModel, context: Context) {
    if (selectedBatch == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(16.dp))
                Text("يرجى تحديد دفعة أولاً لعرض التقرير وطباعته.", fontSize = 14.sp)
            }
        }
        return
    }

    val kpis = viewModel.calculateKPIs()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = "مركز طباعة تقارير الأداء والأمن الحيوي 📄",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "قم بتجميع وتصدير ملف الأداء الإنتاجي المتكامل، والوضع الصحي الشامل للقطيع، وجداول التحصينات السنوية في شكل مستند PDF طبي فاخر جاهز للطباعة الفورية والمشاركة بلمسة واحدة.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Summary Card visualizer inside report tab
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.End) {
                Text(
                    text = "ملخص الدورة الحالية (دفعة ${selectedBatch.batchNumber})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                ReportRowAr("حالة الدورة المتزامنة:", if (selectedBatch.isClosed) "مغلقة ومكتملة" else "نشطة وقيد النمو")
                ReportRowAr("إجمالي الكتاكيت المستلمة:", "${selectedBatch.initialChickCount} فرخ")
                ReportRowAr("إجمالي وفيات القطيع الحالية:", "${kpis.totalMortality} طير (${String.format("%.2f", kpis.mortalityPercent)}%)")
                ReportRowAr("إجمالي الطيور الحية الحالية:", "${kpis.remainingBirds} طير (${String.format("%.2f", kpis.livabilityPercent)}%)")
                ReportRowAr("متوسط الوزن المسجل بالفحص:", "${kpis.avgWeightGrams} جرام")
                ReportRowAr("معدل التحويل التراكمي (FCR):", String.format("%.2f", kpis.fcr))
                ReportRowAr("الكفاءة الإنتاجية الكلية (EPI):", String.format("%.1f", kpis.epi))
                
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.generateAndSharePDFReport(context) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تصدير تقرير الـ PDF المطبوع والمشاركة الحية", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Dr. Daifallah profile Card
        Text(
            text = "المستشار العلمي والبيطري للبرنامج:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Placeholder representing doctor profile
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "د. ضيف الله الحسني",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "طبيب ومستشار بيطري متخصص في رعاية قطعان اللاحم والبياض وإدارة عنابر الأنظمة المغلقة الحديثة والأمن الحيوي المطور.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun ReportRowAr(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
    }
}
