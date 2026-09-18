/*
 * AlpPlantAreaFill.java
 *
 * ALP CAD — single-species planting area scatter (SPIKE-38A / 38B tight fill).
 */
package com.eteks.sweethome3d.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Generates editable plant instances inside an area boundary from a stored fill recipe.
 */
public final class AlpPlantAreaFill {
  public static final String ROOM_FILL_ID = "alp.plantFill.id";
  public static final String ROOM_SYMBOL_ID = "alp.plantFill.symbolId";
  public static final String ROOM_SPACING = "alp.plantFill.spacing";
  public static final String ROOM_SEED = "alp.plantFill.seed";
  public static final String ROOM_SIZE_VARIATION = "alp.plantFill.sizeVariation";
  public static final String ROOM_EDGE_OFFSET = "alp.plantFill.edgeOffset";
  public static final String ROOM_TIGHT_FILL = "alp.plantFill.tightFill";

  public static final String PLANT_FILL_ID = "alp.plantFill.id";
  public static final String PLANT_FILL_GENERATED = "alp.plantFill.generated";

  public static final float DEFAULT_SIZE_VARIATION_PERCENT = 5f;
  public static final float DEFAULT_EDGE_OFFSET_FACTOR = 0.3f;
  public static final float TIGHT_EDGE_OFFSET_FACTOR = 0.12f;
  public static final float DEFAULT_JITTER_FACTOR = 0.25f;
  public static final float TIGHT_JITTER_FACTOR = 0.08f;
  public static final float DEFAULT_RADIUS_INSET_FACTOR = 0.35f;
  public static final float TIGHT_RADIUS_INSET_FACTOR = 0.15f;
  private static final float HEX_ROW_STEP_FACTOR = (float)(Math.sqrt(3) / 2);

  private AlpPlantAreaFill() {
  }

  /**
   * Immutable fill recipe stored on the area (room) and linked from generated plants.
   */
  public static final class Recipe {
    private final String fillId;
    private final String symbolCatalogId;
    private final float spacing;
    private final float sizeVariationPercent;
    private final float edgeOffset;
    private final long randomSeed;
    private final boolean tightFill;

    public Recipe(String fillId, String symbolCatalogId, float spacing,
                  float sizeVariationPercent, float edgeOffset, long randomSeed,
                  boolean tightFill) {
      this.fillId = fillId;
      this.symbolCatalogId = symbolCatalogId;
      this.spacing = spacing;
      this.sizeVariationPercent = sizeVariationPercent;
      this.edgeOffset = edgeOffset;
      this.randomSeed = randomSeed;
      this.tightFill = tightFill;
    }

    public String getFillId() {
      return this.fillId;
    }

    public String getSymbolCatalogId() {
      return this.symbolCatalogId;
    }

    public float getSpacing() {
      return this.spacing;
    }

    public float getSizeVariationPercent() {
      return this.sizeVariationPercent;
    }

    public float getEdgeOffset() {
      return this.edgeOffset;
    }

    public long getRandomSeed() {
      return this.randomSeed;
    }

    public boolean isTightFill() {
      return this.tightFill;
    }
  }

  public static boolean hasRecipe(Room room) {
    return room != null && getFillId(room) != null;
  }

  public static Recipe getRecipe(Room room) {
    if (room == null) {
      return null;
    }
    String fillId = getFillId(room);
    String symbolId = room.getProperty(ROOM_SYMBOL_ID);
    if (fillId == null || symbolId == null || symbolId.trim().length() == 0) {
      return null;
    }
    float spacing = parseFloat(room.getProperty(ROOM_SPACING), 0f);
    if (spacing <= 0) {
      return null;
    }
    boolean tightFill = parseBoolean(room.getProperty(ROOM_TIGHT_FILL), false);
    float defaultEdgeOffset = spacing * (tightFill
        ? TIGHT_EDGE_OFFSET_FACTOR : DEFAULT_EDGE_OFFSET_FACTOR);
    return new Recipe(fillId, symbolId.trim(), spacing,
        parseFloat(room.getProperty(ROOM_SIZE_VARIATION), DEFAULT_SIZE_VARIATION_PERCENT),
        parseFloat(room.getProperty(ROOM_EDGE_OFFSET), defaultEdgeOffset),
        parseLong(room.getProperty(ROOM_SEED), System.currentTimeMillis()),
        tightFill);
  }

  public static void saveRecipe(Room room, Recipe recipe) {
    if (room == null || recipe == null) {
      return;
    }
    room.setProperty(ROOM_FILL_ID, recipe.getFillId());
    room.setProperty(ROOM_SYMBOL_ID, recipe.getSymbolCatalogId());
    room.setProperty(ROOM_SPACING, Float.toString(recipe.getSpacing()));
    room.setProperty(ROOM_SIZE_VARIATION, Float.toString(recipe.getSizeVariationPercent()));
    room.setProperty(ROOM_EDGE_OFFSET, Float.toString(recipe.getEdgeOffset()));
    room.setProperty(ROOM_SEED, Long.toString(recipe.getRandomSeed()));
    if (recipe.isTightFill()) {
      room.setProperty(ROOM_TIGHT_FILL, "true");
    } else {
      removeProperty(room, ROOM_TIGHT_FILL);
    }
  }

  public static void clearRecipe(Room room) {
    if (room == null) {
      return;
    }
    removeProperty(room, ROOM_FILL_ID);
    removeProperty(room, ROOM_SYMBOL_ID);
    removeProperty(room, ROOM_SPACING);
    removeProperty(room, ROOM_SIZE_VARIATION);
    removeProperty(room, ROOM_EDGE_OFFSET);
    removeProperty(room, ROOM_SEED);
    removeProperty(room, ROOM_TIGHT_FILL);
  }

  public static List<HomePieceOfFurniture> findGeneratedPlants(Home home, Room room) {
    List<HomePieceOfFurniture> generated = new ArrayList<HomePieceOfFurniture>();
    if (home == null || room == null) {
      return generated;
    }
    String fillId = getFillId(room);
    if (fillId == null) {
      return generated;
    }
    Level level = room.getLevel();
    for (HomePieceOfFurniture piece : home.getFurniture()) {
      if (isGeneratedPlantForFill(piece, fillId, level)) {
        generated.add(piece);
      }
    }
    return generated;
  }

  public static List<Placement> buildPlacements(Room room, AlpPlantMetadata metadata, Recipe recipe) {
    List<Placement> placements = new ArrayList<Placement>();
    if (room == null || metadata == null || recipe == null) {
      return placements;
    }
    Random random = new Random(recipe.getRandomSeed());
    float plantRadius = Math.max(metadata.getDefaultWidth(), metadata.getDefaultDepth()) * 0.5f;
    List<float []> centers = scatterCenters(room, recipe.getSpacing(), recipe.getEdgeOffset(),
        plantRadius, random, recipe.isTightFill());
    for (float [] center : centers) {
      float width = metadata.getDefaultWidth();
      float depth = metadata.getDefaultDepth();
      float angle = 0;
      if (metadata.isSupportsRandomScale() && recipe.getSizeVariationPercent() > 0) {
        float scale = 1f + (random.nextFloat() * 2f - 1f)
            * (recipe.getSizeVariationPercent() / 100f);
        width *= scale;
        depth *= scale;
      }
      if (metadata.isSupportsRandomRotation()) {
        angle = random.nextFloat() * 360f;
      }
      placements.add(new Placement(center [0], center [1], width, depth, angle));
    }
    return placements;
  }

  public static void markGeneratedPlant(HomePieceOfFurniture plant, String fillId) {
    plant.setProperty(PLANT_FILL_ID, fillId);
    plant.setProperty(PLANT_FILL_GENERATED, "true");
  }

  /**
   * Clears area-fill linkage so a duplicated or pasted plant is standalone.
   */
  public static void clearGeneratedPlantLinkage(HomePieceOfFurniture plant) {
    if (plant == null) {
      return;
    }
    if (plant.getProperty(PLANT_FILL_ID) != null) {
      plant.setProperty(PLANT_FILL_ID, null);
    }
    if (plant.getProperty(PLANT_FILL_GENERATED) != null) {
      plant.setProperty(PLANT_FILL_GENERATED, null);
    }
  }

  /**
   * Returns whether a placed plant was generated by an area scatter fill.
   */
  public static boolean isGeneratedPlant(HomePieceOfFurniture piece) {
    if (piece == null || !AlpPlantUtils.isPlant(piece)) {
      return false;
    }
    String fillId = piece.getProperty(PLANT_FILL_ID);
    return fillId != null && fillId.trim().length() > 0
        && "true".equalsIgnoreCase(piece.getProperty(PLANT_FILL_GENERATED));
  }

  /**
   * Returns the planting area that owns the fill recipe for a generated plant.
   */
  public static Room findParentRoom(Home home, HomePieceOfFurniture plant) {
    if (home == null || !isGeneratedPlant(plant)) {
      return null;
    }
    String fillId = plant.getProperty(PLANT_FILL_ID).trim();
    Level level = plant.getLevel();
    for (Room room : home.getRooms()) {
      if (level != null && room.getLevel() != level) {
        continue;
      }
      if (fillId.equals(getFillId(room))) {
        return room;
      }
    }
    return null;
  }

  public static final class Placement {
    private final float x;
    private final float y;
    private final float width;
    private final float depth;
    private final float angle;

    public Placement(float x, float y, float width, float depth, float angle) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.depth = depth;
      this.angle = angle;
    }

    public float getX() {
      return this.x;
    }

    public float getY() {
      return this.y;
    }

    public float getWidth() {
      return this.width;
    }

    public float getDepth() {
      return this.depth;
    }

    public float getAngle() {
      return this.angle;
    }
  }

  public static Recipe createRecipe(Room room, String symbolCatalogId, float spacing,
                                    float sizeVariationPercent, float edgeOffset, Long seed,
                                    boolean tightFill, boolean preserveFillId) {
    String fillId = preserveFillId ? getFillId(room) : null;
    if (fillId == null) {
      fillId = UUID.randomUUID().toString();
    }
    long randomSeed = seed != null ? seed.longValue() : System.currentTimeMillis();
    if (edgeOffset < 0) {
      edgeOffset = spacing * (tightFill
          ? TIGHT_EDGE_OFFSET_FACTOR : DEFAULT_EDGE_OFFSET_FACTOR);
    }
    if (sizeVariationPercent < 0) {
      sizeVariationPercent = DEFAULT_SIZE_VARIATION_PERCENT;
    }
    return new Recipe(fillId, symbolCatalogId, spacing, sizeVariationPercent, edgeOffset,
        randomSeed, tightFill);
  }

  static List<float []> scatterCenters(Room room, float spacing, float edgeOffset,
                                       float plantRadius, Random random, boolean tightFill) {
    List<float []> centers = new ArrayList<float []>();
    if (room == null || spacing <= 0 || random == null) {
      return centers;
    }
    float [] min = room.getBoundsMinimumCoordinates();
    float [] max = room.getBoundsMaximumCoordinates();
    float jitterFactor = tightFill ? TIGHT_JITTER_FACTOR : DEFAULT_JITTER_FACTOR;
    float radiusInsetFactor = tightFill ? TIGHT_RADIUS_INSET_FACTOR : DEFAULT_RADIUS_INSET_FACTOR;
    float jitter = spacing * jitterFactor;
    float inset = Math.max(edgeOffset, 0) + plantRadius * radiusInsetFactor;

    float rowStep = tightFill ? spacing * HEX_ROW_STEP_FACTOR : spacing;
    float colStep = spacing;
    float startY = min [1] + (tightFill ? rowStep * 0.5f : spacing * 0.5f);
    int rowIndex = 0;
    for (float y = startY; y <= max [1]; y += rowStep, rowIndex++) {
      float rowOffset = tightFill && (rowIndex % 2) == 1 ? spacing * 0.5f : 0f;
      for (float x = min [0] + spacing * 0.5f + rowOffset; x <= max [0]; x += colStep) {
        float jx = x + (random.nextFloat() - 0.5f) * jitter;
        float jy = y + (random.nextFloat() - 0.5f) * jitter;
        if (isValidPlantCenter(room, jx, jy, inset)) {
          centers.add(new float [] {jx, jy});
        }
      }
    }
    return centers;
  }

  private static boolean isValidPlantCenter(Room room, float x, float y, float inset) {
    if (!room.containsPoint(x, y, 0)) {
      return false;
    }
    return distanceToRoomBoundary(room, x, y) >= inset;
  }

  private static float distanceToRoomBoundary(Room room, float x, float y) {
    float [][] points = room.getPoints();
    float minDistance = Float.MAX_VALUE;
    for (int i = 0; i < points.length; i++) {
      float x0 = points [i][0];
      float y0 = points [i][1];
      float x1 = points [(i + 1) % points.length][0];
      float y1 = points [(i + 1) % points.length][1];
      minDistance = Math.min(minDistance, distancePointToSegment(x, y, x0, y0, x1, y1));
    }
    return minDistance;
  }

  private static float distancePointToSegment(float px, float py,
                                                float x0, float y0, float x1, float y1) {
    float dx = x1 - x0;
    float dy = y1 - y0;
    float lengthSquared = dx * dx + dy * dy;
    if (lengthSquared == 0) {
      return (float)Math.hypot(px - x0, py - y0);
    }
    float t = ((px - x0) * dx + (py - y0) * dy) / lengthSquared;
    t = Math.max(0, Math.min(1, t));
    float projX = x0 + t * dx;
    float projY = y0 + t * dy;
    return (float)Math.hypot(px - projX, py - projY);
  }

  private static boolean isGeneratedPlantForFill(HomePieceOfFurniture piece,
                                                 String fillId, Level level) {
    if (!AlpPlantUtils.isPlant(piece) || level == null || piece.getLevel() != level) {
      return false;
    }
    return fillId.equals(piece.getProperty(PLANT_FILL_ID))
        && "true".equalsIgnoreCase(piece.getProperty(PLANT_FILL_GENERATED));
  }

  private static String getFillId(Room room) {
    String fillId = room.getProperty(ROOM_FILL_ID);
    if (fillId == null || fillId.trim().length() == 0) {
      return null;
    }
    return fillId.trim();
  }

  private static float parseFloat(String value, float defaultValue) {
    if (value == null || value.trim().length() == 0) {
      return defaultValue;
    }
    try {
      return Float.parseFloat(value.trim());
    } catch (NumberFormatException ex) {
      return defaultValue;
    }
  }

  private static long parseLong(String value, long defaultValue) {
    if (value == null || value.trim().length() == 0) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException ex) {
      return defaultValue;
    }
  }

  private static boolean parseBoolean(String value, boolean defaultValue) {
    if (value == null || value.trim().length() == 0) {
      return defaultValue;
    }
    return "true".equalsIgnoreCase(value.trim());
  }

  private static void removeProperty(Room room, String propertyName) {
    if (room.getProperty(propertyName) != null) {
      room.setProperty(propertyName, null);
    }
  }
}
