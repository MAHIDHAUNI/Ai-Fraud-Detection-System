package com.frauddetect.gui;

import com.frauddetect.dao.AlertDAO;
import com.frauddetect.dao.SettingsDAO;
import com.frauddetect.dao.TransactionDAO;
import com.frauddetect.dao.UserDAO;
import com.frauddetect.db.DBConnection;
import com.frauddetect.detection.FraudDetectionEngine;
import com.frauddetect.exception.AuthenticationException;
import com.frauddetect.exception.DatabaseException;
import com.frauddetect.model.Admin;
import com.frauddetect.model.Customer;
import com.frauddetect.model.User;
import com.frauddetect.service.AlertService;
import com.frauddetect.service.AuthService;
import com.frauddetect.service.ReportService;
import com.frauddetect.service.TransactionService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Screen 1: Secure Authentication and Role-Based Redirection Frame.
 * Directs Customers to CustomerDashboard and Administrators to AdminDashboard.
 */
public class LoginFrame extends JFrame {

    private final AuthService authService;
    private final TransactionService transactionService;
    private final AlertService alertService;
    private final ReportService reportService;
    private final SettingsDAO settingsDAO;
    private final UserDAO userDAO;
    private final FraudDetectionEngine engine;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;

    public LoginFrame(AuthService authService,
                      TransactionService transactionService,
                      AlertService alertService,
                      ReportService reportService,
                      SettingsDAO settingsDAO,
                      UserDAO userDAO,
                      FraudDetectionEngine engine) {
        this.authService = authService;
        this.transactionService = transactionService;
        this.alertService = alertService;
        this.reportService = reportService;
        this.settingsDAO = settingsDAO;
        this.userDAO = userDAO;
        this.engine = engine;

        initUI();
    }

    private void initUI() {
        setTitle("AI Fraud Detection System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 560);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(UIHelper.COLOR_BG);
        setLayout(new BorderLayout());

        // Header Banner
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setBackground(UIHelper.COLOR_HEADER_BG);
        header.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel titleLabel = new JLabel("🛡️ AI Fraud Detection System", SwingConstants.CENTER);
        titleLabel.setFont(UIHelper.FONT_TITLE);
        titleLabel.setForeground(Color.WHITE);

        JLabel subLabel = new JLabel("Real-time Financial Monitoring & Security Console", SwingConstants.CENTER);
        subLabel.setFont(UIHelper.FONT_SMALL);
        subLabel.setForeground(new Color(148, 163, 184));

        header.add(titleLabel, BorderLayout.CENTER);
        header.add(subLabel, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // Center Card Panel
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(new EmptyBorder(16, 24, 16, 24));

        JPanel card = UIHelper.createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(400, 320));

        JLabel loginTitle = new JLabel("Sign In to Your Account");
        loginTitle.setFont(UIHelper.FONT_HEADER);
        loginTitle.setForeground(UIHelper.COLOR_TEXT_MAIN);
        loginTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(loginTitle);
        card.add(Box.createVerticalStrut(16));

        // Username
        JLabel userLabel = new JLabel("Username / Account ID");
        userLabel.setFont(UIHelper.FONT_BOLD);
        userLabel.setForeground(UIHelper.COLOR_TEXT_MAIN);
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(userLabel);
        card.add(Box.createVerticalStrut(4));

        usernameField = new JTextField("admin");
        usernameField.setFont(UIHelper.FONT_REGULAR);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(usernameField);
        card.add(Box.createVerticalStrut(12));

        // Password
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(UIHelper.FONT_BOLD);
        passLabel.setForeground(UIHelper.COLOR_TEXT_MAIN);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(passLabel);
        card.add(Box.createVerticalStrut(4));

        passwordField = new JPasswordField("admin123");
        passwordField.setFont(UIHelper.FONT_REGULAR);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(passwordField);
        card.add(Box.createVerticalStrut(20));

        // Login Button
        loginButton = UIHelper.createPrimaryButton("Sign In  ➔");
        loginButton.setFont(UIHelper.FONT_BOLD);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.addActionListener(e -> performLogin());
        card.add(loginButton);
        card.add(Box.createVerticalStrut(12));

        // Quick Demo Fill buttons
        JPanel demoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        demoPanel.setOpaque(false);
        demoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton adminFill = new JButton("Demo Admin");
        adminFill.setFont(UIHelper.FONT_SMALL);
        adminFill.setCursor(new Cursor(Cursor.HAND_CURSOR));
        adminFill.addActionListener(e -> {
            usernameField.setText("admin");
            passwordField.setText("admin123");
        });

        JButton customerFill = new JButton("Demo Customer");
        customerFill.setFont(UIHelper.FONT_SMALL);
        customerFill.setCursor(new Cursor(Cursor.HAND_CURSOR));
        customerFill.addActionListener(e -> {
            usernameField.setText("john_doe");
            passwordField.setText("password123");
        });

        JButton registerBtn = new JButton("Register New Customer");
        registerBtn.setFont(UIHelper.FONT_SMALL);
        registerBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerBtn.addActionListener(e -> showRegisterDialog());

        demoPanel.add(adminFill);
        demoPanel.add(customerFill);
        demoPanel.add(registerBtn);
        card.add(demoPanel);

        centerWrapper.add(card);
        add(centerWrapper, BorderLayout.CENTER);

        // Enter key shortcut
        getRootPane().setDefaultButton(loginButton);
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        loginButton.setEnabled(false);
        loginButton.setText("Authenticating...");

        SwingUtilities.invokeLater(() -> {
            try {
                User user = authService.login(username, password);

                // Role-based Navigation
                if (user instanceof Admin) {
                    AdminDashboard adminDash = new AdminDashboard(
                            (Admin) user,
                            transactionService,
                            alertService,
                            reportService,
                            settingsDAO,
                            userDAO,
                            engine
                    );
                    adminDash.setVisible(true);
                    dispose();
                } else if (user instanceof Customer) {
                    CustomerDashboard customerDash = new CustomerDashboard(
                            (Customer) user,
                            transactionService,
                            alertService,
                            userDAO
                    );
                    customerDash.setVisible(true);
                    dispose();
                } else {
                    UIHelper.showError(this, "Unrecognized user role.", "Login Error");
                }
            } catch (AuthenticationException e) {
                UIHelper.showError(this, e.getMessage(), "Authentication Failed");
            } catch (DatabaseException e) {
                UIHelper.showError(this, "Database error during login: " + e.getMessage(), "System Error");
            } finally {
                loginButton.setEnabled(true);
                loginButton.setText("Sign In  ➔");
            }
        });
    }

    private void showRegisterDialog() {
        JDialog dialog = new JDialog(this, "Register New Customer", true);
        dialog.setSize(380, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;

        JTextField regUser = new JTextField();
        JPasswordField regPass = new JPasswordField();
        JTextField regName = new JTextField();
        JTextField regEmail = new JTextField();
        JTextField regCountry = new JTextField("India");

        form.add(new JLabel("Username:"), gbc); gbc.gridx = 1; form.add(regUser, gbc); gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Password:"), gbc); gbc.gridx = 1; form.add(regPass, gbc); gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Full Name:"), gbc); gbc.gridx = 1; form.add(regName, gbc); gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Email:"), gbc); gbc.gridx = 1; form.add(regEmail, gbc); gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Home Country:"), gbc); gbc.gridx = 1; form.add(regCountry, gbc); gbc.gridx = 0; gbc.gridy++;

        JButton createBtn = UIHelper.createPrimaryButton("Create Account");
        createBtn.addActionListener(ev -> {
            try {
                authService.registerCustomer(
                        regUser.getText().trim(),
                        new String(regPass.getPassword()),
                        regName.getText().trim(),
                        regEmail.getText().trim(),
                        regCountry.getText().trim()
                );
                UIHelper.showInfo(dialog, "Account created successfully! You can now log in.", "Registration Success");
                usernameField.setText(regUser.getText().trim());
                passwordField.setText(new String(regPass.getPassword()));
                dialog.dispose();
            } catch (Exception ex) {
                UIHelper.showError(dialog, ex.getMessage(), "Registration Error");
            }
        });

        gbc.gridwidth = 2; gbc.gridx = 0; gbc.gridy++;
        form.add(createBtn, gbc);

        dialog.add(form, BorderLayout.CENTER);
        dialog.setVisible(true);
    }
}
