import com.cab302.cab302.Database.Backend;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Simple unit tests for Backend (user-related functionality).
 *
 * WARNING: these tests delete the database file named `cab302.db` in the working directory
 * before and after each test to ensure a clean state. Back up any real data before running.
 */
public class UserTest {

    private Backend backend;

    @BeforeEach
    void setUp() throws Exception {
        backend = new Backend(true);
    }

    @AfterEach
    void tearDown() throws Exception {
        backend.close();
    }

    @Test
    void testAddUserAndAuthenticate() throws Exception {
        long id = backend.addUser("Alice", "alice@example.com", "Secret123", "Calculus");
        assertTrue(id > 0, "User ID should be positive");

        boolean authSuccess = backend.authenticate("alice@example.com", "Secret123");
        assertTrue(authSuccess, "Authentication should succeed with correct password");

        boolean authFail = backend.authenticate("alice@example.com", "WrongPassword");
        assertFalse(authFail, "Authentication should fail with wrong password");
    }

    @Test
    void testGetUser() throws Exception {
        backend.addUser("Bob", "bob@example.com", "Pass456", "Electrical");

        Optional<Backend.User> userOpt = backend.getUser("bob@example.com");
        assertTrue(userOpt.isPresent(), "User should be returned");

        Backend.User user = userOpt.get();
        assertEquals("Bob", user.name());
        assertEquals("bob@example.com", user.email());
        assertEquals("Electrical", user.focusArea());
    }

    @Test
    void testDuplicateEmailFails() throws Exception {
        backend.addUser("Charlie", "charlie@example.com", "abc123", "Physics");

        assertThrows(SQLException.class, () -> {
            backend.addUser("Charlie2", "charlie@example.com", "def456", "Mechanical");
        }, "Should not allow duplicate email");
    }
}
