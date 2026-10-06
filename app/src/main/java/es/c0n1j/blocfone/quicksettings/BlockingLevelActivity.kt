package es.c0n1j.blocfone.quicksettings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.c0n1j.blocfone.R
import es.c0n1j.blocfone.data.SettingsReadState
import es.c0n1j.blocfone.data.SettingsRepository
import es.c0n1j.blocfone.domain.BlockingMode
import es.c0n1j.blocfone.ui.BlockingModeSelector
import es.c0n1j.blocfone.ui.theme.BlocFoneTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class BlockingLevelActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = SettingsRepository(applicationContext)

        setContent {
            BlocFoneTheme {
                val settingsState by repository.settings.collectAsStateWithLifecycle(
                    initialValue = SettingsReadState.Loading(lastValid = null),
                )
                val scope = rememberCoroutineScope()
                var saveError by remember { mutableStateOf(false) }
                val selectedMode = settingsState.lastValid?.rules?.mode

                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .safeDrawingPadding()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.blocking_level_title),
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.close),
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.blocking_level_description),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (selectedMode != null) {
                            BlockingModeSelector(
                                selectedMode = selectedMode,
                                onSelectMode = { mode: BlockingMode ->
                                    scope.launch {
                                        try {
                                            repository.setMode(mode)
                                            saveError = false
                                        } catch (error: CancellationException) {
                                            throw error
                                        } catch (_: Throwable) {
                                            saveError = true
                                        }
                                    }
                                },
                            )
                        } else {
                            Text(stringResource(R.string.settings_loading))
                        }
                        if (saveError) {
                            Text(
                                text = stringResource(R.string.storage_error),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}
