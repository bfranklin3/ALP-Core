/*
 * AlpPlantUtils.java
 *
 * ALP CAD — plant identification and schedule aggregation (SPIKE-29b).
 */
package com.eteks.sweethome3d.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
   * Returns aggregated plant schedule rows for the given level.
   */
  public static List<PlantScheduleRow> buildSchedule(Home home, Level level) {
    Map<String, PlantScheduleRow> rowsByKey = new LinkedHashMap<String, PlantScheduleRow>();
    if (home != null && level != null) {
      for (HomePieceOfFurniture piece : home.getFurniture()) {
        if (!isPlant(piece) || piece.getLevel() != level) {
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
