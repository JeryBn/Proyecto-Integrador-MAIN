package pe.tecsup.main;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

class AdminCredentialsTest {
    final SecurityConfig config = new SecurityConfig();
    final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    @Test void administratorDisabledUnlessConfigured() {
        assertFalse(config.users(encoder,"admin","","").userExists("admin"));
    }
    @Test void environmentPasswordIsStoredAsBcrypt() {
        var users=config.users(encoder,"admin_test","","SyntheticTestPassword2026!");
        var admin=users.loadUserByUsername("admin_test");
        assertTrue(encoder.matches("SyntheticTestPassword2026!",admin.getPassword()));
        assertTrue(admin.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }
    @Test void rejectsDemoNameCollisionAndWeakPassword() {
        assertThrows(IllegalArgumentException.class, () -> config.users(encoder,"alumno","","SyntheticTestPassword2026!"));
        assertThrows(IllegalArgumentException.class, () -> config.users(encoder,"admin","","short"));
        assertThrows(IllegalArgumentException.class, () -> config.users(encoder,"admin","not-bcrypt",""));
    }
}
