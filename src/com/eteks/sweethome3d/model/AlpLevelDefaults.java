/*
 * AlpLevelDefaults.java
 *
 * Sweet Home 3D / ALP CAD — flat level defaults for landscape site plans (SPIKE-14, SPIKE-16).
 */
package com.eteks.sweethome3d.model;

/**
 * Helpers for ALP landscape-oriented level defaults on new homes.
 */
public final class AlpLevelDefaults {
  private static final String [] STARTER_LEVEL_NAME_KEYS = {
      "referenceLevelName",
      "existingLevelName",
      "proposedLevelName",
      "plantsLevelName",
      "annotationsLevelName",
  };

  /** Index of the level selected after creating the starter site plan. */
  private static final int STARTER_SELECTED_LEVEL_INDEX = 2;

  private AlpLevelDefaults() {
  }

  /**
   * Adds a single {@code Plan} level when a new home has no levels yet.
   */
  public static void addDefaultPlanLevel(Home home, UserPreferences preferences) {
    if (!home.getLevels().isEmpty()) {
      return;
    }
    Level plan = new Level(getDefaultLevelName(preferences),
        0, preferences.getNewFloorThickness(), preferences.getNewLevelHeight());
    home.addLevel(plan);
    home.setSelectedLevel(plan);
  }

  /**
   * Adds the five-level ALP site-plan template when a new home has no levels yet.
   * All levels share elevation 0 (flat overlays). {@code Reference} is locked.
   * {@code Proposed} is selected for drawing.
   */
  public static void addStarterSitePlanLevels(Home home, UserPreferences preferences) {
    if (!home.getLevels().isEmpty()) {
      return;
    }
    float floorThickness = preferences.getNewFloorThickness();
    float levelHeight = preferences.getNewLevelHeight();
    Level selectedLevel = null;
    for (int i = 0; i < STARTER_LEVEL_NAME_KEYS.length; i++) {
      String name = preferences.getLocalizedString(AlpLevelDefaults.class, STARTER_LEVEL_NAME_KEYS [i]);
      Level level = new Level(name, 0, floorThickness, levelHeight);
      if (i == 0) {
        level.setLocked(true);
      }
      home.addLevel(level);
      if (i == STARTER_SELECTED_LEVEL_INDEX) {
        selectedLevel = level;
      }
    }
    home.setSelectedLevel(selectedLevel);
  }

  /**
   * Returns the name for the first / base plan level.
   */
  public static String getDefaultLevelName(UserPreferences preferences) {
    return preferences.getLocalizedString(AlpLevelDefaults.class, "defaultLevelName");
  }

  /**
   * Returns the name for an overlay level ({@code layerNumber} is 2, 3, …).
   */
  public static String getLayerName(UserPreferences preferences, int layerNumber) {
    return preferences.getLocalizedString(AlpLevelDefaults.class, "layerName", layerNumber);
  }
}
