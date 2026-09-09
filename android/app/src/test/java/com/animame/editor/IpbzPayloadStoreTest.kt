package com.animame.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IpbzPayloadStoreTest {
    @Test fun recognizesOnlyIbpzSignature() {
        assertTrue(IpbzPayloadStore.isIpbz("IPBZ\u0001\u0002".toByteArray(Charsets.ISO_8859_1)) )
        assertFalse(IpbzPayloadStore.isIpbz("IPBX\u0001".toByteArray(Charsets.ISO_8859_1)))
        assertFalse(IpbzPayloadStore.isIpbz(byteArrayOf(1, 2, 3)))
    }

    @Test fun fingerprintIsDeterministicAndSensitiveToPayload() {
        val a = "IPBZabcdef".toByteArray(Charsets.ISO_8859_1)
        val b = "IPBZabcdeg".toByteArray(Charsets.ISO_8859_1)
        assertEquals(IpbzPayloadStore.fingerprint(a), IpbzPayloadStore.fingerprint(a.copyOf()))
        assertTrue(IpbzPayloadStore.fingerprint(a) != IpbzPayloadStore.fingerprint(b))
    }
}
