package com.ahora.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahora.app.data.TAG_NAME_MAX_LENGTH
import com.ahora.app.data.Tag
import com.ahora.app.data.TagPalette
import com.ahora.app.data.sanitizeTagName
import com.ahora.app.ui.components.DialogTitle
import com.ahora.app.ui.components.TagDot
import com.ahora.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Gestión de etiquetas (1.28.0): crear, renombrar, cambiar el color y
 * eliminar, todo en un solo lugar. Se abre desde Ajustes → Etiquetas.
 *
 * - Crear: nombre (máx. 24 caracteres) + un color de la paleta fija.
 * - Editar: la fila se convierte en editor; "Cancelar" no toca nada.
 * - Eliminar: pide confirmación y dice en cuántas tareas se usa. Las
 *   tareas no se borran: solo quedan sin esa etiqueta.
 *
 * Los fallos (nombre vacío o duplicado) se avisan con [onMessage], que
 * los muestra en el Snackbar de Ajustes (mismo patrón que el respaldo).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsDialog(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit
) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val taskTags by viewModel.taskTags.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editingTag by remember { mutableStateOf<Tag?>(null) }
    var deletingTag by remember { mutableStateOf<Tag?>(null) }

    // Cuántas tareas usan cada etiqueta, para mostrarlo en su fila y en
    // la confirmación de borrado. Las tablas son pequeñas: se calcula
    // aquí, sin consultas extra.
    val usage: Map<Long, Int> = remember(tags, taskTags) {
        tags.associate { tag ->
            tag.id to taskTags.values.sumOf { list -> list.count { it.id == tag.id } }
        }
    }

    fun save(create: Boolean, tag: Tag?, name: String, colorIndex: Int) {
        scope.launch {
            val result = runCatching {
                if (create) viewModel.createTag(name, colorIndex)
                else viewModel.updateTag(tag!!, name, colorIndex)
            }
            result.fold(
                onSuccess = { editingTag = null },
                onFailure = { e -> onMessage(e.message ?: "No se pudo guardar la etiqueta") }
            )
        }
    }

    fun delete(tag: Tag) {
        deletingTag = null
        scope.launch {
            runCatching { viewModel.deleteTag(tag) }
                .onFailure { e -> onMessage(e.message ?: "No se pudo eliminar la etiqueta") }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitle("Etiquetas") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (tags.isEmpty()) {
                    Text(
                        text = "Aún no tienes etiquetas. Crea la primera abajo: sirven para " +
                            "agrupar tus tareas y filtrarlas en «Todas».",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.m))
                }
                tags.forEach { tag ->
                    val editing = editingTag?.id == tag.id
                    if (editing) {
                        TagEditor(
                            initialName = tag.name,
                            initialColor = tag.colorIndex,
                            // Al editar, su propio nombre no cuenta como
                            // duplicado.
                            takenNames = tags
                                .filter { it.id != tag.id }
                                .map { it.name.lowercase() }
                                .toSet(),
                            confirmLabel = "Guardar",
                            // Al guardar se desmonta el editor (editingTag
                            // = null), así que no necesita reiniciarse.
                            resetKey = tag.id,
                            onConfirm = { name, color -> save(false, tag, name, color) },
                            onCancel = { editingTag = null }
                        )
                    } else {
                        TagRow(
                            tag = tag,
                            usageCount = usage[tag.id] ?: 0,
                            onEdit = { editingTag = tag },
                            onDelete = { deletingTag = tag }
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.s))
                }

                // Crear: el formulario vive al final, tras la lista. Se
                // reinicia cada vez que la lista crece (etiqueta creada).
                if (editingTag == null) {
                    TagEditor(
                        initialName = "",
                        initialColor = 0,
                        takenNames = tags.map { it.name.lowercase() }.toSet(),
                        confirmLabel = "Añadir",
                        title = "Nueva etiqueta",
                        resetKey = tags.size,
                        onConfirm = { name, color -> save(true, null, name, color) },
                        onCancel = {}
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )

    deletingTag?.let { tag ->
        val count = usage[tag.id] ?: 0
        AlertDialog(
            onDismissRequest = { deletingTag = null },
            title = { DialogTitle("Eliminar etiqueta") },
            text = {
                Text(
                    when (count) {
                        0 -> "«${tag.name}» no se usa en ninguna tarea."
                        1 -> "«${tag.name}» se usa en 1 tarea: se quitará de ella, " +
                            "pero la tarea no se borra."
                        else -> "«${tag.name}» se usa en $count tareas: se quitará " +
                            "de ellas, pero las tareas no se borran."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { delete(tag) }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTag = null }) { Text("Cancelar") }
            }
        )
    }
}

/** Una fila de etiqueta: punto de color, nombre, uso y acciones. */
@Composable
private fun TagRow(
    tag: Tag,
    usageCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        TagDot(colorIndex = tag.colorIndex)
        Spacer(modifier = Modifier.width(Spacing.s))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tag.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = when (usageCount) {
                    0 -> "Sin tareas"
                    1 -> "1 tarea"
                    else -> "$usageCount tareas"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = "Editar «${tag.name}»",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Eliminar «${tag.name}»",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Editor de etiqueta (crear o renombrar): nombre + selector de la
 * paleta fija de 8 colores. Valida en local: vacío y duplicado
 * (insensible a mayúsculas) se avisan aquí, sin tocar la base de datos.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagEditor(
    initialName: String,
    initialColor: Int,
    takenNames: Set<String>,
    confirmLabel: String,
    /** Clave que reinicia el editor al cambiar (p. ej. tras crear una). */
    resetKey: Any?,
    onConfirm: (name: String, colorIndex: Int) -> Unit,
    onCancel: () -> Unit,
    title: String? = null
) {
    var name by remember(resetKey, initialName) { mutableStateOf(initialName) }
    var colorIndex by remember(resetKey, initialColor) { mutableIntStateOf(initialColor) }
    var error by remember(resetKey) { mutableStateOf<String?>(null) }

    fun submit() {
        val clean = sanitizeTagName(name)
        error = when {
            clean == null -> "El nombre no puede estar vacío"
            clean.lowercase() in takenNames -> "Ya existe una etiqueta con ese nombre"
            else -> null
        }
        if (error == null) onConfirm(clean!!, TagPalette.sanitizeColorIndex(colorIndex))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
        }
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it.take(TAG_NAME_MAX_LENGTH)
                error = null
            },
            label = { Text("Nombre") },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(Spacing.s))
        // Paleta fija de 8 colores (1.28.0): tonos medios que se
        // distinguen en modo claro y oscuro. El seleccionado lleva el
        // anillo del acento propio + marca de verificación.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            TagPalette.COLORS.forEachIndexed { index, color ->
                val selected = index == colorIndex
                IconButton(
                    onClick = { colorIndex = index },
                    modifier = Modifier
                        .size(40.dp)
                        // TalkBack: cada color se anuncia por su nombre
                        // ("Color Rojo", "Color Azul"…).
                        .semantics {
                            contentDescription = "Color ${TagPalette.colorName(index)}"
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .then(
                                if (selected) Modifier.border(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                ) else Modifier
                            )
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                // TalkBack: cada color se anuncia por su nombre (ver el
                // `contentDescription` en el modificador del botón).
            }
        }
        Spacer(modifier = Modifier.height(Spacing.s))
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = ::submit) { Text(confirmLabel) }
            if (title == null) {
                TextButton(onClick = onCancel) { Text("Cancelar") }
            }
        }
    }
}
