package com.cab302.cab302.Database;

import java.security.SecureRandom;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Backend implements AutoCloseable {

    // DB file created in the working directory
    private static final String DB_URL = "jdbc:sqlite:cab302.db";

    // Allowed focus areas (simple whitelist)
    private static final String[] ALLOWED_FOCUS = {
            "Electrical", "Dynamics", "Calculus", "Physics", "Mechanical", "Probability", "Other"
    };

    // PBKDF2 params (Java 21 / Corretto 21 supports this out of the box)
    private static final String KDF_ALGO = "PBKDF2WithHmacSHA256";
    private static final int KDF_ITER = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private Connection conn;

    public Backend() throws SQLException {
        connect();
        initSchema();
    }

    // ----------------------- Public API -----------------------

    /** Create a new user (throws if username exists). */
    public long addUser(String username, String password, String focusArea) throws Exception {
        requireNonBlank(username, "username");
        requireNonBlank(password, "password");
        focusArea = sanitizeFocus(focusArea);

        String[] kdf = hashPassword(password);
        String sql = """
                INSERT INTO users(username, password_hash, password_salt, focus_area, created_at)
                VALUES(?,?,?,?,?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username.trim());
            ps.setString(2, kdf[1]); // hash
            ps.setString(3, kdf[0]); // salt
            ps.setString(4, focusArea);
            ps.setString(5, Instant.now().toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Failed to insert user");
    }

    /** Authenticate a user by username/password. */
    public boolean authenticate(String username, String password) throws Exception {
        String sql = "SELECT password_hash, password_salt FROM users WHERE username = ?";
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

    /** Fetch a user (without password fields). */
    public Optional<User> getUser(String username) throws SQLException {
        String sql = """
                SELECT id, username, focus_area, created_at
                FROM users
                WHERE username = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("focus_area"),
                            rs.getString("created_at")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    /** Insert a question. */
    public long addQuestion(String focusArea, String question, String answer, String reference) throws SQLException {
        requireNonBlank(question, "question");
        requireNonBlank(answer, "answer");
        focusArea = sanitizeFocus(focusArea);

        String sql = """
                INSERT INTO questions(focus_area, question, answer, reference, created_at)
                VALUES(?,?,?,?,?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, focusArea);
            ps.setString(2, question.trim());
            ps.setString(3, answer.trim());
            ps.setString(4, reference == null ? null : reference.trim());
            ps.setString(5, Instant.now().toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Failed to insert question");
    }

    /** Get questions by focus area (randomized), limited. If focusArea is null/"Any", returns from all. */
    public List<Question> getQuestions(String focusArea, int limit) throws SQLException {
        String base = """
                SELECT id, focus_area, question, answer, reference, created_at
                FROM questions
                """;
        boolean all = (focusArea == null) || focusArea.equalsIgnoreCase("Any");
        String sql = all ? base + " ORDER BY RANDOM() LIMIT ?" :
                base + " WHERE focus_area = ? ORDER BY RANDOM() LIMIT ?";

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

    public long countUsers() throws SQLException { return scalarLong("SELECT COUNT(*) FROM users"); }
    public long countQuestions() throws SQLException { return scalarLong("SELECT COUNT(*) FROM questions"); }

    @Override public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) conn.close();
    }

    // ----------------------- Internals -----------------------

    private void connect() throws SQLException {
        // Ensure driver is registered (usually automatic with xerial, but harmless)
        try { Class.forName("org.sqlite.JDBC"); } catch (ClassNotFoundException ignored) {}

        conn = DriverManager.getConnection(DB_URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA journal_mode = WAL");
        }
    }

    private void initSchema() throws SQLException {
        String[] ddl = {
                "PRAGMA foreign_keys = ON",

                // profiles
                """
        CREATE TABLE IF NOT EXISTS profiles(
          profile_id       INTEGER PRIMARY KEY AUTOINCREMENT,
          name             TEXT NOT NULL,
          email            TEXT NOT NULL UNIQUE,
          username         TEXT NOT NULL UNIQUE,
          password_hash    TEXT NOT NULL,
          password_salt    TEXT NOT NULL,
          student_teacher  TEXT NOT NULL CHECK (student_teacher IN ('student','teacher')),
          created_at       TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
        );
        """,

                // focus areas
                """
        CREATE TABLE IF NOT EXISTS focus_areas(
          focus_area_id  INTEGER PRIMARY KEY,
          area_name      TEXT NOT NULL UNIQUE
        );
        """,

                // profile ↔ focus areas (many-to-many)
                """
        CREATE TABLE IF NOT EXISTS profile_focus_areas(
          profile_id     INTEGER NOT NULL,
          focus_area_id  INTEGER NOT NULL,
          PRIMARY KEY (profile_id, focus_area_id),
          FOREIGN KEY (profile_id)    REFERENCES profiles(profile_id)    ON DELETE CASCADE,
          FOREIGN KEY (focus_area_id) REFERENCES focus_areas(focus_area_id) ON DELETE CASCADE
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
          question_type  TEXT NOT NULL CHECK (question_type IN ('numerical','worded','object')),
          created_at     TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
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
          image        TEXT,
          FOREIGN KEY (question_id) REFERENCES question_library(question_id) ON DELETE CASCADE
        );
        """,

                // sessions
                """
        CREATE TABLE IF NOT EXISTS sessions(
          session_id          INTEGER PRIMARY KEY AUTOINCREMENT,
          profile_id          INTEGER NOT NULL,
          flagged_question_id INTEGER,
          started_at          TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
          FOREIGN KEY (profile_id)          REFERENCES profiles(profile_id)           ON DELETE CASCADE,
          FOREIGN KEY (flagged_question_id) REFERENCES question_library(question_id) ON DELETE SET NULL
        );
        """,
                "CREATE INDEX IF NOT EXISTS idx_sessions_profile ON sessions(profile_id);",

                // statistics
                """
        CREATE TABLE IF NOT EXISTS statistics(
          statistics_id  INTEGER PRIMARY KEY AUTOINCREMENT,
          question_id    INTEGER NOT NULL,
          profile_id     INTEGER NOT NULL,
          user_answer    TEXT,
          speed          REAL,
          accuracy       REAL,
          created_at     TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
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

    // ----------------------- DTOs -----------------------

    public record User(long id, String username, String focusArea, String createdAt) {}
    public record Question(long id, String focusArea, String question, String answer, String reference, String createdAt) {}

    // ----------------------- Quick manual test -----------------------
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
