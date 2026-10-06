package es.c0n1j.blocfone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.c0n1j.blocfone.R
import es.c0n1j.blocfone.domain.BlockingMode

@Composable
fun BlockingModeSelector(
    selectedMode: BlockingMode,
    onSelectMode: (BlockingMode) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.mode_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            ModeRow(
                titleRes = R.string.mode_all_title,
                descriptionRes = R.string.mode_all_description,
                selected = selectedMode == BlockingMode.ALL_INCOMING,
                onClick = { onSelectMode(BlockingMode.ALL_INCOMING) },
            )
            ModeRow(
                titleRes = R.string.mode_unknown_title,
                descriptionRes = R.string.mode_unknown_description,
                selected = selectedMode == BlockingMode.UNKNOWN_NUMBERS,
                onClick = { onSelectMode(BlockingMode.UNKNOWN_NUMBERS) },
            )
            ModeRow(
                titleRes = R.string.mode_selected_title,
                descriptionRes = R.string.mode_selected_description,
                selected = selectedMode == BlockingMode.SELECTED_NUMBERS,
                onClick = { onSelectMode(BlockingMode.SELECTED_NUMBERS) },
            )
        }
    }
}

@Composable
private fun ModeRow(titleRes: Int, descriptionRes: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(descriptionRes), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
