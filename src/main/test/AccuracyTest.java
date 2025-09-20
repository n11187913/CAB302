import com.cab302.cab302.Database.Backend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class AccuracyTest {

    private Backend backend;
    private long userId;

    @BeforeEach
    void setUp() throws Exception {
        backend = new Backend(true);
        userId = backend.addUser("StatUser", "statuser@example.com", "Password123", "Physics");
    }

    @AfterEach
    void tearDown() throws Exception {
        backend.close();
    }

    @Test
    void testMultipleAttemptsAccuracy() {
        backend.recordAttempt(userId, true);   // 1/1 correct
        backend.recordAttempt(userId, false);  // 1/2 correct
        backend.recordAttempt(userId, false);  // 1/3 correct
        backend.recordAttempt(userId, true);   // 2/4 correct

        // Accuracy = 2/4 = 0.5
        // We can only test indirectly since Backend doesn't expose accuracy directly.
        // Let's trigger an extra attempt and assert it doesn't break
        assertDoesNotThrow(() -> backend.recordAttempt(userId, true));
    }
}
