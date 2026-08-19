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
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

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
 * Docked selection-mode inspector for SPIKE-19.
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

  public SelectionInspectorPane(Home home,
                                UserPreferences preferences,
                                HomeController controller) {
    super(new BorderLayout());
    this.home = home;
    this.preferences = preferences;
    this.cardLayout = new CardLayout();
    this.cardPanel = new JPanel(this.cardLayout);
    this.emptyLabel = new JLabel("", JLabel.CENTER);
    this.emptyLabel.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));
    this.roomInspectorPanel = new RoomInspectorPanel(home, preferences, controller);

    this.cardPanel.add(this.emptyLabel, EMPTY_CARD);
    this.cardPanel.add(this.roomInspectorPanel, ROOM_CARD);
    add(this.cardPanel, BorderLayout.NORTH);
    setMinimumSize(new Dimension((int)(200 * SwingTools.getResolutionScale()), 0));

    home.addSelectionListener(new SelectionListener() {
        public void selectionChanged(SelectionEvent ev) {
          updateForSelection();
        }
      });
    updateForSelection();
  }

  private void updateForSelection() {
    this.roomInspectorPanel.commitPendingEdits();

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

    this.roomInspectorPanel.refresh();
    this.cardLayout.show(this.cardPanel, ROOM_CARD);
  }

  private void showEmpty(String messageKey) {
    this.emptyLabel.setText(this.preferences.getLocalizedString(
        SelectionInspectorPane.class, messageKey));
    this.cardLayout.show(this.cardPanel, EMPTY_CARD);
  }

  private static boolean isRoomLocked(Room room) {
    Level level = room.getLevel();
    return level != null && level.isLocked();
  }

  /**
   * Area/Room inspector content with live name editing.
   */
  private static class RoomInspectorPanel extends JPanel {
    private final Home           home;
    private final RoomController roomController;
    private final UserPreferences preferences;
    private JTextField           nameTextField;
    private String               lastCommittedName;

    RoomInspectorPanel(Home home,
                       UserPreferences preferences,
                       HomeController controller) {
      super(new BorderLayout());
      this.home = home;
      this.preferences = preferences;
      this.roomController = controller.createRoomController();
      createNameField();
      layoutNameField();
    }

    private void createNameField() {
      this.nameTextField = new AutoCompleteTextField(
          "", 10, this.preferences.getAutoCompletionStrings("RoomName"));
      if (!OperatingSystem.isMacOSXLeopardOrSuperior()) {
        SwingTools.addAutoSelectionOnFocusGain(this.nameTextField);
      }

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
              nameTextField.transferFocus();
            }
          }
        });
    }

    private void layoutNameField() {
      int labelAlignment = OperatingSystem.isMacOSX()
          ? GridBagConstraints.LINE_END
          : GridBagConstraints.LINE_START;
      int standardGap = Math.round(5 * SwingTools.getResolutionScale());
      JLabel nameLabel = new JLabel(SwingTools.getLocalizedLabelText(
          this.preferences, RoomPanel.class, "nameLabel.text"));
      if (!OperatingSystem.isMacOSX()) {
        nameLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(
            this.preferences.getLocalizedString(RoomPanel.class, "nameLabel.mnemonic")).getKeyCode());
      }
      nameLabel.setLabelFor(this.nameTextField);

      JPanel namePanel = SwingTools.createTitledPanel(this.preferences.getLocalizedString(
          RoomPanel.class, "nameAndAreaPanel.title"));
      namePanel.add(nameLabel, new GridBagConstraints(
          0, 0, 1, 1, 0, 0, labelAlignment,
          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, standardGap), 0, 0));
      namePanel.add(this.nameTextField, new GridBagConstraints(
          1, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));

      namePanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
      add(namePanel, BorderLayout.NORTH);
    }

    void refresh() {
      this.roomController.refreshProperties();
      String name = this.roomController.getName();
      this.nameTextField.setText(name != null ? name : "");
      this.lastCommittedName = name;
    }

    void commitPendingEdits() {
      if (!this.nameTextField.isEnabled()) {
        return;
      }
      String name = this.nameTextField.getText();
      if (name == null || name.trim().length() == 0) {
        name = "";
      }
      String committed = this.lastCommittedName != null ? this.lastCommittedName : "";
      if (committed.equals(name)) {
        return;
      }
      if (Home.getRoomsSubList(this.home.getSelectedItems()).isEmpty()) {
        return;
      }
      this.roomController.setName(name);
      this.roomController.modifyRooms();
      this.lastCommittedName = this.roomController.getName();
    }
  }
}
