package com.example.appenergytracker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.appenergytracker.utils.PasswordManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PasswordManagerTest {
    
    private lateinit var passwordManager: PasswordManager
    private lateinit var context: Context
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        passwordManager = PasswordManager(context)
        // 清除之前的測試資料
        passwordManager.clearPassword()
    }
    
    @Test
    fun testPasswordNotSetInitially() {
        assertFalse(passwordManager.isPasswordSet())
    }
    
    @Test
    fun testSetPassword() {
        val testPassword = "123456"
        assertTrue(passwordManager.setPassword(testPassword))
        assertTrue(passwordManager.isPasswordSet())
    }
    
    @Test
    fun testVerifyPassword() {
        val testPassword = "123456"
        passwordManager.setPassword(testPassword)
        
        assertTrue(passwordManager.verifyPassword(testPassword))
        assertFalse(passwordManager.verifyPassword("654321"))
        assertFalse(passwordManager.verifyPassword("12345"))
    }
    
    @Test
    fun testClearPassword() {
        val testPassword = "123456"
        passwordManager.setPassword(testPassword)
        assertTrue(passwordManager.isPasswordSet())
        
        passwordManager.clearPassword()
        assertFalse(passwordManager.isPasswordSet())
        assertFalse(passwordManager.verifyPassword(testPassword))
    }
}
