package com.example.node_mind.presentation.mindmap

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapScreen(
    viewModel: MindMapViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var isConnectionMode by remember { mutableStateOf(false) }
    var connectionStartNode by remember { mutableStateOf<NodePosition?>(null) }
    
    // Handle errors with auto-dismiss
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        when {
            uiState.isLoading -> {
                LoadingState()
            }
            uiState.error != null -> {
                ErrorState(
                    error = uiState.error!!,
                    onRetry = viewModel::refreshData
                )
            }
            else -> {
                EnhancedMindMapContent(
                    uiState = uiState,
                    canvasSize = canvasSize,
                    isConnectionMode = isConnectionMode,
                    connectionStartNode = connectionStartNode,
                    onCanvasSizeChanged = { canvasSize = it },
                    onNodeClick = { nodePosition ->
                        when {
                            isConnectionMode -> {
                                if (connectionStartNode == null) {
                                    connectionStartNode = nodePosition
                                } else {
                                    // Create connection
                                    viewModel.connectNodes(
                                        connectionStartNode!!.node.id,
                                        nodePosition.node.id
                                    )
                                    connectionStartNode = null
                                    isConnectionMode = false
                                }
                            }
                            else -> viewModel.selectNode(nodePosition)
                        }
                    },
                    onNodeDrag = viewModel::moveNode,
                    onScaleChange = viewModel::updateScale,
                    onOffsetChange = viewModel::updateOffset
                )
            }
        }

        // Enhanced toolbar with connection mode
        EnhancedMindMapToolbar(
            isConnectionMode = isConnectionMode,
            connectionStartNode = connectionStartNode,
            onAddNode = { viewModel.showAddDialog() },
            onToggleConnectionMode = {
                isConnectionMode = !isConnectionMode
                if (!isConnectionMode) {
                    connectionStartNode = null
                }
            },
            onDeleteSelectedNodes = viewModel::deleteSelectedNodes,
            onResetView = viewModel::resetView,
            onSave = viewModel::saveMindMap,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // Connection mode indicator
        if (isConnectionMode) {
            ConnectionModeIndicator(
                connectionStartNode = connectionStartNode,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp)
            )
        }

        // Error display
        uiState.error?.let { error ->
            EnhancedErrorSnackbar(
                error = error,
                onDismiss = viewModel::clearError,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        
        // Zoom controls
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { viewModel.updateScale((uiState.scale * 1.2f).coerceAtMost(3f)) },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Zoom In",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            FloatingActionButton(
                onClick = { viewModel.updateScale((uiState.scale * 0.8f).coerceAtLeast(0.3f)) },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Zoom Out",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
    
    // Add/Edit Node Dialog
    if (uiState.showAddDialog || uiState.showEditDialog) {
        NodeDialog(
            isEditing = uiState.showEditDialog,
            title = uiState.nodeTitle,
            content = uiState.nodeContent,
            tags = uiState.nodeTags,
            onTitleChange = viewModel::updateNodeTitle,
            onContentChange = viewModel::updateNodeContent,
            onTagsChange = viewModel::updateNodeTags,
            onSave = viewModel::saveNode,
            onDismiss = viewModel::hideDialogs
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
            Text(
                text = "Loading Mind Map...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Something went wrong",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun EnhancedMindMapContent(
    uiState: MindMapUiState,
    canvasSize: IntSize,
    isConnectionMode: Boolean,
    connectionStartNode: NodePosition?,
    onCanvasSizeChanged: (IntSize) -> Unit,
    onNodeClick: (NodePosition) -> Unit,
    onNodeDrag: (String, Float, Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Float, Float) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    onScaleChange((uiState.scale * zoom).coerceIn(0.3f, 3f))
                    onOffsetChange(
                        uiState.offsetX + pan.x,
                        uiState.offsetY + pan.y
                    )
                }
            }
            .onGloballyPositioned { coordinates ->
                onCanvasSizeChanged(coordinates.size)
            }
    ) {
        // Enhanced connection canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = uiState.scale
                    scaleY = uiState.scale
                    translationX = uiState.offsetX
                    translationY = uiState.offsetY
                }
        ) {
            // Draw enhanced connections
            uiState.connections.forEach { connection ->
                drawEnhancedConnection(
                    connection = connection,
                    isHighlighted = isConnectionMode && 
                        (connectionStartNode?.node?.id == connection.fromNodeId ||
                         connectionStartNode?.node?.id == connection.toNodeId)
                )
            }
            
            // Draw temporary connection line in connection mode
            if (isConnectionMode && connectionStartNode != null) {
                drawTempConnectionLine(connectionStartNode, size)
            }
        }
        
        // Enhanced nodes
        uiState.nodePositions.forEach { nodePosition ->
            EnhancedMindMapNode(
                nodePosition = nodePosition,
                scale = uiState.scale,
                offsetX = uiState.offsetX,
                offsetY = uiState.offsetY,
                isConnectionMode = isConnectionMode,
                isConnectionStart = connectionStartNode?.node?.id == nodePosition.node.id,
                onNodeClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNodeClick(nodePosition) 
                },
                onNodeDrag = { deltaX, deltaY ->
                    onNodeDrag(
                        nodePosition.node.id,
                        nodePosition.x + deltaX / uiState.scale,
                        nodePosition.y + deltaY / uiState.scale
                    )
                }
            )
        }
        
        // Empty state
        if (uiState.nodePositions.isEmpty()) {
            EnhancedEmptyState(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun EnhancedMindMapNode(
    nodePosition: NodePosition,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    isConnectionMode: Boolean,
    isConnectionStart: Boolean,
    onNodeClick: () -> Unit,
    onNodeDrag: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val node = nodePosition.node
    val animatedScale by animateFloatAsState(
        targetValue = when {
            isConnectionStart -> 1.3f
            nodePosition.isSelected -> 1.15f
            isConnectionMode -> 0.9f
            else -> 1f
        },
        animationSpec = tween(300), 
        label = "nodeScale"
    )
    
    val animatedElevation by animateFloatAsState(
        targetValue = when {
            isConnectionStart -> 16f
            nodePosition.isSelected -> 12f
            else -> 6f
        },
        animationSpec = tween(300),
        label = "nodeElevation"
    )
    
    // Dynamic color based on node type and state
    val nodeColor = remember(node, nodePosition.isSelected, isConnectionStart) {
        when {
            isConnectionStart -> Color(0xFF4CAF50) // Green for connection start
            nodePosition.isSelected -> Color(0xFF2196F3) // Blue for selected
            node.tags.contains("important") -> Color(0xFFF44336) // Red for important
            node.tags.contains("idea") -> Color(0xFFFF9800) // Orange for ideas
            node.tags.contains("task") -> Color(0xFF9C27B0) // Purple for tasks
            else -> Color(0xFF607D8B) // Default blue-grey
        }
    }
    
    Box(
        modifier = modifier
            .offset(
                x = with(LocalDensity.current) { (nodePosition.x * scale + offsetX).toDp() },
                y = with(LocalDensity.current) { (nodePosition.y * scale + offsetY).toDp() }
            )
            .size(140.dp)
            .scale(animatedScale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onNodeClick() }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    onNodeDrag(dragAmount.x, dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Enhanced card with gradient and shadow
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = nodeColor.copy(alpha = 0.1f)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = animatedElevation.dp
            ),
            border = BorderStroke(
                width = if (nodePosition.isSelected || isConnectionStart) 3.dp else 1.dp,
                color = nodeColor.copy(alpha = if (nodePosition.isSelected || isConnectionStart) 1f else 0.3f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                nodeColor.copy(alpha = 0.2f),
                                nodeColor.copy(alpha = 0.05f)
                            ),
                            radius = 100f
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Node icon based on tags
                    val nodeIcon = remember(node.tags) {
                        when {
                            node.tags.contains("task") -> Icons.Default.CheckCircle
                            node.tags.contains("idea") -> Icons.Default.Star
                            node.tags.contains("important") -> Icons.Default.Star
                            node.tags.contains("note") -> Icons.Default.Edit
                            else -> Icons.Default.Star
                        }
                    }
                    
                    Icon(
                        imageVector = nodeIcon,
                        contentDescription = null,
                        tint = nodeColor,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    // Enhanced title with better typography
                    Text(
                        text = node.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Connection indicator
                    if (node.connectedNodeIds.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = nodeColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${node.connectedNodeIds.size}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = nodeColor
                            )
                        }
                    }
                    
                    // Enhanced tags display
                    if (node.tags.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.height(20.dp)
                        ) {
                            items(node.tags.take(3)) { tag ->
                                Surface(
                                    modifier = Modifier
                                        .height(16.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    color = nodeColor.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp
                                        ),
                                        color = nodeColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Selection indicator
                if (nodePosition.isSelected || isConnectionStart) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(16.dp)
                            .background(
                                color = nodeColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isConnectionStart) Icons.Default.Share else Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedMindMapToolbar(
    isConnectionMode: Boolean,
    connectionStartNode: NodePosition?,
    onAddNode: () -> Unit,
    onToggleConnectionMode: () -> Unit,
    onDeleteSelectedNodes: () -> Unit,
    onResetView: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Add node
            EnhancedToolbarButton(
                icon = Icons.Default.Add,
                label = "Add",
                onClick = onAddNode,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Connection mode
            EnhancedToolbarButton(
                icon = Icons.Default.Share,
                label = if (isConnectionMode) "Cancel" else "Connect",
                onClick = onToggleConnectionMode,
                color = if (isConnectionMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                isActive = isConnectionMode
            )
            
            // Delete selected
            EnhancedToolbarButton(
                icon = Icons.Default.Delete,
                label = "Delete",
                onClick = onDeleteSelectedNodes,
                color = MaterialTheme.colorScheme.error
            )
            
            // Reset view
            EnhancedToolbarButton(
                icon = Icons.Default.Refresh,
                label = "Reset",
                onClick = onResetView,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Save
            EnhancedToolbarButton(
                icon = Icons.Default.Check,
                label = "Save",
                onClick = onSave,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun EnhancedToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = if (isActive) color.copy(alpha = 0.2f) else Color.Transparent
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            ),
            color = color
        )
    }
}

@Composable
private fun ConnectionModeIndicator(
    connectionStartNode: NodePosition?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Text(
                text = if (connectionStartNode == null) {
                    "Select first node to connect"
                } else {
                    "Select second node to complete connection"
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun EnhancedErrorSnackbar(
    error: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun EnhancedEmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Animated brain icon
        val animatedScale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(1000),
            label = "brainScale"
        )
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(animatedScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🧠",
                fontSize = 64.sp
            )
        }
        
        Text(
            text = "Your Mind Map Awaits",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Create nodes to visualize your thoughts and connect ideas",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeDialog(
    isEditing: Boolean,
    title: String,
    content: String,
    tags: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Node" else "Add New Node",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    label = { Text("Content (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = tags,
                    onValueChange = onTagsChange,
                    label = { Text("Tags (comma separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ideas, work, personal") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isEditing) "Update" else "Create")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

private fun DrawScope.drawEnhancedConnection(
    connection: Connection,
    isHighlighted: Boolean = false
) {
    val startX = connection.fromX + 70.dp.toPx() // Node center
    val startY = connection.fromY + 70.dp.toPx()
    val endX = connection.toX + 70.dp.toPx()
    val endY = connection.toY + 70.dp.toPx()
    
    // Calculate control points for smooth curve
    val midX = (startX + endX) / 2
    val midY = (startY + endY) / 2
    val distance = sqrt((endX - startX).pow(2) + (endY - startY).pow(2))
    val curvature = (distance / 4).coerceAtMost(100.dp.toPx())
    
    val controlX = midX
    val controlY = midY - curvature
    
    val path = Path().apply {
        moveTo(startX, startY)
        quadraticBezierTo(controlX, controlY, endX, endY)
    }
    
    // Enhanced connection styling
    val connectionColor = if (isHighlighted) {
        Color(0xFF4CAF50)
    } else {
        Color(0xFF2196F3).copy(alpha = 0.7f)
    }
    
    val strokeWidth = if (isHighlighted) 5.dp.toPx() else 3.dp.toPx()
    
    // Draw shadow
    drawPath(
        path = path,
        color = Color.Black.copy(alpha = 0.1f),
        style = Stroke(
            width = strokeWidth + 2.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
    
    // Draw main connection
    drawPath(
        path = path,
        color = connectionColor,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
    
    // Draw enhanced arrow
    val arrowAngle = atan2(endY - controlY, endX - controlX)
    val arrowLength = 20.dp.toPx()
    val arrowWidth = 10.dp.toPx()
    
    val arrowPath = Path().apply {
        moveTo(endX, endY)
        lineTo(
            endX - arrowLength * cos(arrowAngle - PI/6).toFloat(),
            endY - arrowLength * sin(arrowAngle - PI/6).toFloat()
        )
        lineTo(
            endX - arrowWidth * cos(arrowAngle).toFloat(),
            endY - arrowWidth * sin(arrowAngle).toFloat()
        )
        lineTo(
            endX - arrowLength * cos(arrowAngle + PI/6).toFloat(),
            endY - arrowLength * sin(arrowAngle + PI/6).toFloat()
        )
        close()
    }
    
    drawPath(
        path = arrowPath,
        color = connectionColor
    )
}

private fun DrawScope.drawTempConnectionLine(
    startNode: NodePosition,
    canvasSize: Size
) {
    val startX = startNode.x + 70.dp.toPx()
    val startY = startNode.y + 70.dp.toPx()
    val endX = canvasSize.width / 2
    val endY = canvasSize.height / 2
    
    drawLine(
        color = Color(0xFF4CAF50).copy(alpha = 0.5f),
        start = Offset(startX, startY),
        end = Offset(endX, endY),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
    )
}
