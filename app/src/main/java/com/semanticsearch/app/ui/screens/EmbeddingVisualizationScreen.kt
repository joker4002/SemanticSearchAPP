package com.semanticsearch.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.semanticsearch.app.R
import com.semanticsearch.app.visualization.EmbeddingPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmbeddingVisualizationScreen(
    points: List<EmbeddingPoint>,
    highlightedIds: Set<Long>,
    onRefresh: () -> Unit
) {
    var selected by remember { mutableStateOf<EmbeddingPoint?>(null) }

    val highlightColor = MaterialTheme.colorScheme.primary
    val baseColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = R.string.viz_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedButton(onClick = onRefresh) {
                Text(stringResource(id = R.string.refresh))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.viz_legend),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        val pointRadius = 6.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points, highlightedIds) {
                        detectTapGestures { tap ->
                            val size = this.size
                            val px = tap.x
                            val py = tap.y

                            var best: EmbeddingPoint? = null
                            var bestDist = Float.MAX_VALUE

                            for (p in points) {
                                val x = p.x * size.width
                                val y = (1f - p.y) * size.height
                                val dx = x - px
                                val dy = y - py
                                val d = dx * dx + dy * dy
                                if (d < bestDist) {
                                    bestDist = d
                                    best = p
                                }
                            }

                            val hit = 24.dp.toPx()
                            val hitThreshold = hit * hit
                            if (best != null && bestDist <= hitThreshold) {
                                selected = best
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                for (p in points) {
                    val x = p.x * w
                    val y = (1f - p.y) * h
                    val color = if (highlightedIds.contains(p.id)) highlightColor else baseColor

                    drawCircle(
                        color = color,
                        radius = pointRadius.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }
    }

    selected?.let { p ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(p.title) },
            text = {
                Text(
                    text = p.snippet,
                    maxLines = 12
                )
            },
            confirmButton = {
                TextButton(onClick = { selected = null }) {
                    Text(stringResource(id = R.string.close))
                }
            }
        )
    }
}
