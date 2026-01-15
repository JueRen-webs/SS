package ui;

import dao.AuditLogDAO;
import dao.UserDAO;
import model.User;
import security.CryptoUtil; // ✅ Added Import
import security.PasswordUtil;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Arrays;

public class RegisterFrame extends JFrame {

    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField emailField;
    private JPasswordField passwordField;

    // Password Checklist Components
    private JPanel checklistPanel;
    private JLabel checkLength, checkUpper, checkLower, checkNumber, checkSpecial;

    private final UserDAO userDao = new UserDAO();
    private final AuditLogDAO auditDao = new AuditLogDAO();
    private final String role;

    // Prompts
    private static final String PROMPT_FIRST = "e.g. John";
    private static final String PROMPT_LAST  = "e.g. Doe";
    private static final String PROMPT_EMAIL = "e.g. john@example.com";
    private static final String PROMPT_PASS  = "Enter your password";

    // Regex
    private static final String REGEX_EMAIL   = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    private static final String REGEX_UPPER   = ".*[A-Z].*";
    private static final String REGEX_LOWER   = ".*[a-z].*";
    private static final String REGEX_NUMBER  = ".*[0-9].*";
    private static final String REGEX_SPECIAL = ".*[@#$%^&+_=!].*";

    public RegisterFrame(String role) {
        this.role = role;

        setTitle("System Registration - " + role);
        setSize(420, 390);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- Header ---
        JPanel headerPanel = new JPanel(new GridBagLayout());
        headerPanel.setBackground(new Color(44, 62, 80)); 
        JLabel headerLabel = new JLabel("CREATE " + role + " ACCOUNT");
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        headerPanel.add(headerLabel);
        add(headerPanel, BorderLayout.NORTH);

        // --- Form ---
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(20, 40, 10, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 5, 8, 5);

        LineBorder lineBorder = new LineBorder(new Color(189, 195, 199), 1);
        EmptyBorder padding = new EmptyBorder(5, 8, 5, 8);
        CompoundBorder fieldBorder = new CompoundBorder(lineBorder, padding);

        // First Name
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        form.add(new JLabel("First Name:"), gbc);
        firstNameField = new JTextField(15);
        firstNameField.setBorder(fieldBorder);
        setupPlaceholder(firstNameField, PROMPT_FIRST);
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(firstNameField, gbc);

        // Last Name
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        form.add(new JLabel("Last Name:"), gbc);
        lastNameField = new JTextField(15);
        lastNameField.setBorder(fieldBorder);
        setupPlaceholder(lastNameField, PROMPT_LAST);
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(lastNameField, gbc);

        // Email
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        form.add(new JLabel("Email Address:"), gbc);
        emailField = new JTextField(15);
        emailField.setBorder(fieldBorder);
        setupPlaceholder(emailField, PROMPT_EMAIL);
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(emailField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        form.add(new JLabel("Password:"), gbc);
        passwordField = new JPasswordField(15);
        passwordField.setBorder(fieldBorder);
        setupPlaceholder(passwordField, PROMPT_PASS);
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(passwordField, gbc);

        // Checklist
        initChecklistPanel();
        gbc.gridx = 1; gbc.gridy = 4;
        gbc.insets = new Insets(0, 5, 15, 5);
        form.add(checklistPanel, gbc);

        add(form, BorderLayout.CENTER);

        // --- Buttons ---
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        JButton cancelBtn = new JButton("Cancel");
        JButton registerBtn = new JButton("Register Now");
        
        styleButton(registerBtn, new Color(46, 204, 113));
        styleButton(cancelBtn, new Color(149, 165, 166));

        btnPanel.add(cancelBtn);
        btnPanel.add(registerBtn);
        add(btnPanel, BorderLayout.SOUTH);

        registerBtn.addActionListener(e -> register());
        cancelBtn.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(registerBtn);

        // Listeners
        passwordField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { validatePasswordRealTime(); }
            public void removeUpdate(DocumentEvent e) { validatePasswordRealTime(); }
            public void changedUpdate(DocumentEvent e) { validatePasswordRealTime(); }
        });
        
        passwordField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                checklistPanel.setVisible(true);
                pack();
            }
            @Override
            public void focusLost(FocusEvent e) {
                String currentPass = new String(passwordField.getPassword());
                if (currentPass.isEmpty() || currentPass.equals(PROMPT_PASS)) {
                    checklistPanel.setVisible(false);
                    pack();
                }
            }
        });
        
        checklistPanel.setVisible(false);
        setVisible(true);
    }

    private void styleButton(JButton btn, Color bgColor) {
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setPreferredSize(new Dimension(115, 30));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
    }

    private void setupPlaceholder(JTextField field, String prompt) {
        field.setText(prompt);
        field.setForeground(Color.GRAY);
        if (field instanceof JPasswordField) ((JPasswordField) field).setEchoChar((char) 0);

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(prompt)) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                    if (field instanceof JPasswordField) ((JPasswordField) field).setEchoChar('•');
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setForeground(Color.GRAY);
                    field.setText(prompt);
                    if (field instanceof JPasswordField) ((JPasswordField) field).setEchoChar((char) 0);
                }
            }
        });
    }

    private void initChecklistPanel() {
        checklistPanel = new JPanel();
        checklistPanel.setLayout(new BoxLayout(checklistPanel, BoxLayout.Y_AXIS));
        checklistPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        checkLength  = createCheckLabel("8-20 characters");
        checkUpper   = createCheckLabel("One Uppercase (A-Z)");
        checkLower   = createCheckLabel("One Lowercase (a-z)");
        checkNumber  = createCheckLabel("One Number (0-9)");
        checkSpecial = createCheckLabel("One Symbol (@#$%^&+_=!)");

        checklistPanel.add(checkLength);
        checklistPanel.add(checkUpper);
        checklistPanel.add(checkLower);
        checklistPanel.add(checkNumber);
        checklistPanel.add(checkSpecial);
    }

    private JLabel createCheckLabel(String text) {
        JLabel label = new JLabel("❌ " + text);
        label.setForeground(Color.RED);
        label.setFont(new Font("SansSerif", Font.PLAIN, 11));
        return label;
    }

    private void validatePasswordRealTime() {
        String pass = new String(passwordField.getPassword());
        if (pass.equals(PROMPT_PASS)) return;

        updateLabel(checkLength, pass.length() >= 8 && pass.length() <= 20);
        updateLabel(checkUpper, pass.matches(REGEX_UPPER));
        updateLabel(checkLower, pass.matches(REGEX_LOWER));
        updateLabel(checkNumber, pass.matches(REGEX_NUMBER));
        updateLabel(checkSpecial, pass.matches(REGEX_SPECIAL));
    }
    
    private void updateLabel(JLabel label, boolean isValid) {
        String text = label.getText().substring(2);
        if (isValid) {
            label.setText("✔ " + text);
            label.setForeground(new Color(0, 150, 0));
        } else {
            label.setText("❌ " + text);
            label.setForeground(Color.RED);
        }
    }

    private void register() {
    String first = firstNameField.getText().trim();
    String last = lastNameField.getText().trim();
    String email = emailField.getText().trim().toLowerCase();
    char[] passwordChars = passwordField.getPassword();
    String pass = new String(passwordChars);

    // 1. 检查空字段
    if (first.equals(PROMPT_FIRST) || last.equals(PROMPT_LAST) || 
        email.equals(PROMPT_EMAIL) || pass.equals(PROMPT_PASS)) {
        JOptionPane.showMessageDialog(this, "Please fill in all fields.");
        return;
    }

    // 2. 检查邮箱格式 (关键修复)
    if (!email.matches(REGEX_EMAIL)) {
        JOptionPane.showMessageDialog(this, "Invalid email format. Please enter a valid email address.");
        return;
    }

    // 3. 检查密码强度 (关键修复)
    boolean isPassValid = pass.length() >= 8 && pass.length() <= 20 &&
                          pass.matches(REGEX_UPPER) &&
                          pass.matches(REGEX_LOWER) &&
                          pass.matches(REGEX_NUMBER) &&
                          pass.matches(REGEX_SPECIAL);

    if (!isPassValid) {
        JOptionPane.showMessageDialog(this, "Password does not meet the security requirements.");
        return;
    }

    // --- 如果通过以上所有检查，才执行注册逻辑 ---
    try {
        String hashed = PasswordUtil.hashPassword(pass);
        byte[] saltBytes = CryptoUtil.generateSalt();
        String uniqueSalt = CryptoUtil.b64(saltBytes);

        User user = new User(first, last, email, hashed, role, uniqueSalt);
        userDao.insert(user);
        auditDao.log("USER_REGISTER", email, email);

        JOptionPane.showMessageDialog(this, "Registration Successful!");
        dispose();

    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
    } finally {
        Arrays.fill(passwordChars, '0');
    }
}
}