package com.example;

import java.sql.*;

public class CachedDocument extends AbstractDecorator {
    private String gcsPath;

    public CachedDocument(Document document) {
        super(document);
        if (document instanceof SmartDocument) {
            this.gcsPath = ((SmartDocument) document).gcsPath;
        }
    }

    public CachedDocument(Document document, String gcsPath) {
        super(document);
        this.gcsPath = gcsPath;
    }

    public void setGcsPath(String gcsPath) {
        this.gcsPath = gcsPath;
    }

    @Override
    public String parse() {
        String cached = getFromCache();
        if (cached != null) {
            return cached;
        }
        String parsed = super.parse();
        saveToCache(parsed);
        return parsed;
    }

    private String getFromCache() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:cache.db")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS cache (path TEXT PRIMARY KEY, content TEXT)");
            }
            String query = "SELECT content FROM cache WHERE path = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, gcsPath);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getString("content");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void saveToCache(String content) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:cache.db")) {
            String insert = "INSERT OR REPLACE INTO cache (path, content) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insert)) {
                pstmt.setString(1, gcsPath);
                pstmt.setString(2, content);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
