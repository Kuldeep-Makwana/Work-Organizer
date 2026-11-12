package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReelSaveStatus
import com.example.ui.theme.DotFontFamily
import com.example.ui.theme.LocalNothingColors
import com.example.ui.theme.NothingMonoFamily
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingSansFamily

@Composable
fun NothingCard(
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 10.dp,
    elevationColor: Color? = null,
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    val colors = LocalNothingColors.current
    val bg = elevationColor ?: colors.cardBackground

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(shapeRadius))
            .background(bg)
            .border(borderWidth, colors.subtleBorder, RoundedCornerShape(shapeRadius))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun NothingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = false,
    isAccentRed: Boolean = false,
    isLoading: Boolean = false,
    testTag: String? = null
) {
    val colors = LocalNothingColors.current

    val bgColor = when {
        !enabled -> colors.elevatedBackground.copy(alpha = 0.5f)
        isAccentRed -> colors.accentRed
        isPrimary -> colors.primaryText
        else -> colors.elevatedBackground
    }

    val contentColor = when {
        !enabled -> colors.secondaryText.copy(alpha = 0.5f)
        isAccentRed -> Color.White
        isPrimary -> colors.mainBackground
        else -> colors.primaryText
    }

    val borderStroke = when {
        !enabled -> BorderStroke(1.dp, colors.subtleBorder.copy(alpha = 0.4f))
        isAccentRed -> null
        isPrimary -> null
        else -> BorderStroke(1.dp, colors.subtleBorder)
    }

    val shape = RoundedCornerShape(8.dp)

    Surface(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = shape,
        color = bgColor,
        border = borderStroke,
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                style = TextStyle(
                    fontFamily = NothingMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    color = contentColor
                )
            )
        }
    }
}

@Composable
fun NothingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    isMono: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    testTag: String? = null
) {
    val colors = LocalNothingColors.current

    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (enabled) colors.elevatedBackground else colors.mainBackground.copy(alpha = 0.6f))
            .border(
                1.dp,
                if (enabled) colors.subtleBorder else colors.subtleBorder.copy(alpha = 0.4f),
                shape
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = TextStyle(
                    fontFamily = if (isMono) NothingMonoFamily else NothingSansFamily,
                    fontSize = 14.sp,
                    color = colors.secondaryText.copy(alpha = 0.6f),
                    letterSpacing = if (isMono) 0.5.sp else 0.sp
                )
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            textStyle = TextStyle(
                fontFamily = if (isMono) NothingMonoFamily else NothingSansFamily,
                fontSize = 14.sp,
                color = if (enabled) colors.primaryText else colors.secondaryText,
                letterSpacing = if (isMono) 0.5.sp else 0.sp
            ),
            cursorBrush = SolidColor(colors.accentRed),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun NothingStatusBadge(
    status: ReelSaveStatus,
    modifier: Modifier = Modifier
) {
    val colors = LocalNothingColors.current

    val (text, color) = when (status) {
        ReelSaveStatus.SAVED -> "SAVED" to colors.statusSaved
        ReelSaveStatus.SAVING -> "SAVING..." to colors.statusSaving
        ReelSaveStatus.UNSAVED -> "UNSAVED" to colors.statusUnsaved
        ReelSaveStatus.FAILED -> "FAILED" to colors.statusFailed
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = TextStyle(
                fontFamily = NothingMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = color,
                letterSpacing = 0.5.sp
            )
        )
    }
}

@Composable
fun ReelCodeChip(
    code: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalNothingColors.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.mainBackground)
            .border(1.dp, colors.subtleBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = code,
            style = TextStyle(
                fontFamily = NothingMonoFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = colors.primaryText,
                letterSpacing = 0.5.sp
            )
        )
    }
}

@Composable
fun SubmittedBadge(
    modifier: Modifier = Modifier
) {
    val colors = LocalNothingColors.current

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.accentRed.copy(alpha = 0.15f))
            .border(1.dp, colors.accentRed, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Locked",
            tint = colors.accentRed,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "SUBMITTED",
            style = TextStyle(
                fontFamily = NothingMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = colors.accentRed,
                letterSpacing = 1.sp
            )
        )
    }
}

@Composable
fun DraftBadge(
    modifier: Modifier = Modifier
) {
    val colors = LocalNothingColors.current

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.elevatedBackground)
            .border(1.dp, colors.subtleBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(colors.secondaryText)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "DRAFT",
            style = TextStyle(
                fontFamily = NothingMonoFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                color = colors.secondaryText,
                letterSpacing = 0.5.sp
            )
        )
    }
}
