package com.example;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class DecoratorTest {

    @Test
    void testTimedDocument() {
        Document document = new Document() {
            @Override
            public String parse() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return "Parsed Content";
            }
        };
        TimedDocument timedDocument = new TimedDocument(document);
        String result = timedDocument.parse();
        Assertions.assertEquals("Parsed Content", result);
    }

    @Test
    void testCachedDocument() throws SQLException {
        String path = "gs://test-bucket/test.png";
        String content = "Cached Content";

        // Setup DB
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:cache.db")) {
            try (java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS cache (path TEXT PRIMARY KEY, content TEXT)");
                stmt.execute("DELETE FROM cache WHERE path = '" + path + "'");
            }
        }

        Document document = new Document() {
            @Override
            public String parse() {
                return "New Content";
            }
        };

        // First run - should be "New Content" and saved to cache
        CachedDocument cachedDocument = new CachedDocument(document, path);
        String result1 = cachedDocument.parse();
        Assertions.assertEquals("New Content", result1);

        // Verify it is in cache
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:cache.db")) {
            String query = "SELECT content FROM cache WHERE path = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, path);
                var rs = pstmt.executeQuery();
                Assertions.assertTrue(rs.next());
                Assertions.assertEquals("New Content", rs.getString("content"));
            }
        }

        // Update DB manually to simulate cache hit with different content
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:cache.db")) {
            String update = "UPDATE cache SET content = ? WHERE path = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(update)) {
                pstmt.setString(1, content);
                pstmt.setString(2, path);
                pstmt.executeUpdate();
            }
        }

        // Second run - should be "Cached Content"
        String result2 = cachedDocument.parse();
        Assertions.assertEquals("Cached Content", result2);
        
        // Cleanup
        new File("cache.db").delete();
    }
}
