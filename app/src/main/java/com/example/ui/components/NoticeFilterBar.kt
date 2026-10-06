package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class FilterItem(
    val id: String, // "all", "important", "event", "general", "announcement"
    val label: String,
    val icon: ImageVector,
    val count: Int? = null
)

@Composable
fun NoticeFilterBar(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        FilterItem("all", "All", Icons.Default.Dashboard),
        FilterItem("important", "Important", Icons.Default.PriorityHigh),
        FilterItem("event", "Events", Icons.Default.Event),
        FilterItem("announcement", "Announcements", Icons.Default.Campaign),
        FilterItem("general", "General", Icons.Default.Info)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val isSelected = selectedCategory.equals(item.id, ignoreCase = true)

            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) {
                    when (item.id) {
                        "important" -> CrimsonPrimary
                        "event" -> GoldAccent
                        else -> CrimsonPrimary
                    }
                } else DarkSurface,
                label = "chip_bg"
            )

            val contentColor by animateColorAsState(
                targetValue = if (isSelected) {
                    if (item.id == "event") Color.Black else Color.White
                } else TextSecondary,
                label = "chip_text"
            )

            val border = if (isSelected) null else BorderStroke(1.dp, DarkSurfaceBorder)

            Surface(
                modifier = Modifier
                    .testTag("filter_chip_${item.id}")
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onCategorySelected(item.id) },
                shape = RoundedCornerShape(12.dp),
                color = backgroundColor,
                border = border,
                tonalElevation = if (isSelected) 3.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = contentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        ),
                        color = contentColor
                    )
                }
            }
        }
    }
}
