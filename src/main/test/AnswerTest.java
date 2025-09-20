import com.cab302.cab302.Database.Backend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AnswerTest {

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
        backend.recordAttempt(userId, false); // now answered=2, correct=1

        assertDoesNotThrow(() -> backend.recordAttempt(userId, true));
    }
}
