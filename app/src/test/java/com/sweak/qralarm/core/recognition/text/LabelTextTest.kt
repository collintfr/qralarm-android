package com.sweak.qralarm.core.recognition.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LabelTextTest {
    private val label = listOf(
        "Walgreens", "NON-DROWSY", "Allergy Relief", "24 HOUR ALLERGY",
        "FEXOFENADINE HYDROCHLORIDE", "TABLETS, 180 mg / ANTIHISTAMINE", "45 TABLETS"
    )

    @Test
    fun tokensNormaliseAndDropGenericWords() {
        val tokens = LabelText.tokens(label)
        assertTrue("WALGREENS" in tokens)
        assertTrue("FEXOFENADINE" in tokens)
        assertTrue("180MG" in tokens)
        assertTrue("24HOUR" in tokens)
        assertFalse("TABLETS" in tokens)
        assertFalse("45" in tokens)
    }

    @Test
    fun keywordsAreWordsReadConsistently() {
        val frames = listOf(
            LabelText.tokens(label),
            LabelText.tokens(label.drop(1)),
            LabelText.tokens(label + "WOOD"),
            LabelText.tokens(listOf("FEXOFENADINE", "ALLERGY", "RELIEF"))
        )
        val keywords = LabelText.selectKeywords(frames)
        assertEquals("FEXOFENADINE", keywords.first())
        assertFalse("WOOD" in keywords)
        assertTrue(LabelText.isUsable(keywords))
    }

    @Test
    fun theSameLabelIsVerifiedDespiteOcrSlips() {
        val keywords = LabelText.selectKeywords(List(3) { LabelText.tokens(label) })
        val misread = LabelText.tokens(listOf("Walgreens", "Alergy Relief", "FEXOFENADlNE", "180 mg"))
        assertTrue(LabelText.verify(keywords, misread).verified)
    }

    @Test
    fun aDifferentProductIsRejected() {
        val keywords = LabelText.selectKeywords(List(3) { LabelText.tokens(label) })
        val faceWash = LabelText.tokens(listOf("CeraVe", "Hydrating Facial Cleanser", "Normal to Dry Skin", "12 FL OZ"))
        assertFalse(LabelText.verify(keywords, faceWash).verified)
        // Same pharmacy brand, different medicine: shares only the brand and generic words.
        val otherWalgreens = LabelText.tokens(listOf("Walgreens", "Pain Relief", "IBUPROFEN TABLETS 200 mg", "24 HOUR"))
        assertFalse(LabelText.verify(keywords, otherWalgreens).verified)
    }

    @Test
    fun shortWordsMustMatchExactly() {
        assertFalse(LabelText.matches("24HR", setOf("24H2")))
        assertTrue(LabelText.matches("ALLERGY", setOf("ALERGY")))
        assertEquals(2, LabelText.editDistance("FEXOFENADINE", "FEXOFENAD1N3", 2))
    }
}
