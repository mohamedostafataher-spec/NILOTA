package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppNotification
import com.example.data.model.DirectConversation
import com.example.data.model.NiloraTaxonomy
import com.example.ui.theme.*
import java.text.DateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NiloraTopBar(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun NiloraFooter(modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, EgyptianGold.copy(alpha = 0.28f)),
        shape = RoundedCornerShape(NiloraRadius.md)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NiloraSpacing.lg, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NiloraBrandSymbol(
                size = 24.dp,
                variant = if (isDark) BrandLogoVariant.ON_DARK else BrandLogoVariant.ON_LIGHT
            )
            Text(
                text = "NILORA • GLOBAL SOUL — EGYPTIAN ORIGIN",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = NiloraTaxonomy.BRAND_VISION,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = NiloraTaxonomy.FOUNDER_ATTRIBUTION,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NiloraSectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(actionLabel)
            }
        }
    }
}

/**
 * Unified secondary tab switcher linking Messages, Discover, and Groups so the mobile
 * bottom navigation stays uncluttered at exactly 5 primary destinations.
 */
@Composable
fun CommunicationHubTabs(
    activeTab: String, // "MESSAGES" | "DISCOVER" | "GROUPS"
    pendingRequestsCount: Int = 0,
    onSelectMessages: () -> Unit,
    onSelectDiscover: () -> Unit,
    onSelectGroups: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("communication_hub_tabs"),
        shape = RoundedCornerShape(NiloraRadius.md),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            HubTabItem(
                label = "Chats",
                selected = activeTab == "MESSAGES",
                badgeCount = 0,
                testTag = "hub_tab_messages",
                onClick = onSelectMessages,
                modifier = Modifier.weight(1f)
            )
            HubTabItem(
                label = "Discover",
                selected = activeTab == "DISCOVER",
                badgeCount = pendingRequestsCount,
                testTag = "hub_tab_discover",
                onClick = onSelectDiscover,
                modifier = Modifier.weight(1f)
            )
            HubTabItem(
                label = "Groups",
                selected = activeTab == "GROUPS",
                badgeCount = 0,
                testTag = "hub_tab_groups",
                onClick = onSelectGroups,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HubTabItem(
    label: String,
    selected: Boolean,
    badgeCount: Int,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .testTag(testTag)
            .clickable(onClick = onClick),
        color = if (selected) MidnightNavy else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (badgeCount > 0) "$label ($badgeCount)" else label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) SoftIvory else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NiloraSearchFilterBar(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    testTag: String = "nilora_search_input",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(NiloraRadius.md),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

@Composable
fun NiloraConversationPreviewCard(
    conversation: DirectConversation,
    currentUserId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val partnerName = conversation.partnerName(currentUserId)
    val partnerLang = conversation.partnerLanguage(currentUserId)
    val timeText = conversation.updatedAt?.toDate()?.let {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(it)
    }.orEmpty()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("conversation_row_${conversation.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(NiloraRadius.md),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = NileTurquoiseContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = partnerName.take(2).uppercase().ifBlank { "NL" },
                        style = MaterialTheme.typography.titleMedium,
                        color = NileTurquoiseOnContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = partnerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (timeText.isNotBlank()) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = conversation.lastMessagePreview.ifBlank { "Open multilingual chat ($partnerLang)" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun NiloraVoiceMessagePlayer(
    durationMs: Long,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onTogglePlay,
        shape = RoundedCornerShape(NiloraRadius.sm),
        border = BorderStroke(1.dp, if (isPlaying) CoralAccessible else NileTurquoiseDark),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Stop voice message" else "Play voice message"
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (isPlaying) "Stop Audio"
            else "Voice Message (${"%.1f".format(durationMs / 1000.0)}s AAC)"
        )
    }
}

@Composable
fun NiloraTranslationPanelCard(
    originalLabel: String,
    originalText: String,
    translatedLabel: String,
    translatedText: String?,
    onSpeakTranslation: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NiloraRadius.md),
        border = BorderStroke(1.dp, EgyptianGold.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(originalLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(originalText, style = MaterialTheme.typography.bodyLarge)
            if (!translatedText.isNullOrBlank()) {
                NiloraGeometricDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(translatedLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    if (onSpeakTranslation != null) {
                        IconButton(onClick = onSpeakTranslation, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak translation", modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Text(translatedText, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun NiloraMapPreviewPanel(
    verifiedLocationsCount: Int,
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenMap),
        shape = RoundedCornerShape(NiloraRadius.lg),
        border = BorderStroke(1.dp, NileTurquoise.copy(alpha = 0.45f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(MidnightNavy, MidnightNavyLight))
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = NileTurquoise.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Map,
                            contentDescription = "Interactive Map",
                            tint = NileTurquoise,
                            modifier = Modifier.padding(10.dp).size(26.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Interactive Discovery Map",
                            style = MaterialTheme.typography.titleMedium,
                            color = SoftIvory,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "OpenStreetMap tiles • Device GPS • $verifiedLocationsCount verified pins",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmSand
                        )
                    }
                }
                Icon(Icons.Filled.ArrowForwardIos, contentDescription = "Open map", tint = EgyptianGold, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun NiloraNotificationCard(
    notification: AppNotification,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.readStatus) MaterialTheme.colorScheme.surface else NileTurquoiseContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(NiloraRadius.md)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(notification.title, style = MaterialTheme.typography.titleMedium)
            Text(notification.message, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${notification.category} • ${if (notification.readStatus) "Read" else "Unread (tap to mark read)"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NiloraAnimatedStatusPulse(
    isActive: Boolean,
    activeColor: Color = CoralAccessible,
    idleColor: Color = NileTurquoise,
    sizeDp: Int = 10
) {
    val reduceMotion = LocalReducedMotion.current
    val alpha = if (isActive && !reduceMotion) {
        val transition = rememberInfiniteTransition(label = "nilora_pulse")
        val animatedAlpha by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.32f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
        animatedAlpha
    } else {
        1f
    }

    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(if (isActive) activeColor else idleColor)
    )
}

@Composable
fun NiloraLoadingState(
    message: String = "Loading verified data...",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NiloraRadius.md),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun NiloraErrorState(
    title: String,
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    UnavailableDataCard(
        title = title,
        explanation = message,
        icon = Icons.Filled.ErrorOutline,
        actionLabel = if (onRetry != null) "Retry" else null,
        onActionClick = onRetry,
        modifier = modifier
    )
}

@Composable
fun NiloraConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) ButtonDefaults.buttonColors(containerColor = ErrorCoral) else ButtonDefaults.buttonColors()
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun UnavailableDataCard(
    title: String,
    explanation: String,
    icon: ImageVector = Icons.Filled.CloudOff,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(NiloraRadius.lg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onActionClick,
                        modifier = Modifier.testTag("unavailable_primary_action")
                    ) {
                        Text(actionLabel)
                    }
                    if (secondaryActionLabel != null && onSecondaryActionClick != null) {
                        OutlinedButton(onClick = onSecondaryActionClick) {
                            Text(secondaryActionLabel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VerifiedSourceBadge(
    sourceName: String,
    lastVerifiedAt: String,
    verificationStatus: String,
    modifier: Modifier = Modifier
) {
    val isApproved = verificationStatus == "APPROVED"
    val bg = if (isApproved) VerifiedEmeraldContainer else WarningAmberContainer
    val fg = if (isApproved) Color(0xFF065F46) else Color(0xFF78350F)
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = if (isApproved) Icons.Filled.Verified else Icons.Filled.Pending,
                contentDescription = verificationStatus,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = buildString {
                    append(if (isApproved) "Verified: $sourceName" else "Status: $verificationStatus")
                    if (lastVerifiedAt.isNotBlank()) append(" • $lastVerifiedAt")
                },
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BookingStatusChip(status: String, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (status) {
        "CONFIRMED" -> Triple(VerifiedEmeraldContainer, Color(0xFF065F46), "Confirmed")
        "REQUEST_SUBMITTED" -> Triple(TurquoiseContainer, TurquoiseOnContainer, "Request Submitted")
        "AWAITING_PROVIDER_CONFIRMATION" -> Triple(WarmSandContainer, WarmSandOnContainer, "Awaiting Provider Confirmation")
        "REJECTED" -> Triple(ErrorCoralContainer, ErrorCoral, "Rejected")
        "CANCELLED" -> Triple(SurfaceMuted, TextSecondaryLight, "Cancelled")
        else -> Triple(SurfaceMuted, TextSecondaryLight, status)
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun OfflineStatusBanner(isOnline: Boolean) {
    if (!isOnline) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ErrorCoralContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.WifiOff, contentDescription = "Offline", tint = ErrorCoral, modifier = Modifier.size(18.dp))
                Text(
                    text = "Offline: Live AI translation, cloud sync, and map tiles require an internet connection.",
                    style = MaterialTheme.typography.labelSmall,
                    color = ErrorCoral
                )
            }
        }
    }
}
