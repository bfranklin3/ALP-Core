/*
 * AlpDuplicatePlantTypeDialog.java
 *
 * ALP CAD — duplicate plant type dialog (SPIKE-45A).
 */
package com.eteks.sweethome3d.swing;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.ParsePosition;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.eteks.sweethome3d.model.AlpPlantCatalogUtils;
import com.eteks.sweethome3d.model.AlpPlantMetadata;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.FurnitureCatalog;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.viewcontroller.FurnitureController;

/**
 * Collects name, schedule name, and default size for a duplicated plant type.
 */
public class AlpDuplicatePlantTypeDialog {
  private final UserPreferences preferences;
  private final FurnitureCatalog catalog;
  private final FurnitureController furnitureController;
  private final CatalogPieceOfFurniture source;

  private JTextField nameField;
  private JTextField scheduleNameField;
  private JTextField widthField;
  private JTextField depthField;

  public AlpDuplicatePlantTypeDialog(UserPreferences preferences,
                                     FurnitureCatalog catalog,
                                     FurnitureController furnitureController,
                                     CatalogPieceOfFurniture source) {
    this.preferences = preferences;
    this.catalog = catalog;
    this.furnitureController = furnitureController;
    this.source = source;
  }

  /**
   * Shows the dialog and returns the new catalog piece, or {@code null} if cancelled.
   */
  public CatalogPieceOfFurniture showDialog(Component parent) {
    HomePieceOfFurniture probe = this.furnitureController.createHomePieceOfFurniture(this.source);
    AlpPlantMetadata metadata = AlpPlantMetadata.fromPiece(probe);

    String defaultName = AlpPlantCatalogUtils.defaultDuplicateName(this.source);
    String defaultScheduleName = metadata != null
        ? metadata.getScheduleName()
        : defaultName;
    float defaultWidth = metadata != null ? metadata.getDefaultWidth() : this.source.getWidth();
    float defaultDepth = metadata != null ? metadata.getDefaultDepth() : this.source.getDepth();

    this.nameField = new JTextField(defaultName, 24);
    this.scheduleNameField = new JTextField(defaultScheduleName, 24);
    this.widthField = new JTextField(formatLength(defaultWidth), 10);
    this.depthField = new JTextField(formatLength(defaultDepth), 10);

    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.anchor = GridBagConstraints.LINE_START;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.insets = new Insets(0, 0, 6, 8);
    addRow(panel, constraints, 0,
        this.preferences.getLocalizedString(AlpDuplicatePlantTypeDialog.class, "nameLabel"),
        this.nameField);
    addRow(panel, constraints, 1,
        this.preferences.getLocalizedString(AlpDuplicatePlantTypeDialog.class, "scheduleNameLabel"),
        this.scheduleNameField);
    addRow(panel, constraints, 2,
        this.preferences.getLocalizedString(AlpDuplicatePlantTypeDialog.class, "widthLabel"),
        this.widthField);
    addRow(panel, constraints, 3,
        this.preferences.getLocalizedString(AlpDuplicatePlantTypeDialog.class, "depthLabel"),
        this.depthField);

    String title = this.preferences.getLocalizedString(AlpDuplicatePlantTypeDialog.class, "title");
    int option = JOptionPane.showConfirmDialog(parent, panel, title,
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (option != JOptionPane.OK_OPTION) {
      return null;
    }

    String displayName = this.nameField.getText();
    if (displayName == null || displayName.trim().length() == 0) {
      JOptionPane.showMessageDialog(parent,
          this.preferences.getLocalizedString(AlpDuplicatePlantTypeDialog.class, "invalidName.message"),
          title, JOptionPane.ERROR_MESSAGE);
      return null;
    }
    String scheduleName = this.scheduleNameField.getText();
    if (scheduleName == null || scheduleName.trim().length() == 0) {
      scheduleName = displayName.trim();
    }
    float width = parseLength(this.widthField.getText());
    float depth = parseLength(this.depthField.getText());
    if (width <= 0 || depth <= 0) {
      JOptionPane.showMessageDialog(parent,
          this.preferences.getLocalizedString(AlpDuplicatePlantTypeDialog.class, "invalidSize.message"),
          title, JOptionPane.ERROR_MESSAGE);
      return null;
    }

    return AlpPlantCatalogUtils.duplicatePlantType(
        this.source, this.catalog, displayName.trim(), scheduleName.trim(), width, depth);
  }

  private void addRow(JPanel panel, GridBagConstraints constraints, int row,
                      String labelText, JComponent field) {
    constraints.gridx = 0;
    constraints.gridy = row;
    constraints.weightx = 0;
    panel.add(new JLabel(labelText), constraints);
    constraints.gridx = 1;
    constraints.weightx = 1;
    panel.add(field, constraints);
  }

  private String formatLength(float lengthInCentimeter) {
    return this.preferences.getLengthUnit().getFormat().format(lengthInCentimeter);
  }

  private float parseLength(String text) {
    if (text == null || text.trim().length() == 0) {
      return -1;
    }
    try {
      ParsePosition position = new ParsePosition(0);
      Number value = (Number)this.preferences.getLengthUnit().getFormat().parseObject(text.trim(), position);
      if (value == null) {
        return -1;
      }
      return value.floatValue();
    } catch (RuntimeException ex) {
      return -1;
    }
  }
}
