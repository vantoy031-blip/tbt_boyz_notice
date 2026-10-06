package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Notice
import com.example.data.model.NoticeCategory
import com.example.ui.theme.CategoryAnnouncementBg
import com.example.ui.theme.CategoryAnnouncementBorder
import com.example.ui.theme.CategoryAnnouncementText
import com.example.ui.theme.CategoryEventBg
import com.example.ui.theme.CategoryEventBorder
import com.example.ui.theme.CategoryEventText
import com.example.ui.theme.CategoryGeneralBg
import com.example.ui.theme.CategoryGeneralBorder
import com.example.ui.theme.CategoryGeneralText
import com.example.ui.theme.CategoryImportantBg
import com.example.ui.theme.CategoryImportantBorder
import com.example.ui.theme.CategoryImportantText
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoticeCard(
    notice: Notice,
    isUnread: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderStroke = when {
        notice.isPinned -> BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.6f))
        notice.isImportant -> BorderStroke(1.2.dp, CrimsonPrimary.copy(alpha = 0.5f))
        else -> BorderStroke(1.dp, DarkSurfaceBorder)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("notice_card_${notice.id}")
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Badges row: Pinned, Important, Category, and NEW indicator
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (notice.isPinned) {
                    PinnedBadge()
                }

                if (notice.isImportant) {
                    ImportantBadge()
                }

                CategoryBadge(category = notice.categoryEnum)

                if (isUnread) {
                    NewIndicatorBadge()
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notice Title
            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    lineHeight = 22.sp
                ),
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Short Description preview
            Text(
                text = notice.shortDescription,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                color = TextSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Footer: Date & Time + "Tap to read ->"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Date and Time",
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${notice.formattedDate} • ${notice.formattedTime}",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "Tap to read",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = if (notice.isImportant) CrimsonPrimary else GoldAccent
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Read full notice",
                        tint = if (notice.isImportant) CrimsonPrimary else GoldAccent,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PinnedBadge() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = GoldContainer,
        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = "Pinned Notice",
                tint = GoldAccent,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "PINNED",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                ),
                color = GoldAccent
            )
        }
    }
}

@Composable
fun ImportantBadge() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = CategoryImportantBg,
        border = BorderStroke(1.dp, CategoryImportantBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PriorityHigh,
                contentDescription = "Important Notice",
                tint = CategoryImportantText,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "IMPORTANT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                ),
                color = CategoryImportantText
            )
        }
    }
}

@Composable
fun CategoryBadge(category: NoticeCategory) {
    val (bg, text, border, icon: ImageVector) = when (category) {
        NoticeCategory.IMPORTANT -> Quad(CategoryImportantBg, CategoryImportantText, CategoryImportantBorder, Icons.Default.PriorityHigh)
        NoticeCategory.EVENT -> Quad(CategoryEventBg, CategoryEventText, CategoryEventBorder, Icons.Default.Event)
        NoticeCategory.GENERAL -> Quad(CategoryGeneralBg, CategoryGeneralText, CategoryGeneralBorder, Icons.Default.Info)
        NoticeCategory.ANNOUNCEMENT -> Quad(CategoryAnnouncementBg, CategoryAnnouncementText, CategoryAnnouncementBorder, Icons.Default.Campaign)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = category.displayName,
                tint = text,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = category.displayName.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = text
            )
        }
    }
}

@Composable
fun NewIndicatorBadge() {
    Box(
        modifier = Modifier
            .background(
                color = CrimsonPrimary,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = "NEW",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.7.sp,
                fontSize = 10.sp
            ),
            color = Color.White
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
