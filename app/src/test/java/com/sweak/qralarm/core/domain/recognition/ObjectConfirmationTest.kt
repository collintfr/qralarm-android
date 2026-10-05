package com.sweak.qralarm.core.domain.recognition

import org.junit.Assert.*
import org.junit.Test

class ObjectConfirmationTest {
    @Test fun confirmsThreeFramesOnce() {
        val gate = ObjectConfirmation()
        assertFalse(gate.observe(0.6f, 0))
        assertFalse(gate.observe(0.9f, 75))
        assertTrue(gate.observe(0.7f, 150))
        assertFalse(gate.observe(0.8f, 225))
    }

    @Test fun briefBurstNeedsMinimumElapsedTime() {
        val gate = ObjectConfirmation()
        assertFalse(gate.observe(0.9f, 0))
        assertFalse(gate.observe(0.9f, 10))
        assertFalse(gate.observe(0.9f, 20))
        assertTrue(gate.observe(0.9f, 150))
    }

    @Test fun missingAndLowConfidenceFramesBreakTheSequence() {
        for (miss in listOf(null, 0.599f, Float.NaN)) {
            val gate = ObjectConfirmation()
            gate.observe(0.8f, 0)
            gate.observe(0.8f, 100)
            assertFalse(gate.observe(miss, 200))
            assertEquals(0f, gate.progress)
            assertFalse(gate.observe(0.8f, 300))
            assertFalse(gate.observe(0.8f, 400))
            assertTrue(gate.observe(0.8f, 500))
        }
    }

    @Test fun staleAndDuplicateFramesCannotConfirm() {
        val gate = ObjectConfirmation()
        gate.observe(0.8f, 100)
        assertFalse(gate.observe(0.8f, 100))
        assertFalse(gate.observe(0.8f, 50))
        assertFalse(gate.observe(0.8f, 200))
        assertTrue(gate.observe(0.8f, 300))
    }

    @Test fun longGapsAndSessionResetsRequireFreshEvidence() {
        val gate = ObjectConfirmation()
        gate.observe(0.8f, 0)
        gate.observe(0.8f, 100)
        assertFalse(gate.observe(0.8f, 601))
        assertFalse(gate.observe(0.8f, 701))
        gate.reset()
        assertEquals(0f, gate.progress)
        assertFalse(gate.observe(0.8f, 800))
        assertFalse(gate.observe(0.8f, 900))
        assertTrue(gate.observe(0.8f, 1000))
        gate.reset()
        assertFalse(gate.observe(0.8f, 1100))
    }
}
