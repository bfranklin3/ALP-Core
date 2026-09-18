/*
 * AlpPlantCatalogUtils.java
 *
 * ALP CAD — catalog plant duplication (SPIKE-45A).
 */
package com.eteks.sweethome3d.model;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Helpers for duplicating ALP catalog plant types into the user plant library.
 */
public final class AlpPlantCatalogUtils {
  /** Category name for user-authored plant types. */
  public static final String USER_PLANTS_CATEGORY = "ALP User Plants";

  private AlpPlantCatalogUtils() {
  }

  /**
   * Returns whether a catalog entry is an ALP plant type (library or user).
   */
  public static boolean isCatalogPlant(CatalogPieceOfFurniture piece) {
    if (piece == null) {
      return false;
    }
    String id = piece.getId();
    return id != null
        && (id.startsWith(AlpPlantUtils.PLANT_CATALOG_ID_PREFIX)
            || id.startsWith(AlpPlantUtils.USER_PLANT_CATALOG_ID_PREFIX));
  }

  /**
   * Returns the user plant category, creating it in the catalog if needed.
   */
  public static FurnitureCategory findOrCreateUserPlantsCategory(FurnitureCatalog catalog) {
    for (FurnitureCategory category : catalog.getCategories()) {
      if (USER_PLANTS_CATEGORY.equals(category.getName())) {
        return category;
      }
    }
    return new FurnitureCategory(USER_PLANTS_CATEGORY);
  }

  /**
   * Creates a modifiable duplicate of a catalog plant with edited identity and size.
   */
  public static CatalogPieceOfFurniture duplicatePlantType(CatalogPieceOfFurniture source,
                                                           FurnitureCatalog catalog,
                                                           String displayName,
                                                           String scheduleName,
                                                           float width,
                                                           float depth) {
    Map<String, String> overrides = new HashMap<String, String>();
    if (scheduleName != null && scheduleName.trim().length() > 0) {
      overrides.put(AlpPlantMetadata.PROPERTY_SCHEDULE_NAME, scheduleName.trim());
    }
    String newId = generateUserPlantId(catalog, displayName);
    String description = buildDescription(source, displayName);
    return CatalogPieceOfFurniture.createModifiableCopy(
        source, newId, displayName.trim(), description, width, depth, overrides);
  }

  /**
   * Returns a default display name for a duplicate of the given source plant.
   */
  public static String defaultDuplicateName(CatalogPieceOfFurniture source) {
    String sourceName = source.getName();
    if (sourceName == null || sourceName.trim().length() == 0) {
      return "Copy of plant";
    }
    return "Copy of " + sourceName.trim();
  }

  private static String buildDescription(CatalogPieceOfFurniture source, String displayName) {
    String sourceDescription = source.getDescription();
    if (sourceDescription != null && sourceDescription.trim().length() > 0) {
      return sourceDescription.trim();
    }
    return displayName.trim() + ". User plant type duplicated from ALP library.";
  }

  private static String generateUserPlantId(FurnitureCatalog catalog, String displayName) {
    String slug = slugify(displayName);
    if (slug.length() == 0) {
      slug = "plant";
    }
    String candidate = AlpPlantUtils.USER_PLANT_CATALOG_ID_PREFIX + slug;
    if (!catalogContainsId(catalog, candidate)) {
      return candidate;
    }
    for (int suffix = 2; suffix < 1000; suffix++) {
      candidate = AlpPlantUtils.USER_PLANT_CATALOG_ID_PREFIX + slug + "-" + suffix;
      if (!catalogContainsId(catalog, candidate)) {
        return candidate;
      }
    }
    return AlpPlantUtils.USER_PLANT_CATALOG_ID_PREFIX + slug + "-" + System.currentTimeMillis();
  }

  private static boolean catalogContainsId(FurnitureCatalog catalog, String id) {
    for (FurnitureCategory category : catalog.getCategories()) {
      for (CatalogPieceOfFurniture piece : category.getFurniture()) {
        if (id.equals(piece.getId())) {
          return true;
        }
      }
    }
    return false;
  }

  private static String slugify(String text) {
    if (text == null) {
      return "";
    }
    String normalized = text.trim().toLowerCase(Locale.ENGLISH);
    normalized = normalized.replaceAll("[^a-z0-9]+", "-");
    normalized = normalized.replaceAll("^-+|-+$", "");
    if (normalized.length() > 40) {
      normalized = normalized.substring(0, 40).replaceAll("-+$", "");
    }
    return normalized;
  }
}
