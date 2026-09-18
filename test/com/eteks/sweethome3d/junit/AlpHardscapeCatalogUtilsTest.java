/*
 * AlpHardscapeCatalogUtilsTest.java
 *
 * ALP CAD — tests for hardscape catalog finish color resolution.
 */
package com.eteks.sweethome3d.junit;

import junit.framework.TestCase;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.model.AlpHardscapeCatalogUtils;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.tools.AlpColorSupport;
import com.eteks.sweethome3d.viewcontroller.FurnitureController;

/**
 * Tests finish color mapping for ALP hardscape catalog IDs.
 */
public class AlpHardscapeCatalogUtilsTest extends TestCase {

  public void testResolveFinishColorFromCatalogId() {
    assertEquals(Integer.valueOf(0xFFB25F3E),
        AlpHardscapeCatalogUtils.resolveFinishColor("alp-hrd-step-12x12-terra"));
    assertEquals(Integer.valueOf(0xFF303236),
        AlpHardscapeCatalogUtils.resolveFinishColor("alp-hrd-step-organic-2-charcoal"));
    assertEquals(Integer.valueOf(0xFFD0B28A),
        AlpHardscapeCatalogUtils.resolveFinishColor("alp-hrd-paver-8x4-sand"));
    assertNull(AlpHardscapeCatalogUtils.resolveFinishColor("alp-plt-boxwood"));
    assertNull(AlpHardscapeCatalogUtils.resolveFinishColor("alp-hrd-step-12x12-unknown"));
  }

  public void testApplyDefaultFinishColorFromCatalogPiece() {
    CatalogPieceOfFurniture catalogPiece = new CatalogPieceOfFurniture(
        "alp-hrd-step-12x12-terra", "Stepping Stone 12 x 12 - Terracotta",
        null, null, null, 30.5f, 30.5f, 3.81f, 0f, true, null, "ALP CAD", true, null, null);
    HomePieceOfFurniture piece = new HomePieceOfFurniture(catalogPiece);
    AlpHardscapeCatalogUtils.applyDefaultFinishColor(piece);
    assertEquals(Integer.valueOf(0xFFB25F3E), piece.getColor());
  }

  public void testApplyDefaultFinishColorSkipsExistingColor() {
    CatalogPieceOfFurniture catalogPiece = new CatalogPieceOfFurniture(
        "alp-hrd-step-12x12-terra", "Stepping Stone 12 x 12 - Terracotta",
        null, null, null, 30.5f, 30.5f, 3.81f, 0f, true, null, "ALP CAD", true, null, null);
    HomePieceOfFurniture piece = new HomePieceOfFurniture(catalogPiece);
    piece.setColor(Integer.valueOf(0xFF0000FF));
    AlpHardscapeCatalogUtils.applyDefaultFinishColor(piece);
    assertEquals(Integer.valueOf(0xFF0000FF), piece.getColor());
  }

  public void testResolvePresentationFillTintUsesColorForHardscape() {
    CatalogPieceOfFurniture catalogPiece = new CatalogPieceOfFurniture(
        "alp-hrd-step-12x12-terra", "Stepping Stone 12 x 12 - Terracotta",
        null, null, null, 30.5f, 30.5f, 3.81f, 0f, true, null, "ALP CAD", true, null, null);
    HomePieceOfFurniture piece = new HomePieceOfFurniture(catalogPiece);
    piece.setColor(Integer.valueOf(0xFFB25F3E));
    assertEquals(Integer.valueOf(0xFFB25F3E),
        AlpHardscapeCatalogUtils.resolvePresentationFillTint(piece));
  }

  public void testResolvePresentationFillTintUsesFillColorForPlants() {
    CatalogPieceOfFurniture catalogPiece = new CatalogPieceOfFurniture(
        "alp-plt-boxwood-hedge-3", "Boxwood Hedge - 3 ft",
        null, null, null, 914.4f, 91.4f, 91.4f, 0f, true, null, "ALP CAD", true, null, null);
    HomePieceOfFurniture piece = new HomePieceOfFurniture(catalogPiece);
    piece.setCatalogId("alp-plt-boxwood-hedge-3");
    piece.setColor(Integer.valueOf(0xFFB25F3E));
    piece.setFillColor(Integer.valueOf(0xFFA8C98B));
    assertEquals(Integer.valueOf(0xFFA8C98B),
        AlpHardscapeCatalogUtils.resolvePresentationFillTint(piece));
  }

  public void testResolvePresentationLineTintUsesOutlineColor() {
    HomePieceOfFurniture piece = createHardscapePiece();
    piece.setOutlineColor(Integer.valueOf(0xFFFF0000));
    assertEquals(Integer.valueOf(0xFFFF0000),
        AlpHardscapeCatalogUtils.resolvePresentationLineTint(piece));
  }

  public void testResolvePresentationLineTintDefaultIsNull() {
    HomePieceOfFurniture piece = createHardscapePiece();
    assertNull(AlpHardscapeCatalogUtils.resolvePresentationLineTint(piece));
  }

  public void testShouldDrawPlanOutlineFalseWhenTransparent() {
    HomePieceOfFurniture piece = createHardscapePiece();
    piece.setOutlineColor(AlpColorSupport.TRANSPARENT_COLOR);
    assertFalse(AlpHardscapeCatalogUtils.shouldDrawPlanOutline(piece));
  }

  public void testShouldDrawPlanOutlineTrueByDefault() {
    HomePieceOfFurniture piece = createHardscapePiece();
    assertTrue(AlpHardscapeCatalogUtils.shouldDrawPlanOutline(piece));
  }

  private HomePieceOfFurniture createHardscapePiece() {
    CatalogPieceOfFurniture catalogPiece = new CatalogPieceOfFurniture(
        "alp-hrd-step-12x12-terra", "Stepping Stone 12 x 12 - Terracotta",
        null, null, null, 30.5f, 30.5f, 3.81f, 0f, true, null, "ALP CAD", true, null, null);
    return new HomePieceOfFurniture(catalogPiece);
  }

  public void testCreateHomePieceOfFurnitureAppliesFinishColor() {
    CatalogPieceOfFurniture catalogPiece = new CatalogPieceOfFurniture(
        "alp-hrd-paver-12x3-charcoal", "Paver 12 x 3 - Charcoal",
        null, null, null, 30.5f, 7.6f, 3.81f, 0f, true, null, "ALP CAD", true, null, null);
    FurnitureController furnitureController = new FurnitureController(
        new Home(), new DefaultUserPreferences(), null);
    HomePieceOfFurniture piece = furnitureController.createHomePieceOfFurniture(catalogPiece);
    assertEquals(Integer.valueOf(0xFF303236), piece.getColor());
  }
}
