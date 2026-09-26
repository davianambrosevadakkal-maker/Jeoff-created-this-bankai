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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
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
import com.example.domain.srs.AnkiSrsEngine
import com.example.domain.srs.SrsRating
import com.example.domain.tts.GermanTtsHelper
import com.example.ui.components.GenderBadge
import com.example.ui.components.GermanUmlautBar
import com.example.ui.components.VerificationFeedbackCard
import com.example.ui.components.getGenderColor
import com.example.ui.theme.SrsAgainColor
import com.example.ui.theme.SrsEasyColor
import com.example.ui.theme.SrsGoodColor
import com.example.ui.theme.SrsHardColor
import com.example.ui.viewmodel.StudySessionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    session: StudySessionState,
    ttsHelper: GermanTtsHelper,
    onBack: () -> Unit,
    onRevealAnswer: () -> Unit,
    onToggleTypingMode: () -> Unit,
    onTypedInputChanged: (String) -> Unit,
    onVerifyAnswer: () -> Unit,
    onSubmitRating: (SrsRating) -> Unit
) {
    BackHandler { onBack() }

    val currentCard = session.currentCard
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (session.isMicroSetSession) "Micro-Set: ${session.microSetLabel}" else session.deckName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "Card ${session.currentIndex + 1} of ${session.cards.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("study_back_btn")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Study")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleTypingMode,
                        modifier = Modifier.testTag("toggle_typing_mode_btn")
                    ) {
                        Icon(
                            imageVector = if (session.isTypingMode) Icons.Default.FlipCameraAndroid else Icons.Default.Keyboard,
                            contentDescription = if (session.isTypingMode) "Switch to Flip Mode" else "Switch to Typing Mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
                    // Flashcard container
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
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
                            // Top Row: Index Badge & TTS
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (currentCard.gender.isNotBlank() && session.isAnswerRevealed) {
                                        GenderBadge(gender = currentCard.gender)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    IconButton(
                                        onClick = { ttsHelper.speak(currentCard.front) },
                                        modifier = Modifier.testTag("tts_pronounce_card")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Pronounce",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Prompt: English Back / Definition
                            Text(
                                text = "ENGLISH MEANING",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentCard.back,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
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

                            // If Answer is Revealed: Show German Front & Synonyms
                            if (session.isAnswerRevealed) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                val genderColor = getGenderColor(currentCard.gender)

                                Text(
                                    text = "GERMAN TARGET",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentCard.front,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    color = if (genderColor != Color.Unspecified) genderColor else MaterialTheme.colorScheme.primary,
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

                    // Input & Verification Area (if in typing mode and answer not revealed yet)
                    if (session.isTypingMode && !session.isAnswerRevealed) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = session.userTypedInput,
                                onValueChange = onTypedInputChanged,
                                label = { Text("Type German translation...") },
                                placeholder = { Text("e.g. ${if (currentCard.gender.isNotBlank()) currentCard.gender + " ..." else "..."}") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("german_answer_input")
                            )

                            // Quick German Umlauts Row
                            GermanUmlautBar(
                                onInsertChar = { ch ->
                                    onTypedInputChanged(session.userTypedInput + ch)
                                }
                            )

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
                            text = "RATE YOUR RECALL (ANKI SM-2):",
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
                                interval = AnkiSrsEngine.getPreviewLabel(currentCard, SrsRating.AGAIN),
                                color = SrsAgainColor,
                                onClick = { onSubmitRating(SrsRating.AGAIN) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rating_again_btn")
                            )

                            // Hard
                            SrsRatingButton(
                                label = "Hard",
                                interval = AnkiSrsEngine.getPreviewLabel(currentCard, SrsRating.HARD),
                                color = SrsHardColor,
                                onClick = { onSubmitRating(SrsRating.HARD) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rating_hard_btn")
                            )

                            // Good
                            SrsRatingButton(
                                label = "Good",
                                interval = AnkiSrsEngine.getPreviewLabel(currentCard, SrsRating.GOOD),
                                color = SrsGoodColor,
                                onClick = { onSubmitRating(SrsRating.GOOD) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rating_good_btn")
                            )

                            // Easy
                            SrsRatingButton(
                                label = "Easy",
                                interval = AnkiSrsEngine.getPreviewLabel(currentCard, SrsRating.EASY),
                                color = SrsEasyColor,
                                onClick = { onSubmitRating(SrsRating.EASY) },
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
