/*
 * PlantScheduleDeckPanel.java
 *
 * ALP CAD — plant schedule / takeoff in the context deck (SPIKE-29b).
 */
package com.eteks.sweethome3d.swing;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import com.eteks.sweethome3d.model.AlpLevelDefaults;
import com.eteks.sweethome3d.model.AlpPlantUtils;
import com.eteks.sweethome3d.model.AlpPlantUtils.PlantScheduleRow;
import com.eteks.sweethome3d.model.CollectionEvent;
import com.eteks.sweethome3d.model.CollectionListener;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Level;
import com.eteks.sweethome3d.model.UserPreferences;

/**
 * Context deck panel showing aggregated plant counts for the Plants layer.
 */
public class PlantScheduleDeckPanel extends JPanel {
  private final Home                 home;
  private final UserPreferences      preferences;
  private final PlantScheduleModel   scheduleModel;
  private final JTable               scheduleTable;

  public PlantScheduleDeckPanel(Home home, UserPreferences preferences) {
    super(new BorderLayout());
    this.home = home;
    this.preferences = preferences;
    AlpCatalogStyles.applyWorkspacePanel(this);
    setBorder(BorderFactory.createEmptyBorder(0, AlpInspectorStyles.scale(4), 0, AlpInspectorStyles.scale(4)));

    this.scheduleModel = new PlantScheduleModel();
    this.scheduleTable = new JTable(this.scheduleModel);
    AlpCatalogStyles.applyInventoryTable(this.scheduleTable);
    float resolutionScale = SwingTools.getResolutionScale();
    if (resolutionScale != 1) {
      this.scheduleTable.setRowHeight(Math.round(this.scheduleTable.getRowHeight() * resolutionScale));
    }

    JScrollPane scrollPane = SwingTools.createScrollPane(this.scheduleTable);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    AlpCatalogStyles.applyCatalogScrollPane(scrollPane);
    add(scrollPane, BorderLayout.CENTER);

    home.addFurnitureListener(new CollectionListener<HomePieceOfFurniture>() {
        public void collectionChanged(CollectionEvent<HomePieceOfFurniture> ev) {
          refreshSchedule();
        }
      });
    home.addPropertyChangeListener(Home.Property.SELECTED_LEVEL, ev -> refreshSchedule());
    refreshSchedule();
  }

  /**
   * Returns whether the home currently has plant furniture on the Plants layer.
   */
  public boolean hasPlantsOnPlantsLayer() {
    Level plantsLevel = AlpLevelDefaults.findPlantsLevel(this.home, this.preferences);
    return plantsLevel != null && AlpPlantUtils.countPlantsOnLevel(this.home, plantsLevel) > 0;
  }

  private void refreshSchedule() {
    Level plantsLevel = AlpLevelDefaults.findPlantsLevel(this.home, this.preferences);
    this.scheduleModel.setRows(AlpPlantUtils.buildSchedule(this.home, plantsLevel));
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
