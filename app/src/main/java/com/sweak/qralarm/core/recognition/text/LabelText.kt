package com.sweak.qralarm.core.recognition.text

/**
 * Recognises an item by the words printed on it. Enrollment keeps the words that are read
 * consistently across several photos of the label ("keywords"); at alarm time a camera frame
 * counts when enough of those keywords are read in it. Only text matters, so the background
 * plays no part. Mirrors eval/text_match.py, where the rules were tuned.
 */
object LabelText {

    data class Rules(
        /** A keyword must be read in at least this share of the enrollment photos (and in two). */
        val keywordFrameShare: Double = 0.25,
        val maxKeywords: Int = 12,
        /** A frame counts when at least this many keywords are read in it... */
        val minMatched: Int = 3,
        /** ...and at least this share of all keywords... */
        val minMatchedShare: Double = 0.4,
        /** ...and one of them is at least this long, so short generic words are never enough. */
        val minLongKeyword: Int = 6
    )

    val DEFAULT_RULES = Rules()

    /** Words that say nothing about which product it is. */
    private val STOP_WORDS = setOf(
        "THE", "AND", "FOR", "WITH", "NET", "WT", "USE", "ONLY", "TABLET", "TABLETS", "CAPSULE",
        "CAPSULES", "MG", "ML", "OZ", "FL", "PER", "EACH", "NEW", "NOT", "ARE", "THIS", "YOUR", "ALL"
    )
    private val UNIT = Regex("^(\\d+)(MG|ML|G|OZ|HR|HOUR|CT|MCG|IU|L|KG|%)$")
    private val WORD = Regex("[\\p{L}\\p{N}%]+")

    /** Normalised words of the recognised [lines]: upper case, "180 mg" joined to "180MG". */
    fun tokens(lines: List<String>): Set<String> {
        val result = mutableSetOf<String>()
        lines.forEach { line ->
            val words = WORD.findAll(line.uppercase()).map { it.value }.toList()
            var i = 0
            while (i < words.size) {
                val word = if (words[i].all(Char::isDigit) && i + 1 < words.size &&
                    UNIT.matches(words[i] + words[i + 1])
                ) {
                    (words[i] + words[i + 1]).also { i++ }
                } else words[i]
                i++
                if (word.length >= 3 && word !in STOP_WORDS && word.any(Char::isLetter)) result += word
            }
        }
        return result
    }

    /** Keywords from the words read in each enrollment photo, most consistent and longest first. */
    fun selectKeywords(frames: List<Set<String>>, rules: Rules = DEFAULT_RULES): List<String> {
        val counts = frames.flatten().groupingBy { it }.eachCount()
        val needed = maxOf(2, kotlin.math.ceil(frames.size * rules.keywordFrameShare).toInt())
        return counts.filterValues { it >= needed }.keys
            .sortedWith(compareByDescending<String> { counts.getValue(it) }.thenByDescending { it.length })
            .take(rules.maxKeywords)
    }

    /** Whether the keywords are distinctive enough to recognise the item reliably. */
    fun isUsable(keywords: List<String>, rules: Rules = DEFAULT_RULES): Boolean =
        keywords.size >= rules.minMatched && keywords.any { it.length >= rules.minLongKeyword }

    /** OCR misreads a letter or two in longer words; short words must be exact. */
    fun matches(keyword: String, frameTokens: Set<String>): Boolean {
        if (keyword in frameTokens) return true
        val limit = when {
            keyword.length <= 4 -> 0
            keyword.length <= 8 -> 1
            else -> 2
        }
        return limit > 0 && frameTokens.any { editDistance(keyword, it, limit) <= limit }
    }

    data class Verdict(val matched: List<String>, val verified: Boolean)

    fun verify(keywords: List<String>, frameTokens: Set<String>, rules: Rules = DEFAULT_RULES): Verdict {
        val matched = keywords.filter { matches(it, frameTokens) }
        val verified = keywords.isNotEmpty() &&
            matched.size >= minOf(rules.minMatched, keywords.size) &&
            matched.size >= rules.minMatchedShare * keywords.size &&
            matched.any { it.length >= rules.minLongKeyword }
        return Verdict(matched, verified)
    }

    /** Levenshtein distance, giving up (returning limit + 1) once it cannot stay within [limit]. */
    fun editDistance(a: String, b: String, limit: Int): Int {
        if (kotlin.math.abs(a.length - b.length) > limit) return limit + 1
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i
            var rowMin = current[0]
            for (j in 1..b.length) {
                current[j] = minOf(
                    previous[j] + 1,
                    current[j - 1] + 1,
                    previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                )
                rowMin = minOf(rowMin, current[j])
            }
            if (rowMin > limit) return limit + 1
            previous = current
        }
        return previous[b.length]
    }
}
