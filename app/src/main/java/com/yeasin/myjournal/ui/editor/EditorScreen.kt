package com.yeasin.myjournal.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeasin.myjournal.data.db.NoteType
import com.yeasin.myjournal.ui.LocalCatalog
import com.yeasin.myjournal.ui.appContainer
import com.yeasin.myjournal.ui.components.FolderPickerSheet
import com.yeasin.myjournal.ui.components.ProjectPickerSheet
import com.yeasin.myjournal.ui.components.TagPickerSheet
import com.yeasin.myjournal.ui.theme.Journal
import com.yeasin.myjournal.util.TextFormat

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(noteId: String, onClose: () -> Unit) {
    val container = appContainer()
    val vm: EditorViewModel = viewModel(
        key = "editor-$noteId",
        factory = viewModelFactory { initializer { EditorViewModel(noteId, container) } },
    )
    val c = Journal.colors
    val snackbar = remember { SnackbarHostState() }
    val goBack = { vm.close(onClose) }
    val meta by vm.meta.collectAsStateWithLifecycle()
    val catalog = LocalCatalog.current
    var picker by remember { mutableStateOf<String?>(null) }

    BackHandler(onBack = goBack)
    LaunchedEffect(vm.message) {
        vm.message?.let { snackbar.showSnackbar(it); vm.message = null }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (vm.isQuick) Text("Quick thought", style = MaterialTheme.typography.bodySmall, color = c.kQuick)
                },
                navigationIcon = {
                    IconButton(onClick = goBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = vm::togglePin) {
                        Icon(
                            if (vm.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (vm.isPinned) "Unpin" else "Pin",
                            tint = if (vm.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { vm.archive(onClose) }) {
                        Icon(Icons.Outlined.Archive, contentDescription = "Archive")
                    }
                    IconButton(onClick = { vm.delete(onClose) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column(Modifier.navigationBarsPadding().imePadding()) {
                HorizontalDivider(color = c.border)
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = vm::toggleChecklist) {
                        Icon(
                            Icons.Outlined.CheckBox,
                            contentDescription = if (vm.type == NoteType.CHECKLIST) "Convert to text" else "Convert to checklist",
                            tint = if (vm.type == NoteType.CHECKLIST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (vm.updatedAt > 0) {
                        Text(
                            "Edited " + TextFormat.clockLabel(vm.updatedAt),
                            style = MaterialTheme.typography.bodySmall, color = c.sub,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    }
                }
            }
        },
    ) { padding ->
        if (!vm.loaded) return@Scaffold
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            val titleStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurface)
            BasicTextField(
                value = vm.title,
                onValueChange = vm::onTitle,
                textStyle = titleStyle,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp),
                decorationBox = { inner ->
                    if (vm.title.isEmpty()) Text("Title", style = titleStyle, color = c.sub)
                    inner()
                },
            )

            if (vm.type == NoteType.TEXT) {
                val bodyStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
                BasicTextField(
                    value = vm.content,
                    onValueChange = vm::onContent,
                    textStyle = bodyStyle,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp),
                    decorationBox = { inner ->
                        if (vm.content.isEmpty()) Text("Note", style = bodyStyle, color = c.sub)
                        inner()
                    },
                )
            } else {
                vm.lines.forEachIndexed { i, line ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 48.dp)) {
                        Checkbox(
                            checked = line.done,
                            onCheckedChange = { vm.toggleLine(i) },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                        )
                        val style = MaterialTheme.typography.bodyLarge.copy(
                            color = if (line.done) c.sub else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (line.done) TextDecoration.LineThrough else null,
                        )
                        BasicTextField(
                            value = line.text,
                            onValueChange = { vm.onLineText(i, it) },
                            textStyle = style,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (line.text.isEmpty()) Text("List item", style = style, color = c.sub)
                                inner()
                            },
                        )
                        IconButton(onClick = { vm.removeLine(i) }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Remove item", tint = c.sub, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                TextButton(onClick = vm::addLine) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("List item")
                }
            }
            // Organize: project, folder, tags. All optional.
            val note = meta?.note
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val project = note?.projectId?.let { catalog.projectById[it] }
                AssistChip(
                    onClick = { picker = "project" },
                    label = { Text(project?.name ?: "No project") },
                    leadingIcon = {
                        if (project != null) {
                            Box(Modifier.size(10.dp).background(c.project(catalog.projectColorSlot(project.id)).second, CircleShape))
                        } else {
                            Icon(Icons.Outlined.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    },
                )
                AssistChip(
                    onClick = { picker = "folder" },
                    label = { Text(catalog.folderPath(note?.folderId).ifEmpty { "No folder" }) },
                    leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
                meta?.tags?.sortedBy { it.name.lowercase() }?.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { vm.setTag(tag.id, false) },
                        label = { Text("#" + tag.name) },
                        trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = "Remove tag ${tag.name}", modifier = Modifier.size(16.dp)) },
                    )
                }
                AssistChip(
                    onClick = { picker = "tag" },
                    label = { Text("Tag") },
                    leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
            Spacer(Modifier.padding(bottom = 48.dp))
        }
    }

    when (picker) {
        "project" -> ProjectPickerSheet("Project", meta?.note?.projectId, onPick = { vm.setProject(it); picker = null }, onDismiss = { picker = null })
        "folder" -> FolderPickerSheet("Move to folder", meta?.note?.folderId, onPick = { vm.setFolder(it); picker = null }, onDismiss = { picker = null })
        "tag" -> TagPickerSheet(
            "Tags",
            isOn = { tag -> meta?.tags?.any { it.id == tag.id } == true },
            onToggle = { tag, on -> vm.setTag(tag.id, on) },
            onDismiss = { picker = null },
        )
    }
}
