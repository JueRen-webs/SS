package dao;

import db.DbConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class AuditLogDAO {

	/**
	 * Logs security-relevant actions. Audit logging must NEVER interrupt main
	 * system flow.
	 */
	public void log(String action, String targetEmail, String performedBy) {

		String sql = "INSERT INTO audit_log (action, target_email, performed_by, timestamp) "
				+ "VALUES (?, ?, ?, datetime('now','localtime'))";

		try (Connection conn = DbConnection.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, action);
			ps.setString(2, targetEmail);
			ps.setString(3, performedBy);

			ps.executeUpdate();

		} catch (Exception e) {
			// Silent fail by design
			System.err.println("Audit log failed: " + e.getMessage());
		}
	}
}
