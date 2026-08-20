/*
 * AlpPlantUtils.java
 *
 * ALP CAD — plant identification and schedule aggregation (SPIKE-29b, SPIKE-30).
 */
package com.eteks.sweethome3d.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helpers for landscape plant furniture and schedule takeoff.
 */
public final class AlpPlantUtils {
  /** Catalog ID prefix for ALP plant library entries. */
  public static final String PLANT_CATALOG_ID_PREFIX = "alp-plt-";

  private AlpPlantUtils() {
  }

  /**
   * Returns whether a placed furniture piece is an ALP catalog plant.
   */
  public static boolean isPlant(HomePieceOfFurniture piece) {
    if (piece == null) {
      return false;
    }
    String catalogId = piece.getCatalogId();
    return catalogId != null && catalogId.startsWith(PLANT_CATALOG_ID_PREFIX);
  }

  /**
   * Returns whether the level participates in proposed plant takeoff.
   */
  public static boolean isProposedPlantingLevel(Level level) {
    return level != null
        && level.getCategory() == LevelCategory.PLANTING
        && level.getPlantTakeoff() == LevelPlantTakeoff.PROPOSED;
  }

  /**
   * Returns whether the level participates in existing plant takeoff.
   */
  public static boolean isExistingPlantingLevel(Level level) {
    return level != null
        && level.getCategory() == LevelCategory.PLANTING
        && level.getPlantTakeoff() == LevelPlantTakeoff.EXISTING;
  }

  /**
   * Returns whether the level is a planting layer with any takeoff mode enabled.
   */
  public static boolean isTakeoffPlantingLevel(Level level) {
    return isProposedPlantingLevel(level) || isExistingPlantingLevel(level);
  }

  /**
   * Returns the number of plant pieces on the given level.
   */
  public static int countPlantsOnLevel(Home home, Level level) {
    if (home == null || level == null) {
      return 0;
    }
    int count = 0;
    for (HomePieceOfFurniture piece : home.getFurniture()) {
      if (isPlant(piece) && piece.getLevel() == level) {
        count++;
      }
    }
    return count;
  }

  /**
   * Returns the number of plant pieces on proposed planting layers.
   */
  public static int countPlantsOnProposedLevels(Home home) {
    return countPlantsOnLevels(home, getProposedPlantingLevels(home));
  }

  /**
   * Returns the number of plant pieces on existing planting layers.
   */
  public static int countPlantsOnExistingLevels(Home home) {
    return countPlantsOnLevels(home, getExistingPlantingLevels(home));
  }

  /**
   * Returns whether the home has plants on any proposed planting layer.
   */
  public static boolean hasProposedPlants(Home home) {
    return countPlantsOnProposedLevels(home) > 0;
  }

  /**
   * Returns whether the home has plants on any existing planting layer.
   */
  public static boolean hasExistingPlants(Home home) {
    return countPlantsOnExistingLevels(home) > 0;
  }

  /**
   * Returns whether the home has plants on any takeoff planting layer.
   */
  public static boolean hasTakeoffPlants(Home home) {
    return hasProposedPlants(home) || hasExistingPlants(home);
  }

  /**
   * Returns proposed planting layers in home level order.
   */
  public static List<Level> getProposedPlantingLevels(Home home) {
    List<Level> levels = new ArrayList<Level>();
    if (home != null) {
      for (Level level : home.getLevels()) {
        if (isProposedPlantingLevel(level)) {
          levels.add(level);
        }
      }
    }
    return levels;
  }

  /**
   * Returns existing planting layers in home level order.
   */
  public static List<Level> getExistingPlantingLevels(Home home) {
    List<Level> levels = new ArrayList<Level>();
    if (home != null) {
      for (Level level : home.getLevels()) {
        if (isExistingPlantingLevel(level)) {
          levels.add(level);
        }
      }
    }
    return levels;
  }

  /**
   * Returns aggregated plant schedule rows for the given level.
   */
  public static List<PlantScheduleRow> buildSchedule(Home home, Level level) {
    List<Level> levels = new ArrayList<Level>(1);
    if (level != null) {
      levels.add(level);
    }
    return buildScheduleForLevels(home, levels);
  }

  /**
   * Returns aggregated plant schedule rows for all proposed planting layers.
   */
  public static List<PlantScheduleRow> buildProposedSchedule(Home home) {
    return buildScheduleForLevels(home, getProposedPlantingLevels(home));
  }

  /**
   * Returns aggregated plant schedule rows for all existing planting layers.
   */
  public static List<PlantScheduleRow> buildExistingSchedule(Home home) {
    return buildScheduleForLevels(home, getExistingPlantingLevels(home));
  }

  private static int countPlantsOnLevels(Home home, List<Level> levels) {
    if (home == null || levels.isEmpty()) {
      return 0;
    }
    Set<Level> levelSet = new HashSet<Level>(levels);
    int count = 0;
    for (HomePieceOfFurniture piece : home.getFurniture()) {
      if (isPlant(piece) && levelSet.contains(piece.getLevel())) {
        count++;
      }
    }
    return count;
  }

  private static List<PlantScheduleRow> buildScheduleForLevels(Home home, List<Level> levels) {
    Map<String, PlantScheduleRow> rowsByKey = new LinkedHashMap<String, PlantScheduleRow>();
    if (home != null && levels != null && !levels.isEmpty()) {
      Set<Level> levelSet = new HashSet<Level>(levels);
      for (HomePieceOfFurniture piece : home.getFurniture()) {
        if (!isPlant(piece) || !levelSet.contains(piece.getLevel())) {
          continue;
        }
        String key = getScheduleKey(piece);
        PlantScheduleRow row = rowsByKey.get(key);
        if (row == null) {
          row = new PlantScheduleRow(getDisplayName(piece), getReference(piece));
          rowsByKey.put(key, row);
        }
        row.incrementCount();
      }
    }
    return new ArrayList<PlantScheduleRow>(rowsByKey.values());
  }

  private static String getScheduleKey(HomePieceOfFurniture piece) {
    String catalogId = piece.getCatalogId();
    if (catalogId != null && catalogId.trim().length() > 0) {
      return catalogId.trim();
    }
    String name = piece.getName();
    return name != null ? name.trim() : "";
  }

  private static String getDisplayName(HomePieceOfFurniture piece) {
    String name = piece.getName();
    if (name != null && name.trim().length() > 0) {
      return name.trim();
    }
    String catalogId = piece.getCatalogId();
    return catalogId != null ? catalogId.trim() : "";
  }

  private static String getReference(HomePieceOfFurniture piece) {
    String catalogId = piece.getCatalogId();
    return catalogId != null ? catalogId.trim() : "";
  }

  /**
   * One aggregated row in the plant schedule table.
   */
  public static final class PlantScheduleRow {
    private final String name;
    private final String reference;
    private int          count;

    public PlantScheduleRow(String name, String reference) {
      this.name = name != null ? name : "";
      this.reference = reference != null ? reference : "";
      this.count = 0;
    }

    public void incrementCount() {
      this.count++;
    }

    public String getName() {
      return this.name;
    }

    public String getReference() {
      return this.reference;
    }

    public int getCount() {
      return this.count;
    }
  }
}
