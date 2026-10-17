package com.gymtracker.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * High-fidelity Google Logo icon painted with official Google Brand colors.
 */
@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = (w.coerceAtMost(h) / 2f) * 0.88f
        val strokeWidth = r * 0.35f

        // Google Blue: #4285F4
        // Google Red: #EA4335
        // Google Yellow: #FBBC05
        // Google Green: #34A853

        val blue = Color(0xFF4285F4)
        val red = Color(0xFFEA4335)
        val yellow = Color(0xFFFBBC05)
        val green = Color(0xFF34A853)

        // Draw outer colored arcs
        val arcRect = Rect(cx - r + strokeWidth / 2f, cy - r + strokeWidth / 2f, cx + r - strokeWidth / 2f, cy + r - strokeWidth / 2f)

        // Blue right segment
        drawArc(
            color = blue,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = strokeWidth),
            topLeft = arcRect.topLeft,
            size = arcRect.size
        )

        // Green bottom segment
        drawArc(
            color = green,
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = strokeWidth),
            topLeft = arcRect.topLeft,
            size = arcRect.size
        )

        // Yellow bottom-left segment
        drawArc(
            color = yellow,
            startAngle = 135f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = strokeWidth),
            topLeft = arcRect.topLeft,
            size = arcRect.size
        )

        // Red top segment
        drawArc(
            color = red,
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = strokeWidth),
            topLeft = arcRect.topLeft,
            size = arcRect.size
        )

        // Center blue crossbar
        drawLine(
            color = blue,
            start = Offset(cx, cy),
            end = Offset(cx + r, cy),
            strokeWidth = strokeWidth
        )
    }
}

/**
 * Official Google Sign-In Styled Button.
 */
@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Sign in with Google",
    isLoading: Boolean = false,
) {
    Surface(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp,
        modifier = modifier
            .height(48.dp)
            .testTag("google_sign_in_button")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Signing in...",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            } else {
                GoogleLogoIcon(Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Active Google Account Card displaying connected user identity.
 */
@Composable
fun GoogleAccountProfileCard(
    email: String,
    displayName: String,
    photoUrl: String?,
    healthLinked: Boolean,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box {
                    if (!photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Google Avatar",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = displayName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Google badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFDDDDDD), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        GoogleLogoIcon(Modifier.size(12.dp))
                    }
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified Google Account",
                            tint = Color(0xFF34A853),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (healthLinked) Color(0xFF34A853) else Color(0xFFFFA000))
                        )
                        Text(
                            text = if (healthLinked) "Google Health Connected" else "Health Link Ready",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = if (healthLinked) Color(0xFF2E7D32) else Color(0xFFE65100)
                        )
                    }
                }
            }

            IconButton(
                onClick = onSignOut,
                modifier = Modifier.testTag("google_sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Sign Out",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

