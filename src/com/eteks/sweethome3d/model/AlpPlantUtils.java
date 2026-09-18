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

import com.eteks.sweethome3d.tools.AlpColorSupport;

/**
 * Helpers for landscape plant furniture and schedule takeoff.
 */
public final class AlpPlantUtils {
  /** Catalog ID prefix for ALP plant library entries. */
  public static final String PLANT_CATALOG_ID_PREFIX = "alp-plt-";
  /** Catalog ID prefix for user-authored ALP plant types (SPIKE-45A). */
  public static final String USER_PLANT_CATALOG_ID_PREFIX = "alp-usr-";
  /** Custom property storing the active plant style preset. */
  public static final String PLANT_STYLE_PRESET_PROPERTY = "alp.plantStylePreset";
  /** Untinted library watercolor wash ({@code fillColor == null}). */
  public static final String PLANT_STYLE_PRESET_DEFAULT_WASH = "defaultWash";
  /** Draft grayscale preset identifier. */
  public static final String PLANT_STYLE_PRESET_DRAFT_GRAY = "draftGray";
  /** Soft presentation preset identifier. */
  public static final String PLANT_STYLE_PRESET_SOFT_GREEN = "softGreen";
  /** Understory preset: soft-green wash at reduced opacity (SPIKE-47B). */
  public static final String PLANT_STYLE_PRESET_UNDERSTORY = "understory";
  /** Bundled plan wash opacity for {@link #PLANT_STYLE_PRESET_UNDERSTORY}. */
  public static final float PLANT_STYLE_UNDERSTORY_OPACITY = 0.6f;
  /** Epsilon for comparing plan wash opacity values. */
  public static final float PLAN_FILL_OPACITY_EPSILON = 0.005f;
  /** Display-only state when {@code fillColor} does not match a known preset. */
  public static final String PLANT_STYLE_PRESET_CUSTOM = "custom";
  /** Outline-only plan fill ({@link AlpColorSupport#TRANSPARENT_COLOR}). */
  public static final String PLANT_STYLE_PRESET_NONE = "none";
  /** Default custom plan-fill tint when unchecking Default wash in the inspector. */
  public static final Integer DEFAULT_PLAN_FILL_TINT = Integer.valueOf(0xFF608040);

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
    return catalogId != null
        && (catalogId.startsWith(PLANT_CATALOG_ID_PREFIX)
            || catalogId.startsWith(USER_PLANT_CATALOG_ID_PREFIX));
  }

  /**
   * Returns the normalized style preset stored on a plant piece.
   */
  public static String getPlantStylePreset(HomePieceOfFurniture piece) {
    if (!isPlant(piece)) {
      return null;
    }
    return normalizePlantStylePreset(piece.getProperty(PLANT_STYLE_PRESET_PROPERTY));
  }

  /**
   * Returns the normalized preset identifier stored on a piece.
   */
  public static String normalizePlantStylePreset(String preset) {
    if (PLANT_STYLE_PRESET_DEFAULT_WASH.equals(preset)) {
      return PLANT_STYLE_PRESET_DEFAULT_WASH;
    } else if (PLANT_STYLE_PRESET_DRAFT_GRAY.equals(preset)) {
      return PLANT_STYLE_PRESET_DRAFT_GRAY;
    } else if (PLANT_STYLE_PRESET_SOFT_GREEN.equals(preset)) {
      return PLANT_STYLE_PRESET_SOFT_GREEN;
    } else if (PLANT_STYLE_PRESET_UNDERSTORY.equals(preset)) {
      return PLANT_STYLE_PRESET_UNDERSTORY;
    } else if (PLANT_STYLE_PRESET_NONE.equals(preset)) {
      return PLANT_STYLE_PRESET_NONE;
    } else if (PLANT_STYLE_PRESET_CUSTOM.equals(preset)) {
      return PLANT_STYLE_PRESET_CUSTOM;
    }
    return PLANT_STYLE_PRESET_SOFT_GREEN;
  }

  /**
   * Returns the display preset inferred from a plan fill color value.
   */
  public static String resolvePlantStylePresetDisplay(Integer fillColor) {
    return resolvePlantStylePresetDisplay(fillColor, HomePieceOfFurniture.DEFAULT_PLAN_FILL_OPACITY);
  }

  /**
   * Returns the display preset inferred from plan fill color and wash opacity (SPIKE-47B Hybrid).
   */
  public static String resolvePlantStylePresetDisplay(Integer fillColor, float planFillOpacity) {
    if (AlpColorSupport.isPlanFillNone(fillColor)) {
      return PLANT_STYLE_PRESET_NONE;
    }
    if (AlpColorSupport.isDefaultPlanWash(fillColor)) {
      return PLANT_STYLE_PRESET_DEFAULT_WASH;
    }
    if (plantStyleFillColorsMatch(fillColor,
        getPlantStylePresetFillColor(PLANT_STYLE_PRESET_DRAFT_GRAY))) {
      if (planFillOpacitiesMatch(planFillOpacity, HomePieceOfFurniture.DEFAULT_PLAN_FILL_OPACITY)) {
        return PLANT_STYLE_PRESET_DRAFT_GRAY;
      }
      return PLANT_STYLE_PRESET_CUSTOM;
    }
    if (plantStyleFillColorsMatch(fillColor,
        getPlantStylePresetFillColor(PLANT_STYLE_PRESET_SOFT_GREEN))) {
      if (planFillOpacitiesMatch(planFillOpacity, PLANT_STYLE_UNDERSTORY_OPACITY)) {
        return PLANT_STYLE_PRESET_UNDERSTORY;
      }
      if (planFillOpacitiesMatch(planFillOpacity, HomePieceOfFurniture.DEFAULT_PLAN_FILL_OPACITY)) {
        return PLANT_STYLE_PRESET_SOFT_GREEN;
      }
      return PLANT_STYLE_PRESET_CUSTOM;
    }
    return PLANT_STYLE_PRESET_CUSTOM;
  }

  /**
   * Returns the display preset inferred from plan fill color and wash state.
   * Honors a stored selectable preset when it still matches the piece (Hybrid SPIKE-47B).
   */
  public static String resolvePlantStylePresetDisplay(HomePieceOfFurniture piece) {
    if (!isPlant(piece) || !piece.hasPlanIconFill()) {
      return null;
    }
    Integer fillColor = piece.getFillColor();
    float planFillOpacity = piece.getPlanFillOpacity();
    String storedRaw = piece.getProperty(PLANT_STYLE_PRESET_PROPERTY);
    if (storedRaw != null && storedRaw.length() > 0) {
      if (PLANT_STYLE_PRESET_CUSTOM.equals(storedRaw)) {
        return PLANT_STYLE_PRESET_CUSTOM;
      }
      String normalized = normalizePlantStylePreset(storedRaw);
      if (isSelectablePlantStylePreset(normalized)
          && plantStylePresetMatchesPiece(normalized, fillColor, planFillOpacity)) {
        return normalized;
      }
      return PLANT_STYLE_PRESET_CUSTOM;
    }
    return resolvePlantStylePresetDisplay(fillColor, planFillOpacity);
  }

  /**
   * Returns whether a selectable preset still matches fill color and opacity on a piece.
   */
  public static boolean plantStylePresetMatchesPiece(String preset, Integer fillColor,
                                                     float planFillOpacity) {
    if (preset == null) {
      return false;
    }
    String normalized = normalizePlantStylePreset(preset);
    if (PLANT_STYLE_PRESET_NONE.equals(normalized)) {
      return AlpColorSupport.isPlanFillNone(fillColor);
    }
    if (PLANT_STYLE_PRESET_DEFAULT_WASH.equals(normalized)) {
      return AlpColorSupport.isDefaultPlanWash(fillColor);
    }
    if (PLANT_STYLE_PRESET_DRAFT_GRAY.equals(normalized)) {
      return plantStyleFillColorsMatch(fillColor,
          getPlantStylePresetFillColor(PLANT_STYLE_PRESET_DRAFT_GRAY));
    }
    if (PLANT_STYLE_PRESET_SOFT_GREEN.equals(normalized)) {
      return plantStyleFillColorsMatch(fillColor,
          getPlantStylePresetFillColor(PLANT_STYLE_PRESET_SOFT_GREEN));
    }
    if (PLANT_STYLE_PRESET_UNDERSTORY.equals(normalized)) {
      return plantStyleFillColorsMatch(fillColor,
          getPlantStylePresetFillColor(PLANT_STYLE_PRESET_UNDERSTORY))
          && planFillOpacitiesMatch(planFillOpacity, PLANT_STYLE_UNDERSTORY_OPACITY);
    }
    return false;
  }

  /**
   * Returns the preset property to store after a manual opacity edit (Hybrid honesty).
   */
  public static String resolvePlantStylePresetPropertyAfterOpacityChange(
      HomePieceOfFurniture piece, Integer fillColor, float planFillOpacity) {
    if (piece != null) {
      String storedRaw = piece.getProperty(PLANT_STYLE_PRESET_PROPERTY);
      if (storedRaw != null && !PLANT_STYLE_PRESET_CUSTOM.equals(storedRaw)) {
        String normalized = normalizePlantStylePreset(storedRaw);
        if (PLANT_STYLE_PRESET_UNDERSTORY.equals(normalized)) {
          if (planFillOpacitiesMatch(planFillOpacity, PLANT_STYLE_UNDERSTORY_OPACITY)
              && plantStyleFillColorsMatch(fillColor,
                  getPlantStylePresetFillColor(PLANT_STYLE_PRESET_UNDERSTORY))) {
            return PLANT_STYLE_PRESET_UNDERSTORY;
          }
          return PLANT_STYLE_PRESET_CUSTOM;
        }
        if (isSelectablePlantStylePreset(normalized)
            && plantStylePresetMatchesPiece(normalized, fillColor, planFillOpacity)) {
          return normalized;
        }
      }
    }
    return resolvePlantStylePresetDisplay(fillColor, planFillOpacity);
  }

  /**
   * Returns whether the preset can be chosen from the inspector dropdown.
   */
  public static boolean isSelectablePlantStylePreset(String preset) {
    return PLANT_STYLE_PRESET_DEFAULT_WASH.equals(preset)
        || PLANT_STYLE_PRESET_DRAFT_GRAY.equals(preset)
        || PLANT_STYLE_PRESET_SOFT_GREEN.equals(preset)
        || PLANT_STYLE_PRESET_UNDERSTORY.equals(preset)
        || PLANT_STYLE_PRESET_NONE.equals(preset);
  }

  /**
   * Returns bundled plan wash opacity for a selectable preset, or {@code null} to preserve current.
   */
  public static Float getPlantStylePresetAppliedOpacity(String preset) {
    if (PLANT_STYLE_PRESET_UNDERSTORY.equals(normalizePlantStylePreset(preset))) {
      return Float.valueOf(PLANT_STYLE_UNDERSTORY_OPACITY);
    }
    return null;
  }

  /**
   * Returns the plan fill tint associated with a selectable preset.
   */
  public static Integer getPlantStylePresetFillColor(String preset) {
    String normalizedPreset = normalizePlantStylePreset(preset);
    if (PLANT_STYLE_PRESET_DRAFT_GRAY.equals(normalizedPreset)) {
      return Integer.valueOf(0xFFA8A8A8);
    } else if (PLANT_STYLE_PRESET_SOFT_GREEN.equals(normalizedPreset)
        || PLANT_STYLE_PRESET_UNDERSTORY.equals(normalizedPreset)) {
      return Integer.valueOf(0xFFA8C98B);
    }
    return null;
  }

  /**
   * Returns the plan fill color to apply for a selectable preset choice.
   */
  public static Integer getPlantStylePresetAppliedFillColor(String preset) {
    if (PLANT_STYLE_PRESET_DEFAULT_WASH.equals(preset)) {
      return null;
    } else if (PLANT_STYLE_PRESET_NONE.equals(preset)) {
      return AlpColorSupport.TRANSPARENT_COLOR;
    }
    return getPlantStylePresetFillColor(preset);
  }

  private static boolean plantStyleFillColorsMatch(Integer fillColor, Integer presetFillColor) {
    if (fillColor == null || presetFillColor == null) {
      return false;
    }
    return (fillColor.intValue() & 0xFFFFFF) == (presetFillColor.intValue() & 0xFFFFFF);
  }

  private static boolean planFillOpacitiesMatch(float opacity, float expectedOpacity) {
    return Math.abs(opacity - expectedOpacity) < PLAN_FILL_OPACITY_EPSILON;
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
    return AlpPlantMetadata.getScheduleName(piece);
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
