package com.cab302.cab302.Database;

import java.security.SecureRandom;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Backend implements AutoCloseable {

    // DB file created in the working directory
    private static final String DB_URL = "jdbc:sqlite:cab302.db";

    // Allowed focus areas (simple whitelist)
    private static final String[] ALLOWED_FOCUS = {
            "Electrical", "Dynamics", "Calculus", "Physics", "Mechanical", "Probability", "Other"
    };

    // PBKDF2 params
    private static final String KDF_ALGO = "PBKDF2WithHmacSHA256";
    private static final int KDF_ITER = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private static Connection conn;

    public Backend() throws SQLException {
        connect();
        initSchema();
    }

    public Backend(boolean dev) throws SQLException {
        connect(dev);
        initSchema();
    }

    // API (kept compatible with existing method names)

    /** Create a new user (stored in 'profiles'). */
    public long addUser(String name, String email, String password, String focusArea) throws Exception {
        requireNonBlank(email, "email");
        requireNonBlank(password, "password");

        int focusId = ensureFocusExists(sanitizeFocus(focusArea));
        String[] kdf = hashPassword(password);

        // NOTE: name/email are required in schema — use sensible placeholders for now

        String sql = """
            INSERT INTO profiles(name, email, password_hash, password_salt, studentTeacher, created_at)
            VALUES(?,?,?,?,?,?)
        """;

        long profileId;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, kdf[1]); // hash
            ps.setString(4, kdf[0]); // salt
            ps.setString(5, "student"); // default role
            ps.setString(6, Instant.now().toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) profileId = rs.getLong(1);
                else throw new SQLException("Failed to insert profile");
            }
        }

        // map profile to focus area
        try (PreparedStatement map = conn.prepareStatement(
                "INSERT OR IGNORE INTO profile_focus_areas(profile_id, focus_area_id) VALUES(?,?)")) {
            map.setLong(1, profileId);
            map.setInt(2, focusId);
            map.executeUpdate();
        }

        String statsSql = """
          INSERT INTO statistics(profile_id, correct_answers, answered, highscore, accuracy)
          VALUES(?,?,?,?,?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(statsSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, profileId);
            ps.setInt(2, 0);
            ps.setInt(3, 0);
            ps.setInt(4, 0);
            ps.setDouble(5, 0);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) profileId = rs.getLong(1);
                else throw new SQLException("Failed to insert profile");
            }
        }

        return profileId;
    }

    /** Authenticate a user by username/password against 'profiles'. */
    public boolean authenticate(String email, String password) throws Exception {
        String sql = "SELECT password_hash, password_salt FROM profiles WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                String hash = rs.getString("password_hash");
                String salt = rs.getString("password_salt");
                return verifyPassword(password, salt, hash);
            }
        }
    }
    public void updateEmail(long profileId, String newEmail) throws SQLException {
        requireNonBlank(newEmail, "email");
        String sql = "UPDATE profiles SET email = ? WHERE profile_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newEmail.trim());
            ps.setLong(2, profileId);
            if (ps.executeUpdate() == 0) throw new SQLException("No profile found for id=" + profileId);
        }
    }

    public void updateName(long profileId, String newName) throws SQLException {
        requireNonBlank(newName, "name");
        String sql = "UPDATE profiles SET name = ? WHERE profile_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newName.trim());
            ps.setLong(2, profileId);
            if (ps.executeUpdate() == 0) throw new SQLException("No profile found for id=" + profileId);
        }
    }

    public void updatePassword(long profileId, String newPassword) throws Exception {
        requireNonBlank(newPassword, "password");
        String[] kdf = hashPassword(newPassword);
        String sql = "UPDATE profiles SET password_hash = ?, password_salt = ? WHERE profile_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kdf[1]);
            ps.setString(2, kdf[0]);
            ps.setLong(3, profileId);
            if (ps.executeUpdate() == 0) throw new SQLException("No profile found for id=" + profileId);
        }
    }

    public void deleteUser(long profileId) throws SQLException {
        String sql = "DELETE FROM profiles WHERE profile_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, profileId);
            ps.executeUpdate(); // cascades remove children
        }
    }


    /** Fetch a user summary (first focus area name if any). */
    public Optional<User> getUser(String email) throws SQLException {
        String sql = """
            SELECT p.name, p.profile_id, p.email, p.created_at,
                   (SELECT fa.area_name
                      FROM profile_focus_areas pfa
                      JOIN focus_areas fa ON fa.focus_area_id = pfa.focus_area_id
                     WHERE pfa.profile_id = p.profile_id
                     ORDER BY fa.area_name LIMIT 1) AS focus_area
              FROM profiles p
             WHERE p.email = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(
                            rs.getLong("profile_id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("focus_area"),
                            rs.getString("created_at")
                    ));
                }
            }
        }
        return Optional.empty();
    }



    /** Record a quiz attempt (includes correctness flag). */
    public long recordAttempt(long profileId, boolean isCorrect) {

        // Initialize variables to hold the stats fetched from the database
        int currentCorrectAnswers = 0;
        int currentAnswered = 0;

        // SQL to get the current stats for the profile
        String getCurrentStats = """
            SELECT s.correct_answers, s.answered FROM statistics s WHERE s.profile_id = ?
            """;

        // Use try-with-resources for PreparedStatement and ResultSet to ensure they are closed
        try (PreparedStatement selectPs = conn.prepareStatement(getCurrentStats)) {
            selectPs.setLong(1, profileId);

            // FIX 1: Use executeQuery() for SELECT. It returns a ResultSet with the query results.
            try (ResultSet rs = selectPs.executeQuery()) {
                // Check if a record was found before trying to read from it
                if (rs.next()) {
                    currentCorrectAnswers = rs.getInt("correct_answers");
                    currentAnswered = rs.getInt("answered");
                }
                // Note: If no record exists, the variables will remain 0,
                // and the UPDATE statement below will fail to update any rows.
                // You may want to add logic here to INSERT a new record if rs.next() is false.
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching statistics for profile " + profileId, e);
        }

        // --- Calculate the new statistics ---
        int newCorrectAnswers;
        if (isCorrect) {
            newCorrectAnswers = currentCorrectAnswers + 1;
        } else {
            newCorrectAnswers = currentCorrectAnswers;
        }

        int newAnswered = currentAnswered + 1;

        // Ensure floating-point division for accurate percentage
        double newAccuracy = (double) newCorrectAnswers / newAnswered;

        // FIX 2: Use a PreparedStatement with '?' placeholders to prevent SQL Injection.
        String updateSql = """
      UPDATE statistics SET correct_answers = ?, answered = ?, accuracy = ? WHERE profile_id = ?
    """;

        try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
            // Bind the new values safely to the '?' placeholders
            updatePs.setInt(1, newCorrectAnswers);
            updatePs.setInt(2, newAnswered);
            updatePs.setDouble(3, newAccuracy);
            updatePs.setLong(4, profileId);

            // Use executeUpdate() for INSERT, UPDATE, or DELETE statements
            updatePs.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error updating statistics for profile " + profileId, e);
        }

        return profileId;
    }

    public int updateHighScore(long profileId, int newScore) {

        int currentHighScore = 0;

        // 1. Get the current high score from the database
        String getScoreSQL = "SELECT highscore FROM statistics WHERE profile_id = ?";

        try (PreparedStatement selectPs = conn.prepareStatement(getScoreSQL)) {
            selectPs.setLong(1, profileId);

            try (ResultSet rs = selectPs.executeQuery()) {
                if (rs.next()) {
                    currentHighScore = rs.getInt("highscore");
                }
                // If the profile has no stats row yet, currentHighScore will remain 0.
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching high score for profile " + profileId, e);
        }

        if (newScore > currentHighScore) {
            String updateScoreSQL = "UPDATE statistics SET highscore = ? WHERE profile_id = ?";

            try (PreparedStatement updatePs = conn.prepareStatement(updateScoreSQL)) {
                updatePs.setInt(1, newScore);
                updatePs.setLong(2, profileId);
                updatePs.executeUpdate();

                // The new score is now the official high score
                return newScore;

            } catch (SQLException e) {
                throw new RuntimeException("Error updating high score for profile " + profileId, e);
            }
        }

        // If the new score was not higher, just return the existing high score
        return currentHighScore;
    }

    public int getHighScore(long profileId) {
        String sql = "SELECT highscore FROM statistics WHERE profile_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, profileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Return the high score if the record exists
                    return rs.getInt("highscore");
                }
            }
        } catch (SQLException e) {
            // Wrap and re-throw the exception for better error handling upstream
            throw new RuntimeException("Error fetching high score for profile " + profileId, e);
        }
        // Return 0 if no high score record is found for the profile
        return 0;
    }

    public long countUsers() throws SQLException { return scalarLong("SELECT COUNT(*) FROM profiles"); }

    @Override public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) conn.close();
    }

    // Internals

    private void connect(boolean dev) throws SQLException {
        try { Class.forName("org.sqlite.JDBC"); }
        catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found; check your pom.xml dependency.", e);
        }
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA journal_mode = WAL");
        }
    }

    private void connect() throws SQLException {
        try { Class.forName("org.sqlite.JDBC"); }
        catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found; check your pom.xml dependency.", e);
        }
        conn = DriverManager.getConnection(DB_URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA journal_mode = WAL");
        }
    }

    /** Full schema with VARCHAR (per your teammate’s review) + is_correct flag in statistics. */
    private void initSchema() throws SQLException {
        String[] ddl = {
                "PRAGMA foreign_keys = ON",

                // profiles
                """
                CREATE TABLE IF NOT EXISTS profiles(
                  profile_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                  name            VARCHAR(100) NOT NULL,
                  email           VARCHAR(255) NOT NULL UNIQUE,
                  password_hash   VARCHAR(255) NOT NULL,
                  password_salt   VARCHAR(255) NOT NULL,
                  studentTeacher  VARCHAR(20) NOT NULL DEFAULT 'student',
                  created_at      VARCHAR(40)  NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
                );
                """,

                // focus areas
                """
                CREATE TABLE IF NOT EXISTS focus_areas(
                  focus_area_id  INTEGER PRIMARY KEY,
                  area_name      VARCHAR(50) NOT NULL UNIQUE
                );
                """,

                // profile ↔ focus areas (many-to-many)
                """
                CREATE TABLE IF NOT EXISTS profile_focus_areas(
                  profile_id     INTEGER NOT NULL,
                  focus_area_id  INTEGER NOT NULL,
                  PRIMARY KEY (profile_id, focus_area_id),
                  FOREIGN KEY (profile_id)    REFERENCES profiles(profile_id)        ON DELETE CASCADE,
                  FOREIGN KEY (focus_area_id) REFERENCES focus_areas(focus_area_id)  ON DELETE CASCADE
                );
                """,

                // sessions
                """
                CREATE TABLE IF NOT EXISTS sessions(
                  session_id          INTEGER PRIMARY KEY AUTOINCREMENT,
                  profile_id          INTEGER NOT NULL,
                  started_at          VARCHAR(40) NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
                  FOREIGN KEY (profile_id)          REFERENCES profiles(profile_id)           ON DELETE CASCADE
                );
                """,
                "CREATE INDEX IF NOT EXISTS idx_sessions_profile ON sessions(profile_id);",

                // statistics (now includes is_correct flag)
                """
                CREATE TABLE IF NOT EXISTS statistics(
                  statistics_id  INTEGER PRIMARY KEY AUTOINCREMENT,
                  profile_id     INTEGER NOT NULL,
                  correct_answers INTEGER NOT NULL,
                  answered       INTEGER NOT NULL,
                  highscore      INTEGER NOT NULL,
                  accuracy       REAL,
                  FOREIGN KEY (profile_id)  REFERENCES profiles(profile_id)          ON DELETE CASCADE
                );
                """,
                "CREATE INDEX IF NOT EXISTS idx_stats_profile  ON statistics(profile_id);",
        };

        try (Statement st = conn.createStatement()) {
            for (String sql : ddl) st.execute(sql);
        }
        try (Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE profiles ADD COLUMN phone VARCHAR(30)");
        } catch (SQLException ignore) {
            // will throw "duplicate column name" after the first run — that's fine
        }

        // Seed focus areas (idempotent)
        String seed = """
            INSERT OR IGNORE INTO focus_areas(focus_area_id, area_name) VALUES
              (1,'Electrical'),(2,'Dynamics'),(3,'Calculus'),(4,'Physics'),
              (5,'Mechanical'),(6,'Probability'),(7,'Other');
        """;
        try (Statement st = conn.createStatement()) {
            st.execute(seed);
        }
    }

    private long scalarLong(String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    private static void requireNonBlank(String s, String field) {
        if (s == null || s.trim().isEmpty())
            throw new IllegalArgumentException(field + " cannot be blank");
    }

    private static String sanitizeFocus(String focus) {
        if (focus == null || focus.isBlank()) return "Other";
        String f = capitalize(focus.trim());
        for (String allowed : ALLOWED_FOCUS) if (allowed.equalsIgnoreCase(f)) return allowed;
        return "Other";
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /** Returns [saltB64, "pbkdf2:ITER:hashB64"] */
    private static String[] hashPassword(String password) throws Exception {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);

        byte[] hash = pbkdf2(password.toCharArray(), salt, KDF_ITER, KEY_BITS);
        return new String[] {
                Base64.getEncoder().encodeToString(salt),
                "pbkdf2:" + KDF_ITER + ":" + Base64.getEncoder().encodeToString(hash)
        };
    }

    private static boolean verifyPassword(String password, String saltB64, String stored) throws Exception {
        if (stored == null || !stored.startsWith("pbkdf2:")) return false;
        String[] parts = stored.split(":");
        if (parts.length != 3) return false;
        int iter = Integer.parseInt(parts[1]);
        byte[] expected = Base64.getDecoder().decode(parts[2]);
        byte[] salt = Base64.getDecoder().decode(saltB64);
        byte[] actual = pbkdf2(password.toCharArray(), salt, iter, expected.length * 8);
        return constantTimeEquals(expected, actual);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iter, int keyBits) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iter, keyBits);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(KDF_ALGO);
        return skf.generateSecret(spec).getEncoded();
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int res = 0;
        for (int i = 0; i < a.length; i++) res |= a[i] ^ b[i];
        return res == 0;
    }

    // helpers

    /** Ensure a focus area exists and return its id. */
    private int ensureFocusExists(String areaName) throws SQLException {
        Integer id = findFocusAreaId(areaName);
        if (id != null) return id;

        String sql = "INSERT INTO focus_areas(area_name) VALUES(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, areaName);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        // fallback (unlikely): try to read again
        Integer retry = findFocusAreaId(areaName);
        if (retry != null) return retry;
        throw new SQLException("Could not create focus area: " + areaName);
    }

    private Integer findFocusAreaId(String areaName) throws SQLException {
        String sql = "SELECT focus_area_id FROM focus_areas WHERE area_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, areaName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return null;
    }

    // DTOs

    public record User(long id, String name, String email, String focusArea, String createdAt) {}
//    public record Question(long id, String focusArea, String question, String answer, String reference, String createdAt) {}

    // Quick manual test

    public static void main(String[] args) throws Exception {
        try (Backend db = new Backend()) {
            if (db.countUsers() == 0) {
                db.addUser("alice", "alice@gmail.com","ChangeMe123", "Calculus");
                db.addUser("bob", "bob@gmail.com", "StrongPass456", "Electrical");
            }
            System.out.println("Auth alice: " + db.authenticate("alice@gmail.com", "ChangeMe123"));
        }
    }
}
