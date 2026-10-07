package org.staacks.alpharemote.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Shared visual components. All actions belong to their runtime adapters. */
@Composable
fun SelfiePage(tag: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxSize().testTag(tag), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp), content = content)
    }
}

@Composable
fun SelfiePageTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() })
        subtitle?.let { SelfieNote(it) }
    }
}

@Composable
fun SelfieSection(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        title?.let { Text(it, style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.semantics { heading() }) }
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        }
    }
}

@Composable
fun SelfieNote(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun SelfiePrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
                        enabled: Boolean = true, stopping: Boolean = false) {
    Button(onClick, modifier.fillMaxWidth().heightIn(min = 80.dp), enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        colors = if (stopping) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer) else ButtonDefaults.buttonColors()) {
        Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

/** One selection group, with full-sized targets and selected semantics for TalkBack. */
@Composable
fun SelfieSegmentedControl(label: String, choices: List<Pair<Int, String>>, selected: Int,
                           enabled: Boolean, tag: String, onSelected: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(4.dp).selectableGroup()) {
            choices.forEach { (value, text) ->
                val checked = value == selected
                Surface(Modifier.weight(1f).heightIn(min = 48.dp)
                    .selectable(selected = checked, enabled = enabled, role = Role.RadioButton, onClick = { onSelected(value) })
                    .testTag("${tag}_$value"),
                    shape = RoundedCornerShape(12.dp),
                    border = if (checked) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                    color = if (checked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Box(Modifier.padding(horizontal = 2.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
                            color = if (!enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)
                                else if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
