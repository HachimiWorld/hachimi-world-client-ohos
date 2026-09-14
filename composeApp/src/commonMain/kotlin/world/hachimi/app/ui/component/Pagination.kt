package world.hachimi.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hachimiworld.composeapp.generated.resources.Res
import hachimiworld.composeapp.generated.resources.ms_chevron_left
import hachimiworld.composeapp.generated.resources.ms_chevron_right
import org.jetbrains.compose.resources.vectorResource

@Composable
fun Pagination(
    modifier: Modifier = Modifier,
    total: Int,
    currentPage: Int,
    pageSize: Int = 10,
    pageSizes: List<Int> = remember { listOf(10, 20, 30, 50, 100) },
    pageSizeChange: (Int) -> Unit,
    pageChange: (Int) -> Unit
) {
    val pageCount = remember(total, pageSize) {
        if (total == 0) return@remember 0
        var count = total / pageSize
        if (total % pageSize == 0) {
            count--
        }
        return@remember count
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("共 $total 条")
        var expanded by remember { mutableStateOf(false) }

        Box {
            Button(onClick = { expanded = true }) {
                Text("$pageSize 条/页")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                for (x in pageSizes) {
                    DropdownMenuItem(onClick = {
                        pageSizeChange(x)
                        expanded = false
                    }, text = {
                        Text(x.toString())
                    })
                }
            }
        }
        Button(onClick = {
            if (currentPage > 0) { pageChange(currentPage - 1) }
        }) {
            Icon(vectorResource(Res.drawable.ms_chevron_left), "Previous")
        }
        for (i in 0..pageCount) {
            val checked = currentPage == i
            if (checked) {
                Button(onClick = {}) {
                    Text((i + 1).toString())
                }
            } else {
                TextButton(onClick = { pageChange(i) }) {
                    Text((i + 1).toString())
                }
            }
        }
        Button(onClick = {
            if (currentPage < pageCount) {
                pageChange(currentPage + 1)
            }
        }) {
            Icon(vectorResource(Res.drawable.ms_chevron_right), "Next")
        }
    }
}