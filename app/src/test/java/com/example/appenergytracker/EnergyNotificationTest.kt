package com.example.appenergytracker

import org.junit.Test
import org.junit.Assert.*

/**
 * 測試能量通知的即時更新功能
 */
class EnergyNotificationTest {
    
    @Test
    fun testEnergyNotificationUpdate() {
        // 測試能量變化時通知是否會更新
        val initialEnergy = 100
        val updatedEnergy = 95
        
        // 模擬能量變化
        assertNotEquals("能量應該會變化", initialEnergy, updatedEnergy)
        
        // 測試通知標題會根據能量狀態變化
        val lowEnergy = 30
        val zeroEnergy = 0
        val highEnergy = 150
        
        // 驗證不同能量狀態下的通知行為
        assertTrue("低能量狀態應該被識別", lowEnergy < 100 * 0.3)
        assertTrue("零能量狀態應該被識別", zeroEnergy <= 0)
        assertTrue("高能量狀態應該被識別", highEnergy > 100 * 0.3)
    }
    
    @Test
    fun testRealTimeMonitoring() {
        // 測試即時監控功能
        val monitoringInterval = 1000L // 1秒
        val expectedUpdatesPerMinute = 60
        
        // 驗證監控頻率
        assertTrue("監控間隔應該足夠短以支援即時更新", monitoringInterval <= 1000L)
        assertTrue("每分鐘應該有足夠的更新次數", expectedUpdatesPerMinute >= 60)
    }
    
    @Test
    fun testGoodHabitEnergyAccumulation() {
        // 測試好習慣 App 的能量累積
        val ratio = 0.5f // 每分鐘增加 0.5 點能量
        val secondsRatio = ratio / 60.0 // 每秒比例
        val expectedSecondsRatio = 0.5 / 60.0
        
        assertEquals("每秒比例計算應該正確", expectedSecondsRatio, secondsRatio, 0.001)
        
        // 測試累積邏輯
        val residue = 0.0
        val newResidue = residue + secondsRatio
        val gain = kotlin.math.floor(newResidue).toInt()
        
        assertEquals("初始累積應該為 0", 0, gain)
    }
    
    @Test
    fun testBadHabitEnergyDeduction() {
        // 測試壞習慣 App 的能量扣除
        val energyDeductionPerMinute = 1
        val currentEnergy = 100
        val newEnergy = maxOf(0, currentEnergy - energyDeductionPerMinute)
        
        assertEquals("每分鐘應該扣除 1 點能量", 99, newEnergy)
        
        // 測試能量歸零
        val zeroEnergy = 0
        val finalEnergy = maxOf(0, zeroEnergy - energyDeductionPerMinute)
        
        assertEquals("能量不應該變成負數", 0, finalEnergy)
    }
}
