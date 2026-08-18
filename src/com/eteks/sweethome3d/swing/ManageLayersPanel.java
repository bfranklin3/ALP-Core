/*
 * ManageLayersPanel.java
 *
 * Sweet Home 3D / ALP CAD — manage all plan layers (SPIKE-15b).
 */
package com.eteks.sweethome3d.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.undo.UndoableEditSupport;

import com.eteks.sweethome3d.model.CollectionEvent;
import com.eteks.sweethome3d.model.CollectionListener;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.Level;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.tools.OperatingSystem;
import com.eteks.sweethome3d.viewcontroller.DialogView;
import com.eteks.sweethome3d.viewcontroller.LevelController;
import com.eteks.sweethome3d.viewcontroller.PlanController;
import com.eteks.sweethome3d.viewcontroller.View;

/**
 * Panel displayed in a dialog to manage layer order and open layer properties.
 */
public class ManageLayersPanel extends JPanel implements DialogView {
  private final Home                  home;
  private final UserPreferences       preferences;
  private final PlanController        planController;
  private final UndoableEditSupport   undoSupport;
  private final String                dialogTitle;
  private final LevelsTableModel      tableModel;
  private final JTable                levelsTable;
  private JScrollPane                   levelsScrollPane;
  private final JButton               moveUpButton;
  private final JButton               moveDownButton;
  private final JButton               modifyLayerButton;
  private boolean                     syncingSelection;

  public ManageLayersPanel(Home home,
                           UserPreferences preferences,
                           PlanController planController,
                           UndoableEditSupport undoSupport) {
    super(new BorderLayout(
        Math.round(5 * SwingTools.getResolutionScale()),
        Math.round(5 * SwingTools.getResolutionScale())));
    this.home = home;
    this.preferences = preferences;
    this.planController = planController;
    this.undoSupport = undoSupport;
    this.dialogTitle = preferences.getLocalizedString(ManageLayersPanel.class, "title");

    String [] columnNames = {
        preferences.getLocalizedString(ManageLayersPanel.class, "nameColumn"),
        preferences.getLocalizedString(ManageLayersPanel.class, "viewableColumn"),
        preferences.getLocalizedString(ManageLayersPanel.class, "lockedColumn")};
    this.tableModel = new LevelsTableModel(columnNames);
    this.levelsTable = new JTable(this.tableModel);
    this.levelsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    float resolutionScale = SwingTools.getResolutionScale();
    if (resolutionScale != 1) {
      this.levelsTable.setRowHeight(Math.round(this.levelsTable.getRowHeight() * resolutionScale));
    }
    TableColumnModel columnModel = this.levelsTable.getColumnModel();
    columnModel.getColumn(1).setMaxWidth(Math.round(70 * resolutionScale));
    columnModel.getColumn(2).setMaxWidth(Math.round(70 * resolutionScale));
    DefaultCellEditor booleanEditor = new DefaultCellEditor(new JCheckBox());
    booleanEditor.setClickCountToStart(1);
    columnModel.getColumn(1).setCellEditor(booleanEditor);
    columnModel.getColumn(2).setCellEditor(booleanEditor);

    this.moveUpButton = new JButton(new ResourceAction(preferences, ManageLayersPanel.class, "MOVE_LAYER_UP") {
        @Override
        public void actionPerformed(ActionEvent ev) {
          moveSelectedLayer(1);
        }
      });
    this.moveDownButton = new JButton(new ResourceAction(preferences, ManageLayersPanel.class, "MOVE_LAYER_DOWN") {
        @Override
        public void actionPerformed(ActionEvent ev) {
          moveSelectedLayer(-1);
        }
      });
    this.modifyLayerButton = new JButton(new ResourceAction(preferences, ManageLayersPanel.class, "MODIFY_LAYER") {
        @Override
        public void actionPerformed(ActionEvent ev) {
          modifySelectedLayer();
        }
      });

    this.levelsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent ev) {
          if (!ev.getValueIsAdjusting()) {
            updateMoveButtonsEnabled();
            modifyLayerButton.setEnabled(getSelectedLevel() != null);
          }
        }
      });

    // Only sync plan selected level when the user picks a layer by name or keyboard,
    // not when toggling Viewable/Locked checkboxes (that would hide upper-layer content).
    this.levelsTable.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent ev) {
          int row = levelsTable.rowAtPoint(ev.getPoint());
          int column = levelsTable.columnAtPoint(ev.getPoint());
          if (row >= 0 && column == 0) {
            syncTableRowToHomeSelectedLevel(row);
          }
        }
      });
    this.levelsTable.addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent ev) {
          if (ev.getKeyCode() == KeyEvent.VK_UP || ev.getKeyCode() == KeyEvent.VK_DOWN) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                  int row = levelsTable.getSelectedRow();
                  if (row >= 0) {
                    syncTableRowToHomeSelectedLevel(row);
                  }
                }
              });
          }
        }
      });

    this.levelsTable.addAncestorListener(new AncestorListener() {
        public void ancestorAdded(AncestorEvent ev) {
          selectLevel(home.getSelectedLevel());
          levelsTable.revalidate();
          levelsTable.repaint();
          ev.getComponent().removeAncestorListener(this);
        }

        public void ancestorRemoved(AncestorEvent ev) {
        }

        public void ancestorMoved(AncestorEvent ev) {
        }
      });

    home.addPropertyChangeListener(Home.Property.SELECTED_LEVEL, selectedLevelListener);
    home.addLevelsListener(levelsListener);
    for (Level level : home.getLevels()) {
      level.addPropertyChangeListener(levelPropertyListener);
    }

    layoutComponents();
    selectLevel(home.getSelectedLevel());
    updateMoveButtonsEnabled();
    modifyLayerButton.setEnabled(getSelectedLevel() != null);
  }

  private final PropertyChangeListener selectedLevelListener = new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent ev) {
        syncHomeSelectionToTable();
      }
    };

  private final CollectionListener<Level> levelsListener = new CollectionListener<Level>() {
      public void collectionChanged(CollectionEvent<Level> ev) {
        if (ev.getType() == CollectionEvent.Type.ADD) {
          ev.getItem().addPropertyChangeListener(levelPropertyListener);
        } else if (ev.getType() == CollectionEvent.Type.DELETE) {
          ev.getItem().removePropertyChangeListener(levelPropertyListener);
        }
        tableModel.refresh();
        syncHomeSelectionToTable();
        updateMoveButtonsEnabled();
      }
    };

  private final PropertyChangeListener levelPropertyListener = new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent ev) {
        tableModel.refresh();
      }
    };

  private void layoutComponents() {
    float resolutionScale = SwingTools.getResolutionScale();
    int standardGap = Math.round(5 * resolutionScale);
    int rowHeight = this.levelsTable.getRowHeight();
    int viewportHeight = rowHeight * 8 + 1;
    int viewportWidth = Math.round(320 * resolutionScale);
    int headerHeight = Math.max(this.levelsTable.getTableHeader().getPreferredSize().height,
        Math.round(22 * resolutionScale));
    this.levelsTable.setPreferredScrollableViewportSize(
        new Dimension(viewportWidth, viewportHeight));
    this.levelsScrollPane = new JScrollPane(this.levelsTable);
    this.levelsScrollPane.setPreferredSize(new Dimension(
        viewportWidth + this.levelsScrollPane.getVerticalScrollBar().getPreferredSize().width,
        viewportHeight + headerHeight + 2));
    add(this.levelsScrollPane, BorderLayout.CENTER);

    JPanel moveButtonsPanel = new JPanel();
    moveButtonsPanel.setLayout(new BoxLayout(moveButtonsPanel, BoxLayout.PAGE_AXIS));
    moveButtonsPanel.add(this.moveUpButton);
    moveButtonsPanel.add(Box.createVerticalStrut(standardGap));
    moveButtonsPanel.add(this.moveDownButton);
    if (!OperatingSystem.isMacOSX()) {
      Dimension preferredSize = this.moveUpButton.getPreferredSize();
      preferredSize.width = preferredSize.height = preferredSize.height + 4;
      this.moveUpButton.setPreferredSize(preferredSize);
      this.moveDownButton.setPreferredSize(preferredSize);
    }
    add(moveButtonsPanel, BorderLayout.EAST);

    JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
    southPanel.add(this.modifyLayerButton);
    add(southPanel, BorderLayout.SOUTH);

    Dimension panelSize = new Dimension(Math.round(420 * resolutionScale),
        Math.round(280 * resolutionScale));
    setPreferredSize(panelSize);
    setMinimumSize(panelSize);
  }

  /**
   * Displays this panel in a modal dialog with a Close button.
   */
  public void displayView(View parentView) {
    try {
      String close = this.preferences.getLocalizedString(ManageLayersPanel.class, "close");
      JComponent parentComponent = SwingUtilities.getRootPane((JComponent)parentView);
      JOptionPane optionPane = new JOptionPane(this,
          JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION,
          null, new Object [] {close}, close);
      JDialog dialog = optionPane.createDialog(parentComponent, this.dialogTitle);
      dialog.applyComponentOrientation(parentComponent != null
          ? parentComponent.getComponentOrientation()
          : ComponentOrientation.getOrientation(Locale.getDefault()));
      dialog.pack();
      dialog.setMinimumSize(getPreferredSize());
      if (parentComponent != null) {
        dialog.setLocationRelativeTo(parentComponent);
      }
      dialog.addComponentListener(new ComponentAdapter() {
          @Override
          public void componentShown(ComponentEvent ev) {
            selectLevel(home.getSelectedLevel());
            levelsScrollPane.revalidate();
            levelsScrollPane.repaint();
            SwingTools.requestFocusInWindow(levelsTable);
            dialog.removeComponentListener(this);
          }
        });
      dialog.setVisible(true);
      dialog.dispose();
    } finally {
      detachListeners();
    }
  }

  private void detachListeners() {
    this.home.removePropertyChangeListener(Home.Property.SELECTED_LEVEL, this.selectedLevelListener);
    this.home.removeLevelsListener(this.levelsListener);
    for (Level level : this.home.getLevels()) {
      level.removePropertyChangeListener(this.levelPropertyListener);
    }
  }

  private Level getSelectedLevel() {
    int row = this.levelsTable.getSelectedRow();
    if (row < 0) {
      return null;
    }
    return this.tableModel.getLevelAt(row);
  }

  private void selectLevel(Level level) {
    if (level == null) {
      return;
    }
    int row = this.tableModel.getRowForLevel(level);
    if (row >= 0) {
      this.syncingSelection = true;
      try {
        this.levelsTable.setRowSelectionInterval(row, row);
        this.levelsTable.scrollRectToVisible(this.levelsTable.getCellRect(row, 0, true));
      } finally {
        this.syncingSelection = false;
      }
    }
  }

  private void syncTableRowToHomeSelectedLevel(int row) {
    if (this.syncingSelection) {
      return;
    }
    Level level = this.tableModel.getLevelAt(row);
    if (level != null && level != this.home.getSelectedLevel()) {
      this.planController.setSelectedLevel(level);
    }
  }

  private void syncHomeSelectionToTable() {
    if (this.syncingSelection) {
      return;
    }
    this.syncingSelection = true;
    try {
      selectLevel(this.home.getSelectedLevel());
    } finally {
      this.syncingSelection = false;
    }
  }

  private void moveSelectedLayer(int direction) {
    Level level = getSelectedLevel();
    if (level != null) {
      LevelController.moveLevelElevationIndex(this.home, this.preferences, this.undoSupport, level, direction);
      selectLevel(level);
      updateMoveButtonsEnabled();
    }
  }

  private void updateMoveButtonsEnabled() {
    Level level = getSelectedLevel();
    this.moveUpButton.setEnabled(level != null
        && LevelController.canMoveLevelElevationIndex(this.home, level, 1));
    this.moveDownButton.setEnabled(level != null
        && LevelController.canMoveLevelElevationIndex(this.home, level, -1));
  }

  private void modifySelectedLayer() {
    Level level = getSelectedLevel();
    if (level != null) {
      this.planController.setSelectedLevel(level);
      this.planController.modifySelectedLevel();
      this.tableModel.refresh();
      selectLevel(level);
      revalidate();
      repaint();
    }
  }

  /**
   * Table model listing levels top-to-bottom in draw order (top row = top of stack).
   */
  private class LevelsTableModel extends AbstractTableModel {
    private final String [] columnNames;

    private LevelsTableModel(String [] columnNames) {
      this.columnNames = columnNames;
    }

    public void refresh() {
      fireTableDataChanged();
    }

    public Level getLevelAt(int rowIndex) {
      List<Level> levels = ManageLayersPanel.this.home.getLevels();
      return levels.get(levels.size() - rowIndex - 1);
    }

    public int getRowForLevel(Level level) {
      List<Level> levels = ManageLayersPanel.this.home.getLevels();
      int index = levels.indexOf(level);
      if (index < 0) {
        return -1;
      }
      return levels.size() - index - 1;
    }

    public int getRowCount() {
      return ManageLayersPanel.this.home.getLevels().size();
    }

    public int getColumnCount() {
      return this.columnNames.length;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
      Level level = getLevelAt(rowIndex);
      switch (columnIndex) {
        case 0 : return level.getName();
        case 1 : return level.isViewable();
        case 2 : return level.isLocked();
        default : return null;
      }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex == 1 || columnIndex == 2;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
      if (!(value instanceof Boolean)) {
        return;
      }
      Level level = getLevelAt(rowIndex);
      if (columnIndex == 1) {
        ManageLayersPanel.this.planController.setLevelViewable(level, (Boolean)value);
      } else if (columnIndex == 2) {
        ManageLayersPanel.this.planController.setLevelLocked(level, (Boolean)value);
      }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      if (columnIndex == 1 || columnIndex == 2) {
        return Boolean.class;
      }
      return String.class;
    }

    @Override
    public String getColumnName(int column) {
      return this.columnNames [column];
    }
  }
}
