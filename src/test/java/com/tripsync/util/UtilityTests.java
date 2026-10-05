package com.tripsync.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilityTests {

    @Test
    @DisplayName("PasswordUtil hash, verify, and token generation test")
    void testPasswordUtil() {
        String raw = "SecureP@ssword123";
        String hash = PasswordUtil.hash(raw);

        assertNotNull(hash);
        assertNotEquals(raw, hash);
        assertTrue(PasswordUtil.verify(raw, hash));
        assertFalse(PasswordUtil.verify("WrongPassword", hash));

        String token = PasswordUtil.generateToken();
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("InviteCodeUtil length and uniqueness test")
    void testInviteCodeUtil() {
        String code1 = InviteCodeUtil.generate(8);
        String code2 = InviteCodeUtil.generate(8);

        assertNotNull(code1);
        assertNotNull(code2);
        assertEquals(8, code1.length());
        assertNotEquals(code1, code2);
    }
}
