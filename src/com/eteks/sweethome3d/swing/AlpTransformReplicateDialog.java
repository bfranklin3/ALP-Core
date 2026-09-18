/*
 * AlpTransformReplicateDialog.java
 *
 * ALP CAD - precision move and linear replicate dialogs (SPIKE-49).
 */
package com.eteks.sweethome3d.swing;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.eteks.sweethome3d.model.UserPreferences;

/**
 * Collects parameters for SPIKE-49 precision placement commands.
 */
public final class AlpTransformReplicateDialog {
  private static final int DEFAULT_FIT_TOTAL_COUNT = 5;

  private AlpTransformReplicateDialog() {
  }

  public static MoveByDistanceResult showMoveByDistanceDialog(Component parent,
                                                              UserPreferences preferences) {
    float maximumLength = preferences.getLengthUnit().getMaximumLength();
    NullableSpinner.NullableSpinnerLengthModel horizontalModel =
        new NullableSpinner.NullableSpinnerLengthModel(preferences, 0f, -maximumLength, maximumLength);
    NullableSpinner.NullableSpinnerLengthModel verticalModel =
        new NullableSpinner.NullableSpinnerLengthModel(preferences, 0f, -maximumLength, maximumLength);
    NullableSpinner horizontalSpinner = new NullableSpinner(horizontalModel);
    NullableSpinner verticalSpinner = new NullableSpinner(verticalModel);

    JPanel panel = createPanel();
    GridBagConstraints constraints = createConstraints();
    addRow(panel, constraints, 0,
        preferences.getLocalizedString(AlpTransformReplicateDialog.class, "horizontalLabel"),
        horizontalSpinner);
    addRow(panel, constraints, 1,
        preferences.getLocalizedString(AlpTransformReplicateDialog.class, "verticalLabel"),
        verticalSpinner);

    String title = preferences.getLocalizedString(AlpTransformReplicateDialog.class, "moveTitle");
    int option = JOptionPane.showConfirmDialog(parent, panel, title,
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (option != JOptionPane.OK_OPTION) {
      return null;
    }
    Float dx = horizontalModel.getLength();
    Float dy = verticalModel.getLength();
    float dxValue = dx != null ? dx.floatValue() : 0f;
    float dyValue = dy != null ? dy.floatValue() : 0f;
    if (dxValue == 0f && dyValue == 0f) {
      showInvalidOffsetMessage(parent, preferences, title);
      return null;
    }
    return new MoveByDistanceResult(dxValue, dyValue);
  }

  public static ReplicateInLineResult showReplicateInLineDialog(Component parent,
                                                                UserPreferences preferences,
                                                                float defaultSpacing) {
    JSpinner copiesSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
    NullableSpinner.NullableSpinnerLengthModel spacingModel =
        new NullableSpinner.NullableSpinnerLengthModel(preferences,
            Math.max(defaultSpacing, preferences.getLengthUnit().getMinimumLength()),
            preferences.getLengthUnit().getMinimumLength(),
            preferences.getLengthUnit().getMaximumLength());
    NullableSpinner spacingSpinner = new NullableSpinner(spacingModel);
    JSpinner angleSpinner = new JSpinner(new SpinnerNumberModel(0d, -360d, 360d, 1d));

    JPanel panel = createPanel();
    GridBagConstraints constraints = createConstraints();
    addRow(panel, constraints, 0,
        preferences.getLocalizedString(AlpTransformReplicateDialog.class, "copiesLabel"),
        copiesSpinner);
    addRow(panel, constraints, 1,
        preferences.getLocalizedString(AlpTransformReplicateDialog.class, "spacingLabel"),
        spacingSpinner);
    addRow(panel, constraints, 2,
        preferences.getLocalizedString(AlpTransformReplicateDialog.class, "angleLabel"),
        angleSpinner);

    String title = preferences.getLocalizedString(AlpTransformReplicateDialog.class, "replicateTitle");
    int option = JOptionPane.showConfirmDialog(parent, panel, title,
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (option != JOptionPane.OK_OPTION) {
      return null;
    }
    Float spacing = spacingModel.getLength();
    if (spacing == null || spacing.floatValue() <= 0) {
      showInvalidLengthMessage(parent, preferences, title);
      return null;
    }
    return new ReplicateInLineResult(
        ((Number)copiesSpinner.getValue()).intValue(),
        spacing.floatValue(),
        ((Number)angleSpinner.getValue()).floatValue());
  }

  public static FitCopiesBetweenPointsResult showFitCopiesBetweenPointsDialog(Component parent,
                                                                              UserPreferences preferences,
                                                                              float defaultSpacing) {
    float defaultDistance = defaultSpacing * (DEFAULT_FIT_TOTAL_COUNT - 1);
    JSpinner totalCountSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_FIT_TOTAL_COUNT, 2, 1000, 1));
    NullableSpinner.NullableSpinnerLengthModel distanceModel =
        new NullableSpinner.NullableSpinnerLengthModel(preferences,
            Math.max(defaultDistance, preferences.getLengthUnit().getMinimumLength()),
            preferences.getLengthUnit().getMinimumLength(),
            preferences.getLengthUnit().getMaximumLength());
    NullableSpinner distanceSpinner = new NullableSpinner(distanceModel);
    JSpinner angleSpinner = new JSpinner(new SpinnerNumberModel(0d, -360d, 360d, 1d));

    JPanel panel = createPanel();
    GridBagConstraints constraints = createConstraints();
    addRow(panel, constraints, 0,
        preferences.getLocalizedString(AlpTransformReplicateDialog.class, "totalCountLabel"),
        totalCountSpinner);
    addRow(panel, constraints, 1,
        preferences.getLocalizedString(AlpTransformReplicateDialog.class, "distanceLabel"),
        distanceSpinner);
    addRow(panel, constraints, 2,
        preferences.getLocalizedString(AlpTransformReplicateDialog.class, "angleLabel"),
        angleSpinner);

    String title = preferences.getLocalizedString(AlpTransformReplicateDialog.class, "fitTitle");
    int option = JOptionPane.showConfirmDialog(parent, panel, title,
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (option != JOptionPane.OK_OPTION) {
      return null;
    }
    Float distance = distanceModel.getLength();
    if (distance == null || distance.floatValue() <= 0) {
      showInvalidLengthMessage(parent, preferences, title);
      return null;
    }
    return new FitCopiesBetweenPointsResult(
        ((Number)totalCountSpinner.getValue()).intValue(),
        distance.floatValue(),
        ((Number)angleSpinner.getValue()).floatValue());
  }

  private static JPanel createPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
    return panel;
  }

  private static GridBagConstraints createConstraints() {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.anchor = GridBagConstraints.LINE_START;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.insets = new Insets(0, 0, 6, 8);
    return constraints;
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

  private static void showInvalidLengthMessage(Component parent, UserPreferences preferences, String title) {
    JOptionPane.showMessageDialog(parent,
        preferences.getLocalizedString(AlpTransformReplicateDialog.class, "invalidLengthMessage"),
        title, JOptionPane.ERROR_MESSAGE);
  }

  private static void showInvalidOffsetMessage(Component parent, UserPreferences preferences, String title) {
    JOptionPane.showMessageDialog(parent,
        preferences.getLocalizedString(AlpTransformReplicateDialog.class, "invalidOffsetMessage"),
        title, JOptionPane.ERROR_MESSAGE);
  }

  public static final class MoveByDistanceResult {
    private final float dx;
    private final float dy;

    private MoveByDistanceResult(float dx, float dy) {
      this.dx = dx;
      this.dy = dy;
    }

    public float getDx() {
      return this.dx;
    }

    public float getDy() {
      return this.dy;
    }
  }

  public static final class ReplicateInLineResult {
    private final int copies;
    private final float spacing;
    private final float angleInDegrees;

    private ReplicateInLineResult(int copies, float spacing, float angleInDegrees) {
      this.copies = copies;
      this.spacing = spacing;
      this.angleInDegrees = angleInDegrees;
    }

    public int getCopies() {
      return this.copies;
    }

    public float getSpacing() {
      return this.spacing;
    }

    public float getAngleInDegrees() {
      return this.angleInDegrees;
    }
  }

  public static final class FitCopiesBetweenPointsResult {
    private final int totalCount;
    private final float distance;
    private final float angleInDegrees;

    private FitCopiesBetweenPointsResult(int totalCount, float distance, float angleInDegrees) {
      this.totalCount = totalCount;
      this.distance = distance;
      this.angleInDegrees = angleInDegrees;
    }

    public int getTotalCount() {
      return this.totalCount;
    }

    public float getDistance() {
      return this.distance;
    }

    public float getAngleInDegrees() {
      return this.angleInDegrees;
    }
  }
}
