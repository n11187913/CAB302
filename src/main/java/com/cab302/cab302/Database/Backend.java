package com.cab302.cab302.Database;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;


public class Backend implements AutoCloseable {

    private static final String DB_URL = "jdbc:sqlite:cab302.db";
    private static final String[] ALLOWED_FOCUS = {
            "Electrical", "Dynamics", "Calculus", "Physics", "Mechanical", "Probability", "Other"
    };


    private static final String KDF_ALGO = "PBKDF2WithHmacSHA256";
    private static final int KDF_ITER = 120_000; // sensible default
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private Connection conn;

    public Backend() throws SQLException {
        connect();
        initSchema();
    }


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



    private void connect() throws SQLException {
        conn = DriverManager.getConnection(DB_URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA journal_mode = WAL");
        }
    }

    private void initSchema() throws SQLException {
        String createUsers = """
            CREATE TABLE IF NOT EXISTS users(
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              username TEXT NOT NULL UNIQUE,
              password_hash TEXT NOT NULL,
              password_salt TEXT NOT NULL,
              focus_area TEXT NOT NULL,
              created_at TEXT NOT NULL
            );
        """;

        String createQuestions = """
            CREATE TABLE IF NOT EXISTS questions(
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              focus_area TEXT NOT NULL,
              question TEXT NOT NULL,
              answer TEXT NOT NULL,
              reference TEXT,
              created_at TEXT NOT NULL
            );
        """;


        String createIndexQFocus = "CREATE INDEX IF NOT EXISTS idx_questions_focus ON questions(focus_area);";
        String createIndexUName = "CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON users(username);";

        try (Statement st = conn.createStatement()) {
            st.execute(createUsers);
            st.execute(createQuestions);
            st.execute(createIndexQFocus);
            st.execute(createIndexUName);
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
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }


    private static String[] hashPassword(String password) throws Exception {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);

        byte[] hash = pbkdf2(password.toCharArray(), salt, KDF_ITER, KEY_BITS);
        return new String[] {
                Base64.getEncoder().encodeToString(salt),
                // store algo:iter:hash for future-proofing
                "pbkdf2:" + KDF_ITER + ":" + Base64.getEncoder().encodeToString(hash)
        };
    }

    private static boolean verifyPassword(String password, String saltB64, String stored) throws Exception {
        if (stored == null || !stored.startsWith("pbkdf2:"))
            return false;
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



    public record User(long id, String username, String focusArea, String createdAt) {}
    public record Question(long id, String focusArea, String question, String answer, String reference, String createdAt) {}


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

