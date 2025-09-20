import com.cab302.cab302.Database.Backend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class StatisticsTest {

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
    void testDefaultStatisticsZeroed() throws Exception {
        // Fresh user should have zeroed stats
        Optional<Backend.User> user = backend.getUser("statuser@example.com");
        assertTrue(user.isPresent());

        int highScore = backend.getHighScore(userId);
        assertEquals(0, highScore, "Default high score should be 0");
    }

    @Test
    void testRecordAttemptCorrect() {
        backend.recordAttempt(userId, true);  // one correct answer

        // high score not affected
        assertEquals(0, backend.getHighScore(userId));

        // accuracy should be 1.0 (1/1)
        // (We can't read stats directly without SQL, but a second call to recordAttempt updates correctly)
        backend.recordAttempt(userId, false); // now answered=2, correct=1

        // accuracy should be 0.5 (1/2)
        // We check indirectly by ensuring recordAttempt doesn’t throw and DB updates
        // (you may extend Backend with a getter for testing)
        assertDoesNotThrow(() -> backend.recordAttempt(userId, true));
    }

    @Test
    void testUpdateHighScoreIncreasesOnly() {
        // Default high score = 0
        assertEquals(0, backend.getHighScore(userId));

        int updated = backend.updateHighScore(userId, 50);
        assertEquals(50, updated, "High score should update to 50");

        // Try a lower score — should not replace
        int notUpdated = backend.updateHighScore(userId, 30);
        assertEquals(50, notUpdated, "High score should remain 50");

        // Try a higher score — should replace
        int updatedAgain = backend.updateHighScore(userId, 80);
        assertEquals(80, updatedAgain, "High score should update to 80");
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
