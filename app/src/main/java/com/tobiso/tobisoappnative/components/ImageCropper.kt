package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import java.io.File
import kotlin.math.*

data class SimpleCropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = left + width / 2f
    val centerY: Float get() = top + height / 2f
}

@Composable
fun ImageCropperDialog(
    imageUri: String,
    onCropComplete: (String) -> Unit,
    onDismiss: () -> Unit,
    aspectRatio: Float = 1f
) {
    var containerSize by remember { mutableStateOf(Size.Zero) }
    var cropRect by remember { 
        mutableStateOf(SimpleCropRect(100f, 100f, 300f, 300f)) 
    }
    var isDragging by remember { mutableStateOf(false) }
    var isResizing by remember { mutableStateOf(false) }
    var resizeHandle by remember { mutableStateOf(-1) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    var dragStartRect by remember { mutableStateOf(SimpleCropRect(0f, 0f, 0f, 0f)) }
    // Accumulate drag deltas between onDrag calls so we can compute the
    // total movement/resizing from the start of the gesture.
    var accumulatedDrag by remember { mutableStateOf(Offset.Zero) }
    
    val context = LocalContext.current
    val density = LocalDensity.current
    val minCropSize = with(density) { 80.dp.toPx() }
    val handleSize = with(density) { 20.dp.toPx() }

    // Inicializace crop rect když známe velikost kontejneru - vždy největší možná čtvercová oblast
    LaunchedEffect(containerSize) {
        if (containerSize != Size.Zero && cropRect.width == 200f) {
            val padding = 20f // Minimální padding od okrajů
            
            // Najdi největší možný čtverec v kontejneru
            val maxSquareSize = min(
                containerSize.width - 2 * padding,
                containerSize.height - 2 * padding
            )
            
            cropRect = SimpleCropRect(
                left = containerSize.width / 2f - maxSquareSize / 2f,
                top = containerSize.height / 2f - maxSquareSize / 2f,
                right = containerSize.width / 2f + maxSquareSize / 2f,
                bottom = containerSize.height / 2f + maxSquareSize / 2f
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        // Consume system Back (button and gesture) so the dialog isn't
        // dismissed by the global back action. The UI still provides the
        // explicit Close icon to dismiss via onDismiss.
        BackHandler(enabled = true) {
            // Intentionally empty to consume the back event.
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Zrušit",
                            tint = Color.White
                        )
                    }
                    
                    Text(
                        "Oříznout obrázek",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    IconButton(
                        onClick = {
                            performCrop(
                                context = context,
                                imageUri = imageUri,
                                cropRect = cropRect,
                                containerSize = containerSize,
                                onComplete = onCropComplete
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Potvrdit",
                            tint = Color.White
                        )
                    }
                }
                
                // Image area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clipToBounds()
                ) {
                    // Background image
                    AsyncImage(
                        model = File(imageUri),
                        contentDescription = "Obrázek k oříznutí",
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { coordinates ->
                                containerSize = coordinates.size.toSize()
                            },
                        contentScale = ContentScale.Fit
                    )
                    
                    // Crop overlay and interaction
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val handle = detectHandle(offset, cropRect, handleSize)
                                        if (handle != -1) {
                                            isResizing = true
                                            resizeHandle = handle
                                            dragStartRect = cropRect
                                            accumulatedDrag = Offset.Zero
                                        } else if (isInsideCropArea(offset, cropRect, handleSize)) {
                                            isDragging = true
                                            dragStartOffset = offset
                                            dragStartRect = cropRect
                                            accumulatedDrag = Offset.Zero
                                        }
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        isResizing = false
                                        resizeHandle = -1
                                        accumulatedDrag = Offset.Zero
                                    }
                                ) { _, dragAmount ->
                                    // accumulate deltas so we can apply the total
                                    // movement/resizing relative to the rect at
                                    // gesture start. The dragAmount parameter is
                                    // the delta since the last event.
                                    accumulatedDrag = accumulatedDrag + dragAmount
                                    when {
                                        isDragging -> {
                                            cropRect = moveCropRect(
                                                rect = dragStartRect,
                                                dragAmount = accumulatedDrag,
                                                containerSize = containerSize,
                                                minSize = minCropSize
                                            )
                                        }
                                        isResizing -> {
                                            cropRect = resizeCropRect(
                                                rect = dragStartRect,
                                                handle = resizeHandle,
                                                dragAmount = accumulatedDrag,
                                                containerSize = containerSize,
                                                minSize = minCropSize
                                            )
                                        }
                                    }
                                }
                            }
                    ) {
                        if (containerSize != Size.Zero) {
                            drawCropOverlay(cropRect, containerSize, handleSize)
                        }
                    }
                    

                }
                
                // Instructions
                Text(
                    text = "Táhněte za střed pro přesunutí • Táhněte za rohy pro změnu velikosti",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ResizeHandles(
    cropRect: SimpleCropRect,
    onCropRectChange: (SimpleCropRect) -> Unit,
    handleSize: Float
) {
    // Handles se vykreslí přímo v Canvas overlay
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCropOverlay(
    cropRect: SimpleCropRect,
    containerSize: Size,
    handleSize: Float
) {
    // Dark overlay outside crop area
    val overlayColor = Color.Black.copy(alpha = 0.6f)
    
    // Top
    if (cropRect.top > 0) {
        drawRect(
            color = overlayColor,
            topLeft = Offset(0f, 0f),
            size = Size(containerSize.width, cropRect.top)
        )
    }
    
    // Bottom  
    if (cropRect.bottom < containerSize.height) {
        drawRect(
            color = overlayColor,
            topLeft = Offset(0f, cropRect.bottom),
            size = Size(containerSize.width, containerSize.height - cropRect.bottom)
        )
    }
    
    // Left
    if (cropRect.left > 0) {
        drawRect(
            color = overlayColor,
            topLeft = Offset(0f, cropRect.top),
            size = Size(cropRect.left, cropRect.height)
        )
    }
    
    // Right
    if (cropRect.right < containerSize.width) {
        drawRect(
            color = overlayColor,
            topLeft = Offset(cropRect.right, cropRect.top),
            size = Size(containerSize.width - cropRect.right, cropRect.height)
        )
    }
    
    // Crop border
    drawRect(
        color = Color.White,
        topLeft = Offset(cropRect.left, cropRect.top),
        size = Size(cropRect.width, cropRect.height),
        style = Stroke(width = 2.dp.toPx())
    )
    
    // Grid lines (rule of thirds)
    val gridColor = Color.White.copy(alpha = 0.5f)
    val strokeWidth = 1.dp.toPx()
    
    // Vertical lines
    for (i in 1..2) {
        val x = cropRect.left + (cropRect.width * i / 3f)
        drawLine(
            color = gridColor,
            start = Offset(x, cropRect.top),
            end = Offset(x, cropRect.bottom),
            strokeWidth = strokeWidth
        )
    }
    
    // Horizontal lines  
    for (i in 1..2) {
        val y = cropRect.top + (cropRect.height * i / 3f)
        drawLine(
            color = gridColor,
            start = Offset(cropRect.left, y),
            end = Offset(cropRect.right, y),
            strokeWidth = strokeWidth
        )
    }
    
    // Corner handles
    val handles = listOf(
        Offset(cropRect.left, cropRect.top),      // Top-left
        Offset(cropRect.right, cropRect.top),     // Top-right  
        Offset(cropRect.left, cropRect.bottom),   // Bottom-left
        Offset(cropRect.right, cropRect.bottom)   // Bottom-right
    )
    
    handles.forEach { handle ->
        drawCircle(
            color = Color.White,
            radius = handleSize / 2f,
            center = handle
        )
        drawCircle(
            color = Color.Blue,
            radius = handleSize / 3f,
            center = handle
        )
    }
}

private fun detectHandle(
    offset: Offset,
    cropRect: SimpleCropRect,
    handleSize: Float
): Int {
    val handles = listOf(
        Offset(cropRect.left, cropRect.top),      // 0: Top-left
        Offset(cropRect.right, cropRect.top),     // 1: Top-right  
        Offset(cropRect.left, cropRect.bottom),   // 2: Bottom-left
        Offset(cropRect.right, cropRect.bottom)   // 3: Bottom-right
    )
    
    handles.forEachIndexed { index, handle ->
        val distance = sqrt(
            (offset.x - handle.x).pow(2) + (offset.y - handle.y).pow(2)
        )
        if (distance <= handleSize) {
            return index
        }
    }
    return -1
}

private fun isInsideCropArea(
    offset: Offset,
    cropRect: SimpleCropRect,
    handleSize: Float
): Boolean {
    return offset.x >= cropRect.left + handleSize &&
           offset.x <= cropRect.right - handleSize &&
           offset.y >= cropRect.top + handleSize &&
           offset.y <= cropRect.bottom - handleSize
}

private fun moveCropRect(
    rect: SimpleCropRect,
    dragAmount: Offset,
    containerSize: Size,
    minSize: Float
): SimpleCropRect {
    val newLeft = (rect.left + dragAmount.x).coerceIn(0f, containerSize.width - rect.width)
    val newTop = (rect.top + dragAmount.y).coerceIn(0f, containerSize.height - rect.height)
    
    return rect.copy(
        left = newLeft,
        top = newTop,
        right = newLeft + rect.width,
        bottom = newTop + rect.height
    )
}

private fun resizeCropRect(
    rect: SimpleCropRect,
    handle: Int,
    dragAmount: Offset,
    containerSize: Size,
    minSize: Float
): SimpleCropRect {
    val newRect = when (handle) {
        0 -> rect.copy( // Top-left
            left = (rect.left + dragAmount.x).coerceIn(0f, rect.right - minSize),
            top = (rect.top + dragAmount.y).coerceIn(0f, rect.bottom - minSize)
        )
        1 -> rect.copy( // Top-right
            right = (rect.right + dragAmount.x).coerceIn(rect.left + minSize, containerSize.width),
            top = (rect.top + dragAmount.y).coerceIn(0f, rect.bottom - minSize)
        )
        2 -> rect.copy( // Bottom-left
            left = (rect.left + dragAmount.x).coerceIn(0f, rect.right - minSize),
            bottom = (rect.bottom + dragAmount.y).coerceIn(rect.top + minSize, containerSize.height)
        )
        3 -> rect.copy( // Bottom-right
            right = (rect.right + dragAmount.x).coerceIn(rect.left + minSize, containerSize.width),
            bottom = (rect.bottom + dragAmount.y).coerceIn(rect.top + minSize, containerSize.height)
        )
        else -> rect
    }
    
    return constrainCropRect(newRect, containerSize, minSize)
}

private fun constrainCropRect(
    rect: SimpleCropRect,
    containerSize: Size,
    minSize: Float
): SimpleCropRect {
    val constrainedLeft = rect.left.coerceIn(0f, containerSize.width - minSize)
    val constrainedTop = rect.top.coerceIn(0f, containerSize.height - minSize)
    val constrainedRight = rect.right.coerceIn(constrainedLeft + minSize, containerSize.width)
    val constrainedBottom = rect.bottom.coerceIn(constrainedTop + minSize, containerSize.height)
    
    // Force square aspect ratio
    val width = constrainedRight - constrainedLeft
    val height = constrainedBottom - constrainedTop
    val size = min(width, height)
    
    return SimpleCropRect(
        left = constrainedLeft,
        top = constrainedTop,
        right = constrainedLeft + size,
        bottom = constrainedTop + size
    )
}

private fun performCrop(
    context: android.content.Context,
    imageUri: String,
    cropRect: SimpleCropRect,
    containerSize: Size,
    onComplete: (String) -> Unit
) {
    try {
        val originalBitmap = android.graphics.BitmapFactory.decodeFile(imageUri)
        
        // Calculate scale factors for ContentScale.Fit
        val scaleX = originalBitmap.width.toFloat() / containerSize.width
        val scaleY = originalBitmap.height.toFloat() / containerSize.height
        val scale = min(scaleX, scaleY) // ContentScale.Fit uses min scale to fit entire image
        
        // Calculate actual image bounds within container
        val actualImageWidth = originalBitmap.width / scale
        val actualImageHeight = originalBitmap.height / scale
        val imageOffsetX = (containerSize.width - actualImageWidth) / 2f
        val imageOffsetY = (containerSize.height - actualImageHeight) / 2f
        
        // Adjust crop coordinates relative to actual image position
        val adjustedCropLeft = cropRect.left - imageOffsetX
        val adjustedCropTop = cropRect.top - imageOffsetY
        
        // Calculate actual crop coordinates on original bitmap
        val actualLeft = (adjustedCropLeft * scale).toInt().coerceAtLeast(0)
        val actualTop = (adjustedCropTop * scale).toInt().coerceAtLeast(0)
        val actualWidth = (cropRect.width * scale).toInt()
            .coerceAtMost(originalBitmap.width - actualLeft)
        val actualHeight = (cropRect.height * scale).toInt()
            .coerceAtMost(originalBitmap.height - actualTop)
        
        // Crop the bitmap
        val croppedBitmap = android.graphics.Bitmap.createBitmap(
            originalBitmap,
            actualLeft,
            actualTop,
            actualWidth,
            actualHeight
        )
        
        // Save cropped image
        val fileName = "profile_image_cropped_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = java.io.FileOutputStream(file)
        
        croppedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.close()
        
        // Clean up memory
        originalBitmap.recycle()
        croppedBitmap.recycle()
        
        onComplete(file.absolutePath)
        
    } catch (e: Exception) {
        e.printStackTrace()
        onComplete(imageUri) // Return original on error
    }
}