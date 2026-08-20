/*
 * MultipleLevelsPlanPanel.java 23 oct. 2011
 *
 * Sweet Home 3D, Copyright (c) 2024 Space Mushrooms <info@sweethome3d.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.eteks.sweethome3d.swing;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.eteks.sweethome3d.model.CollectionEvent;
import com.eteks.sweethome3d.model.CollectionListener;
import com.eteks.sweethome3d.model.DimensionLine;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.LengthUnit;
import com.eteks.sweethome3d.model.Level;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.TextStyle;
import com.eteks.sweethome3d.model.SelectionEvent;
import com.eteks.sweethome3d.model.SelectionListener;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.tools.OperatingSystem;
import com.eteks.sweethome3d.viewcontroller.HomeView;
import com.eteks.sweethome3d.viewcontroller.PlanController;
import com.eteks.sweethome3d.viewcontroller.PlanController.EditableProperty;
import com.eteks.sweethome3d.viewcontroller.PlanView;
import com.eteks.sweethome3d.viewcontroller.View;

/**
 * A panel for multiple levels plans where users can select the displayed level.
 * @author Emmanuel Puybaret
 */
public class MultipleLevelsPlanPanel extends JPanel implements PlanView, Printable {
  private static final String ONE_LEVEL_PANEL_NAME = "oneLevelPanel";
  private static final String MULTIPLE_LEVELS_PANEL_NAME = "multipleLevelsPanel";
  private static final int LEVEL_TAB_DRAG_THRESHOLD = 5;

  private static final ImageIcon sameElevationIcon = SwingTools.getScaledImageIcon(MultipleLevelsPlanPanel.class.getResource("resources/sameElevation.png"));

  private final Home    home;
  private JComponent  planComponent;
  private JScrollPane planScrollPane;
  private JPanel      planContentPanel;
  private JPanel      planChromePanel;
  private JComponent  drawStrip;
  private JComponent  textFormattingPanel;
  private UserPreferences preferences;
  private PlanController planController;
  private LevelTabbedPane multipleLevelsTabbedPane;
  private JPanel      multipleLevelsPanel;
  private JButton     addLayerButton;
  private JPanel      oneLevelPanel;
  private MouseAdapter levelTabMouseHandler;
  private int         levelTabDragSourceStackIndex = -1;
  private Point       levelTabDragStartPoint;
  private boolean     levelTabDragging;
  private boolean     levelTabDragOccurred;

  public MultipleLevelsPlanPanel(Home home,
                                 UserPreferences preferences,
                                 PlanController controller) {
    super(new CardLayout());
    this.home = home;
    createComponents(home, preferences, controller);
    layoutComponents();
    updateSelectedTab(home);
  }

  /**
   * Creates components displayed by this panel.
   */
  private void createComponents(final Home home,
                                final UserPreferences preferences, final PlanController controller) {
    this.preferences = preferences;
    this.planController = controller;
    this.planComponent = (JComponent)createPlanComponent(home, preferences, controller);

    UIManager.getDefaults().put("TabbedPane.contentBorderInsets", OperatingSystem.isMacOSX()
        ? new Insets(2, 2, 2, 2)
        : new Insets(-1, 0, 2, 2));
    this.multipleLevelsTabbedPane = new LevelTabbedPane();
    if (OperatingSystem.isMacOSX()) {
      this.multipleLevelsTabbedPane.setBorder(new EmptyBorder(-2, -6, -7, -6));
    }
    this.multipleLevelsPanel = new JPanel(new BorderLayout(0, 0));
    AlpCatalogStyles.applyLevelTabBar(this.multipleLevelsPanel);
    this.multipleLevelsPanel.add(this.multipleLevelsTabbedPane, BorderLayout.CENTER);
    this.addLayerButton = createAddLayerButton(preferences, controller);
    if (this.addLayerButton != null) {
      this.multipleLevelsPanel.add(this.addLayerButton, BorderLayout.EAST);
    }
    List<Level> levels = home.getLevels();
    this.planScrollPane = new JScrollPane(this.planComponent);
    this.planScrollPane.setMinimumSize(new Dimension());
    if (OperatingSystem.isMacOSX()) {
      this.planScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
      this.planScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    }
    this.planChromePanel = new JPanel(new BorderLayout(0, 0));
    AlpCatalogStyles.applyWorkspacePanel(this.planChromePanel);
    this.planContentPanel = new JPanel(new BorderLayout(0, 0));
    this.planContentPanel.add(this.planChromePanel, BorderLayout.NORTH);
    this.planContentPanel.add(this.planScrollPane, BorderLayout.CENTER);
    installRenameKeyBinding(home);

    final boolean addLayerControlAvailable = createTabs(home, preferences);
    final ChangeListener changeListener = new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          Component selectedComponent = multipleLevelsTabbedPane.getSelectedComponent();
          if (selectedComponent instanceof LevelLabel) {
            controller.setSelectedLevel(((LevelLabel)selectedComponent).getLevel());
          }
          refreshTabSelectionStates(home);
        }
      };
    this.multipleLevelsTabbedPane.addChangeListener(changeListener);
    // Add mouse listeners for tab selection, modify level, and drag-reorder
    this.levelTabMouseHandler = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent ev) {
          resetLevelTabDragState();
          int levelCount = home.getLevels().size();
          int indexAtLocation = multipleLevelsTabbedPane.indexAtLocation(ev.getX(), ev.getY());
          if (indexAtLocation >= 0 && indexAtLocation < levelCount) {
            levelTabDragSourceStackIndex = indexAtLocation;
            levelTabDragStartPoint = ev.getPoint();
          }
        }

        @Override
        public void mouseDragged(MouseEvent ev) {
          if (levelTabDragSourceStackIndex < 0 || levelTabDragStartPoint == null) {
            return;
          }
          if (!levelTabDragging) {
            if (levelTabDragStartPoint.distance(ev.getPoint()) < LEVEL_TAB_DRAG_THRESHOLD) {
              return;
            }
            levelTabDragging = true;
            levelTabDragOccurred = true;
            multipleLevelsTabbedPane.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            controller.setSelectedLevel(home.getLevels().get(levelTabDragSourceStackIndex));
          }
          int dropStackIndex = getLevelTabDropStackIndex(ev.getX(), ev.getY());
          if (dropStackIndex >= 0
              && controller.canReorderLevelToStackIndex(home.getLevels().get(levelTabDragSourceStackIndex), dropStackIndex)) {
            multipleLevelsTabbedPane.setInsertIndicatorStackIndex(dropStackIndex);
          } else {
            multipleLevelsTabbedPane.setInsertIndicatorStackIndex(null);
          }
        }

        @Override
        public void mouseReleased(MouseEvent ev) {
          if (levelTabDragging) {
            Level draggedLevel = home.getLevels().get(levelTabDragSourceStackIndex);
            int dropStackIndex = getLevelTabDropStackIndex(ev.getX(), ev.getY());
            if (controller.canReorderLevelToStackIndex(draggedLevel, dropStackIndex)) {
              controller.reorderLevelToStackIndex(draggedLevel, dropStackIndex);
            }
          }
          endLevelTabDrag();
        }

        @Override
        public void mouseClicked(MouseEvent ev) {
          if (levelTabDragOccurred) {
            levelTabDragOccurred = false;
            return;
          }
          int indexAtLocation = multipleLevelsTabbedPane.indexAtLocation(ev.getX(), ev.getY());
          if (ev.getClickCount() == 1) {
            final Level oldSelectedLevel = home.getSelectedLevel();
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                  if (oldSelectedLevel == home.getSelectedLevel()) {
                    planComponent.requestFocusInWindow();
                  }
                }
              });
          } else if (indexAtLocation != -1 && ev.getClickCount() >= 2) {
            Level level = home.getLevels().get(indexAtLocation);
            controller.setSelectedLevel(level);
            controller.modifySelectedLevel();
          }
        }
      };
    this.multipleLevelsTabbedPane.addMouseListener(this.levelTabMouseHandler);
    this.multipleLevelsTabbedPane.addMouseMotionListener(this.levelTabMouseHandler);

     // Add listeners to levels to maintain tabs name and order
    final PropertyChangeListener levelChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (Level.Property.NAME.name().equals(ev.getPropertyName())) {
            int index = home.getLevels().indexOf(ev.getSource());
            multipleLevelsTabbedPane.setTitleAt(index, (String)ev.getNewValue());
            updateTabComponent(home, index);
          } else if (Level.Property.VIEWABLE.name().equals(ev.getPropertyName())
              || Level.Property.LOCKED.name().equals(ev.getPropertyName())
              || Level.Property.CATEGORY.name().equals(ev.getPropertyName())) {
            updateTabComponent(home, home.getLevels().indexOf(ev.getSource()));
          } else if (Level.Property.ELEVATION.name().equals(ev.getPropertyName())
              || Level.Property.ELEVATION_INDEX.name().equals(ev.getPropertyName())) {
            multipleLevelsTabbedPane.removeChangeListener(changeListener);
            multipleLevelsTabbedPane.removeAll();
            createTabs(home, preferences);
            updateSelectedTab(home);
            multipleLevelsTabbedPane.addChangeListener(changeListener);
          }
        }
      };
    for (Level level : levels) {
      level.addPropertyChangeListener(levelChangeListener);
    }
    home.addLevelsListener(new CollectionListener<Level>() {
        public void collectionChanged(CollectionEvent<Level> ev) {
          multipleLevelsTabbedPane.removeChangeListener(changeListener);
          switch (ev.getType()) {
            case ADD:
              multipleLevelsTabbedPane.insertTab(ev.getItem().getName(), null, new LevelLabel(ev.getItem()), null, ev.getIndex());
              updateTabComponent(home, ev.getIndex());
              ev.getItem().addPropertyChangeListener(levelChangeListener);
              break;
            case DELETE:
              ev.getItem().removePropertyChangeListener(levelChangeListener);
              multipleLevelsTabbedPane.remove(ev.getIndex());
              break;
          }
          updateLayout(home);
          multipleLevelsTabbedPane.addChangeListener(changeListener);
        }
      });

    home.addPropertyChangeListener(Home.Property.SELECTED_LEVEL, new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent ev) {
        multipleLevelsTabbedPane.removeChangeListener(changeListener);
        updateSelectedTab(home);
        multipleLevelsTabbedPane.addChangeListener(changeListener);
      }
    });

    this.oneLevelPanel = new JPanel(new BorderLayout());

    if (OperatingSystem.isJavaVersionGreaterOrEqual("1.6")) {
      home.addPropertyChangeListener(Home.Property.ALL_LEVELS_SELECTION, new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            multipleLevelsTabbedPane.repaint();
            refreshTabComponents(home);
          }
        });
    }

    if (addLayerControlAvailable) {
      preferences.addPropertyChangeListener(UserPreferences.Property.LANGUAGE,
          new LanguageChangeListener(this));
    }
  }

  /**
   * Creates the + Layer button shown beside the tab strip.
   */
  private JButton createAddLayerButton(UserPreferences preferences, final PlanController controller) {
    try {
      preferences.getLocalizedString(MultipleLevelsPlanPanel.class, "ADD_LEVEL.SmallIcon");
    } catch (IllegalArgumentException ex) {
      return null;
    }
    JButton button = new JButton(SwingTools.getLocalizedLabelText(
        preferences, MultipleLevelsPlanPanel.class, "addLayerButton.text"));
    button.setToolTipText(preferences.getLocalizedString(
        MultipleLevelsPlanPanel.class, "ADD_LEVEL.ShortDescription"));
    AlpCatalogStyles.applyAddLayerButton(button);
    button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          controller.addLevel();
        }
      });
    return button;
  }

  /**
   * Creates and returns the main plan component displayed and layout by this component.
   */
  protected PlanView createPlanComponent(final Home home, final UserPreferences preferences,
                                              final PlanController controller) {
    return new PlanComponent(home, preferences, controller);
  }

  /**
   * Updates tab component with a label that will display tab text outlined by selection color
   * when all objects are selected at all levels.
   */
  private void updateTabComponent(final Home home, int i) {
    if (OperatingSystem.isJavaVersionGreaterOrEqual("1.6")) {
      List<Level> levels = home.getLevels();
      if (i < 0 || i >= levels.size()) {
        return;
      }
      Level level = levels.get(i);
      boolean selected = this.multipleLevelsTabbedPane.getSelectedIndex() == i;
      Component existingTabComponent = getTabComponentAt(i);
      AlpLevelTabComponent tabComponent;
      if (existingTabComponent instanceof AlpLevelTabComponent) {
        tabComponent = (AlpLevelTabComponent)existingTabComponent;
      } else {
        tabComponent = new AlpLevelTabComponent(home, this.planComponent, this.preferences, sameElevationIcon);
        setTabComponentAt(i, tabComponent);
      }
      tabComponent.updateLevel(level, levels, i, selected);
      tabComponent.installTabMouseHandler(this.multipleLevelsTabbedPane, this.levelTabMouseHandler);
    }
  }

  /**
   * Installs plan-adjacent draw strip and contextual text formatting (SPIKE-31 Phase 2).
   */
  public void setPlanChrome(JComponent drawStrip, JComponent textFormattingPanel) {
    this.drawStrip = drawStrip;
    this.textFormattingPanel = textFormattingPanel;
    rebuildPlanChrome();
  }

  /**
   * Starts inline rename for the selected level tab (F2 or context menu).
   */
  public void startInlineRenameForSelectedLevel(final Home home) {
    Level selectedLevel = home.getSelectedLevel();
    if (selectedLevel == null) {
      return;
    }
    int index = home.getLevels().indexOf(selectedLevel);
    if (index < 0) {
      return;
    }
    Component tabComponent = getTabComponentAt(index);
    if (!(tabComponent instanceof AlpLevelTabComponent)) {
      return;
    }
    final AlpLevelTabComponent levelTab = (AlpLevelTabComponent)tabComponent;
    if (levelTab.isInlineRenameActive()) {
      return;
    }
    levelTab.beginInlineRename(selectedLevel.getName(), new AlpLevelTabComponent.InlineRenameListener() {
        public void commitRename(String newName) {
          if (!newName.equals(selectedLevel.getName())) {
            selectedLevel.setName(newName);
          }
        }

        public void cancelRename() {
        }
      });
  }

  private void rebuildPlanChrome() {
    this.planChromePanel.removeAll();
    boolean hasChrome = false;
    if (this.drawStrip != null) {
      this.planChromePanel.add(this.drawStrip, BorderLayout.NORTH);
      hasChrome = true;
    }
    if (this.textFormattingPanel != null) {
      this.planChromePanel.add(this.textFormattingPanel, BorderLayout.SOUTH);
      hasChrome = this.textFormattingPanel.isVisible() || hasChrome;
    }
    this.planChromePanel.setVisible(hasChrome);
    this.planContentPanel.revalidate();
    this.planContentPanel.repaint();
  }

  /**
   * Updates visibility of the contextual text-formatting row.
   */
  public void updateTextFormattingVisibility(boolean visible) {
    if (this.textFormattingPanel != null) {
      this.textFormattingPanel.setVisible(visible);
      rebuildPlanChrome();
    }
  }

  private void installRenameKeyBinding(final Home home) {
    KeyStroke f2 = KeyStroke.getKeyStroke("F2");
    this.planComponent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(f2, "renameLayer");
    this.planComponent.getActionMap().put("renameLayer", new AbstractAction() {
        public void actionPerformed(ActionEvent ev) {
          startInlineRenameForSelectedLevel(home);
        }
      });
    this.multipleLevelsTabbedPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(f2, "renameLayerTabBar");
    this.multipleLevelsTabbedPane.getActionMap().put("renameLayerTabBar", new AbstractAction() {
        public void actionPerformed(ActionEvent ev) {
          startInlineRenameForSelectedLevel(home);
        }
      });
  }

  private void refreshTabSelectionStates(Home home) {
    int selectedIndex = this.multipleLevelsTabbedPane.getSelectedIndex();
    for (int i = 0; i < home.getLevels().size(); i++) {
      Component tabComponent = getTabComponentAt(i);
      if (tabComponent instanceof AlpLevelTabComponent) {
        ((AlpLevelTabComponent)tabComponent).setTabSelected(i == selectedIndex);
      }
    }
  }

  private void refreshTabComponents(Home home) {
    for (int i = 0; i < home.getLevels().size(); i++) {
      updateTabComponent(home, i);
    }
  }

  private Component getTabComponentAt(int index) {
    try {
      return (Component)this.multipleLevelsTabbedPane.getClass()
          .getMethod("getTabComponentAt", int.class)
          .invoke(this.multipleLevelsTabbedPane, index);
    } catch (InvocationTargetException ex) {
      throw new RuntimeException(ex);
    } catch (IllegalAccessException ex) {
      throw new IllegalAccessError(ex.getMessage());
    } catch (NoSuchMethodException ex) {
      throw new NoSuchMethodError(ex.getMessage());
    }
  }

  private void setTabComponentAt(int index, Component tabComponent) {
    try {
      this.multipleLevelsTabbedPane.getClass()
          .getMethod("setTabComponentAt", int.class, Component.class)
          .invoke(this.multipleLevelsTabbedPane, index, tabComponent);
    } catch (InvocationTargetException ex) {
      throw new RuntimeException(ex);
    } catch (IllegalAccessException ex) {
      throw new IllegalAccessError(ex.getMessage());
    } catch (NoSuchMethodException ex) {
      throw new NoSuchMethodError(ex.getMessage());
    }
  }

  /**
   * Preferences property listener bound to this component with a weak reference to avoid
   * strong link between preferences and this component.
   */
  private static class LanguageChangeListener implements PropertyChangeListener {
    private WeakReference<MultipleLevelsPlanPanel> planPanel;

    public LanguageChangeListener(MultipleLevelsPlanPanel planPanel) {
      this.planPanel = new WeakReference<MultipleLevelsPlanPanel>(planPanel);
    }

    public void propertyChange(PropertyChangeEvent ev) {
      // If help pane was garbage collected, remove this listener from preferences
      MultipleLevelsPlanPanel planPanel = this.planPanel.get();
      UserPreferences preferences = (UserPreferences)ev.getSource();
      if (planPanel == null) {
        preferences.removePropertyChangeListener(UserPreferences.Property.LANGUAGE, this);
      } else {
        if (planPanel.addLayerButton != null) {
          planPanel.addLayerButton.setText(SwingTools.getLocalizedLabelText(
              preferences, MultipleLevelsPlanPanel.class, "addLayerButton.text"));
          planPanel.addLayerButton.setToolTipText(preferences.getLocalizedString(
              MultipleLevelsPlanPanel.class, "ADD_LEVEL.ShortDescription"));
        }
      }
    }
  }

  /**
   * Creates the tabs from <code>home</code> levels, and returns <code>true</code>
   * if the + Layer control is available.
   */
  private boolean createTabs(Home home, UserPreferences preferences) {
    List<Level> levels = home.getLevels();
    for (int i = 0; i < levels.size(); i++) {
      Level level = levels.get(i);
      this.multipleLevelsTabbedPane.addTab(level.getName(), new LevelLabel(level));
      updateTabComponent(home, i);
    }
    try {
      preferences.getLocalizedString(MultipleLevelsPlanPanel.class, "ADD_LEVEL.SmallIcon");
      return this.addLayerButton != null;
    } catch (IllegalArgumentException ex) {
      if (this.addLayerButton != null) {
        this.addLayerButton.setVisible(false);
      }
      return false;
    }
  }

  /**
   * Selects the tab matching the selected level in <code>home</code>.
   */
  private void updateSelectedTab(Home home) {
    List<Level> levels = home.getLevels();
    Level selectedLevel = home.getSelectedLevel();
    if (levels.size() >= 2 && selectedLevel != null) {
      this.multipleLevelsTabbedPane.setSelectedIndex(levels.indexOf(selectedLevel));
      displayPlanComponentAtSelectedIndex(home);
      refreshTabSelectionStates(home);
    }
    updateLayout(home);
  }

  /**
   * Display the plan component at the selected tab index.
   */
  private void displayPlanComponentAtSelectedIndex(Home home) {
    int planIndex = this.multipleLevelsTabbedPane.indexOfComponent(this.planContentPanel);
    if (planIndex != -1) {
      // Replace plan component by a dummy label to avoid losing tab
      this.multipleLevelsTabbedPane.setComponentAt(planIndex, new LevelLabel(home.getLevels().get(planIndex)));
    }
    this.multipleLevelsTabbedPane.setComponentAt(this.multipleLevelsTabbedPane.getSelectedIndex(), this.planContentPanel);
  }

  /**
   * Switches between a simple plan component view and a tabbed pane for multiple levels.
   */
  private void updateLayout(Home home) {
    CardLayout layout = (CardLayout)getLayout();
    List<Level> levels = home.getLevels();
    boolean focus = this.planComponent.hasFocus();
    if (levels.size() < 2 || home.getSelectedLevel() == null) {
      int planIndex = this.multipleLevelsTabbedPane.indexOfComponent(this.planContentPanel);
      if (planIndex != -1) {
        // Replace plan component by a dummy label to avoid losing tab
        this.multipleLevelsTabbedPane.setComponentAt(planIndex, new LevelLabel(home.getLevels().get(planIndex)));
      }
      this.oneLevelPanel.removeAll();
      this.oneLevelPanel.add(this.planContentPanel, BorderLayout.CENTER);
      layout.show(this, ONE_LEVEL_PANEL_NAME);
    } else {
      layout.show(this, MULTIPLE_LEVELS_PANEL_NAME);
    }
    if (focus) {
      this.planComponent.requestFocusInWindow();
    }
  }

  /**
   * Layouts the components displayed by this panel.
   */
  private void layoutComponents() {
    add(this.multipleLevelsPanel, MULTIPLE_LEVELS_PANEL_NAME);
    add(this.oneLevelPanel, ONE_LEVEL_PANEL_NAME);

    SwingTools.installFocusBorder(this.planComponent);
    setFocusTraversalPolicyProvider(false);
    setMinimumSize(new Dimension());
  }

  @Override
  public void setTransferHandler(TransferHandler newHandler) {
    this.planComponent.setTransferHandler(newHandler);
  }

  @Override
  public void setComponentPopupMenu(final JPopupMenu popup) {
    JPopupMenu planComponentPopup = new JPopupMenu();
    JPopupMenu tabbedPanePopup = new JPopupMenu();
    // Split popup menu
    for (Component component : popup.getComponents()) {
      if (component instanceof JMenuItem
          && (HomeView.ActionType.ADD_LEVEL.name().equals(((JMenuItem)component).getAction().getValue(ResourceAction.RESOURCE_PREFIX))
              || HomeView.ActionType.ADD_LEVEL_AT_SAME_ELEVATION.name().equals(((JMenuItem)component).getAction().getValue(ResourceAction.RESOURCE_PREFIX)))) {
        tabbedPanePopup.add(component);
        // Add also menu item to plan component and enable it only when there's no level in home
        final ResourceAction.PopupMenuItemAction menuItemAction = new ResourceAction.PopupMenuItemAction(((JMenuItem)component).getAction());
        planComponentPopup.add(menuItemAction);
        final JMenuItem planMenuItem = (JMenuItem)planComponentPopup.getComponent(planComponentPopup.getComponentCount() - 1);
        planMenuItem.setEnabled(this.multipleLevelsTabbedPane.getTabCount() <= 1 && menuItemAction.isEnabled());
        this.multipleLevelsTabbedPane.addPropertyChangeListener("indexForTabComponent",
            new PropertyChangeListener() {
              public void propertyChange(PropertyChangeEvent ev) {
                // Change visibility later once tabs are fully updated
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                      planMenuItem.setEnabled(multipleLevelsTabbedPane.getTabCount() <= 1 && menuItemAction.isEnabled());
                    }
                  });
              }
            });
      } else if (component instanceof JMenuItem
                 && (HomeView.ActionType.MAKE_LEVEL_UNVIEWABLE.name().equals(((JMenuItem)component).getAction().getValue(ResourceAction.RESOURCE_PREFIX))
                     || HomeView.ActionType.MAKE_LEVEL_VIEWABLE.name().equals(((JMenuItem)component).getAction().getValue(ResourceAction.RESOURCE_PREFIX))
                     || HomeView.ActionType.MAKE_LEVEL_ONLY_VIEWABLE_ONE.name().equals(((JMenuItem)component).getAction().getValue(ResourceAction.RESOURCE_PREFIX))
                     || HomeView.ActionType.MAKE_ALL_LEVELS_VIEWABLE.name().equals(((JMenuItem)component).getAction().getValue(ResourceAction.RESOURCE_PREFIX))
                     || HomeView.ActionType.MODIFY_LEVEL.name().equals(((JMenuItem)component).getAction().getValue(ResourceAction.RESOURCE_PREFIX))
                     || HomeView.ActionType.MOVE_LAYER_UP.name().equals(((JMenuItem)component).getAction().getValue(ResourceAction.RESOURCE_PREFIX))
                     || HomeView.ActionType.MOVE_LAYER_DOWN.name().equals(((JMenuItem)component).getAction().getValue(ResourceAction.RESOURCE_PREFIX))
                     || HomeView.ActionType.DELETE_LEVEL.name().equals(((JMenuItem)component).getAction().getValue(ResourceAction.RESOURCE_PREFIX)))) {
        tabbedPanePopup.add(component);
      } else {
        planComponentPopup.add(component);
      }
    }

    // Listener that will dispatch events to the listeners added to the popup in parameter
    PopupMenuListener popupMenuListener = new PopupMenuListener() {
        public void popupMenuWillBecomeVisible(PopupMenuEvent ev) {
          for (PopupMenuListener l : popup.getPopupMenuListeners()) {
            l.popupMenuWillBecomeVisible(ev);
          }
        }

        public void popupMenuWillBecomeInvisible(PopupMenuEvent ev) {
          for (PopupMenuListener l : popup.getPopupMenuListeners()) {
            l.popupMenuWillBecomeInvisible(ev);
          }
        }

        public void popupMenuCanceled(PopupMenuEvent ev) {
          for (PopupMenuListener l : popup.getPopupMenuListeners()) {
            l.popupMenuCanceled(ev);
          }
        }
      };
    if (tabbedPanePopup.getComponentCount() > 0) {
      JMenuItem renameLayerMenuItem = new JMenuItem(
          SwingTools.getLocalizedLabelText(this.preferences, MultipleLevelsPlanPanel.class, "renameLayer.text"));
      renameLayerMenuItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            startInlineRenameForSelectedLevel(MultipleLevelsPlanPanel.this.home);
          }
        });
      tabbedPanePopup.insert(renameLayerMenuItem, 0);
      tabbedPanePopup.insert(new JPopupMenu.Separator(), 1);
      this.multipleLevelsTabbedPane.setComponentPopupMenu(tabbedPanePopup);
      SwingTools.hideDisabledMenuItems(tabbedPanePopup);
      tabbedPanePopup.addPopupMenuListener(popupMenuListener);
    }
    this.planComponent.setComponentPopupMenu(planComponentPopup);
    SwingTools.hideDisabledMenuItems(planComponentPopup);
    planComponentPopup.addPopupMenuListener(popupMenuListener);
  }

  @Override
  public void addMouseMotionListener(final MouseMotionListener l) {
    this.planComponent.addMouseMotionListener(new MouseMotionListener() {
        public void mouseMoved(MouseEvent ev) {
          l.mouseMoved(SwingUtilities.convertMouseEvent(planComponent, ev, MultipleLevelsPlanPanel.this));
        }

        public void mouseDragged(MouseEvent ev) {
          l.mouseDragged(SwingUtilities.convertMouseEvent(planComponent, ev, MultipleLevelsPlanPanel.this));
        }
      });
  }

  @Override
  public void addMouseListener(final MouseListener l) {
    this.planComponent.addMouseListener(new MouseListener() {
        public void mouseReleased(MouseEvent ev) {
          l.mouseReleased(SwingUtilities.convertMouseEvent(planComponent, ev, MultipleLevelsPlanPanel.this));
        }

        public void mousePressed(MouseEvent ev) {
          l.mousePressed(SwingUtilities.convertMouseEvent(planComponent, ev, MultipleLevelsPlanPanel.this));
        }

        public void mouseExited(MouseEvent ev) {
          l.mouseExited(SwingUtilities.convertMouseEvent(planComponent, ev, MultipleLevelsPlanPanel.this));
        }

        public void mouseEntered(MouseEvent ev) {
          l.mouseEntered(SwingUtilities.convertMouseEvent(planComponent, ev, MultipleLevelsPlanPanel.this));
        }

        public void mouseClicked(MouseEvent ev) {
          l.mouseClicked(SwingUtilities.convertMouseEvent(planComponent, ev, MultipleLevelsPlanPanel.this));
        }
      });
  }

  @Override
  public void addFocusListener(final FocusListener l) {
    FocusListener componentFocusListener = new FocusListener() {
        public void focusGained(FocusEvent ev) {
          l.focusGained(new FocusEvent(MultipleLevelsPlanPanel.this, FocusEvent.FOCUS_GAINED, ev.isTemporary(), ev.getOppositeComponent()));
        }

        public void focusLost(FocusEvent ev) {
          l.focusLost(new FocusEvent(MultipleLevelsPlanPanel.this, FocusEvent.FOCUS_LOST, ev.isTemporary(), ev.getOppositeComponent()));
        }
      };
    this.planComponent.addFocusListener(componentFocusListener);
    this.multipleLevelsTabbedPane.addFocusListener(componentFocusListener);
  }

  /**
   * Returns an image of the plan for transfer purpose.
   */
  public Object createTransferData(DataType dataType) {
    return ((PlanView)this.planComponent).createTransferData(dataType);
  }

  /**
   * Returns <code>true</code> if the plan component supports the given format type.
   */
  public boolean isFormatTypeSupported(FormatType formatType) {
    return ((PlanView)this.planComponent).isFormatTypeSupported(formatType);
  }

  /**
   * Writes the plan in the given output stream at SVG (Scalable Vector Graphics) format if this is the requested format.
   */
  public void exportData(OutputStream out, FormatType formatType, Properties settings) throws IOException {
    ((PlanView)this.planComponent).exportData(out, formatType, settings);
  }

  /**
   * Sets rectangle selection feedback coordinates.
   */
  public void setRectangleFeedback(float x0, float y0, float x1, float y1) {
    ((PlanView)this.planComponent).setRectangleFeedback(x0, y0, x1, y1);
  }

  /**
   * Ensures selected items are visible in the plan displayed by this component and moves
   * its scroll bars if needed.
   */
  public void makeSelectionVisible() {
    ((PlanView)this.planComponent).makeSelectionVisible();
  }

  /**
   * Ensures the point at (<code>x</code>, <code>y</code>) is visible in the plan displayed by this component,
   * moving its scroll bars if needed.
   */
  public void makePointVisible(float x, float y) {
    ((PlanView)this.planComponent).makePointVisible(x, y);
  }

  /**
   * Returns the scale used to display the plan displayed by this component.
   */
  public float getScale() {
    return ((PlanView)this.planComponent).getScale();
  }

  /**
   * Sets the scale used to display the plan displayed by this component.
   */
  public void setScale(float scale) {
    ((PlanView)this.planComponent).setScale(scale);
  }

  /**
   * Moves the plan displayed by this component from (dx, dy) unit in the scrolling zone it belongs to.
   */
  public void moveView(float dx, float dy) {
    ((PlanView)this.planComponent).moveView(dx, dy);
  }

  /**
   * Returns <code>x</code> converted in model coordinates space.
   */
  public float convertXPixelToModel(int x) {
    return ((PlanView)this.planComponent).convertXPixelToModel(SwingUtilities.convertPoint(this, x, 0, this.planComponent).x);
  }

  /**
   * Returns <code>y</code> converted in model coordinates space.
   */
  public float convertYPixelToModel(int y) {
    return ((PlanView)this.planComponent).convertYPixelToModel(SwingUtilities.convertPoint(this, 0, y, this.planComponent).y);
  }

  /**
   * Returns <code>x</code> converted in screen coordinates space.
   */
  public int convertXModelToScreen(float x) {
    return ((PlanView)this.planComponent).convertXModelToScreen(x);
  }

  /**
   * Returns <code>y</code> converted in screen coordinates space.
   */
  public int convertYModelToScreen(float y) {
    return ((PlanView)this.planComponent).convertYModelToScreen(y);
  }

  /**
   * Returns the length in centimeters of a pixel with the current scale.
   */
  public float getPixelLength() {
    return ((PlanView)this.planComponent).getPixelLength();
  }

  /**
   * Returns the coordinates of the bounding rectangle of the <code>text</code> displayed at
   * the point (<code>x</code>,<code>y</code>).
   */
  public float [][] getTextBounds(String text, TextStyle style, float x, float y, float angle) {
    return ((PlanView)this.planComponent).getTextBounds(text, style, x, y, angle);
  }

  /**
   * Returns the coordinates of the bounding rectangle of the <code>text</code> displayed at
   * the point (<code>x</code>,<code>y</code>).
   */
  public float [][] getTextBounds(String text, TextStyle style, float x, float y, float angle, Float maxWidth) {
    return ((PlanView)this.planComponent).getTextBounds(text, style, x, y, angle, maxWidth);
  }

  /**
   * Sets the cursor of this component.
   */
  public void setCursor(CursorType cursorType) {
    ((PlanView)this.planComponent).setCursor(cursorType);
  }

  /**
   * Sets the cursor of this component.
   */
  @Override
  public void setCursor(Cursor cursor) {
    this.planComponent.setCursor(cursor);
  }

  /**
   * Returns the cursor of this component.
   */
  @Override
  public Cursor getCursor() {
    return this.planComponent.getCursor();
  }

  /**
   * Sets tool tip text displayed as feedback.
   */
  public void setToolTipFeedback(String toolTipFeedback, float x, float y) {
    ((PlanView)this.planComponent).setToolTipFeedback(toolTipFeedback, x, y);
  }

  /**
   * Sets properties edited in tool tip.
   */
  public void setToolTipEditedProperties(EditableProperty [] toolTipEditedProperties, Object [] toolTipPropertyValues,
                                         float x, float y) {
    ((PlanView)this.planComponent).setToolTipEditedProperties(toolTipEditedProperties, toolTipPropertyValues, x, y);
  }

  /**
   * Sets the value of a property edited in tool tip.
   */
  public void setToolTipEditedPropertyValue(PlanController.EditableProperty toolTipEditedProperty,
                                           Object toolTipPropertyValue) {
    ((PlanView)this.planComponent).setToolTipEditedPropertyValue(toolTipEditedProperty, toolTipPropertyValue);
  }

  /**
   * Deletes tool tip text from screen.
   */
  public void deleteToolTipFeedback() {
    ((PlanView)this.planComponent).deleteToolTipFeedback();
  }

  /**
   * Sets whether the resize indicator of selected wall or piece of furniture
   * should be visible or not.
   */
  public void setResizeIndicatorVisible(boolean visible) {
    ((PlanView)this.planComponent).setResizeIndicatorVisible(visible);
  }

  /**
   * Sets the location point for alignment feedback.
   */
  public void setAlignmentFeedback(Class<? extends Selectable> alignedObjectClass, Selectable alignedObject, float x,
                                   float y, boolean showPoint) {
    ((PlanView)this.planComponent).setAlignmentFeedback(alignedObjectClass, alignedObject, x, y, showPoint);
  }

  /**
   * Sets the points used to draw an angle in the plan displayed by this component.
   */
  public void setAngleFeedback(float xCenter, float yCenter, float x1, float y1, float x2, float y2) {
    ((PlanView)this.planComponent).setAngleFeedback(xCenter, yCenter, x1, y1, x2, y2);
  }

  /**
   * Sets the feedback of dragged items drawn during a drag and drop operation,
   * initiated from outside of the plan displayed by this component.
   */
  public void setDraggedItemsFeedback(List<Selectable> draggedItems) {
    ((PlanView)this.planComponent).setDraggedItemsFeedback(draggedItems);
  }

  /**
   * Sets the given dimension lines to be drawn as feedback.
   */
  public void setDimensionLinesFeedback(List<DimensionLine> dimensionLines) {
    ((PlanView)this.planComponent).setDimensionLinesFeedback(dimensionLines);
  }

  /**
   * Deletes all elements shown as feedback.
   */
  public void deleteFeedback() {
    ((PlanView)this.planComponent).deleteFeedback();
  }

  /**
   * Returns <code>true</code> if the given coordinates belong to the plan displayed by this component.
   */
  public boolean canImportDraggedItems(List<Selectable> items, int x, int y) {
    JViewport viewport = this.planScrollPane.getViewport();
    Point point = SwingUtilities.convertPoint(this, x, y, viewport);
    return viewport.contains(point);
  }

  /**
   * Returns the size of the given piece of furniture in the horizontal plan.
   */
  public float [] getPieceOfFurnitureSizeInPlan(HomePieceOfFurniture piece) {
    return ((PlanView)this.planComponent).getPieceOfFurnitureSizeInPlan(piece);
  }

  /**
   * Returns <code>true</code> if this component is able to compute the size of horizontally rotated furniture.
   */
  public boolean isFurnitureSizeInPlanSupported() {
    return ((PlanView)this.planComponent).isFurnitureSizeInPlanSupported();
  }

  /**
   * Returns the component used as an horizontal ruler for the plan displayed by this component.
   */
  public View getHorizontalRuler() {
    return ((PlanView)this.planComponent).getHorizontalRuler();
  }

  /**
   * Returns the component used as a vertical ruler for the plan displayed by this component.
   */
  public View getVerticalRuler() {
    return ((PlanView)this.planComponent).getVerticalRuler();
  }

  /**
   * Prints the plan component.
   */
  public int print(Graphics g, PageFormat pageFormat, int pageIndex) throws PrinterException {
    if (this.planComponent instanceof Printable) {
      return ((Printable)this.planComponent).print(g, pageFormat, pageIndex);
    } else {
      throw new IllegalStateException("Embeded plan view not printable");
    }
  }

  /**
   * Returns the preferred scale to print the plan component.
   */
  public float getPrintPreferredScale(Graphics graphics, PageFormat pageFormat) {
    return getPrintPreferredScale(LengthUnit.inchToCentimeter((float)pageFormat.getImageableWidth() / 72),
        LengthUnit.inchToCentimeter((float)pageFormat.getImageableHeight() / 72));
  }

  /**
   * Returns the preferred scale to ensure it can be fully printed on the given print zone.
   */
  public float getPrintPreferredScale(float preferredWidth, float preferredHeight) {
    return ((PlanView)this.planComponent).getPrintPreferredScale(preferredWidth, preferredHeight);
  }

  private void resetLevelTabDragState() {
    this.levelTabDragSourceStackIndex = -1;
    this.levelTabDragStartPoint = null;
    this.levelTabDragging = false;
    this.levelTabDragOccurred = false;
  }

  private void endLevelTabDrag() {
    this.levelTabDragging = false;
    this.levelTabDragSourceStackIndex = -1;
    this.levelTabDragStartPoint = null;
    this.multipleLevelsTabbedPane.setInsertIndicatorStackIndex(null);
    this.multipleLevelsTabbedPane.setCursor(Cursor.getDefaultCursor());
  }

  /**
   * Returns the stack index where a dragged level tab would be inserted, or {@code -1} if invalid.
   */
  private int getLevelTabDropStackIndex(int x, int y) {
    int levelCount = this.multipleLevelsTabbedPane.getLevelTabCount();
    if (levelCount <= 1) {
      return -1;
    }
    for (int i = 0; i < levelCount; i++) {
      Rectangle bounds = this.multipleLevelsTabbedPane.getBoundsAt(i);
      if (bounds != null && bounds.contains(x, y)) {
        if (x < bounds.x + bounds.width / 2) {
          return i;
        } else {
          return Math.min(i + 1, levelCount - 1);
        }
      }
    }
    for (int i = 0; i < levelCount; i++) {
      Rectangle bounds = this.multipleLevelsTabbedPane.getBoundsAt(i);
      if (bounds != null && x < bounds.x + bounds.width / 2) {
        return i;
      }
    }
    return levelCount - 1;
  }

  /**
   * Tabbed pane subclass that paints a drop insert indicator during level tab drag-reorder.
   */
  private class LevelTabbedPane extends JTabbedPane {
    private Integer insertIndicatorStackIndex;

    public LevelTabbedPane() {
      setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    public int getLevelTabCount() {
      return getTabCount();
    }

    public void setInsertIndicatorStackIndex(Integer insertIndicatorStackIndex) {
      if (this.insertIndicatorStackIndex != insertIndicatorStackIndex
          && (this.insertIndicatorStackIndex == null
              || !this.insertIndicatorStackIndex.equals(insertIndicatorStackIndex))) {
        this.insertIndicatorStackIndex = insertIndicatorStackIndex;
        repaint();
      }
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (this.insertIndicatorStackIndex != null) {
        int indicatorX = getInsertIndicatorX(this.insertIndicatorStackIndex);
        Rectangle tabBounds = getInsertIndicatorTabAreaBounds();
        if (indicatorX >= 0 && tabBounds != null) {
          Graphics2D g2D = (Graphics2D)g;
          g2D.setColor(getForeground());
          g2D.setStroke(new BasicStroke(2f));
          g2D.drawLine(indicatorX, tabBounds.y + 1, indicatorX, tabBounds.y + tabBounds.height - 1);
        }
      }
    }

    private Rectangle getInsertIndicatorTabAreaBounds() {
      for (int i = 0, levelCount = getLevelTabCount(); i < levelCount; i++) {
        Rectangle bounds = getBoundsAt(i);
        if (bounds != null) {
          return bounds;
        }
      }
      return null;
    }

    private int getInsertIndicatorX(int insertStackIndex) {
      int levelCount = getLevelTabCount();
      if (insertStackIndex <= 0) {
        Rectangle bounds = getBoundsAt(0);
        return bounds != null ? bounds.x : -1;
      }
      if (insertStackIndex >= levelCount) {
        Rectangle bounds = getBoundsAt(levelCount - 1);
        return bounds != null ? bounds.x + bounds.width : -1;
      }
      Rectangle previousTabBounds = getBoundsAt(insertStackIndex - 1);
      Rectangle nextTabBounds = getBoundsAt(insertStackIndex);
      if (previousTabBounds != null && nextTabBounds != null) {
        return (previousTabBounds.x + previousTabBounds.width + nextTabBounds.x) / 2;
      }
      return -1;
    }
  }

  /**
   * A dummy label used to track tabs matching levels.
   */
  private static class LevelLabel extends JLabel {
    private final Level level;

    public LevelLabel(Level level) {
      this.level = level;

    }

    public Level getLevel() {
      return this.level;
    }
  }
}
