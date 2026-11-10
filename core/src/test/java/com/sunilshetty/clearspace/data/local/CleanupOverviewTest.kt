package com.sunilshetty.clearspace.data.local
import com.sunilshetty.clearspace.data.model.*
import org.junit.Assert.assertEquals
import org.junit.Test
class CleanupOverviewTest {
    @Test fun overlappingCategoriesOnlyCountAFileOnce() {
        assertEquals(150L, uniqueCandidateBytes(listOf(CleanupCandidate("a", 100), CleanupCandidate("a", 100), CleanupCandidate("b", 50))))
    }
    @Test fun emptyAndNegativeSizesDoNotCreateSavings() {
        assertEquals(0L, uniqueCandidateBytes(emptyList()))
        assertEquals(0L, uniqueCandidateBytes(listOf(CleanupCandidate("a", -5))))
    }
}
