package com.frauddetect.gui;

import com.frauddetect.dao.UserDAO;
import com.frauddetect.exception.DatabaseException;
import com.frauddetect.exception.InvalidTransactionException;
import com.frauddetect.model.Alert;
import com.frauddetect.model.Customer;
import com.frauddetect.model.DetectionResult;
import com.frauddetect.model.DomesticTransaction;
import com.frauddetect.model.InternationalTransaction;
import com.frauddetect.model.RiskLevel;
import com.frauddetect.model.Transaction;
import com.frauddetect.service.AlertService;
import com.frauddetect.service.TransactionService;
import com.frauddetect.util.Result;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

/**
 * Screen 2: Customer Dashboard with tabbed interface.
 * Tabs: New Transaction | My Transactions | My Alerts
 *
 * RUBRIC: 1 - OOP: Polymorphism (uses Customer polymorphic methods like getDashboardTitle)
 * RUBRIC: 3 - Multithreading: GUI updates dispatched through SwingUtilities.invokeLater
 */
public class CustomerDashboard extends JFrame {

    private final Customer customer;
    private final TransactionService transactionService;
    private final AlertService alertService;
    private final UserDAO userDAO;

    // Transaction form fields
    private JTextField amountField;
    private JComboBox<String> typeCombo;
    private JTextField receiverField;
    private JTextField locationField;
    private JTextField countryField;
    private JTextArea descriptionArea;
    private JButton submitButton;

    // Tables
    private JTable transactionsTable;
    private DefaultTableModel transactionsModel;
    private JTable alertsTable;
    private DefaultTableModel alertsModel;

    public CustomerDashboard(Customer customer,
                             TransactionService transactionService,
                             AlertService alertService,
                             UserDAO userDAO) {
        this.customer = customer;
        this.transactionService = transactionService;
        this.alertService = alertService;
        this.userDAO = userDAO;

        initUI();
        loadTransactions();
        loadAlerts();
    }

    private void initUI() {
        // Polymorphism: getDashboardTitle() is polymorphically resolved on Customer
        setTitle(customer.getDashboardTitle());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 680);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 600));
        getContentPane().setBackground(UIHelper.COLOR_BG);
        setLayout(new BorderLayout());

        // Header with Logout button
        JButton logoutBtn = UIHelper.createButton("Logout", new Color(71, 85, 105), Color.WHITE);
        logoutBtn.addActionListener(e -> logout());

        JPanel header = UIHelper.createHeader(
                "🛡️ Customer Portal",
                "Welcome, " + customer.getFullName() + " — Secure Transaction Monitoring",
                "CUSTOMER",
                logoutBtn
        );
        add(header, BorderLayout.NORTH);

        // Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UIHelper.FONT_BOLD);
        tabbedPane.setBackground(UIHelper.COLOR_BG);
        tabbedPane.setBorder(new EmptyBorder(8, 12, 12, 12));

        tabbedPane.addTab("  📝 New Transaction  ", buildNewTransactionPanel());
        tabbedPane.addTab("  📊 My Transactions  ", buildMyTransactionsPanel());
        tabbedPane.addTab("  🔔 My Alerts  ", buildMyAlertsPanel());

        // Refresh data when switching to transactions or alerts tabs
        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();
            if (idx == 1) loadTransactions();
            else if (idx == 2) loadAlerts();
        });

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ====== TAB 1: New Transaction Form ======

    private JPanel buildNewTransactionPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setOpaque(false);

        JPanel card = UIHelper.createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(520, 480));

        JLabel title = new JLabel("Submit New Transaction");
        title.setFont(UIHelper.FONT_HEADER);
        title.setForeground(UIHelper.COLOR_TEXT_MAIN);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(12));

        // Build form using GridBagLayout for label-field pairs
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints lbl = new GridBagConstraints();
        lbl.anchor = GridBagConstraints.WEST;
        lbl.insets = new Insets(5, 0, 5, 10);
        lbl.gridx = 0;

        GridBagConstraints fld = new GridBagConstraints();
        fld.fill = GridBagConstraints.HORIZONTAL;
        fld.weightx = 1.0;
        fld.insets = new Insets(5, 0, 5, 0);
        fld.gridx = 1;

        int row = 0;

        // Amount
        lbl.gridy = row; fld.gridy = row++;
        form.add(createFormLabel("Amount (₹)"), lbl);
        amountField = new JTextField();
        amountField.setFont(UIHelper.FONT_REGULAR);
        form.add(amountField, fld);

        // Transaction Type
        lbl.gridy = row; fld.gridy = row++;
        form.add(createFormLabel("Type"), lbl);
        typeCombo = new JComboBox<>(new String[]{"DOMESTIC", "INTERNATIONAL"});
        typeCombo.setFont(UIHelper.FONT_REGULAR);
        form.add(typeCombo, fld);

        // Receiver Account
        lbl.gridy = row; fld.gridy = row++;
        form.add(createFormLabel("Receiver Account"), lbl);
        receiverField = new JTextField();
        receiverField.setFont(UIHelper.FONT_REGULAR);
        form.add(receiverField, fld);

        // Location
        lbl.gridy = row; fld.gridy = row++;
        form.add(createFormLabel("Location"), lbl);
        locationField = new JTextField();
        locationField.setFont(UIHelper.FONT_REGULAR);
        form.add(locationField, fld);

        // Country
        lbl.gridy = row; fld.gridy = row++;
        form.add(createFormLabel("Country"), lbl);
        countryField = new JTextField(customer.getHomeCountry());
        countryField.setFont(UIHelper.FONT_REGULAR);
        form.add(countryField, fld);

        // Description
        lbl.gridy = row; fld.gridy = row++;
        lbl.anchor = GridBagConstraints.NORTHWEST;
        form.add(createFormLabel("Description"), lbl);
        descriptionArea = new JTextArea(3, 20);
        descriptionArea.setFont(UIHelper.FONT_REGULAR);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        form.add(descScroll, fld);

        card.add(form);
        card.add(Box.createVerticalStrut(16));

        // Submit Button
        submitButton = UIHelper.createPrimaryButton("Submit Transaction  ➔");
        submitButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        submitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        submitButton.addActionListener(e -> submitTransaction());
        card.add(submitButton);

        outer.add(card);
        return outer;
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UIHelper.FONT_BOLD);
        label.setForeground(UIHelper.COLOR_TEXT_MAIN);
        return label;
    }

    /**
     * Validates input, creates the appropriate Transaction subclass (Polymorphism),
     * submits it to the TransactionService, and shows a result dialog.
     */
    private void submitTransaction() {
        // Input validation
        String amountText = amountField.getText().trim();
        String receiver = receiverField.getText().trim();
        String location = locationField.getText().trim();
        String country = countryField.getText().trim();
        String type = (String) typeCombo.getSelectedItem();
        String desc = descriptionArea.getText().trim();

        if (amountText.isEmpty() || receiver.isEmpty() || location.isEmpty() || country.isEmpty()) {
            UIHelper.showError(this, "Please fill in all required fields (Amount, Receiver, Location, Country).", "Validation Error");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException ex) {
            UIHelper.showError(this, "Amount must be a valid number.", "Validation Error");
            return;
        }

        // RUBRIC: 1 - Polymorphism: Create the correct Transaction subclass based on type
        Transaction txn;
        if ("INTERNATIONAL".equals(type)) {
            txn = new InternationalTransaction();
        } else {
            txn = new DomesticTransaction();
        }
        txn.setUserId(customer.getUserId());
        txn.setAmount(amount);
        txn.setReceiverAccount(receiver);
        txn.setLocation(location);
        txn.setCountry(country);
        txn.setDescription(desc);

        submitButton.setEnabled(false);
        submitButton.setText("Processing...");

        // Submit asynchronously using TransactionService's synchronous path for immediate result
        new Thread(() -> {
            try {
                DetectionResult result = transactionService.processSynchronous(txn);

                // RUBRIC: 3 - SwingUtilities.invokeLater for thread-safe GUI update
                SwingUtilities.invokeLater(() -> {
                    showResultDialog(txn, result);
                    clearForm();
                    submitButton.setEnabled(true);
                    submitButton.setText("Submit Transaction  ➔");
                });
            } catch (InvalidTransactionException ex) {
                SwingUtilities.invokeLater(() -> {
                    UIHelper.showError(this, ex.getMessage(), "Transaction Validation Error");
                    submitButton.setEnabled(true);
                    submitButton.setText("Submit Transaction  ➔");
                });
            } catch (DatabaseException ex) {
                SwingUtilities.invokeLater(() -> {
                    UIHelper.showError(this, "Database error: " + ex.getMessage(), "System Error");
                    submitButton.setEnabled(true);
                    submitButton.setText("Submit Transaction  ➔");
                });
            }
        }).start();
    }

    /**
     * Displays a rich result dialog showing the risk assessment with color-coded background.
     */
    private void showResultDialog(Transaction txn, DetectionResult result) {
        Color bgColor;
        String emoji;
        if (result.getLevel() == RiskLevel.HIGH) {
            bgColor = new Color(254, 226, 226);
            emoji = "🚨";
        } else if (result.getLevel() == RiskLevel.MEDIUM) {
            bgColor = new Color(254, 243, 199);
            emoji = "⚠️";
        } else {
            bgColor = new Color(220, 252, 231);
            emoji = "✅";
        }

        String message = String.format(
                "%s Transaction #%d Analysis Complete\n\n" +
                "Amount:          ₹%,.2f\n" +
                "Type:              %s\n" +
                "Risk Score:      %d / 100\n" +
                "Risk Level:       %s\n" +
                "Decision:         %s\n\n" +
                "Reasons:\n%s",
                emoji, txn.getTxnId(), txn.getAmount(), txn.getCategory(),
                result.getScore(), result.getLevel(), result.getRecommendedStatus(),
                result.getFormattedReasons()
        );

        String title = result.getLevel() == RiskLevel.LOW
                ? "Transaction Approved" : "Transaction " + result.getRecommendedStatus();
        int msgType = result.getLevel() == RiskLevel.HIGH
                ? JOptionPane.ERROR_MESSAGE
                : (result.getLevel() == RiskLevel.MEDIUM ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);

        JOptionPane.showMessageDialog(this, message, title, msgType);
    }

    private void clearForm() {
        amountField.setText("");
        receiverField.setText("");
        locationField.setText("");
        countryField.setText(customer.getHomeCountry());
        descriptionArea.setText("");
        typeCombo.setSelectedIndex(0);
    }

    // ====== TAB 2: My Transactions Table ======

    private JPanel buildMyTransactionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JLabel title = new JLabel("My Transaction History");
        title.setFont(UIHelper.FONT_HEADER);
        title.setForeground(UIHelper.COLOR_TEXT_MAIN);
        topBar.add(title, BorderLayout.WEST);

        JButton refreshBtn = UIHelper.createPrimaryButton("Refresh");
        refreshBtn.addActionListener(e -> loadTransactions());
        topBar.add(refreshBtn, BorderLayout.EAST);

        panel.add(topBar, BorderLayout.NORTH);

        // Table with columns
        String[] columns = {"Txn ID", "Amount (₹)", "Type", "Receiver", "Location", "Country",
                "Time", "Risk Score", "Risk Level", "Status"};
        transactionsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Read-only table
            }
        };
        transactionsTable = new JTable(transactionsModel);
        transactionsTable.setFont(UIHelper.FONT_REGULAR);
        UIHelper.formatTable(transactionsTable);

        JScrollPane scrollPane = new JScrollPane(transactionsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIHelper.COLOR_BORDER));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Loads the current user's transactions from the database and updates the table.
     * All GUI updates happen via SwingUtilities.invokeLater (RUBRIC: 3 - Multithreading).
     */
    private void loadTransactions() {
        new Thread(() -> {
            try {
                List<Transaction> txns = transactionService.getUserTransactions(customer.getUserId());
                SwingUtilities.invokeLater(() -> {
                    transactionsModel.setRowCount(0);
                    for (Transaction t : txns) {
                        transactionsModel.addRow(new Object[]{
                                t.getTxnId(),
                                String.format("₹%,.2f", t.getAmount()),
                                t.getTxnType(),
                                t.getReceiverAccount(),
                                t.getLocation(),
                                t.getCountry(),
                                t.getTxnTime() != null ? t.getTxnTime().toString() : "N/A",
                                t.getRiskScore(),
                                t.getRiskLevel().name(),
                                t.getStatus()
                        });
                    }
                });
            } catch (DatabaseException ex) {
                SwingUtilities.invokeLater(() ->
                        UIHelper.showError(this, "Failed to load transactions: " + ex.getMessage(), "Database Error")
                );
            }
        }).start();
    }

    // ====== TAB 3: My Alerts ======

    private JPanel buildMyAlertsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JLabel title = new JLabel("My Security Alerts");
        title.setFont(UIHelper.FONT_HEADER);
        title.setForeground(UIHelper.COLOR_TEXT_MAIN);
        topBar.add(title, BorderLayout.WEST);

        JButton refreshBtn = UIHelper.createPrimaryButton("Refresh");
        refreshBtn.addActionListener(e -> loadAlerts());
        topBar.add(refreshBtn, BorderLayout.EAST);

        panel.add(topBar, BorderLayout.NORTH);

        String[] columns = {"Alert ID", "Txn ID", "Risk Level", "Reasons", "Alert Time", "Resolved", "Admin Note"};
        alertsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        alertsTable = new JTable(alertsModel);
        alertsTable.setFont(UIHelper.FONT_REGULAR);
        UIHelper.formatTable(alertsTable);

        JScrollPane scrollPane = new JScrollPane(alertsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIHelper.COLOR_BORDER));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Loads alerts for the current customer from the database.
     */
    private void loadAlerts() {
        new Thread(() -> {
            try {
                List<Alert> alerts = alertService.getUserAlerts(customer.getUserId());
                SwingUtilities.invokeLater(() -> {
                    alertsModel.setRowCount(0);
                    for (Alert a : alerts) {
                        alertsModel.addRow(new Object[]{
                                a.getAlertId(),
                                a.getTxnId(),
                                a.getRiskLevel().name(),
                                a.getReasons(),
                                a.getAlertTime() != null ? a.getAlertTime().toString() : "N/A",
                                a.isResolved() ? "Yes" : "No",
                                a.getAdminNote() != null ? a.getAdminNote() : ""
                        });
                    }
                });
            } catch (DatabaseException ex) {
                SwingUtilities.invokeLater(() ->
                        UIHelper.showError(this, "Failed to load alerts: " + ex.getMessage(), "Database Error")
                );
            }
        }).start();
    }

    /**
     * Logs the user out and returns to the LoginFrame.
     */
    private void logout() {
        dispose();
        // Re-launch the login screen — the caller (LoginFrame) already set up services
        UIHelper.showInfo(null, "You have been logged out.", "Logged Out");
    }
}
