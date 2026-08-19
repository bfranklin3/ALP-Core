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
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.List;

import java.text.MessageFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.Level;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.SelectionEvent;
import com.eteks.sweethome3d.model.SelectionListener;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.tools.OperatingSystem;
import com.eteks.sweethome3d.viewcontroller.HomeController;
import com.eteks.sweethome3d.viewcontroller.RoomController;

/**
 * Docked selection-mode inspector for SPIKE-19 / SPIKE-21.
 */
public class SelectionInspectorPane extends JPanel {
  private static final String EMPTY_CARD = "empty";
  private static final String ROOM_CARD = "room";

  private final Home              home;
  private final UserPreferences   preferences;
  private final CardLayout        cardLayout;
  private final JPanel            cardPanel;
  private final JLabel            emptyLabel;
  private final RoomInspectorPanel roomInspectorPanel;
  private final List<Room>        monitoredRooms;
  private boolean                 showingRoomInspector;
  private boolean                 suppressSelectionListener;
  private final PropertyChangeListener roomPropertyListener;

  public SelectionInspectorPane(Home home,
                                UserPreferences preferences,
                                HomeController controller) {
    super(new BorderLayout());
    this.home = home;
    this.preferences = preferences;
    this.cardLayout = new CardLayout();
    this.cardPanel = new JPanel(this.cardLayout);
    this.emptyLabel = new JLabel("", JLabel.CENTER);
    this.emptyLabel.setBorder(AlpInspectorStyles.emptyStateBorder());
    this.roomInspectorPanel = new RoomInspectorPanel(home, preferences, controller);
    this.monitoredRooms = new ArrayList<Room>();
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

    this.cardPanel.add(this.emptyLabel, EMPTY_CARD);
    this.cardPanel.add(this.roomInspectorPanel, ROOM_CARD);
    add(this.cardPanel, BorderLayout.NORTH);
    setMinimumSize(new Dimension((int)(200 * SwingTools.getResolutionScale()), 0));

    home.addSelectionListener(new SelectionListener() {
        public void selectionChanged(SelectionEvent ev) {
          if (suppressSelectionListener) {
            return;
          }
          // monitoredRooms still refers to the previous selection here.
          roomInspectorPanel.commitPendingEditsForRooms(
              new ArrayList<Room>(monitoredRooms));
          updateForSelection();
        }
      });
    updateForSelection();
  }

  private void updateForSelection() {
    List<Selectable> selectedItems = this.home.getSelectedItems();
    List<Room> selectedRooms = Home.getRoomsSubList(selectedItems);
    if (selectedRooms.isEmpty()) {
      showEmpty("emptySelection.message");
      return;
    }
    if (selectedItems.size() != selectedRooms.size()) {
      showEmpty("mixedSelection.message");
      return;
    }
    for (Room room : selectedRooms) {
      if (isRoomLocked(room)) {
        showEmpty("lockedRoom.message");
        return;
      }
    }

    updateMonitoredRooms(selectedRooms);
    this.showingRoomInspector = true;
    this.roomInspectorPanel.refresh();
    this.cardLayout.show(this.cardPanel, ROOM_CARD);
  }

  private void showEmpty(String messageKey) {
    this.showingRoomInspector = false;
    updateMonitoredRooms(null);
    this.emptyLabel.setText(this.preferences.getLocalizedString(
        SelectionInspectorPane.class, messageKey));
    this.cardLayout.show(this.cardPanel, EMPTY_CARD);
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

  private static boolean isRoomLocked(Room room) {
    Level level = room.getLevel();
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
    private NullableCheckBox       areaVisibleCheckBox;
    private ColorButton            floorColorButton;
    private JLabel                 floorOpacityLabel;
    private NullableSpinner          floorOpacitySpinner;
    private NullableSpinner.NullableSpinnerNumberModel floorOpacitySpinnerModel;
    private NullableCheckBox       smoothedCheckBox;
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

      this.areaVisibleCheckBox = new NullableCheckBox(SwingTools.getLocalizedLabelText(
          this.preferences, RoomPanel.class, "areaVisibleCheckBox.text"));
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
      this.floorColorButton.setColorDialogTitle(this.preferences.getLocalizedString(
          RoomPanel.class, "floorColorDialog.title"));
      this.floorColorButton.addPropertyChangeListener(ColorButton.COLOR_PROPERTY,
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              if (updatingFromController) {
                return;
              }
              roomController.setFloorColor(floorColorButton.getColor());
              roomController.setFloorPaint(RoomController.RoomPaint.COLORED);
              applyRoomChanges();
            }
          });

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
      namePanel.add(this.areaVisibleCheckBox, new GridBagConstraints(
          0, 1, 2, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 10), 0, 0));

      fieldsPanel.add(namePanel, new GridBagConstraints(
          0, row++, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, fieldInsets, 0, 0));

      JPanel floorPanel = SwingTools.createTitledPanel(this.preferences.getLocalizedString(
          RoomPanel.class, "floorPanel.title"));
      JLabel floorColorLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, RoomPanel.class, "floorColorRadioButton.text"));
      floorPanel.add(floorColorLabel, new GridBagConstraints(
          0, 0, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      floorPanel.add(this.floorColorButton, new GridBagConstraints(
          1, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 10), 0, 0));
      floorPanel.add(this.floorOpacityLabel, new GridBagConstraints(
          0, 1, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      floorPanel.add(this.floorOpacitySpinner, new GridBagConstraints(
          1, 1, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));
      if (this.smoothedCheckBox != null) {
        floorPanel.add(this.smoothedCheckBox, new GridBagConstraints(
            0, 2, 2, 1, 1, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 10), 0, 0));
      }

      fieldsPanel.add(floorPanel, new GridBagConstraints(
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

    void refresh() {
      updateSelectionSummary();
      this.updatingFromController = true;
      try {
        this.roomController.refreshProperties();

        this.areaVisibleCheckBox.setNullable(this.roomController.getAreaVisible() == null);
        this.areaVisibleCheckBox.setValue(this.roomController.getAreaVisible());

        this.floorColorButton.setColor(this.roomController.getFloorColor());
        updateFloorColorEnabled();

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

    private void updateFloorColorEnabled() {
      Boolean floorVisible = this.roomController.getFloorVisible();
      this.floorColorButton.setEnabled(floorVisible == null || floorVisible);
    }

    private void updateFloorOpacityEnabled() {
      Boolean floorVisible = this.roomController.getFloorVisible();
      boolean enabled = floorVisible == null || floorVisible;
      this.floorOpacitySpinner.setEnabled(enabled);
      this.floorOpacityLabel.setEnabled(enabled);
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
}
