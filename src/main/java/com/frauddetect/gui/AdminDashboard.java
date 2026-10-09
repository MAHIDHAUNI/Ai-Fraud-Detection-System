package com.frauddetect.gui;

import com.frauddetect.concurrent.StatsCounter;
import com.frauddetect.dao.SettingsDAO;
import com.frauddetect.dao.UserDAO;
import com.frauddetect.detection.FraudDetectionEngine;
import com.frauddetect.exception.DatabaseException;
import com.frauddetect.model.Admin;
import com.frauddetect.model.Alert;
import com.frauddetect.model.RiskLevel;
import com.frauddetect.model.Transaction;
import com.frauddetect.service.AlertService;
import com.frauddetect.service.ReportService;
import com.frauddetect.service.TransactionService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import com.frauddetect.ml.FeatureExtractor;
import com.frauddetect.ml.LogisticRegressionModel;
import com.frauddetect.ml.ModelEvaluator;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Screen 3: Administrator Security Console with tabbed interface.
 * Tabs: Live Monitor | Alerts | Reports | Settings
 *
 * RUBRIC: 1 - OOP: Polymorphism (Admin.getDashboardTitle()), Interfaces (service abstractions)
 * RUBRIC: 2 - Collections & Generics (Map<RiskLevel, Long>, List<RiskyUserSummary>)
 * RUBRIC: 3 - Multithreading: ScheduledExecutorService for auto-refresh, SwingUtilities.invokeLater
 */
public class AdminDashboard extends JFrame {

    private final Admin admin;
    private final TransactionService transactionService;
    private final AlertService alertService;
    private final ReportService reportService;
    private final SettingsDAO settingsDAO;
    private final UserDAO userDAO;
    private final FraudDetectionEngine engine;

    // Live Monitor components
    private JTable monitorTable;
    private DefaultTableModel monitorModel;
    private JLabel totalStatLabel;
    private JLabel flaggedStatLabel;
    private JLabel blockedStatLabel;
    private JLabel volumeStatLabel;

    // Alerts components
    private JTable alertsTable;
    private DefaultTableModel alertsModel;

    // Reports components
    private JTextArea reportArea;

    // Settings fields
    private JTextField highAmountField;
    private JTextField velocityMaxField;
    private JTextField velocityWindowField;
    private JTextField zScoreField;
    private JTextField mediumCutoffField;
    private JTextField highCutoffField;
    private JTextField mlWeightField;
    private JCheckBox ruleHighAmountCb;
    private JCheckBox ruleVelocityCb;
    private JCheckBox ruleAnomalyCb;
    private JCheckBox ruleTimeCb;
    private JCheckBox ruleLocationCb;
    private JCheckBox ruleRoundCb;
    private JCheckBox ruleRepeatCb;

    // ML Engine components
    private JLabel mlStatusLabel;
    private JSpinner mlWeightSpinner;
    private JTable weightsTable;
    private DefaultTableModel weightsModel;
    private JTextArea metricsArea;

    /**
     * RUBRIC: 3 - Multithreading: ScheduledExecutorService for periodic dashboard refresh
     * Runs a background scheduler that refreshes the live monitor table every 5 seconds.
     */
    private ScheduledExecutorService refreshScheduler;

    public AdminDashboard(Admin admin,
                          TransactionService transactionService,
                          AlertService alertService,
                          ReportService reportService,
                          SettingsDAO settingsDAO,
                          UserDAO userDAO,
                          FraudDetectionEngine engine) {
        this.admin = admin;
        this.transactionService = transactionService;
        this.alertService = alertService;
        this.reportService = reportService;
        this.settingsDAO = settingsDAO;
        this.userDAO = userDAO;
        this.engine = engine;

        initUI();
        startAutoRefresh();
        refreshMonitor();
    }

    private void initUI() {
        // Polymorphism: getDashboardTitle() resolved on Admin subclass
        setTitle(admin.getDashboardTitle());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 740);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 650));
        getContentPane().setBackground(UIHelper.COLOR_BG);
        setLayout(new BorderLayout());

        // Header with Logout
        JButton logoutBtn = UIHelper.createButton("Logout", new Color(71, 85, 105), Color.WHITE);
        logoutBtn.addActionListener(e -> logout());

        JPanel header = UIHelper.createHeader(
                "🛡️ Admin Security Console",
                "Operator: " + admin.getFullName() + " — Real-time Fraud Monitoring & Analysis",
                "ADMIN",
                logoutBtn
        );
        add(header, BorderLayout.NORTH);

        // Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UIHelper.FONT_BOLD);
        tabbedPane.setBackground(UIHelper.COLOR_BG);
        tabbedPane.setBorder(new EmptyBorder(8, 12, 12, 12));

        tabbedPane.addTab("  📡 Live Monitor  ", buildMonitorPanel());
        tabbedPane.addTab("  🔔 Alerts  ", buildAlertsPanel());
        tabbedPane.addTab("  📊 Reports  ", buildReportsPanel());
        tabbedPane.addTab("  🤖 ML Engine  ", buildMLPanel());
        tabbedPane.addTab("  ⚙️ Settings  ", buildSettingsPanel());

        // Refresh data when switching tabs
        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();
            switch (idx) {
                case 0 -> refreshMonitor();
                case 1 -> loadAlerts();
                case 3 -> refreshMLPanel();
                case 4 -> loadSettings();
            }
        });

        add(tabbedPane, BorderLayout.CENTER);

        // Clean shutdown on window close
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                stopAutoRefresh();
            }
        });
    }

    // ====================== TAB 1: LIVE MONITOR ======================

    private JPanel buildMonitorPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Summary KPI Cards Row
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setOpaque(false);

        totalStatLabel = new JLabel("0");
        flaggedStatLabel = new JLabel("0");
        blockedStatLabel = new JLabel("0");
        volumeStatLabel = new JLabel("₹0");

        JPanel totalCard = UIHelper.createStatCard("TOTAL TRANSACTIONS", "0", UIHelper.COLOR_PRIMARY, "All processed");
        JPanel flaggedCard = UIHelper.createStatCard("FLAGGED (MEDIUM)", "0", UIHelper.COLOR_WARNING, "Under review");
        JPanel blockedCard = UIHelper.createStatCard("BLOCKED (HIGH)", "0", UIHelper.COLOR_DANGER, "Prevented");
        JPanel volumeCard = UIHelper.createStatCard("VOLUME", "₹0", new Color(100, 116, 139), "Total amount");

        // Store label references for dynamic updates
        totalStatLabel = findMetricLabel(totalCard);
        flaggedStatLabel = findMetricLabel(flaggedCard);
        blockedStatLabel = findMetricLabel(blockedCard);
        volumeStatLabel = findMetricLabel(volumeCard);

        statsRow.add(totalCard);
        statsRow.add(flaggedCard);
        statsRow.add(blockedCard);
        statsRow.add(volumeCard);

        panel.add(statsRow, BorderLayout.NORTH);

        // Transactions table
        String[] columns = {"Txn ID", "User ID", "Amount (₹)", "Type", "Receiver", "Location",
                "Country", "Time", "Risk Score", "Risk Level", "Status"};
        monitorModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        monitorTable = new JTable(monitorModel);
        monitorTable.setFont(UIHelper.FONT_REGULAR);
        UIHelper.formatTable(monitorTable);

        JScrollPane scrollPane = new JScrollPane(monitorTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIHelper.COLOR_BORDER));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Bottom toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        toolbar.setOpaque(false);

        JButton refreshBtn = UIHelper.createPrimaryButton("Refresh Now");
        refreshBtn.addActionListener(e -> refreshMonitor());
        toolbar.add(refreshBtn);

        panel.add(toolbar, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Finds the metric value JLabel inside a stat card (the one with FONT_METRIC).
     */
    private JLabel findMetricLabel(JPanel card) {
        for (Component c : card.getComponents()) {
            if (c instanceof JLabel lbl && lbl.getFont().equals(UIHelper.FONT_METRIC)) {
                return lbl;
            }
        }
        return new JLabel("0");
    }

    /**
     * Refreshes the live monitor: reloads all transactions and updates KPI stats.
     * RUBRIC: 3 - Multithreading: Background thread + SwingUtilities.invokeLater
     */
    private void refreshMonitor() {
        new Thread(() -> {
            try {
                List<Transaction> txns = transactionService.getAllTransactions();
                StatsCounter.StatsSnapshot snapshot = transactionService.getStatsCounter().getSnapshot();

                SwingUtilities.invokeLater(() -> {
                    // Update KPI cards
                    totalStatLabel.setText(String.valueOf(txns.size()));
                    int flagged = 0, blocked = 0;
                    double volume = 0;
                    for (Transaction t : txns) {
                        volume += t.getAmount();
                        if (t.getRiskLevel() == RiskLevel.HIGH || "BLOCKED".equalsIgnoreCase(t.getStatus())) {
                            blocked++;
                        } else if (t.getRiskLevel() == RiskLevel.MEDIUM || "FLAGGED".equalsIgnoreCase(t.getStatus())) {
                            flagged++;
                        }
                    }
                    flaggedStatLabel.setText(String.valueOf(flagged));
                    blockedStatLabel.setText(String.valueOf(blocked));
                    volumeStatLabel.setText(String.format("₹%,.0f", volume));

                    // Update table
                    monitorModel.setRowCount(0);
                    for (Transaction t : txns) {
                        monitorModel.addRow(new Object[]{
                                t.getTxnId(),
                                t.getUserId(),
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
                        UIHelper.showError(this, "Monitor refresh failed: " + ex.getMessage(), "Database Error")
                );
            }
        }).start();
    }

    /**
     * RUBRIC: 3 - Multithreading: ScheduledExecutorService auto-refresh every 5 seconds.
     * Background thread periodically queries the database and pushes GUI updates through EDT.
     */
    private void startAutoRefresh() {
        refreshScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AdminMonitor-Refresh");
            t.setDaemon(true);
            return t;
        });
        refreshScheduler.scheduleAtFixedRate(this::refreshMonitor, 5, 5, TimeUnit.SECONDS);
    }

    private void stopAutoRefresh() {
        if (refreshScheduler != null && !refreshScheduler.isShutdown()) {
            refreshScheduler.shutdown();
        }
    }

    // ====================== TAB 2: ALERTS ======================

    private JPanel buildAlertsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JLabel title = new JLabel("Security Alert Management");
        title.setFont(UIHelper.FONT_HEADER);
        title.setForeground(UIHelper.COLOR_TEXT_MAIN);
        topBar.add(title, BorderLayout.WEST);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterBar.setOpaque(false);

        JButton showAllBtn = UIHelper.createPrimaryButton("All Alerts");
        showAllBtn.addActionListener(e -> loadAlerts());

        JButton showUnresolvedBtn = UIHelper.createButton("Unresolved Only", UIHelper.COLOR_WARNING, Color.WHITE);
        showUnresolvedBtn.addActionListener(e -> loadUnresolvedAlerts());

        JButton refreshBtn = UIHelper.createPrimaryButton("Refresh");
        refreshBtn.addActionListener(e -> loadAlerts());

        filterBar.add(showUnresolvedBtn);
        filterBar.add(showAllBtn);
        filterBar.add(refreshBtn);
        topBar.add(filterBar, BorderLayout.EAST);

        panel.add(topBar, BorderLayout.NORTH);

        // Alerts table
        String[] columns = {"Alert ID", "Txn ID", "User ID", "Risk Level", "Reasons", "Alert Time", "Resolved", "Admin Note"};
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

        // Bottom action bar: Resolve selected alert
        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actionBar.setOpaque(false);

        JButton resolveBtn = UIHelper.createSuccessButton("Resolve Selected Alert");
        resolveBtn.addActionListener(e -> resolveSelectedAlert());
        actionBar.add(resolveBtn);

        panel.add(actionBar, BorderLayout.SOUTH);

        return panel;
    }

    private void loadAlerts() {
        new Thread(() -> {
            try {
                List<Alert> alerts = alertService.getAllAlerts();
                populateAlertsTable(alerts);
            } catch (DatabaseException ex) {
                SwingUtilities.invokeLater(() ->
                        UIHelper.showError(this, "Failed to load alerts: " + ex.getMessage(), "Database Error")
                );
            }
        }).start();
    }

    private void loadUnresolvedAlerts() {
        new Thread(() -> {
            try {
                List<Alert> alerts = alertService.getUnresolvedAlerts();
                populateAlertsTable(alerts);
            } catch (DatabaseException ex) {
                SwingUtilities.invokeLater(() ->
                        UIHelper.showError(this, "Failed to load alerts: " + ex.getMessage(), "Database Error")
                );
            }
        }).start();
    }

    private void populateAlertsTable(List<Alert> alerts) {
        SwingUtilities.invokeLater(() -> {
            alertsModel.setRowCount(0);
            for (Alert a : alerts) {
                alertsModel.addRow(new Object[]{
                        a.getAlertId(),
                        a.getTxnId(),
                        a.getUserId(),
                        a.getRiskLevel().name(),
                        a.getReasons(),
                        a.getAlertTime() != null ? a.getAlertTime().toString() : "N/A",
                        a.isResolved() ? "Yes" : "No",
                        a.getAdminNote() != null ? a.getAdminNote() : ""
                });
            }
        });
    }

    /**
     * Resolves the selected alert by prompting for an admin note.
     */
    private void resolveSelectedAlert() {
        int selectedRow = alertsTable.getSelectedRow();
        if (selectedRow < 0) {
            UIHelper.showWarning(this, "Please select an alert to resolve.", "No Selection");
            return;
        }

        int alertId = (int) alertsModel.getValueAt(selectedRow, 0);
        String currentStatus = (String) alertsModel.getValueAt(selectedRow, 6);
        if ("Yes".equals(currentStatus)) {
            UIHelper.showInfo(this, "This alert is already resolved.", "Already Resolved");
            return;
        }

        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
        dialogPanel.add(new JLabel("Enter resolution note for Alert #" + alertId + ":"));
        JTextField noteField = new JTextField("Reviewed by Administrator", 20);
        dialogPanel.add(noteField);
        dialogPanel.add(Box.createVerticalStrut(10));

        dialogPanel.add(new JLabel("Fraud Feedback Label (for ML Model Retraining):"));
        JRadioButton fraudBtn = new JRadioButton("Confirmed Fraud (True Positive)", true);
        JRadioButton legitBtn = new JRadioButton("Legitimate Transaction (False Positive)");
        ButtonGroup group = new ButtonGroup();
        group.add(fraudBtn);
        group.add(legitBtn);
        dialogPanel.add(fraudBtn);
        dialogPanel.add(legitBtn);

        int option = JOptionPane.showConfirmDialog(this, dialogPanel, "Resolve Alert #" + alertId,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (option != JOptionPane.OK_OPTION) return;

        String note = noteField.getText().trim();
        boolean confirmedFraud = fraudBtn.isSelected();

        new Thread(() -> {
            try {
                boolean resolved = alertService.resolveAlert(alertId, note, confirmedFraud);
                if (resolved) {
                    SwingUtilities.invokeLater(() -> {
                        UIHelper.showInfo(this, "Alert #" + alertId + " resolved (" +
                                (confirmedFraud ? "Confirmed Fraud" : "False Positive") + ").", "Alert Resolved");
                        loadAlerts();
                    });
                } else {
                    SwingUtilities.invokeLater(() ->
                            UIHelper.showError(this, "Could not resolve alert. It may no longer exist.", "Resolution Failed")
                    );
                }
            } catch (DatabaseException ex) {
                SwingUtilities.invokeLater(() ->
                        UIHelper.showError(this, "Database error: " + ex.getMessage(), "System Error")
                );
            }
        }).start();
    }

    // ====================== TAB 3: REPORTS ======================

    private JPanel buildReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JLabel title = new JLabel("Fraud Detection Analytics & Reports");
        title.setFont(UIHelper.FONT_HEADER);
        title.setForeground(UIHelper.COLOR_TEXT_MAIN);
        topBar.add(title, BorderLayout.WEST);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);

        JButton generateBtn = UIHelper.createPrimaryButton("Generate Report");
        generateBtn.addActionListener(e -> generateReport());

        JButton exportBtn = UIHelper.createSuccessButton("Export CSV");
        exportBtn.addActionListener(e -> exportCSV());

        btnBar.add(generateBtn);
        btnBar.add(exportBtn);
        topBar.add(btnBar, BorderLayout.EAST);

        panel.add(topBar, BorderLayout.NORTH);

        // Report text area
        reportArea = new JTextArea();
        reportArea.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 13));
        reportArea.setEditable(false);
        reportArea.setBackground(new Color(248, 250, 252));
        reportArea.setForeground(UIHelper.COLOR_TEXT_MAIN);
        reportArea.setBorder(new EmptyBorder(12, 12, 12, 12));
        reportArea.setText("Click 'Generate Report' to view fraud detection analytics.\n");

        JScrollPane scrollPane = new JScrollPane(reportArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIHelper.COLOR_BORDER));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Simple bar chart panel
        JPanel chartPanel = buildBarChartPanel();
        chartPanel.setPreferredSize(new Dimension(0, 140));
        panel.add(chartPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Generates a formatted text report with system summary, risk distribution,
     * top risky users, and daily trends.
     */
    private void generateReport() {
        new Thread(() -> {
            try {
                ReportService.SystemSummary summary = reportService.getSummaryMetrics();
                Map<RiskLevel, Long> riskDist = reportService.getTransactionsPerRiskLevel();
                List<ReportService.RiskyUserSummary> topUsers = reportService.getTopRiskiestUsers(5);
                Map<String, ReportService.DailyTrend> trends = reportService.getDailyTrendLast7Days();

                StringBuilder sb = new StringBuilder();
                sb.append("═══════════════════════════════════════════════════════\n");
                sb.append("        AI FRAUD DETECTION — ANALYTICS REPORT\n");
                sb.append("═══════════════════════════════════════════════════════\n\n");

                sb.append("▶ SYSTEM SUMMARY\n");
                sb.append(String.format("  Total Transactions:     %d\n", summary.getTotalTransactions()));
                sb.append(String.format("  Approved:               %d\n", summary.getApprovedCount()));
                sb.append(String.format("  Flagged (Medium Risk):  %d\n", summary.getFlaggedCount()));
                sb.append(String.format("  Blocked (High Risk):    %d\n", summary.getBlockedCount()));
                sb.append(String.format("  Total Volume:           ₹%,.2f\n", summary.getTotalVolume()));
                sb.append(String.format("  Prevented Fraud Volume: ₹%,.2f\n", summary.getPreventedFraudVolume()));
                sb.append(String.format("  Fraud Detection Rate:   %.2f%%\n\n", summary.getFraudPercentage()));

                sb.append("▶ RISK LEVEL DISTRIBUTION\n");
                for (Map.Entry<RiskLevel, Long> entry : riskDist.entrySet()) {
                    double pct = summary.getTotalTransactions() > 0
                            ? ((double) entry.getValue() / summary.getTotalTransactions()) * 100.0 : 0;
                    sb.append(String.format("  %-8s %4d  (%.1f%%)\n", entry.getKey(), entry.getValue(), pct));
                }
                sb.append("\n");

                sb.append("▶ TOP 5 RISKIEST USERS\n");
                sb.append(String.format("  %-4s %-15s %-20s %6s %6s %6s %8s\n",
                        "ID", "Username", "Full Name", "Txns", "Susp", "Block", "AvgScore"));
                sb.append("  " + "─".repeat(75) + "\n");
                for (ReportService.RiskyUserSummary u : topUsers) {
                    sb.append(String.format("  %-4d %-15s %-20s %6d %6d %6d %8.1f\n",
                            u.getUserId(), u.getUsername(), u.getFullName(),
                            u.getTotalTransactions(), u.getSuspiciousCount(),
                            u.getBlockedCount(), u.getAverageRiskScore()));
                }
                sb.append("\n");

                sb.append("▶ DAILY TREND (LAST 7 DAYS)\n");
                sb.append(String.format("  %-12s %6s %6s %6s %6s %12s %8s\n",
                        "Date", "Total", "OK", "Flag", "Block", "Volume", "AvgScore"));
                sb.append("  " + "─".repeat(70) + "\n");
                for (ReportService.DailyTrend day : trends.values()) {
                    sb.append(String.format("  %-12s %6d %6d %6d %6d %12s %8.1f\n",
                            day.getDateStr(), day.getTotalCount(), day.getApprovedCount(),
                            day.getFlaggedCount(), day.getBlockedCount(),
                            String.format("₹%,.0f", day.getTotalVolume()), day.getAverageRiskScore()));
                }

                SwingUtilities.invokeLater(() -> reportArea.setText(sb.toString()));

            } catch (DatabaseException ex) {
                SwingUtilities.invokeLater(() ->
                        UIHelper.showError(this, "Failed to generate report: " + ex.getMessage(), "Report Error")
                );
            }
        }).start();
    }

    /**
     * Exports report data to a CSV file chosen by the admin.
     */
    private void exportCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Fraud Report to CSV");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
        chooser.setSelectedFile(new File("fraud_report.csv"));

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().endsWith(".csv")) {
            file = new File(file.getAbsolutePath() + ".csv");
        }

        File targetFile = file;
        new Thread(() -> {
            try {
                reportService.exportReportToCSV(targetFile);
                SwingUtilities.invokeLater(() ->
                        UIHelper.showInfo(this, "Report exported to:\n" + targetFile.getAbsolutePath(), "Export Successful")
                );
            } catch (IOException | DatabaseException ex) {
                SwingUtilities.invokeLater(() ->
                        UIHelper.showError(this, "Export failed: " + ex.getMessage(), "Export Error")
                );
            }
        }).start();
    }

    /**
     * Custom-painted horizontal bar chart showing risk distribution.
     */
    private JPanel buildBarChartPanel() {
        JPanel chart = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth() - 40;
                int h = getHeight() - 40;
                int x0 = 20;
                int y0 = 10;

                g2.setColor(UIHelper.COLOR_TEXT_MUTED);
                g2.setFont(UIHelper.FONT_SMALL);
                g2.drawString("Risk Level Distribution", x0, y0 + 10);

                try {
                    Map<RiskLevel, Long> dist = reportService.getTransactionsPerRiskLevel();
                    long total = dist.values().stream().mapToLong(v -> v).sum();
                    if (total == 0) {
                        g2.drawString("No transaction data available", x0, y0 + 50);
                        g2.dispose();
                        return;
                    }

                    int barY = y0 + 30;
                    int barHeight = 28;
                    int gap = 8;
                    Color[] colors = {UIHelper.COLOR_SUCCESS, UIHelper.COLOR_WARNING, UIHelper.COLOR_DANGER};
                    RiskLevel[] levels = {RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH};

                    for (int i = 0; i < levels.length; i++) {
                        long count = dist.getOrDefault(levels[i], 0L);
                        double pct = (double) count / total;
                        int barWidth = (int) (pct * (w - 100));
                        barWidth = Math.max(barWidth, 2);

                        g2.setColor(colors[i]);
                        g2.fillRoundRect(x0 + 80, barY, barWidth, barHeight, 6, 6);

                        g2.setColor(UIHelper.COLOR_TEXT_MAIN);
                        g2.setFont(UIHelper.FONT_BOLD);
                        g2.drawString(levels[i].name(), x0, barY + 20);

                        g2.setFont(UIHelper.FONT_SMALL);
                        g2.drawString(String.format("%d (%.0f%%)", count, pct * 100), x0 + 84 + barWidth, barY + 20);

                        barY += barHeight + gap;
                    }
                } catch (DatabaseException ex) {
                    g2.drawString("Error loading chart data", x0, y0 + 50);
                }
                g2.dispose();
            }
        };
        chart.setOpaque(false);
        return chart;
    }

    // ====================== TAB 4: ML ENGINE ======================

    private JPanel buildMLPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Top Control Bar Card
        JPanel topCard = UIHelper.createCardPanel();
        topCard.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 8));

        mlStatusLabel = new JLabel("Status: Initialized");
        mlStatusLabel.setFont(UIHelper.FONT_BOLD);
        topCard.add(mlStatusLabel);

        topCard.add(Box.createHorizontalStrut(20));

        JLabel weightLbl = new JLabel("Hybrid ML Weight:");
        weightLbl.setFont(UIHelper.FONT_BOLD);
        topCard.add(weightLbl);

        mlWeightSpinner = new JSpinner(new SpinnerNumberModel(engine.getMlWeight(), 0.0, 1.0, 0.05));
        mlWeightSpinner.setFont(UIHelper.FONT_REGULAR);
        mlWeightSpinner.setPreferredSize(new Dimension(75, 28));
        topCard.add(mlWeightSpinner);

        JButton saveWeightBtn = UIHelper.createButton("Save ML Weight", UIHelper.COLOR_PRIMARY, Color.WHITE);
        saveWeightBtn.addActionListener(e -> saveMLWeight());
        topCard.add(saveWeightBtn);

        topCard.add(Box.createHorizontalStrut(20));

        JButton retrainBtn = UIHelper.createSuccessButton("🔄 Retrain ML Model");
        retrainBtn.addActionListener(e -> retrainMLModel());
        topCard.add(retrainBtn);

        panel.add(topCard, BorderLayout.NORTH);

        // Center Split Panel
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        centerPanel.setOpaque(false);

        // Left Card: Model Parameters
        JPanel leftCard = UIHelper.createCardPanel();
        leftCard.setLayout(new BorderLayout(0, 8));

        JLabel weightsTitle = new JLabel("Model Feature Weights & Normalization Parameters");
        weightsTitle.setFont(UIHelper.FONT_SUBTITLE);
        weightsTitle.setForeground(UIHelper.COLOR_PRIMARY);
        leftCard.add(weightsTitle, BorderLayout.NORTH);

        String[] cols = {"Feature Name", "Weight", "Mean", "Std Dev"};
        weightsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        weightsTable = new JTable(weightsModel);
        weightsTable.setRowHeight(24);
        weightsTable.setFont(UIHelper.FONT_REGULAR);
        weightsTable.getTableHeader().setFont(UIHelper.FONT_BOLD);
        leftCard.add(new JScrollPane(weightsTable), BorderLayout.CENTER);

        centerPanel.add(leftCard);

        // Right Card: Evaluation Metrics
        JPanel rightCard = UIHelper.createCardPanel();
        rightCard.setLayout(new BorderLayout(0, 8));

        JLabel metricsTitle = new JLabel("Evaluation Metrics & Confusion Matrix (70/30 Split)");
        metricsTitle.setFont(UIHelper.FONT_SUBTITLE);
        metricsTitle.setForeground(UIHelper.COLOR_PRIMARY);
        rightCard.add(metricsTitle, BorderLayout.NORTH);

        metricsArea = new JTextArea();
        metricsArea.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 12));
        metricsArea.setEditable(false);
        metricsArea.setBackground(new Color(248, 250, 252));
        metricsArea.setForeground(UIHelper.COLOR_TEXT_MAIN);
        metricsArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        rightCard.add(new JScrollPane(metricsArea), BorderLayout.CENTER);

        centerPanel.add(rightCard);

        panel.add(centerPanel, BorderLayout.CENTER);

        refreshMLPanel();
        return panel;
    }

    private void refreshMLPanel() {
        LogisticRegressionModel model = engine.getModel();
        if (model != null && model.isTrained()) {
            mlStatusLabel.setText("● ML Model Active (Plain Java Logistic Regression)");
            mlStatusLabel.setForeground(UIHelper.COLOR_SUCCESS);

            weightsModel.setRowCount(0);
            double[] weights = model.getWeights();
            double[] means = model.getFeatureMeans();
            double[] stds = model.getFeatureStds();

            weightsModel.addRow(new Object[]{"Bias (w0)", String.format("%.4f", weights[0]), "N/A", "N/A"});
            for (int i = 0; i < FeatureExtractor.FEATURE_NAMES.length; i++) {
                weightsModel.addRow(new Object[]{
                        FeatureExtractor.FEATURE_NAMES[i],
                        String.format("%.4f", weights[i + 1]),
                        String.format("%.2f", means[i]),
                        String.format("%.2f", stds[i])
                });
            }

            ModelEvaluator.EvaluationResult metrics = engine.getModelMetrics();
            if (metrics != null) {
                metricsArea.setText(metrics.toFormattedString());
            } else {
                metricsArea.setText("Model trained. Evaluation metrics available upon retraining.");
            }
        } else {
            mlStatusLabel.setText("● Model Untrained");
            mlStatusLabel.setForeground(UIHelper.COLOR_DANGER);
        }
    }

    private void retrainMLModel() {
        new Thread(() -> {
            ModelEvaluator.EvaluationResult result = engine.retrainModel();
            SwingUtilities.invokeLater(() -> {
                refreshMLPanel();
                UIHelper.showInfo(this, "ML Model successfully retrained!\n\n" +
                        String.format("Accuracy:  %.2f%%\nPrecision: %.2f%%\nRecall:    %.2f%%\nF1-Score:  %.4f",
                                result.getAccuracy() * 100, result.getPrecision() * 100,
                                result.getRecall() * 100, result.getF1Score()), "Model Retrained");
            });
        }).start();
    }

    private void saveMLWeight() {
        double weight = (double) mlWeightSpinner.getValue();
        engine.setMlWeight(weight);
        new Thread(() -> {
            try {
                settingsDAO.update("ML_WEIGHT", String.valueOf(weight));
                SwingUtilities.invokeLater(() ->
                        UIHelper.showInfo(this, "ML hybrid weight set to " + weight + " and saved.", "ML Weight Saved")
                );
            } catch (DatabaseException ex) {
                SwingUtilities.invokeLater(() ->
                        UIHelper.showError(this, "Failed to save ML weight: " + ex.getMessage(), "Error")
                );
            }
        }).start();
    }

    // ====================== TAB 4: SETTINGS ======================

    private JPanel buildSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel title = new JLabel("Detection Engine Configuration");
        title.setFont(UIHelper.FONT_HEADER);
        title.setForeground(UIHelper.COLOR_TEXT_MAIN);
        panel.add(title, BorderLayout.NORTH);

        // Main settings form
        JPanel formWrapper = new JPanel(new GridBagLayout());
        formWrapper.setOpaque(false);

        JPanel card = UIHelper.createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(600, 500));

        // Threshold settings
        JLabel threshTitle = new JLabel("Threshold Configuration");
        threshTitle.setFont(UIHelper.FONT_SUBTITLE);
        threshTitle.setForeground(UIHelper.COLOR_PRIMARY);
        threshTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(threshTitle);
        card.add(Box.createVerticalStrut(8));

        JPanel threshGrid = new JPanel(new GridBagLayout());
        threshGrid.setOpaque(false);
        threshGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints lbl = new GridBagConstraints();
        lbl.anchor = GridBagConstraints.WEST;
        lbl.insets = new Insets(4, 0, 4, 10);
        lbl.gridx = 0;

        GridBagConstraints fld = new GridBagConstraints();
        fld.fill = GridBagConstraints.HORIZONTAL;
        fld.weightx = 1.0;
        fld.insets = new Insets(4, 0, 4, 0);
        fld.gridx = 1;

        int row = 0;

        highAmountField = new JTextField(); highAmountField.setFont(UIHelper.FONT_REGULAR);
        lbl.gridy = row; fld.gridy = row++;
        threshGrid.add(settingsLabel("High Amount Threshold (₹)"), lbl);
        threshGrid.add(highAmountField, fld);

        velocityMaxField = new JTextField(); velocityMaxField.setFont(UIHelper.FONT_REGULAR);
        lbl.gridy = row; fld.gridy = row++;
        threshGrid.add(settingsLabel("Velocity Max Transactions"), lbl);
        threshGrid.add(velocityMaxField, fld);

        velocityWindowField = new JTextField(); velocityWindowField.setFont(UIHelper.FONT_REGULAR);
        lbl.gridy = row; fld.gridy = row++;
        threshGrid.add(settingsLabel("Velocity Window (minutes)"), lbl);
        threshGrid.add(velocityWindowField, fld);

        zScoreField = new JTextField(); zScoreField.setFont(UIHelper.FONT_REGULAR);
        lbl.gridy = row; fld.gridy = row++;
        threshGrid.add(settingsLabel("Z-Score Threshold"), lbl);
        threshGrid.add(zScoreField, fld);

        mediumCutoffField = new JTextField(); mediumCutoffField.setFont(UIHelper.FONT_REGULAR);
        lbl.gridy = row; fld.gridy = row++;
        threshGrid.add(settingsLabel("Medium Risk Cutoff Score"), lbl);
        threshGrid.add(mediumCutoffField, fld);

        highCutoffField = new JTextField(); highCutoffField.setFont(UIHelper.FONT_REGULAR);
        lbl.gridy = row; fld.gridy = row++;
        threshGrid.add(settingsLabel("High Risk Cutoff Score"), lbl);
        threshGrid.add(highCutoffField, fld);

        card.add(threshGrid);
        card.add(Box.createVerticalStrut(16));

        // Rule enable/disable checkboxes
        JLabel rulesTitle = new JLabel("Rule Activation");
        rulesTitle.setFont(UIHelper.FONT_SUBTITLE);
        rulesTitle.setForeground(UIHelper.COLOR_PRIMARY);
        rulesTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(rulesTitle);
        card.add(Box.createVerticalStrut(8));

        JPanel rulesPanel = new JPanel(new GridLayout(4, 2, 8, 4));
        rulesPanel.setOpaque(false);
        rulesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        ruleHighAmountCb = new JCheckBox("High Amount Rule"); ruleHighAmountCb.setFont(UIHelper.FONT_REGULAR); ruleHighAmountCb.setOpaque(false);
        ruleVelocityCb = new JCheckBox("Velocity Rule"); ruleVelocityCb.setFont(UIHelper.FONT_REGULAR); ruleVelocityCb.setOpaque(false);
        ruleAnomalyCb = new JCheckBox("Statistical Anomaly Rule"); ruleAnomalyCb.setFont(UIHelper.FONT_REGULAR); ruleAnomalyCb.setOpaque(false);
        ruleTimeCb = new JCheckBox("Unusual Time Rule"); ruleTimeCb.setFont(UIHelper.FONT_REGULAR); ruleTimeCb.setOpaque(false);
        ruleLocationCb = new JCheckBox("New Location Rule"); ruleLocationCb.setFont(UIHelper.FONT_REGULAR); ruleLocationCb.setOpaque(false);
        ruleRoundCb = new JCheckBox("Round Amount Rule"); ruleRoundCb.setFont(UIHelper.FONT_REGULAR); ruleRoundCb.setOpaque(false);
        ruleRepeatCb = new JCheckBox("Rapid Repeat Rule"); ruleRepeatCb.setFont(UIHelper.FONT_REGULAR); ruleRepeatCb.setOpaque(false);

        rulesPanel.add(ruleHighAmountCb);
        rulesPanel.add(ruleVelocityCb);
        rulesPanel.add(ruleAnomalyCb);
        rulesPanel.add(ruleTimeCb);
        rulesPanel.add(ruleLocationCb);
        rulesPanel.add(ruleRoundCb);
        rulesPanel.add(ruleRepeatCb);

        card.add(rulesPanel);
        card.add(Box.createVerticalStrut(16));

        // Save Button
        JButton saveBtn = UIHelper.createSuccessButton("💾  Save Settings & Reload Engine");
        saveBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        saveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveBtn.addActionListener(e -> saveSettings());
        card.add(saveBtn);

        formWrapper.add(card);
        panel.add(formWrapper, BorderLayout.CENTER);

        // Load current settings values
        loadSettings();

        return panel;
    }

    private JLabel settingsLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UIHelper.FONT_BOLD);
        label.setForeground(UIHelper.COLOR_TEXT_MAIN);
        return label;
    }

    /**
     * Loads current settings from the database into the form fields.
     */
    private void loadSettings() {
        new Thread(() -> {
            try {
                Map<String, String> settings = settingsDAO.getAll();
                SwingUtilities.invokeLater(() -> {
                    highAmountField.setText(settings.getOrDefault("HIGH_AMOUNT_THRESHOLD", "50000"));
                    velocityMaxField.setText(settings.getOrDefault("VELOCITY_MAX_TXNS", "5"));
                    velocityWindowField.setText(settings.getOrDefault("VELOCITY_WINDOW_MIN", "10"));
                    zScoreField.setText(settings.getOrDefault("ZSCORE_THRESHOLD", "3.0"));
                    mediumCutoffField.setText(settings.getOrDefault("MEDIUM_RISK_CUTOFF", "40"));
                    highCutoffField.setText(settings.getOrDefault("HIGH_RISK_CUTOFF", "70"));

                    ruleHighAmountCb.setSelected(Boolean.parseBoolean(settings.getOrDefault("RULE_HIGH_AMOUNT_ENABLED", "true")));
                    ruleVelocityCb.setSelected(Boolean.parseBoolean(settings.getOrDefault("RULE_VELOCITY_ENABLED", "true")));
                    ruleAnomalyCb.setSelected(Boolean.parseBoolean(settings.getOrDefault("RULE_ANOMALY_ENABLED", "true")));
                    ruleTimeCb.setSelected(Boolean.parseBoolean(settings.getOrDefault("RULE_TIME_ENABLED", "true")));
                    ruleLocationCb.setSelected(Boolean.parseBoolean(settings.getOrDefault("RULE_LOCATION_ENABLED", "true")));
                    ruleRoundCb.setSelected(Boolean.parseBoolean(settings.getOrDefault("RULE_ROUND_ENABLED", "true")));
                    ruleRepeatCb.setSelected(Boolean.parseBoolean(settings.getOrDefault("RULE_REPEAT_ENABLED", "true")));
                });
            } catch (DatabaseException ex) {
                SwingUtilities.invokeLater(() ->
                        UIHelper.showError(this, "Failed to load settings: " + ex.getMessage(), "Settings Error")
                );
            }
        }).start();
    }

    /**
     * Saves all settings to the database and calls engine.reloadSettings()
     * to apply changes live without restarting the application.
     */
    private void saveSettings() {
        new Thread(() -> {
            try {
                // Validate numeric inputs before saving
                String highAmt = highAmountField.getText().trim();
                String velMax = velocityMaxField.getText().trim();
                String velWin = velocityWindowField.getText().trim();
                String zScore = zScoreField.getText().trim();
                String medCut = mediumCutoffField.getText().trim();
                String highCut = highCutoffField.getText().trim();

                // Basic numeric validation
                try {
                    Double.parseDouble(highAmt);
                    Integer.parseInt(velMax);
                    Integer.parseInt(velWin);
                    Double.parseDouble(zScore);
                    Integer.parseInt(medCut);
                    Integer.parseInt(highCut);
                } catch (NumberFormatException nfe) {
                    SwingUtilities.invokeLater(() ->
                            UIHelper.showError(this, "All threshold fields must contain valid numbers.", "Validation Error")
                    );
                    return;
                }

                // Persist each setting through SettingsDAO (PreparedStatement)
                settingsDAO.update("HIGH_AMOUNT_THRESHOLD", highAmt);
                settingsDAO.update("VELOCITY_MAX_TXNS", velMax);
                settingsDAO.update("VELOCITY_WINDOW_MIN", velWin);
                settingsDAO.update("ZSCORE_THRESHOLD", zScore);
                settingsDAO.update("MEDIUM_RISK_CUTOFF", medCut);
                settingsDAO.update("HIGH_RISK_CUTOFF", highCut);

                settingsDAO.update("RULE_HIGH_AMOUNT_ENABLED", String.valueOf(ruleHighAmountCb.isSelected()));
                settingsDAO.update("RULE_VELOCITY_ENABLED", String.valueOf(ruleVelocityCb.isSelected()));
                settingsDAO.update("RULE_ANOMALY_ENABLED", String.valueOf(ruleAnomalyCb.isSelected()));
                settingsDAO.update("RULE_TIME_ENABLED", String.valueOf(ruleTimeCb.isSelected()));
                settingsDAO.update("RULE_LOCATION_ENABLED", String.valueOf(ruleLocationCb.isSelected()));
                settingsDAO.update("RULE_ROUND_ENABLED", String.valueOf(ruleRoundCb.isSelected()));
                settingsDAO.update("RULE_REPEAT_ENABLED", String.valueOf(ruleRepeatCb.isSelected()));

                // Reload the detection engine with the new settings
                engine.reloadSettings();

                SwingUtilities.invokeLater(() ->
                        UIHelper.showInfo(this, "Settings saved and detection engine reloaded successfully.", "Settings Updated")
                );
            } catch (DatabaseException ex) {
                SwingUtilities.invokeLater(() ->
                        UIHelper.showError(this, "Failed to save settings: " + ex.getMessage(), "Database Error")
                );
            }
        }).start();
    }

    /**
     * Logs out and shuts down background services.
     */
    private void logout() {
        stopAutoRefresh();
        dispose();
        UIHelper.showInfo(null, "You have been logged out.", "Logged Out");
    }
}
