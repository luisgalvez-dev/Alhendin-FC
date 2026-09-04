package com.luis.alhendinfc.ui.pizarra

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class DrawTool {
    PEN,
    ARROW,
    OVAL,
    ERASER
}

enum class BoardBackground {
    FIELD,
    IMAGE,
    VIDEO
}

data class BoardPoint(val x: Float, val y: Float)

data class DrawStroke(
    val tool: DrawTool,
    val colorArgb: Long,
    val width: Float,
    val points: List<BoardPoint>
)

/** Colores ARGB empaquetados (mismo formato que [android.graphics.Color]). */
object PizarraColors {
    const val WHITE = 0xFFFFFFFFL
    const val YELLOW = 0xFFFFEB3BL
    const val RED = 0xFFFF5252L
    const val BLUE = 0xFF40C4FFL
    const val BLACK = 0xFF212121L
}

data class PizarraUiState(
    val tool: DrawTool = DrawTool.PEN,
    val colorArgb: Long = PizarraColors.WHITE,
    val strokeWidth: Float = 8f,
    val background: BoardBackground = BoardBackground.FIELD,
    val mediaUri: Uri? = null,
    val strokes: List<DrawStroke> = emptyList(),
    val currentStroke: DrawStroke? = null,
    val isVideoPlaying: Boolean = false
)

class PizarraViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PizarraUiState())
    val uiState: StateFlow<PizarraUiState> = _uiState.asStateFlow()

    fun setTool(tool: DrawTool) {
        _uiState.update { it.copy(tool = tool) }
    }

    fun setColor(colorArgb: Long) {
        _uiState.update { it.copy(colorArgb = colorArgb) }
    }

    fun setStrokeWidth(width: Float) {
        _uiState.update { it.copy(strokeWidth = width) }
    }

    fun useDefaultField() {
        _uiState.update {
            it.copy(
                background = BoardBackground.FIELD,
                mediaUri = null,
                isVideoPlaying = false
            )
        }
    }

    fun setImage(uri: Uri) {
        _uiState.update {
            it.copy(
                background = BoardBackground.IMAGE,
                mediaUri = uri,
                isVideoPlaying = false
            )
        }
    }

    fun setVideo(uri: Uri) {
        _uiState.update {
            it.copy(
                background = BoardBackground.VIDEO,
                mediaUri = uri,
                isVideoPlaying = false
            )
        }
    }

    fun setVideoPlaying(playing: Boolean) {
        _uiState.update { it.copy(isVideoPlaying = playing) }
    }

    fun startStroke(point: BoardPoint) {
        val state = _uiState.value
        val stroke = DrawStroke(
            tool = state.tool,
            colorArgb = state.colorArgb,
            width = state.strokeWidth,
            points = listOf(point)
        )
        _uiState.update { it.copy(currentStroke = stroke) }
    }

    fun updateStroke(point: BoardPoint) {
        val current = _uiState.value.currentStroke ?: return
        val updated = when (current.tool) {
            DrawTool.PEN, DrawTool.ERASER ->
                current.copy(points = current.points + point)
            DrawTool.ARROW, DrawTool.OVAL -> {
                val start = current.points.firstOrNull() ?: point
                current.copy(points = listOf(start, point))
            }
        }
        _uiState.update { it.copy(currentStroke = updated) }
    }

    fun finishStroke() {
        val current = _uiState.value.currentStroke ?: return
        if (current.points.size < 2 &&
            (current.tool == DrawTool.ARROW || current.tool == DrawTool.OVAL)
        ) {
            _uiState.update { it.copy(currentStroke = null) }
            return
        }
        if (current.points.isEmpty()) {
            _uiState.update { it.copy(currentStroke = null) }
            return
        }
        _uiState.update {
            it.copy(
                strokes = it.strokes + current,
                currentStroke = null
            )
        }
    }

    fun undo() {
        _uiState.update { state ->
            when {
                state.currentStroke != null -> state.copy(currentStroke = null)
                state.strokes.isNotEmpty() -> state.copy(strokes = state.strokes.dropLast(1))
                else -> state
            }
        }
    }

    fun clearAll() {
        _uiState.update {
            it.copy(strokes = emptyList(), currentStroke = null)
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PizarraViewModel() as T
                }
            }
    }
}
