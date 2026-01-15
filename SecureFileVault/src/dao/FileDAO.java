package dao;

import db.DbConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FileDAO {

	// =========================
	// INSERT FILE
	// =========================
	public void insertFile(String ownerEmail, String originalName, String encryptedPath, String ivBase64) {

		String sql = "INSERT INTO files " + "(owner_email, original_name, encrypted_path, iv, uploaded_at) "
				+ "VALUES (?, ?, ?, ?, datetime('now','localtime'))";

		try (Connection conn = DbConnection.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, ownerEmail);
			ps.setString(2, originalName);
			ps.setString(3, encryptedPath);
			ps.setString(4, ivBase64);

			ps.executeUpdate();

		} catch (Exception e) {
			throw new RuntimeException("Upload failed", e);
		}
	}

	// =========================
	// LIST FILES BY OWNER
	// =========================
	public List<String[]> findFilesByOwner(String ownerEmail) {

		List<String[]> list = new ArrayList<>();

		String sql = "SELECT id, original_name, uploaded_at " + "FROM files WHERE owner_email = ? "
				+ "ORDER BY uploaded_at DESC";

		try (Connection conn = DbConnection.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, ownerEmail);
			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				list.add(new String[] { rs.getString("id"), rs.getString("original_name"),
						rs.getString("uploaded_at") });
			}

		} catch (SQLException e) {
			throw new RuntimeException("File list load failed", e);
		}

		return list;
	}

	// =========================
	// GET FILE METADATA
	// =========================
	public String[] getFileMetaById(int id, String ownerEmail) {

		String sql = "SELECT encrypted_path, iv, original_name " + "FROM files WHERE id = ? AND owner_email = ?";

		try (Connection conn = DbConnection.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, id);
			ps.setString(2, ownerEmail);

			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return new String[] { rs.getString("encrypted_path"), rs.getString("iv"),
						rs.getString("original_name") };
			}

		} catch (SQLException e) {
			throw new RuntimeException("File metadata read failed", e);
		}

		return null;
	}
}
