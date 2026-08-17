package com.owo.dao;

import com.owo.entity.Akun;
import com.owo.utils.DBHelper;
import com.owo.utils.PasswordUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class AkunDAO {
    private static PasswordUtil passwordUtil = new PasswordUtil();

    public static Akun createAkun(String nama, String email, String password) throws SQLException {
        String sql = "INSERT INTO akun (nama, email, hashed_password) VALUES (?, ?, ?)";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            // Hash once and reuse, so the returned entity carries the same hash as the row.
            String hashedPassword = passwordUtil.hashPassword(password);

            pstmt.setString(1, nama);
            pstmt.setString(2, email);
            pstmt.setString(3, hashedPassword);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating akun failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    return Akun.fromHashedPassword(id, nama, email, hashedPassword);
                } else {
                    throw new SQLException("Creating akun failed, no ID obtained.");
                }
            }
        }
    }

    public static Akun getAkunByID(int id) throws SQLException {
        String sql = "SELECT * FROM akun WHERE id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String nama = rs.getString("nama");
                    String email = rs.getString("email");
                    String hashedPassword = rs.getString("hashed_password");
                    return Akun.fromHashedPassword(id, nama, email, hashedPassword);
                }
            }
        }
        return null;
    }

    public static Akun getAkunByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM akun WHERE email = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String nama = rs.getString("nama");
                    String hashedPassword = rs.getString("hashed_password");
                    return Akun.fromHashedPassword(id, nama, email, hashedPassword);
                }
            }
        }
        return null;
    }

    public static void updateAkun(Akun akun) throws SQLException {
        String sql = "UPDATE akun SET nama = ?, email = ?, hashed_password = ? WHERE id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, akun.getNama());
            pstmt.setString(2, akun.getEmail());
            pstmt.setString(3, akun.getHashedPassword());
            pstmt.setInt(4, akun.getID());
            
            pstmt.executeUpdate();
        }
    }
} 