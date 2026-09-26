package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GenderDas
import com.example.ui.theme.GenderDasBg
import com.example.ui.theme.GenderDer
import com.example.ui.theme.GenderDerBg
import com.example.ui.theme.GenderDie
import com.example.ui.theme.GenderDieBg
import com.example.ui.theme.GenderPlural
import com.example.ui.theme.GenderPluralBg

@Composable
fun GenderBadge(
    gender: String,
    modifier: Modifier = Modifier
) {
    val clean = gender.trim().lowercase()
    if (clean.isBlank()) return

    val (badgeText, textColor, bgColor) = when {
        clean == "der" || clean.startsWith("m") -> Triple("der (m)", GenderDer, GenderDerBg)
        clean == "die" || clean.startsWith("f") -> Triple("die (f)", GenderDie, GenderDieBg)
        clean == "das" || clean.startsWith("n") -> Triple("das (n)", GenderDas, GenderDasBg)
        clean.contains("pl") -> Triple("die (Pl.)", GenderPlural, GenderPluralBg)
        else -> Triple(gender.uppercase(), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = badgeText,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

fun getGenderColor(gender: String): Color {
    val clean = gender.trim().lowercase()
    return when {
        clean == "der" || clean.startsWith("m") -> GenderDer
        clean == "die" || clean.startsWith("f") -> GenderDie
        clean == "das" || clean.startsWith("n") -> GenderDas
        clean.contains("pl") -> GenderPlural
        else -> Color.Unspecified
    }
}
