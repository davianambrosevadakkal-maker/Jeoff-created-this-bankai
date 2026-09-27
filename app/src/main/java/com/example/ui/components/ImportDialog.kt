package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.db.DeckEntity
import com.example.domain.importer.CsvColumnMapping
import com.example.ui.screens.CreateDeckDialog
import com.example.ui.viewmodel.CsvImportState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportModalDialog(
    state: CsvImportState,
    decks: List<DeckEntity>,
    onCsvContentChanged: (String) -> Unit,
    onMappingChanged: (CsvColumnMapping) -> Unit,
    onTargetDeckChanged: (Long) -> Unit,
    onCreateNewDeck: (String, String, String) -> Unit,
    onExecuteCsvImport: () -> Unit,
    onImportAnkiUri: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: CSV, 1: Anki .apkg
    var showCreateDeckDialog by remember { mutableStateOf(false) }
    var deckDropdownExpanded by remember { mutableStateOf(false) }

    val targetDeck = decks.find { it.id == state.targetDeckId }
    val isAutoCreateAnki = state.targetDeckId <= 0L
    val targetDeckName = targetDeck?.name ?: "Selected Deck"

    // File launcher for .apkg / .colpkg or .csv
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            if (selectedTab == 0) {
                // Read CSV text
                try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val text = stream.bufferedReader().readText()
                        onCsvContentChanged(text)
                    }
                } catch (e: Exception) {
                    // Handled
                }
            } else {
                // Anki package
                onImportAnkiUri(it)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("import_dialog"),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Import",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Import Cards & Decks",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_import_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: CSV vs Anki .apkg
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("CSV / TSV Import", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_csv")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Anki (.apkg / .colpkg)", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_anki")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Target Destination Deck Selection Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_target_deck_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "IMPORT INTO DECK:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = { showCreateDeckDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("new_deck_from_import_link")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Deck", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        ExposedDropdownMenuBox(
                            expanded = deckDropdownExpanded,
                            onExpandedChange = { deckDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val deckDisplayName = if (selectedTab == 1 && isAutoCreateAnki) {
                                "✨ Auto-create new deck from Anki package"
                            } else {
                                targetDeck?.name ?: "Select a deck..."
                            }
                            val deckDetails = if (selectedTab == 1 && isAutoCreateAnki) {
                                "Internal Anki deck name will be used"
                            } else {
                                "${targetDeck?.cardCount ?: 0} cards currently"
                            }

                            OutlinedTextField(
                                value = "$deckDisplayName  ($deckDetails)",
                                onValueChange = {},
                                readOnly = true,
                                leadingIcon = {
                                    if (selectedTab == 1 && isAutoCreateAnki) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        val color = try {
                                            Color(android.graphics.Color.parseColor(targetDeck?.colorHex ?: "#2563EB"))
                                        } catch (e: Exception) {
                                            MaterialTheme.colorScheme.primary
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                        )
                                    }
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deckDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("target_deck_dropdown_field")
                            )

                            ExposedDropdownMenu(
                                expanded = deckDropdownExpanded,
                                onDismissRequest = { deckDropdownExpanded = false }
                            ) {
                                if (selectedTab == 1) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text("✨ Auto-create from Anki package", fontWeight = FontWeight.Bold)
                                                    Text(
                                                        "Creates a new deck using the name stored inside the .apkg",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        },
                                        trailingIcon = if (isAutoCreateAnki) {
                                            { Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary) }
                                        } else null,
                                        onClick = {
                                            onTargetDeckChanged(-1L)
                                            deckDropdownExpanded = false
                                        },
                                        modifier = Modifier.testTag("target_deck_auto_anki")
                                    )
                                    HorizontalDivider()
                                }

                                decks.forEach { d ->
                                    val isCurrent = d.id == state.targetDeckId && (!isAutoCreateAnki || selectedTab == 0)
                                    val dColor = try {
                                        Color(android.graphics.Color.parseColor(d.colorHex))
                                    } catch (e: Exception) {
                                        MaterialTheme.colorScheme.primary
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(dColor)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = d.name,
                                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                                Text(
                                                    text = "${d.cardCount} cards",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        trailingIcon = if (isCurrent) {
                                            { Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary) }
                                        } else null,
                                        onClick = {
                                            onTargetDeckChanged(d.id)
                                            deckDropdownExpanded = false
                                        },
                                        modifier = Modifier.testTag("target_deck_select_${d.id}")
                                    )
                                }

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("➕ Create New Deck...", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    onClick = {
                                        deckDropdownExpanded = false
                                        showCreateDeckDialog = true
                                    },
                                    modifier = Modifier.testTag("target_deck_create_new_btn")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // CSV Import View
                    CsvImportContent(
                        state = state,
                        targetDeckName = targetDeckName,
                        onContentChanged = onCsvContentChanged,
                        onMappingChanged = onMappingChanged,
                        onPickFile = {
                            filePickerLauncher.launch(arrayOf("text/*", "text/comma-separated-values", "*/*"))
                        },
                        onExecuteImport = onExecuteCsvImport
                    )
                } else {
                    // Anki Package Import View
                    AnkiPackageContent(
                        targetDeckName = targetDeckName,
                        isAutoCreate = isAutoCreateAnki,
                        onPickAnkiFile = {
                            filePickerLauncher.launch(arrayOf("*/*"))
                        },
                        onLoadSampleCsv = {
                            val sample = """
                                German,English,Gender,Example,Tags
                                die Herausforderung,challenge,die,Eine echte Herausforderung!,Goethe B2
                                der Fortschritt,progress,der,Du machst große Fortschritte.,B1
                                das Verhalten,behavior,das,Sein Verhalten war einwandfrei.,B2
                                anfangen,to start / begin,,Wann fangen wir an?,Verbs
                                die Zuverlässigkeit,reliability,die,Auf ihn kann man zählen.,Adjektiv
                            """.trimIndent()
                            onCsvContentChanged(sample)
                            selectedTab = 0
                        }
                    )
                }
            }
        }
    }

    if (showCreateDeckDialog) {
        CreateDeckDialog(
            onDismiss = { showCreateDeckDialog = false },
            onCreate = { name, desc, color ->
                onCreateNewDeck(name, desc, color)
                showCreateDeckDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CsvImportContent(
    state: CsvImportState,
    targetDeckName: String,
    onContentChanged: (String) -> Unit,
    onMappingChanged: (CsvColumnMapping) -> Unit,
    onPickFile: () -> Unit,
    onExecuteImport: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Paste CSV data or select file:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedButton(
                onClick = onPickFile,
                modifier = Modifier.testTag("pick_csv_file_btn")
            ) {
                Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Pick File", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pick CSV File")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = state.rawCsv,
            onValueChange = onContentChanged,
            placeholder = { Text("Paste CSV or TSV content here...\ne.g.:\nder Tisch,table,der,Möbel\ndie Lampe,lamp,die,Licht") },
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .testTag("csv_input_field"),
            maxLines = 6
        )

        // Preview & Column Mapping Section
        if (state.preview != null && state.preview.headers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.TableChart,
                    contentDescription = "Preview",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Data Preview (Delimiter: '${state.delimiter}') - ~${state.preview.totalEstimatedRows} cards",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sample rows preview table
            val tableScroll = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .horizontalScroll(tableScroll)
                    .padding(8.dp)
            ) {
                Column {
                    // Header row
                    Row {
                        state.preview.headers.forEachIndexed { i, h ->
                            Text(
                                text = "Col $i: $h",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .width(130.dp)
                                    .padding(4.dp)
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    // Sample data rows
                    state.preview.sampleRows.forEach { row ->
                        Row {
                            row.forEach { cell ->
                                Text(
                                    text = cell,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .width(130.dp)
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Customizable Column Mapping:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            val headers = state.preview.headers

            // Mapping: Front / German Word
            ColumnSelectorDropdown(
                label = "Front (German Word)*",
                selectedIndex = state.currentMapping.frontIndex,
                headers = headers,
                onSelected = { idx ->
                    onMappingChanged(state.currentMapping.copy(frontIndex = idx))
                }
            )

            // Mapping: Back / English Meaning
            ColumnSelectorDropdown(
                label = "Back (English Meaning)*",
                selectedIndex = state.currentMapping.backIndex,
                headers = headers,
                onSelected = { idx ->
                    onMappingChanged(state.currentMapping.copy(backIndex = idx))
                }
            )

            // Mapping: Gender (Optional)
            ColumnSelectorDropdown(
                label = "Gender (der/die/das) (Optional)",
                selectedIndex = state.currentMapping.genderIndex ?: -1,
                headers = headers,
                allowNone = true,
                onSelected = { idx ->
                    onMappingChanged(state.currentMapping.copy(genderIndex = if (idx < 0) null else idx))
                }
            )

            // Mapping: Example Sentence / Notes (Optional)
            ColumnSelectorDropdown(
                label = "Example Sentence / Notes (Optional)",
                selectedIndex = state.currentMapping.exampleIndex ?: -1,
                headers = headers,
                allowNone = true,
                onSelected = { idx ->
                    onMappingChanged(state.currentMapping.copy(exampleIndex = if (idx < 0) null else idx))
                }
            )

            // Mapping: Tags (Optional)
            ColumnSelectorDropdown(
                label = "Tags / Category (Optional)",
                selectedIndex = state.currentMapping.tagsIndex ?: -1,
                headers = headers,
                allowNone = true,
                onSelected = { idx ->
                    onMappingChanged(state.currentMapping.copy(tagsIndex = if (idx < 0) null else idx))
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.statusMessage != null) {
                Text(
                    text = state.statusMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = onExecuteImport,
                enabled = !state.isProcessing && state.rawCsv.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("execute_csv_import_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importing cards...")
                } else {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = "Import", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Into '$targetDeckName'", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnSelectorDropdown(
    label: String,
    selectedIndex: Int,
    headers: List<String>,
    allowNone: Boolean = false,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayValue = if (selectedIndex in headers.indices) {
        "Col $selectedIndex: ${headers[selectedIndex]}"
    } else {
        if (allowNone) "(None / Auto-detect)" else "Select Column"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 12.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (allowNone) {
                DropdownMenuItem(
                    text = { Text("(None / Auto-detect)") },
                    onClick = {
                        onSelected(-1)
                        expanded = false
                    }
                )
            }
            headers.forEachIndexed { index, header ->
                DropdownMenuItem(
                    text = { Text("Col $index: $header") },
                    onClick = {
                        onSelected(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AnkiPackageContent(
    targetDeckName: String,
    isAutoCreate: Boolean,
    onPickAnkiFile: () -> Unit,
    onLoadSampleCsv: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Anki Compatibility",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Native Anki Package (.apkg / .colpkg)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "AnkiDroidBANKAI includes native parsing for Anki export archives! It extracts your decks, tags, notes, and intervals from the internal SQLite database automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onPickAnkiFile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("select_anki_package_btn")
                ) {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = "Select .apkg")
                    Spacer(modifier = Modifier.width(8.dp))
                    val btnLabel = if (isAutoCreate) "Pick File & Auto-Create Deck" else "Pick File & Import into '$targetDeckName'"
                    Text(btnLabel, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Want to try importing right away?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onLoadSampleCsv,
            modifier = Modifier.testTag("load_sample_csv_btn")
        ) {
            Text("Load Sample German CSV with Columns")
        }
    }
}
