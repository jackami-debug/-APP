package com.example.appenergytracker.util

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

class PasswordManager private constructor(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "password_prefs"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_MASTER_KEY_ALIAS = "password_master_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_LENGTH_BIT = 128
        
        @Volatile
        private var INSTANCE: PasswordManager? = null
        
        fun getInstance(context: Context): PasswordManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PasswordManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    
    /**
     * 設定密碼
     */
    fun setPassword(password: String): Boolean {
        return try {
            val hashedPassword = hashPassword(password)
            val encryptedHash = encryptData(hashedPassword)
            sharedPreferences.edit()
                .putString(KEY_PASSWORD_HASH, encryptedHash)
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
    fun verifyPassword(password: String): Boolean {
        return try {
            val storedEncryptedHash = sharedPreferences.getString(KEY_PASSWORD_HASH, null)
            if (storedEncryptedHash == null) return false
            
            val decryptedHash = decryptData(storedEncryptedHash)
            val inputHash = hashPassword(password)
            
            decryptedHash == inputHash
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 檢查是否已設定密碼
     */
    fun hasPassword(): Boolean {
        return sharedPreferences.contains(KEY_PASSWORD_HASH)
    }
    
    /**
     * 清除密碼
     */
    fun clearPassword(): Boolean {
        return try {
            sharedPreferences.edit()
                .remove(KEY_PASSWORD_HASH)
                .apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 簡單的密碼雜湊（在實際應用中應使用更安全的雜湊算法）
     */
    private fun hashPassword(password: String): String {
        // 這裡使用簡單的雜湊，實際應用中應使用 bcrypt 或 Argon2
        return password.hashCode().toString()
    }
    
    /**
     * 加密數據
     */
    private fun encryptData(data: String): String {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val encryptedBytes = cipher.doFinal(data.toByteArray())
        val combined = cipher.iv + encryptedBytes
        
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }
    
    /**
     * 解密數據
     */
    private fun decryptData(encryptedData: String): String {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        
        val decoded = Base64.decode(encryptedData, Base64.DEFAULT)
        val iv = decoded.sliceArray(0 until IV_SIZE)
        val encryptedBytes = decoded.sliceArray(IV_SIZE until decoded.size)
        
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes)
    }
    
    /**
     * 獲取或創建密鑰
     */
    private fun getOrCreateSecretKey(): SecretKey {
        return if (keyStore.containsAlias(KEY_MASTER_KEY_ALIAS)) {
            keyStore.getKey(KEY_MASTER_KEY_ALIAS, null) as SecretKey
        } else {
            createSecretKey()
        }
    }
    
    /**
     * 創建新的密鑰
     */
    private fun createSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keyGenSpec = KeyGenParameterSpec.Builder(
            KEY_MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .build()
        
        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }
    
}
