/*
 * AlpLevelRoleControls.java
 *
 * ALP CAD — shared Category / Takeoff combo helpers (SPIKE-30).
 */
package com.eteks.sweethome3d.swing;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

import com.eteks.sweethome3d.model.LevelCategory;
import com.eteks.sweethome3d.model.LevelPlantTakeoff;
import com.eteks.sweethome3d.model.UserPreferences;

/**
 * Factory helpers for layer role controls in inspectors and dialogs.
 */
final class AlpLevelRoleControls {
  private AlpLevelRoleControls() {
  }

  static JComboBox<LevelCategory> createCategoryComboBox(UserPreferences preferences) {
    JComboBox<LevelCategory> comboBox = new JComboBox<LevelCategory>(
        LevelCategory.values());
    comboBox.setRenderer(new EnumComboBoxRenderer<LevelCategory>(preferences, LevelCategory.class));
    return comboBox;
  }

  static JComboBox<LevelPlantTakeoff> createPlantTakeoffComboBox(UserPreferences preferences) {
    JComboBox<LevelPlantTakeoff> comboBox = new JComboBox<LevelPlantTakeoff>(
        LevelPlantTakeoff.values());
    comboBox.setRenderer(new EnumComboBoxRenderer<LevelPlantTakeoff>(preferences, LevelPlantTakeoff.class));
    return comboBox;
  }

  static JLabel createCategoryLabel(UserPreferences preferences) {
    return new JLabel(SwingTools.getLocalizedLabelText(preferences, LevelPanel.class, "categoryLabel.text"));
  }

  static JLabel createPlantTakeoffLabel(UserPreferences preferences) {
    return new JLabel(SwingTools.getLocalizedLabelText(preferences, LevelPanel.class, "plantTakeoffLabel.text"));
  }

  static void updatePlantTakeoffVisibility(JComponent plantTakeoffLabel,
                                           JComponent plantTakeoffComboBox,
                                           LevelCategory category) {
    boolean planting = category == LevelCategory.PLANTING;
    plantTakeoffLabel.setVisible(planting);
    plantTakeoffComboBox.setVisible(planting);
    plantTakeoffComboBox.setEnabled(planting);
  }

  private static final class EnumComboBoxRenderer<T extends Enum<T>> extends javax.swing.DefaultListCellRenderer {
    private final UserPreferences preferences;
    private final Class<?>        enumClass;

    EnumComboBoxRenderer(UserPreferences preferences, Class<?> enumClass) {
      this.preferences = preferences;
      this.enumClass = enumClass;
    }

    @Override
    public java.awt.Component getListCellRendererComponent(
        javax.swing.JList list, Object value, int index,
        boolean isSelected, boolean cellHasFocus) {
      java.awt.Component component = super.getListCellRendererComponent(
          list, value, index, isSelected, cellHasFocus);
      if (value instanceof Enum && component instanceof JLabel) {
        Enum<?> enumValue = (Enum<?>)value;
        ((JLabel)component).setText(this.preferences.getLocalizedString(
            this.enumClass, enumValue.name().toLowerCase() + ".label"));
      }
      return component;
    }
  }
}
