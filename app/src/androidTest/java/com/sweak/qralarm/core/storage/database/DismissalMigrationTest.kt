package com.sweak.qralarm.core.storage.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DismissalMigrationTest {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), QRAlarmDatabase::class.java
    )

    @Test fun preservesExistingAlarmsAndCodeReferences() {
        helper.createDatabase("dismissal-migration", 10).use { db ->
            db.execSQL("INSERT INTO code (codeId, value, name) VALUES (1, 'test-code', 'Bathroom')")
            for (usesCode in listOf(0, 1)) {
                db.execSQL("""
                    INSERT INTO alarm (
                        alarmHourOfDay, alarmMinute, isAlarmEnabled, isAlarmRunning,
                        nextAlarmTimeInMillis, numberOfSnoozes, snoozeDurationInMinutes,
                        numberOfSnoozesLeft, isAlarmSnoozed, ringtone, areVibrationsEnabled,
                        isUsingCode, assignedCodeId, gentleWakeUpDurationInSeconds,
                        temporaryMuteDurationInSeconds
                    ) VALUES (7, 30, 1, 0, 123456, 2, 5, 2, 0, 'GENTLE_GUITAR', 1, $usesCode, 1, 10, 15)
                """.trimIndent())
            }
        }
        helper.runMigrationsAndValidate("dismissal-migration", 11, true, QRAlarmDatabase.MIGRATION_10_11).use { db ->
            db.query("SELECT * FROM alarm ORDER BY alarmId").use { cursor ->
                assertEquals(2, cursor.count)
                for (method in listOf("NONE", "CODE")) {
                    assertTrue(cursor.moveToNext())
                    assertEquals(method, cursor.getString(cursor.getColumnIndexOrThrow("dismissalMethod")))
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("objectCategoryId")))
                    assertEquals(1L, cursor.getLong(cursor.getColumnIndexOrThrow("assignedCodeId")))
                    assertEquals(123456L, cursor.getLong(cursor.getColumnIndexOrThrow("nextAlarmTimeInMillis")))
                    assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("isAlarmEnabled")))
                }
            }
            db.execSQL("UPDATE alarm SET dismissalMethod = 'OBJECT', objectCategoryId = 'sink' WHERE alarmId = 1")
            db.query("SELECT objectCategoryId FROM alarm WHERE alarmId = 1").use {
                assertTrue(it.moveToFirst())
                assertEquals("sink", it.getString(0))
            }
            db.execSQL("PRAGMA foreign_keys = ON")
            db.execSQL("DELETE FROM code WHERE codeId = 1")
            db.query("SELECT assignedCodeId FROM alarm").use {
                while (it.moveToNext()) assertTrue(it.isNull(0))
            }
        }
    }
}
