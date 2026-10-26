package com.ccwatermark.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {
    @Test fun newerReleaseIsDetected() {
        assertTrue(VersionComparator.compare("1.3.0", "1.2.1") > 0)
        assertTrue(VersionComparator.compare("v1.2.2", "1.2.1") > 0)
    }

    @Test fun olderAndEqualVersionsAreNotUpdates() {
        assertTrue(VersionComparator.compare("1.2.0", "1.2.1") < 0)
        assertEquals(0, VersionComparator.compare("v1.2.1", "1.2.1"))
        assertEquals(0, VersionComparator.compare("1.2", "1.2.0"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidVersionIsRejected() {
        VersionComparator.compare("latest", "1.2.1")
    }
}
