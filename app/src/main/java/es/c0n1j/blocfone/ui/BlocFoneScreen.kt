package es.c0n1j.blocfone.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.c0n1j.blocfone.R
import es.c0n1j.blocfone.contacts.ContactCandidate
import es.c0n1j.blocfone.contacts.ContactSyncState
import es.c0n1j.blocfone.data.ContactContribution
import es.c0n1j.blocfone.domain.BlockingMode
import es.c0n1j.blocfone.domain.ScreeningRules
import es.c0n1j.blocfone.ui.theme.BlocFoneTheme

@Composable
fun BlocFoneScreen(
    state: BlocFoneUiState,
    roleGranted: Boolean,
    roleAvailable: Boolean,
    contactsGranted: Boolean,
    onRequestRole: () -> Unit,
    onRequestContacts: () -> Unit,
    onBlockingEnabledChange: (Boolean) -> Unit,
    onSelectMode: (BlockingMode) -> Unit,
    onAddSelected: (String, (Boolean) -> Unit) -> Unit,
    onRemoveSelected: (String) -> Unit,
    onOpenContactPicker: () -> Unit,
    onDismissContactPicker: () -> Unit,
    onContactQueryChange: (String) -> Unit,
    onContactSelected: (es.c0n1j.blocfone.data.ContactRef) -> Unit,
    onRemoveContact: (String) -> Unit,
    onAddManual: (String, (Boolean) -> Unit) -> Unit,
    onRemoveManual: (String) -> Unit,
    onRetrySync: () -> Unit,
    onRetrySettingsRead: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.isLoading) StateMessage(R.string.settings_loading)
            state.settingsError?.let { error ->
                SectionCard(title = stringResource(R.string.settings_read_error_title)) {
                    Text(
                        text = stringResource(R.string.settings_read_error_with_detail, error),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    OutlinedButton(onClick = onRetrySettingsRead) { Text(stringResource(R.string.retry)) }
                }
            }
            if (state.hasSettings) {
                StatusCard(
                    rules = state.rules,
                    roleGranted = roleGranted,
                    roleAvailable = roleAvailable,
                    contactsGranted = contactsGranted,
                    onRequestContacts = onRequestContacts,
                    onRequestRole = onRequestRole,
                    onBlockingEnabledChange = onBlockingEnabledChange,
                )
                ModesCard(state.rules, onSelectMode)
                if (state.rules.mode == BlockingMode.ALL_INCOMING) {
                    ExceptionsCard(
                        state = state,
                        contactsGranted = contactsGranted,
                        onRequestContacts = onRequestContacts,
                        onOpenContactPicker = onOpenContactPicker,
                        onRemoveContact = onRemoveContact,
                        onAddManual = onAddManual,
                        onRemoveManual = onRemoveManual,
                        onRetrySync = onRetrySync,
                    )
                } else if (state.rules.mode == BlockingMode.SELECTED_NUMBERS) {
                    SelectedNumbers(state.rules.selectedNumbers, onAddSelected, onRemoveSelected)
                } else {
                    Text(
                        text = stringResource(R.string.exceptions_mode_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            state.error?.let { error ->
                Text(
                    text = stringResource(R.string.storage_error_with_detail, error),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            SectionCard(title = stringResource(R.string.privacy_title)) {
                Text(stringResource(R.string.privacy_body), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.limitations_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    ContactPickerDialog(
        picker = state.picker,
        selectedLookupKeys = state.exceptions.contacts.mapTo(mutableSetOf()) { it.lookupKey },
        onDismiss = onDismissContactPicker,
        onQueryChange = onContactQueryChange,
        onSelected = onContactSelected,
    )
}

@Composable
private fun SelectedNumbers(
    numbers: Set<String>,
    onAddNumber: (String, (Boolean) -> Unit) -> Unit,
    onRemoveNumber: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var invalidInput by remember { mutableStateOf(false) }
    SectionCard(title = stringResource(R.string.numbers_title)) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it; invalidInput = false },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.number_hint)) },
            singleLine = true,
            isError = invalidInput,
        supportingText = if (invalidInput) {
            { Text(stringResource(R.string.invalid_number)) }
        } else {
            null
        },
        )
        Button(onClick = {
            val submitted = input
            onAddNumber(submitted) { added ->
                if (input == submitted) {
                    invalidInput = !added
                    if (added) input = ""
                }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_number)) }
        Text(stringResource(R.string.exact_match_note), style = MaterialTheme.typography.bodySmall)
        if (numbers.isEmpty()) Text(stringResource(R.string.empty_numbers))
        numbers.sorted().forEach { number ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(number, modifier = Modifier.weight(1f))
                TextButton(onClick = { onRemoveNumber(number) }) { Text(stringResource(R.string.remove_number)) }
            }
        }
    }
}

@Composable
private fun StatusCard(
    rules: ScreeningRules,
    roleGranted: Boolean,
    roleAvailable: Boolean,
    contactsGranted: Boolean,
    onRequestRole: () -> Unit,
    onRequestContacts: () -> Unit,
    onBlockingEnabledChange: (Boolean) -> Unit,
) = SectionCard(title = stringResource(R.string.status_title)) {
    val label = stringResource(R.string.blocking_enabled_label)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.blocking_enabled_description), style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = rules.isBlockingEnabled,
            onCheckedChange = onBlockingEnabledChange,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
    HorizontalDivider()
    StatusRow(stringResource(R.string.role_label), roleGranted, stringResource(R.string.grant_role), roleAvailable, onRequestRole)
    if (!roleAvailable) Text(stringResource(R.string.role_unavailable), color = MaterialTheme.colorScheme.error)
    HorizontalDivider()
    StatusRow(stringResource(R.string.contacts_label), contactsGranted, stringResource(R.string.grant_contacts), true, onRequestContacts)
}

@Composable
private fun ModesCard(rules: ScreeningRules, onSelectMode: (BlockingMode) -> Unit) =
    SectionCard(title = stringResource(R.string.mode_title)) {
        ModeRow(R.string.mode_all_title, R.string.mode_all_description, rules.mode == BlockingMode.ALL_INCOMING) {
            onSelectMode(BlockingMode.ALL_INCOMING)
        }
        ModeRow(R.string.mode_unknown_title, R.string.mode_unknown_description, rules.mode == BlockingMode.UNKNOWN_NUMBERS) {
            onSelectMode(BlockingMode.UNKNOWN_NUMBERS)
        }
        ModeRow(R.string.mode_selected_title, R.string.mode_selected_description, rules.mode == BlockingMode.SELECTED_NUMBERS) {
            onSelectMode(BlockingMode.SELECTED_NUMBERS)
        }
    }

@Composable
private fun ExceptionsCard(
    state: BlocFoneUiState,
    contactsGranted: Boolean,
    onRequestContacts: () -> Unit,
    onOpenContactPicker: () -> Unit,
    onRemoveContact: (String) -> Unit,
    onAddManual: (String, (Boolean) -> Unit) -> Unit,
    onRemoveManual: (String) -> Unit,
    onRetrySync: () -> Unit,
) = SectionCard(title = stringResource(R.string.exceptions_title)) {
    Text(stringResource(R.string.exceptions_description), style = MaterialTheme.typography.bodyMedium)
    SyncStatus(state.syncState, contactsGranted, onRequestContacts, onRetrySync)
    Button(onClick = onOpenContactPicker, enabled = contactsGranted, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.select_contact))
    }
    ContactList(state.exceptions.contacts, state.exceptions.contactContributions, onRemoveContact)
    ManualNumbers(state.exceptions.manualNumbers, onAddManual, onRemoveManual)
}

@Composable
private fun SyncStatus(
    syncState: ContactSyncState,
    contactsGranted: Boolean,
    onRequestContacts: () -> Unit,
    onRetrySync: () -> Unit,
) {
    when (syncState) {
        ContactSyncState.Idle, ContactSyncState.Synced -> Unit
        ContactSyncState.Syncing -> StateMessage(R.string.contacts_syncing)
        ContactSyncState.PermissionRequired -> {
            StateMessage(R.string.contacts_permission_cache)
            if (!contactsGranted) OutlinedButton(onClick = onRequestContacts) { Text(stringResource(R.string.grant_contacts)) }
        }
        is ContactSyncState.Stale -> {
            StateMessage(R.string.contacts_sync_error)
            OutlinedButton(onClick = onRetrySync) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun StateMessage(textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun ContactList(
    contacts: Set<es.c0n1j.blocfone.data.ContactRef>,
    contributions: Set<ContactContribution>,
    onRemoveContact: (String) -> Unit,
) {
    Text(stringResource(R.string.selected_contacts_title), style = MaterialTheme.typography.titleMedium)
    if (contacts.isEmpty()) {
        Text(stringResource(R.string.empty_contacts), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val numbersByLookupKey = contributions.associate { it.contact.lookupKey to it.numbers }
    contacts.sortedBy { it.displayName }.forEach { contact ->
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.displayName.ifBlank { stringResource(R.string.unnamed_contact) })
                Text(
                    stringResource(R.string.contact_number_count, numbersByLookupKey[contact.lookupKey].orEmpty().size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { onRemoveContact(contact.lookupKey) }) {
                Text(stringResource(R.string.remove_contact))
            }
        }
    }
}

@Composable
private fun ManualNumbers(
    numbers: Set<String>,
    onAddManual: (String, (Boolean) -> Unit) -> Unit,
    onRemoveManual: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var invalidInput by remember { mutableStateOf(false) }
    Text(stringResource(R.string.manual_numbers_title), style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        value = input,
        onValueChange = { input = it; invalidInput = false },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.number_hint)) },
        singleLine = true,
        isError = invalidInput,
        supportingText = if (invalidInput) {
            { Text(stringResource(R.string.invalid_manual_number)) }
        } else {
            null
        },
    )
    Button(onClick = {
        val submitted = input
        onAddManual(submitted) { added ->
            if (input == submitted) {
                invalidInput = !added
                if (added) input = ""
            }
        }
    }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_exception)) }
    if (numbers.isEmpty()) Text(stringResource(R.string.empty_manual_numbers), color = MaterialTheme.colorScheme.onSurfaceVariant)
    numbers.sorted().forEach { number ->
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(number, modifier = Modifier.weight(1f))
            TextButton(onClick = { onRemoveManual(number) }) { Text(stringResource(R.string.remove_number)) }
        }
    }
}

@Composable
private fun ContactPickerDialog(
    picker: ContactPickerState,
    selectedLookupKeys: Set<String>,
    onDismiss: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSelected: (es.c0n1j.blocfone.data.ContactRef) -> Unit,
) {
    if (picker == ContactPickerState.Hidden) return
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.contact_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; onQueryChange(it) },
                    label = { Text(stringResource(R.string.search_contacts)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                when (picker) {
                    ContactPickerState.Loading -> StateMessage(R.string.contacts_loading)
                    is ContactPickerState.Error -> StateMessage(R.string.contacts_load_error)
                    is ContactPickerState.Results -> ContactPickerResults(
                        contacts = picker.contacts,
                        selectedLookupKeys = selectedLookupKeys,
                        onSelected = onSelected,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    ContactPickerState.Hidden -> Unit
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun ContactPickerResults(
    contacts: List<ContactCandidate>,
    selectedLookupKeys: Set<String>,
    onSelected: (es.c0n1j.blocfone.data.ContactRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (contacts.isEmpty()) {
        StateMessage(R.string.contacts_empty)
        return
    }
    LazyColumn(modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        items(contacts, key = { candidate -> candidate.contact.lookupKey }) { candidate ->
            val alreadySelected = candidate.contact.lookupKey in selectedLookupKeys
            val name = candidate.contact.displayName.ifBlank { stringResource(R.string.unnamed_contact) }
            val selectionState = stringResource(
                if (alreadySelected) R.string.contact_already_selected else R.string.select_contact,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !alreadySelected, role = Role.Button) { onSelected(candidate.contact) }
                    .semantics(mergeDescendants = true) { stateDescription = selectionState }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(name)
                    Text(
                        stringResource(R.string.contact_all_numbers, candidate.numbers.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(selectionState)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun StatusRow(label: String, granted: Boolean, buttonText: String, enabled: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(if (granted) R.string.active else R.string.inactive))
        }
        if (!granted) OutlinedButton(onClick = onClick, enabled = enabled) { Text(buttonText) }
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

@Preview(showBackground = true)
@Composable
private fun BlocFoneScreenPreview() = BlocFoneTheme {
    BlocFoneScreen(
        state = BlocFoneUiState(
            rules = ScreeningRules(mode = BlockingMode.ALL_INCOMING),
            hasSettings = true,
            isLoading = false,
        ),
        roleGranted = true, roleAvailable = true, contactsGranted = true,
        onRequestRole = {}, onRequestContacts = {}, onBlockingEnabledChange = {}, onSelectMode = {},
        onAddSelected = { _, _ -> }, onRemoveSelected = {},
        onOpenContactPicker = {}, onDismissContactPicker = {}, onContactQueryChange = {}, onContactSelected = {},
        onRemoveContact = {}, onAddManual = { _, _ -> }, onRemoveManual = {}, onRetrySync = {},
        onRetrySettingsRead = {},
    )
}
