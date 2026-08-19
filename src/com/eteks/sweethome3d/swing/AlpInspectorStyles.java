/*
 * AlpInspectorStyles.java
 *
 * Sweet Home 3D, Copyright (c) 2024 Space Mushrooms <info@sweethome3d.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */
package com.eteks.sweethome3d.swing;

import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.border.Border;

/**
 * Shared layout and typography for ALP docked inspector panes (SPIKE-21).
 */
public final class AlpInspectorStyles {
  private AlpInspectorStyles() {
  }

  public static int scale(int value) {
    return Math.round(value * SwingTools.getResolutionScale());
  }

  public static Insets panelInsets() {
    int padding = scale(10);
    return new Insets(padding, padding, padding, padding);
  }

  public static Border emptyStateBorder() {
    return BorderFactory.createEmptyBorder(scale(20), scale(14), scale(20), scale(14));
  }

  public static Border sectionGapBorder() {
    return BorderFactory.createEmptyBorder(0, 0, scale(8), 0);
  }

  public static void applySummaryText(JLabel label, String title, String subtitle) {
    StringBuilder html = new StringBuilder("<html><b>");
    html.append(escapeHtml(title));
    html.append("</b>");
    if (subtitle != null && subtitle.length() > 0) {
      html.append("<br><font color='#666666' size='-1'>");
      html.append(escapeHtml(subtitle));
      html.append("</font>");
    }
    html.append("</html>");
    label.setText(html.toString());
  }

  private static String escapeHtml(String text) {
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
