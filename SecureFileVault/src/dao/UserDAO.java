package dao;

import db.DbConnection;
import model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // =========================
    // INSERT USER (REGISTER)
    // =========================
    public boolean insert(User user) {
        String sql = "INSERT INTO users (FirstName, LastName, Email, Password, role, salt) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DbConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getFirstName());
            ps.setString(2, user.getLastName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());
            ps.setString(5, user.getRole());
            
            
            ps.setString(6, user.getVaultSalt());

            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                throw new RuntimeException("Email already exists");
            }
            throw new RuntimeException("User registration failed: " + e.getMessage());
        }
    }

    // =========================
    // CHECK ADMIN EXISTS
    // =========================
    public boolean adminExists() {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
        try (Connection conn = DbConnection.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    // =========================
    // FIND USER BY EMAIL (LOGIN) 
    // =========================
    public User findByEmail(String email) {
        // ✅ UPDATED SQL: Select 'salt' column
        String sql = "SELECT FirstName, LastName, Email, Password, role, salt " +
                     "FROM users WHERE Email = ?";

        try (Connection conn = DbConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {

                    return new User(
                        rs.getString("FirstName"),
                        rs.getString("LastName"),
                        rs.getString("Email"),
                        rs.getString("Password"),
                        rs.getString("role"),
                        rs.getString("salt") 
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*// =========================
    // UPDATE PASSWORD
    // =========================
    public int updatePasswordByEmail(String email, String newHashedPassword) {
        String sql = "UPDATE users SET Password = ? WHERE Email = ?";
        try (Connection conn = DbConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHashedPassword);
            ps.setString(2, email);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Password update failed");
        }
    }*/

    // =========================
    // LOAD ALL USERS (ADMIN VIEW)
    // =========================
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT FirstName, LastName, Email, role FROM users";

        try (Connection conn = DbConnection.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                // For admin view, we don't need the password or salt, so we pass null
                list.add(new User(
                    rs.getString("FirstName"),
                    rs.getString("LastName"),
                    rs.getString("Email"),
                    null,
                    rs.getString("role"),
                    null 
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Load users failed");
        }
        return list;
    }

   /* // =========================
    // DELETE USER
    // =========================
    public int deleteByEmail(String email) {
        String sql = "DELETE FROM users WHERE Email = ?";
        try (Connection conn = DbConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Delete failed");
        }
    }*/
}