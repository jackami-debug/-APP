package com.example.appenergytracker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.appenergytracker.utils.PasswordManager

@Composable
fun PasswordScreen(
    navController: NavController,
    onPasswordSet: (String) -> Unit
) {
    val context = LocalContext.current
    val passwordManager = remember { PasswordManager(context) }
    
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isSettingPassword by remember { mutableStateOf(false) }
    var isPasswordSet by remember { mutableStateOf(false) }
    
    // 檢查是否已經設定過密碼
    LaunchedEffect(Unit) {
        isPasswordSet = passwordManager.isPasswordSet()
        isSettingPassword = !isPasswordSet
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4A90E2), // 藍色
                        Color(0xFF7ED321), // 綠色
                        Color(0xFFFF9500)  // 橙色
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 上半部分：鎖頭圖示和文字
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 60.dp)
            ) {
                // 鎖頭圖示
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(12.dp),
                    tint = Color.White
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 主要標題
                Text(
                    text = if (isSettingPassword) "設定密碼" else "輸入密碼",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 副標題
                Text(
                    text = if (isSettingPassword) 
                        "請設定一個6位數字密碼來保護您的應用程式" 
                    else 
                        "您的密碼用於解鎖此應用程式",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 密碼點顯示
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(6) { index ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index < password.length) Color.White 
                                    else Color.White.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
                
                // 如果是設定密碼模式，顯示確認密碼點
                if (isSettingPassword) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "確認密碼",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(6) { index ->
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index < confirmPassword.length) Color.White 
                                        else Color.White.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }
                }
            }
            
            // 下半部分：數字鍵盤
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 60.dp)
            ) {
                // 數字鍵盤
                val numbers = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "⌫")
                )
                
                numbers.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        row.forEach { number ->
                            if (number.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .clickable {
                                            when (number) {
                                                "⌫" -> {
                                                    if (isSettingPassword) {
                                                        if (confirmPassword.isNotEmpty()) {
                                                            confirmPassword = confirmPassword.dropLast(1)
                                                        } else if (password.isNotEmpty()) {
                                                            password = password.dropLast(1)
                                                        }
                                                    } else {
                                                        if (password.isNotEmpty()) {
                                                            password = password.dropLast(1)
                                                        }
                                                    }
                                                }
                                                else -> {
                                                    if (isSettingPassword) {
                                                        if (password.length < 6) {
                                                            password += number
                                                        } else if (confirmPassword.length < 6) {
                                                            confirmPassword += number
                                                        }
                                                    } else {
                                                        if (password.length < 6) {
                                                            password += number
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = number,
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                // 空位
                                Spacer(modifier = Modifier.size(64.dp))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 確認按鈕
                Button(
                    onClick = {
                        if (isSettingPassword) {
                            if (password.length == 6 && password == confirmPassword) {
                                // 儲存密碼
                                if (passwordManager.setPassword(password)) {
                                    onPasswordSet(password)
                                    Toast.makeText(context, "密碼設定成功", Toast.LENGTH_SHORT).show()
                                    navController.navigate("main") {
                                        popUpTo("password") { inclusive = true }
                                    }
                                } else {
                                    Toast.makeText(context, "密碼設定失敗", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            // 驗證密碼
                            if (passwordManager.verifyPassword(password)) {
                                Toast.makeText(context, "密碼正確", Toast.LENGTH_SHORT).show()
                                navController.navigate("main") {
                                    popUpTo("password") { inclusive = true }
                                }
                            } else {
                                Toast.makeText(context, "密碼錯誤", Toast.LENGTH_SHORT).show()
                                password = ""
                            }
                        }
                    },
                    enabled = if (isSettingPassword) {
                        password.length == 6 && confirmPassword.length == 6 && password == confirmPassword
                    } else {
                        password.length == 6
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF4A90E2)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Text(
                        text = if (isSettingPassword) "設定密碼" else "解鎖",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
