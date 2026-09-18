/*
 * AlpPlantAreaFillDialog.java
 *
 * ALP CAD — plant area scatter fill dialog (SPIKE-38A / 38B tight fill).
 */
package com.eteks.sweethome3d.swing;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.eteks.sweethome3d.model.AlpPlantAreaFill;
import com.eteks.sweethome3d.model.AlpPlantCatalogUtils;
import com.eteks.sweethome3d.model.AlpPlantMetadata;
import com.eteks.sweethome3d.model.AlpPlantUtils;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.FurnitureCatalog;
import com.eteks.sweethome3d.model.FurnitureCategory;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.viewcontroller.FurnitureController;

/**
 * Collects plant symbol and spacing for an area scatter fill.
 */
public class AlpPlantAreaFillDialog {
  private final UserPreferences preferences;
  private final Room room;
  private final FurnitureCatalog catalog;
  private final FurnitureController furnitureController;
  private final String defaultSymbolCatalogId;
  private final boolean regenerateMode;

  private JComboBox<PlantChoice> plantComboBox;
  private JTextField spacingField;
  private JSpinner sizeVariationSpinner;
  private JCheckBox tightFillCheckBox;

  public AlpPlantAreaFillDialog(UserPreferences preferences,
                                Room room,
                                FurnitureCatalog catalog,
                                FurnitureController furnitureController,
                                HomePieceOfFurniture defaultPlant,
                                boolean regenerateMode) {
    this.preferences = preferences;
    this.room = room;
    this.catalog = catalog;
    this.furnitureController = furnitureController;
    this.regenerateMode = regenerateMode;
    if (defaultPlant != null && AlpPlantUtils.isPlant(defaultPlant)) {
      this.defaultSymbolCatalogId = defaultPlant.getCatalogId();
    } else {
      AlpPlantAreaFill.Recipe recipe = AlpPlantAreaFill.getRecipe(room);
      this.defaultSymbolCatalogId = recipe != null ? recipe.getSymbolCatalogId() : null;
    }
  }

  /**
   * Shows the dialog and returns a recipe, or {@code null} if cancelled.
   */
  public AlpPlantAreaFill.Recipe showDialog(Component parent) {
    List<PlantChoice> choices = collectPlantChoices();
    if (choices.isEmpty()) {
      JOptionPane.showMessageDialog(parent,
          this.preferences.getLocalizedString(AlpPlantAreaFillDialog.class, "noPlants.message"),
          this.preferences.getLocalizedString(AlpPlantAreaFillDialog.class, "title"),
          JOptionPane.WARNING_MESSAGE);
      return null;
    }

    this.plantComboBox = new JComboBox<PlantChoice>(new DefaultComboBoxModel<PlantChoice>(
        choices.toArray(new PlantChoice [choices.size()])));
    selectDefaultPlant(choices);
    this.plantComboBox.addActionListener(new AbstractAction() {
        private static final long serialVersionUID = 1L;

        public void actionPerformed(java.awt.event.ActionEvent ev) {
          updateSpacingDefault();
        }
      });

    this.spacingField = new JTextField(10);
    this.sizeVariationSpinner = new JSpinner(new SpinnerNumberModel(
        AlpPlantAreaFill.DEFAULT_SIZE_VARIATION_PERCENT, 0, 20, 1));
    this.tightFillCheckBox = new JCheckBox(this.preferences.getLocalizedString(
        AlpPlantAreaFillDialog.class, "tightFillLabel"));
    this.tightFillCheckBox.setToolTipText(this.preferences.getLocalizedString(
        AlpPlantAreaFillDialog.class, "tightFillTip"));

    AlpPlantAreaFill.Recipe existingRecipe = AlpPlantAreaFill.getRecipe(this.room);
    if (existingRecipe != null) {
      this.spacingField.setText(formatLength(existingRecipe.getSpacing()));
      this.sizeVariationSpinner.setValue(Float.valueOf(existingRecipe.getSizeVariationPercent()));
      this.tightFillCheckBox.setSelected(existingRecipe.isTightFill());
    } else {
      updateSpacingDefault();
    }

    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.anchor = GridBagConstraints.LINE_START;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.insets = new Insets(0, 0, 6, 8);
    addRow(panel, constraints, 0,
        this.preferences.getLocalizedString(AlpPlantAreaFillDialog.class, "plantLabel"),
        this.plantComboBox);
    addRow(panel, constraints, 1,
        this.preferences.getLocalizedString(AlpPlantAreaFillDialog.class, "spacingLabel"),
        this.spacingField);
    addRow(panel, constraints, 2,
        this.preferences.getLocalizedString(AlpPlantAreaFillDialog.class, "sizeVariationLabel"),
        this.sizeVariationSpinner);
    constraints.gridx = 0;
    constraints.gridy = 3;
    constraints.gridwidth = 2;
    constraints.weightx = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.insets = new Insets(0, 0, 6, 0);
    panel.add(this.tightFillCheckBox, constraints);

    String title = this.preferences.getLocalizedString(AlpPlantAreaFillDialog.class,
        this.regenerateMode ? "regenerateTitle" : "title");
    int option = JOptionPane.showConfirmDialog(parent, panel, title,
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (option != JOptionPane.OK_OPTION) {
      return null;
    }

    PlantChoice choice = (PlantChoice)this.plantComboBox.getSelectedItem();
    if (choice == null) {
      return null;
    }
    float spacing = parseLength(this.spacingField.getText());
    if (spacing <= 0) {
      JOptionPane.showMessageDialog(parent,
          this.preferences.getLocalizedString(AlpPlantAreaFillDialog.class, "invalidSpacing.message"),
          title, JOptionPane.ERROR_MESSAGE);
      return null;
    }
    float sizeVariation = ((Number)this.sizeVariationSpinner.getValue()).floatValue();
    Long seed = existingRecipe != null && this.regenerateMode
        ? Long.valueOf(existingRecipe.getRandomSeed())
        : null;
    return AlpPlantAreaFill.createRecipe(this.room, choice.getCatalogId(), spacing,
        sizeVariation, -1, seed, this.tightFillCheckBox.isSelected(), this.regenerateMode);
  }

  private void selectDefaultPlant(List<PlantChoice> choices) {
    if (this.defaultSymbolCatalogId == null) {
      return;
    }
    for (int i = 0; i < choices.size(); i++) {
      if (this.defaultSymbolCatalogId.equals(choices.get(i).getCatalogId())) {
        this.plantComboBox.setSelectedIndex(i);
        return;
      }
    }
  }

  private void updateSpacingDefault() {
    PlantChoice choice = (PlantChoice)this.plantComboBox.getSelectedItem();
    if (choice == null) {
      return;
    }
    AlpPlantAreaFill.Recipe existingRecipe = AlpPlantAreaFill.getRecipe(this.room);
    if (existingRecipe == null || !choice.getCatalogId().equals(existingRecipe.getSymbolCatalogId())) {
      this.spacingField.setText(formatLength(choice.getDefaultSpacing()));
    }
  }

  private List<PlantChoice> collectPlantChoices() {
    List<PlantChoice> choices = new ArrayList<PlantChoice>();
    for (FurnitureCategory category : this.catalog.getCategories()) {
      for (CatalogPieceOfFurniture piece : category.getFurniture()) {
        if (AlpPlantCatalogUtils.isCatalogPlant(piece)) {
          HomePieceOfFurniture probe = this.furnitureController.createHomePieceOfFurniture(piece);
          AlpPlantMetadata metadata = AlpPlantMetadata.fromPiece(probe);
          float spacing = metadata != null
              ? metadata.getDefaultSpacing()
              : Math.min(piece.getWidth(), piece.getDepth()) * 0.65f;
          choices.add(new PlantChoice(piece.getId(), piece.getName(), spacing));
        }
      }
    }
    return choices;
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

  private static void addRow(JPanel panel, GridBagConstraints constraints, int row,
                             String labelText, JComponent field) {
    constraints.gridx = 0;
    constraints.gridy = row;
    constraints.gridwidth = 1;
    constraints.weightx = 0;
    constraints.fill = GridBagConstraints.NONE;
    constraints.insets = new Insets(0, 0, 6, 8);
    panel.add(new JLabel(labelText), constraints);
    constraints.gridx = 1;
    constraints.weightx = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.insets = new Insets(0, 0, 6, 0);
    panel.add(field, constraints);
  }

  private static final class PlantChoice {
    private final String catalogId;
    private final String displayName;
    private final float defaultSpacing;

    private PlantChoice(String catalogId, String displayName, float defaultSpacing) {
      this.catalogId = catalogId;
      this.displayName = displayName;
      this.defaultSpacing = defaultSpacing;
    }

    public String getCatalogId() {
      return this.catalogId;
    }

    public float getDefaultSpacing() {
      return this.defaultSpacing;
    }

    @Override
    public String toString() {
      return this.displayName;
    }
  }
}
