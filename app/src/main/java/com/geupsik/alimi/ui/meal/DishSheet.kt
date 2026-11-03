package com.geupsik.alimi.ui.meal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.geupsik.alimi.domain.Allergy
import com.geupsik.alimi.domain.Dish
import com.geupsik.alimi.ui.components.openInBrowser
import java.net.URLEncoder

/** 메뉴를 누르면 뜨는 창. 이 메뉴에 든 알레르기 식품과 사진 검색 버튼을 보여준다. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DishSheet(dish: Dish, myAllergies: Set<Int>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            Text(dish.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))

            Text(
                "알레르기 유발 식품",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            if (dish.allergyNumbers.isEmpty()) {
                Text("표시된 식품이 없어요.", style = MaterialTheme.typography.bodyLarge)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    dish.allergyNumbers.sorted().forEach { number ->
                        AllergyTag(Allergy.names(setOf(number)), isMine = number in myAllergies)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { openInBrowser(context, imageSearchUrl(dish.name)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Image, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("사진 찾아보기")
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "인터넷 이미지 검색 결과라 실제 급식과 모양이 다를 수 있어요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AllergyTag(name: String, isMine: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isMine) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Text(
            if (isMine) "$name · 내 알레르기" else name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isMine) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isMine) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

/** 한국 음식은 네이버 이미지 검색이 잘 나온다. "토스트바/우유"처럼 묶인 메뉴는 그대로 검색해도 된다. */
private fun imageSearchUrl(name: String): String =
    "https://m.search.naver.com/search.naver?where=m_image&query=" + URLEncoder.encode(name, "UTF-8")
