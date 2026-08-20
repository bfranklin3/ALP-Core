/*
 * LayerItemsDeckPanel.java
 *
 * ALP CAD — active-layer inventory in the context deck (SPIKE-29b).
 */
package com.eteks.sweethome3d.swing;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;

import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Level;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.viewcontroller.FurnitureView;
import com.eteks.sweethome3d.viewcontroller.HomeController;

/**
 * Context deck panel showing furniture and areas on the active layer only.
 */
public class LayerItemsDeckPanel extends JPanel {
  private final Home              home;
  private final UserPreferences   preferences;
  private final HomeController    controller;
  private final FurnitureTable    furnitureTable;
  private final RoomTable         roomTable;

  public LayerItemsDeckPanel(Home home, UserPreferences preferences, HomeController controller) {
    super(new BorderLayout());
    this.home = home;
    this.preferences = preferences;
    this.controller = controller;
    AlpCatalogStyles.applyWorkspacePanel(this);
    setBorder(BorderFactory.createEmptyBorder(0, AlpInspectorStyles.scale(4), 0, AlpInspectorStyles.scale(4)));

    this.furnitureTable = new FurnitureTable(home, preferences, null);
    this.furnitureTable.setFurnitureFilter(createLevelFilter());
    addRowSelectListener(this.furnitureTable, true);

    JScrollPane furnitureScrollPane = SwingTools.createScrollPane(this.furnitureTable);
    furnitureScrollPane.setBorder(BorderFactory.createEmptyBorder());
    AlpCatalogStyles.applyCatalogScrollPane(furnitureScrollPane);
    enableViewportFocus(furnitureScrollPane);

    this.roomTable = new RoomTable(home, preferences, controller);
    this.roomTable.setLevelFilter(home.getSelectedLevel());
    addRowSelectListener(this.roomTable, false);

    JScrollPane roomScrollPane = SwingTools.createScrollPane(this.roomTable);
    roomScrollPane.setBorder(BorderFactory.createEmptyBorder());
    AlpCatalogStyles.applyCatalogScrollPane(roomScrollPane);
    enableViewportFocus(roomScrollPane);

    JTabbedPane tabbedPane = new JTabbedPane();
    AlpCatalogStyles.applyInventoryTabbedPane(tabbedPane);
    tabbedPane.addTab(this.preferences.getLocalizedString(HomePane.class, "furnitureTab.title"),
        furnitureScrollPane);
    tabbedPane.addTab(this.preferences.getLocalizedString(HomePane.class, "areasTab.title"),
        roomScrollPane);
    AlpCatalogStyles.updateInventoryTabColors(tabbedPane);

    add(tabbedPane, BorderLayout.CENTER);

    home.addPropertyChangeListener(Home.Property.SELECTED_LEVEL,
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            refreshForSelectedLevel();
          }
        });
  }

  private FurnitureView.FurnitureFilter createLevelFilter() {
    return new FurnitureView.FurnitureFilter() {
        public boolean include(Home home, HomePieceOfFurniture piece) {
          Level selectedLevel = home.getSelectedLevel();
          return selectedLevel != null && piece.getLevel() == selectedLevel;
        }
      };
  }

  private void refreshForSelectedLevel() {
    Level selectedLevel = this.home.getSelectedLevel();
    this.furnitureTable.setFurnitureFilter(createLevelFilter());
    this.roomTable.setLevelFilter(selectedLevel);
  }

  private void addRowSelectListener(final javax.swing.JTable table, final boolean furnitureTable) {
    table.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent ev) {
          if (ev.getClickCount() >= 1 && table.getSelectedRow() >= 0) {
            if (furnitureTable) {
              Object value = table.getModel().getValueAt(table.getSelectedRow(), 0);
              if (value instanceof HomePieceOfFurniture) {
                controller.getPlanController().selectItem((HomePieceOfFurniture)value);
              }
            } else {
              Room room = roomTable.getRoomAt(table.getSelectedRow());
              if (room != null) {
                controller.getPlanController().selectItem(room);
              }
            }
          }
        }
      });
  }

  private static void enableViewportFocus(JScrollPane scrollPane) {
    final JViewport viewport = scrollPane.getViewport();
    viewport.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent ev) {
          viewport.getView().requestFocusInWindow();
        }
      });
  }
}
