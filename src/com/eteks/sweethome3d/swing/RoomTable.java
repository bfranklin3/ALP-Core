/*
 * RoomTable.java
 *
 * Sweet Home 3D, Copyright (c) 2024 Space Mushrooms <info@sweethome3d.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */
package com.eteks.sweethome3d.swing;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.KeyboardFocusManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.eteks.sweethome3d.model.CollectionEvent;
import com.eteks.sweethome3d.model.CollectionListener;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.Level;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.SelectionEvent;
import com.eteks.sweethome3d.model.SelectionListener;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.viewcontroller.HomeController;
import com.eteks.sweethome3d.viewcontroller.PlanController;

/**
 * A table displaying home rooms/areas.
 */
public class RoomTable extends JTable {
  private UserPreferences        preferences;
  private ListSelectionListener  tableSelectionListener;
  private boolean                selectionByUser;

  public RoomTable(final Home home, UserPreferences preferences, final HomeController controller) {
    this.preferences = preferences;
    float resolutionScale = SwingTools.getResolutionScale();
    if (resolutionScale != 1) {
      setRowHeight(Math.round(getRowHeight() * resolutionScale));
    }
    
    setModel(new RoomTableModel(home));
    setColumnModel(new RoomTableColumnModel(home, preferences));
    
    updateTableSelectedRooms(home);
    
    addSelectionListeners(home, controller);
    addHomeListener(home);
    addMouseListener(controller);
  }

  private void addSelectionListeners(final Home home, final HomeController controller) {
    final SelectionListener homeSelectionListener = new SelectionListener() {
      public void selectionChanged(SelectionEvent ev) {
        updateTableSelectedRooms(home);
      }
    };

    this.tableSelectionListener = new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent ev) {
        if (!ev.getValueIsAdjusting()) {
          selectionByUser = true;
          int[] selectedRows = getSelectedRows();
          List<Room> selectedRooms = new ArrayList<Room>();
          RoomTableModel tableModel = (RoomTableModel) getModel();
          for (int index : selectedRows) {
            selectedRooms.add(tableModel.getRoom(index));
          }
          home.setSelectedItems(new ArrayList<Selectable>(selectedRooms));
          selectionByUser = false;
        }
      }
    };
    
    getSelectionModel().addListSelectionListener(this.tableSelectionListener);
    home.addSelectionListener(homeSelectionListener);
  }

  private void updateTableSelectedRooms(Home home) {
    ListSelectionModel selectionModel = getSelectionModel();
    selectionModel.removeListSelectionListener(this.tableSelectionListener);

    RoomTableModel tableModel = (RoomTableModel) getModel();
    List<Selectable> selectedItems = home.getSelectedItems();
    List<Room> selectedRooms = Home.getRoomsSubList(selectedItems);
    
    clearSelection();
    for (Room room : selectedRooms) {
      int rowIndex = tableModel.getRoomIndex(room);
      if (rowIndex != -1) {
        addRowSelectionInterval(rowIndex, rowIndex);
      }
    }

    selectionModel.addListSelectionListener(this.tableSelectionListener);
  }

  private void addHomeListener(final Home home) {
    home.addRoomsListener(new CollectionListener<Room>() {
      public void collectionChanged(CollectionEvent<Room> ev) {
        ((RoomTableModel) getModel()).updateRooms();
      }
    });
  }

  private void addMouseListener(final HomeController controller) {
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent ev) {
        if (ev.getClickCount() == 2 && getSelectedRowCount() > 0) {
          controller.getPlanController().modifySelectedRooms();
        }
      }
    });
  }

  private class RoomTableModel extends AbstractTableModel {
    private final Home home;
    private List<Room> rooms;

    public RoomTableModel(Home home) {
      this.home = home;
      updateRooms();
    }

    public void updateRooms() {
      this.rooms = new ArrayList<Room>(this.home.getRooms());
      fireTableDataChanged();
    }

    public int getRowCount() {
      return this.rooms.size();
    }

    public int getColumnCount() {
      return 5; // Name, Level, Area, Floor Visible, Area Label Visible
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
      Room room = this.rooms.get(rowIndex);
      switch (columnIndex) {
        case 0: return room.getName() == null ? "" : room.getName();
        case 1: return room.getLevel() == null ? "" : room.getLevel().getName();
        case 2: return String.format("%.2f", room.getArea());
        case 3: return room.isFloorVisible();
        case 4: return room.isAreaVisible();
        default: return null;
      }
    }

    public Room getRoom(int rowIndex) {
      return this.rooms.get(rowIndex);
    }

    public int getRoomIndex(Room room) {
      return this.rooms.indexOf(room);
    }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
      if (columnIndex >= 3) {
        return Boolean.class;
      }
      return String.class;
    }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex >= 3; // Fills and Labels toggles
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      Room room = this.rooms.get(rowIndex);
      if (columnIndex == 3) {
        room.setFloorVisible((Boolean) aValue);
      } else if (columnIndex == 4) {
        room.setAreaVisible((Boolean) aValue);
      }
    }
  }

  private class RoomTableColumnModel extends DefaultTableColumnModel {
    public RoomTableColumnModel(Home home, UserPreferences preferences) {
      String[] columnNames = {
        preferences.getLocalizedString(RoomTable.class, "nameColumn.label"),
        preferences.getLocalizedString(RoomTable.class, "levelColumn.label"),
        preferences.getLocalizedString(RoomTable.class, "areaColumn.label"),
        preferences.getLocalizedString(RoomTable.class, "floorVisibleColumn.label"),
        preferences.getLocalizedString(RoomTable.class, "areaVisibleColumn.label")
      };
      
      for (int i = 0; i < columnNames.length; i++) {
        TableColumn column = new TableColumn(i);
        column.setHeaderValue(columnNames[i]);
        addColumn(column);
      }
    }
  }
}
