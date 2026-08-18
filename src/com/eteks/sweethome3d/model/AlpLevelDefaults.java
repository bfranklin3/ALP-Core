/*
 * AlpLevelDefaults.java
 *
 * Sweet Home 3D / ALP CAD — flat level defaults for landscape site plans (SPIKE-14).
 */
package com.eteks.sweethome3d.model;

/**
 * Helpers for ALP landscape-oriented level defaults on new homes.
 */
public final class AlpLevelDefaults {
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
