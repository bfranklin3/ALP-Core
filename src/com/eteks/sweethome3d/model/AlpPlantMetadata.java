/*
 * AlpPlantMetadata.java
 *
 * ALP CAD — plant symbol metadata (SPIKE-40 / SPIKE-40A).
 */
package com.eteks.sweethome3d.model;

/**
 * Reads SPIKE-40 plant metadata from catalog custom properties on placed furniture.
 */
public final class AlpPlantMetadata {
  /** Catalog custom property: {@code png} or {@code svg}. */
  public static final String PROPERTY_ASSET_TYPE = "alp.plant.assetType";
  /** Catalog custom property: whether plan wash tinting is supported. */
  public static final String PROPERTY_SUPPORTS_TINT = "alp.plant.supportsTint";
  /** Catalog custom property: radial / low-impact rotation appearance. */
  public static final String PROPERTY_IS_SYMMETRICAL = "alp.plant.isSymmetrical";
  /** Catalog custom property: area fill may rotate instances. */
  public static final String PROPERTY_SUPPORTS_RANDOM_ROTATION = "alp.plant.supportsRandomRotation";
  /** Catalog custom property: area fill may vary instance size slightly. */
  public static final String PROPERTY_SUPPORTS_RANDOM_SCALE = "alp.plant.supportsRandomScale";
  /** Catalog custom property: recommended center spacing in centimeters. */
  public static final String PROPERTY_DEFAULT_SPACING = "alp.plant.defaultSpacing";
  /** Catalog custom property: schedule / takeoff display name. */
  public static final String PROPERTY_SCHEDULE_NAME = "alp.plant.scheduleName";
  /** Catalog custom property: common name (optional). */
  public static final String PROPERTY_COMMON_NAME = "alp.plant.commonName";
  /** Catalog custom property: botanical name (optional). */
  public static final String PROPERTY_BOTANICAL_NAME = "alp.plant.botanicalName";

  public static final String ASSET_TYPE_PNG = "png";
  public static final String ASSET_TYPE_SVG = "svg";

  private final String  symbolId;
  private final String  displayName;
  private final String  scheduleName;
  private final String  commonName;
  private final String  botanicalName;
  private final String  assetType;
  private final float   defaultWidth;
  private final float   defaultDepth;
  private final float   defaultSpacing;
  private final boolean keepProportions;
  private final boolean supportsTint;
  private final boolean symmetrical;
  private final boolean supportsRandomRotation;
  private final boolean supportsRandomScale;

  private AlpPlantMetadata(String symbolId, String displayName, String scheduleName,
                           String commonName, String botanicalName, String assetType,
                           float defaultWidth, float defaultDepth, float defaultSpacing,
                           boolean keepProportions, boolean supportsTint, boolean symmetrical,
                           boolean supportsRandomRotation, boolean supportsRandomScale) {
    this.symbolId = symbolId;
    this.displayName = displayName;
    this.scheduleName = scheduleName;
    this.commonName = commonName;
    this.botanicalName = botanicalName;
    this.assetType = assetType;
    this.defaultWidth = defaultWidth;
    this.defaultDepth = defaultDepth;
    this.defaultSpacing = defaultSpacing;
    this.keepProportions = keepProportions;
    this.supportsTint = supportsTint;
    this.symmetrical = symmetrical;
    this.supportsRandomRotation = supportsRandomRotation;
    this.supportsRandomScale = supportsRandomScale;
  }

  /**
   * Returns metadata for an ALP plant piece, or {@code null} if not a plant.
   */
  public static AlpPlantMetadata fromPiece(HomePieceOfFurniture piece) {
    if (!AlpPlantUtils.isPlant(piece)) {
      return null;
    }
    String symbolId = piece.getCatalogId();
    String displayName = piece.getName();
    if (displayName == null || displayName.trim().length() == 0) {
      displayName = symbolId != null ? symbolId : "";
    } else {
      displayName = displayName.trim();
    }
    if (symbolId != null) {
      symbolId = symbolId.trim();
    }

    boolean symmetrical = parseBooleanProperty(piece, PROPERTY_IS_SYMMETRICAL, true);
    boolean supportsRandomRotation = parseBooleanProperty(
        piece, PROPERTY_SUPPORTS_RANDOM_ROTATION, symmetrical);
    boolean supportsRandomScale = parseBooleanProperty(
        piece, PROPERTY_SUPPORTS_RANDOM_SCALE, true);
    boolean supportsTint = parseBooleanProperty(
        piece, PROPERTY_SUPPORTS_TINT, piece.hasPlanIconFill());
    String assetType = getStringProperty(piece, PROPERTY_ASSET_TYPE, ASSET_TYPE_PNG);
    float defaultWidth = piece.getWidth();
    float defaultDepth = piece.getDepth();
    float defaultSpacing = parseFloatProperty(
        piece, PROPERTY_DEFAULT_SPACING, inferDefaultSpacing(defaultWidth, defaultDepth));
    String scheduleName = getStringProperty(piece, PROPERTY_SCHEDULE_NAME, displayName);
    String commonName = getStringProperty(piece, PROPERTY_COMMON_NAME, null);
    String botanicalName = getStringProperty(piece, PROPERTY_BOTANICAL_NAME, null);
    boolean keepProportions = !piece.isDeformable();

    return new AlpPlantMetadata(symbolId, displayName, scheduleName, commonName, botanicalName,
        assetType, defaultWidth, defaultDepth, defaultSpacing, keepProportions, supportsTint,
        symmetrical, supportsRandomRotation, supportsRandomScale);
  }

  /**
   * Returns the schedule display name, falling back to the piece name.
   */
  public static String getScheduleName(HomePieceOfFurniture piece) {
    AlpPlantMetadata metadata = fromPiece(piece);
    if (metadata != null) {
      return metadata.getScheduleName();
    }
    String name = piece.getName();
    if (name != null && name.trim().length() > 0) {
      return name.trim();
    }
    String catalogId = piece.getCatalogId();
    return catalogId != null ? catalogId.trim() : "";
  }

  private static float inferDefaultSpacing(float width, float depth) {
    return Math.min(width, depth) * 0.65f;
  }

  private static String getStringProperty(HomePieceOfFurniture piece, String key, String defaultValue) {
    String value = piece.getProperty(key);
    if (value == null || value.trim().length() == 0) {
      return defaultValue;
    }
    return value.trim();
  }

  private static boolean parseBooleanProperty(HomePieceOfFurniture piece, String key, boolean defaultValue) {
    String value = piece.getProperty(key);
    if (value == null) {
      return defaultValue;
    }
    value = value.trim();
    if ("true".equalsIgnoreCase(value) || "1".equals(value)) {
      return true;
    } else if ("false".equalsIgnoreCase(value) || "0".equals(value)) {
      return false;
    }
    return defaultValue;
  }

  private static float parseFloatProperty(HomePieceOfFurniture piece, String key, float defaultValue) {
    String value = piece.getProperty(key);
    if (value == null || value.trim().length() == 0) {
      return defaultValue;
    }
    try {
      return Float.parseFloat(value.trim());
    } catch (NumberFormatException ex) {
      return defaultValue;
    }
  }

  public String getSymbolId() {
    return this.symbolId;
  }

  public String getDisplayName() {
    return this.displayName;
  }

  public String getScheduleName() {
    return this.scheduleName;
  }

  public String getCommonName() {
    return this.commonName;
  }

  public String getBotanicalName() {
    return this.botanicalName;
  }

  public String getAssetType() {
    return this.assetType;
  }

  public float getDefaultWidth() {
    return this.defaultWidth;
  }

  public float getDefaultDepth() {
    return this.defaultDepth;
  }

  public float getDefaultSpacing() {
    return this.defaultSpacing;
  }

  public boolean isKeepProportions() {
    return this.keepProportions;
  }

  public boolean isSupportsTint() {
    return this.supportsTint;
  }

  public boolean isSymmetrical() {
    return this.symmetrical;
  }

  public boolean isSupportsRandomRotation() {
    return this.supportsRandomRotation;
  }

  public boolean isSupportsRandomScale() {
    return this.supportsRandomScale;
  }
}
