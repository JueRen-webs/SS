package db;

import java.sql.Connection;
import java.sql.DriverManager;

public class DbConnection {

	public static Connection connect() {
		Connection conn = null;
		try {
			String dbPath = System.getProperty("user.dir") + "/src/encrypted_files/JavaDatabaseTutorial.db"; 
	        String url = "jdbc:sqlite:" + dbPath;
			conn = DriverManager.getConnection(url);
			System.out.println("Connected to SQLite.");
		} catch (Exception e) {
			e.printStackTrace(); // IMPORTANT for debugging
		}
		return conn;
	}
}
