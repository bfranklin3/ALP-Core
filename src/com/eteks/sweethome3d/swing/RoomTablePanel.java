/*
 * RoomTablePanel.java
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
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.viewcontroller.HomeController;

/**
 * A panel that displays the RoomTable.
 */
public class RoomTablePanel extends JPanel {
  private RoomTable roomTable;

  public RoomTablePanel(Home home, UserPreferences preferences, HomeController controller) {
    super(new BorderLayout());
    this.roomTable = new RoomTable(home, preferences, controller);
    
    JScrollPane scrollPane = SwingTools.createScrollPane(this.roomTable);
    scrollPane.setMinimumSize(new Dimension());
    
    // Give focus to table when viewport is clicked
    final JViewport viewport = scrollPane.getViewport();
    viewport.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent ev) {
        viewport.getView().requestFocusInWindow();
      }
    });

    add(scrollPane, BorderLayout.CENTER);
  }
}
