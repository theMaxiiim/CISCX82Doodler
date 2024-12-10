package com.maxdejesus.ciscx82doodler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                DrawingApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingApp() {
    val paths = remember { mutableStateListOf<Pair<Path, PaintOptions>>() }
    val undonePaths = remember { mutableStateListOf<Pair<Path, PaintOptions>>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentColor by remember { mutableStateOf(Color.Black) }
    var currentStrokeWidth by remember { mutableStateOf(5f) }
    var currentAlpha by remember { mutableStateOf(1f) }

    var showColorPicker by remember { mutableStateOf(false) }
    var showBrushSizePicker by remember { mutableStateOf(false) }
    var showOpacityPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Drawing Canvas") },
                actions = {
                    TextButton(onClick = {
                        paths.clear()
                        undonePaths.clear()
                    }) {
                        Text("Clear")
                    }
                    TextButton(onClick = { showColorPicker = true }) {
                        Text("Color")
                    }
                    TextButton(onClick = { showBrushSizePicker = true }) {
                        Text("Brush Size")
                    }
                    TextButton(onClick = { showOpacityPicker = true }) {
                        Text("Opacity")
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        // Color Picker Dialog
        if (showColorPicker) {
            ColorPickerDialog(
                onColorSelected = { color ->
                    currentColor = color
                    showColorPicker = false
                },
                onDismissRequest = {
                    showColorPicker = false
                }
            )
        }

        // Brush Size Picker Dialog
        if (showBrushSizePicker) {
            BrushSizePickerDialog(
                currentSize = currentStrokeWidth,
                onSizeSelected = { size ->
                    currentStrokeWidth = size
                    showBrushSizePicker = false
                },
                onDismissRequest = {
                    showBrushSizePicker = false
                }
            )
        }

        // Opacity Picker Dialog
        if (showOpacityPicker) {
            OpacityPickerDialog(
                currentAlpha = currentAlpha,
                onAlphaSelected = { alpha ->
                    currentAlpha = alpha
                    showOpacityPicker = false
                },
                onDismissRequest = {
                    showOpacityPicker = false
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    // Detect long press to invert colors
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                invertColors(paths)
                            }
                        )
                    }
                    // Detect dragging for drawing
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath = Path().apply {
                                    moveTo(offset.x, offset.y)
                                }
                            },
                            onDrag = { change, _ ->
                                currentPath?.lineTo(change.position.x, change.position.y)
                            },
                            onDragEnd = {
                                currentPath?.let { path ->
                                    paths.add(
                                        path to PaintOptions(
                                            color = currentColor,
                                            strokeWidth = currentStrokeWidth,
                                            alpha = currentAlpha
                                        )
                                    )
                                    undonePaths.clear()
                                }
                                currentPath = null
                            }
                        )
                    }
            ) {
                // Draw all the completed paths
                for ((path, paintOptions) in paths) {
                    drawPath(
                        path = path,
                        color = paintOptions.color.copy(alpha = paintOptions.alpha),
                        style = Stroke(width = paintOptions.strokeWidth)
                    )
                }
                // Draw the path currently being drawn
                currentPath?.let { path ->
                    drawPath(
                        path = path,
                        color = currentColor.copy(alpha = currentAlpha),
                        style = Stroke(width = currentStrokeWidth)
                    )
                }
            }

            // Undo/Redo buttons at the bottom-left corner
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        if (paths.isNotEmpty()) {
                            val lastPath = paths.removeLast()
                            undonePaths.add(lastPath)
                        }
                    },
                    enabled = paths.isNotEmpty()
                ) {
                    Text("Undo")
                }
                TextButton(
                    onClick = {
                        if (undonePaths.isNotEmpty()) {
                            val pathToRestore = undonePaths.removeLast()
                            paths.add(pathToRestore)
                        }
                    },
                    enabled = undonePaths.isNotEmpty()
                ) {
                    Text("Redo")
                }
            }
        }
    }
}

data class PaintOptions(val color: Color, val strokeWidth: Float, val alpha: Float)

fun invertColor(color: Color): Color {
    return Color(
        red = 1f - color.red,
        green = 1f - color.green,
        blue = 1f - color.blue,
        alpha = color.alpha
    )
}

fun invertColors(paths: MutableList<Pair<Path, PaintOptions>>) {
    for (i in paths.indices) {
        val (path, paintOptions) = paths[i]
        paths[i] = path to paintOptions.copy(color = invertColor(paintOptions.color))
    }
}

@Composable
fun ColorPickerDialog(
    onColorSelected: (Color) -> Unit,
    onDismissRequest: () -> Unit
) {
    val colors = listOf(
        Color.Black, Color.Red, Color.Green, Color.Blue,
        Color.Yellow, Color.Magenta, Color.Cyan, Color.Gray
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Color") },
        text = {
            Column {
                for (colorRow in colors.chunked(4)) {
                    Row {
                        for (color in colorRow) {
                            TextButton(
                                onClick = { onColorSelected(color) },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(color)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
fun BrushSizePickerDialog(
    currentSize: Float,
    onSizeSelected: (Float) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sizes = listOf(2f, 5f, 10f, 15f, 20f)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Brush Size") },
        text = {
            Column {
                for (size in sizes) {
                    TextButton(
                        onClick = { onSizeSelected(size) },
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .height(size.dp)
                                .fillMaxWidth()
                                .background(Color.Black)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
fun OpacityPickerDialog(
    currentAlpha: Float,
    onAlphaSelected: (Float) -> Unit,
    onDismissRequest: () -> Unit
) {
    val opacities = listOf(1f, 0.75f, 0.5f, 0.25f, 0.1f)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Opacity") },
        text = {
            Column {
                for (alpha in opacities) {
                    val percentage = (alpha * 100).toInt()
                    TextButton(
                        onClick = { onAlphaSelected(alpha) },
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.Black.copy(alpha = alpha))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$percentage%")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}