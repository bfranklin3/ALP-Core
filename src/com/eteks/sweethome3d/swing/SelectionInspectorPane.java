/*
 * SelectionInspectorPane.java
 *
 * Sweet Home 3D, Copyright (c) 2024 Space Mushrooms <info@sweethome3d.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */
package com.eteks.sweethome3d.swing;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.text.MessageFormat;

import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.eteks.sweethome3d.model.CollectionEvent;
import com.eteks.sweethome3d.model.CollectionListener;
import com.eteks.sweethome3d.model.DimensionLine;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.HomeTexture;
import com.eteks.sweethome3d.model.Label;
import com.eteks.sweethome3d.model.Level;
import com.eteks.sweethome3d.model.LevelCategory;
import com.eteks.sweethome3d.model.LevelPlantTakeoff;
import com.eteks.sweethome3d.model.Library;
import com.eteks.sweethome3d.model.Polyline;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.SelectionEvent;
import com.eteks.sweethome3d.model.SelectionListener;
import com.eteks.sweethome3d.model.TextureImage;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.model.Wall;
import com.eteks.sweethome3d.tools.AlpColorSupport;
import com.eteks.sweethome3d.tools.OperatingSystem;
import com.eteks.sweethome3d.viewcontroller.DimensionLineController;
import com.eteks.sweethome3d.viewcontroller.HomeController;
import com.eteks.sweethome3d.viewcontroller.HomeController;
import com.eteks.sweethome3d.viewcontroller.HomeFurnitureController;
import com.eteks.sweethome3d.viewcontroller.LabelController;
import com.eteks.sweethome3d.viewcontroller.LevelController;
import com.eteks.sweethome3d.viewcontroller.PolylineController;
import com.eteks.sweethome3d.viewcontroller.RoomController;
import com.eteks.sweethome3d.viewcontroller.TextureChoiceController;
import com.eteks.sweethome3d.viewcontroller.WallController;

/**
 * Docked selection-mode inspector for SPIKE-19 / SPIKE-21.
 */
public class SelectionInspectorPane extends JPanel {
  private static final String EMPTY_CARD = "empty";
  private static final String ROOM_CARD = "room";
  private static final String POLYLINE_CARD = "polyline";
  private static final String LABEL_CARD = "label";
  private static final String DIMENSION_CARD = "dimension";
  private static final String WALL_CARD = "wall";
  private static final String FURNITURE_CARD = "furniture";
  private static final String LEVEL_CARD = "level";

  private final Home              home;
  private final HomeController    homeController;
  private final UserPreferences   preferences;
  private final CardLayout        cardLayout;
  private final JPanel            cardPanel;
  private final JLabel            emptyLabel;
  private final RoomInspectorPanel roomInspectorPanel;
  private final PolylineInspectorPanel polylineInspectorPanel;
  private final LabelInspectorPanel labelInspectorPanel;
  private final DimensionLineInspectorPanel dimensionLineInspectorPanel;
  private final WallInspectorPanel wallInspectorPanel;
  private final FurnitureInspectorPanel furnitureInspectorPanel;
  private final LevelInspectorPanel levelInspectorPanel;
  private final List<Room>        monitoredRooms;
  private final List<Polyline>    monitoredPolylines;
  private final List<Label>       monitoredLabels;
  private final List<DimensionLine> monitoredDimensionLines;
  private final List<Wall>        monitoredWalls;
  private final List<HomePieceOfFurniture> monitoredFurniture;
  private boolean                 showingRoomInspector;
  private boolean                 showingPolylineInspector;
  private boolean                 showingLabelInspector;
  private boolean                 showingDimensionLineInspector;
  private boolean                 showingWallInspector;
  private boolean                 showingFurnitureInspector;
  private boolean                 showingLevelInspector;
  private boolean                 suppressSelectionListener;
  private final PropertyChangeListener roomPropertyListener;
  private final PropertyChangeListener polylinePropertyListener;
  private final PropertyChangeListener labelPropertyListener;
  private final PropertyChangeListener dimensionLinePropertyListener;
  private final PropertyChangeListener wallPropertyListener;
  private final PropertyChangeListener furniturePropertyListener;

  public SelectionInspectorPane(Home home,
                                UserPreferences preferences,
                                HomeController controller) {
    super(new BorderLayout());
    this.home = home;
    this.homeController = controller;
    this.preferences = preferences;
    this.cardLayout = new CardLayout();
    this.cardPanel = new JPanel(this.cardLayout);
    this.emptyLabel = new JLabel("", JLabel.CENTER);
    this.emptyLabel.setBorder(AlpInspectorStyles.emptyStateBorder());
    this.emptyLabel.setFocusable(false);
    this.roomInspectorPanel = new RoomInspectorPanel(home, preferences, controller);
    this.polylineInspectorPanel = new PolylineInspectorPanel(home, preferences, controller);
    this.labelInspectorPanel = new LabelInspectorPanel(home, preferences, controller);
    this.dimensionLineInspectorPanel = new DimensionLineInspectorPanel(home, preferences, controller);
    this.wallInspectorPanel = new WallInspectorPanel(home, preferences, controller);
    this.furnitureInspectorPanel = new FurnitureInspectorPanel(home, preferences, controller);
    this.levelInspectorPanel = new LevelInspectorPanel(home, preferences, controller);
    this.monitoredRooms = new ArrayList<Room>();
    this.monitoredPolylines = new ArrayList<Polyline>();
    this.monitoredLabels = new ArrayList<Label>();
    this.monitoredDimensionLines = new ArrayList<DimensionLine>();
    this.monitoredWalls = new ArrayList<Wall>();
    this.monitoredFurniture = new ArrayList<HomePieceOfFurniture>();
    this.roomPropertyListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (!showingRoomInspector) {
            return;
          }
          if (Room.Property.NAME.name().equals(ev.getPropertyName())) {
            roomInspectorPanel.syncNameFieldFromModel();
          } else {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                  roomInspectorPanel.refresh();
                }
              });
          }
        }
      };
    this.polylinePropertyListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (!showingPolylineInspector) {
            return;
          }
          EventQueue.invokeLater(new Runnable() {
              public void run() {
                polylineInspectorPanel.refresh();
              }
            });
        }
      };
    this.labelPropertyListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (!showingLabelInspector) {
            return;
          }
          if (Label.Property.TEXT.name().equals(ev.getPropertyName())) {
            labelInspectorPanel.syncTextFieldFromModel();
          } else {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                  labelInspectorPanel.refresh();
                }
              });
          }
        }
      };
    this.dimensionLinePropertyListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (!showingDimensionLineInspector) {
            return;
          }
          EventQueue.invokeLater(new Runnable() {
              public void run() {
                dimensionLineInspectorPanel.refresh();
              }
            });
        }
      };
    this.wallPropertyListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (!showingWallInspector) {
            return;
          }
          EventQueue.invokeLater(new Runnable() {
              public void run() {
                wallInspectorPanel.refresh();
              }
            });
        }
      };
    this.furniturePropertyListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (!showingFurnitureInspector) {
            return;
          }
          if (HomePieceOfFurniture.Property.NAME.name().equals(ev.getPropertyName())) {
            furnitureInspectorPanel.syncNameFieldFromModel();
          } else {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                  furnitureInspectorPanel.refresh();
                }
              });
          }
        }
      };

    this.cardPanel.add(this.emptyLabel, EMPTY_CARD);
    this.cardPanel.add(this.roomInspectorPanel, ROOM_CARD);
    this.cardPanel.add(this.polylineInspectorPanel, POLYLINE_CARD);
    this.cardPanel.add(this.labelInspectorPanel, LABEL_CARD);
    this.cardPanel.add(this.dimensionLineInspectorPanel, DIMENSION_CARD);
    this.cardPanel.add(this.wallInspectorPanel, WALL_CARD);
    this.cardPanel.add(this.furnitureInspectorPanel, FURNITURE_CARD);
    this.cardPanel.add(this.levelInspectorPanel, LEVEL_CARD);
    add(this.cardPanel, BorderLayout.NORTH);
    applyInspectorWorkspaceStyle();
    setMinimumSize(new Dimension(Math.max(1, (int)(200 * SwingTools.getResolutionScale())), 0));
    setPreferredSize(new Dimension(Math.max(1, (int)(300 * SwingTools.getResolutionScale())), 0));
    updateInspectorPanelsEnabled();

    home.addSelectionListener(new SelectionListener() {
        public void selectionChanged(SelectionEvent ev) {
          if (suppressSelectionListener) {
            return;
          }
          // monitoredRooms still refers to the previous selection here.
          roomInspectorPanel.commitPendingEditsForRooms(
              new ArrayList<Room>(monitoredRooms));
          labelInspectorPanel.commitPendingEditsForLabels(
              new ArrayList<Label>(monitoredLabels));
          furnitureInspectorPanel.commitPendingEditsForFurniture(
              new ArrayList<HomePieceOfFurniture>(monitoredFurniture));
          levelInspectorPanel.commitPendingEdits();
          updateForSelection();
        }
      });
    home.addPropertyChangeListener(Home.Property.SELECTED_LEVEL, new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (home.getSelectedItems().isEmpty()) {
            levelInspectorPanel.commitPendingEdits();
            showLevelInspector();
          }
        }
      });
    home.addLevelsListener(new CollectionListener<Level>() {
        public void collectionChanged(CollectionEvent<Level> ev) {
          if (showingLevelInspector) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                  levelInspectorPanel.refresh();
                }
              });
          }
        }
      });
    updateForSelection();
  }

  private void applyInspectorWorkspaceStyle() {
    AlpCatalogStyles.applyWorkspacePanel(this);
    AlpCatalogStyles.applyWorkspacePanel(this.cardPanel);
    AlpCatalogStyles.applyWorkspacePanel(this.emptyLabel);
    AlpCatalogStyles.applyWorkspacePanel(this.roomInspectorPanel);
    AlpCatalogStyles.applyWorkspacePanel(this.polylineInspectorPanel);
    AlpCatalogStyles.applyWorkspacePanel(this.labelInspectorPanel);
    AlpCatalogStyles.applyWorkspacePanel(this.dimensionLineInspectorPanel);
    AlpCatalogStyles.applyWorkspacePanel(this.wallInspectorPanel);
    AlpCatalogStyles.applyWorkspacePanel(this.furnitureInspectorPanel);
    AlpCatalogStyles.applyWorkspacePanel(this.levelInspectorPanel);
  }

  private void updateForSelection() {
    List<Selectable> selectedItems = this.home.getSelectedItems();
    if (selectedItems.isEmpty()) {
      if (this.home.getSelectedLevel() != null) {
        showLevelInspector();
      } else {
        showEmpty("emptySelection.message");
      }
      return;
    }

    List<Room> selectedRooms = Home.getRoomsSubList(selectedItems);
    if (!selectedRooms.isEmpty() && selectedItems.size() == selectedRooms.size()) {
      for (Room room : selectedRooms) {
        if (isRoomLocked(room)) {
          showEmpty("lockedRoom.message");
          return;
        }
      }
      updateMonitoredPolylines(null);
      updateMonitoredLabels(null);
      updateMonitoredDimensionLines(null);
      updateMonitoredWalls(null);
      updateMonitoredFurniture(null);
      updateMonitoredRooms(selectedRooms);
      this.showingRoomInspector = true;
      this.showingPolylineInspector = false;
      this.showingLabelInspector = false;
      this.showingDimensionLineInspector = false;
      this.showingWallInspector = false;
      this.showingFurnitureInspector = false;
      this.showingLevelInspector = false;
      updateInspectorPanelsEnabled();
      this.roomInspectorPanel.clearPendingFloorTextureMode();
      this.roomInspectorPanel.refresh();
      this.cardLayout.show(this.cardPanel, ROOM_CARD);
      return;
    }

    List<Polyline> selectedPolylines = Home.getPolylinesSubList(selectedItems);
    if (!selectedPolylines.isEmpty() && selectedItems.size() == selectedPolylines.size()) {
      for (Polyline polyline : selectedPolylines) {
        if (isPolylineLocked(polyline)) {
          showEmpty("lockedPolyline.message");
          return;
        }
      }
      updateMonitoredRooms(null);
      updateMonitoredLabels(null);
      updateMonitoredDimensionLines(null);
      updateMonitoredWalls(null);
      updateMonitoredFurniture(null);
      updateMonitoredPolylines(selectedPolylines);
      this.showingRoomInspector = false;
      this.showingPolylineInspector = true;
      this.showingLabelInspector = false;
      this.showingDimensionLineInspector = false;
      this.showingWallInspector = false;
      this.showingFurnitureInspector = false;
      this.showingLevelInspector = false;
      updateInspectorPanelsEnabled();
      this.polylineInspectorPanel.refresh();
      this.cardLayout.show(this.cardPanel, POLYLINE_CARD);
      return;
    }

    List<Label> selectedLabels = Home.getLabelsSubList(selectedItems);
    if (!selectedLabels.isEmpty() && selectedItems.size() == selectedLabels.size()) {
      for (Label label : selectedLabels) {
        if (isLabelLocked(label)) {
          showEmpty("lockedLabel.message");
          return;
        }
      }
      updateMonitoredRooms(null);
      updateMonitoredPolylines(null);
      updateMonitoredDimensionLines(null);
      updateMonitoredWalls(null);
      updateMonitoredFurniture(null);
      updateMonitoredLabels(selectedLabels);
      this.showingRoomInspector = false;
      this.showingPolylineInspector = false;
      this.showingLabelInspector = true;
      this.showingDimensionLineInspector = false;
      this.showingWallInspector = false;
      this.showingFurnitureInspector = false;
      this.showingLevelInspector = false;
      updateInspectorPanelsEnabled();
      this.labelInspectorPanel.refresh();
      this.cardLayout.show(this.cardPanel, LABEL_CARD);
      return;
    }

    List<DimensionLine> selectedDimensionLines = Home.getDimensionLinesSubList(selectedItems);
    if (!selectedDimensionLines.isEmpty() && selectedItems.size() == selectedDimensionLines.size()) {
      for (DimensionLine dimensionLine : selectedDimensionLines) {
        if (isDimensionLineLocked(dimensionLine)) {
          showEmpty("lockedDimensionLine.message");
          return;
        }
      }
      updateMonitoredRooms(null);
      updateMonitoredPolylines(null);
      updateMonitoredLabels(null);
      updateMonitoredWalls(null);
      updateMonitoredFurniture(null);
      updateMonitoredDimensionLines(selectedDimensionLines);
      this.showingRoomInspector = false;
      this.showingPolylineInspector = false;
      this.showingLabelInspector = false;
      this.showingDimensionLineInspector = true;
      this.showingWallInspector = false;
      this.showingFurnitureInspector = false;
      this.showingLevelInspector = false;
      updateInspectorPanelsEnabled();
      this.dimensionLineInspectorPanel.refresh();
      this.cardLayout.show(this.cardPanel, DIMENSION_CARD);
      return;
    }

    List<Wall> selectedWalls = Home.getWallsSubList(selectedItems);
    if (!selectedWalls.isEmpty() && selectedItems.size() == selectedWalls.size()) {
      for (Wall wall : selectedWalls) {
        if (isWallLocked(wall)) {
          showEmpty("lockedWall.message");
          return;
        }
      }
      updateMonitoredRooms(null);
      updateMonitoredPolylines(null);
      updateMonitoredLabels(null);
      updateMonitoredDimensionLines(null);
      updateMonitoredFurniture(null);
      updateMonitoredWalls(selectedWalls);
      this.showingRoomInspector = false;
      this.showingPolylineInspector = false;
      this.showingLabelInspector = false;
      this.showingDimensionLineInspector = false;
      this.showingWallInspector = true;
      this.showingFurnitureInspector = false;
      this.showingLevelInspector = false;
      updateInspectorPanelsEnabled();
      this.wallInspectorPanel.refresh();
      this.cardLayout.show(this.cardPanel, WALL_CARD);
      return;
    }

    List<HomePieceOfFurniture> selectedFurniture = Home.getFurnitureSubList(selectedItems);
    if (!selectedFurniture.isEmpty() && selectedItems.size() == selectedFurniture.size()) {
      for (HomePieceOfFurniture piece : selectedFurniture) {
        if (isFurnitureLocked(piece)) {
          showEmpty("lockedFurniture.message");
          return;
        }
      }
      updateMonitoredRooms(null);
      updateMonitoredPolylines(null);
      updateMonitoredLabels(null);
      updateMonitoredDimensionLines(null);
      updateMonitoredWalls(null);
      updateMonitoredFurniture(selectedFurniture);
      this.showingRoomInspector = false;
      this.showingPolylineInspector = false;
      this.showingLabelInspector = false;
      this.showingDimensionLineInspector = false;
      this.showingWallInspector = false;
      this.showingFurnitureInspector = true;
      this.showingLevelInspector = false;
      updateInspectorPanelsEnabled();
      this.furnitureInspectorPanel.refresh();
      this.cardLayout.show(this.cardPanel, FURNITURE_CARD);
      return;
    }

    showEmpty("mixedSelection.message");
  }

  private void showEmpty(String messageKey) {
    this.showingRoomInspector = false;
    this.showingPolylineInspector = false;
    this.showingLabelInspector = false;
    this.showingDimensionLineInspector = false;
    this.showingWallInspector = false;
    this.showingFurnitureInspector = false;
    this.showingLevelInspector = false;
    updateMonitoredRooms(null);
    updateMonitoredPolylines(null);
    updateMonitoredLabels(null);
    updateMonitoredDimensionLines(null);
    updateMonitoredWalls(null);
    updateMonitoredFurniture(null);
    this.emptyLabel.setText(this.preferences.getLocalizedString(
        SelectionInspectorPane.class, messageKey));
    this.cardLayout.show(this.cardPanel, EMPTY_CARD);
    updateInspectorPanelsEnabled();
  }

  /**
   * Shows the docked layer inspector for the active plan tab (SPIKE-24).
   */
  private void showLevelInspector() {
    this.showingRoomInspector = false;
    this.showingPolylineInspector = false;
    this.showingLabelInspector = false;
    this.showingDimensionLineInspector = false;
    this.showingWallInspector = false;
    this.showingFurnitureInspector = false;
    this.showingLevelInspector = true;
    updateMonitoredRooms(null);
    updateMonitoredPolylines(null);
    updateMonitoredLabels(null);
    updateMonitoredDimensionLines(null);
    updateMonitoredWalls(null);
    updateMonitoredFurniture(null);
    updateInspectorPanelsEnabled();
    this.levelInspectorPanel.refresh();
    this.cardLayout.show(this.cardPanel, LEVEL_CARD);
  }

  /**
   * Enables only the active inspector card so tab order skips hidden panels.
   */
  private void updateInspectorPanelsEnabled() {
    setInspectorCardEnabled(this.roomInspectorPanel, this.showingRoomInspector);
    setInspectorCardEnabled(this.polylineInspectorPanel, this.showingPolylineInspector);
    setInspectorCardEnabled(this.labelInspectorPanel, this.showingLabelInspector);
    setInspectorCardEnabled(this.dimensionLineInspectorPanel, this.showingDimensionLineInspector);
    setInspectorCardEnabled(this.wallInspectorPanel, this.showingWallInspector);
    setInspectorCardEnabled(this.furnitureInspectorPanel, this.showingFurnitureInspector);
    setInspectorCardEnabled(this.levelInspectorPanel, this.showingLevelInspector);
  }

  /**
   * Toggles an inspector card and its descendants so hidden CardLayout panels
   * are removed from keyboard focus traversal.
   */
  private static void setInspectorCardEnabled(JPanel panel, boolean enabled) {
    panel.setEnabled(enabled);
    setDescendantsEnabled(panel, enabled);
  }

  private static void setDescendantsEnabled(Container container, boolean enabled) {
    for (Component child : container.getComponents()) {
      child.setEnabled(enabled);
      if (child instanceof Container) {
        setDescendantsEnabled((Container) child, enabled);
      }
    }
  }

  /**
   * Wires a field label mnemonic (non-Mac) and keyboard activation target.
   */
  private static void configureInspectorFieldLabel(UserPreferences preferences,
                                                   Class<?> resourceClass,
                                                   String mnemonicKey,
                                                   JLabel label,
                                                   JComponent field) {
    if (!OperatingSystem.isMacOSX()) {
      label.setDisplayedMnemonic(KeyStroke.getKeyStroke(
          preferences.getLocalizedString(resourceClass, mnemonicKey)).getKeyCode());
    }
    label.setLabelFor(field);
  }

  /**
   * Inspector checkboxes should toggle on the first click even when plan view
   * still owns keyboard focus (macOS otherwise needs click-to-focus, then click-to-toggle).
   */
  private static JCheckBox createImmediateClickCheckBox(String text) {
    return new ImmediateClickCheckBox(text);
  }

  /**
   * Check box that toggles from mouse release without taking keyboard focus.
   */
  private static class ImmediateClickCheckBox extends JCheckBox {
    private boolean clickArmed;

    ImmediateClickCheckBox(String text) {
      super(text);
      setFocusable(false);
      setRequestFocusEnabled(false);
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
      if (!isEnabled()) {
        super.processMouseEvent(e);
        return;
      }
      switch (e.getID()) {
        case MouseEvent.MOUSE_PRESSED:
          if (SwingUtilities.isLeftMouseButton(e)) {
            this.clickArmed = true;
            ButtonModel model = getModel();
            model.setArmed(true);
            model.setPressed(true);
          }
          break;
        case MouseEvent.MOUSE_RELEASED:
          if (SwingUtilities.isLeftMouseButton(e) && this.clickArmed && contains(e.getPoint())) {
            ButtonModel model = getModel();
            model.setArmed(false);
            model.setPressed(false);
            model.setSelected(!model.isSelected());
            fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click"));
          } else {
            getModel().setArmed(false);
            getModel().setPressed(false);
          }
          this.clickArmed = false;
          break;
        case MouseEvent.MOUSE_EXITED:
          // Keep clickArmed until release; a overlapping sibling can spuriously exit.
          break;
        default:
          super.processMouseEvent(e);
          break;
      }
    }
  }

  private void updateMonitoredRooms(List<Room> rooms) {
    for (Room room : this.monitoredRooms) {
      room.removePropertyChangeListener(this.roomPropertyListener);
    }
    this.monitoredRooms.clear();
    if (rooms != null) {
      for (Room room : rooms) {
        room.addPropertyChangeListener(this.roomPropertyListener);
        this.monitoredRooms.add(room);
      }
    }
  }

  private void updateMonitoredPolylines(List<Polyline> polylines) {
    for (Polyline polyline : this.monitoredPolylines) {
      polyline.removePropertyChangeListener(this.polylinePropertyListener);
    }
    this.monitoredPolylines.clear();
    if (polylines != null) {
      for (Polyline polyline : polylines) {
        polyline.addPropertyChangeListener(this.polylinePropertyListener);
        this.monitoredPolylines.add(polyline);
      }
    }
  }

  private static boolean isRoomLocked(Room room) {
    Level level = room.getLevel();
    return level != null && level.isLocked();
  }

  private static boolean isPolylineLocked(Polyline polyline) {
    Level level = polyline.getLevel();
    return level != null && level.isLocked();
  }

  private void updateMonitoredLabels(List<Label> labels) {
    for (Label label : this.monitoredLabels) {
      label.removePropertyChangeListener(this.labelPropertyListener);
    }
    this.monitoredLabels.clear();
    if (labels != null) {
      for (Label label : labels) {
        label.addPropertyChangeListener(this.labelPropertyListener);
        this.monitoredLabels.add(label);
      }
    }
  }

  private static boolean isLabelLocked(Label label) {
    Level level = label.getLevel();
    return level != null && level.isLocked();
  }

  private void updateMonitoredDimensionLines(List<DimensionLine> dimensionLines) {
    for (DimensionLine dimensionLine : this.monitoredDimensionLines) {
      dimensionLine.removePropertyChangeListener(this.dimensionLinePropertyListener);
    }
    this.monitoredDimensionLines.clear();
    if (dimensionLines != null) {
      for (DimensionLine dimensionLine : dimensionLines) {
        dimensionLine.addPropertyChangeListener(this.dimensionLinePropertyListener);
        this.monitoredDimensionLines.add(dimensionLine);
      }
    }
  }

  private static boolean isDimensionLineLocked(DimensionLine dimensionLine) {
    Level level = dimensionLine.getLevel();
    return level != null && level.isLocked();
  }

  private void updateMonitoredWalls(List<Wall> walls) {
    for (Wall wall : this.monitoredWalls) {
      wall.removePropertyChangeListener(this.wallPropertyListener);
    }
    this.monitoredWalls.clear();
    if (walls != null) {
      for (Wall wall : walls) {
        wall.addPropertyChangeListener(this.wallPropertyListener);
        this.monitoredWalls.add(wall);
      }
    }
  }

  private static boolean isWallLocked(Wall wall) {
    Level level = wall.getLevel();
    return level != null && level.isLocked();
  }

  private void updateMonitoredFurniture(List<HomePieceOfFurniture> furniture) {
    for (HomePieceOfFurniture piece : this.monitoredFurniture) {
      piece.removePropertyChangeListener(this.furniturePropertyListener);
    }
    this.monitoredFurniture.clear();
    if (furniture != null) {
      for (HomePieceOfFurniture piece : furniture) {
        piece.addPropertyChangeListener(this.furniturePropertyListener);
        this.monitoredFurniture.add(piece);
      }
    }
  }

  private static boolean isFurnitureLocked(HomePieceOfFurniture piece) {
    Level level = piece.getLevel();
    return level != null && level.isLocked();
  }

  /**
   * Area/Room inspector with live edit fields (SPIKE-21 P0/P1).
   */
  private class RoomInspectorPanel extends JPanel {
    private final Home             home;
    private final HomeController   homeController;
    private final RoomController   roomController;
    private final UserPreferences  preferences;
    private JLabel                 summaryLabel;
    private JButton                openFullEditorButton;
    private JTextField             nameTextField;
    private JLabel                 levelFieldLabel;
    private JLabel                 levelValueLabel;
    private NullableCheckBox       nameVisibleCheckBox;
    private NullableCheckBox       areaVisibleCheckBox;
    private ColorButton            floorColorButton;
    private JRadioButton           floorColorRadioButton;
    private JRadioButton           floorTextureRadioButton;
    private JLabel                 floorTextureLibraryLabel;
    private JComboBox              floorTextureLibraryComboBox;
    private JLabel                 floorTextureCategoryLabel;
    private JComboBox              floorTextureCategoryComboBox;
    private JLabel                 floorTextureLabel;
    private JComponent             floorTextureComponent;
    private boolean                floorPaintControlsAvailable;
    private boolean                updatingFloorTextureFilters;
    // Texture mode selected in the UI but not yet applied to the rooms because no texture is chosen
    private boolean                pendingFloorTextureMode;
    private JLabel                 floorOpacityLabel;
    private NullableSpinner          floorOpacitySpinner;
    private NullableSpinner.NullableSpinnerNumberModel floorOpacitySpinnerModel;
    private NullableCheckBox       smoothedCheckBox;
    private JLabel                 outlineThicknessLabel;
    private NullableSpinner        outlineThicknessSpinner;
    private NullableSpinner.NullableSpinnerLengthModel outlineThicknessSpinnerModel;
    private JLabel                 outlineDashStyleLabel;
    private JComboBox              outlineDashStyleComboBox;
    private JLabel                 outlineColorLabel;
    private ColorButton            outlineColorButton;
    private String                 nameFieldSyncedValue;
    private boolean                nameFieldUserEdited;
    private boolean                updatingFromController;

    RoomInspectorPanel(Home home,
                       UserPreferences preferences,
                       HomeController controller) {
      super(new BorderLayout());
      this.home = home;
      this.homeController = controller;
      this.preferences = preferences;
      this.roomController = controller.createRoomController();
      createHeader();
      createFields();
      layoutFields();
    }

    private void createHeader() {
      this.summaryLabel = new JLabel();
      this.openFullEditorButton = new JButton(this.preferences.getLocalizedString(
          SelectionInspectorPane.class, "openFullEditorButton.text"));
      if (!OperatingSystem.isMacOSX()) {
        this.openFullEditorButton.setMnemonic(KeyStroke.getKeyStroke(
            this.preferences.getLocalizedString(
                SelectionInspectorPane.class, "openFullEditorButton.mnemonic")).getKeyCode());
      }
      this.openFullEditorButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            commitPendingEdits();
            homeController.getPlanController().modifySelectedRooms();
          }
        });
    }

    private void createFields() {
      this.nameTextField = new AutoCompleteTextField(
          "", 10, this.preferences.getAutoCompletionStrings("RoomName"));
      if (!OperatingSystem.isMacOSXLeopardOrSuperior()) {
        SwingTools.addAutoSelectionOnFocusGain(this.nameTextField);
      }
      final PropertyChangeListener nameChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            syncNameFieldFromModel();
          }
        };
      this.roomController.addPropertyChangeListener(
          RoomController.Property.NAME, nameChangeListener);
      this.nameTextField.getDocument().addDocumentListener(new DocumentListener() {
          public void changedUpdate(DocumentEvent ev) {
            updateControllerNameFromField(nameChangeListener);
          }

          public void insertUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }

          public void removeUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
        });
      this.nameTextField.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            commitPendingEdits();
          }
        });
      this.nameTextField.addFocusListener(new FocusAdapter() {
          @Override
          public void focusLost(FocusEvent ev) {
            commitPendingEdits();
          }
        });
      this.nameTextField.addKeyListener(new KeyAdapter() {
          @Override
          public void keyPressed(KeyEvent ev) {
            if (ev.getKeyCode() == KeyEvent.VK_ENTER) {
              commitPendingEdits();
            }
          }
        });

      this.nameVisibleCheckBox = new NullableCheckBox(SwingTools.getLocalizedLabelText(
          this.preferences, RoomPanel.class, "nameVisibleCheckBox.text"));
      if (!OperatingSystem.isMacOSX()) {
        this.nameVisibleCheckBox.setMnemonic(KeyStroke.getKeyStroke(
            this.preferences.getLocalizedString(
                RoomPanel.class, "nameVisibleCheckBox.mnemonic")).getKeyCode());
      }
      this.nameVisibleCheckBox.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            if (updatingFromController) {
              return;
            }
            roomController.setNameVisible(nameVisibleCheckBox.getValue());
            applyRoomChanges();
          }
        });

      this.areaVisibleCheckBox = new NullableCheckBox(SwingTools.getLocalizedLabelText(
          this.preferences, RoomPanel.class, "areaVisibleCheckBox.text"));
      if (!OperatingSystem.isMacOSX()) {
        this.areaVisibleCheckBox.setMnemonic(KeyStroke.getKeyStroke(
            this.preferences.getLocalizedString(
                RoomPanel.class, "areaVisibleCheckBox.mnemonic")).getKeyCode());
      }
      this.areaVisibleCheckBox.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            if (updatingFromController) {
              return;
            }
            roomController.setAreaVisible(areaVisibleCheckBox.getValue());
            applyRoomChanges();
          }
        });

      this.floorColorButton = new ColorButton(this.preferences);
      this.floorColorButton.setNullColorAllowed(true);
      this.floorColorButton.setColorDialogTitle(this.preferences.getLocalizedString(
          RoomPanel.class, "floorColorDialog.title"));
      this.floorColorButton.addPropertyChangeListener(ColorButton.COLOR_PROPERTY,
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              if (updatingFromController) {
                return;
              }
              pendingFloorTextureMode = false;
              roomController.setFloorColor(floorColorButton.getColor());
              roomController.setFloorPaint(RoomController.RoomPaint.COLORED);
              applyRoomChanges();
              updateFloorOpacityEnabled();
            }
          });
      this.roomController.addPropertyChangeListener(RoomController.Property.FLOOR_COLOR,
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              updateFloorOpacityEnabled();
            }
          });

      this.floorPaintControlsAvailable = this.roomController.isPropertyEditable(
          RoomController.Property.FLOOR_PAINT);
      if (this.floorPaintControlsAvailable) {
        this.floorColorRadioButton = new JRadioButton(SwingTools.getLocalizedLabelText(
            this.preferences, RoomPanel.class, "floorColorRadioButton.text"));
        this.floorColorRadioButton.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
              if (updatingFromController || !floorColorRadioButton.isSelected()) {
                return;
              }
              pendingFloorTextureMode = false;
              roomController.setFloorPaint(RoomController.RoomPaint.COLORED);
              applyRoomChanges();
              updateFloorPaintControlsVisibility();
            }
          });
        this.floorTextureRadioButton = new JRadioButton(SwingTools.getLocalizedLabelText(
            this.preferences, RoomPanel.class, "floorTextureRadioButton.text"));
        this.floorTextureRadioButton.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
              if (updatingFromController || !floorTextureRadioButton.isSelected()) {
                return;
              }
              roomController.setFloorPaint(RoomController.RoomPaint.TEXTURED);
              // Applying textured paint without a texture would clear the rooms fill and reset
              // them to their default paint, so wait until a texture is actually chosen
              if (roomController.getFloorTextureController().getTexture() != null) {
                pendingFloorTextureMode = false;
                applyRoomChanges();
              } else {
                pendingFloorTextureMode = true;
              }
              updateFloorPaintControlsVisibility();
              updateFloorTextureFilterControls(true);
            }
          });
        ButtonGroup floorPaintButtonGroup = new ButtonGroup();
        floorPaintButtonGroup.add(this.floorColorRadioButton);
        floorPaintButtonGroup.add(this.floorTextureRadioButton);
        if (!OperatingSystem.isMacOSX()) {
          this.floorColorRadioButton.setMnemonic(KeyStroke.getKeyStroke(
              this.preferences.getLocalizedString(
                  RoomPanel.class, "floorColorRadioButton.mnemonic")).getKeyCode());
          this.floorTextureRadioButton.setMnemonic(KeyStroke.getKeyStroke(
              this.preferences.getLocalizedString(
                  RoomPanel.class, "floorTextureRadioButton.mnemonic")).getKeyCode());
        }
        this.floorTextureComponent = (JComponent)this.roomController.getFloorTextureController().getView();
        this.floorTextureLibraryLabel = new JLabel(this.preferences.getLocalizedString(
            SelectionInspectorPane.class, "floorTextureLibraryLabel.text"));
        this.floorTextureLibraryComboBox = new JComboBox();
        this.floorTextureLibraryComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
              super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
              if (value instanceof Library) {
                Library library = (Library)value;
                setText(library.getName() != null ? library.getName() : library.getId());
              }
              return this;
            }
          });
        this.floorTextureLibraryComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ev) {
              if (updatingFromController || updatingFloorTextureFilters
                  || ev.getStateChange() != ItemEvent.SELECTED) {
                return;
              }
              updateFloorTextureCategoryComboBox(false);
              applyFloorTextureFiltersFromControls();
            }
          });
        this.floorTextureCategoryLabel = new JLabel(this.preferences.getLocalizedString(
            SelectionInspectorPane.class, "floorTextureCategoryLabel.text"));
        this.floorTextureCategoryComboBox = new JComboBox();
        this.floorTextureCategoryComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ev) {
              if (updatingFromController || updatingFloorTextureFilters
                  || ev.getStateChange() != ItemEvent.SELECTED) {
                return;
              }
              applyFloorTextureFiltersFromControls();
            }
          });
        this.floorTextureLabel = new JLabel(this.preferences.getLocalizedString(
            SelectionInspectorPane.class, "floorTextureLabel.text"));
        configureInspectorFieldLabel(this.preferences, SelectionInspectorPane.class,
            "floorTextureLabel.mnemonic", this.floorTextureLabel, this.floorTextureComponent);
        this.roomController.addPropertyChangeListener(RoomController.Property.FLOOR_PAINT,
            new PropertyChangeListener() {
              public void propertyChange(PropertyChangeEvent ev) {
                if (updatingFromController) {
                  return;
                }
                updateFloorPaintRadioButtons();
                updateFloorPaintControlsVisibility();
              }
            });
        this.roomController.getFloorTextureController().addPropertyChangeListener(
            TextureChoiceController.Property.TEXTURE,
            new PropertyChangeListener() {
              public void propertyChange(PropertyChangeEvent ev) {
                if (updatingFromController) {
                  return;
                }
                if (pendingFloorTextureMode) {
                  pendingFloorTextureMode = false;
                  roomController.setFloorPaint(RoomController.RoomPaint.TEXTURED);
                }
                applyRoomChanges();
              }
            });
      }

      this.floorOpacityLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, RoomPanel.class, "floorOpacityLabel.text", "%"));
      this.floorOpacitySpinnerModel = new NullableSpinner.NullableSpinnerNumberModel(
          75f, 0f, 100f, 5f) {
          @Override
          Format getFormat() {
            return new DecimalFormat("0.#");
          }
        };
      this.floorOpacitySpinner = new NullableSpinner(this.floorOpacitySpinnerModel);
      this.floorOpacitySpinnerModel.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            if (updatingFromController) {
              return;
            }
            Number value = (Number)floorOpacitySpinnerModel.getValue();
            roomController.setFloorOpacity(value != null
                ? value.floatValue() / 100f
                : null);
            applyRoomChanges();
          }
        });

      if (this.roomController.isPropertyEditable(RoomController.Property.SMOOTHED)) {
        this.smoothedCheckBox = new NullableCheckBox(SwingTools.getLocalizedLabelText(
            this.preferences, RoomPanel.class, "smoothedCheckBox.text"));
        if (!OperatingSystem.isMacOSX()) {
          this.smoothedCheckBox.setMnemonic(KeyStroke.getKeyStroke(
              this.preferences.getLocalizedString(
                  RoomPanel.class, "smoothedCheckBox.mnemonic")).getKeyCode());
        }
        this.smoothedCheckBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
              if (updatingFromController) {
                return;
              }
              roomController.setSmoothed(smoothedCheckBox.getValue());
              applyRoomChanges();
            }
          });
      }

      this.outlineThicknessLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, PolylinePanel.class, "thicknessLabel.text",
          this.preferences.getLengthUnit().getName()));
      this.outlineThicknessSpinnerModel = new NullableSpinner.NullableSpinnerLengthModel(
          this.preferences, this.preferences.getLengthUnit().getMinimumLength(), 50f);
      this.outlineThicknessSpinner = new NullableSpinner(this.outlineThicknessSpinnerModel);
      this.outlineThicknessSpinnerModel.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            if (updatingFromController) {
              return;
            }
            roomController.setOutlineThickness(outlineThicknessSpinnerModel.getLength());
            applyRoomChanges();
          }
        });

      this.outlineDashStyleLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, PolylinePanel.class, "dashStyleLabel.text"));
      List<Polyline.DashStyle> dashStyles = AlpDashStyleSupport.getDashStyleChoicesForCombo(
          this.roomController.getOutlineDashStyle(), true);
      this.outlineDashStyleComboBox = new JComboBox(
          new DefaultComboBoxModel(dashStyles.toArray(new Polyline.DashStyle [dashStyles.size()])));
      this.outlineDashStyleComboBox.setRenderer(new AlpDashStyleListCellRenderer(this.preferences));
      this.outlineDashStyleComboBox.addItemListener(new ItemListener() {
          public void itemStateChanged(ItemEvent ev) {
            if (updatingFromController || ev.getStateChange() != ItemEvent.SELECTED) {
              return;
            }
            roomController.setOutlineDashStyle(
                (Polyline.DashStyle)outlineDashStyleComboBox.getSelectedItem());
            applyRoomChanges();
          }
        });

      this.outlineColorLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, PolylinePanel.class, "colorLabel.text"));
      this.outlineColorButton = new ColorButton(this.preferences);
      this.outlineColorButton.setNullColorAllowed(true);
      this.outlineColorButton.setColorDialogTitle(this.preferences.getLocalizedString(
          SelectionInspectorPane.class, "outlineColorDialog.title"));
      this.outlineColorButton.addPropertyChangeListener(ColorButton.COLOR_PROPERTY,
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              if (updatingFromController) {
                return;
              }
              roomController.setOutlineColor(outlineColorButton.getColor());
              applyRoomChanges();
            }
          });
    }

    private void layoutFields() {
      int labelAlignment = OperatingSystem.isMacOSX()
          ? GridBagConstraints.LINE_END
          : GridBagConstraints.LINE_START;
      int standardGap = Math.round(5 * SwingTools.getResolutionScale());
      Insets fieldInsets = new Insets(0, 0, standardGap, 0);

      JPanel fieldsPanel = new JPanel(new GridBagLayout());
      int row = 0;

      JPanel namePanel = SwingTools.createTitledPanel(this.preferences.getLocalizedString(
          RoomPanel.class, "nameAndAreaPanel.title"));
      JLabel nameLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, RoomPanel.class, "nameLabel.text"));
      if (!OperatingSystem.isMacOSX()) {
        nameLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(
            this.preferences.getLocalizedString(RoomPanel.class, "nameLabel.mnemonic")).getKeyCode());
      }
      nameLabel.setLabelFor(this.nameTextField);
      namePanel.add(nameLabel, new GridBagConstraints(
          0, 0, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      namePanel.add(this.nameTextField, new GridBagConstraints(
          1, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
      this.levelFieldLabel = new JLabel(this.preferences.getLocalizedString(
          SelectionInspectorPane.class, "areaLevelLabel.text"));
      this.levelValueLabel = new JLabel();
      namePanel.add(this.levelFieldLabel, new GridBagConstraints(
          0, 1, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      namePanel.add(this.levelValueLabel, new GridBagConstraints(
          1, 1, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
      namePanel.add(this.nameVisibleCheckBox, new GridBagConstraints(
          0, 2, 2, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 10), 0, 0));
      namePanel.add(this.areaVisibleCheckBox, new GridBagConstraints(
          0, 3, 2, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 10), 0, 0));

      fieldsPanel.add(namePanel, new GridBagConstraints(
          0, row++, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, fieldInsets, 0, 0));

      JPanel floorPanel = SwingTools.createTitledPanel(this.preferences.getLocalizedString(
          RoomPanel.class, "floorPanel.title"));
      int floorRow = 0;
      if (this.floorPaintControlsAvailable) {
        floorPanel.add(this.floorColorRadioButton, new GridBagConstraints(
            0, floorRow, 1, 1, 0, 0, labelAlignment,
            GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
        floorPanel.add(this.floorColorButton, new GridBagConstraints(
            1, floorRow++, 1, 1, 1, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
        floorPanel.add(this.floorTextureRadioButton, new GridBagConstraints(
            0, floorRow++, 2, 1, 1, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, new Insets(0, 8, standardGap, 10), 0, 0));
        floorPanel.add(this.floorTextureLibraryLabel, new GridBagConstraints(
            0, floorRow, 1, 1, 0, 0, labelAlignment,
            GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
        floorPanel.add(this.floorTextureLibraryComboBox, new GridBagConstraints(
            1, floorRow++, 1, 1, 1, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
        floorPanel.add(this.floorTextureCategoryLabel, new GridBagConstraints(
            0, floorRow, 1, 1, 0, 0, labelAlignment,
            GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
        floorPanel.add(this.floorTextureCategoryComboBox, new GridBagConstraints(
            1, floorRow++, 1, 1, 1, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
        floorPanel.add(this.floorTextureLabel, new GridBagConstraints(
            0, floorRow, 1, 1, 0, 0, labelAlignment,
            GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
        floorPanel.add(this.floorTextureComponent, new GridBagConstraints(
            1, floorRow++, 1, 1, 1, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
      } else {
        JLabel floorColorLabel = new JLabel(SwingTools.getLocalizedLabelText(
            this.preferences, RoomPanel.class, "floorColorRadioButton.text"));
        configureInspectorFieldLabel(this.preferences, RoomPanel.class,
            "floorColorRadioButton.mnemonic", floorColorLabel, this.floorColorButton);
        floorPanel.add(floorColorLabel, new GridBagConstraints(
            0, floorRow, 1, 1, 0, 0, labelAlignment,
            GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
        floorPanel.add(this.floorColorButton, new GridBagConstraints(
            1, floorRow++, 1, 1, 1, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
      }
      configureInspectorFieldLabel(this.preferences, RoomPanel.class,
          "floorOpacityLabel.mnemonic", this.floorOpacityLabel, this.floorOpacitySpinner);
      floorPanel.add(this.floorOpacityLabel, new GridBagConstraints(
          0, floorRow, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      floorPanel.add(this.floorOpacitySpinner, new GridBagConstraints(
          1, floorRow++, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));
      if (this.smoothedCheckBox != null) {
        floorPanel.add(this.smoothedCheckBox, new GridBagConstraints(
            0, floorRow, 2, 1, 1, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 10), 0, 0));
      }

      fieldsPanel.add(floorPanel, new GridBagConstraints(
          0, row++, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, fieldInsets, 0, 0));

      JPanel outlinePanel = SwingTools.createTitledPanel(this.preferences.getLocalizedString(
          SelectionInspectorPane.class, "outlinePanel.title"));
      configureInspectorFieldLabel(this.preferences, PolylinePanel.class,
          "thicknessLabel.mnemonic", this.outlineThicknessLabel, this.outlineThicknessSpinner);
      configureInspectorFieldLabel(this.preferences, PolylinePanel.class,
          "dashStyleLabel.mnemonic", this.outlineDashStyleLabel, this.outlineDashStyleComboBox);
      configureInspectorFieldLabel(this.preferences, PolylinePanel.class,
          "colorLabel.mnemonic", this.outlineColorLabel, this.outlineColorButton);
      outlinePanel.add(this.outlineThicknessLabel, new GridBagConstraints(
          0, 0, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      outlinePanel.add(this.outlineThicknessSpinner, new GridBagConstraints(
          1, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
      outlinePanel.add(this.outlineDashStyleLabel, new GridBagConstraints(
          0, 1, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      outlinePanel.add(this.outlineDashStyleComboBox, new GridBagConstraints(
          1, 1, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
      outlinePanel.add(this.outlineColorLabel, new GridBagConstraints(
          0, 2, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      outlinePanel.add(this.outlineColorButton, new GridBagConstraints(
          1, 2, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));

      fieldsPanel.add(outlinePanel, new GridBagConstraints(
          0, row, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

      fieldsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

      JPanel headerPanel = new JPanel(new GridBagLayout());
      headerPanel.setBorder(AlpInspectorStyles.sectionGapBorder());
      headerPanel.add(this.summaryLabel, new GridBagConstraints(
          0, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, AlpInspectorStyles.scale(6), 0), 0, 0));
      headerPanel.add(this.openFullEditorButton, new GridBagConstraints(
          0, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

      JPanel contentPanel = new JPanel(new BorderLayout());
      contentPanel.setBorder(BorderFactory.createEmptyBorder(
          AlpInspectorStyles.panelInsets().top,
          AlpInspectorStyles.panelInsets().left,
          AlpInspectorStyles.panelInsets().bottom,
          AlpInspectorStyles.panelInsets().right));
      contentPanel.add(headerPanel, BorderLayout.NORTH);
      contentPanel.add(fieldsPanel, BorderLayout.CENTER);
      add(contentPanel, BorderLayout.NORTH);
    }

    private void updateSelectionSummary() {
      List<Room> rooms = Home.getRoomsSubList(this.home.getSelectedItems());
      String title;
      if (rooms.size() == 1) {
        Room room = rooms.get(0);
        String name = room.getName();
        if (name != null && name.trim().length() > 0) {
          title = MessageFormat.format(this.preferences.getLocalizedString(
              SelectionInspectorPane.class, "summarySingleAreaNamed.title"), name.trim());
        } else {
          title = this.preferences.getLocalizedString(
              SelectionInspectorPane.class, "summarySingleArea.title");
        }
      } else {
        title = MessageFormat.format(this.preferences.getLocalizedString(
            SelectionInspectorPane.class, "summaryMultipleAreas.title"), rooms.size());
      }

      String levelName = getCommonLevelName(rooms);
      String subtitle = levelName != null
          ? MessageFormat.format(this.preferences.getLocalizedString(
              SelectionInspectorPane.class, "summaryLayer.text"), levelName)
          : this.preferences.getLocalizedString(
              SelectionInspectorPane.class, "summaryMixedLevels.text");
      AlpInspectorStyles.applySummaryText(this.summaryLabel, title, subtitle);
    }

    private String getCommonLevelName(List<Room> rooms) {
      if (rooms.isEmpty()) {
        return null;
      }
      Level level = rooms.get(0).getLevel();
      String levelName = level != null ? level.getName() : null;
      for (int i = 1; i < rooms.size(); i++) {
        Level otherLevel = rooms.get(i).getLevel();
        String otherLevelName = otherLevel != null ? otherLevel.getName() : null;
        if (levelName == null) {
          if (otherLevelName != null) {
            return null;
          }
        } else if (!levelName.equals(otherLevelName)) {
          return null;
        }
      }
      return levelName;
    }

    private void updateLevelDisplay() {
      this.levelValueLabel.setText(getLevelDisplayText(Home.getRoomsSubList(this.home.getSelectedItems())));
    }

    private String getLevelDisplayText(List<Room> rooms) {
      if (rooms.isEmpty()) {
        return "";
      }
      Level level = rooms.get(0).getLevel();
      for (int i = 1; i < rooms.size(); i++) {
        Level otherLevel = rooms.get(i).getLevel();
        if (level == null && otherLevel != null
            || level != null && !level.equals(otherLevel)) {
          return this.preferences.getLocalizedString(
              SelectionInspectorPane.class, "summaryMixedLevels.text");
        }
      }
      if (level == null) {
        return this.preferences.getLocalizedString(
            SelectionInspectorPane.class, "areaLevelNone.text");
      }
      String levelName = level.getName();
      if (levelName == null || levelName.trim().length() == 0) {
        return this.preferences.getLocalizedString(
            SelectionInspectorPane.class, "areaLevelNone.text");
      }
      return levelName.trim();
    }

    void refresh() {
      updateSelectionSummary();
      updateLevelDisplay();
      this.updatingFromController = true;
      try {
        this.roomController.refreshProperties();

        this.nameVisibleCheckBox.setNullable(this.roomController.getNameVisible() == null);
        this.nameVisibleCheckBox.setValue(this.roomController.getNameVisible());

        this.areaVisibleCheckBox.setNullable(this.roomController.getAreaVisible() == null);
        this.areaVisibleCheckBox.setValue(this.roomController.getAreaVisible());

        this.floorColorButton.setColor(this.roomController.getFloorColor());
        updateFloorPaintRadioButtons();
        updateFloorPaintControlsVisibility();
        updateFloorTextureFilterControls(true);

        Float floorOpacity = this.roomController.getFloorOpacity();
        this.floorOpacitySpinnerModel.setNullable(floorOpacity == null);
        this.floorOpacitySpinnerModel.setValue(floorOpacity != null
            ? floorOpacity * 100
            : null);
        updateFloorOpacityEnabled();

        if (this.smoothedCheckBox != null) {
          this.smoothedCheckBox.setNullable(this.roomController.getSmoothed() == null);
          this.smoothedCheckBox.setValue(this.roomController.getSmoothed());
        }

        Float outlineThickness = this.roomController.getOutlineThickness();
        this.outlineThicknessSpinnerModel.setNullable(outlineThickness == null);
        this.outlineThicknessSpinnerModel.setLength(outlineThickness);

        this.outlineDashStyleComboBox.setSelectedItem(this.roomController.getOutlineDashStyle());

        this.outlineColorButton.setColor(this.roomController.getOutlineColor());
      } finally {
        this.updatingFromController = false;
      }
      syncNameFieldFromModel();
    }

    void syncNameFieldFromModel() {
      if (this.updatingFromController || this.nameFieldUserEdited) {
        return;
      }
      String displayName = getSelectedRoomsCommonName();
      if (displayName == null) {
        displayName = "";
      }
      this.updatingFromController = true;
      try {
        this.nameTextField.setText(displayName);
        this.nameFieldSyncedValue = displayName;
      } finally {
        this.updatingFromController = false;
      }
    }

    private void updateControllerNameFromField(PropertyChangeListener nameChangeListener) {
      if (this.updatingFromController) {
        return;
      }
      this.nameFieldUserEdited = true;
      this.roomController.removePropertyChangeListener(
          RoomController.Property.NAME, nameChangeListener);
      String name = this.nameTextField.getText();
      if (name == null || name.trim().length() == 0) {
        this.roomController.setName("");
      } else {
        this.roomController.setName(name);
      }
      this.roomController.addPropertyChangeListener(
          RoomController.Property.NAME, nameChangeListener);
    }

    void clearPendingFloorTextureMode() {
      this.pendingFloorTextureMode = false;
    }

    private void updateFloorPaintRadioButtons() {
      if (!this.floorPaintControlsAvailable) {
        return;
      }
      RoomController.RoomPaint floorPaint = this.roomController.getFloorPaint();
      if (this.pendingFloorTextureMode) {
        this.floorTextureRadioButton.setSelected(true);
      } else if (floorPaint == RoomController.RoomPaint.COLORED) {
        this.floorColorRadioButton.setSelected(true);
      } else if (floorPaint == RoomController.RoomPaint.TEXTURED) {
        this.floorTextureRadioButton.setSelected(true);
      } else {
        SwingTools.deselectAllRadioButtons(this.floorColorRadioButton, this.floorTextureRadioButton);
      }
    }

    private void updateFloorPaintControlsVisibility() {
      Boolean floorVisible = this.roomController.getFloorVisible();
      boolean enabled = floorVisible == null || floorVisible;
      if (this.floorPaintControlsAvailable) {
        RoomController.RoomPaint floorPaint = this.roomController.getFloorPaint();
        boolean colored = !this.pendingFloorTextureMode && floorPaint == RoomController.RoomPaint.COLORED;
        boolean textured = this.pendingFloorTextureMode || floorPaint == RoomController.RoomPaint.TEXTURED;
        boolean mixed = !this.pendingFloorTextureMode && floorPaint == null;
        this.floorColorRadioButton.setEnabled(enabled);
        this.floorTextureRadioButton.setEnabled(enabled);
        this.floorColorButton.setVisible(colored || mixed);
        this.floorColorButton.setEnabled(enabled && (colored || mixed));
        boolean showTextureControls = textured || mixed;
        this.floorTextureLibraryLabel.setVisible(showTextureControls);
        this.floorTextureLibraryComboBox.setVisible(showTextureControls);
        this.floorTextureCategoryLabel.setVisible(showTextureControls);
        this.floorTextureCategoryComboBox.setVisible(showTextureControls);
        this.floorTextureLabel.setVisible(showTextureControls);
        this.floorTextureComponent.setVisible(showTextureControls);
        this.floorTextureLibraryComboBox.setEnabled(enabled && showTextureControls);
        this.floorTextureCategoryComboBox.setEnabled(enabled && showTextureControls);
        this.floorTextureComponent.setEnabled(enabled && showTextureControls);
      } else {
        this.floorColorButton.setEnabled(enabled);
      }
      updateFloorOpacityEnabled();
    }

    private void updateFloorOpacityEnabled() {
      Boolean floorVisible = this.roomController.getFloorVisible();
      boolean enabled = floorVisible == null || floorVisible;
      boolean transparentFill = isTransparentFloorFill();
      boolean opacityEnabled = enabled && !transparentFill;
      this.floorOpacitySpinner.setEnabled(opacityEnabled);
      this.floorOpacityLabel.setEnabled(opacityEnabled);
    }

    private boolean isTransparentFloorFill() {
      if (this.pendingFloorTextureMode
          || this.roomController.getFloorPaint() == RoomController.RoomPaint.TEXTURED) {
        return false;
      }
      if (AlpColorSupport.isTransparentColor(this.roomController.getFloorColor())) {
        return true;
      }
      return AlpColorSupport.isTransparentColor(this.floorColorButton.getColor());
    }

    private LevelCategory getSelectedRoomsLevelCategory() {
      List<Room> rooms = Home.getRoomsSubList(this.home.getSelectedItems());
      if (rooms.isEmpty()) {
        return LevelCategory.GENERAL;
      }
      Level level = rooms.get(0).getLevel();
      LevelCategory category = level != null ? level.getCategory() : LevelCategory.GENERAL;
      for (int i = 1; i < rooms.size(); i++) {
        Level otherLevel = rooms.get(i).getLevel();
        LevelCategory otherCategory = otherLevel != null
            ? otherLevel.getCategory()
            : LevelCategory.GENERAL;
        if (category != otherCategory) {
          return LevelCategory.GENERAL;
        }
      }
      return category != null ? category : LevelCategory.GENERAL;
    }

    private void updateFloorTextureFilterControls(boolean resetDefaults) {
      if (!this.floorPaintControlsAvailable) {
        return;
      }
      this.updatingFloorTextureFilters = true;
      try {
        List<Library> libraries = AlpTextureCatalogSupport.getTextureLibraryChoices(this.preferences);
        Library selectedLibrary = null;
        String selectedCategoryName = null;
        if (resetDefaults) {
          HomeTexture floorTexture = this.roomController.getFloorTextureController().getTexture();
          LevelCategory levelCategory = getSelectedRoomsLevelCategory();
          selectedLibrary = AlpTextureCatalogSupport.resolveDefaultLibrary(
              this.preferences, floorTexture, levelCategory);
          selectedCategoryName = AlpTextureCatalogSupport.resolveDefaultCategoryName(
              this.preferences, floorTexture, levelCategory,
              selectedLibrary != null ? selectedLibrary.getId() : null);
        } else {
          Object currentLibrary = this.floorTextureLibraryComboBox.getSelectedItem();
          if (currentLibrary instanceof Library) {
            selectedLibrary = (Library)currentLibrary;
          }
          selectedCategoryName = AlpTextureCatalogSupport.getCategoryNameForSelection(
              this.preferences, this.floorTextureCategoryComboBox.getSelectedItem());
        }

        this.floorTextureLibraryComboBox.setModel(new DefaultComboBoxModel(
            libraries.toArray(new Library [libraries.size()])));
        this.floorTextureLibraryComboBox.setEnabled(libraries.size() > 1);
        // Match on id because the imported textures entry is rebuilt at each call
        Library libraryItem = selectedLibrary != null
            ? AlpTextureCatalogSupport.findLibraryById(libraries, selectedLibrary.getId())
            : null;
        if (libraryItem != null) {
          this.floorTextureLibraryComboBox.setSelectedItem(libraryItem);
        } else {
          this.floorTextureLibraryComboBox.setSelectedIndex(0);
        }

        updateFloorTextureCategoryComboBox(true);
        if (selectedCategoryName != null) {
          this.floorTextureCategoryComboBox.setSelectedItem(selectedCategoryName);
        } else {
          this.floorTextureCategoryComboBox.setSelectedIndex(0);
        }
        applyFloorTextureFiltersFromControls();
      } finally {
        this.updatingFloorTextureFilters = false;
      }
    }

    private void updateFloorTextureCategoryComboBox(boolean preserveSelection) {
      Library library = (Library)this.floorTextureLibraryComboBox.getSelectedItem();
      String previousCategoryName = preserveSelection
          ? AlpTextureCatalogSupport.getCategoryNameForSelection(
              this.preferences, this.floorTextureCategoryComboBox.getSelectedItem())
          : null;
      List<String> categoryNames = library != null
          ? AlpTextureCatalogSupport.getCategoryNamesForLibrary(this.preferences, library.getId())
          : new ArrayList<String>();
      String allCategoriesLabel = AlpTextureCatalogSupport.getAllCategoriesLabel(this.preferences);
      List<String> categoryItems = new ArrayList<String>();
      categoryItems.add(allCategoriesLabel);
      categoryItems.addAll(categoryNames);
      this.floorTextureCategoryComboBox.setModel(new DefaultComboBoxModel(
          categoryItems.toArray(new String [categoryItems.size()])));
      if (previousCategoryName != null && categoryNames.contains(previousCategoryName)) {
        this.floorTextureCategoryComboBox.setSelectedItem(previousCategoryName);
      } else {
        this.floorTextureCategoryComboBox.setSelectedIndex(0);
      }
    }

    private void applyFloorTextureFiltersFromControls() {
      Library library = (Library)this.floorTextureLibraryComboBox.getSelectedItem();
      String libraryId = library != null ? library.getId() : null;
      String categoryName = AlpTextureCatalogSupport.getCategoryNameForSelection(
          this.preferences, this.floorTextureCategoryComboBox.getSelectedItem());
      AlpTextureCatalogSupport.applyTextureFilters(
          this.roomController.getFloorTextureController(), libraryId, categoryName);
      if (!this.updatingFloorTextureFilters) {
        // Only a deliberate choice should override the resolved defaults later on
        AlpTextureCatalogSupport.rememberSessionSelection(libraryId, categoryName);
      }
    }

    void commitPendingEdits() {
      commitPendingEditsForRooms(Home.getRoomsSubList(this.home.getSelectedItems()));
    }

    void commitPendingEditsForRooms(List<Room> targetRooms) {
      if (!this.nameFieldUserEdited || !isNameCommitNeededForRooms(targetRooms)) {
        markNameFieldSynced();
        return;
      }
      if (targetRooms.isEmpty()) {
        return;
      }
      List<Selectable> savedSelection = new ArrayList<Selectable>(this.home.getSelectedItems());
      SelectionInspectorPane.this.suppressSelectionListener = true;
      try {
        this.home.setSelectedItems(new ArrayList<Selectable>(targetRooms));
        applyRoomChanges();
        this.home.setSelectedItems(savedSelection);
      } finally {
        SelectionInspectorPane.this.suppressSelectionListener = false;
      }
    }

    private boolean isNameCommitNeeded() {
      return isNameCommitNeededForRooms(Home.getRoomsSubList(this.home.getSelectedItems()));
    }

    private boolean isNameCommitNeededForRooms(List<Room> rooms) {
      String textName = this.nameTextField.getText();
      if (textName == null) {
        textName = "";
      }
      String modelName = getCommonName(rooms);
      if (modelName == null) {
        modelName = "";
      }
      return !textName.equals(modelName);
    }

    private void syncControllerNameForModify() {
      if (this.nameFieldUserEdited && isNameCommitNeeded()) {
        String textName = this.nameTextField.getText();
        if (textName == null || textName.trim().length() == 0) {
          this.roomController.setName("");
        } else {
          this.roomController.setName(textName);
        }
      } else {
        // Leave name unchanged when editing color, opacity, etc.
        this.roomController.setName(null);
      }
    }

    private String getSelectedRoomsCommonName() {
      return getCommonName(Home.getRoomsSubList(this.home.getSelectedItems()));
    }

    private String getCommonName(List<Room> rooms) {
      if (rooms.isEmpty()) {
        return null;
      }
      String name = rooms.get(0).getName();
      for (int i = 1; i < rooms.size(); i++) {
        if (name == null) {
          if (rooms.get(i).getName() != null) {
            return null;
          }
        } else if (!name.equals(rooms.get(i).getName())) {
          return null;
        }
      }
      return name;
    }

    private void applyRoomChanges() {
      if (this.updatingFromController) {
        return;
      }
      if (Home.getRoomsSubList(this.home.getSelectedItems()).isEmpty()) {
        return;
      }
      syncControllerNameForModify();
      this.roomController.modifyRooms();
      markNameFieldSynced();
    }

    private void markNameFieldSynced() {
      String textName = this.nameTextField.getText();
      this.nameFieldSyncedValue = textName != null ? textName : "";
      this.nameFieldUserEdited = false;
    }
  }

  /**
   * Polyline inspector with live edit fields (SPIKE-21 P2).
   */
  private class PolylineInspectorPanel extends JPanel {
    private final Home                home;
    private final HomeController      homeController;
    private final PolylineController  polylineController;
    private final UserPreferences     preferences;
    private JLabel                    summaryLabel;
    private JButton                   openFullEditorButton;
    private JLabel                    thicknessLabel;
    private NullableSpinner           thicknessSpinner;
    private NullableSpinner.NullableSpinnerLengthModel thicknessSpinnerModel;
    private JLabel                    dashStyleLabel;
    private JComboBox                 dashStyleComboBox;
    private JLabel                    colorLabel;
    private ColorButton               colorButton;
    private NullableCheckBox          closedPathCheckBox;
    private boolean                   updatingFromController;

    PolylineInspectorPanel(Home home,
                           UserPreferences preferences,
                           HomeController controller) {
      super(new BorderLayout());
      this.home = home;
      this.homeController = controller;
      this.preferences = preferences;
      this.polylineController = controller.createPolylineController();
      createHeader();
      createFields();
      layoutFields();
    }

    private void createHeader() {
      this.summaryLabel = new JLabel();
      this.openFullEditorButton = new JButton(this.preferences.getLocalizedString(
          SelectionInspectorPane.class, "openFullEditorButton.text"));
      if (!OperatingSystem.isMacOSX()) {
        this.openFullEditorButton.setMnemonic(KeyStroke.getKeyStroke(
            this.preferences.getLocalizedString(
                SelectionInspectorPane.class, "openFullEditorButton.mnemonic")).getKeyCode());
      }
      this.openFullEditorButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            homeController.getPlanController().modifySelectedPolylines();
          }
        });
    }

    private void createFields() {
      this.thicknessLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, PolylinePanel.class, "thicknessLabel.text",
          this.preferences.getLengthUnit().getName()));
      this.thicknessSpinnerModel = new NullableSpinner.NullableSpinnerLengthModel(
          this.preferences, this.preferences.getLengthUnit().getMinimumLength(), 50f);
      this.thicknessSpinner = new NullableSpinner(this.thicknessSpinnerModel);
      this.thicknessSpinnerModel.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            if (updatingFromController) {
              return;
            }
            polylineController.setThickness(thicknessSpinnerModel.getLength());
            applyPolylineChanges();
          }
        });

      this.dashStyleLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, PolylinePanel.class, "dashStyleLabel.text"));
      List<Polyline.DashStyle> dashStyles = AlpDashStyleSupport.getDashStyleChoicesForCombo(
          this.polylineController.getDashStyle(), true);
      this.dashStyleComboBox = new JComboBox(
          new DefaultComboBoxModel(dashStyles.toArray(new Polyline.DashStyle [dashStyles.size()])));
      this.dashStyleComboBox.setRenderer(new AlpDashStyleListCellRenderer(this.preferences));
      this.dashStyleComboBox.addItemListener(new ItemListener() {
          public void itemStateChanged(ItemEvent ev) {
            if (updatingFromController || ev.getStateChange() != ItemEvent.SELECTED) {
              return;
            }
            polylineController.setDashStyle(
                (Polyline.DashStyle)dashStyleComboBox.getSelectedItem());
            applyPolylineChanges();
          }
        });

      this.colorLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, PolylinePanel.class, "colorLabel.text"));
      this.colorButton = new ColorButton(this.preferences);
      this.colorButton.setColorDialogTitle(this.preferences.getLocalizedString(
          PolylinePanel.class, "colorDialog.title"));
      this.colorButton.addPropertyChangeListener(ColorButton.COLOR_PROPERTY,
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              if (updatingFromController) {
                return;
              }
              polylineController.setColor(colorButton.getColor());
              applyPolylineChanges();
            }
          });

      this.closedPathCheckBox = new NullableCheckBox(this.preferences.getLocalizedString(
          SelectionInspectorPane.class, "closedPathCheckBox.text"));
      if (!OperatingSystem.isMacOSX()) {
        this.closedPathCheckBox.setMnemonic(KeyStroke.getKeyStroke(
            this.preferences.getLocalizedString(
                SelectionInspectorPane.class, "closedPathCheckBox.mnemonic")).getKeyCode());
      }
      this.closedPathCheckBox.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            if (updatingFromController) {
              return;
            }
            polylineController.setClosedPath(closedPathCheckBox.getValue());
            applyPolylineChanges();
          }
        });
    }

    private void layoutFields() {
      int labelAlignment = OperatingSystem.isMacOSX()
          ? GridBagConstraints.LINE_END
          : GridBagConstraints.LINE_START;
      int standardGap = Math.round(5 * SwingTools.getResolutionScale());
      Insets fieldInsets = new Insets(0, 0, standardGap, 0);

      JPanel fieldsPanel = new JPanel(new GridBagLayout());
      JPanel strokePanel = SwingTools.createTitledPanel(this.preferences.getLocalizedString(
          SelectionInspectorPane.class, "polylineStrokePanel.title"));
      configureInspectorFieldLabel(this.preferences, PolylinePanel.class,
          "thicknessLabel.mnemonic", this.thicknessLabel, this.thicknessSpinner);
      configureInspectorFieldLabel(this.preferences, PolylinePanel.class,
          "dashStyleLabel.mnemonic", this.dashStyleLabel, this.dashStyleComboBox);
      configureInspectorFieldLabel(this.preferences, PolylinePanel.class,
          "colorLabel.mnemonic", this.colorLabel, this.colorButton);
      strokePanel.add(this.thicknessLabel, new GridBagConstraints(
          0, 0, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      strokePanel.add(this.thicknessSpinner, new GridBagConstraints(
          1, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
      strokePanel.add(this.dashStyleLabel, new GridBagConstraints(
          0, 1, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      strokePanel.add(this.dashStyleComboBox, new GridBagConstraints(
          1, 1, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
      strokePanel.add(this.colorLabel, new GridBagConstraints(
          0, 2, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      strokePanel.add(this.colorButton, new GridBagConstraints(
          1, 2, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
      strokePanel.add(this.closedPathCheckBox, new GridBagConstraints(
          0, 3, 2, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 10), 0, 0));

      fieldsPanel.add(strokePanel, new GridBagConstraints(
          0, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, fieldInsets, 0, 0));

      JPanel headerPanel = new JPanel(new GridBagLayout());
      headerPanel.setBorder(AlpInspectorStyles.sectionGapBorder());
      headerPanel.add(this.summaryLabel, new GridBagConstraints(
          0, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, AlpInspectorStyles.scale(6), 0), 0, 0));
      headerPanel.add(this.openFullEditorButton, new GridBagConstraints(
          0, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

      JPanel contentPanel = new JPanel(new BorderLayout());
      contentPanel.setBorder(BorderFactory.createEmptyBorder(
          AlpInspectorStyles.panelInsets().top,
          AlpInspectorStyles.panelInsets().left,
          AlpInspectorStyles.panelInsets().bottom,
          AlpInspectorStyles.panelInsets().right));
      contentPanel.add(headerPanel, BorderLayout.NORTH);
      contentPanel.add(fieldsPanel, BorderLayout.CENTER);
      add(contentPanel, BorderLayout.NORTH);
    }

    void refresh() {
      updateSelectionSummary();
      this.updatingFromController = true;
      try {
        this.polylineController.refreshProperties();

        Float thickness = this.polylineController.getThickness();
        this.thicknessSpinnerModel.setNullable(thickness == null);
        this.thicknessSpinnerModel.setLength(thickness);

        this.dashStyleComboBox.setSelectedItem(this.polylineController.getDashStyle());

        this.colorButton.setColor(this.polylineController.getColor());

        Boolean closedPath = this.polylineController.getClosedPath();
        this.closedPathCheckBox.setNullable(closedPath == null);
        this.closedPathCheckBox.setValue(closedPath);
        updateClosedPathEnabled();
      } finally {
        this.updatingFromController = false;
      }
    }

    private void updateClosedPathEnabled() {
      List<Polyline> polylines = Home.getPolylinesSubList(this.home.getSelectedItems());
      boolean enabled = true;
      for (Polyline polyline : polylines) {
        if (polyline.getPointCount() <= 2) {
          enabled = false;
          break;
        }
      }
      this.closedPathCheckBox.setEnabled(enabled);
    }

    private void updateSelectionSummary() {
      List<Polyline> polylines = Home.getPolylinesSubList(this.home.getSelectedItems());
      String title;
      if (polylines.size() == 1) {
        title = this.preferences.getLocalizedString(
            SelectionInspectorPane.class, "summarySinglePolyline.title");
      } else {
        title = MessageFormat.format(this.preferences.getLocalizedString(
            SelectionInspectorPane.class, "summaryMultiplePolylines.title"), polylines.size());
      }

      String levelName = getCommonLevelName(polylines);
      String subtitle = levelName != null
          ? MessageFormat.format(this.preferences.getLocalizedString(
              SelectionInspectorPane.class, "summaryLayer.text"), levelName)
          : this.preferences.getLocalizedString(
              SelectionInspectorPane.class, "summaryMixedLevels.text");
      AlpInspectorStyles.applySummaryText(this.summaryLabel, title, subtitle);
    }

    private String getCommonLevelName(List<Polyline> polylines) {
      if (polylines.isEmpty()) {
        return null;
      }
      Level level = polylines.get(0).getLevel();
      String levelName = level != null ? level.getName() : null;
      for (int i = 1; i < polylines.size(); i++) {
        Level otherLevel = polylines.get(i).getLevel();
        String otherLevelName = otherLevel != null ? otherLevel.getName() : null;
        if (levelName == null) {
          if (otherLevelName != null) {
            return null;
          }
        } else if (!levelName.equals(otherLevelName)) {
          return null;
        }
      }
      return levelName;
    }

    private void applyPolylineChanges() {
      if (this.updatingFromController) {
        return;
      }
      if (Home.getPolylinesSubList(this.home.getSelectedItems()).isEmpty()) {
        return;
      }
      this.polylineController.modifyPolylines();
    }
  }

  /**
   * Label/Text inspector with live edit fields (SPIKE-21 P2).
   */
  private class LabelInspectorPanel extends JPanel {
    private final Home               home;
    private final HomeController     homeController;
    private final LabelController    labelController;
    private final UserPreferences    preferences;
    private JLabel                   summaryLabel;
    private JButton                  openFullEditorButton;
    private JTextArea                textTextArea;
    private JLabel                   fontSizeLabel;
    private NullableSpinner          fontSizeSpinner;
    private NullableSpinner.NullableSpinnerLengthModel fontSizeSpinnerModel;
    private NullableCheckBox         boldCheckBox;
    private NullableCheckBox         italicCheckBox;
    private JLabel                   colorLabel;
    private ColorButton              colorButton;
    private boolean                  textFieldUserEdited;
    private boolean                  updatingFromController;

    LabelInspectorPanel(Home home,
                        UserPreferences preferences,
                        HomeController controller) {
      super(new BorderLayout());
      this.home = home;
      this.homeController = controller;
      this.preferences = preferences;
      this.labelController = controller.createLabelController();
      createHeader();
      createFields();
      layoutFields();
    }

    private void createHeader() {
      this.summaryLabel = new JLabel();
      this.openFullEditorButton = new JButton(this.preferences.getLocalizedString(
          SelectionInspectorPane.class, "openFullEditorButton.text"));
      if (!OperatingSystem.isMacOSX()) {
        this.openFullEditorButton.setMnemonic(KeyStroke.getKeyStroke(
            this.preferences.getLocalizedString(
                SelectionInspectorPane.class, "openFullEditorButton.mnemonic")).getKeyCode());
      }
      this.openFullEditorButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            commitPendingEdits();
            homeController.getPlanController().modifySelectedLabels();
          }
        });
    }

    private void createFields() {
      this.textTextArea = new JTextArea("", 3, 20);
      this.textTextArea.setLineWrap(true);
      this.textTextArea.setWrapStyleWord(true);
      this.textTextArea.setDocument(new AutoCompleteDocument(this.textTextArea,
          this.preferences.getAutoCompletionStrings("LabelText")));
      final PropertyChangeListener textChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            syncTextFieldFromModel();
          }
        };
      this.labelController.addPropertyChangeListener(
          LabelController.Property.TEXT, textChangeListener);
      this.textTextArea.getDocument().addDocumentListener(new DocumentListener() {
          public void changedUpdate(DocumentEvent ev) {
            updateControllerTextFromField(textChangeListener);
          }

          public void insertUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }

          public void removeUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
        });
      this.textTextArea.addFocusListener(new FocusAdapter() {
          @Override
          public void focusLost(FocusEvent ev) {
            commitPendingEdits();
          }
        });

      this.fontSizeLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, LabelPanel.class, "fontSizeLabel.text",
          this.preferences.getLengthUnit().getName()));
      this.fontSizeSpinnerModel = new NullableSpinner.NullableSpinnerLengthModel(
          this.preferences, 5, 999);
      this.fontSizeSpinner = new NullableSpinner(this.fontSizeSpinnerModel);
      this.fontSizeSpinnerModel.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            if (updatingFromController) {
              return;
            }
            labelController.setFontSize(fontSizeSpinnerModel.getLength());
            applyLabelChanges();
          }
        });

      this.boldCheckBox = new NullableCheckBox(this.preferences.getLocalizedString(
          SelectionInspectorPane.class, "boldCheckBox.text"));
      if (!OperatingSystem.isMacOSX()) {
        this.boldCheckBox.setMnemonic(KeyStroke.getKeyStroke(
            this.preferences.getLocalizedString(
                SelectionInspectorPane.class, "boldCheckBox.mnemonic")).getKeyCode());
      }
      this.boldCheckBox.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            if (updatingFromController) {
              return;
            }
            labelController.setBold(boldCheckBox.getValue());
            applyLabelChanges();
          }
        });

      this.italicCheckBox = new NullableCheckBox(this.preferences.getLocalizedString(
          SelectionInspectorPane.class, "italicCheckBox.text"));
      if (!OperatingSystem.isMacOSX()) {
        this.italicCheckBox.setMnemonic(KeyStroke.getKeyStroke(
            this.preferences.getLocalizedString(
                SelectionInspectorPane.class, "italicCheckBox.mnemonic")).getKeyCode());
      }
      this.italicCheckBox.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            if (updatingFromController) {
              return;
            }
            labelController.setItalic(italicCheckBox.getValue());
            applyLabelChanges();
          }
        });

      this.colorLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, LabelPanel.class, "colorLabel.text"));
      this.colorButton = new ColorButton(this.preferences);
      this.colorButton.setColorDialogTitle(this.preferences.getLocalizedString(
          LabelPanel.class, "colorDialog.title"));
      this.colorButton.addPropertyChangeListener(ColorButton.COLOR_PROPERTY,
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              if (updatingFromController) {
                return;
              }
              labelController.setColor(colorButton.getColor());
              applyLabelChanges();
            }
          });
    }

    private void layoutFields() {
      int labelAlignment = OperatingSystem.isMacOSX()
          ? GridBagConstraints.LINE_END
          : GridBagConstraints.LINE_START;
      int standardGap = Math.round(5 * SwingTools.getResolutionScale());
      Insets fieldInsets = new Insets(0, 0, standardGap, 0);

      JPanel fieldsPanel = new JPanel(new GridBagLayout());
      int row = 0;

      JPanel textPanel = SwingTools.createTitledPanel(this.preferences.getLocalizedString(
          SelectionInspectorPane.class, "labelTextPanel.title"));
      JLabel textLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, LabelPanel.class, "textLabel.text"));
      if (!OperatingSystem.isMacOSX()) {
        textLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(
            this.preferences.getLocalizedString(LabelPanel.class, "textLabel.mnemonic")).getKeyCode());
      }
      textLabel.setLabelFor(this.textTextArea);
      textPanel.add(textLabel, new GridBagConstraints(
          0, 0, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      textPanel.add(this.textTextArea, new GridBagConstraints(
          1, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.BOTH, new Insets(0, 0, standardGap, 10), 0, 0));

      fieldsPanel.add(textPanel, new GridBagConstraints(
          0, row++, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, fieldInsets, 0, 0));

      JPanel stylePanel = SwingTools.createTitledPanel(this.preferences.getLocalizedString(
          SelectionInspectorPane.class, "labelStylePanel.title"));
      configureInspectorFieldLabel(this.preferences, LabelPanel.class,
          "fontSizeLabel.mnemonic", this.fontSizeLabel, this.fontSizeSpinner);
      stylePanel.add(this.fontSizeLabel, new GridBagConstraints(
          0, 0, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      stylePanel.add(this.fontSizeSpinner, new GridBagConstraints(
          1, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
      stylePanel.add(this.boldCheckBox, new GridBagConstraints(
          0, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      stylePanel.add(this.italicCheckBox, new GridBagConstraints(
          1, 1, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
      stylePanel.add(this.colorLabel, new GridBagConstraints(
          0, 2, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      stylePanel.add(this.colorButton, new GridBagConstraints(
          1, 2, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));

      fieldsPanel.add(stylePanel, new GridBagConstraints(
          0, row, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

      JPanel headerPanel = new JPanel(new GridBagLayout());
      headerPanel.setBorder(AlpInspectorStyles.sectionGapBorder());
      headerPanel.add(this.summaryLabel, new GridBagConstraints(
          0, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, AlpInspectorStyles.scale(6), 0), 0, 0));
      headerPanel.add(this.openFullEditorButton, new GridBagConstraints(
          0, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

      JPanel contentPanel = new JPanel(new BorderLayout());
      contentPanel.setBorder(BorderFactory.createEmptyBorder(
          AlpInspectorStyles.panelInsets().top,
          AlpInspectorStyles.panelInsets().left,
          AlpInspectorStyles.panelInsets().bottom,
          AlpInspectorStyles.panelInsets().right));
      contentPanel.add(headerPanel, BorderLayout.NORTH);
      contentPanel.add(fieldsPanel, BorderLayout.CENTER);
      add(contentPanel, BorderLayout.NORTH);
    }

    void refresh() {
      updateSelectionSummary();
      this.updatingFromController = true;
      try {
        this.labelController.refreshProperties();

        Float fontSize = this.labelController.getFontSize();
        this.fontSizeSpinnerModel.setNullable(fontSize == null);
        this.fontSizeSpinnerModel.setLength(fontSize);

        Boolean bold = this.labelController.getBold();
        this.boldCheckBox.setNullable(bold == null);
        this.boldCheckBox.setValue(bold);

        Boolean italic = this.labelController.getItalic();
        this.italicCheckBox.setNullable(italic == null);
        this.italicCheckBox.setValue(italic);

        this.colorButton.setColor(this.labelController.getColor());
      } finally {
        this.updatingFromController = false;
      }
      syncTextFieldFromModel();
    }

    void syncTextFieldFromModel() {
      if (this.updatingFromController || this.textFieldUserEdited) {
        return;
      }
      String displayText = getSelectedLabelsCommonText();
      if (displayText == null) {
        displayText = "";
      }
      this.updatingFromController = true;
      try {
        this.textTextArea.setText(displayText);
      } finally {
        this.updatingFromController = false;
      }
    }

    private void updateControllerTextFromField(PropertyChangeListener textChangeListener) {
      if (this.updatingFromController) {
        return;
      }
      this.textFieldUserEdited = true;
      this.labelController.removePropertyChangeListener(
          LabelController.Property.TEXT, textChangeListener);
      String text = this.textTextArea.getText();
      if (text == null || text.trim().length() == 0) {
        this.labelController.setText("");
      } else {
        this.labelController.setText(text);
      }
      this.labelController.addPropertyChangeListener(
          LabelController.Property.TEXT, textChangeListener);
    }

    private void updateSelectionSummary() {
      List<Label> labels = Home.getLabelsSubList(this.home.getSelectedItems());
      String title;
      if (labels.size() == 1) {
        String text = labels.get(0).getText();
        if (text != null && text.trim().length() > 0) {
          String summaryText = text.trim().replaceAll("\\s+", " ");
          if (summaryText.length() > 40) {
            summaryText = summaryText.substring(0, 37) + "...";
          }
          title = MessageFormat.format(this.preferences.getLocalizedString(
              SelectionInspectorPane.class, "summarySingleLabelNamed.title"), summaryText);
        } else {
          title = this.preferences.getLocalizedString(
              SelectionInspectorPane.class, "summarySingleLabel.title");
        }
      } else {
        title = MessageFormat.format(this.preferences.getLocalizedString(
            SelectionInspectorPane.class, "summaryMultipleLabels.title"), labels.size());
      }

      String levelName = getCommonLevelName(labels);
      String subtitle = levelName != null
          ? MessageFormat.format(this.preferences.getLocalizedString(
              SelectionInspectorPane.class, "summaryLayer.text"), levelName)
          : this.preferences.getLocalizedString(
              SelectionInspectorPane.class, "summaryMixedLevels.text");
      AlpInspectorStyles.applySummaryText(this.summaryLabel, title, subtitle);
    }

    private String getCommonLevelName(List<Label> labels) {
      if (labels.isEmpty()) {
        return null;
      }
      Level level = labels.get(0).getLevel();
      String levelName = level != null ? level.getName() : null;
      for (int i = 1; i < labels.size(); i++) {
        Level otherLevel = labels.get(i).getLevel();
        String otherLevelName = otherLevel != null ? otherLevel.getName() : null;
        if (levelName == null) {
          if (otherLevelName != null) {
            return null;
          }
        } else if (!levelName.equals(otherLevelName)) {
          return null;
        }
      }
      return levelName;
    }

    void commitPendingEdits() {
      commitPendingEditsForLabels(Home.getLabelsSubList(this.home.getSelectedItems()));
    }

    void commitPendingEditsForLabels(List<Label> targetLabels) {
      if (!this.textFieldUserEdited || !isTextCommitNeededForLabels(targetLabels)) {
        markTextFieldSynced();
        return;
      }
      if (targetLabels.isEmpty()) {
        return;
      }
      List<Selectable> savedSelection = new ArrayList<Selectable>(this.home.getSelectedItems());
      SelectionInspectorPane.this.suppressSelectionListener = true;
      try {
        this.home.setSelectedItems(new ArrayList<Selectable>(targetLabels));
        applyLabelChanges();
        this.home.setSelectedItems(savedSelection);
      } finally {
        SelectionInspectorPane.this.suppressSelectionListener = false;
      }
    }

    private boolean isTextCommitNeededForLabels(List<Label> labels) {
      String textAreaText = this.textTextArea.getText();
      if (textAreaText == null) {
        textAreaText = "";
      }
      String modelText = getCommonText(labels);
      if (modelText == null) {
        modelText = "";
      }
      return !textAreaText.equals(modelText);
    }

    private String getSelectedLabelsCommonText() {
      return getCommonText(Home.getLabelsSubList(this.home.getSelectedItems()));
    }

    private String getCommonText(List<Label> labels) {
      if (labels.isEmpty()) {
        return null;
      }
      String text = labels.get(0).getText();
      for (int i = 1; i < labels.size(); i++) {
        if (text == null) {
          if (labels.get(i).getText() != null) {
            return null;
          }
        } else if (!text.equals(labels.get(i).getText())) {
          return null;
        }
      }
      return text;
    }

    private void syncControllerTextForModify() {
      if (this.textFieldUserEdited && isTextCommitNeededForLabels(
          Home.getLabelsSubList(this.home.getSelectedItems()))) {
        String text = this.textTextArea.getText();
        if (text == null || text.trim().length() == 0) {
          this.labelController.setText("");
        } else {
          this.labelController.setText(text);
        }
      } else {
        this.labelController.setText(null);
      }
    }

    private void applyLabelChanges() {
      if (this.updatingFromController) {
        return;
      }
      if (Home.getLabelsSubList(this.home.getSelectedItems()).isEmpty()) {
        return;
      }
      syncControllerTextForModify();
      this.labelController.modifyLabels();
      markTextFieldSynced();
    }

    private void markTextFieldSynced() {
      this.textFieldUserEdited = false;
    }
  }

  /**
   * Dimension line inspector with live edit fields (SPIKE-21 P2b).
   */
  private class DimensionLineInspectorPanel extends JPanel {
    private final Home                      home;
    private final HomeController            homeController;
    private final DimensionLineController   dimensionLineController;
    private final UserPreferences           preferences;
    private JLabel                          summaryLabel;
    private JButton                         openFullEditorButton;
    private JLabel                          offsetLabel;
    private NullableSpinner                 offsetSpinner;
    private NullableSpinner.NullableSpinnerLengthModel offsetSpinnerModel;
    private JLabel                          lengthFontSizeLabel;
    private NullableSpinner                 lengthFontSizeSpinner;
    private NullableSpinner.NullableSpinnerLengthModel lengthFontSizeSpinnerModel;
    private JLabel                          colorLabel;
    private ColorButton                     colorButton;
    private boolean                         updatingFromController;

    DimensionLineInspectorPanel(Home home,
                                UserPreferences preferences,
                                HomeController controller) {
      super(new BorderLayout());
      this.home = home;
      this.homeController = controller;
      this.preferences = preferences;
      this.dimensionLineController = controller.createDimensionLineController();
      createHeader();
      createFields();
      layoutFields();
    }

    private void createHeader() {
      this.summaryLabel = new JLabel();
      this.openFullEditorButton = new JButton(this.preferences.getLocalizedString(
          SelectionInspectorPane.class, "openFullEditorButton.text"));
      if (!OperatingSystem.isMacOSX()) {
        this.openFullEditorButton.setMnemonic(KeyStroke.getKeyStroke(
            this.preferences.getLocalizedString(
                SelectionInspectorPane.class, "openFullEditorButton.mnemonic")).getKeyCode());
      }
      this.openFullEditorButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            homeController.getPlanController().modifySelectedDimensionLines();
          }
        });
    }

    private void createFields() {
      String unitName = this.preferences.getLengthUnit().getName();
      this.offsetLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, DimensionLinePanel.class, "offsetLabel.text", unitName));
      this.offsetSpinnerModel = new NullableSpinner.NullableSpinnerLengthModel(
          this.preferences, -10000, 10000);
      this.offsetSpinner = new NullableSpinner(this.offsetSpinnerModel);
      this.offsetSpinnerModel.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            if (updatingFromController) {
              return;
            }
            dimensionLineController.setOffset(offsetSpinnerModel.getLength());
            applyDimensionLineChanges();
          }
        });

      this.lengthFontSizeLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, DimensionLinePanel.class, "lengthFontSizeLabel.text", unitName));
      this.lengthFontSizeSpinnerModel = new NullableSpinner.NullableSpinnerLengthModel(
          this.preferences, 5, 999);
      this.lengthFontSizeSpinner = new NullableSpinner(this.lengthFontSizeSpinnerModel);
      this.lengthFontSizeSpinnerModel.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            if (updatingFromController) {
              return;
            }
            dimensionLineController.setLengthFontSize(lengthFontSizeSpinnerModel.getLength());
            applyDimensionLineChanges();
          }
        });

      this.colorLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, DimensionLinePanel.class, "colorLabel.text"));
      this.colorButton = new ColorButton(this.preferences);
      this.colorButton.setColorDialogTitle(this.preferences.getLocalizedString(
          DimensionLinePanel.class, "colorDialog.title"));
      this.colorButton.addPropertyChangeListener(ColorButton.COLOR_PROPERTY,
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              if (updatingFromController) {
                return;
              }
              dimensionLineController.setColor(colorButton.getColor());
              applyDimensionLineChanges();
            }
          });
    }

    private void layoutFields() {
      int labelAlignment = OperatingSystem.isMacOSX()
          ? GridBagConstraints.LINE_END
          : GridBagConstraints.LINE_START;
      int standardGap = Math.round(5 * SwingTools.getResolutionScale());
      Insets fieldInsets = new Insets(0, 0, standardGap, 0);

      JPanel fieldsPanel = new JPanel(new GridBagLayout());
      JPanel stylePanel = SwingTools.createTitledPanel(this.preferences.getLocalizedString(
          SelectionInspectorPane.class, "dimensionStylePanel.title"));
      configureInspectorFieldLabel(this.preferences, DimensionLinePanel.class,
          "offsetLabel.mnemonic", this.offsetLabel, this.offsetSpinner);
      configureInspectorFieldLabel(this.preferences, DimensionLinePanel.class,
          "lengthFontSizeLabel.mnemonic", this.lengthFontSizeLabel, this.lengthFontSizeSpinner);
      stylePanel.add(this.offsetLabel, new GridBagConstraints(
          0, 0, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      stylePanel.add(this.offsetSpinner, new GridBagConstraints(
          1, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
      stylePanel.add(this.lengthFontSizeLabel, new GridBagConstraints(
          0, 1, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      stylePanel.add(this.lengthFontSizeSpinner, new GridBagConstraints(
          1, 1, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
      stylePanel.add(this.colorLabel, new GridBagConstraints(
          0, 2, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      stylePanel.add(this.colorButton, new GridBagConstraints(
          1, 2, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));

      fieldsPanel.add(stylePanel, new GridBagConstraints(
          0, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, fieldInsets, 0, 0));

      JPanel headerPanel = new JPanel(new GridBagLayout());
      headerPanel.setBorder(AlpInspectorStyles.sectionGapBorder());
      headerPanel.add(this.summaryLabel, new GridBagConstraints(
          0, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, AlpInspectorStyles.scale(6), 0), 0, 0));
      headerPanel.add(this.openFullEditorButton, new GridBagConstraints(
          0, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

      JPanel contentPanel = new JPanel(new BorderLayout());
      contentPanel.setBorder(BorderFactory.createEmptyBorder(
          AlpInspectorStyles.panelInsets().top,
          AlpInspectorStyles.panelInsets().left,
          AlpInspectorStyles.panelInsets().bottom,
          AlpInspectorStyles.panelInsets().right));
      contentPanel.add(headerPanel, BorderLayout.NORTH);
      contentPanel.add(fieldsPanel, BorderLayout.CENTER);
      add(contentPanel, BorderLayout.NORTH);
    }

    void refresh() {
      updateSelectionSummary();
      this.updatingFromController = true;
      try {
        this.dimensionLineController.refreshProperties();

        Float offset = this.dimensionLineController.getOffset();
        this.offsetSpinnerModel.setNullable(offset == null);
        this.offsetSpinnerModel.setLength(offset);

        Float lengthFontSize = this.dimensionLineController.getLengthFontSize();
        this.lengthFontSizeSpinnerModel.setNullable(lengthFontSize == null);
        this.lengthFontSizeSpinnerModel.setLength(lengthFontSize);

        this.colorButton.setColor(this.dimensionLineController.getColor());
      } finally {
        this.updatingFromController = false;
      }
    }

    private void updateSelectionSummary() {
      List<DimensionLine> dimensionLines = Home.getDimensionLinesSubList(this.home.getSelectedItems());
      String title;
      if (dimensionLines.size() == 1) {
        title = this.preferences.getLocalizedString(
            SelectionInspectorPane.class, "summarySingleDimension.title");
      } else {
        title = MessageFormat.format(this.preferences.getLocalizedString(
            SelectionInspectorPane.class, "summaryMultipleDimensions.title"), dimensionLines.size());
      }

      String levelName = getCommonLevelName(dimensionLines);
      String subtitle = levelName != null
          ? MessageFormat.format(this.preferences.getLocalizedString(
              SelectionInspectorPane.class, "summaryLayer.text"), levelName)
          : this.preferences.getLocalizedString(
              SelectionInspectorPane.class, "summaryMixedLevels.text");
      AlpInspectorStyles.applySummaryText(this.summaryLabel, title, subtitle);
    }

    private String getCommonLevelName(List<DimensionLine> dimensionLines) {
      if (dimensionLines.isEmpty()) {
        return null;
      }
      Level level = dimensionLines.get(0).getLevel();
      String levelName = level != null ? level.getName() : null;
      for (int i = 1; i < dimensionLines.size(); i++) {
        Level otherLevel = dimensionLines.get(i).getLevel();
        String otherLevelName = otherLevel != null ? otherLevel.getName() : null;
        if (levelName == null) {
          if (otherLevelName != null) {
            return null;
          }
        } else if (!levelName.equals(otherLevelName)) {
          return null;
        }
      }
      return levelName;
    }

    private void syncControllerGeometryForModify() {
      this.dimensionLineController.setXStart(null);
      this.dimensionLineController.setYStart(null);
      this.dimensionLineController.setElevationStart(null);
      this.dimensionLineController.setXEnd(null);
      this.dimensionLineController.setYEnd(null);
      this.dimensionLineController.setElevationEnd(null);
      this.dimensionLineController.setVisibleIn3D(null);
      this.dimensionLineController.setPitch(null);
    }

    private void applyDimensionLineChanges() {
      if (this.updatingFromController) {
        return;
      }
      if (Home.getDimensionLinesSubList(this.home.getSelectedItems()).isEmpty()) {
        return;
      }
      syncControllerGeometryForModify();
      this.dimensionLineController.modifyDimensionLines();
    }
  }

  /**
   * Wall inspector with live edit fields (SPIKE-21 P3).
   */
  private class WallInspectorPanel extends JPanel {
    private final Home              home;
    private final HomeController    homeController;
    private final WallController    wallController;
    private final UserPreferences   preferences;
    private JLabel                  summaryLabel;
    private JButton                 openFullEditorButton;
    private JLabel                  thicknessLabel;
    private NullableSpinner         thicknessSpinner;
    private NullableSpinner.NullableSpinnerLengthModel thicknessSpinnerModel;
    private JLabel                  heightLabel;
    private NullableSpinner         heightSpinner;
    private NullableSpinner.NullableSpinnerLengthModel heightSpinnerModel;
    private JLabel                  patternLabel;
    private JComboBox               patternComboBox;
    private boolean                 updatingFromController;

    WallInspectorPanel(Home home,
                       UserPreferences preferences,
                       HomeController controller) {
      super(new BorderLayout());
      this.home = home;
      this.homeController = controller;
      this.preferences = preferences;
      this.wallController = controller.createWallController();
      createHeader();
      createFields();
      layoutFields();
    }

    private void createHeader() {
      this.summaryLabel = new JLabel();
      this.openFullEditorButton = new JButton(this.preferences.getLocalizedString(
          SelectionInspectorPane.class, "openFullEditorButton.text"));
      if (!OperatingSystem.isMacOSX()) {
        this.openFullEditorButton.setMnemonic(KeyStroke.getKeyStroke(
            this.preferences.getLocalizedString(
                SelectionInspectorPane.class, "openFullEditorButton.mnemonic")).getKeyCode());
      }
      this.openFullEditorButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            homeController.getPlanController().modifySelectedWalls();
          }
        });
    }

    private void createFields() {
      float minimumLength = this.preferences.getLengthUnit().getMinimumLength();
      float maximumLength = this.preferences.getLengthUnit().getMaximumLength();
      String unitName = this.preferences.getLengthUnit().getName();

      this.thicknessLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, WallPanel.class, "thicknessLabel.text", unitName));
      this.thicknessSpinnerModel = new NullableSpinner.NullableSpinnerLengthModel(
          this.preferences, minimumLength, maximumLength / 10);
      this.thicknessSpinner = new NullableSpinner(this.thicknessSpinnerModel);
      this.thicknessSpinnerModel.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            if (updatingFromController) {
              return;
            }
            wallController.setThickness(thicknessSpinnerModel.getLength());
            applyWallChanges(false);
          }
        });

      this.heightLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, WallPanel.class, "rectangularWallHeightLabel.text", unitName));
      this.heightSpinnerModel = new NullableSpinner.NullableSpinnerLengthModel(
          this.preferences, minimumLength, maximumLength);
      this.heightSpinner = new NullableSpinner(this.heightSpinnerModel);
      this.heightSpinnerModel.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            if (updatingFromController) {
              return;
            }
            wallController.setRectangularWallHeight(heightSpinnerModel.getLength());
            applyWallChanges(false);
          }
        });

      this.patternLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, WallPanel.class, "patternLabel.text"));
      this.patternComboBox = new JComboBox();
      this.patternComboBox.setRenderer(createPatternComboBoxRenderer());
      this.patternComboBox.addItemListener(new ItemListener() {
          public void itemStateChanged(ItemEvent ev) {
            if (updatingFromController || ev.getStateChange() != ItemEvent.SELECTED) {
              return;
            }
            applyWallChanges(true);
          }
        });
    }

    private DefaultListCellRenderer createPatternComboBoxRenderer() {
      final float resolutionScale = SwingTools.getResolutionScale();
      return new DefaultListCellRenderer() {
          @Override
          public Component getListCellRendererComponent(final JList list,
              Object value, int index, boolean isSelected, boolean cellHasFocus) {
            TextureImage pattern = (TextureImage)value;
            final Component component = super.getListCellRendererComponent(
                list, pattern == null ? " " : "", index, isSelected, cellHasFocus);
            if (pattern != null) {
              final BufferedImage patternImage = SwingTools.getPatternImage(
                  pattern, list.getBackground(), list.getForeground());
              setIcon(new Icon() {
                  public int getIconWidth() {
                    return (int)(patternImage.getWidth() * 4 * resolutionScale + 1);
                  }

                  public int getIconHeight() {
                    return (int)(patternImage.getHeight() * resolutionScale + 2);
                  }

                  public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2D = (Graphics2D)g;
                    g2D.scale(resolutionScale, resolutionScale);
                    for (int i = 0; i < 4; i++) {
                      g2D.drawImage(patternImage, x + i * patternImage.getWidth(), y + 1, list);
                    }
                    g2D.scale(1 / resolutionScale, 1 / resolutionScale);
                    g2D.setColor(list.getForeground());
                    g2D.drawRect(x, y, getIconWidth() - 2, getIconHeight() - 1);
                  }
                });
            }
            return component;
          }
        };
    }

    private void updatePatternComboBoxModel() {
      TextureImage pattern = this.wallController.getPattern();
      List<TextureImage> patterns = this.preferences.getPatternsCatalog().getPatterns();
      if (pattern == null) {
        patterns = new ArrayList<TextureImage>(patterns);
        patterns.add(0, null);
      }
      this.patternComboBox.setModel(new DefaultComboBoxModel(patterns.toArray()));
      this.patternComboBox.setSelectedItem(pattern);
    }

    private void layoutFields() {
      int labelAlignment = OperatingSystem.isMacOSX()
          ? GridBagConstraints.LINE_END
          : GridBagConstraints.LINE_START;
      int standardGap = Math.round(5 * SwingTools.getResolutionScale());
      Insets fieldInsets = new Insets(0, 0, standardGap, 0);

      JPanel fieldsPanel = new JPanel(new GridBagLayout());
      JPanel stylePanel = SwingTools.createTitledPanel(this.preferences.getLocalizedString(
          SelectionInspectorPane.class, "wallStylePanel.title"));
      configureInspectorFieldLabel(this.preferences, WallPanel.class,
          "thicknessLabel.mnemonic", this.thicknessLabel, this.thicknessSpinner);
      configureInspectorFieldLabel(this.preferences, WallPanel.class,
          "rectangularWallHeightLabel.mnemonic", this.heightLabel, this.heightSpinner);
      configureInspectorFieldLabel(this.preferences, WallPanel.class,
          "patternLabel.mnemonic", this.patternLabel, this.patternComboBox);
      stylePanel.add(this.thicknessLabel, new GridBagConstraints(
          0, 0, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      stylePanel.add(this.thicknessSpinner, new GridBagConstraints(
          1, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
      stylePanel.add(this.heightLabel, new GridBagConstraints(
          0, 1, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      stylePanel.add(this.heightSpinner, new GridBagConstraints(
          1, 1, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
      stylePanel.add(this.patternLabel, new GridBagConstraints(
          0, 2, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      stylePanel.add(this.patternComboBox, new GridBagConstraints(
          1, 2, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));

      fieldsPanel.add(stylePanel, new GridBagConstraints(
          0, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, fieldInsets, 0, 0));

      JPanel headerPanel = new JPanel(new GridBagLayout());
      headerPanel.setBorder(AlpInspectorStyles.sectionGapBorder());
      headerPanel.add(this.summaryLabel, new GridBagConstraints(
          0, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, AlpInspectorStyles.scale(6), 0), 0, 0));
      headerPanel.add(this.openFullEditorButton, new GridBagConstraints(
          0, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

      JPanel contentPanel = new JPanel(new BorderLayout());
      contentPanel.setBorder(BorderFactory.createEmptyBorder(
          AlpInspectorStyles.panelInsets().top,
          AlpInspectorStyles.panelInsets().left,
          AlpInspectorStyles.panelInsets().bottom,
          AlpInspectorStyles.panelInsets().right));
      contentPanel.add(headerPanel, BorderLayout.NORTH);
      contentPanel.add(fieldsPanel, BorderLayout.CENTER);
      add(contentPanel, BorderLayout.NORTH);
    }

    void refresh() {
      updateSelectionSummary();
      this.updatingFromController = true;
      try {
        this.wallController.refreshProperties();

        Float thickness = this.wallController.getThickness();
        this.thicknessSpinnerModel.setNullable(thickness == null);
        this.thicknessSpinnerModel.setLength(thickness);

        Float height = this.wallController.getRectangularWallHeight();
        this.heightSpinnerModel.setNullable(height == null);
        this.heightSpinnerModel.setLength(height);

        updatePatternComboBoxModel();

        boolean rectangularWalls = this.wallController.getShape()
            == WallController.WallShape.RECTANGULAR_WALL;
        this.heightLabel.setEnabled(rectangularWalls);
        this.heightSpinner.setEnabled(rectangularWalls);
      } finally {
        this.updatingFromController = false;
      }
    }

    private void updateSelectionSummary() {
      List<Wall> walls = Home.getWallsSubList(this.home.getSelectedItems());
      String title;
      if (walls.size() == 1) {
        title = this.preferences.getLocalizedString(
            SelectionInspectorPane.class, "summarySingleWall.title");
      } else {
        title = MessageFormat.format(this.preferences.getLocalizedString(
            SelectionInspectorPane.class, "summaryMultipleWalls.title"), walls.size());
      }

      String levelName = getCommonLevelName(walls);
      String subtitle = levelName != null
          ? MessageFormat.format(this.preferences.getLocalizedString(
              SelectionInspectorPane.class, "summaryLayer.text"), levelName)
          : this.preferences.getLocalizedString(
              SelectionInspectorPane.class, "summaryMixedLevels.text");
      AlpInspectorStyles.applySummaryText(this.summaryLabel, title, subtitle);
    }

    private String getCommonLevelName(List<Wall> walls) {
      if (walls.isEmpty()) {
        return null;
      }
      Level level = walls.get(0).getLevel();
      String levelName = level != null ? level.getName() : null;
      for (int i = 1; i < walls.size(); i++) {
        Level otherLevel = walls.get(i).getLevel();
        String otherLevelName = otherLevel != null ? otherLevel.getName() : null;
        if (levelName == null) {
          if (otherLevelName != null) {
            return null;
          }
        } else if (!levelName.equals(otherLevelName)) {
          return null;
        }
      }
      return levelName;
    }

    private void syncControllerWallFieldsForModify(boolean applyPattern) {
      this.wallController.setXStart(null);
      this.wallController.setYStart(null);
      this.wallController.setXEnd(null);
      this.wallController.setYEnd(null);
      this.wallController.setArcExtentInDegrees(null);
      this.wallController.setSlopingWallHeightAtStart(null);
      this.wallController.setSlopingWallHeightAtEnd(null);
      this.wallController.setLeftSidePaint(null);
      this.wallController.setRightSidePaint(null);
      this.wallController.setTopPaint(null);
      this.wallController.getLeftSideBaseboardController().setVisible(null);
      this.wallController.getRightSideBaseboardController().setVisible(null);
      if (applyPattern) {
        this.wallController.setPattern((TextureImage)this.patternComboBox.getSelectedItem());
      } else {
        this.wallController.setPattern(null);
      }
    }

    private void applyWallChanges(boolean applyPattern) {
      if (this.updatingFromController) {
        return;
      }
      if (Home.getWallsSubList(this.home.getSelectedItems()).isEmpty()) {
        return;
      }
      syncControllerWallFieldsForModify(applyPattern);
      this.wallController.modifyWalls();
    }
  }

  /**
   * Furniture inspector with live edit fields (SPIKE-21 P3).
   */
  private class FurnitureInspectorPanel extends JPanel {
    private final Home                      home;
    private final HomeController            homeController;
    private final HomeFurnitureController   furnitureController;
    private final UserPreferences           preferences;
    private JLabel                          summaryLabel;
    private JButton                         openFullEditorButton;
    private AutoCompleteTextField           nameTextField;
    private JLabel                          widthLabel;
    private NullableSpinner                 widthSpinner;
    private NullableSpinner.NullableSpinnerLengthModel widthSpinnerModel;
    private JLabel                          depthLabel;
    private NullableSpinner                 depthSpinner;
    private NullableSpinner.NullableSpinnerLengthModel depthSpinnerModel;
    private JLabel                          heightLabel;
    private NullableSpinner                 heightSpinner;
    private NullableSpinner.NullableSpinnerLengthModel heightSpinnerModel;
    private JLabel                          angleLabel;
    private NullableSpinner                 angleSpinner;
    private NullableSpinner.NullableSpinnerNumberModel angleSpinnerModel;
    private JLabel                          colorLabel;
    private ColorButton                     colorButton;
    private boolean                         nameFieldUserEdited;
    private boolean                         updatingFromController;

    FurnitureInspectorPanel(Home home,
                            UserPreferences preferences,
                            HomeController controller) {
      super(new BorderLayout());
      this.home = home;
      this.homeController = controller;
      this.preferences = preferences;
      this.furnitureController = controller.createHomeFurnitureController();
      createHeader();
      createFields();
      layoutFields();
    }

    private void createHeader() {
      this.summaryLabel = new JLabel();
      this.openFullEditorButton = new JButton(this.preferences.getLocalizedString(
          SelectionInspectorPane.class, "openFullEditorButton.text"));
      if (!OperatingSystem.isMacOSX()) {
        this.openFullEditorButton.setMnemonic(KeyStroke.getKeyStroke(
            this.preferences.getLocalizedString(
                SelectionInspectorPane.class, "openFullEditorButton.mnemonic")).getKeyCode());
      }
      this.openFullEditorButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            commitPendingEdits();
            homeController.getPlanController().modifySelectedFurniture();
          }
        });
    }

    private void createFields() {
      float minimumLength = this.preferences.getLengthUnit().getMinimumLength();
      float maximumLength = this.preferences.getLengthUnit().getMaximumLength();
      String unitName = this.preferences.getLengthUnit().getName();

      this.nameTextField = new AutoCompleteTextField(
          "", 10, this.preferences.getAutoCompletionStrings("HomePieceOfFurnitureName"));
      if (!OperatingSystem.isMacOSXLeopardOrSuperior()) {
        SwingTools.addAutoSelectionOnFocusGain(this.nameTextField);
      }
      final PropertyChangeListener nameChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            syncNameFieldFromModel();
          }
        };
      this.furnitureController.addPropertyChangeListener(
          HomeFurnitureController.Property.NAME, nameChangeListener);
      this.nameTextField.getDocument().addDocumentListener(new DocumentListener() {
          public void changedUpdate(DocumentEvent ev) {
            updateControllerNameFromField(nameChangeListener);
          }

          public void insertUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }

          public void removeUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
        });
      this.nameTextField.addFocusListener(new FocusAdapter() {
          @Override
          public void focusLost(FocusEvent ev) {
            commitPendingEdits();
          }
        });

      this.widthLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, HomeFurniturePanel.class, "widthLabel.text", unitName));
      this.widthSpinnerModel = new NullableSpinner.NullableSpinnerLengthModel(
          this.preferences, minimumLength, maximumLength);
      this.widthSpinner = new NullableSpinner(this.widthSpinnerModel);
      this.widthSpinnerModel.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            if (updatingFromController) {
              return;
            }
            furnitureController.setWidth(widthSpinnerModel.getLength());
            applyFurnitureChanges(false, false);
          }
        });

      this.depthLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, HomeFurniturePanel.class, "depthLabel.text", unitName));
      this.depthSpinnerModel = new NullableSpinner.NullableSpinnerLengthModel(
          this.preferences, minimumLength, maximumLength);
      this.depthSpinner = new NullableSpinner(this.depthSpinnerModel);
      this.depthSpinnerModel.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            if (updatingFromController) {
              return;
            }
            furnitureController.setDepth(depthSpinnerModel.getLength());
            applyFurnitureChanges(false, false);
          }
        });

      this.heightLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, HomeFurniturePanel.class, "heightLabel.text", unitName));
      this.heightSpinnerModel = new NullableSpinner.NullableSpinnerLengthModel(
          this.preferences, minimumLength, maximumLength);
      this.heightSpinner = new NullableSpinner(this.heightSpinnerModel);
      this.heightSpinnerModel.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            if (updatingFromController) {
              return;
            }
            furnitureController.setHeight(heightSpinnerModel.getLength());
            applyFurnitureChanges(false, false);
          }
        });

      this.angleLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, HomeFurniturePanel.class, "angleLabel.text"));
      this.angleSpinnerModel = new NullableSpinner.NullableSpinnerModuloNumberModel(
          0f, 0f, 360f, 1f);
      this.angleSpinner = new NullableSpinner(this.angleSpinnerModel);
      this.angleSpinnerModel.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            if (updatingFromController) {
              return;
            }
            Number value = (Number)angleSpinnerModel.getValue();
            if (value == null) {
              furnitureController.setAngle(null);
            } else {
              furnitureController.setAngle((float)Math.toRadians(value.doubleValue()));
            }
            applyFurnitureChanges(false, false);
          }
        });

      this.colorLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, HomeFurniturePanel.class, "colorRadioButton.text"));
      this.colorButton = new ColorButton(this.preferences);
      this.colorButton.setColorDialogTitle(this.preferences.getLocalizedString(
          HomeFurniturePanel.class, "colorDialog.title"));
      this.colorButton.addPropertyChangeListener(ColorButton.COLOR_PROPERTY,
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              if (updatingFromController) {
                return;
              }
              furnitureController.setColor(colorButton.getColor());
              applyFurnitureChanges(false, true);
            }
          });
    }

    private void layoutFields() {
      int labelAlignment = OperatingSystem.isMacOSX()
          ? GridBagConstraints.LINE_END
          : GridBagConstraints.LINE_START;
      int standardGap = Math.round(5 * SwingTools.getResolutionScale());
      Insets fieldInsets = new Insets(0, 0, standardGap, 0);

      JPanel fieldsPanel = new JPanel(new GridBagLayout());
      int row = 0;

      JPanel namePanel = SwingTools.createTitledPanel(this.preferences.getLocalizedString(
          HomeFurniturePanel.class, "namePanel.title"));
      JLabel nameLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, HomeFurniturePanel.class, "nameLabel.text"));
      if (!OperatingSystem.isMacOSX()) {
        nameLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(
            this.preferences.getLocalizedString(
                HomeFurniturePanel.class, "nameLabel.mnemonic")).getKeyCode());
      }
      nameLabel.setLabelFor(this.nameTextField);
      namePanel.add(nameLabel, new GridBagConstraints(
          0, 0, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      namePanel.add(this.nameTextField, new GridBagConstraints(
          1, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));
      fieldsPanel.add(namePanel, new GridBagConstraints(
          0, row++, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, fieldInsets, 0, 0));

      JPanel sizePanel = SwingTools.createTitledPanel(this.preferences.getLocalizedString(
          HomeFurniturePanel.class, "sizePanel.title"));
      configureInspectorFieldLabel(this.preferences, HomeFurniturePanel.class,
          "widthLabel.mnemonic", this.widthLabel, this.widthSpinner);
      configureInspectorFieldLabel(this.preferences, HomeFurniturePanel.class,
          "depthLabel.mnemonic", this.depthLabel, this.depthSpinner);
      configureInspectorFieldLabel(this.preferences, HomeFurniturePanel.class,
          "heightLabel.mnemonic", this.heightLabel, this.heightSpinner);
      sizePanel.add(this.widthLabel, new GridBagConstraints(
          0, 0, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      sizePanel.add(this.widthSpinner, new GridBagConstraints(
          1, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
      sizePanel.add(this.depthLabel, new GridBagConstraints(
          0, 1, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      sizePanel.add(this.depthSpinner, new GridBagConstraints(
          1, 1, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
      sizePanel.add(this.heightLabel, new GridBagConstraints(
          0, 2, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      sizePanel.add(this.heightSpinner, new GridBagConstraints(
          1, 2, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));
      fieldsPanel.add(sizePanel, new GridBagConstraints(
          0, row++, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, fieldInsets, 0, 0));

      JPanel orientationPanel = SwingTools.createTitledPanel(this.preferences.getLocalizedString(
          HomeFurniturePanel.class, "orientationPanel.title"));
      configureInspectorFieldLabel(this.preferences, HomeFurniturePanel.class,
          "angleLabel.mnemonic", this.angleLabel, this.angleSpinner);
      orientationPanel.add(this.angleLabel, new GridBagConstraints(
          0, 0, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      orientationPanel.add(this.angleSpinner, new GridBagConstraints(
          1, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));
      fieldsPanel.add(orientationPanel, new GridBagConstraints(
          0, row++, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, fieldInsets, 0, 0));

      JPanel paintPanel = SwingTools.createTitledPanel(this.preferences.getLocalizedString(
          HomeFurniturePanel.class, "colorAndTexturePanel.title"));
      configureInspectorFieldLabel(this.preferences, HomeFurniturePanel.class,
          "colorRadioButton.mnemonic", this.colorLabel, this.colorButton);
      paintPanel.add(this.colorLabel, new GridBagConstraints(
          0, 0, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      paintPanel.add(this.colorButton, new GridBagConstraints(
          1, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));
      fieldsPanel.add(paintPanel, new GridBagConstraints(
          0, row, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

      JPanel headerPanel = new JPanel(new GridBagLayout());
      headerPanel.setBorder(AlpInspectorStyles.sectionGapBorder());
      headerPanel.add(this.summaryLabel, new GridBagConstraints(
          0, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, AlpInspectorStyles.scale(6), 0), 0, 0));
      headerPanel.add(this.openFullEditorButton, new GridBagConstraints(
          0, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

      JPanel contentPanel = new JPanel(new BorderLayout());
      contentPanel.setBorder(BorderFactory.createEmptyBorder(
          AlpInspectorStyles.panelInsets().top,
          AlpInspectorStyles.panelInsets().left,
          AlpInspectorStyles.panelInsets().bottom,
          AlpInspectorStyles.panelInsets().right));
      contentPanel.add(headerPanel, BorderLayout.NORTH);
      contentPanel.add(fieldsPanel, BorderLayout.CENTER);
      add(contentPanel, BorderLayout.NORTH);
    }

    void refresh() {
      updateSelectionSummary();
      this.updatingFromController = true;
      try {
        this.furnitureController.refreshProperties();

        Float width = this.furnitureController.getWidth();
        this.widthSpinnerModel.setNullable(width == null);
        this.widthSpinnerModel.setLength(width);

        Float depth = this.furnitureController.getDepth();
        this.depthSpinnerModel.setNullable(depth == null);
        this.depthSpinnerModel.setLength(depth);

        Float height = this.furnitureController.getHeight();
        this.heightSpinnerModel.setNullable(height == null);
        this.heightSpinnerModel.setLength(height);

        Float angle = this.furnitureController.getAngle();
        this.angleSpinnerModel.setNullable(angle == null);
        this.angleSpinnerModel.setValue(angle != null
            ? new Float((float)Math.toDegrees(angle))
            : null);

        this.colorButton.setColor(this.furnitureController.getColor());

        boolean resizable = this.furnitureController.isResizable();
        this.widthLabel.setEnabled(resizable);
        this.widthSpinner.setEnabled(resizable);
        this.depthLabel.setEnabled(resizable);
        this.depthSpinner.setEnabled(resizable);
        this.heightLabel.setEnabled(resizable);
        this.heightSpinner.setEnabled(resizable);
      } finally {
        this.updatingFromController = false;
      }
      syncNameFieldFromModel();
    }

    void syncNameFieldFromModel() {
      if (this.updatingFromController || this.nameFieldUserEdited) {
        return;
      }
      String displayName = getSelectedFurnitureCommonName();
      if (displayName == null) {
        displayName = "";
      }
      this.updatingFromController = true;
      try {
        this.nameTextField.setText(displayName);
      } finally {
        this.updatingFromController = false;
      }
    }

    private void updateControllerNameFromField(PropertyChangeListener nameChangeListener) {
      if (this.updatingFromController) {
        return;
      }
      this.nameFieldUserEdited = true;
      this.furnitureController.removePropertyChangeListener(
          HomeFurnitureController.Property.NAME, nameChangeListener);
      String name = this.nameTextField.getText();
      if (name == null || name.trim().length() == 0) {
        this.furnitureController.setName("");
      } else {
        this.furnitureController.setName(name);
      }
      this.furnitureController.addPropertyChangeListener(
          HomeFurnitureController.Property.NAME, nameChangeListener);
    }

    private void updateSelectionSummary() {
      List<HomePieceOfFurniture> furniture = Home.getFurnitureSubList(this.home.getSelectedItems());
      String title;
      if (furniture.size() == 1) {
        HomePieceOfFurniture piece = furniture.get(0);
        String name = piece.getName();
        if (name != null && name.trim().length() > 0) {
          title = MessageFormat.format(this.preferences.getLocalizedString(
              SelectionInspectorPane.class, "summarySingleFurnitureNamed.title"), name.trim());
        } else {
          title = this.preferences.getLocalizedString(
              SelectionInspectorPane.class, "summarySingleFurniture.title");
        }
      } else {
        title = MessageFormat.format(this.preferences.getLocalizedString(
            SelectionInspectorPane.class, "summaryMultipleFurniture.title"), furniture.size());
      }

      String levelName = getCommonLevelName(furniture);
      String subtitle = levelName != null
          ? MessageFormat.format(this.preferences.getLocalizedString(
              SelectionInspectorPane.class, "summaryLayer.text"), levelName)
          : this.preferences.getLocalizedString(
              SelectionInspectorPane.class, "summaryMixedLevels.text");
      AlpInspectorStyles.applySummaryText(this.summaryLabel, title, subtitle);
    }

    private String getCommonLevelName(List<HomePieceOfFurniture> furniture) {
      if (furniture.isEmpty()) {
        return null;
      }
      Level level = furniture.get(0).getLevel();
      String levelName = level != null ? level.getName() : null;
      for (int i = 1; i < furniture.size(); i++) {
        Level otherLevel = furniture.get(i).getLevel();
        String otherLevelName = otherLevel != null ? otherLevel.getName() : null;
        if (levelName == null) {
          if (otherLevelName != null) {
            return null;
          }
        } else if (!levelName.equals(otherLevelName)) {
          return null;
        }
      }
      return levelName;
    }

    void commitPendingEdits() {
      commitPendingEditsForFurniture(Home.getFurnitureSubList(this.home.getSelectedItems()));
    }

    void commitPendingEditsForFurniture(List<HomePieceOfFurniture> targetFurniture) {
      if (!this.nameFieldUserEdited || !isNameCommitNeededForFurniture(targetFurniture)) {
        markNameFieldSynced();
        return;
      }
      if (targetFurniture.isEmpty()) {
        return;
      }
      List<Selectable> savedSelection = new ArrayList<Selectable>(this.home.getSelectedItems());
      SelectionInspectorPane.this.suppressSelectionListener = true;
      try {
        this.home.setSelectedItems(new ArrayList<Selectable>(targetFurniture));
        applyFurnitureChanges(true, false);
        this.home.setSelectedItems(savedSelection);
      } finally {
        SelectionInspectorPane.this.suppressSelectionListener = false;
      }
    }

    private boolean isNameCommitNeededForFurniture(List<HomePieceOfFurniture> furniture) {
      String textName = this.nameTextField.getText();
      if (textName == null) {
        textName = "";
      }
      String modelName = getCommonName(furniture);
      if (modelName == null) {
        modelName = "";
      }
      return !textName.equals(modelName);
    }

    private String getSelectedFurnitureCommonName() {
      return getCommonName(Home.getFurnitureSubList(this.home.getSelectedItems()));
    }

    private String getCommonName(List<HomePieceOfFurniture> furniture) {
      if (furniture.isEmpty()) {
        return null;
      }
      String name = furniture.get(0).getName();
      for (int i = 1; i < furniture.size(); i++) {
        if (name == null) {
          if (furniture.get(i).getName() != null) {
            return null;
          }
        } else if (!name.equals(furniture.get(i).getName())) {
          return null;
        }
      }
      return name;
    }

    private void syncControllerFurnitureFieldsForModify(boolean applyName, boolean applyColor) {
      if (applyName && this.nameFieldUserEdited
          && isNameCommitNeededForFurniture(Home.getFurnitureSubList(this.home.getSelectedItems()))) {
        String name = this.nameTextField.getText();
        if (name == null || name.trim().length() == 0) {
          this.furnitureController.setName("");
        } else {
          this.furnitureController.setName(name);
        }
      } else {
        this.furnitureController.setName(null);
      }
      this.furnitureController.setX(null);
      this.furnitureController.setY(null);
      this.furnitureController.setElevation(null);
      this.furnitureController.setRoll(null);
      this.furnitureController.setPitch(null);
      if (applyColor) {
        this.furnitureController.setPaint(HomeFurnitureController.FurniturePaint.COLORED);
        this.furnitureController.setColor(this.colorButton.getColor());
      } else {
        this.furnitureController.setPaint(null);
      }
    }

    private void applyFurnitureChanges(boolean applyName, boolean applyColor) {
      if (this.updatingFromController) {
        return;
      }
      if (Home.getFurnitureSubList(this.home.getSelectedItems()).isEmpty()) {
        return;
      }
      syncControllerFurnitureFieldsForModify(applyName, applyColor);
      this.furnitureController.modifyFurniture();
      if (applyName) {
        markNameFieldSynced();
      }
    }

    private void markNameFieldSynced() {
      this.nameFieldUserEdited = false;
    }
  }

  /**
   * Layer inspector when plan selection is empty (SPIKE-24).
   */
  private class LevelInspectorPanel extends JPanel {
    private final Home             home;
    private final HomeController   homeController;
    private final LevelController  levelController;
    private final UserPreferences  preferences;
    private JLabel                 summaryLabel;
    private JButton                openFullEditorButton;
    private JTextField             nameTextField;
    private JCheckBox              viewableCheckBox;
    private JCheckBox              lockedCheckBox;
    private JCheckBox              selectCurrentLayerOnlyCheckBox;
    private JComboBox<LevelCategory> categoryComboBox;
    private JLabel                 plantTakeoffLabel;
    private JComboBox<LevelPlantTakeoff> plantTakeoffComboBox;
    private boolean                updatingFromController;
    private boolean                nameFieldUserEdited;
    private String                 nameFieldSyncedValue;

    LevelInspectorPanel(Home home,
                        UserPreferences preferences,
                        HomeController controller) {
      super(new BorderLayout());
      this.home = home;
      this.homeController = controller;
      this.preferences = preferences;
      this.levelController = controller.createLevelController();
      createHeader();
      createFields();
      layoutFields();
    }

    private void createHeader() {
      this.summaryLabel = new JLabel();
      this.openFullEditorButton = new JButton(this.preferences.getLocalizedString(
          SelectionInspectorPane.class, "openFullLevelEditorButton.text"));
      if (!OperatingSystem.isMacOSX()) {
        this.openFullEditorButton.setMnemonic(KeyStroke.getKeyStroke(
            this.preferences.getLocalizedString(
                SelectionInspectorPane.class, "openFullLevelEditorButton.mnemonic")).getKeyCode());
      }
      this.openFullEditorButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            commitPendingEdits();
            homeController.getPlanController().modifySelectedLevel();
            refresh();
          }
        });
    }

    private void createFields() {
      this.nameTextField = new AutoCompleteTextField(
          "", 15, this.preferences.getAutoCompletionStrings("LevelName"));
      if (!OperatingSystem.isMacOSXLeopardOrSuperior()) {
        SwingTools.addAutoSelectionOnFocusGain(this.nameTextField);
      }
      final PropertyChangeListener nameChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            syncNameFieldFromModel();
          }
        };
      this.levelController.addPropertyChangeListener(
          LevelController.Property.NAME, nameChangeListener);
      this.nameTextField.getDocument().addDocumentListener(new DocumentListener() {
          public void changedUpdate(DocumentEvent ev) {
            updateControllerNameFromField(nameChangeListener);
          }

          public void insertUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }

          public void removeUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
        });
      this.nameTextField.addFocusListener(new FocusAdapter() {
          @Override
          public void focusLost(FocusEvent ev) {
            commitPendingEdits();
          }
        });
      this.nameTextField.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            commitPendingEdits();
          }
        });

      this.viewableCheckBox = createImmediateClickCheckBox(SwingTools.getLocalizedLabelText(
          this.preferences, LevelPanel.class, "viewableCheckBox.text"));
      if (!OperatingSystem.isMacOSX()) {
        this.viewableCheckBox.setMnemonic(KeyStroke.getKeyStroke(
            this.preferences.getLocalizedString(
                LevelPanel.class, "viewableCheckBox.mnemonic")).getKeyCode());
      }
      this.viewableCheckBox.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            if (updatingFromController) {
              return;
            }
            levelController.setViewable(viewableCheckBox.isSelected());
            applyLevelChanges();
          }
        });

      this.lockedCheckBox = createImmediateClickCheckBox(SwingTools.getLocalizedLabelText(
          this.preferences, LevelPanel.class, "lockedCheckBox.text"));
      if (!OperatingSystem.isMacOSX()) {
        this.lockedCheckBox.setMnemonic(KeyStroke.getKeyStroke(
            this.preferences.getLocalizedString(
                LevelPanel.class, "lockedCheckBox.mnemonic")).getKeyCode());
      }
      this.lockedCheckBox.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            if (updatingFromController) {
              return;
            }
            levelController.setLocked(lockedCheckBox.isSelected());
            applyLevelChanges();
          }
        });

      this.selectCurrentLayerOnlyCheckBox = createImmediateClickCheckBox(
          this.preferences.getLocalizedString(
              SelectionInspectorPane.class, "selectCurrentLayerOnlyCheckBox.text"));
      if (!OperatingSystem.isMacOSX()) {
        this.selectCurrentLayerOnlyCheckBox.setMnemonic(KeyStroke.getKeyStroke(
            this.preferences.getLocalizedString(
                SelectionInspectorPane.class, "selectCurrentLayerOnlyCheckBox.mnemonic")).getKeyCode());
      }
      this.selectCurrentLayerOnlyCheckBox.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            if (updatingFromController) {
              return;
            }
            home.setSelectCurrentLayerOnly(selectCurrentLayerOnlyCheckBox.isSelected());
          }
        });
      this.home.addPropertyChangeListener(
          Home.Property.SELECT_CURRENT_LAYER_ONLY, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              if (updatingFromController) {
                return;
              }
              updatingFromController = true;
              try {
                selectCurrentLayerOnlyCheckBox.setSelected(home.isSelectCurrentLayerOnly());
              } finally {
                updatingFromController = false;
              }
            }
          });

      this.categoryComboBox = AlpLevelRoleControls.createCategoryComboBox(this.preferences);
      this.categoryComboBox.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            if (updatingFromController) {
              return;
            }
            LevelCategory category = (LevelCategory)categoryComboBox.getSelectedItem();
            if (category != null) {
              levelController.setCategory(category);
              updatePlantTakeoffVisibility();
              applyLevelChanges();
            }
          }
        });
      this.levelController.addPropertyChangeListener(LevelController.Property.CATEGORY,
          new PropertyChangeListener() {
              public void propertyChange(PropertyChangeEvent ev) {
                if (updatingFromController) {
                  return;
                }
                updatingFromController = true;
                try {
                  categoryComboBox.setSelectedItem(levelController.getCategory());
                  updatePlantTakeoffVisibility();
                } finally {
                  updatingFromController = false;
                }
              }
            });

      this.plantTakeoffLabel = AlpLevelRoleControls.createPlantTakeoffLabel(this.preferences);
      this.plantTakeoffComboBox = AlpLevelRoleControls.createPlantTakeoffComboBox(this.preferences);
      this.plantTakeoffComboBox.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            if (updatingFromController) {
              return;
            }
            LevelPlantTakeoff plantTakeoff = (LevelPlantTakeoff)plantTakeoffComboBox.getSelectedItem();
            if (plantTakeoff != null) {
              levelController.setPlantTakeoff(plantTakeoff);
              applyLevelChanges();
            }
          }
        });
      this.levelController.addPropertyChangeListener(LevelController.Property.PLANT_TAKEOFF,
          new PropertyChangeListener() {
              public void propertyChange(PropertyChangeEvent ev) {
                if (updatingFromController) {
                  return;
                }
                updatingFromController = true;
                try {
                  plantTakeoffComboBox.setSelectedItem(levelController.getPlantTakeoff());
                } finally {
                  updatingFromController = false;
                }
              }
            });

    }

    private void layoutFields() {
      int labelAlignment = OperatingSystem.isMacOSX()
          ? GridBagConstraints.LINE_END
          : GridBagConstraints.LINE_START;
      int standardGap = Math.round(5 * SwingTools.getResolutionScale());
      Insets fieldInsets = new Insets(0, 0, standardGap, 0);

      JPanel fieldsPanel = new JPanel(new GridBagLayout());
      int row = 0;

      JPanel propertiesPanel = SwingTools.createTitledPanel(this.preferences.getLocalizedString(
          SelectionInspectorPane.class, "layerPropertiesPanel.title"));
      JLabel nameLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, LevelPanel.class, "nameLabel.text"));
      configureInspectorFieldLabel(this.preferences, LevelPanel.class,
          "nameLabel.mnemonic", nameLabel, this.nameTextField);
      propertiesPanel.add(nameLabel, new GridBagConstraints(
          0, 0, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      propertiesPanel.add(this.nameTextField, new GridBagConstraints(
          1, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
      JLabel categoryLabel = AlpLevelRoleControls.createCategoryLabel(this.preferences);
      configureInspectorFieldLabel(this.preferences, LevelPanel.class,
          "categoryLabel.mnemonic", categoryLabel, this.categoryComboBox);
      propertiesPanel.add(categoryLabel, new GridBagConstraints(
          0, 1, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      propertiesPanel.add(this.categoryComboBox, new GridBagConstraints(
          1, 1, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
      propertiesPanel.add(this.plantTakeoffLabel, new GridBagConstraints(
          0, 2, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      propertiesPanel.add(this.plantTakeoffComboBox, new GridBagConstraints(
          1, 2, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
      propertiesPanel.add(this.viewableCheckBox, new GridBagConstraints(
          0, 3, 2, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 10), 0, 0));
      propertiesPanel.add(this.lockedCheckBox, new GridBagConstraints(
          0, 4, 2, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 10), 0, 0));
      propertiesPanel.add(this.selectCurrentLayerOnlyCheckBox, new GridBagConstraints(
          0, 5, 2, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 10), 0, 0));

      fieldsPanel.add(propertiesPanel, new GridBagConstraints(
          0, row, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, fieldInsets, 0, 0));

      JPanel headerPanel = new JPanel(new GridBagLayout());
      headerPanel.setBorder(AlpInspectorStyles.sectionGapBorder());
      headerPanel.add(this.summaryLabel, new GridBagConstraints(
          0, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, AlpInspectorStyles.scale(6), 0), 0, 0));
      headerPanel.add(this.openFullEditorButton, new GridBagConstraints(
          0, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

      JPanel contentPanel = new JPanel(new BorderLayout());
      contentPanel.setBorder(BorderFactory.createEmptyBorder(
          AlpInspectorStyles.panelInsets().top,
          AlpInspectorStyles.panelInsets().left,
          AlpInspectorStyles.panelInsets().bottom,
          AlpInspectorStyles.panelInsets().right));
      contentPanel.add(headerPanel, BorderLayout.NORTH);
      contentPanel.add(fieldsPanel, BorderLayout.CENTER);
      add(contentPanel, BorderLayout.NORTH);
    }

    void refresh() {
      updateSelectionSummary();
      this.levelController.refreshProperties();
      this.updatingFromController = true;
      try {
        String name = this.levelController.getName();
        this.nameTextField.setText(name != null ? name : "");
        this.nameFieldSyncedValue = name != null ? name : "";
        this.nameFieldUserEdited = false;
        Boolean viewable = this.levelController.getViewable();
        this.viewableCheckBox.setSelected(viewable == null || viewable);
        this.lockedCheckBox.setSelected(Boolean.TRUE.equals(this.levelController.getLocked()));
        this.selectCurrentLayerOnlyCheckBox.setSelected(this.home.isSelectCurrentLayerOnly());
        syncRoleControlsFromModel();
      } finally {
        this.updatingFromController = false;
      }
    }

    void commitPendingEdits() {
      if (this.home.getSelectedLevel() == null) {
        return;
      }
      if (this.nameFieldUserEdited && isNameCommitNeeded()) {
        syncControllerNameForModify();
        applyLevelChanges();
      }
    }

    private void updateSelectionSummary() {
      AlpInspectorStyles.applySummaryText(this.summaryLabel,
          this.preferences.getLocalizedString(
              SelectionInspectorPane.class, "summarySingleLayer.title"),
          null);
    }

    private void syncNameFieldFromModel() {
      if (this.updatingFromController || this.nameFieldUserEdited) {
        return;
      }
      String displayName = this.levelController.getName();
      if (displayName == null) {
        displayName = "";
      }
      this.updatingFromController = true;
      try {
        this.nameTextField.setText(displayName);
        this.nameFieldSyncedValue = displayName;
      } finally {
        this.updatingFromController = false;
      }
    }

    private void updateControllerNameFromField(PropertyChangeListener nameChangeListener) {
      if (this.updatingFromController) {
        return;
      }
      this.nameFieldUserEdited = true;
      this.levelController.removePropertyChangeListener(
          LevelController.Property.NAME, nameChangeListener);
      String name = this.nameTextField.getText();
      if (name == null || name.trim().length() == 0) {
        this.levelController.setName("");
      } else {
        this.levelController.setName(name);
      }
      this.levelController.addPropertyChangeListener(
          LevelController.Property.NAME, nameChangeListener);
    }

    private boolean isNameCommitNeeded() {
      if (!this.nameFieldUserEdited) {
        return false;
      }
      String textName = this.nameTextField.getText();
      if (textName == null) {
        textName = "";
      }
      Level level = this.home.getSelectedLevel();
      String persistedName = level != null && level.getName() != null ? level.getName() : "";
      return !textName.equals(persistedName);
    }

    private void syncControllerNameForModify() {
      String name = this.nameTextField.getText();
      if (name == null || name.trim().length() == 0) {
        this.levelController.setName("");
      } else {
        this.levelController.setName(name);
      }
    }

    private void applyLevelChanges() {
      if (this.updatingFromController || this.home.getSelectedLevel() == null) {
        return;
      }
      if (this.nameFieldUserEdited) {
        syncControllerNameForModify();
      }
      this.levelController.modifyLevels();
      this.nameFieldSyncedValue = this.nameTextField.getText();
      this.nameFieldUserEdited = false;
      syncCheckBoxesFromModel();
      syncRoleControlsFromModel();
      updateSelectionSummary();
    }

    private void syncRoleControlsFromModel() {
      Level level = this.home.getSelectedLevel();
      LevelCategory category = level != null ? level.getCategory() : this.levelController.getCategory();
      LevelPlantTakeoff plantTakeoff = level != null
          ? level.getPlantTakeoff() : this.levelController.getPlantTakeoff();
      if (category == null) {
        category = LevelCategory.GENERAL;
      }
      if (plantTakeoff == null) {
        plantTakeoff = LevelPlantTakeoff.EXCLUDE;
      }
      this.updatingFromController = true;
      try {
        this.categoryComboBox.setSelectedItem(category);
        this.plantTakeoffComboBox.setSelectedItem(plantTakeoff);
        updatePlantTakeoffVisibility();
      } finally {
        this.updatingFromController = false;
      }
    }

    private void updatePlantTakeoffVisibility() {
      LevelCategory category = (LevelCategory)this.categoryComboBox.getSelectedItem();
      if (category == null) {
        Level level = this.home.getSelectedLevel();
        category = level != null ? level.getCategory() : this.levelController.getCategory();
      }
      if (category == null) {
        category = LevelCategory.GENERAL;
      }
      AlpLevelRoleControls.updatePlantTakeoffVisibility(
          this.plantTakeoffLabel, this.plantTakeoffComboBox, category);
    }

    /**
     * modifyLevels() fires a selection change before it applies the locked value,
     * so any refresh triggered mid-modification reads a stale level. Re-read the
     * level once the modification finished.
     */
    private void syncCheckBoxesFromModel() {
      Level level = this.home.getSelectedLevel();
      if (level == null) {
        return;
      }
      this.updatingFromController = true;
      try {
        this.viewableCheckBox.setSelected(level.isViewable());
        this.lockedCheckBox.setSelected(level.isLocked());
      } finally {
        this.updatingFromController = false;
      }
    }
  }
}
