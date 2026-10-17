package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;

public final class AuthNavigationTest {
    @Test public void googleOauthOnlyTrustedSecureHost() {
        assertTrue(AuthNavigation.isGoogleOAuth("https://accounts.google.com/o/oauth2/v2/auth?client_id=abc"));
        assertTrue(AuthNavigation.isGoogleOAuth("https://accounts.google.com/o/oauth2/auth"));
        assertFalse(AuthNavigation.isGoogleOAuth("https://accounts.google.com/signin/v2"));
        assertFalse(AuthNavigation.isGoogleOAuth("https://accounts.google.com.evil.net/o/oauth2/auth"));
        assertFalse(AuthNavigation.isGoogleOAuth("http://accounts.google.com/o/oauth2/auth"));
        assertFalse(AuthNavigation.isGoogleOAuth("https://other.com/?next=https://accounts.google.com/o/oauth2/auth"));
        assertFalse(AuthNavigation.isGoogleOAuth("javascript:alert(1)"));
    }
    @Test public void noVideoInspectionOnSensitiveLogin() {
        assertTrue(AuthNavigation.isSensitiveLoginPage("https://accounts.google.com/signin/v2"));
        assertTrue(AuthNavigation.isSensitiveLoginPage("https://m.facebook.com/checkpoint/123"));
        assertTrue(AuthNavigation.isSensitiveLoginPage("https://www.facebook.com/login/"));
        assertFalse(AuthNavigation.isSensitiveLoginPage("https://www.facebook.com/watch/?v=123"));
        assertFalse(AuthNavigation.isSensitiveLoginPage("https://facebook.com.evil.com/login/"));
    }
    @Test public void facebookChallengeDetection() {
        assertTrue(AuthNavigation.isFacebookCheckpoint("https://www.facebook.com/checkpoint/123"));
        assertTrue(AuthNavigation.isFacebookCheckpoint("https://m.facebook.com/checkpoint/"));
        assertFalse(AuthNavigation.isFacebookCheckpoint("https://facebook.com.evil.com/checkpoint/"));
        assertFalse(AuthNavigation.isFacebookCheckpoint("https://www.facebook.com/profile.php"));
        assertFalse(AuthNavigation.isFacebookCheckpoint("http://www.facebook.com/checkpoint/"));
    }
}
