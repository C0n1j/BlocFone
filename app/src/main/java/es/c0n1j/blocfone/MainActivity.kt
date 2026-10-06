package es.c0n1j.blocfone

import android.Manifest
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import es.c0n1j.blocfone.contacts.AndroidContactsDataSource
import es.c0n1j.blocfone.contacts.ContactSyncCoordinator
import es.c0n1j.blocfone.data.SettingsRepository
import es.c0n1j.blocfone.ui.BlocFoneScreen
import es.c0n1j.blocfone.ui.BlocFoneViewModel
import es.c0n1j.blocfone.ui.theme.BlocFoneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = SettingsRepository(applicationContext)
        val contactsDataSource = AndroidContactsDataSource(contentResolver)
        val contactSyncCoordinator = ContactSyncCoordinator(
            context = applicationContext,
            repository = repository,
            contactsDataSource = contactsDataSource,
            scope = lifecycleScope,
        )
        lifecycle.addObserver(contactSyncCoordinator)

        setContent {
            BlocFoneTheme {
                BlocFoneApp(repository, contactsDataSource, contactSyncCoordinator)
            }
        }
    }
}

@Composable
private fun BlocFoneApp(
    repository: SettingsRepository,
    contactsDataSource: AndroidContactsDataSource,
    contactSyncCoordinator: ContactSyncCoordinator,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val roleManager = remember { context.getSystemService(RoleManager::class.java) }
    val roleAvailable = remember { roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) }
    var roleGranted by remember { mutableStateOf(false) }
    var contactsGranted by remember { mutableStateOf(false) }
    val viewModel: BlocFoneViewModel = viewModel(
        factory = BlocFoneViewModel.factory(repository, contactsDataSource, contactSyncCoordinator),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    fun refreshStatus() {
        roleGranted = roleAvailable && roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        contactsGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onContactsPermissionChanged(contactsGranted)
    }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { refreshStatus() }
    val contactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refreshStatus() }

    LaunchedEffect(Unit) { refreshStatus() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BlocFoneScreen(
        state = state,
        roleGranted = roleGranted,
        roleAvailable = roleAvailable,
        contactsGranted = contactsGranted,
        onRequestRole = {
            roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
        },
        onRequestContacts = { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) },
        onBlockingEnabledChange = viewModel::setBlockingEnabled,
        onSelectMode = viewModel::setMode,
        onAddSelected = viewModel::addSelectedNumber,
        onRemoveSelected = viewModel::removeSelectedNumber,
        onOpenContactPicker = viewModel::openContactPicker,
        onDismissContactPicker = viewModel::dismissContactPicker,
        onContactQueryChange = viewModel::searchContacts,
        onContactSelected = viewModel::selectContact,
        onRemoveContact = viewModel::removeContact,
        onAddManual = viewModel::addManualNumber,
        onRemoveManual = viewModel::removeManualNumber,
        onRetrySync = viewModel::retrySync,
        onRetrySettingsRead = viewModel::retrySettingsRead,
    )
}
