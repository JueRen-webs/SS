package ui;

import dao.UserDAO;
import dao.AuditLogDAO;
import model.User;

import java.io.File;
import java.io.PrintWriter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.List;

public class UserManagementFrame extends JFrame {

	private JTable table;
	private DefaultTableModel tableModel;
	private JTextField searchField;
	private TableRowSorter<DefaultTableModel> sorter;

	private final UserDAO dao = new UserDAO();
	private final AuditLogDAO auditDao = new AuditLogDAO();

	// Only admin can access this screen (validated in LoginFrame)
	public UserManagementFrame() {

		setTitle("User Management");
		setSize(700, 450);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout(10, 10));

		// ===== TABLE MODEL (READ-ONLY) =====
		tableModel = new DefaultTableModel(new Object[] { "First Name", "Last Name", "Email", "Role" }, 0) {

			@Override
			public boolean isCellEditable(int row, int column) {
				return false; // 🔒 disable editing
			}
		};

		table = new JTable(tableModel);

		// ===== SORTER (SORT + FILTER) =====
		sorter = new TableRowSorter<>(tableModel);
		table.setRowSorter(sorter);

		// ===== SEARCH PANEL =====
		JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
		searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

		searchPanel.add(new JLabel("Search:"), BorderLayout.WEST);

		searchField = new JTextField();
		searchPanel.add(searchField, BorderLayout.CENTER);

		add(searchPanel, BorderLayout.NORTH);

		// ===== TABLE =====
		add(new JScrollPane(table), BorderLayout.CENTER);

		// ===== BUTTONS =====
		JButton refreshBtn = new JButton("Refresh");
		JButton addBtn = new JButton("Add User");
		JButton deleteBtn = new JButton("Delete Selected");
		JButton changePwBtn = new JButton("Change Password");
		JButton exportBtn = new JButton("Export CSV");
		JButton logoutBtn = new JButton("Logout");

		JPanel btnPanel = new JPanel();
		btnPanel.add(refreshBtn);
		btnPanel.add(addBtn);
		btnPanel.add(deleteBtn);
		btnPanel.add(changePwBtn);
		btnPanel.add(exportBtn);
		btnPanel.add(logoutBtn);

		add(btnPanel, BorderLayout.SOUTH);

		// ===== ACTIONS =====
		refreshBtn.addActionListener(e -> loadUsers());

		addBtn.addActionListener(e -> new RegisterFrame(this));

		deleteBtn.addActionListener(e -> deleteSelected());

		changePwBtn.addActionListener(e -> new ChangePasswordFrame("admin@gmail.com"));

		exportBtn.addActionListener(e -> exportToCSV());

		logoutBtn.addActionListener(e -> {
			int confirm = JOptionPane.showConfirmDialog(this, "Logout now?", "Confirm", JOptionPane.YES_NO_OPTION);
			if (confirm == JOptionPane.YES_OPTION) {
				dispose();
				new LoginFrame();
			}
		});

		// ===== LIVE SEARCH FILTER =====
		searchField.getDocument().addDocumentListener(new DocumentListener() {

			private void filter() {
				String text = searchField.getText();

				if (text.trim().isEmpty()) {
					sorter.setRowFilter(null); // show all
				} else {
					sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
				}
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				filter();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				filter();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				filter();
			}
		});

		loadUsers();
		setVisible(true);
	}

	// ===== LOAD USERS =====
	public void loadUsers() {
		tableModel.setRowCount(0);
		List<User> users = dao.findAll();

		for (User u : users) {
			tableModel.addRow(new Object[] { u.getFirstName(), u.getLastName(), u.getEmail(), u.getRole() });
		}
	}

	// ===== DELETE USER =====
	private void deleteSelected() {

		int viewRow = table.getSelectedRow();
		if (viewRow == -1) {
			JOptionPane.showMessageDialog(this, "Please select a user first.");
			return;
		}

		int modelRow = table.convertRowIndexToModel(viewRow);
		String email = tableModel.getValueAt(modelRow, 2).toString();

		int confirm = JOptionPane.showConfirmDialog(this, "Delete user " + email + "?", "Confirm",
				JOptionPane.YES_NO_OPTION);

		if (confirm != JOptionPane.YES_OPTION)
			return;

		int rows = dao.deleteByEmail(email);
		if (rows == 1) {
			auditDao.log("DELETE_USER", email, "admin@gmail.com");
			JOptionPane.showMessageDialog(this, "Deleted successfully.");
			loadUsers();
		} else {
			JOptionPane.showMessageDialog(this, "User not found.");
		}
	}

	// exportToCSV()
	private void exportToCSV() {

		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Save CSV File");

		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		File file = chooser.getSelectedFile();

		// ensure .csv extension
		if (!file.getName().toLowerCase().endsWith(".csv")) {
			file = new File(file.getAbsolutePath() + ".csv");
		}

		try (PrintWriter pw = new PrintWriter(file)) {

			// CSV header
			pw.println("First Name,Last Name,Email");

			// export ONLY visible rows (filtered ones)
			for (int i = 0; i < table.getRowCount(); i++) {

				int modelRow = table.convertRowIndexToModel(i);

				String fn = tableModel.getValueAt(modelRow, 0).toString();
				String ln = tableModel.getValueAt(modelRow, 1).toString();
				String email = tableModel.getValueAt(modelRow, 2).toString();

				pw.println(fn + "," + ln + "," + email);
			}

			JOptionPane.showMessageDialog(this, "Export successful:\n" + file.getAbsolutePath());

		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Export failed.", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

}
