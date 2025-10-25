package it.atraj.habittracker.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Simple muscle/bicep icon for All Might theme
 * 
 * Performance: Optimized with cached drawing operations
 */
@Composable
fun MuscleIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    // Cache the drawing operations to avoid recalculations
    val drawOperation = remember(tint) {
        { drawScope: DrawScope ->
            with(drawScope) {
                val color = if (tint != Color.Unspecified) tint else Color.Black
                val canvasWidth = size.width
                val canvasHeight = size.height
                
                // Draw a simplified bicep shape (circle for muscle)
                drawCircle(
                    color = color,
                    radius = canvasWidth / 3,
                    center = Offset(canvasWidth / 2, canvasHeight / 2),
                    style = Stroke(width = 3f)
                )
                
                // Draw forearm line
                drawLine(
                    color = color,
                    start = Offset(canvasWidth * 0.7f, canvasHeight / 2),
                    end = Offset(canvasWidth * 0.9f, canvasHeight * 0.7f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
                
                // Draw upper arm line
                drawLine(
                    color = color,
                    start = Offset(canvasWidth * 0.3f, canvasHeight / 2),
                    end = Offset(canvasWidth * 0.1f, canvasHeight * 0.3f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
    
    Canvas(modifier = modifier.size(24.dp)) {
        drawOperation(this)
    }
}
