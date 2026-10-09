package com.frauddetect.gui;

import com.formdev.flatlaf.FlatLightLaf;
import com.frauddetect.model.RiskLevel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * UI Design System and Helper Utilities for Swing GUI.
 * Provides curated modern color palettes, typography, custom card components,
 * and badge table cell renderers.
 */
public final class UIHelper {

    // Color Palette
    public static final Color COLOR_PRIMARY = new Color(37, 99, 235);       // Royal Blue
    public static final Color COLOR_PRIMARY_DARK = new Color(29, 78, 216);
    public static final Color COLOR_HEADER_BG = new Color(15, 23, 42);       // Dark Slate Navy
    public static final Color COLOR_BG = new Color(248, 250, 252);           // Soft Slate Background
    public static final Color COLOR_CARD_BG = Color.WHITE;
    public static final Color COLOR_BORDER = new Color(226, 232, 240);
    public static final Color COLOR_TEXT_MAIN = new Color(30, 41, 59);
    public static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);

    // Risk & Status Colors
    public static final Color COLOR_SUCCESS = new Color(22, 163, 74);        // Green (LOW / APPROVED)
    public static final Color COLOR_WARNING = new Color(217, 119, 6);        // Amber (MEDIUM / FLAGGED)
    public static final Color COLOR_DANGER = new Color(220, 38, 38);         // Red (HIGH / BLOCKED)
    public static final Color COLOR_PENDING = new Color(100, 116, 139);      // Muted Grey

    // Typography
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_METRIC = new Font("Segoe UI", Font.BOLD, 24);

    private UIHelper() {}

    /**
     * Installs the modern FlatLaf theme.
     */
    public static void setupTheme() {
        try {
            FlatLightLaf.setup();
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("TabbedPane.showTabSeparators", true);
            UIManager.put("Table.rowHeight", 32);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("Table.gridColor", new Color(241, 245, 249));
            UIManager.put("TableHeader.height", 34);
            UIManager.put("TableHeader.font", FONT_BOLD);
        } catch (Exception e) {
            System.err.println("Note: FlatLaf setup fallback: " + e.getMessage());
        }
    }

    /**
     * Creates a styled primary or action button.
     */
    public static JButton createButton(String text, Color bgColor, Color fgColor) {
        JButton button = new JButton(text);
        button.setFont(FONT_BOLD);
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(8, 16, 8, 16));
        return button;
    }

    public static JButton createPrimaryButton(String text) {
        return createButton(text, COLOR_PRIMARY, Color.WHITE);
    }

    public static JButton createDangerButton(String text) {
        return createButton(text, COLOR_DANGER, Color.WHITE);
    }

    public static JButton createSuccessButton(String text) {
        return createButton(text, COLOR_SUCCESS, Color.WHITE);
    }

    /**
     * Creates a rounded card container panel.
     */
    public static JPanel createCardPanel() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBackground(COLOR_CARD_BG);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        return card;
    }

    /**
     * Creates a modern top header panel.
     */
    public static JPanel createHeader(String title, String subtitle, String userRoleBadge, JButton actionButton) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_HEADER_BG);
        header.setBorder(new EmptyBorder(14, 20, 14, 20));

        JPanel leftPanel = new JPanel(new BorderLayout(0, 2));
        leftPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(Color.WHITE);

        JLabel subLabel = new JLabel(subtitle);
        subLabel.setFont(FONT_SMALL);
        subLabel.setForeground(new Color(148, 163, 184));

        leftPanel.add(titleLabel, BorderLayout.NORTH);
        leftPanel.add(subLabel, BorderLayout.SOUTH);

        header.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        if (userRoleBadge != null && !userRoleBadge.isBlank()) {
            JLabel badge = new JLabel("  " + userRoleBadge + "  ");
            badge.setOpaque(true);
            badge.setBackground(new Color(30, 41, 59));
            badge.setForeground(new Color(226, 232, 240));
            badge.setFont(FONT_BOLD);
            badge.setBorder(new LineBorder(new Color(51, 65, 85), 1, true));
            rightPanel.add(badge);
        }

        if (actionButton != null) {
            rightPanel.add(actionButton);
        }

        header.add(rightPanel, BorderLayout.EAST);
        return header;
    }

    /**
     * Creates a KPI statistic card with a colored accent bar.
     */
    public static JPanel createStatCard(String title, String value, Color accentColor, String subtitle) {
        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout(0, 6));

        JLabel titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setFont(FONT_SMALL);
        titleLabel.setForeground(COLOR_TEXT_MUTED);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(FONT_METRIC);
        valueLabel.setForeground(accentColor);

        JLabel subLabel = new JLabel(subtitle != null ? subtitle : "");
        subLabel.setFont(FONT_SMALL);
        subLabel.setForeground(COLOR_TEXT_MUTED);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(subLabel, BorderLayout.SOUTH);

        card.setPreferredSize(new Dimension(180, 95));
        return card;
    }

    /**
     * Configures modern cell rendering with custom status and risk badges for JTable.
     */
    public static void formatTable(JTable table) {
        table.setFillsViewportHeight(true);
        table.setShowGrid(true);
        table.setGridColor(new Color(241, 245, 249));
        table.setSelectionBackground(new Color(224, 231, 255));
        table.setSelectionForeground(COLOR_TEXT_MAIN);

        DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }
                if (c instanceof JLabel) {
                    ((JLabel) c).setBorder(new EmptyBorder(0, 8, 0, 8));
                }
                return c;
            }
        };

        DefaultTableCellRenderer badgeRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setOpaque(true);

                String text = (value != null) ? value.toString() : "";
                if ("HIGH".equalsIgnoreCase(text) || "BLOCKED".equalsIgnoreCase(text)) {
                    label.setBackground(new Color(254, 226, 226)); // Soft Red
                    label.setForeground(COLOR_DANGER);
                    label.setFont(FONT_BOLD);
                } else if ("MEDIUM".equalsIgnoreCase(text) || "FLAGGED".equalsIgnoreCase(text)) {
                    label.setBackground(new Color(254, 243, 199)); // Soft Amber
                    label.setForeground(COLOR_WARNING);
                    label.setFont(FONT_BOLD);
                } else if ("LOW".equalsIgnoreCase(text) || "APPROVED".equalsIgnoreCase(text)) {
                    label.setBackground(new Color(220, 252, 231)); // Soft Green
                    label.setForeground(COLOR_SUCCESS);
                    label.setFont(FONT_BOLD);
                } else if ("PENDING".equalsIgnoreCase(text)) {
                    label.setBackground(new Color(241, 245, 249)); // Soft Grey
                    label.setForeground(COLOR_PENDING);
                    label.setFont(FONT_BOLD);
                } else {
                    label.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    label.setForeground(COLOR_TEXT_MAIN);
                }
                return label;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            String colName = table.getColumnName(i).toLowerCase();
            if (colName.contains("risk") || colName.contains("status") || colName.contains("level")) {
                table.getColumnModel().getColumn(i).setCellRenderer(badgeRenderer);
            } else {
                table.getColumnModel().getColumn(i).setCellRenderer(defaultRenderer);
            }
        }
    }

    /**
     * Safely displays an error alert via JOptionPane on the Event Dispatch Thread.
     */
    public static void showError(Component parent, String message, String title) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(parent, message, title != null ? title : "System Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    public static void showInfo(Component parent, String message, String title) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(parent, message, title != null ? title : "Information", JOptionPane.INFORMATION_MESSAGE)
        );
    }

    public static void showWarning(Component parent, String message, String title) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(parent, message, title != null ? title : "Warning", JOptionPane.WARNING_MESSAGE)
        );
    }
}
