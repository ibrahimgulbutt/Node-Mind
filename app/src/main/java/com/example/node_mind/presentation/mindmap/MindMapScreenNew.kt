package com.example.node_mind.presentation.mindmap

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.node_mind.data.model.Node
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapScreen(
    viewModel: MindMapViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Handle errors
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            viewModel.clearError()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            // Main mind map canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        canvasSize = coordinates.size
                    }
                    .pointerInput(Unit) {
                        // Handle pan and zoom
                        detectTransformGestures { _, pan, zoom, _ ->
                            viewModel.updateOffset(pan.x, pan.y)
                            viewModel.updateScale(uiState.scale * zoom)
                        }
                    }
            ) {
                // Draw connections and nodes
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
                    // Draw connections first (behind nodes)
                    uiState.connections.forEach { connection ->
                        drawConnection(
                            connection = connection,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }
                
                // Draw nodes on top of connections
                uiState.nodes.forEach { nodePosition ->
                    MindMapNode(
                        nodePosition = nodePosition,
                        scale = uiState.scale,
                        offsetX = uiState.offsetX,
                        offsetY = uiState.offsetY,
                        onNodeClick = { viewModel.selectNode(nodePosition) },
                        onNodeDrag = { deltaX, deltaY ->
                            viewModel.moveNode(
                                nodePosition.node.id,
                                nodePosition.x + deltaX / uiState.scale,
                                nodePosition.y + deltaY / uiState.scale
                            )
                        }
                    )
                }
                
                // Empty state
                if (uiState.nodes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyMindMapState(
                            onAddClick = viewModel::showAddDialog
                        )
                    }
                }
            }
        }
        
        // Top controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mind Map",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Center button
                FloatingActionButton(
                    onClick = viewModel::centerMap,
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Center Map",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Add node button
                FloatingActionButton(
                    onClick = viewModel::showAddDialog,
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Node",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        
        // Bottom panel for selected node
        uiState.selectedNode?.let { selectedNode ->
            SelectedNodePanel(
                nodePosition = selectedNode,
                onEdit = { viewModel.showEditDialog(selectedNode.node) },
                onDelete = { viewModel.deleteNode(selectedNode.node) },
                onConnect = { /* TODO: Implement connection mode */ },
                onDeselect = { viewModel.selectNode(selectedNode) },
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
                onClick = { viewModel.updateScale(uiState.scale * 1.2f) },
                modifier = Modifier.size(40.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                elevation = FloatingActionButtonDefaults.elevation(2.dp)
            ) {
                Text(
                    text = "+",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            FloatingActionButton(
                onClick = { viewModel.updateScale(uiState.scale * 0.8f) },
                modifier = Modifier.size(40.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                elevation = FloatingActionButtonDefaults.elevation(2.dp)
            ) {
                Text(
                    text = "−",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
private fun MindMapNode(
    nodePosition: NodePosition,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    onNodeClick: () -> Unit,
    onNodeDrag: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val node = nodePosition.node
    val animatedScale by animateFloatAsState(
        targetValue = if (nodePosition.isSelected) 1.1f else 1f,
        animationSpec = tween(200), label = ""
    )
    
    Box(
        modifier = modifier
            .offset(
                x = with(LocalDensity.current) { (nodePosition.x * scale + offsetX).toDp() },
                y = with(LocalDensity.current) { (nodePosition.y * scale + offsetY).toDp() }
            )
            .size(120.dp)
            .scale(animatedScale)
            .pointerInput(Unit) {
                detectTapGestures {
                    onNodeClick()
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    onNodeDrag(dragAmount.x, dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (nodePosition.isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (nodePosition.isSelected) 8.dp else 4.dp
            ),
            border = if (nodePosition.isSelected) {
                androidx.compose.foundation.BorderStroke(
                    2.dp,
                    MaterialTheme.colorScheme.primary
                )
            } else null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Node title
                Text(
                    text = node.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (nodePosition.isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Tags indicator
                if (node.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.height(16.dp)
                    ) {
                        items(node.tags.take(3)) { tag ->
                            Surface(
                                modifier = Modifier.size(6.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {}
                        }
                    }
                }
                
                // Connection count
                if (node.connectedNodeIds.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${node.connectedNodeIds.size} connections",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (nodePosition.isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedNodePanel(
    nodePosition: NodePosition,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onConnect: () -> Unit,
    onDeselect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = nodePosition.node.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                IconButton(onClick = onDeselect) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Deselect",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Content preview
            if (nodePosition.node.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = nodePosition.node.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Tags
            if (nodePosition.node.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(nodePosition.node.tags) { tag ->
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            
            // Action buttons
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
                
                OutlinedButton(
                    onClick = onConnect,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Connect")
                }
                
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun EmptyMindMapState(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🧬",
            fontSize = 64.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Create Your Mind Map",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Add your first node to start visualizing connections",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add First Node")
        }
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

private fun DrawScope.drawConnection(
    connection: Connection,
    color: Color
) {
    val startX = connection.fromX + 60.dp.toPx() // Node center offset
    val startY = connection.fromY + 60.dp.toPx()
    val endX = connection.toX + 60.dp.toPx()
    val endY = connection.toY + 60.dp.toPx()
    
    // Draw curved line
    val controlPointX = (startX + endX) / 2
    val controlPointY = min(startY, endY) - 50.dp.toPx()
    
    val path = Path().apply {
        moveTo(startX, startY)
        quadraticBezierTo(controlPointX, controlPointY, endX, endY)
    }
    
    drawPath(
        path = path,
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    )
    
    // Draw arrow at the end
    val arrowAngle = atan2(endY - controlPointY, endX - controlPointX)
    val arrowLength = 15.dp.toPx()
    val arrowAngle1 = arrowAngle + PI / 6
    val arrowAngle2 = arrowAngle - PI / 6
    
    drawLine(
        color = color,
        start = Offset(endX, endY),
        end = Offset(
            endX - arrowLength * cos(arrowAngle1).toFloat(),
            endY - arrowLength * sin(arrowAngle1).toFloat()
        ),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
    )
    
    drawLine(
        color = color,
        start = Offset(endX, endY),
        end = Offset(
            endX - arrowLength * cos(arrowAngle2).toFloat(),
            endY - arrowLength * sin(arrowAngle2).toFloat()
        ),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
    )
}
