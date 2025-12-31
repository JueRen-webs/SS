package ui;

import dao.AuditLogDAO;
import dao.FileDAO;
import model.User;
import security.CryptoUtil;

import javax.crypto.SecretKey;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

public class FileVaultFrame extends JFrame {

	private final User loggedInUser;
	private final FileDAO fileDao = new FileDAO();
	private final AuditLogDAO auditDao = new AuditLogDAO();

	private JTable table;
	private DefaultTableModel tableModel;

	// 🔐 Vault key derived from LOGIN PASSWORD (session-only)
	private final SecretKey vaultKey;

	public FileVaultFrame(User user) {
		this.loggedInUser = user;

		try {
			// 🔐 Derive AES key from login password (Option C)
			byte[] salt = "STATIC_SALT_DEMO".getBytes(); // acceptable for course demo
			int iter = 120000;

			this.vaultKey = CryptoUtil.deriveKey(loggedInUser.getSessionPassword(), salt, iter);

		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Vault initialization failed", "Error", JOptionPane.ERROR_MESSAGE);
			throw new RuntimeException(e);
		}

		setTitle("Secure File Vault - " + loggedInUser.getEmail());
		setSize(800, 450);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout(10, 10));

		// ===== TABLE =====
		tableModel = new DefaultTableModel(new Object[] { "ID", "File Name", "Uploaded At" }, 0) {
			@Override
			public boolean isCellEditable(int r, int c) {
				return false;
			}
		};

		table = new JTable(tableModel);
		add(new JScrollPane(table), BorderLayout.CENTER);

		// ===== BUTTONS =====
		JButton uploadBtn = new JButton("Upload & Encrypt");
		JButton decryptBtn = new JButton("Decrypt & Save");
		JButton refreshBtn = new JButton("Refresh");
		JButton logoutBtn = new JButton("Logout");

		JPanel btnPanel = new JPanel();
		btnPanel.add(uploadBtn);
		btnPanel.add(decryptBtn);
		btnPanel.add(refreshBtn);
		btnPanel.add(logoutBtn);
		add(btnPanel, BorderLayout.SOUTH);

		uploadBtn.addActionListener(e -> uploadEncrypt());
		decryptBtn.addActionListener(e -> decryptSave());
		refreshBtn.addActionListener(e -> loadFiles());

		logoutBtn.addActionListener(e -> {
			loggedInUser.setSessionPassword(null); // clear secret
			dispose();
			new LoginFrame();
		});

		loadFiles();
		setVisible(true);
	}

	private void loadFiles() {
		tableModel.setRowCount(0);
		List<String[]> files = fileDao.findFilesByOwner(loggedInUser.getEmail());
		for (String[] f : files)
			tableModel.addRow(f);
	}

	// =============================
	// UPLOAD + ENCRYPT + DELETE
	// =============================
	private void uploadEncrypt() {

		JFileChooser chooser = new JFileChooser();
		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
			return;

		File input = chooser.getSelectedFile();

		try {
			byte[] plain = Files.readAllBytes(input.toPath());
			byte[] iv = CryptoUtil.generateIV();

			byte[] encrypted = CryptoUtil.encrypt(plain, vaultKey, iv);

			File dir = new File("encrypted_files");
			if (!dir.exists())
				dir.mkdirs();

			String stored = UUID.randomUUID() + ".enc";
			File out = new File(dir, stored);
			Files.write(out.toPath(), encrypted);

			// ✅ SECURE DELETE ORIGINAL FILE
			secureDelete(input);

			fileDao.insertFile(loggedInUser.getEmail(), input.getName(), out.getAbsolutePath(), CryptoUtil.b64(iv));

			auditDao.log("FILE_UPLOAD", input.getName(), loggedInUser.getEmail());
			auditDao.log("FILE_ENCRYPT", input.getName(), loggedInUser.getEmail());

			JOptionPane.showMessageDialog(this, "File encrypted & original removed.");

			loadFiles();

		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Upload failed", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	// =============================
	// DECRYPT + RESTORE FILENAME
	// =============================
	private void decryptSave() {

		int row = table.getSelectedRow();
		if (row == -1) {
			JOptionPane.showMessageDialog(this, "Select a file first.");
			return;
		}

		int id = Integer.parseInt(tableModel.getValueAt(table.convertRowIndexToModel(row), 0).toString());

		try {
			String[] meta = fileDao.getFileMetaById(id, loggedInUser.getEmail());

			if (meta == null) {
				JOptionPane.showMessageDialog(this, "Access denied.");
				return;
			}

			String encPath = meta[0];
			byte[] iv = CryptoUtil.fromB64(meta[1]);
			String originalName = meta[2];

			JFileChooser chooser = new JFileChooser();
			chooser.setSelectedFile(new File(originalName));

			if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
				return;

			File saveAs = chooser.getSelectedFile();

			byte[] encrypted = Files.readAllBytes(new File(encPath).toPath());

			byte[] plain = CryptoUtil.decrypt(encrypted, vaultKey, iv);

			Files.write(saveAs.toPath(), plain);

			auditDao.log("FILE_DECRYPT", originalName, loggedInUser.getEmail());
			auditDao.log("FILE_DOWNLOAD", originalName, loggedInUser.getEmail());

			JOptionPane.showMessageDialog(this, "File decrypted & restored.");

		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Decryption failed", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	// =============================
	// BEST-EFFORT SECURE DELETE
	// =============================
	private void secureDelete(File file) {
		try {
			long len = file.length();
			try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
				raf.seek(0);
				raf.write(new byte[(int) len]);
			}
			Files.deleteIfExists(file.toPath());
		} catch (Exception ignored) {
			try {
				Files.deleteIfExists(file.toPath());
			} catch (Exception e) {
			}
		}
	}
}
