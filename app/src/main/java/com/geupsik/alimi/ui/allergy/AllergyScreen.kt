package com.geupsik.alimi.ui.allergy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geupsik.alimi.domain.Allergy
import com.geupsik.alimi.ui.components.AllergyDisclaimer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllergyScreen(
    onDone: () -> Unit,
    vm: AllergyViewModel = viewModel(factory = AllergyViewModel.Factory),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.done) {
        if (state.done) onDone()
    }

    if (state.showNotice) {
        AllergyNoticeDialog(onConfirm = vm::onNoticeConfirm, onDismiss = vm::onNoticeDismiss)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("알레르기 등록") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
        bottomBar = {
            Column {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Button(
                        onClick = vm::onSaveClick,
                        enabled = state.loaded && state.changed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(if (state.selected.isEmpty()) "알레르기 없음으로 저장" else "저장 (${state.selected.size}개)")
                    }
                }
                AllergyDisclaimer()
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp)) {
                    Text(
                        "해당하는 알레르기를 모두 골라 주세요",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "고른 알레르기가 들어간 메뉴는 급식 화면과 알림에서 빨간색으로 표시돼요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (state.loaded) {
                items(Allergy.NAMES.entries.toList(), key = { it.key }) { (number, name) ->
                    AllergyCheckItem(
                        number = number,
                        name = name,
                        checked = number in state.selected,
                        onToggle = { vm.toggle(number) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AllergyCheckItem(number: Int, name: String, checked: Boolean, onToggle: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (checked) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() }),
    ) {
        Row(
            modifier = Modifier.padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 행 전체가 눌리니 체크박스 자체는 클릭을 받지 않게 한다
            Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(12.dp))
            Text(
                "$number",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp),
            )
            Text(name, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun AllergyNoticeDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("알레르기 표시 안내") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NoticeLine("나이스 급식 정보의 법정 알레르기 유발 식품 19종만 기준으로 표시해요. 그 외 식품은 표시되지 않아요.")
                NoticeLine("학교에서 입력한 정보에 오류가 있을 수 있어요.")
                NoticeLine("참고용으로만 쓰고, 정확한 정보는 꼭 학교 영양실에 확인하세요.")
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("확인했어요") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun NoticeLine(text: String) {
    Row {
        Text("•  ", style = MaterialTheme.typography.bodyMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
