package es.c0n1j.blocfone

import android.Manifest
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import es.c0n1j.blocfone.data.SettingsRepository
import es.c0n1j.blocfone.domain.BlockingMode
import es.c0n1j.blocfone.domain.ScreeningRules
import es.c0n1j.blocfone.ui.theme.BlocFoneTheme
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private enum class AddNumberResult {
    ADDED,
    INVALID,
    STORAGE_FAILURE,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = SettingsRepository(applicationContext)

        setContent {
            BlocFoneTheme {
                BlocFoneApp(repository)
            }
        }
    }
}


@Composable
private fun BlocFoneApp(repository: SettingsRepository) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val roleManager = remember { context.getSystemService(RoleManager::class.java) }
    val roleAvailable = remember { roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) }
    var roleGranted by remember { mutableStateOf(false) }
    var contactsGranted by remember { mutableStateOf(false) }
    var storageError by remember { mutableStateOf(false) }

    fun refreshStatus() {
        roleGranted = roleAvailable && roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        contactsGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { refreshStatus() }
    val contactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refreshStatus() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { refreshStatus() }

    val rules by remember(repository) {
        repository.rules
            .onEach { storageError = false }
            .catch {
                storageError = true
                emit(ScreeningRules())
            }
    }.collectAsState(initial = ScreeningRules())
    val scope = rememberCoroutineScope()

    BlocFoneScreen(
        rules = rules,
        roleGranted = roleGranted,
        roleAvailable = roleAvailable,
        contactsGranted = contactsGranted,
        storageError = storageError,
        onRequestRole = {
            roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
        },
        onRequestContacts = {
            contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
        },
        onSelectMode = { mode ->
            scope.launch {
                runCatching { repository.setMode(mode) }
                    .onSuccess { storageError = false }
                    .onFailure { storageError = true }
            }
        },
        onAddNumber = { input, onResult ->
            scope.launch {
                runCatching { repository.addNumber(input) }
                    .onSuccess { added ->
                        storageError = false
                        onResult(if (added) AddNumberResult.ADDED else AddNumberResult.INVALID)
                    }
                    .onFailure {
                        storageError = true
                        onResult(AddNumberResult.STORAGE_FAILURE)
                    }
            }
        },
        onRemoveNumber = { number ->
            scope.launch {
                runCatching { repository.removeNumber(number) }
                    .onSuccess { storageError = false }
                    .onFailure { storageError = true }
            }
        },
    )
}

@Composable
private fun BlocFoneScreen(
    rules: ScreeningRules,
    roleGranted: Boolean,
    roleAvailable: Boolean,
    contactsGranted: Boolean,
    storageError: Boolean,
    onRequestRole: () -> Unit,
    onRequestContacts: () -> Unit,
    onSelectMode: (BlockingMode) -> Unit,
    onAddNumber: (String, (AddNumberResult) -> Unit) -> Unit,
    onRemoveNumber: (String) -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
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

            SectionCard(title = stringResource(R.string.status_title)) {
                StatusRow(
                    label = stringResource(R.string.role_label),
                    granted = roleGranted,
                    buttonText = stringResource(R.string.grant_role),
                    enabled = roleAvailable,
                    onClick = onRequestRole,
                )
                if (!roleAvailable) {
                    Text(
                        stringResource(R.string.role_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                HorizontalDivider()
                StatusRow(
                    label = stringResource(R.string.contacts_label),
                    granted = contactsGranted,
                    buttonText = stringResource(R.string.grant_contacts),
                    onClick = onRequestContacts,
                )
            }

            SectionCard(title = stringResource(R.string.mode_title)) {
                ModeRow(
                    title = stringResource(R.string.mode_all_title),
                    description = stringResource(R.string.mode_all_description),
                    selected = rules.mode == BlockingMode.ALL_INCOMING,
                    onClick = { onSelectMode(BlockingMode.ALL_INCOMING) },
                )
                ModeRow(
                    title = stringResource(R.string.mode_unknown_title),
                    description = stringResource(R.string.mode_unknown_description),
                    selected = rules.mode == BlockingMode.UNKNOWN_NUMBERS,
                    onClick = { onSelectMode(BlockingMode.UNKNOWN_NUMBERS) },
                )
                ModeRow(
                    title = stringResource(R.string.mode_selected_title),
                    description = stringResource(R.string.mode_selected_description),
                    selected = rules.mode == BlockingMode.SELECTED_NUMBERS,
                    onClick = { onSelectMode(BlockingMode.SELECTED_NUMBERS) },
                )
            }

            SelectedNumbersCard(rules.selectedNumbers, onAddNumber, onRemoveNumber)

            if (storageError) {
                Text(
                    text = stringResource(R.string.storage_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            SectionCard(title = stringResource(R.string.privacy_title)) {
                Text(
                    text = stringResource(R.string.privacy_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.limitations_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    granted: Boolean,
    buttonText: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(if (granted) R.string.active else R.string.inactive),
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        if (!granted) {
            OutlinedButton(onClick = onClick, enabled = enabled) {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun ModeRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SelectedNumbersCard(
    numbers: Set<String>,
    onAddNumber: (String, (AddNumberResult) -> Unit) -> Unit,
    onRemoveNumber: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var invalidInput by remember { mutableStateOf(false) }

    SectionCard(title = stringResource(R.string.numbers_title)) {
        OutlinedTextField(
            value = input,
            onValueChange = {
                input = it
                invalidInput = false
            },
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
        Button(
            onClick = {
                val submittedInput = input
                onAddNumber(submittedInput) { result ->
                    if (input == submittedInput) {
                        invalidInput = result == AddNumberResult.INVALID
                        if (result == AddNumberResult.ADDED) input = ""
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.add_number))
        }
        Text(
            stringResource(R.string.exact_match_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (numbers.isEmpty()) {
            Text(
                stringResource(R.string.empty_numbers),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            numbers.sorted().forEach { number ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(number, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { onRemoveNumber(number) }) {
                        Text(stringResource(R.string.remove_number))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
private fun BlocFoneScreenPreview() {
    BlocFoneTheme {
        BlocFoneScreen(
            rules = ScreeningRules(
                mode = BlockingMode.UNKNOWN_NUMBERS,
                selectedNumbers = setOf("+34 123 456 789", "987654321")
            ),
            roleGranted = true,
            roleAvailable = true,
            contactsGranted = true,
            storageError = false,
            onRequestRole = {},
            onRequestContacts = {},
            onSelectMode = {},
            onAddNumber = { _, _ -> },
            onRemoveNumber = {}
        )
    }
}

@Preview(showBackground = true, name = "Permissions Missing", group = "States")
@Composable
private fun BlocFoneScreenPermissionsPreview() {
    BlocFoneTheme {
        BlocFoneScreen(
            rules = ScreeningRules(),
            roleGranted = false,
            roleAvailable = true,
            contactsGranted = false,
            storageError = false,
            onRequestRole = {},
            onRequestContacts = {},
            onSelectMode = {},
            onAddNumber = { _, _ -> },
            onRemoveNumber = {}
        )
    }
}
