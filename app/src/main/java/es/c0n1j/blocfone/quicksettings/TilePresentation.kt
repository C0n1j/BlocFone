package es.c0n1j.blocfone.quicksettings

import androidx.annotation.StringRes
import es.c0n1j.blocfone.R

enum class TileVisualState {
    ACTIVE,
    INACTIVE,
}

data class TilePresentation(
    val state: TileVisualState,
    @param:StringRes val labelRes: Int,
    @param:StringRes val subtitleRes: Int?,
)

fun tilePresentation(
    isBlockingEnabled: Boolean,
    supportsSubtitle: Boolean,
): TilePresentation = TilePresentation(
    state = if (isBlockingEnabled) TileVisualState.ACTIVE else TileVisualState.INACTIVE,
    labelRes = R.string.app_name,
    subtitleRes = if (supportsSubtitle) {
        if (isBlockingEnabled) R.string.tile_blocking_active else R.string.tile_blocking_paused
    } else {
        null
    },
)
