/*
 * ContextDeckPane.java
 *
 * ALP CAD — right-column context deck (SPIKE-29): selector + situational panels.
 */
package com.eteks.sweethome3d.swing;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.eteks.sweethome3d.model.AlpLevelDefaults;
import com.eteks.sweethome3d.model.AlpPlantUtils;
import com.eteks.sweethome3d.model.CollectionEvent;
import com.eteks.sweethome3d.model.CollectionListener;
import com.eteks.sweethome3d.model.DimensionLine;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Label;
import com.eteks.sweethome3d.model.Level;
import com.eteks.sweethome3d.model.Polyline;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.SelectionEvent;
import com.eteks.sweethome3d.model.SelectionListener;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.model.Wall;
import com.eteks.sweethome3d.viewcontroller.HomeController;

/**
 * Bottom pane of the right work column: segmented selector and situational panels.
 */
public class ContextDeckPane extends JPanel {
  /** Home visual property for vertical split divider (inspector vs context deck). */
  public static final String DIVIDER_LOCATION_PROPERTY =
      "com.eteks.sweethome3d.SweetHome3D.ContextDeckDividerLocation";
  /** Home visual property for last manually chosen context deck panel. */
  public static final String LAST_PANEL_PROPERTY =
      "com.eteks.sweethome3d.SweetHome3D.LastContextDeckPanel";

  /** Minimum height of the context deck when the vertical split is expanded. */
  public static final int MIN_CONTEXT_DECK_HEIGHT =
      Math.max(1, AlpInspectorStyles.scale(72));

  /** Panel identifiers for {@link CardLayout}. */
  public enum Panel {
    LAYERS("LAYERS"),
    SELECTION("SELECTION"),
    LAYER_ITEMS("LAYER_ITEMS"),
    PLANTS("PLANTS");

    private final String id;

    Panel(String id) {
      this.id = id;
    }

    public String getId() {
      return this.id;
    }

    public static Panel fromId(String id) {
      if (id != null) {
        for (Panel panel : values()) {
          if (panel.id.equals(id)) {
            return panel;
          }
        }
      }
      return LAYERS;
    }
  }

  private final Home              home;
  private final UserPreferences   preferences;
  private final HomeController    controller;
  private final CardLayout        cardLayout;
  private final JPanel            cardPanel;
  private final JList<Level>      layersList;
  private final DefaultListModel<Selectable> selectionListModel;
  private final JList<Selectable> selectionList;
  private final JToggleButton     layersButton;
  private final JToggleButton     selectionButton;
  private final JToggleButton     layerItemsButton;
  private final JToggleButton     plantsButton;
  private final PlantScheduleDeckPanel plantSchedulePanel;
  private Panel                   manualPanel;
  private Panel                   showingPanel;
  private final Set<Level>        plantsAutoShownLevels;

  public ContextDeckPane(Home home, UserPreferences preferences, HomeController controller) {
    super(new BorderLayout());
    this.home = home;
    this.preferences = preferences;
    this.controller = controller;
    AlpCatalogStyles.applyWorkspacePanel(this);
    setMinimumSize(new Dimension(0, MIN_CONTEXT_DECK_HEIGHT));

    this.manualPanel = Panel.fromId(home.getProperty(LAST_PANEL_PROPERTY));
    this.showingPanel = this.manualPanel;
    this.plantsAutoShownLevels = new HashSet<Level>();

    this.cardLayout = new CardLayout();
    this.cardPanel = new JPanel(this.cardLayout);
    AlpCatalogStyles.applyWorkspacePanel(this.cardPanel);

    this.layersList = createLayersList();
    JScrollPane layersScrollPane = wrapScrollPane(this.layersList);
    int layerRowHeight = this.layersList.getFixedCellHeight();
    layersScrollPane.setPreferredSize(new Dimension(0,
        layerRowHeight * 5 + AlpInspectorStyles.scale(4)));
    this.cardPanel.add(layersScrollPane, Panel.LAYERS.getId());

    this.selectionListModel = new DefaultListModel<Selectable>();
    this.selectionList = createSelectionList();
    this.cardPanel.add(wrapScrollPane(this.selectionList), Panel.SELECTION.getId());

    LayerItemsDeckPanel layerItemsPanel = new LayerItemsDeckPanel(home, preferences, controller);
    this.cardPanel.add(layerItemsPanel, Panel.LAYER_ITEMS.getId());

    this.plantSchedulePanel = new PlantScheduleDeckPanel(home, preferences);
    this.cardPanel.add(this.plantSchedulePanel, Panel.PLANTS.getId());

    JPanel selectorPanel = new JPanel(new GridLayout(2, 2,
        AlpInspectorStyles.scale(4), AlpInspectorStyles.scale(2)));
    AlpCatalogStyles.applyWorkspacePanel(selectorPanel);
    selectorPanel.setBorder(BorderFactory.createEmptyBorder(
        AlpInspectorStyles.scale(4), AlpInspectorStyles.scale(8),
        AlpInspectorStyles.scale(4), AlpInspectorStyles.scale(8)));

    this.layersButton = createSelectorButton(Panel.LAYERS);
    this.selectionButton = createSelectorButton(Panel.SELECTION);
    this.layerItemsButton = createSelectorButton(Panel.LAYER_ITEMS);
    this.plantsButton = createSelectorButton(Panel.PLANTS);

    ButtonGroup selectorGroup = new ButtonGroup();
    selectorGroup.add(this.layersButton);
    selectorGroup.add(this.selectionButton);
    selectorGroup.add(this.layerItemsButton);
    selectorGroup.add(this.plantsButton);

    selectorPanel.add(this.layersButton);
    selectorPanel.add(this.selectionButton);
    selectorPanel.add(this.layerItemsButton);
    selectorPanel.add(this.plantsButton);

    add(selectorPanel, BorderLayout.NORTH);
    add(this.cardPanel, BorderLayout.CENTER);

    home.addSelectionListener(new SelectionListener() {
        public void selectionChanged(SelectionEvent ev) {
          refreshSelectionList();
          applyAutoSwitch();
        }
      });
    home.addLevelsListener(new CollectionListener<Level>() {
        public void collectionChanged(CollectionEvent<Level> ev) {
          refreshLayersList();
        }
      });
    home.addPropertyChangeListener(Home.Property.SELECTED_LEVEL, ev -> {
        updateLayersListSelection();
        applyPlantsAutoSwitch();
      });
    home.addFurnitureListener(new CollectionListener<HomePieceOfFurniture>() {
        public void collectionChanged(CollectionEvent<HomePieceOfFurniture> ev) {
          updateSelectorEnabledState();
          applyPlantsAutoSwitch();
        }
      });

    refreshLayersList();
    refreshSelectionList();
    showPanel(resolveInitialPanel(), false);
    updateSelectorEnabledState();
  }

  private JScrollPane wrapScrollPane(JComponent view) {
    JScrollPane scrollPane = SwingTools.createScrollPane(view);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    AlpCatalogStyles.applyCatalogScrollPane(scrollPane);
    return scrollPane;
  }

  private JList<Level> createLayersList() {
    JList<Level> list = new JList<Level>();
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setCellRenderer(new LayerListCellRenderer());
    list.setFixedCellHeight(AlpInspectorStyles.scale(22));
    list.addListSelectionListener(new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent ev) {
          if (ev.getValueIsAdjusting()) {
            return;
          }
          Level level = list.getSelectedValue();
          if (level != null && level != home.getSelectedLevel()) {
            controller.getPlanController().setSelectedLevel(level);
          }
        }
      });
    return list;
  }

  private JList<Selectable> createSelectionList() {
    JList<Selectable> list = new JList<Selectable>(this.selectionListModel);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setCellRenderer(new SelectionListCellRenderer());
    list.setFixedCellHeight(AlpInspectorStyles.scale(22));
    list.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent ev) {
          if (ev.getClickCount() >= 1) {
            Selectable item = list.getSelectedValue();
            if (item != null) {
              controller.getPlanController().selectItem(item);
            }
          }
        }
      });
    return list;
  }

  private JToggleButton createSelectorButton(final Panel panel) {
    JToggleButton button = new JToggleButton(this.preferences.getLocalizedString(
        ContextDeckPane.class, panel.name().toLowerCase() + "Selector.text"));
    button.setFocusable(false);
    AlpCatalogStyles.applyWorkspacePanel(button);
    button.addActionListener(ev -> showPanel(panel, true));
    return button;
  }

  private Panel resolveInitialPanel() {
    int selectionCount = this.home.getSelectedItems().size();
    if (selectionCount >= 2) {
      return Panel.SELECTION;
    } else if (selectionCount == 0) {
      Level selectedLevel = this.home.getSelectedLevel();
      if (selectedLevel != null
          && AlpLevelDefaults.isPlantsLevel(selectedLevel, this.preferences)
          && AlpPlantUtils.countPlantsOnLevel(this.home, selectedLevel) > 0) {
        return Panel.PLANTS;
      }
      return Panel.LAYERS;
    }
    return this.manualPanel;
  }

  private void applyPlantsAutoSwitch() {
    if (this.home.getSelectedItems().size() >= 2) {
      return;
    }
    Level selectedLevel = this.home.getSelectedLevel();
    if (selectedLevel == null
        || !AlpLevelDefaults.isPlantsLevel(selectedLevel, this.preferences)
        || this.plantsAutoShownLevels.contains(selectedLevel)) {
      updateSelectorEnabledState();
      return;
    }
    if (AlpPlantUtils.countPlantsOnLevel(this.home, selectedLevel) > 0) {
      this.plantsAutoShownLevels.add(selectedLevel);
      showPanel(Panel.PLANTS, false);
    }
    updateSelectorEnabledState();
  }

  private void applyAutoSwitch() {
    int selectionCount = this.home.getSelectedItems().size();
    if (selectionCount >= 2) {
      showPanel(Panel.SELECTION, false);
    } else if (selectionCount == 0) {
      showPanel(Panel.LAYERS, false);
    }
    updateSelectorEnabledState();
  }

  private void showPanel(Panel panel, boolean manual) {
    if (panel == Panel.SELECTION && this.home.getSelectedItems().size() < 2) {
      panel = this.manualPanel != null ? this.manualPanel : Panel.LAYERS;
    }
    if (manual) {
      this.manualPanel = panel;
      this.controller.setHomeProperty(LAST_PANEL_PROPERTY, panel.getId());
    }
    this.showingPanel = panel;
    this.cardLayout.show(this.cardPanel, panel.getId());
    updateSelectorState();
    if (panel == Panel.LAYERS) {
      updateLayersListSelection();
    }
  }

  private void updateSelectorState() {
    this.layersButton.setSelected(this.showingPanel == Panel.LAYERS);
    this.selectionButton.setSelected(this.showingPanel == Panel.SELECTION);
    this.layerItemsButton.setSelected(this.showingPanel == Panel.LAYER_ITEMS);
    this.plantsButton.setSelected(this.showingPanel == Panel.PLANTS);
  }

  private void updateSelectorEnabledState() {
    this.selectionButton.setEnabled(this.home.getSelectedItems().size() >= 2
        || this.showingPanel == Panel.SELECTION);
    this.plantsButton.setEnabled(this.plantSchedulePanel.hasPlantsOnPlantsLayer()
        || this.showingPanel == Panel.PLANTS);
  }

  private void refreshLayersList() {
    List<Level> levels = new ArrayList<Level>(this.home.getLevels());
    Collections.reverse(levels);
    this.layersList.setListData(levels.toArray(new Level [levels.size()]));
    updateLayersListSelection();
  }

  private void updateLayersListSelection() {
    Level selectedLevel = this.home.getSelectedLevel();
    if (selectedLevel == null) {
      this.layersList.clearSelection();
    } else {
      this.layersList.setSelectedValue(selectedLevel, true);
    }
  }

  private void refreshSelectionList() {
    List<Selectable> selectedItems = this.home.getSelectedItems();
    this.selectionListModel.clear();
    for (Selectable item : selectedItems) {
      this.selectionListModel.addElement(item);
    }
    updateSelectorEnabledState();
  }

  private static String getSelectableLabel(Selectable item, UserPreferences preferences) {
    if (item instanceof HomePieceOfFurniture) {
      String name = ((HomePieceOfFurniture)item).getName();
      return name != null && name.trim().length() > 0 ? name.trim()
          : preferences.getLocalizedString(ContextDeckPane.class, "furnitureType.text");
    } else if (item instanceof Room) {
      String name = ((Room)item).getName();
      return name != null && name.trim().length() > 0 ? name.trim()
          : preferences.getLocalizedString(ContextDeckPane.class, "areaType.text");
    } else if (item instanceof Wall) {
      return preferences.getLocalizedString(ContextDeckPane.class, "wallType.text");
    } else if (item instanceof Label) {
      String text = ((Label)item).getText();
      return text != null && text.trim().length() > 0 ? text.trim()
          : preferences.getLocalizedString(ContextDeckPane.class, "labelType.text");
    } else if (item instanceof Polyline) {
      return preferences.getLocalizedString(ContextDeckPane.class, "polylineType.text");
    } else if (item instanceof DimensionLine) {
      return preferences.getLocalizedString(ContextDeckPane.class, "dimensionType.text");
    }
    return item.getClass().getSimpleName();
  }

  private static String getSelectableLayerName(Selectable item) {
    if (item instanceof com.eteks.sweethome3d.model.Elevatable) {
      Level level = ((com.eteks.sweethome3d.model.Elevatable)item).getLevel();
      if (level != null) {
        String name = level.getName();
        if (name != null && name.trim().length() > 0) {
          return name.trim();
        }
      }
    }
    return null;
  }

  private class LayerListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList list, Object value,
                                                  int index, boolean isSelected,
                                                  boolean cellHasFocus) {
      JLabel label = (JLabel)super.getListCellRendererComponent(
          list, value, index, isSelected, cellHasFocus);
      if (value instanceof Level) {
        Level level = (Level)value;
        String name = level.getName();
        if (name == null || name.trim().length() == 0) {
          name = "\u2014";
        }
        StringBuilder html = new StringBuilder("<html>");
        html.append(isSelected ? "<b>" : "");
        html.append(escapeHtml(name.trim()));
        html.append(isSelected ? "</b>" : "");
        if (!level.isViewable()) {
          html.append(" <font color='#888888'>");
          html.append(escapeHtml(preferences.getLocalizedString(
              SelectionInspectorPane.class, "layerListHidden.text")));
          html.append("</font>");
        }
        if (level.isLocked()) {
          html.append(" <font color='#888888'>");
          html.append(escapeHtml(preferences.getLocalizedString(
              SelectionInspectorPane.class, "layerListLocked.text")));
          html.append("</font>");
        }
        html.append("</html>");
        label.setText(html.toString());
      }
      return label;
    }
  }

  private class SelectionListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList list, Object value,
                                                  int index, boolean isSelected,
                                                  boolean cellHasFocus) {
      JLabel label = (JLabel)super.getListCellRendererComponent(
          list, value, index, isSelected, cellHasFocus);
      if (value instanceof Selectable) {
        Selectable item = (Selectable)value;
        String itemLabel = getSelectableLabel(item, preferences);
        String layerName = getSelectableLayerName(item);
        StringBuilder html = new StringBuilder("<html>");
        html.append(isSelected ? "<b>" : "");
        html.append(escapeHtml(itemLabel));
        html.append(isSelected ? "</b>" : "");
        if (layerName != null) {
          html.append(" <font color='#888888'>");
          html.append(escapeHtml(layerName));
          html.append("</font>");
        }
        html.append("</html>");
        label.setText(html.toString());
      }
      return label;
    }
  }

  private static String escapeHtml(String text) {
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
