package com.owotest;

import com.owo.entity.Akun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AkunTest {
    private Akun akun;

    @BeforeEach
    void setUp() {
        akun = new Akun("Test User", "test@example.com", "password123");
    }

    @Test
    void testConstructorAndGetters() {
        assertEquals("Test User", akun.getNama());
        assertEquals("test@example.com", akun.getEmail());
    }

    @Test
    void testSetNama() {
        akun.setNama("New Name");
        assertEquals("New Name", akun.getNama());
    }

    @Test
    void testSetEmail() {
        akun.setEmail("new@example.com");
        assertEquals("new@example.com", akun.getEmail());
    }

    @Test
    void testSetHashedPassword() {
        akun.setHashedPassword("newpassword");
        assertTrue(akun.checkPassword("newpassword"));
        assertFalse(akun.checkPassword("password123"));
    }

    @Test
    void testCheckPassword() {
        assertTrue(akun.checkPassword("password123"));
        assertFalse(akun.checkPassword("wrongpassword"));
    }

    @Test
    void testCheckEmail() {
        assertTrue(akun.checkEmail("test@example.com"));
        assertFalse(akun.checkEmail("wrong@example.com"));
    }
}
