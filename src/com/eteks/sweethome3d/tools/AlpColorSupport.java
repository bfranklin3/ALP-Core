/*
 * AlpColorSupport.java
 *
 * ALP CAD helpers for transparent / none color handling.
 */
package com.eteks.sweethome3d.tools;

import java.awt.Color;
import java.awt.Graphics;

/**
 * Utilities for ALP transparent color sentinel ({@code 0x00000000}).
 */
public final class AlpColorSupport {
  /** Sentinel stored in model/XML for explicit no-color (fill or outline). */
  public static final Integer TRANSPARENT_COLOR = 0;

  private AlpColorSupport() {
  }

  /**
   * Returns whether {@code color} is the transparent sentinel (alpha channel is 0).
   */
  public static boolean isTransparentColor(Integer color) {
    return color != null && (color.intValue() & 0xFF000000) == 0;
  }

  /**
   * Returns whether {@code rgb} is the transparent sentinel (alpha channel is 0).
   */
  public static boolean isTransparentRgb(int rgb) {
    return (rgb & 0xFF000000) == 0;
  }

  /**
   * Paints the standard none swatch: white fill, gray border, red diagonal.
   */
  public static void paintTransparentColorIcon(Graphics g, int x, int y, int width, int height) {
    g.setColor(Color.WHITE);
    g.fillRect(x, y, width, height);
    g.setColor(Color.GRAY);
    g.drawRect(x, y, width - 1, height - 1);
    g.setColor(Color.RED);
    g.drawLine(x + 2, y + 2, x + width - 3, y + height - 3);
  }
}
