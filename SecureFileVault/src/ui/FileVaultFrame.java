package ui;

import dao.AuditLogDAO;
import dao.FileDAO;
import model.User;
import security.CryptoUtil;

import javax.crypto.SecretKey;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
    private final SecretKey vaultKey;

    public FileVaultFrame(User user) {
        this.loggedInUser = user;

        try {
            // ✅ CRITICAL FIX: Use DB salt if available, fallback for legacy users
            byte[] salt;
            String dbSalt = loggedInUser.getVaultSalt();
            
            if (dbSalt != null && !dbSalt.isEmpty()) {
                salt = CryptoUtil.fromB64(dbSalt);
            } else {
                salt = "STATIC_SALT_DEMO".getBytes(); // Fallback for old accounts
            }

            int iter = 600000;
            this.vaultKey = CryptoUtil.deriveKey(loggedInUser.getSessionPassword(), salt, iter);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Vault initialization failed", "Error", JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException(e);
        }

        setTitle("Secure Vault - " + loggedInUser.getEmail());
        setSize(950, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- Header ---
        JPanel headerPanel = new JPanel(new GridBagLayout());
        headerPanel.setBackground(new Color(44, 62, 80));
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        JLabel headerLabel = new JLabel("SECURE FILE VAULT");
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setFont(new Font("Monospaced", Font.BOLD, 22));
        headerPanel.add(headerLabel); 
        add(headerPanel, BorderLayout.NORTH);

        // --- Table ---
        tableModel = new DefaultTableModel(new Object[] { "ID", "File Name", "Uploaded At" }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        table.getTableHeader().setBackground(new Color(153, 153, 153));
        table.getTableHeader().setForeground(Color.WHITE);
        
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                ((JLabel) comp).setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
                return comp;
            }
        });

        table.setRowHeight(35);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.setGridColor(new Color(230, 230, 230));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25)); 
        scrollPane.getViewport().setBackground(Color.WHITE); 
        add(scrollPane, BorderLayout.CENTER); 

        // --- Buttons ---
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 20));
        
        JButton uploadBtn = new JButton("Encrypt and Upload");
        JButton decryptBtn = new JButton("Decrypt and Save");
        JButton refreshBtn = new JButton("Refresh");
        JButton auditBtn = new JButton("View Audit Logs");
        JButton logoutBtn = new JButton("Logout");

        styleButton(uploadBtn, new Color(46, 204, 113));
        styleButton(decryptBtn, new Color(52, 152, 219));
        styleButton(refreshBtn, new Color(149, 165, 166));
        styleButton(auditBtn, new Color(155, 89, 182));
        styleButton(logoutBtn, new Color(231, 76, 60));

        btnPanel.add(uploadBtn);
        btnPanel.add(decryptBtn);
        btnPanel.add(refreshBtn);
        btnPanel.add(auditBtn);
        btnPanel.add(logoutBtn);
        add(btnPanel, BorderLayout.SOUTH);

        // Listeners
        uploadBtn.addActionListener(e -> uploadEncrypt());
        decryptBtn.addActionListener(e -> decryptSave());
        refreshBtn.addActionListener(e -> loadFiles());
        auditBtn.addActionListener(e -> showAuditDialog());
        logoutBtn.addActionListener(e -> performLogout("USER_LOGOUT"));

        this.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { performLogout("WINDOW_CLOSE_LOGOUT"); }
        });

        loadFiles();
        setVisible(true);
    }

    private void styleButton(JButton btn, Color bgColor) {
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(165, 35));
    }

    private void performLogout(String actionType) {
        auditDao.log(actionType, "N/A", loggedInUser.getEmail());
        loggedInUser.setSessionPassword(null); 
        dispose();
        new LoginFrame();
    }

    private void loadFiles() {
        tableModel.setRowCount(0);
        List<String[]> files = fileDao.findFilesByOwner(loggedInUser.getEmail());
        for (String[] f : files) tableModel.addRow(f);
    }

    private void uploadEncrypt() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File input = chooser.getSelectedFile();
        try {
            byte[] plain = Files.readAllBytes(input.toPath());
            byte[] iv = CryptoUtil.generateIV();
            byte[] encrypted = CryptoUtil.encrypt(plain, vaultKey, iv);
            File dir = new File("encrypted_files");
            if (!dir.exists()) dir.mkdirs();
            String stored = UUID.randomUUID() + ".enc";
            File out = new File(dir, stored);
            Files.write(out.toPath(), encrypted);
            secureDelete(input);
            fileDao.insertFile(loggedInUser.getEmail(), input.getName(), out.getAbsolutePath(), CryptoUtil.b64(iv));
            auditDao.log("FILE_UPLOAD", input.getName(), loggedInUser.getEmail());
            loadFiles();
            JOptionPane.showMessageDialog(this, "File encrypted & stored.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Upload failed");
        }
    }

    private void decryptSave() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a file first.");
            return;
        }
        int id = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        try {
            String[] meta = fileDao.getFileMetaById(id, loggedInUser.getEmail());
            if (meta == null) return;
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File(meta[2]));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                byte[] encrypted = Files.readAllBytes(new File(meta[0]).toPath());
                byte[] plain = CryptoUtil.decrypt(encrypted, vaultKey, CryptoUtil.fromB64(meta[1]));
                Files.write(chooser.getSelectedFile().toPath(), plain);
                auditDao.log("FILE_DECRYPT", meta[2], loggedInUser.getEmail());
                JOptionPane.showMessageDialog(this, "File decrypted & restored.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Decryption failed");
        }
    }

    private void secureDelete(File file) {
        try {
            long len = file.length();
            try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                raf.seek(0);
                raf.write(new byte[(int) len]);
            }
            Files.deleteIfExists(file.toPath());
        } catch (Exception ignored) {}
    }

    private void showAuditDialog() {
        JDialog dialog = new JDialog(this, "System Audit Logs", true);
        dialog.setSize(700, 450);
        dialog.setLocationRelativeTo(this);
        String[] cols = {"ID", "Action", "Target", "User", "Timestamp"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        JTable logTable = new JTable(model);
        List<Object[]> logs = auditDao.fetchLogs(loggedInUser);
        for (Object[] row : logs) model.addRow(row);
        dialog.add(new JScrollPane(logTable));
        dialog.setVisible(true);
    }
}