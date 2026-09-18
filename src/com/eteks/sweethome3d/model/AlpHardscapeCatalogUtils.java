/*
 * AlpHardscapeCatalogUtils.java
 *
 * ALP CAD — hardscape catalog finish color from variant ID (ALP-Hardscape library).
 */
package com.eteks.sweethome3d.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.eteks.sweethome3d.tools.AlpColorSupport;

/**
 * Helpers for ALP hardscape catalog entries ({@code alp-hrd-*} IDs).
 */
public final class AlpHardscapeCatalogUtils {
  /** Catalog ID prefix for ALP hardscape library entries. */
  public static final String HARDSCAPE_CATALOG_ID_PREFIX = "alp-hrd-";

  private static final Map<String, Integer> FINISH_COLORS_BY_SLUG;

  static {
    Map<String, Integer> colors = new HashMap<String, Integer>();
    colors.put("terra", rgb(178, 95, 62));
    colors.put("grey", rgb(142, 142, 138));
    colors.put("white", rgb(228, 226, 218));
    colors.put("sand", rgb(208, 178, 138));
    colors.put("charcoal", rgb(48, 50, 54));
    FINISH_COLORS_BY_SLUG = Collections.unmodifiableMap(colors);
  }

  private AlpHardscapeCatalogUtils() {
  }

  /**
   * Returns whether a catalog ID belongs to the ALP hardscape library.
   */
  public static boolean isHardscapeCatalogId(String catalogId) {
    return catalogId != null && catalogId.startsWith(HARDSCAPE_CATALOG_ID_PREFIX);
  }

  /**
   * Returns whether a placed piece is an ALP hardscape catalog entry.
   */
  public static boolean isHardscape(HomePieceOfFurniture piece) {
    return piece != null && isHardscapeCatalogId(piece.getCatalogId());
  }

  /**
   * Returns the finish RGB for a hardscape catalog ID, or {@code null} if unknown.
   */
  public static Integer resolveFinishColor(String catalogId) {
    if (!isHardscapeCatalogId(catalogId)) {
      return null;
    }
    int lastDash = catalogId.lastIndexOf('-');
    if (lastDash < 0 || lastDash >= catalogId.length() - 1) {
      return null;
    }
    return FINISH_COLORS_BY_SLUG.get(catalogId.substring(lastDash + 1));
  }

  /**
   * Returns whether the plan outline should be drawn for this piece.
   */
  public static boolean shouldDrawPlanOutline(HomePieceOfFurniture piece) {
    if (piece == null) {
      return true;
    }
    if (!isHardscape(piece)) {
      return true;
    }
    return !AlpColorSupport.isTransparentColor(piece.getOutlineColor());
  }

  /**
   * Returns the tint for plan line art, or {@code null} to keep catalog black lines.
   */
  public static Integer resolvePresentationLineTint(HomePieceOfFurniture piece) {
    if (piece == null || !isHardscape(piece)) {
      return null;
    }
    Integer outlineColor = piece.getOutlineColor();
    if (outlineColor != null && !AlpColorSupport.isTransparentColor(outlineColor)) {
      return outlineColor;
    }
    return null;
  }

  /**
   * Returns the tint for {@code planIconFill} in Presentation mode.
   * Plants keep catalog watercolor when {@code fillColor} is default; hardscape
   * and other non-plant layered symbols use inspector {@link HomePieceOfFurniture#getColor()}.
   */
  public static Integer resolvePresentationFillTint(HomePieceOfFurniture piece) {
    if (piece == null) {
      return null;
    }
    Integer fillColor = piece.getFillColor();
    if (AlpColorSupport.isPlanFillNone(fillColor)) {
      return null;
    }
    if (fillColor != null && !AlpColorSupport.isDefaultPlanWash(fillColor)) {
      return fillColor;
    }
    if (AlpPlantUtils.isPlant(piece)) {
      return null;
    }
    return piece.getColor();
  }

  private static final String PLAN_ICON_LINE_PROPERTY = "planIconLine";

  /**
   * Copies layered plan content properties from catalog onto a new home piece.
   */
  public static void applyCatalogPlanLayers(HomePieceOfFurniture homePiece,
                                            CatalogPieceOfFurniture catalogPiece) {
    if (homePiece == null || catalogPiece == null || !isHardscapeCatalogId(catalogPiece.getId())) {
      return;
    }
    Content planIconFill = catalogPiece.getContentProperty(HomePieceOfFurniture.PLAN_ICON_FILL_PROPERTY);
    if (planIconFill != null) {
      homePiece.setProperty(HomePieceOfFurniture.PLAN_ICON_FILL_PROPERTY, planIconFill);
    }
    Content planIconLine = catalogPiece.getContentProperty(PLAN_ICON_LINE_PROPERTY);
    if (planIconLine != null) {
      homePiece.setProperty(PLAN_ICON_LINE_PROPERTY, planIconLine);
    }
  }

  /**
   * Applies the catalog finish color when a new home piece has no color yet.
   */
  public static void applyDefaultFinishColor(HomePieceOfFurniture piece) {
    if (piece == null || piece.getColor() != null || !piece.isTexturable()) {
      return;
    }
    Integer color = resolveFinishColor(piece.getCatalogId());
    if (color != null) {
      piece.setColor(color);
    }
  }

  private static Integer rgb(int red, int green, int blue) {
    return Integer.valueOf(0xFF000000 | (red << 16) | (green << 8) | blue);
  }
}
