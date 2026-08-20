/*
 * AlpLevelTabComponent.java
 *
 * ALP CAD — custom layer tab chrome (SPIKE-31).
 */
package com.eteks.sweethome3d.swing;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.Level;
import com.eteks.sweethome3d.model.LevelCategory;
import com.eteks.sweethome3d.model.UserPreferences;

/**
 * Tab header for a plan layer: truncated name, lock/hidden glyphs, category strip.
 */
public final class AlpLevelTabComponent extends JPanel {
  private static ImageIcon lockIcon;
  private static ImageIcon hiddenIcon;

  private final Home             home;
  private final JComponent       planComponent;
  private final UserPreferences  preferences;
  private final JLabel           nameLabel;
  private final JLabel           lockLabel;
  private final JLabel           hiddenLabel;
  private final JPanel           categoryStrip;
  private final ImageIcon        sameElevationIcon;
  private boolean                selected;

  public AlpLevelTabComponent(Home home,
                              JComponent planComponent,
                              UserPreferences preferences,
                              ImageIcon sameElevationIcon) {
    super(new BorderLayout(0, 0));
    this.home = home;
    this.planComponent = planComponent;
    this.preferences = preferences;
    this.sameElevationIcon = sameElevationIcon;
    this.nameLabel = createNameLabel();
    this.lockLabel = createGlyphLabel(getLockIcon());
    this.hiddenLabel = createGlyphLabel(getHiddenIcon());
    this.categoryStrip = new JPanel();
    this.categoryStrip.setPreferredSize(new Dimension(0, AlpCatalogStyles.scale(2)));
    this.categoryStrip.setMinimumSize(new Dimension(0, AlpCatalogStyles.scale(2)));
    this.categoryStrip.setOpaque(true);

    JPanel contentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, AlpCatalogStyles.scale(2), 0));
    contentPanel.setOpaque(false);
    contentPanel.add(this.nameLabel);
    contentPanel.add(this.lockLabel);
    contentPanel.add(this.hiddenLabel);

    add(contentPanel, BorderLayout.CENTER);
    add(this.categoryStrip, BorderLayout.SOUTH);
    setOpaque(true);
    setToolTipText("");
  }

  /**
   * Refreshes tab content from the given level and tab index.
   */
  public void updateLevel(Level level, List<Level> levels, int index, boolean selected) {
    this.selected = selected;
    String name = level.getName();
    this.nameLabel.setText(truncateName(name));
    this.nameLabel.setToolTipText(name);
    this.nameLabel.setEnabled(level.isViewable());
    this.nameLabel.setIcon(index > 0
        && levels.get(index - 1).getElevation() == level.getElevation()
        ? this.sameElevationIcon
        : null);

    this.lockLabel.setVisible(level.isLocked());
    this.hiddenLabel.setVisible(!level.isViewable());

    LevelCategory category = level.getCategory();
    if (category == null) {
      category = LevelCategory.GENERAL;
    }
    this.categoryStrip.setBackground(AlpCatalogStyles.levelCategoryStripColor(category));

    StringBuilder tooltip = new StringBuilder(name);
    if (level.isLocked()) {
      tooltip.append("\n").append(getLockedTooltip());
    }
    if (!level.isViewable()) {
      tooltip.append("\n").append(getHiddenTooltip());
    }
    setToolTipText(tooltip.toString());

    AlpCatalogStyles.applyLevelTabComponent(this, selected);
    this.nameLabel.setForeground(AlpCatalogStyles.levelTabTextColor(selected));
    revalidate();
    repaint();
  }

  public void setTabSelected(boolean selected) {
    if (this.selected != selected) {
      this.selected = selected;
      AlpCatalogStyles.applyLevelTabComponent(this, selected);
      this.nameLabel.setForeground(AlpCatalogStyles.levelTabTextColor(selected));
      repaint();
    }
  }

  private JLabel createNameLabel() {
    return new JLabel() {
        @Override
        protected void paintComponent(Graphics g) {
          if (home.isAllLevelsSelection() && isEnabled()) {
            Graphics2D g2D = (Graphics2D)g;
            g2D.setPaint(PlanComponent.getDefaultSelectionColor(planComponent));
            Composite oldComposite = g2D.getComposite();
            g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            Font font = getFont();
            FontMetrics fontMetrics = getFontMetrics(font);
            float strokeWidth = fontMetrics.getHeight() * 0.125f;
            g2D.setStroke(new BasicStroke(strokeWidth));
            FontRenderContext fontRenderContext = g2D.getFontRenderContext();
            TextLayout textLayout = new TextLayout(getText(), font, fontRenderContext);
            AffineTransform oldTransform = g2D.getTransform();
            if (getIcon() != null) {
              g2D.translate(getIcon().getIconWidth() + getIconTextGap(), 0);
            }
            g2D.draw(textLayout.getOutline(AffineTransform.getTranslateInstance(-strokeWidth / 5,
                (getHeight() - fontMetrics.getHeight()) / 2 + fontMetrics.getAscent() - strokeWidth / 5)));
            g2D.setComposite(oldComposite);
            g2D.setTransform(oldTransform);
          }
          super.paintComponent(g);
        }
      };
  }

  private static JLabel createGlyphLabel(ImageIcon icon) {
    JLabel label = new JLabel(icon);
    label.setVisible(false);
    label.setOpaque(false);
    return label;
  }

  private String truncateName(String name) {
    int maxWidth = AlpCatalogStyles.scale(96);
    FontMetrics fontMetrics = this.nameLabel.getFontMetrics(this.nameLabel.getFont());
    if (fontMetrics.stringWidth(name) <= maxWidth) {
      return name;
    }
    String ellipsis = "...";
    for (int i = name.length() - 1; i > 0; i--) {
      String candidate = name.substring(0, i) + ellipsis;
      if (fontMetrics.stringWidth(candidate) <= maxWidth) {
        return candidate;
      }
    }
    return ellipsis;
  }

  private String getLockedTooltip() {
    try {
      return this.preferences.getLocalizedString(AlpLevelTabComponent.class, "lockedTooltip");
    } catch (IllegalArgumentException ex) {
      return "Layer locked";
    }
  }

  private String getHiddenTooltip() {
    try {
      return this.preferences.getLocalizedString(AlpLevelTabComponent.class, "hiddenTooltip");
    } catch (IllegalArgumentException ex) {
      return "Layer hidden";
    }
  }

  private static ImageIcon getLockIcon() {
    if (lockIcon == null) {
      lockIcon = SwingTools.getScaledImageIcon(
          AlpLevelTabComponent.class.getResource("resources/actions/plan-locked.png"));
    }
    return lockIcon;
  }

  private static ImageIcon getHiddenIcon() {
    if (hiddenIcon == null) {
      hiddenIcon = SwingTools.getScaledImageIcon(
          AlpLevelTabComponent.class.getResource("resources/actions/plan-make-level-unviewable.png"));
    }
    return hiddenIcon;
  }
}
