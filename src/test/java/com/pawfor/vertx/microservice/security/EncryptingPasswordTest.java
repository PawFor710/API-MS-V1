package com.pawfor.vertx.microservice.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptingPasswordTest {

    @Test
    public void encryptPasswordTest() {
        //Given
        EncryptingPassword encryptingPassword = new EncryptingPassword();
        String password = "password";

        //When
        String encryptedPassword = encryptingPassword.encryptPassword(password);

        //Then
        assertNotEquals(password, encryptedPassword);
    }
}