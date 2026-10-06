package es.c0n1j.blocfone.quicksettings

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import es.c0n1j.blocfone.data.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

class BlocFoneTileService : TileService() {
    private lateinit var repository: SettingsRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listeningJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        repository = SettingsRepository(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        listeningJob?.cancel()
        listeningJob = serviceScope.launch {
            repository.settings
                .mapNotNull { state -> state.lastValid?.rules?.isBlockingEnabled }
                .collect(::updateTile)
        }
    }

    override fun onStopListening() {
        listeningJob?.cancel()
        listeningJob = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            try {
                updateTile(repository.toggleBlockingEnabled())
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                repository.retrySettingsRead()
            }
        }
    }

    override fun onDestroy() {
        listeningJob = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun updateTile(isBlockingEnabled: Boolean) {
        val tile = qsTile ?: return
        val presentation = tilePresentation(
            isBlockingEnabled = isBlockingEnabled,
            supportsSubtitle = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
        )
        tile.state = when (presentation.state) {
            TileVisualState.ACTIVE -> Tile.STATE_ACTIVE
            TileVisualState.INACTIVE -> Tile.STATE_INACTIVE
        }
        tile.label = getString(presentation.labelRes)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = presentation.subtitleRes?.let(::getString)
        }
        tile.updateTile()
    }
}
