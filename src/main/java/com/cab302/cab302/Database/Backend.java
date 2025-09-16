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

    private Connection conn;

    public Backend() throws SQLException {
        connect();
        initSchema();
    }

    // ------------------------------------------------------------
    // Public API (kept compatible with your existing method names)
    // ------------------------------------------------------------

    /** Create a new user (stored in 'profiles'). */
    public long addUser(String username, String password, String focusArea) throws Exception {
        requireNonBlank(username, "username");
        requireNonBlank(password, "password");

        int focusId = ensureFocusExists(sanitizeFocus(focusArea));
        String[] kdf = hashPassword(password);

        // NOTE: name/email are required in schema — use sensible placeholders for now
        String name = username;
        String email = username + "@local";

        String sql = """
            INSERT INTO profiles(name, email, username, password_hash, password_salt, studentTeacher, created_at)
            VALUES(?,?,?,?,?,?,?)
        """;

        long profileId;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, username.trim());
            ps.setString(4, kdf[1]); // hash
            ps.setString(5, kdf[0]); // salt
            ps.setString(6, "student"); // default role
            ps.setString(7, Instant.now().toString());
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

        return profileId;
    }

    /** Authenticate a user by username/password against 'profiles'. */
    public boolean authenticate(String username, String password) throws Exception {
        String sql = "SELECT password_hash, password_salt FROM profiles WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                String hash = rs.getString("password_hash");
                String salt = rs.getString("password_salt");
                return verifyPassword(password, salt, hash);
            }
        }
    }

    /** Fetch a user summary (first focus area name if any). */
    public Optional<User> getUser(String username) throws SQLException {
        String sql = """
            SELECT p.profile_id, p.username, p.created_at,
                   (SELECT fa.area_name
                      FROM profile_focus_areas pfa
                      JOIN focus_areas fa ON fa.focus_area_id = pfa.focus_area_id
                     WHERE pfa.profile_id = p.profile_id
                     ORDER BY fa.area_name LIMIT 1) AS focus_area
              FROM profiles p
             WHERE p.username = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(
                            rs.getLong("profile_id"),
                            rs.getString("username"),
                            rs.getString("focus_area"),
                            rs.getString("created_at")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    /** Insert a question into 'question_library'. */
    public long addQuestion(String focusArea, String question, String answer, String reference) throws SQLException {
        requireNonBlank(question, "question");
        requireNonBlank(answer, "answer");

        int focusId = ensureFocusExists(sanitizeFocus(focusArea));

        String sql = """
            INSERT INTO question_library(question, answer, reference, focus_area_id, question_type, created_at)
            VALUES(?,?,?,?,?,?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, question.trim());
            ps.setString(2, answer.trim());
            ps.setString(3, reference == null ? null : reference.trim());
            ps.setInt(4, focusId);
            ps.setString(5, "worded"); // default type; change as needed
            ps.setString(6, Instant.now().toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Failed to insert question");
    }

    /** Get questions by focus area (randomized). If focusArea null/"Any": from all. */
    public List<Question> getQuestions(String focusArea, int limit) throws SQLException {
        boolean all = (focusArea == null) || focusArea.equalsIgnoreCase("Any");
        String base = """
            SELECT q.question_id AS id,
                   fa.area_name   AS focus_area,
                   q.question,
                   q.answer,
                   q.reference,
                   q.created_at
              FROM question_library q
              LEFT JOIN focus_areas fa ON fa.focus_area_id = q.focus_area_id
        """;
        String sql = all
                ? base + " ORDER BY RANDOM() LIMIT ?"
                : base + " WHERE fa.area_name = ? ORDER BY RANDOM() LIMIT ?";

        List<Question> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (all) {
                ps.setInt(1, Math.max(1, limit));
            } else {
                ps.setString(1, sanitizeFocus(focusArea));
                ps.setInt(2, Math.max(1, limit));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Question(
                            rs.getLong("id"),
                            rs.getString("focus_area"),
                            rs.getString("question"),
                            rs.getString("answer"),
                            rs.getString("reference"),
                            rs.getString("created_at")
                    ));
                }
            }
        }
        return out;
    }

    /** Record a quiz attempt (includes correctness flag). */
    public long recordAttempt(long profileId, long questionId, String userAnswer,
                              boolean isCorrect, double speedSeconds, double accuracy) throws SQLException {
        String sql = """
          INSERT INTO statistics(question_id, profile_id, user_answer, is_correct, speed, accuracy)
          VALUES(?,?,?,?,?,?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, questionId);
            ps.setLong(2, profileId);
            ps.setString(3, userAnswer);
            ps.setInt(4, isCorrect ? 1 : 0);
            ps.setDouble(5, speedSeconds);
            ps.setDouble(6, accuracy);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Failed to insert statistics row.");
    }

    public long countUsers() throws SQLException { return scalarLong("SELECT COUNT(*) FROM profiles"); }
    public long countQuestions() throws SQLException { return scalarLong("SELECT COUNT(*) FROM question_library"); }

    @Override public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) conn.close();
    }

    // ------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------

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
                  username        VARCHAR(50)  NOT NULL UNIQUE,
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

                // question library
                """
                CREATE TABLE IF NOT EXISTS question_library(
                  question_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                  question       TEXT NOT NULL,
                  answer         TEXT NOT NULL,
                  reference      TEXT,
                  focus_area_id  INTEGER,
                  question_type  VARCHAR(20) NOT NULL CHECK (question_type IN ('numerical','worded','object')),
                  created_at     VARCHAR(40)  NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
                  FOREIGN KEY (focus_area_id) REFERENCES focus_areas(focus_area_id) ON DELETE SET NULL
                );
                """,
                "CREATE INDEX IF NOT EXISTS idx_qlib_focus ON question_library(focus_area_id);",
                "CREATE INDEX IF NOT EXISTS idx_qlib_type  ON question_library(question_type);",

                // numerical subtype
                """
                CREATE TABLE IF NOT EXISTS numerical_questions(
                  question_id  INTEGER PRIMARY KEY,
                  question     TEXT NOT NULL,
                  answer       TEXT NOT NULL,
                  FOREIGN KEY (question_id) REFERENCES question_library(question_id) ON DELETE CASCADE
                );
                """,

                // worded subtype
                """
                CREATE TABLE IF NOT EXISTS worded_questions(
                  question_id  INTEGER PRIMARY KEY,
                  question     TEXT NOT NULL,
                  answer       TEXT NOT NULL,
                  FOREIGN KEY (question_id) REFERENCES question_library(question_id) ON DELETE CASCADE
                );
                """,

                // object subtype
                """
                CREATE TABLE IF NOT EXISTS object_questions(
                  question_id  INTEGER PRIMARY KEY,
                  question     TEXT NOT NULL,
                  answer       TEXT NOT NULL,
                  image        VARCHAR(255),
                  FOREIGN KEY (question_id) REFERENCES question_library(question_id) ON DELETE CASCADE
                );
                """,

                // sessions
                """
                CREATE TABLE IF NOT EXISTS sessions(
                  session_id          INTEGER PRIMARY KEY AUTOINCREMENT,
                  profile_id          INTEGER NOT NULL,
                  flagged_question_id INTEGER,
                  started_at          VARCHAR(40) NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
                  FOREIGN KEY (profile_id)          REFERENCES profiles(profile_id)           ON DELETE CASCADE,
                  FOREIGN KEY (flagged_question_id) REFERENCES question_library(question_id) ON DELETE SET NULL
                );
                """,
                "CREATE INDEX IF NOT EXISTS idx_sessions_profile ON sessions(profile_id);",

                // statistics (now includes is_correct flag)
                """
                CREATE TABLE IF NOT EXISTS statistics(
                  statistics_id  INTEGER PRIMARY KEY AUTOINCREMENT,
                  question_id    INTEGER NOT NULL,
                  profile_id     INTEGER NOT NULL,
                  user_answer    VARCHAR(500),
                  is_correct     INTEGER NOT NULL CHECK (is_correct IN (0,1)) DEFAULT 0,
                  speed          REAL,
                  accuracy       REAL,
                  created_at     VARCHAR(40) NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
                  FOREIGN KEY (question_id) REFERENCES question_library(question_id) ON DELETE CASCADE,
                  FOREIGN KEY (profile_id)  REFERENCES profiles(profile_id)          ON DELETE CASCADE
                );
                """,
                "CREATE INDEX IF NOT EXISTS idx_stats_profile  ON statistics(profile_id);",
                "CREATE INDEX IF NOT EXISTS idx_stats_question ON statistics(question_id);"
        };

        try (Statement st = conn.createStatement()) {
            for (String sql : ddl) st.execute(sql);
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

    // ------------- helpers -------------

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

    // ------------------------------------------------------------
    // DTOs (unchanged names for compatibility)
    // ------------------------------------------------------------

    public record User(long id, String username, String focusArea, String createdAt) {}
    public record Question(long id, String focusArea, String question, String answer, String reference, String createdAt) {}

    // ------------------------------------------------------------
    // Quick manual test
    // ------------------------------------------------------------
    public static void main(String[] args) throws Exception {
        try (Backend db = new Backend()) {
            if (db.countUsers() == 0) {
                db.addUser("alice", "ChangeMe123", "Calculus");
                db.addUser("bob", "StrongPass456", "Electrical");
            }
            if (db.countQuestions() == 0) {
                db.addQuestion("Calculus", "Differentiate f(x)=x^2", "f'(x)=2x", "Stewart Calculus, Ch2");
                db.addQuestion("Electrical", "Ohm's Law?", "V=IR", "Any EE101 text");
            }

            System.out.println("Users: " + db.countUsers() + ", Questions: " + db.countQuestions());
            System.out.println("Auth alice: " + db.authenticate("alice", "ChangeMe123"));

            List<Question> q = db.getQuestions("Any", 3);
            q.forEach(qq -> System.out.println("Q: " + qq.question() + " [" + qq.focusArea() + "]"));
        }
    }
}
