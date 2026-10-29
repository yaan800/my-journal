package com.yeasin.myjournal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yeasin.myjournal.data.db.TagEntity
import com.yeasin.myjournal.ui.LocalCatalog
import com.yeasin.myjournal.ui.appContainer
import com.yeasin.myjournal.ui.theme.Journal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Bottom sheet frame shared by all pickers. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerSheet(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp).imePadding()) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp))
            content()
        }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, indent: Int = 0, dotColor: androidx.compose.ui.graphics.Color? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(start = (12 + indent * 20).dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(horizontal = 12.dp))
        if (dotColor != null) {
            Box(Modifier.size(10.dp).background(dotColor, CircleShape))
            Spacer(Modifier.width(10.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Create-new row at the bottom of a picker. */
@Composable
private fun CreateRow(placeholder: String, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = name, onValueChange = { name = it }, singleLine = true,
            placeholder = { Text(placeholder) }, modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Button(onClick = { if (name.isNotBlank()) { onCreate(name.trim()); name = "" } }, enabled = name.isNotBlank()) { Text("Add") }
    }
}

@Composable
fun ProjectPickerSheet(
    title: String,
    currentId: String?,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
    noneLabel: String = "No project",
    allowCreate: Boolean = true,
) {
    val catalog = LocalCatalog.current
    val c = appContainer()
    val scope = rememberCoroutineScope()
    PickerSheet(title, onDismiss) {
        LazyColumn(Modifier.heightIn(max = 420.dp)) {
            item { RadioRow(noneLabel, currentId == null) { onPick(null) } }
            items(catalog.projects, key = { it.id }) { p ->
                RadioRow(p.name, currentId == p.id, dotColor = Journal.colors.project(catalog.projectColorSlot(p.id)).second) { onPick(p.id) }
            }
        }
        if (allowCreate) {
            CreateRow("Create new project") { name ->
                scope.launch { onPick(withContext(Dispatchers.IO) { c.catalogRepo.createProject(name) }) }
            }
        }
    }
}

@Composable
fun FolderPickerSheet(
    title: String,
    currentId: String?,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
    noneLabel: String = "None",
    allowCreate: Boolean = true,
) {
    val catalog = LocalCatalog.current
    val c = appContainer()
    val scope = rememberCoroutineScope()
    PickerSheet(title, onDismiss) {
        LazyColumn(Modifier.heightIn(max = 420.dp)) {
            item { RadioRow(noneLabel, currentId == null) { onPick(null) } }
            items(catalog.folderTree(), key = { it.first.id }) { (f, depth) ->
                RadioRow(f.name, currentId == f.id, indent = depth) { onPick(f.id) }
            }
        }
        if (allowCreate) {
            CreateRow("Create new folder") { name ->
                scope.launch { onPick(withContext(Dispatchers.IO) { c.catalogRepo.createFolder(name, null) }) }
            }
        }
    }
}

/** Tags: tick any number. [isOn] says whether a tag is on for every note being edited. */
@Composable
fun TagPickerSheet(title: String, isOn: (TagEntity) -> Boolean, onToggle: (TagEntity, Boolean) -> Unit, onDismiss: () -> Unit) {
    val catalog = LocalCatalog.current
    val c = appContainer()
    val scope = rememberCoroutineScope()
    var filter by remember { mutableStateOf("") }
    val shown = catalog.tags.filter { filter.isBlank() || it.name.contains(filter.trim().removePrefix("#"), ignoreCase = true) }
    val canCreate = filter.isNotBlank() && catalog.tags.none { it.name.equals(filter.trim().removePrefix("#"), ignoreCase = true) }
    PickerSheet(title, onDismiss) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = filter, onValueChange = { filter = it }, singleLine = true,
                placeholder = { Text("Find or create a tag") }, modifier = Modifier.weight(1f),
            )
            if (canCreate) {
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val name = filter.trim()
                    filter = ""
                    scope.launch {
                        val id = withContext(Dispatchers.IO) { c.catalogRepo.tagId(name) }
                        onToggle(TagEntity(id = id, name = name.removePrefix("#")), true)
                    }
                }) { Text("Create") }
            }
        }
        LazyColumn(Modifier.heightIn(max = 420.dp)) {
            items(shown, key = { it.id }) { tag ->
                val on = isOn(tag)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        .clickable(role = Role.Checkbox) { onToggle(tag, !on) }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = on, onCheckedChange = null, modifier = Modifier.padding(horizontal = 12.dp))
                    Text(tag.name, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

/** A plain list of actions, e.g. the "more" menu for selected notes. */
@Composable
fun OptionsSheet(title: String, options: List<Pair<String, () -> Unit>>, onDismiss: () -> Unit) {
    PickerSheet(title, onDismiss) {
        options.forEach { (label, action) ->
            Text(
                label, style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth().clickable(onClick = action).padding(horizontal = 24.dp, vertical = 15.dp),
            )
        }
    }
}
