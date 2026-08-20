/*
 * PlantScheduleDeckPanel.java
 *
 * ALP CAD — plant schedule / takeoff in the context deck (SPIKE-29b, SPIKE-30).
 */
package com.eteks.sweethome3d.swing;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import com.eteks.sweethome3d.model.AlpPlantUtils;
import com.eteks.sweethome3d.model.AlpPlantUtils.PlantScheduleRow;
import com.eteks.sweethome3d.model.CollectionEvent;
import com.eteks.sweethome3d.model.CollectionListener;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Level;
import com.eteks.sweethome3d.model.UserPreferences;

/**
 * Context deck panel showing aggregated plant counts for all takeoff planting layers.
 */
public class PlantScheduleDeckPanel extends JPanel {
  private final Home                 home;
  private final UserPreferences      preferences;
  private final PlantScheduleModel   proposedScheduleModel;
  private final PlantScheduleModel   existingScheduleModel;
  private final JTable               proposedScheduleTable;
  private final JTable               existingScheduleTable;
  private final JButton              existingSectionToggleButton;
  private final JPanel               existingSectionPanel;
  private final Map<Level, PropertyChangeListener> levelRoleListeners;
  private boolean                    existingSectionExpanded;

  public PlantScheduleDeckPanel(Home home, UserPreferences preferences) {
    super(new BorderLayout());
    this.home = home;
    this.preferences = preferences;
    this.levelRoleListeners = new HashMap<Level, PropertyChangeListener>();
    AlpCatalogStyles.applyWorkspacePanel(this);
    setBorder(BorderFactory.createEmptyBorder(0, AlpInspectorStyles.scale(4), 0, AlpInspectorStyles.scale(4)));

    this.proposedScheduleModel = new PlantScheduleModel();
    this.existingScheduleModel = new PlantScheduleModel();
    this.proposedScheduleTable = createScheduleTable(this.proposedScheduleModel);
    this.existingScheduleTable = createScheduleTable(this.existingScheduleModel);

    JPanel proposedPanel = new JPanel(new BorderLayout());
    AlpCatalogStyles.applyWorkspacePanel(proposedPanel);
    proposedPanel.add(createSectionLabel("proposedSection.title"), BorderLayout.NORTH);
    proposedPanel.add(wrapTable(this.proposedScheduleTable), BorderLayout.CENTER);

    this.existingSectionToggleButton = new JButton();
    this.existingSectionToggleButton.setFocusable(false);
    AlpCatalogStyles.applyWorkspacePanel(this.existingSectionToggleButton);
    this.existingSectionToggleButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          setExistingSectionExpanded(!existingSectionExpanded);
        }
      });

    this.existingSectionPanel = new JPanel(new BorderLayout());
    AlpCatalogStyles.applyWorkspacePanel(this.existingSectionPanel);
    this.existingSectionPanel.add(wrapTable(this.existingScheduleTable), BorderLayout.CENTER);

    JPanel existingPanel = new JPanel(new BorderLayout());
    AlpCatalogStyles.applyWorkspacePanel(existingPanel);
    existingPanel.add(this.existingSectionToggleButton, BorderLayout.NORTH);
    existingPanel.add(this.existingSectionPanel, BorderLayout.CENTER);

    JPanel sectionsPanel = new JPanel(new BorderLayout(0, AlpInspectorStyles.scale(6)));
    AlpCatalogStyles.applyWorkspacePanel(sectionsPanel);
    sectionsPanel.add(proposedPanel, BorderLayout.CENTER);
    sectionsPanel.add(existingPanel, BorderLayout.SOUTH);
    add(sectionsPanel, BorderLayout.CENTER);

    home.addFurnitureListener(new CollectionListener<HomePieceOfFurniture>() {
        public void collectionChanged(CollectionEvent<HomePieceOfFurniture> ev) {
          refreshSchedule();
        }
      });
    home.addLevelsListener(new CollectionListener<Level>() {
        public void collectionChanged(CollectionEvent<Level> ev) {
          registerLevelRoleListeners();
          refreshSchedule();
        }
      });
    registerLevelRoleListeners();
    setExistingSectionExpanded(false);
    refreshSchedule();
  }

  /**
   * Returns whether the home currently has plants on any takeoff planting layer.
   */
  public boolean hasTakeoffPlants() {
    return AlpPlantUtils.hasTakeoffPlants(this.home);
  }

  private JTable createScheduleTable(PlantScheduleModel model) {
    JTable table = new JTable(model);
    AlpCatalogStyles.applyInventoryTable(table);
    float resolutionScale = SwingTools.getResolutionScale();
    if (resolutionScale != 1) {
      table.setRowHeight(Math.round(table.getRowHeight() * resolutionScale));
    }
    return table;
  }

  private JScrollPane wrapTable(JTable table) {
    JScrollPane scrollPane = SwingTools.createScrollPane(table);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    AlpCatalogStyles.applyCatalogScrollPane(scrollPane);
    return scrollPane;
  }

  private javax.swing.JLabel createSectionLabel(String resourceKey) {
    javax.swing.JLabel label = new javax.swing.JLabel(
        this.preferences.getLocalizedString(PlantScheduleDeckPanel.class, resourceKey));
    label.setBorder(BorderFactory.createEmptyBorder(0, 0, AlpInspectorStyles.scale(2), 0));
    return label;
  }

  private void setExistingSectionExpanded(boolean expanded) {
    this.existingSectionExpanded = expanded;
    this.existingSectionPanel.setVisible(expanded);
    updateExistingSectionToggleLabel();
    revalidate();
    repaint();
  }

  private void updateExistingSectionToggleLabel() {
    int count = AlpPlantUtils.countPlantsOnExistingLevels(this.home);
    String title = this.preferences.getLocalizedString(
        PlantScheduleDeckPanel.class, "existingSection.title");
    String prefix = this.existingSectionExpanded ? "\u25BE " : "\u25B8 ";
    this.existingSectionToggleButton.setText(prefix + title + " (" + count + ")");
    this.existingSectionToggleButton.setVisible(count > 0
        || this.existingSectionExpanded
        || !AlpPlantUtils.getExistingPlantingLevels(this.home).isEmpty());
  }

  private void registerLevelRoleListeners() {
    for (Map.Entry<Level, PropertyChangeListener> entry : this.levelRoleListeners.entrySet()) {
      Level level = entry.getKey();
      PropertyChangeListener listener = entry.getValue();
      level.removePropertyChangeListener(Level.Property.CATEGORY.name(), listener);
      level.removePropertyChangeListener(Level.Property.PLANT_TAKEOFF.name(), listener);
    }
    this.levelRoleListeners.clear();

    PropertyChangeListener listener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          refreshSchedule();
        }
      };
    for (Level level : this.home.getLevels()) {
      level.addPropertyChangeListener(Level.Property.CATEGORY.name(), listener);
      level.addPropertyChangeListener(Level.Property.PLANT_TAKEOFF.name(), listener);
      this.levelRoleListeners.put(level, listener);
    }
  }

  private void refreshSchedule() {
    this.proposedScheduleModel.setRows(AlpPlantUtils.buildProposedSchedule(this.home));
    this.existingScheduleModel.setRows(AlpPlantUtils.buildExistingSchedule(this.home));
    updateExistingSectionToggleLabel();
  }

  private class PlantScheduleModel extends AbstractTableModel {
    private List<PlantScheduleRow> rows = java.util.Collections.emptyList();

    public void setRows(List<PlantScheduleRow> rows) {
      this.rows = rows != null ? rows : java.util.Collections.<PlantScheduleRow>emptyList();
      fireTableDataChanged();
    }

    public int getRowCount() {
      return this.rows.size();
    }

    public int getColumnCount() {
      return 3;
    }

    public String getColumnName(int columnIndex) {
      switch (columnIndex) {
        case 0:
          return preferences.getLocalizedString(PlantScheduleDeckPanel.class, "nameColumn.label");
        case 1:
          return preferences.getLocalizedString(PlantScheduleDeckPanel.class, "referenceColumn.label");
        case 2:
          return preferences.getLocalizedString(PlantScheduleDeckPanel.class, "countColumn.label");
        default:
          return "";
      }
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
      PlantScheduleRow row = this.rows.get(rowIndex);
      switch (columnIndex) {
        case 0:
          return row.getName();
        case 1:
          return row.getReference();
        case 2:
          return row.getCount();
        default:
          return null;
      }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      if (columnIndex == 2) {
        return Integer.class;
      }
      return String.class;
    }
  }
}
