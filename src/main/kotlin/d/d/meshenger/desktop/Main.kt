/*
 * Copyright (C) 2026 Meshenger Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package d.d.meshenger.desktop

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import d.d.meshenger.desktop.contact.Contact
import d.d.meshenger.desktop.disaster.DisasterModeManager
import d.d.meshenger.desktop.disaster.DisasterSignal
import d.d.meshenger.desktop.disaster.RubbleAudioProcessor
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

fun main() = application {
    val windowState = rememberWindowState(width = 1100.dp, height = 820.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Meshenger Windows - P2P Emergency Network & Disaster Beacon (ISO 22324)",
        state = windowState
    ) {
        MeshengerAppTheme {
            MainDesktopScreen()
        }
    }
}

@Composable
fun MeshengerAppTheme(content: @Composable () -> Unit) {
    val darkColors = darkColorScheme(
        primary = Color(0xFF4F46E5),
        secondary = Color(0xFFDC2626),
        background = Color(0xFF0F172A),
        surface = Color(0xFF1E293B),
        onPrimary = Color.White,
        onBackground = Color.White,
        onSurface = Color.White
    )
    MaterialTheme(colorScheme = darkColors, content = content)
}

enum class NavTab {
    DISASTER_BEACON,
    CONTACTS,
    CALLS,
    ABOUT
}

@Composable
fun MainDesktopScreen() {
    var selectedTab by remember { mutableStateOf(NavTab.DISASTER_BEACON) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            HeaderBar()

            // Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White
            ) {
                Tab(
                    selected = selectedTab == NavTab.DISASTER_BEACON,
                    onClick = { selectedTab = NavTab.DISASTER_BEACON },
                    text = { Text("📡 AFET KİPİ / DISASTER BEACON", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == NavTab.CONTACTS,
                    onClick = { selectedTab = NavTab.CONTACTS },
                    text = { Text("👥 KİŞİLER / CONTACTS", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == NavTab.CALLS,
                    onClick = { selectedTab = NavTab.CALLS },
                    text = { Text("📞 ARAMALAR / CALLS", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == NavTab.ABOUT,
                    onClick = { selectedTab = NavTab.ABOUT },
                    text = { Text("ℹ️ HAKKINDA / ABOUT", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }

            // Tab Body Content
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                when (selectedTab) {
                    NavTab.DISASTER_BEACON -> DisasterBeaconTabScreen()
                    NavTab.CONTACTS -> ContactsTabScreen()
                    NavTab.CALLS -> CallsTabScreen()
                    NavTab.ABOUT -> AboutTabScreen()
                }
            }
        }
    }
}

@Composable
fun HeaderBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFDC2626))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "🚨 MESHENGER WINDOWS - ACİL DURUM & AFET KİPİ",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "P2P Serverless Emergency Network • ISO 22324 International Standard Compliant",
                color = Color(0xFFFEE2E2),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun DisasterBeaconTabScreen() {
    var isBeaconActive by remember { mutableStateOf(DisasterModeManager.isDisasterModeActive()) }
    var currentStatus by remember { mutableStateOf(DisasterModeManager.currentStatus) }
    var medicalNotes by remember { mutableStateOf(DisasterModeManager.medicalNotes) }
    var isWhistleActive by remember { mutableStateOf(DisasterModeManager.isWhistleActive()) }
    var isStrobeActive by remember { mutableStateOf(DisasterModeManager.isStrobeActive()) }

    var isRubbleListening by remember { mutableStateOf(RubbleAudioProcessor.isListeningActive()) }
    var gainMultiplier by remember { mutableStateOf(RubbleAudioProcessor.gainMultiplier.toInt()) }
    var audioAmplitude by remember { mutableStateOf(0) }
    var isPeakWarningDetected by remember { mutableStateOf(false) }

    var activeSignals by remember { mutableStateOf(listOf<DisasterSignal>()) }

    // Register audio amplitude listener
    DisposableEffect(Unit) {
        RubbleAudioProcessor.setOnAudioAmplitudeListener(object : RubbleAudioProcessor.OnAudioAmplitudeListener {
            override fun onAmplitudeChanged(amplitudePercentage: Int, peakDetected: Boolean) {
                audioAmplitude = amplitudePercentage
                isPeakWarningDetected = peakDetected
            }
        })
        onDispose { RubbleAudioProcessor.setOnAudioAmplitudeListener(null) }
    }

    // Register signal updates
    DisposableEffect(Unit) {
        val listener = object : DisasterModeManager.OnSignalReceivedListener {
            override fun onSignalsUpdated(signals: List<DisasterSignal>) {
                activeSignals = signals
            }
        }
        DisasterModeManager.registerListener(listener)
        onDispose { DisasterModeManager.unregisterListener(listener) }
    }

    // Strobe Flash Window handling
    if (isStrobeActive) {
        StrobeOverlayWindow(onClose = {
            DisasterModeManager.stopStrobe()
            isStrobeActive = false
        })
    }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // Left Column: Controls
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Beacon Main Switch Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("📡 AFET KİPİ YAYINI (DISASTER BEACON)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        val statusText = if (isBeaconActive) "Yayın Açık (Wi-Fi UDP & Mesh Active)" else "Yayın Kapalı / Broadcast Inactive"
                        val statusColor = if (isBeaconActive) Color(0xFF4ADE80) else Color(0xFF94A3B8)
                        Text(statusText, color = statusColor, fontSize = 12.sp)
                    }
                    Switch(
                        checked = isBeaconActive,
                        onCheckedChange = { checked ->
                            isBeaconActive = checked
                            if (checked) {
                                DisasterModeManager.startDisasterMode()
                            } else {
                                DisasterModeManager.stopDisasterMode()
                                if (RubbleAudioProcessor.isListeningActive()) {
                                    RubbleAudioProcessor.stopListening()
                                    isRubbleListening = false
                                }
                            }
                        }
                    )
                }
            }

            // Interactive Controls Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("⚠️ ACİL DURUM / EMERGENCY STATUS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                // Radio Status Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusOptionButton(
                        text = "🔴 SOS / HELP",
                        selected = currentStatus == DisasterSignal.StatusType.HELP,
                        activeBgColor = Color(0xFFDC2626),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isBeaconActive) {
                                currentStatus = DisasterSignal.StatusType.HELP
                                DisasterModeManager.currentStatus = currentStatus
                            }
                        }
                    )
                    StatusOptionButton(
                        text = "🔵 MEDICAL",
                        selected = currentStatus == DisasterSignal.StatusType.MEDICAL,
                        activeBgColor = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isBeaconActive) {
                                currentStatus = DisasterSignal.StatusType.MEDICAL
                                DisasterModeManager.currentStatus = currentStatus
                            }
                        }
                    )
                    StatusOptionButton(
                        text = "🟢 SAFE",
                        selected = currentStatus == DisasterSignal.StatusType.SAFE,
                        activeBgColor = Color(0xFF16A34A),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isBeaconActive) {
                                currentStatus = DisasterSignal.StatusType.SAFE
                                DisasterModeManager.currentStatus = currentStatus
                            }
                        }
                    )
                }

                // Medical Notes Field
                OutlinedTextField(
                    value = medicalNotes,
                    onValueChange = {
                        medicalNotes = it
                        DisasterModeManager.medicalNotes = it
                    },
                    label = { Text("📋 Emergency Medical & Location Notes") },
                    placeholder = { Text("Örn: Floor 3, Rubble Trap / Blood A Rh+") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Whistle & Strobe Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (isWhistleActive) {
                                DisasterModeManager.stopWhistle()
                                isWhistleActive = false
                            } else {
                                DisasterModeManager.startWhistle()
                                isWhistleActive = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isWhistleActive) Color.Red else Color(0xFFD97706)),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(if (isWhistleActive) "🔊 SIREN DURDUR" else "🔊 3.5 kHz DÜDÜK", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (isStrobeActive) {
                                DisasterModeManager.stopStrobe()
                                isStrobeActive = false
                            } else {
                                DisasterModeManager.startStrobe()
                                isStrobeActive = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isStrobeActive) Color.Red else Color(0xFF4F46E5)),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(if (isStrobeActive) "🔦 FLAŞ DURDUR" else "🔦 STROBE FLAŞ", fontWeight = FontWeight.Bold)
                    }
                }

                // 2. Rubble Audio Listener Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("🎙️ ENKAZ DİNLEME & SES YÜKSELTİCİ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                val rubbleText = if (isRubbleListening) "Dinleme Açık (${gainMultiplier}x Kazanç • Gürültü Filtreli)" else "Dinleme Kapalı / Listener Inactive"
                                Text(rubbleText, color = Color(0xFFA5B4FC), fontSize = 12.sp)
                            }
                            Switch(
                                checked = isRubbleListening,
                                onCheckedChange = { checked ->
                                    isRubbleListening = checked
                                    if (checked) {
                                        RubbleAudioProcessor.startListening()
                                    } else {
                                        RubbleAudioProcessor.stopListening()
                                    }
                                }
                            )
                        }

                        if (isRubbleListening) {
                            // Gain Selector Buttons
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🔊 KAZANÇ:", color = Color(0xFFC7D2FE), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                GainButton("3x", selected = gainMultiplier == 3) {
                                    RubbleAudioProcessor.gainMultiplier = 3.0f
                                    gainMultiplier = 3
                                }
                                GainButton("5x", selected = gainMultiplier == 5) {
                                    RubbleAudioProcessor.gainMultiplier = 5.0f
                                    gainMultiplier = 5
                                }
                                GainButton("10x (MAX)", selected = gainMultiplier == 10) {
                                    RubbleAudioProcessor.gainMultiplier = 10.0f
                                    gainMultiplier = 10
                                }
                            }

                            // Amplitude Meter & Warning Text
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                LinearProgressIndicator(
                                    progress = { audioAmplitude / 100f },
                                    modifier = Modifier.fillMaxWidth().height(10.dp),
                                    color = if (isPeakWarningDetected) Color(0xFFEF4444) else Color(0xFF818CF8),
                                    trackColor = Color(0xFF312E81)
                                )
                                Text(
                                    text = if (isPeakWarningDetected) "⚠️ YÜKSEK SES / TIKIRTI ALGILANDI! (PEAK DETECTED)" else "Ortam Dinleniyor... / Monitoring Audio...",
                                    color = if (isPeakWarningDetected) Color(0xFFEF4444) else Color(0xFF818CF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right Column: Live Beacon Signal Network
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("📡 DISASTER BEACON NETWORK (CANLI SİNYALLER)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

            if (activeSignals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E293B), shape = RoundedCornerShape(8.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Henüz çevrede aktif bir acil durum sinyali algılanmadı / No active beacon detected.",
                        color = Color(0xFF9CA3AF),
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeSignals) { signal ->
                        DisasterSignalCard(signal)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusOptionButton(text: String, selected: Boolean, activeBgColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bgColor by animateColorAsState(if (selected) activeBgColor else Color(0xFF334155))
    Box(
        modifier = modifier
            .height(44.dp)
            .background(bgColor, shape = RoundedCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun GainButton(text: String, selected: Boolean, onClick: () -> Unit) {
    val bgColor = if (selected) Color(0xFF4338CA) else Color(0xFF312E81)
    Box(
        modifier = Modifier
            .background(bgColor, shape = RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DisasterSignalCard(signal: DisasterSignal) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    val (badgeText, badgeColor) = when (signal.status) {
        DisasterSignal.StatusType.HELP -> "🔴 SOS / RED ALERT (HELP NEEDED)" to Color(0xFFDC2626)
        DisasterSignal.StatusType.MEDICAL -> "🔵 MEDICAL ASSISTANCE NEEDED" to Color(0xFF2563EB)
        DisasterSignal.StatusType.SAFE -> "🟢 STATUS OK / SAFE" to Color(0xFF16A34A)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(signal.senderName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(badgeColor, shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(badgeText, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            val locStr = if (signal.latitude != null && signal.longitude != null) {
                "Konum: Enlem ${String.format(Locale.US, "%.5f", signal.latitude)}, Boylam ${String.format(Locale.US, "%.5f", signal.longitude)} (IP: ${signal.ipAddress})"
            } else {
                "Konum: IP Adresi (${signal.ipAddress})"
            }
            Text(locStr, color = Color(0xFF94A3B8), fontSize = 11.sp)

            val notesStr = if (signal.medicalNotes.isNotEmpty()) "Not: ${signal.medicalNotes}" else "Not: Bilgi verilmedi"
            Text(notesStr, color = Color(0xFFCBD5E1), fontSize = 12.sp)

            val timeStr = dateFormat.format(Date(signal.timestamp))
            Text("Son Yayın: $timeStr • Doğrudan Mesh İletim", color = Color(0xFF64748B), fontSize = 10.sp)
        }
    }
}

@Composable
fun StrobeOverlayWindow(onClose: () -> Unit) {
    var isWhite by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(150)
            isWhite = !isWhite
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isWhite) Color.White else Color.Red)
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "🔦 VISUAL SOS STROBE ACTIVE\n(Kapatmak için tıklayın)",
            color = if (isWhite) Color.Black else Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ContactsTabScreen() {
    val sampleContacts = remember {
        listOf(
            Contact("1", "Ahmet Yılmaz (Samsung S21)", "192.168.1.45", isOnline = true, statusText = "🟢 Safe"),
            Contact("2", "Mehmet Kaya (Xiaomi 13)", "192.168.1.62", isOnline = true, statusText = "🔴 SOS Emergency"),
            Contact("3", "Ayşe Demir (Windows PC)", "192.168.1.100", isOnline = true, statusText = "🔵 Medical Assistance")
        )
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("👥 P2P MESH AĞINDAKİ CİHAZLAR & KİŞİLER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sampleContacts) { contact ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(contact.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("IP: ${contact.ipAddress} • P2P Active", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                        Text(contact.statusText, color = Color(0xFF818CF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CallsTabScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("📞 P2P SESLİ & VİDEO ARAMA PANELİ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Yerel ağ veya Mesh bağlantısı üzerinden sunucusuz doğrudan WebRTC çağrı modülü.", color = Color(0xFF94A3B8), fontSize = 13.sp)
        }
    }
}

@Composable
fun AboutTabScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("ℹ️ HAKKINDA / ABOUT MESHENGER WINDOWS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
            "Meshenger, hücresel ağlar ve internet altyapısı çöktüğünde bile yerel Wi-Fi, Ethernet ve Bluetooth Mesh ağları üzerinden kesintisiz P2P haberleşme sağlayan şifreli ve sunucusuz bir acil durum platformudur.",
            color = Color(0xFFCBD5E1),
            fontSize = 14.sp
        )
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🌟 Temel Özellikler:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("• ISO 22324 Uluslararası Akıllı Acil Durum Renk Paleti ve Sinyal Standardı", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Text("• Enkaz Dinleme & Ses Yükseltici (300Hz Yüksek Geçiren Filtre + 3x/5x/10x Kazanç Boost)", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Text("• Akustik Siren (3.5 kHz Akustik Düdük Tonu) ve Strobe SOS Flaş", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Text("• Android ve Windows Cihazlar Arasında Tam Uyumlu UDP Mesh Sinyalleşmesi", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("❤️ YARARLANILAN PROJELER VE TEŞEKKÜRLER / REFERENCES & THANKS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("• Enkaz Dinleme Uygulaması (Gürültü filtreleme & ses yükseltme): https://github.com/ozansarier/enkazdinlemeuygulamasi", color = Color(0xFF818CF8), fontSize = 12.sp)
                Text("• Orijinal Meshenger Android Projesi (P2P WebRTC haberleşme altyapısı): https://github.com/meshenger-app/meshenger-android", color = Color(0xFF818CF8), fontSize = 12.sp)
                Text("• qaul.net (Şebekesiz Mesh İletişim Konsepti): https://github.com/qaul/qaul.net", color = Color(0xFF818CF8), fontSize = 12.sp)
                Text("• WebRTC Project (Gerçek zamanlı P2P medya akış motoru): https://webrtc.org", color = Color(0xFF818CF8), fontSize = 12.sp)
                Text("• Libsodium / LazySodium (Kriptografik güvenlik & E2E şifreleme): https://libsodium.org", color = Color(0xFF818CF8), fontSize = 12.sp)
                Text("• ISO 22324 Standartları (Acil durum yönetim renk rehberi): https://www.iso.org/standard/50060.html", color = Color(0xFF818CF8), fontSize = 12.sp)
                Text("🙏 TEŞEKKÜR: Tüm arama-kurtarma ekiplerine, açık kaynak geliştiricilerine ve insanlık namına emek veren herkese sonsuz teşekkürlerimizle.", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
