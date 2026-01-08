package dao;

import db.DbConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
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
	
	public java.util.List<Object[]> fetchLogs(model.User user) {
	    java.util.List<Object[]> data = new java.util.ArrayList<>();
	    
	    // 权限逻辑：admin 查全部，普通用户只能查自己
	    boolean isAdmin = "admin".equalsIgnoreCase(user.getRole());
	    String sql = isAdmin 
	        ? "SELECT id, action, target_email, performed_by, timestamp FROM audit_log ORDER BY timestamp DESC"
	        : "SELECT id, action, target_email, performed_by, timestamp FROM audit_log WHERE performed_by = ? ORDER BY timestamp DESC";

	    try (java.sql.Connection conn = db.DbConnection.connect(); 
	         java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
	        
	        if (!isAdmin) {
	            ps.setString(1, user.getEmail());
	        }

	        try (java.sql.ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	                data.add(new Object[]{ 
	                    rs.getInt("id"), 
	                    rs.getString("action"), 
	                    rs.getString("target_email"), 
	                    rs.getString("performed_by"), 
	                    rs.getString("timestamp") 
	                });
	            }
	        }
	    } catch (Exception e) {
	        System.err.println("Error fetching audit logs: " + e.getMessage());
	    }
	    return data;
	}
}
