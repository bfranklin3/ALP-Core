/*
 * AlpToolBarButton.java
 *
 * ALP CAD — vertical icon + label toolbar buttons (SPIKE-31).
 */
package com.eteks.sweethome3d.swing;

import java.awt.Font;
import java.awt.Insets;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

/**
 * Factory for SmartDraw-style toolbar buttons (icon above short label).
 */
public final class AlpToolBarButton {
  private AlpToolBarButton() {
  }

  /**
   * Creates a labeled toolbar push button for the given action.
   */
  public static JButton createButton(Action action) {
    JButton button = new JButton(new ResourceAction.LabeledToolBarAction(action));
    configure(button);
    return button;
  }

  /**
   * Creates a labeled toolbar toggle button for the given plan mode action.
   */
  public static JToggleButton createToggleButton(Action action) {
    JToggleButton button = new JToggleButton(new ResourceAction.LabeledToolBarAction(action));
    Object toggleButtonModel = action.getValue(ResourceAction.TOGGLE_BUTTON_MODEL);
    if (toggleButtonModel instanceof JToggleButton.ToggleButtonModel) {
      button.setModel((JToggleButton.ToggleButtonModel)toggleButtonModel);
    }
    configure(button);
    return button;
  }

  /**
   * Applies vertical icon + label layout to an existing toolbar button.
   */
  public static void setButtonAction(JButton button, Action action) {
    button.setAction(new ResourceAction.LabeledToolBarAction(action));
    configure(button);
  }

  /**
   * Applies vertical icon + label layout to an existing toolbar button.
   */
  public static void configure(AbstractButton button) {
    button.setFocusable(false);
    button.setHorizontalTextPosition(SwingConstants.CENTER);
    button.setVerticalTextPosition(SwingConstants.BOTTOM);
    button.setIconTextGap(AlpCatalogStyles.scale(2));
    float labelSize = Math.max(10f, button.getFont().getSize2D() - 1f);
    button.setFont(button.getFont().deriveFont(Font.PLAIN, labelSize));
    int margin = AlpCatalogStyles.scale(2);
    button.setMargin(new Insets(margin, margin, margin, margin));
    AlpCatalogStyles.applyToolBarButton(button);
  }
}
