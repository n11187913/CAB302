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

}
