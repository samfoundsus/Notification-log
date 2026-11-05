package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.db.NotificationEntity
import com.example.util.AppIconProvider
import com.example.util.TimeFormatUtil

enum class GroupPosition {
    SINGLE,
    TOP,
    MIDDLE,
    BOTTOM
}

fun GroupPosition.toCardShape(): RoundedCornerShape = when (this) {
    GroupPosition.SINGLE -> RoundedCornerShape(24.dp)
    GroupPosition.TOP -> RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = 6.dp,
        bottomEnd = 6.dp
    )
    GroupPosition.MIDDLE -> RoundedCornerShape(6.dp)
    GroupPosition.BOTTOM -> RoundedCornerShape(
        topStart = 6.dp,
        topEnd = 6.dp,
        bottomStart = 24.dp,
        bottomEnd = 24.dp
    )
}

fun buildNotificationBodyText(
    text: String,
    sender: String?,
    isGroup: Boolean,
    isOutgoing: Boolean,
    boldColor: Color
): AnnotatedString {
    val cleanSender = sender?.trim().orEmpty()
    if (!isGroup || isOutgoing || cleanSender.isEmpty()) {
        return AnnotatedString(text)
    }

    val trimmedText = text.trimStart()
    val colonPrefix = "$cleanSender:"
    val spacedColonPrefix = "$cleanSender :"

    return when {
        trimmedText.startsWith(colonPrefix, ignoreCase = true) -> {
            val remainder = trimmedText.substring(colonPrefix.length).trimStart()
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) {
                    append(cleanSender)
                    append(": ")
                }
                append(remainder)
            }
        }
        trimmedText.startsWith(spacedColonPrefix, ignoreCase = true) -> {
            val remainder = trimmedText.substring(spacedColonPrefix.length).trimStart()
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) {
                    append(cleanSender)
                    append(": ")
                }
                append(remainder)
            }
        }
        else -> {
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) {
                    append(cleanSender)
                    append(": ")
                }
                append(text)
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationEntity,
    groupPosition: GroupPosition,
    expanded: Boolean,
    onOpenNotification: () -> Unit,
    onToggleExpand: () -> Unit,
    iconProvider: AppIconProvider,
    modifier: Modifier = Modifier
) {
    val shape = remember(groupPosition) { groupPosition.toCardShape() }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val justNowLabel = stringResource(R.string.time_just_now)
    val yesterdayLabel = stringResource(R.string.day_yesterday)
    val youLabel = stringResource(R.string.label_you)

    val relativeTime = remember(notification.timestamp, justNowLabel, yesterdayLabel) {
        TimeFormatUtil.formatRelativeTime(
            timestamp = notification.timestamp,
            justNowLabel = justNowLabel,
            yesterdayLabel = yesterdayLabel
        )
    }

    val headerMetaText = remember(
        notification.appName,
        notification.isOutgoing,
        youLabel,
        relativeTime
    ) {
        if (notification.isOutgoing) {
            "${notification.appName} · $youLabel · $relativeTime"
        } else {
            "${notification.appName} · $relativeTime"
        }
    }

    val exactDateTime = remember(notification.timestamp, expanded) {
        if (expanded) TimeFormatUtil.formatExactDateTime(notification.timestamp) else ""
    }

    val removalStatusLine = remember(
        notification.removedAt,
        notification.removalReason,
        expanded
    ) {
        val removedAt = notification.removedAt
        if (expanded && removedAt != null) {
            val reason = notification.removalReason?.takeIf { it.isNotBlank() } ?: "Other"
            val formattedTime = TimeFormatUtil.formatExactTime(removedAt)
            "$reason $formattedTime"
        } else {
            null
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val containerColor = when {
        notification.isOutgoing && (expanded || isPressed) -> {
            lerp(colorScheme.secondaryContainer, colorScheme.onSecondaryContainer, 0.08f)
        }
        notification.isOutgoing -> {
            colorScheme.secondaryContainer
        }
        expanded || isPressed -> {
            colorScheme.surfaceContainerHigh
        }
        else -> {
            colorScheme.surfaceContainer
        }
    }

    val titleColor = if (notification.isOutgoing) {
        colorScheme.onSecondaryContainer
    } else {
        colorScheme.onSurface
    }

    val bodyColor = if (notification.isOutgoing) {
        colorScheme.onSecondaryContainer.copy(alpha = 0.88f)
    } else {
        colorScheme.onSurfaceVariant
    }

    val metaColor = if (notification.isOutgoing) {
        colorScheme.onSecondaryContainer.copy(alpha = 0.78f)
    } else {
        colorScheme.onSurfaceVariant
    }

    val formattedBody = remember(
        notification.text,
        notification.sender,
        notification.isGroup,
        notification.isOutgoing,
        titleColor
    ) {
        buildNotificationBodyText(
            text = notification.text,
            sender = notification.sender,
            isGroup = notification.isGroup,
            isOutgoing = notification.isOutgoing,
            boldColor = titleColor
        )
    }

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "expand_chevron_rotation"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onOpenNotification
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .testTag("notification_card_${notification.id}"),
        shape = shape,
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            AppIconMonogram(
                packageName = notification.packageName,
                iconProvider = iconProvider,
                size = 40.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = headerMetaText,
                    style = MaterialTheme.typography.labelMedium,
                    color = metaColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = formattedBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = bodyColor,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (expanded) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(
                        color = colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = exactDateTime,
                        style = MaterialTheme.typography.labelMedium,
                        color = metaColor
                    )
                    if (removalStatusLine != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = removalStatusLine,
                            style = MaterialTheme.typography.labelMedium,
                            color = metaColor,
                            modifier = Modifier.testTag("removal_status_${notification.id}")
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = notification.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = metaColor
                    )
                }
            }

            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("expand_button_${notification.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(
                        if (expanded) {
                            R.string.cd_collapse_notification
                        } else {
                            R.string.cd_expand_notification
                        }
                    ),
                    tint = metaColor,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation)
                )
            }
        }
    }
}
