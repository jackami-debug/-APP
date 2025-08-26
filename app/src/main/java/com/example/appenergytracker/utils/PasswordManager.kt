package com.example.appenergytracker.utils

import android.content.Context
import android.content.SharedPreferences

class PasswordManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "password_prefs"
        private const val KEY_PASSWORD = "password"
        private const val KEY_IS_PASSWORD_SET = "is_password_set"
    }
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * 檢查是否已經設定過密碼
     */
    fun isPasswordSet(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_PASSWORD_SET, false)
    }
    
    /**
     * 設定密碼
     */
    fun setPassword(password: String): Boolean {
        return try {
            sharedPreferences.edit()
                .putString(KEY_PASSWORD, password)
                .putBoolean(KEY_IS_PASSWORD_SET, true)
                .apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 驗證密碼
     */
    fun verifyPassword(inputPassword: String): Boolean {
        return try {
            val storedPassword = sharedPreferences.getString(KEY_PASSWORD, null)
            if (storedPassword == null) return false
            
            inputPassword == storedPassword
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 清除密碼
     */
    fun clearPassword() {
        sharedPreferences.edit()
            .remove(KEY_PASSWORD)
            .putBoolean(KEY_IS_PASSWORD_SET, false)
            .apply()
    }
}
