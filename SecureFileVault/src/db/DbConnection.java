package db;

import java.sql.Connection;
import java.sql.DriverManager;

public class DbConnection {

	public static Connection connect() {
		Connection conn = null;
		try {
			String url = "jdbc:sqlite:C:/Users/User/eclipse-workspace/SecureFileVault/src/encrypted_files/JavaDatabaseTutorial.db";
			conn = DriverManager.getConnection(url);
			System.out.println("Connected to SQLite.");
		} catch (Exception e) {
			e.printStackTrace(); // IMPORTANT for debugging
		}
		return conn;
	}
}
