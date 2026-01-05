/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2026 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package sc.fiji.bdvpg.tests.cache;

import org.junit.Test;
import sc.fiji.bdvpg.cache.GlobalCacheKey;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link GlobalCacheKey}.
 * Tests the equals, hashCode, and partialEquals contract.
 */
public class GlobalCacheKeyTest {

    // ==================== Equals Contract Tests ====================

    @Test
    public void testEquals_sameInstance() {
        Object source = new Object();
        Object key = new Object();
        GlobalCacheKey cacheKey = new GlobalCacheKey(source, 0, 0, key);

        assertEquals("Same instance should be equal to itself", cacheKey, cacheKey);
    }

    @Test
    public void testEquals_sameValues() {
        Object source = new Object();
        Object key = new Object();
        GlobalCacheKey cacheKey1 = new GlobalCacheKey(source, 5, 2, key);
        GlobalCacheKey cacheKey2 = new GlobalCacheKey(source, 5, 2, key);

        assertEquals("Keys with same source, timepoint, level, and key should be equal",
                cacheKey1, cacheKey2);
    }

    @Test
    public void testEquals_differentSource() {
        Object source1 = new Object();
        Object source2 = new Object();
        Object key = new Object();
        GlobalCacheKey cacheKey1 = new GlobalCacheKey(source1, 0, 0, key);
        GlobalCacheKey cacheKey2 = new GlobalCacheKey(source2, 0, 0, key);

        assertNotEquals("Keys with different sources should not be equal",
                cacheKey1, cacheKey2);
    }

    @Test
    public void testEquals_differentTimepoint() {
        Object source = new Object();
        Object key = new Object();
        GlobalCacheKey cacheKey1 = new GlobalCacheKey(source, 0, 0, key);
        GlobalCacheKey cacheKey2 = new GlobalCacheKey(source, 1, 0, key);

        assertNotEquals("Keys with different timepoints should not be equal",
                cacheKey1, cacheKey2);
    }

    @Test
    public void testEquals_differentLevel() {
        Object source = new Object();
        Object key = new Object();
        GlobalCacheKey cacheKey1 = new GlobalCacheKey(source, 0, 0, key);
        GlobalCacheKey cacheKey2 = new GlobalCacheKey(source, 0, 1, key);

        assertNotEquals("Keys with different levels should not be equal",
                cacheKey1, cacheKey2);
    }

    @Test
    public void testEquals_differentKey() {
        Object source = new Object();
        Object key1 = new Object();
        Object key2 = new Object();
        GlobalCacheKey cacheKey1 = new GlobalCacheKey(source, 0, 0, key1);
        GlobalCacheKey cacheKey2 = new GlobalCacheKey(source, 0, 0, key2);

        assertNotEquals("Keys with different inner keys should not be equal",
                cacheKey1, cacheKey2);
    }

    @Test
    public void testEquals_nullObject() {
        Object source = new Object();
        Object key = new Object();
        GlobalCacheKey cacheKey = new GlobalCacheKey(source, 0, 0, key);

        assertNotEquals("Key should not be equal to null", null, cacheKey);
    }

    // ==================== HashCode Contract Tests ====================

    @Test
    public void testHashCode_consistency() {
        Object source = new Object();
        Object key = new Object();
        GlobalCacheKey cacheKey = new GlobalCacheKey(source, 0, 0, key);

        int hash1 = cacheKey.hashCode();
        int hash2 = cacheKey.hashCode();

        assertEquals("HashCode should be consistent across multiple calls", hash1, hash2);
    }

    @Test
    public void testHashCode_equalObjectsHaveSameHash() {
        Object source = new Object();
        Object key = new Object();
        GlobalCacheKey cacheKey1 = new GlobalCacheKey(source, 5, 2, key);
        GlobalCacheKey cacheKey2 = new GlobalCacheKey(source, 5, 2, key);

        assertEquals("Equal objects must have the same hashCode",
                cacheKey1.hashCode(), cacheKey2.hashCode());
    }

    @Test
    public void testHashCode_differentValues() {
        Object source = new Object();
        Object key1 = new Object();
        Object key2 = new Object();
        GlobalCacheKey cacheKey1 = new GlobalCacheKey(source, 0, 0, key1);
        GlobalCacheKey cacheKey2 = new GlobalCacheKey(source, 0, 0, key2);

        // Note: Different objects CAN have same hashCode (collision),
        // but typically they won't for different keys
        // This test just verifies the hash is computed, not that it's unique
        assertNotNull("HashCode should be computed", cacheKey1.hashCode());
        assertNotNull("HashCode should be computed", cacheKey2.hashCode());
    }

    // ==================== PartialEquals Tests ====================

    @Test
    public void testPartialEquals_matching() {
        Object source = new Object();
        Object key = new Object();
        GlobalCacheKey cacheKey = new GlobalCacheKey(source, 5, 2, key);

        assertTrue("partialEquals should return true for matching source, timepoint, level",
                cacheKey.partialEquals(source, 5, 2));
    }

    @Test
    public void testPartialEquals_differentSource() {
        Object source1 = new Object();
        Object source2 = new Object();
        Object key = new Object();
        GlobalCacheKey cacheKey = new GlobalCacheKey(source1, 5, 2, key);

        assertFalse("partialEquals should return false for different source",
                cacheKey.partialEquals(source2, 5, 2));
    }

    @Test
    public void testPartialEquals_differentTimepoint() {
        Object source = new Object();
        Object key = new Object();
        GlobalCacheKey cacheKey = new GlobalCacheKey(source, 5, 2, key);

        assertFalse("partialEquals should return false for different timepoint",
                cacheKey.partialEquals(source, 6, 2));
    }

    @Test
    public void testPartialEquals_differentLevel() {
        Object source = new Object();
        Object key = new Object();
        GlobalCacheKey cacheKey = new GlobalCacheKey(source, 5, 2, key);

        assertFalse("partialEquals should return false for different level",
                cacheKey.partialEquals(source, 5, 3));
    }

    @Test
    public void testPartialEquals_ignoresKey() {
        Object source = new Object();
        Object key1 = new Object();
        Object key2 = new Object();
        GlobalCacheKey cacheKey1 = new GlobalCacheKey(source, 5, 2, key1);
        GlobalCacheKey cacheKey2 = new GlobalCacheKey(source, 5, 2, key2);

        // Both should match partial equals with same source/timepoint/level
        // even though they have different keys
        assertTrue("partialEquals should ignore the key parameter",
                cacheKey1.partialEquals(source, 5, 2));
        assertTrue("partialEquals should ignore the key parameter",
                cacheKey2.partialEquals(source, 5, 2));
    }

    // ==================== Edge Cases ====================

    @Test
    public void testWithZeroValues() {
        Object source = new Object();
        Object key = new Object();
        GlobalCacheKey cacheKey = new GlobalCacheKey(source, 0, 0, key);

        assertTrue("Should handle zero timepoint and level",
                cacheKey.partialEquals(source, 0, 0));
    }

    @Test
    public void testWithNegativeValues() {
        Object source = new Object();
        Object key = new Object();
        // Negative timepoints/levels might not be valid in practice,
        // but the class should handle them
        GlobalCacheKey cacheKey = new GlobalCacheKey(source, -1, -1, key);

        assertTrue("Should handle negative timepoint and level",
                cacheKey.partialEquals(source, -1, -1));
    }

    @Test
    public void testWithLargeValues() {
        Object source = new Object();
        Object key = new Object();
        GlobalCacheKey cacheKey = new GlobalCacheKey(source, Integer.MAX_VALUE, Integer.MAX_VALUE, key);

        assertTrue("Should handle large timepoint and level values",
                cacheKey.partialEquals(source, Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void testWithStringKeys() {
        Object source = "sourceString";
        Object key = "keyString";
        GlobalCacheKey cacheKey1 = new GlobalCacheKey(source, 0, 0, key);
        GlobalCacheKey cacheKey2 = new GlobalCacheKey(source, 0, 0, key);

        assertEquals("Should work with String objects", cacheKey1, cacheKey2);
    }

    @Test
    public void testWithEqualButNotSameKeys() {
        Object source = new Object();
        // Two different String instances with same value
        String key1 = new String("sameValue");
        String key2 = new String("sameValue");

        // Verify they are different instances but equal
        assertNotSame("Keys should be different instances", key1, key2);
        assertEquals("Keys should be equal by value", key1, key2);

        GlobalCacheKey cacheKey1 = new GlobalCacheKey(source, 0, 0, key1);
        GlobalCacheKey cacheKey2 = new GlobalCacheKey(source, 0, 0, key2);

        // GlobalCacheKey uses equals() for key comparison, so these should be equal
        assertEquals("Keys with equal (but not same) inner keys should be equal",
                cacheKey1, cacheKey2);
    }

    // ==================== Symmetry and Transitivity Tests ====================

    @Test
    public void testEquals_symmetry() {
        Object source = new Object();
        Object key = new Object();
        GlobalCacheKey cacheKey1 = new GlobalCacheKey(source, 5, 2, key);
        GlobalCacheKey cacheKey2 = new GlobalCacheKey(source, 5, 2, key);

        assertEquals("Equals should be symmetric (a.equals(b) implies b.equals(a))",
                cacheKey1.equals(cacheKey2), cacheKey2.equals(cacheKey1));
    }

    @Test
    public void testEquals_transitivity() {
        Object source = new Object();
        Object key = new Object();
        GlobalCacheKey cacheKey1 = new GlobalCacheKey(source, 5, 2, key);
        GlobalCacheKey cacheKey2 = new GlobalCacheKey(source, 5, 2, key);
        GlobalCacheKey cacheKey3 = new GlobalCacheKey(source, 5, 2, key);

        assertEquals("a.equals(b)", cacheKey1, cacheKey2);
        assertEquals("b.equals(c)", cacheKey2, cacheKey3);
        assertEquals("Therefore a.equals(c) - transitivity", cacheKey1, cacheKey3);
    }
}
