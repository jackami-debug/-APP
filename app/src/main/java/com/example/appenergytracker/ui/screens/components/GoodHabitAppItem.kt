package com.example.appenergytracker.ui.screens.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import com.example.appenergytracker.model.GoodHabitApp

@Composable
fun GoodHabitAppItem(
    app: GoodHabitApp,
    icon: ImageBitmap?,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit,
    onRatioChange: (Float) -> Unit,
    backgroundColor: Color = Color(0xFFF0F8FF), // 預設淺藍色背景
    showRatioField: Boolean = true
) {
    var ratioText by remember { mutableStateOf(app.ratio.toString()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = app.name,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(end = 8.dp)
                )
            } else {
                // 顯示首字母作為替代
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(end = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = app.name.take(1))
                }
            }

            // App 名稱與比例欄位
            Column(modifier = Modifier.weight(1f)) {
                Text(text = app.name, style = MaterialTheme.typography.bodyLarge)

                if (showRatioField) {
                    OutlinedTextField(
                        value = ratioText,
                        onValueChange = {
                            ratioText = it
                            it.toFloatOrNull()?.let { newRatio ->
                                onRatioChange(newRatio)
                            }
                        },
                        label = { Text("轉換比例") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 加入按鈕
            IconButton(
                onClick = { onToggle(!isSelected) },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = if (isSelected) {
                        androidx.compose.material.icons.Icons.Default.Check
                    } else {
                        androidx.compose.material.icons.Icons.Default.Add
                    },
                    contentDescription = if (isSelected) "已加入" else "加入",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
