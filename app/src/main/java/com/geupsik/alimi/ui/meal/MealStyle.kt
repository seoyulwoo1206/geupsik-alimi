package com.geupsik.alimi.ui.meal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 조식·중식·석식마다 다른 이모지와 파스텔 색을 써서 한눈에 구분되게 한다. */
object MealStyle {

    fun emoji(code: Int) = when (code) {
        1 -> "🌅"
        3 -> "🌙"
        else -> "☀️"
    }

    /** 끼니 아이콘 뒤 동그라미 색. 아침은 복숭아, 점심은 로고 노랑, 저녁은 연보라. */
    @Composable
    fun tint(code: Int): Color {
        val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        return when (code) {
            1 -> if (dark) Color(0xFF5A3A22) else Color(0xFFFFE0C7)
            3 -> if (dark) Color(0xFF3D3760) else Color(0xFFE6E1FF)
            else -> if (dark) Color(0xFF5C4300) else Color(0xFFFFE08A)
        }
    }
}

/** 끼니 이모지를 파스텔 동그라미 안에 넣은 배지 */
@Composable
fun MealIcon(code: Int, size: Dp = 40.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(MealStyle.tint(code)),
        contentAlignment = Alignment.Center,
    ) {
        Text(MealStyle.emoji(code), fontSize = (size.value * 0.5f).sp)
    }
}
