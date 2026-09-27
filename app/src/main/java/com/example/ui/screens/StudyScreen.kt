package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.domain.audio.VoiceRecordState
import com.example.domain.audio.VoiceRecordingHelper
import com.example.domain.srs.ClozeHelper
import com.example.data.db.FlashcardEntity
import com.example.domain.srs.AnkiSrsEngine
import com.example.domain.srs.SrsDeckSettings
import com.example.domain.srs.SrsRating
import com.example.domain.tts.GermanTtsHelper
import com.example.ui.components.GenderBadge
import com.example.ui.components.GermanUmlautBar
import com.example.ui.components.VerificationFeedbackCard
import com.example.ui.components.WhiteboardOverlay
import com.example.ui.components.getGenderColor
import com.example.ui.theme.SrsAgainColor
import com.example.ui.theme.SrsEasyColor
import com.example.ui.theme.SrsGoodColor
import com.example.ui.theme.SrsHardColor
import com.example.ui.viewmodel.StudyDirection
import com.example.ui.viewmodel.StudySessionState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    session: StudySessionState,
    ttsHelper: GermanTtsHelper,
    srsSettings: SrsDeckSettings = SrsDeckSettings(),
    onBack: () -> Unit,
    onRevealAnswer: () -> Unit,
    onToggleTypingMode: () -> Unit,
    onDirectionChanged: (StudyDirection) -> Unit,
    onTypedInputChanged: (String) -> Unit,
    onVerifyAnswer: () -> Unit,
    onSubmitRating: (SrsRating) -> Unit,
    onUndo: () -> Unit = {},
    onToggleWhiteboard: () -> Unit = {},
    onOpenCardDiagnostics: (FlashcardEntity) -> Unit = {},
    onSuspendCard: () -> Unit = {},
    onBuryCard: () -> Unit = {},
    onToggleFlag: (Int) -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    BackHandler { onBack() }

    val currentCard = session.currentCard
    val scrollState = rememberScrollState()
    var directionMenuExpanded by remember { mutableStateOf(false) }
    var overflowMenuExpanded by remember { mutableStateOf(false) }

    var secondsElapsed by remember(session.currentIndex) { mutableIntStateOf(0) }
    LaunchedEffect(session.currentIndex) {
        secondsElapsed = 0
        while (true) {
            delay(1000L)
            secondsElapsed++
        }
    }

    val isPromptGerman = session.isCurrentPromptGerman
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val voiceRecorder = remember { VoiceRecordingHelper(context) }
    var voiceState by remember { mutableStateOf(VoiceRecordState.IDLE) }
    DisposableEffect(Unit) {
        onDispose {
            voiceRecorder.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (session.isMicroSetSession) "Micro-Set: ${session.microSetLabel}" else session.deckName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            if (session.isCramMode) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFDC2626))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("CRAM", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Card ${session.currentIndex + 1}/${session.cards.size} • ${srsSettings.algorithm.shortName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Answer Timer Display
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = String.format("%02d:%02d", secondsElapsed / 60, secondsElapsed % 60),
                                    fontSize = 10.sp,
                                    color = Color(0xFFCBD5E1),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("study_back_btn")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Study")
                    }
                },
                actions = {
                    // Undo Button
                    IconButton(
                        onClick = onUndo,
                        enabled = session.canUndo,
                        modifier = Modifier.testTag("study_undo_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo Rating",
                            tint = if (session.canUndo) Color(0xFFF59E0B) else Color(0xFF475569)
                        )
                    }

                    // Whiteboard Scratchpad Toggle
                    IconButton(
                        onClick = onToggleWhiteboard,
                        modifier = Modifier.testTag("study_whiteboard_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = "Toggle Whiteboard",
                            tint = if (session.showWhiteboard) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Card Diagnostics Info Button
                    currentCard?.let { card ->
                        IconButton(
                            onClick = { onOpenCardDiagnostics(card) },
                            modifier = Modifier.testTag("study_card_info_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Card SRS Diagnostics",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Flag Button & Dropdown
                        var flagMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            val flagColor = when (card.flag) {
                                1 -> Color(0xFFEF4444)
                                2 -> Color(0xFFF97316)
                                3 -> Color(0xFF10B981)
                                4 -> Color(0xFF3B82F6)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            IconButton(
                                onClick = { flagMenuExpanded = true },
                                modifier = Modifier.testTag("study_flag_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = "Flag Card",
                                    tint = flagColor
                                )
                            }

                            DropdownMenu(
                                expanded = flagMenuExpanded,
                                onDismissRequest = { flagMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("No Flag") },
                                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = Color.Gray) },
                                    onClick = {
                                        onToggleFlag(0)
                                        flagMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Red Flag") },
                                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFFEF4444)) },
                                    onClick = {
                                        onToggleFlag(1)
                                        flagMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Orange Flag") },
                                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFFF97316)) },
                                    onClick = {
                                        onToggleFlag(2)
                                        flagMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Green Flag") },
                                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFF10B981)) },
                                    onClick = {
                                        onToggleFlag(3)
                                        flagMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Blue Flag") },
                                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFF3B82F6)) },
                                    onClick = {
                                        onToggleFlag(4)
                                        flagMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Direction Switcher Dropdown (German->English, English->German, Alternating)
                    Box {
                        OutlinedButton(
                            onClick = { directionMenuExpanded = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("study_direction_btn")
                        ) {
                            Text(session.direction.badge, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = "Switch Direction", modifier = Modifier.size(14.dp))
                        }

                        DropdownMenu(
                            expanded = directionMenuExpanded,
                            onDismissRequest = { directionMenuExpanded = false }
                        ) {
                            StudyDirection.values().forEach { dir ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(dir.badge, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = dir.displayName,
                                                fontWeight = if (session.direction == dir) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    },
                                    trailingIcon = if (session.direction == dir) {
                                        { Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                    } else null,
                                    onClick = {
                                        onDirectionChanged(dir)
                                        directionMenuExpanded = false
                                    },
                                    modifier = Modifier.testTag("direction_option_${dir.name}")
                                )
                            }
                        }
                    }

                    // Overflow Menu
                    Box {
                        IconButton(onClick = { overflowMenuExpanded = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More Actions")
                        }

                        DropdownMenu(
                            expanded = overflowMenuExpanded,
                            onDismissRequest = { overflowMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (session.isTypingMode) "Switch to Flip Mode" else "Switch to Typing Mode") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (session.isTypingMode) Icons.Default.FlipCameraAndroid else Icons.Default.Keyboard,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    onToggleTypingMode()
                                    overflowMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Bury Card (Skip Today)") },
                                leadingIcon = { Icon(Icons.Default.Bedtime, contentDescription = null) },
                                onClick = {
                                    onBuryCard()
                                    overflowMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Suspend Card (Freeze)") },
                                leadingIcon = { Icon(Icons.Default.PauseCircle, contentDescription = null) },
                                onClick = {
                                    onSuspendCard()
                                    overflowMenuExpanded = false
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("SRS Settings & Retention") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    onOpenSettings()
                                    overflowMenuExpanded = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Linear Progress Indicator
            LinearProgressIndicator(
                progress = { session.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (session.isFinished || currentCard == null) {
                // Session Complete Screen
                SessionFinishedView(
                    session = session,
                    onReturn = onBack
                )
            } else {
                // Active Study Card
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    var totalDragX by remember(session.currentIndex) { mutableFloatStateOf(0f) }

                    // Flashcard container
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(session.currentIndex, session.isAnswerRevealed) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (totalDragX > 120f) {
                                            // Swiped right -> Good
                                            if (session.isAnswerRevealed) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onSubmitRating(SrsRating.GOOD)
                                            } else {
                                                onRevealAnswer()
                                            }
                                        } else if (totalDragX < -120f) {
                                            // Swiped left -> Again
                                            if (session.isAnswerRevealed) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onSubmitRating(SrsRating.AGAIN)
                                            } else {
                                                onRevealAnswer()
                                            }
                                        }
                                        totalDragX = 0f
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        totalDragX += dragAmount
                                    }
                                )
                            }
                            .testTag("study_flashcard"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top Row: Index Badge, Direction Tag, & Audio
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "#${currentCard.orderIndex}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Mode direction pill
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isPromptGerman) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                                else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isPromptGerman) "🇩🇪 → 🇬🇧" else "🇬🇧 → 🇩🇪",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPromptGerman) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (currentCard.gender.isNotBlank() && (isPromptGerman || session.isAnswerRevealed)) {
                                        GenderBadge(gender = currentCard.gender)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    // Voice Recording Microphone Button
                                    IconButton(
                                        onClick = {
                                            when (voiceState) {
                                                VoiceRecordState.IDLE -> {
                                                    voiceRecorder.startRecording(
                                                        onStateChange = { voiceState = it },
                                                        onError = {}
                                                    )
                                                }
                                                VoiceRecordState.RECORDING -> {
                                                    voiceRecorder.stopRecording { voiceState = it }
                                                }
                                                VoiceRecordState.PLAYING -> {
                                                    voiceRecorder.stopPlayback()
                                                    voiceState = VoiceRecordState.IDLE
                                                }
                                            }
                                        },
                                        modifier = Modifier.testTag("voice_record_mic_btn")
                                    ) {
                                        Icon(
                                            imageVector = when (voiceState) {
                                                VoiceRecordState.RECORDING -> Icons.Default.Stop
                                                VoiceRecordState.PLAYING -> Icons.Default.PlayArrow
                                                VoiceRecordState.IDLE -> Icons.Default.Mic
                                            },
                                            contentDescription = "Record Pronunciation",
                                            tint = when (voiceState) {
                                                VoiceRecordState.RECORDING -> Color(0xFFEF4444)
                                                VoiceRecordState.PLAYING -> Color(0xFF10B981)
                                                VoiceRecordState.IDLE -> MaterialTheme.colorScheme.primary
                                            }
                                        )
                                    }

                                    if (voiceRecorder.hasRecording() && voiceState == VoiceRecordState.IDLE) {
                                        IconButton(
                                            onClick = {
                                                voiceRecorder.playRecording(
                                                    onStateChange = { voiceState = it },
                                                    onCompleted = { voiceState = VoiceRecordState.IDLE }
                                                )
                                            },
                                            modifier = Modifier.testTag("voice_playback_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play your recording",
                                                tint = Color(0xFF38BDF8)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { ttsHelper.speak(currentCard.front) },
                                        modifier = Modifier.testTag("tts_pronounce_card")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Pronounce German",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // --- PROMPT (Front of Card) ---
                            val rawPrompt = if (isPromptGerman) currentCard.front else currentCard.back
                            val hasCloze = ClozeHelper.hasCloze(rawPrompt)
                            val promptLabel = if (hasCloze) "CLOZE DELETION" else if (isPromptGerman) "GERMAN PROMPT" else "ENGLISH MEANING"
                            val promptText = if (hasCloze) {
                                if (session.isAnswerRevealed) ClozeHelper.renderClozeRevealed(rawPrompt)
                                else ClozeHelper.renderClozeQuestion(rawPrompt)
                            } else {
                                rawPrompt
                            }
                            val promptColor = if (isPromptGerman) {
                                val gc = getGenderColor(currentCard.gender)
                                if (gc != Color.Unspecified) gc else MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }

                            Text(
                                text = promptLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = promptText,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = promptColor,
                                textAlign = TextAlign.Center
                            )

                            if (currentCard.tags.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tag: ${currentCard.tags}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // --- TARGET / REVEALED ANSWER (Back of Card) ---
                            if (session.isAnswerRevealed) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                val targetLabel = if (isPromptGerman) "ENGLISH MEANING" else "GERMAN TARGET"
                                val targetText = if (isPromptGerman) currentCard.back else currentCard.front
                                val targetColor = if (!isPromptGerman) {
                                    val gc = getGenderColor(currentCard.gender)
                                    if (gc != Color.Unspecified) gc else MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }

                                Text(
                                    text = targetLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = targetText,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    color = targetColor,
                                    textAlign = TextAlign.Center
                                )

                                if (currentCard.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = currentCard.notes,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                if (currentCard.synonyms.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Synonyms: ${currentCard.synonyms}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                // Dynamic Synonyms Section
                                if (session.dynamicSynonyms.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Psychology,
                                                    contentDescription = "Synonyms",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Dynamic German Synonyms",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                session.dynamicSynonyms.take(4).forEach { syn ->
                                                    SuggestionChip(
                                                        onClick = { ttsHelper.speak(syn) },
                                                        label = { Text(syn, fontSize = 12.sp) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Whiteboard Overlay (if enabled)
                    if (session.showWhiteboard) {
                        Spacer(modifier = Modifier.height(10.dp))
                        WhiteboardOverlay(onClose = onToggleWhiteboard)
                    }

                    // Input & Verification Area (if in typing mode and answer not revealed yet)
                    if (session.isTypingMode && !session.isAnswerRevealed) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            val inputLabel = if (isPromptGerman) "Type English translation..." else "Type German translation..."
                            val inputPlaceholder = if (isPromptGerman) {
                                "e.g. ${currentCard.back.split(',', '/').firstOrNull()?.trim() ?: "..."}"
                            } else {
                                "e.g. ${if (currentCard.gender.isNotBlank()) currentCard.gender + " ..." else "..."}"
                            }

                            OutlinedTextField(
                                value = session.userTypedInput,
                                onValueChange = onTypedInputChanged,
                                label = { Text(inputLabel) },
                                placeholder = { Text(inputPlaceholder) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("study_answer_input")
                            )

                            // Quick German Umlauts Row (visible when user needs to type German)
                            if (!isPromptGerman) {
                                GermanUmlautBar(
                                    onInsertChar = { ch ->
                                        onTypedInputChanged(session.userTypedInput + ch)
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onRevealAnswer,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("skip_to_answer_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Show Answer")
                                }

                                Button(
                                    onClick = onVerifyAnswer,
                                    enabled = session.userTypedInput.isNotBlank(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("verify_answer_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Verify", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Verification Feedback Card (after checking)
                    if (session.verificationResult != null) {
                        VerificationFeedbackCard(result = session.verificationResult)
                    }

                    // If in Flip mode and answer not revealed yet
                    if (!session.isTypingMode && !session.isAnswerRevealed) {
                        Button(
                            onClick = onRevealAnswer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("reveal_answer_flip_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Visibility, contentDescription = "Reveal")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reveal Answer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    // SRS Repetition Buttons (Again, Hard, Good, Easy) - visible once answer is revealed
                    if (session.isAnswerRevealed) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "RATE YOUR RECALL (${srsSettings.algorithm.title.uppercase()}):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Again
                            SrsRatingButton(
                                label = "Again",
                                interval = AnkiSrsEngine.getPreviewLabel(currentCard, SrsRating.AGAIN, srsSettings),
                                color = SrsAgainColor,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSubmitRating(SrsRating.AGAIN)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rating_again_btn")
                            )

                            // Hard
                            SrsRatingButton(
                                label = "Hard",
                                interval = AnkiSrsEngine.getPreviewLabel(currentCard, SrsRating.HARD, srsSettings),
                                color = SrsHardColor,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSubmitRating(SrsRating.HARD)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rating_hard_btn")
                            )

                            // Good
                            SrsRatingButton(
                                label = "Good",
                                interval = AnkiSrsEngine.getPreviewLabel(currentCard, SrsRating.GOOD, srsSettings),
                                color = SrsGoodColor,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSubmitRating(SrsRating.GOOD)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rating_good_btn")
                            )

                            // Easy
                            SrsRatingButton(
                                label = "Easy",
                                interval = AnkiSrsEngine.getPreviewLabel(currentCard, SrsRating.EASY, srsSettings),
                                color = SrsEasyColor,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSubmitRating(SrsRating.EASY)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rating_easy_btn")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun SrsRatingButton(
    label: String,
    interval: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = interval,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 10.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun SessionFinishedView(
    session: StudySessionState,
    onReturn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = "Session Complete",
            tint = Color(0xFFF59E0B),
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ausgezeichnet! 🎉",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (session.isMicroSetSession) {
                "Micro-set session '${session.microSetLabel}' finished! You reviewed ${session.cards.size} cards."
            } else {
                "Congratulations! You completed your review session of ${session.cards.size} cards."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onReturn,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("finish_session_return_btn")
        ) {
            Text("Return to Cards", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
