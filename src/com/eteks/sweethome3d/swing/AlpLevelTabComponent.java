/*
 * AlpLevelTabComponent.java
 *
 * ALP CAD — custom layer tab chrome (SPIKE-31).
 */
package com.eteks.sweethome3d.swing;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

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
  private final JPanel           contentPanel;
  private final ImageIcon        sameElevationIcon;
  private boolean                selected;
  private JTextField             renameField;
  private InlineRenameListener   renameListener;

  /**
   * Callback for inline tab rename (SPIKE-31 Phase 2).
   */
  public interface InlineRenameListener {
    void commitRename(String newName);
    void cancelRename();
  }

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
    this.contentPanel = contentPanel;

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
    int maxWidth = AlpCatalogStyles.levelTabMaxWidth();
    setPreferredSize(new Dimension(maxWidth, getPreferredSize().height));
    setMaximumSize(new Dimension(maxWidth, Short.MAX_VALUE));
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

  /**
   * Forwards tab mouse events to the tabbed pane handler (fixes Modify Level double-click).
   */
  public void installTabMouseHandler(final JTabbedPane tabbedPane, final MouseAdapter handler) {
    MouseAdapter forwarder = new MouseAdapter() {
        private MouseEvent forward(MouseEvent e) {
          Component source = (Component)e.getSource();
          Point point = SwingUtilities.convertPoint(source, e.getPoint(), tabbedPane);
          return new MouseEvent(tabbedPane, e.getID(), e.getWhen(), e.getModifiersEx(),
              point.x, point.y, e.getClickCount(), e.isPopupTrigger(), e.getButton());
        }

        @Override
        public void mousePressed(MouseEvent e) {
          handler.mousePressed(forward(e));
        }

        @Override
        public void mouseReleased(MouseEvent e) {
          handler.mouseReleased(forward(e));
        }

        @Override
        public void mouseClicked(MouseEvent e) {
          handler.mouseClicked(forward(e));
        }

        @Override
        public void mouseDragged(MouseEvent e) {
          handler.mouseDragged(forward(e));
        }
      };
    installMouseHandlerRecursive(this, forwarder);
  }

  private static void installMouseHandlerRecursive(JComponent component, MouseAdapter handler) {
    component.addMouseListener(handler);
    component.addMouseMotionListener(handler);
    for (Component child : component.getComponents()) {
      if (child instanceof JComponent) {
        installMouseHandlerRecursive((JComponent)child, handler);
      }
    }
  }

  /**
   * Shows an inline editor on this tab for renaming.
   * @return {@code false} if rename is already in progress
   */
  public boolean beginInlineRename(String currentName, InlineRenameListener listener) {
    if (this.renameField != null) {
      return false;
    }
    this.renameListener = listener;
    this.renameField = new JTextField(currentName, 12);
    this.renameField.setBorder(null);
    this.renameField.selectAll();
    this.contentPanel.remove(this.nameLabel);
    this.contentPanel.add(this.renameField, 0);
    this.contentPanel.revalidate();
    this.renameField.requestFocusInWindow();
    this.renameField.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          commitInlineRename();
        }
      });
    this.renameField.addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent ev) {
          if (ev.getKeyCode() == KeyEvent.VK_ESCAPE) {
            cancelInlineRename();
          }
        }
      });
    this.renameField.addFocusListener(new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent ev) {
          if (renameField != null && !ev.isTemporary()) {
            commitInlineRename();
          }
        }
      });
    revalidate();
    repaint();
    return true;
  }

  public void cancelInlineRename() {
    if (this.renameField == null) {
      return;
    }
    endInlineRename(false);
    if (this.renameListener != null) {
      this.renameListener.cancelRename();
    }
  }

  private void commitInlineRename() {
    if (this.renameField == null) {
      return;
    }
    String newName = this.renameField.getText().trim();
    endInlineRename(true);
    if (this.renameListener != null && newName.length() > 0) {
      this.renameListener.commitRename(newName);
    } else if (this.renameListener != null) {
      this.renameListener.cancelRename();
    }
  }

  private void endInlineRename(boolean keepEditorRemoved) {
    if (this.renameField != null) {
      this.contentPanel.remove(this.renameField);
      this.contentPanel.add(this.nameLabel, 0);
      this.renameField = null;
      this.renameListener = null;
      revalidate();
      repaint();
    }
  }

  public boolean isInlineRenameActive() {
    return this.renameField != null;
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
    int maxWidth = AlpCatalogStyles.levelTabMaxWidth() - AlpCatalogStyles.scale(16);
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
