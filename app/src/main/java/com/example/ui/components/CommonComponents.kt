package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.AppConstants
import com.example.core.Formatters

@Composable
fun TaliKhataLogoMark(size: Dp = 64.dp, showLabel: Boolean = true) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = R.drawable.img_talikhata_icon),
            contentDescription = AppConstants.APP_NAME,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.24f))
                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(size * 0.24f)),
        )
        if (showLabel) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = AppConstants.APP_NAME,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
fun JeobayerShopBadge(compact: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 10.dp else 14.dp,
                vertical = if (compact) 5.dp else 8.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Storefront,
                contentDescription = AppConstants.SHOP_LEDGER_NAME,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (compact) 16.dp else 20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = AppConstants.SHOP_LEDGER_NAME,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (compact) 12.sp else 14.sp,
                    letterSpacing = 1.1.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (!compact) {
                    Text(
                        text = "ডিজিটাল খাতা • ${AppConstants.SHOP_BANGLA_NAME}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
fun TopWhatsAppSupportButton(label: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF16A34A).copy(alpha = 0.14f),
        border = BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.45f)),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("whatsapp_support_button")
            .clip(RoundedCornerShape(20.dp))
            .clickable { Formatters.openOwnerSupportWhatsApp(context) },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Chat,
                contentDescription = label,
                tint = Color(0xFF15803D),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF15803D),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SyncStatusPill(isOnline: Boolean, isSyncing: Boolean, pendingCount: Int, onClick: () -> Unit) {
    val (icon, text, tint) = when {
        isSyncing -> Triple(Icons.Rounded.CloudSync, "সিঙ্ক হচ্ছে...", MaterialTheme.colorScheme.tertiary)
        !isOnline -> Triple(Icons.Rounded.CloudOff, "অফলাইন ($pendingCount পেন্ডিং)", Color(0xFFD97706))
        pendingCount > 0 -> Triple(Icons.Rounded.CloudSync, "$pendingCount পেন্ডিং সিঙ্ক", MaterialTheme.colorScheme.primary)
        else -> Triple(Icons.Rounded.CloudDone, "ক্লাউড সিঙ্কড", MaterialTheme.colorScheme.primary)
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = tint.copy(alpha = 0.12f),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = text, tint = tint, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = tint)
        }
    }
}

@Composable
fun MetricTileCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun EmptyStateCard(icon: ImageVector, title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
