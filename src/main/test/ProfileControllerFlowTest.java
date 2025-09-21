import com.cab302.cab302.Database.Backend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ProfileControllerFlowTest {

    private Backend backend;
    private long userId;

    @BeforeEach
    void setUp() throws Exception {
        backend = new Backend(true);
        userId = backend.addUser(
                "Test User",
                "testuser@example.com",
                "Password123",
                "Physics"
        );
    }
    @AfterEach
    void tearDown() throws Exception {
        backend.close();
    }
    @Test
    void testChangeEmailDoesNotThrow() {
        assertDoesNotThrow(() ->
                backend.updateEmail(userId, "new.email@example.com")
        );
    }
    @Test
    void testChangePasswordDoesNotThrow() {
        assertDoesNotThrow(() ->
                backend.updatePassword(userId, "NewPassw0rd")
        );
    }
    @Test
    void testChangeNameDoesNotThrow() {
        assertDoesNotThrow(() ->
                backend.updateName(userId, "Alice Johnson")
        );
    }
    @Test
    void testDeleteAccountDoesNotThrow() {
        assertDoesNotThrow(() ->
                backend.deleteUser(userId)
        );
    }
    @Test
    void testFullProfileUpdateFlowDoesNotThrow() {
        assertDoesNotThrow(() -> {
            backend.updateName(userId, "Charlie Brown");
            backend.updateEmail(userId, "charlie.brown@example.com");
            backend.updatePassword(userId, "Sup3rStr0ng");
            backend.deleteUser(userId);
        });
    }
}
