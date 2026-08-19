/*
 * AlpCatalogStyles.java
 *
 * ALP CAD — shared look-and-feel for the left library column (SPIKE-22).
 */
package com.eteks.sweethome3d.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * Soft palette and chip/card styling for catalog and inventory panels.
 */
public final class AlpCatalogStyles {
  private static final Color PANEL_BACKGROUND        = new Color(0xF4F6F2);
  private static final Color CATEGORY_BACKGROUND     = new Color(0xE4EBE0);
  private static final Color CATEGORY_SELECTED       = new Color(0xD2DDC8);
  private static final Color CHIP_BACKGROUND         = Color.WHITE;
  private static final Color CHIP_SELECTED         = new Color(0xE8EEF4);
  private static final Color CHIP_BORDER             = new Color(0xC8CEC4);
  private static final Color CHIP_SELECTED_BORDER    = new Color(0x98A8B8);
  private static final Color TABLE_GRID              = new Color(0xD8DED4);
  private static final Color TABLE_HEADER_BACKGROUND = new Color(0xECEFE8);
  private static final Color TAB_TEXT_SELECTED       = new Color(0x2A2A2A);
  private static final Color TAB_TEXT_UNSELECTED     = new Color(0x5A5A5A);

  private AlpCatalogStyles() {
  }

  public static int scale(int value) {
    return Math.round(value * SwingTools.getResolutionScale());
  }

  public static Color panelBackground() {
    return PANEL_BACKGROUND;
  }

  public static Color categoryBackground(boolean selected) {
    return selected ? CATEGORY_SELECTED : CATEGORY_BACKGROUND;
  }

  public static Border categoryBorder() {
    int padding = scale(6);
    return new EmptyBorder(padding, scale(8), padding, scale(8));
  }

  /** Vertical gap between category rows where panel background shows through. */
  public static int categoryRowGap() {
    return scale(3);
  }

  /** Horizontal inset for category chip rows. */
  public static int categoryRowSideInset() {
    return scale(4);
  }

  public static Border filterRowBorder() {
    return new EmptyBorder(scale(6), scale(8), scale(4), scale(8));
  }

  public static String formatCategoryLabel(String name, int count) {
    return name + " (" + count + ")";
  }

  public static void applyCatalogPanel(JPanel panel) {
    applyWorkspacePanel(panel);
  }

  /** Applies the shared ALP soft workspace background to a panel. */
  public static void applyWorkspacePanel(JComponent component) {
    component.setBackground(PANEL_BACKGROUND);
    if (component instanceof JPanel || component instanceof JLabel) {
      component.setOpaque(true);
    }
  }

  public static void applyCatalogScrollPane(JScrollPane scrollPane) {
    scrollPane.getViewport().setBackground(PANEL_BACKGROUND);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.setBackground(PANEL_BACKGROUND);
  }

  public static void applyCatalogTree(JTree tree) {
    tree.setBackground(PANEL_BACKGROUND);
    tree.setOpaque(true);
    if (tree.getRowHeight() > 0) {
      tree.setRowHeight(tree.getRowHeight() + categoryRowGap() * 2);
    }
  }

  public static void applyInventoryTabbedPane(JTabbedPane tabbedPane) {
    tabbedPane.setBackground(PANEL_BACKGROUND);
    tabbedPane.putClientProperty("JTabbedPane.tabSelectedForeground", TAB_TEXT_SELECTED);
    tabbedPane.putClientProperty("JTabbedPane.tabUnselectedForeground", TAB_TEXT_UNSELECTED);
    tabbedPane.addChangeListener(ev -> updateInventoryTabColors((JTabbedPane) ev.getSource()));
    updateInventoryTabColors(tabbedPane);
  }

  public static void updateInventoryTabColors(JTabbedPane tabbedPane) {
    int selectedIndex = tabbedPane.getSelectedIndex();
    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
      tabbedPane.setForegroundAt(i, i == selectedIndex ? TAB_TEXT_SELECTED : TAB_TEXT_UNSELECTED);
    }
  }

  public static void paintCategoryRowBackground(Graphics g, int x, int y, int width, int height, boolean selected) {
    if (width <= 0 || height <= 0) {
      return;
    }
    Graphics2D g2D = (Graphics2D)g.create();
    try {
      g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2D.setColor(categoryBackground(selected));
      int arc = scale(8);
      g2D.fillRoundRect(x, y, width, height, arc, arc);
      g2D.setColor(CHIP_BORDER);
      g2D.drawRoundRect(x, y, width - 1, height - 1, arc, arc);
    } finally {
      g2D.dispose();
    }
  }

  public static void applyInventoryTable(JTable table) {
    table.setBackground(Color.WHITE);
    table.setGridColor(TABLE_GRID);
    table.setShowVerticalLines(false);
    table.setIntercellSpacing(new java.awt.Dimension(0, scale(1)));
    table.setRowHeight(Math.max(table.getRowHeight(), scale(22)));
    if (table.getTableHeader() != null) {
      table.getTableHeader().setBackground(TABLE_HEADER_BACKGROUND);
      table.getTableHeader().setReorderingAllowed(false);
    }
  }

  public static void paintChipBackground(Graphics g, int x, int y, int width, int height, boolean selected) {
    Graphics2D g2D = (Graphics2D)g.create();
    try {
      g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2D.setColor(selected ? CHIP_SELECTED : CHIP_BACKGROUND);
      int arc = scale(8);
      g2D.fillRoundRect(x, y, width, height, arc, arc);
      g2D.setColor(selected ? CHIP_SELECTED_BORDER : CHIP_BORDER);
      g2D.drawRoundRect(x, y, width - 1, height - 1, arc, arc);
    } finally {
      g2D.dispose();
    }
  }

  public static Insets chipInsets() {
    int padding = scale(4);
    return new Insets(padding, padding, padding, padding);
  }

  /**
   * Paints chip background behind a catalog list cell renderer.
   */
  public static void paintListChipBackground(Component renderer, Graphics g, boolean selected) {
    Insets insets = chipInsets();
    int x = insets.left / 2;
    int y = insets.top / 2;
    int width = renderer.getWidth() - x * 2;
    int height = renderer.getHeight() - y * 2;
    if (width > 0 && height > 0) {
      paintChipBackground(g, x, y, width, height, selected);
    }
  }
}
