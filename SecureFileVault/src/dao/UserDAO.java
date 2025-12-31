package dao;

import db.DbConnection;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

	public boolean insert(User user) {
		String sql = "INSERT INTO users (FirstName, LastName, Email, Password, role, vault_salt, vault_iv, vault_key_enc, vault_iter) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

		try (Connection conn = DbConnection.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, user.getFirstName());
			ps.setString(2, user.getLastName());
			ps.setString(3, user.getEmail());
			ps.setString(4, user.getPassword());
			ps.setString(5, user.getRole());
			ps.setBytes(6, user.getVaultSalt());
			ps.setBytes(7, user.getVaultIv());
			ps.setBytes(8, user.getVaultKeyEnc());
			ps.setInt(9, user.getVaultIter());

			ps.executeUpdate();
			return true;

		} catch (SQLException e) {
			if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
				throw new RuntimeException("Email already exists");
			}
			throw new RuntimeException("DB insert failed");
		}
	}

	public boolean adminExists() {
		String sql = "SELECT COUNT(*) FROM users WHERE Email = 'admin@gmail.com'";
		try (Connection conn = DbConnection.connect();
				Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery(sql)) {
			return rs.next() && rs.getInt(1) > 0;
		} catch (SQLException e) {
			return false;
		}
	}

	public User findByEmail(String email) {
		String sql = "SELECT FirstName, LastName, Email, Password, role, vault_salt, vault_iv, vault_key_enc, vault_iter "
				+ "FROM users WHERE Email = ?";

		try (Connection conn = DbConnection.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, email);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return new User(rs.getString("FirstName"), rs.getString("LastName"), rs.getString("Email"),
							rs.getString("Password"), rs.getString("role"), rs.getBytes("vault_salt"),
							rs.getBytes("vault_iv"), rs.getBytes("vault_key_enc"), rs.getInt("vault_iter"));
				}
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}

	//  Save vault metadata (Option C)
	public int updateVaultMeta(String email, byte[] salt, byte[] iv, byte[] keyEnc, int iter) {
		String sql = "UPDATE users SET vault_salt=?, vault_iv=?, vault_key_enc=?, vault_iter=? WHERE Email=?";
		try (Connection conn = DbConnection.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setBytes(1, salt);
			ps.setBytes(2, iv);
			ps.setBytes(3, keyEnc);
			ps.setInt(4, iter);
			ps.setString(5, email);

			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("Vault meta update failed");
		}
	}

	public int updatePasswordByEmail(String email, String newHashedPassword) {
		String sql = "UPDATE users SET Password = ? WHERE Email = ?";
		try (Connection conn = DbConnection.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, newHashedPassword);
			ps.setString(2, email);
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("Password update failed");
		}
	}

	//  FIX: use the (fn,ln,email,password,role) constructor
	public List<User> findAll() {
		List<User> list = new ArrayList<>();
		String sql = "SELECT FirstName, LastName, Email, role FROM users";

		try (Connection conn = DbConnection.connect();
				Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery(sql)) {

			while (rs.next()) {
				list.add(new User(rs.getString("FirstName"), rs.getString("LastName"), rs.getString("Email"), null,
						rs.getString("role")));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Load failed");
		}
		return list;
	}

	public int deleteByEmail(String email) {
		String sql = "DELETE FROM users WHERE Email = ?";
		try (Connection conn = DbConnection.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, email);
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("Delete failed");
		}
	}
}
