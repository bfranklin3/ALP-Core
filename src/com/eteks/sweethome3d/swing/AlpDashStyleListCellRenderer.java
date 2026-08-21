/*
 * AlpDashStyleListCellRenderer.java
 *
 * ALP CAD — dash style combo preview icons (SPIKE-34).
 */
package com.eteks.sweethome3d.swing;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JList;

import com.eteks.sweethome3d.j3d.ShapeTools;
import com.eteks.sweethome3d.model.Polyline;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.tools.OperatingSystem;

/**
 * Renders a dash style combo item with a line preview icon and localized label.
 */
final class AlpDashStyleListCellRenderer extends DefaultListCellRenderer {

  /**
   * Optional live source for dash offset and custom pattern (full polyline editor).
   */
  interface DashStylePreviewSource {
    float getDashOffset();
    float [] getCustomPattern();
  }

  private final UserPreferences         preferences;
  private final DashStylePreviewSource  previewSource;
  private final float                   dashOffset;
  private final float []                customPattern;
  private final float                   resolutionScale;

  AlpDashStyleListCellRenderer(UserPreferences preferences) {
    this(preferences, null, 0f, null);
  }

  AlpDashStyleListCellRenderer(UserPreferences preferences,
                               DashStylePreviewSource previewSource) {
    this(preferences, previewSource, 0f, null);
  }

  private AlpDashStyleListCellRenderer(UserPreferences preferences,
                                       DashStylePreviewSource previewSource,
                                       float dashOffset,
                                       float [] customPattern) {
    this.preferences = preferences;
    this.previewSource = previewSource;
    this.dashOffset = dashOffset;
    this.customPattern = customPattern;
    this.resolutionScale = SwingTools.getResolutionScale();
  }

  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index,
      boolean isSelected, boolean cellHasFocus) {
    Polyline.DashStyle dashStyle = (Polyline.DashStyle)value;
    String text = AlpDashStyleSupport.getDisplayName(this.preferences, dashStyle);
    Component component = super.getListCellRendererComponent(
        list, text, index, isSelected, cellHasFocus);
    if (dashStyle != null) {
      float offset = this.previewSource != null
          ? this.previewSource.getDashOffset()
          : this.dashOffset;
      float [] pattern = this.previewSource != null
          ? this.previewSource.getCustomPattern()
          : this.customPattern;
      setIcon(new DashStylePreviewIcon(dashStyle, offset, pattern,
          this.resolutionScale, list.getForeground()));
    } else {
      setIcon(null);
    }
    return component;
  }

  private static final class DashStylePreviewIcon implements Icon {
    private final Polyline.DashStyle dashStyle;
    private final float              dashOffset;
    private final float []           customPattern;
    private final float              resolutionScale;
    private final java.awt.Color     lineColor;

    DashStylePreviewIcon(Polyline.DashStyle dashStyle,
                         float dashOffset,
                         float [] customPattern,
                         float resolutionScale,
                         java.awt.Color lineColor) {
      this.dashStyle = dashStyle;
      this.dashOffset = dashOffset;
      this.customPattern = customPattern;
      this.resolutionScale = resolutionScale;
      this.lineColor = lineColor;
    }

    public int getIconWidth() {
      return Math.round(64 * this.resolutionScale);
    }

    public int getIconHeight() {
      return Math.round(16 * this.resolutionScale);
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
      Graphics2D g2D = (Graphics2D)g.create();
      int logicalWidth = Math.round(getIconWidth() / this.resolutionScale);
      int logicalHeight = Math.round(getIconHeight() / this.resolutionScale);
      g2D.translate(x, y);
      g2D.scale(this.resolutionScale, this.resolutionScale);
      if (OperatingSystem.isMacOSXLeopardOrSuperior()) {
        g2D.translate(0, 2);
      }
      // Clip so partial dash segments at the pattern wrap do not bleed into the label
      g2D.clipRect(0, 0, logicalWidth, logicalHeight);
      g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2D.setColor(this.lineColor);
      float [] dashPattern = this.dashStyle != Polyline.DashStyle.CUSTOMIZED
          ? this.dashStyle.getDashPattern()
          : this.customPattern;
      Stroke stroke = ShapeTools.getStroke(2, Polyline.CapStyle.BUTT, Polyline.JoinStyle.MITER,
          dashPattern, this.dashOffset);
      g2D.setStroke(stroke);
      g2D.drawLine(4, 8, logicalWidth - 4, 8);
      g2D.dispose();
    }
  }
}
