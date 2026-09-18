/*
 * AlpTransformReplicateTest.java
 *
 * ALP CAD — automated tests for SPIKE-49 precision placement.
 */
package com.eteks.sweethome3d.junit;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.undo.UndoManager;

import junit.framework.TestCase;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.model.AlpPlantMetadata;
import com.eteks.sweethome3d.model.AlpTransformReplicateSupport;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.FurnitureCategory;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.viewcontroller.HomeController;
import com.eteks.sweethome3d.viewcontroller.ViewFactory;

/**
 * Tests SPIKE-49 vector math and controller placement behavior.
 */
public class AlpTransformReplicateTest extends TestCase {
  private static final float EPSILON = 0.01f;

  private UserPreferences preferences;
  private Home home;
  private HomeController homeController;
  private CatalogPieceOfFurniture catalogPiece;

  @Override
  protected void setUp() {
    Locale.setDefault(Locale.ENGLISH);
    this.preferences = new DefaultUserPreferences();
    this.home = new Home();
    ViewFactory viewFactory = new SwingViewFactory();
    this.homeController = new HomeController(this.home, this.preferences, viewFactory);
    this.catalogPiece = findCatalogPiece(this.preferences);
  }

  /**
   * Tests line replication and fit spacing vector math.
   */
  public void testVectorMath() {
    float [] eastStep = AlpTransformReplicateSupport.computeStepVector(150f, 0f);
    assertEquals(150f, eastStep [0], EPSILON);
    assertEquals(0f, eastStep [1], EPSILON);

    float [] northStep = AlpTransformReplicateSupport.computeStepVector(200f, 90f);
    assertEquals(0f, northStep [0], EPSILON);
    assertEquals(200f, northStep [1], EPSILON);

    assertEquals(100f, AlpTransformReplicateSupport.computeFitSpacing(600f, 7), EPSILON);
    assertEquals(0f, AlpTransformReplicateSupport.computeFitSpacing(600f, 1), EPSILON);

    assertEquals(450f,
        AlpTransformReplicateSupport.computeCopyAxisOffset(3, eastStep [0]), EPSILON);
  }

  /**
   * Tests replicate count and final copy positions.
   */
  public void testReplicatePositions() throws InterruptedException, InvocationTargetException {
    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          HomePieceOfFurniture source = addSelectedPiece(100f, 200f, 100f, 60f);
          homeController.replicateSelectedFurnitureInLine(3, 150f, 0f);

          assertEquals("Home should contain source plus three copies",
              4, home.getFurniture().size());
          assertCopyAt(source.getX() + 150f, source.getY(), 1);
          assertCopyAt(source.getX() + 300f, source.getY(), 2);
          assertCopyAt(source.getX() + 450f, source.getY(), 3);
        }
      });
  }

  /**
   * Tests fit-between spacing across a run distance.
   */
  public void testFitCopiesPositions() throws InterruptedException, InvocationTargetException {
    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          HomePieceOfFurniture source = addSelectedPiece(50f, 75f, 80f, 80f);
          homeController.fitSelectedFurnitureCopiesBetweenPoints(4, 300f, 90f);

          assertEquals("Home should contain source plus three fit copies",
              4, home.getFurniture().size());
          assertCopyAt(source.getX(), source.getY() + 100f, 1);
          assertCopyAt(source.getX(), source.getY() + 200f, 2);
          assertCopyAt(source.getX(), source.getY() + 300f, 3);
        }
      });
  }

  /**
   * Tests undo for move and replicate operations.
   */
  public void testUndoMoveAndReplicate() throws InterruptedException, InvocationTargetException {
    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          HomePieceOfFurniture source = addSelectedPiece(200f, 300f, 90f, 70f);
          float originalX = source.getX();
          float originalY = source.getY();
          int originalCount = home.getFurniture().size();

          homeController.moveSelectedFurnitureByDistance(40f, -25f);
          assertEquals(originalX + 40f, source.getX(), EPSILON);
          assertEquals(originalY - 25f, source.getY(), EPSILON);
          undoLastEdit();
          assertEquals(originalX, source.getX(), EPSILON);
          assertEquals(originalY, source.getY(), EPSILON);

          homeController.replicateSelectedFurnitureInLine(2, 120f, 0f);
          assertEquals(originalCount + 2, home.getFurniture().size());
          undoLastEdit();
          assertEquals(originalCount, home.getFurniture().size());
        }
      });
  }

  /**
   * Tests plant spacing default and non-plant fallback spacing.
   */
  public void testPlantDefaultSpacing() throws InterruptedException, InvocationTargetException {
    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          HomePieceOfFurniture plant = new HomePieceOfFurniture(catalogPiece);
          plant.setCatalogId("alp-plt-boxwood");
          plant.setWidth(80f);
          plant.setDepth(80f);
          plant.setProperty(AlpPlantMetadata.PROPERTY_DEFAULT_SPACING, "120");
          home.addPieceOfFurniture(plant);
          home.setSelectedItems(Collections.singletonList(plant));
          assertEquals(120f,
              homeController.getSelectedTransformReplicateDefaultSpacing(), EPSILON);

          HomePieceOfFurniture chair = new HomePieceOfFurniture(catalogPiece);
          chair.setWidth(100f);
          chair.setDepth(80f);
          home.addPieceOfFurniture(chair);
          home.setSelectedItems(Collections.singletonList(chair));
          assertEquals(100f,
              homeController.getSelectedTransformReplicateDefaultSpacing(), EPSILON);
        }
      });
  }

  private void undoLastEdit() {
    try {
      UndoManager undoManager = (UndoManager)TestUtilities.getField(homeController, "undoManager");
      undoManager.undo();
    } catch (NoSuchFieldException ex) {
      fail("Couldn't access HomeController undo manager");
    } catch (IllegalAccessException ex) {
      fail("Couldn't access HomeController undo manager");
    }
  }

  private CatalogPieceOfFurniture findCatalogPiece(UserPreferences preferences) {
    for (FurnitureCategory category : preferences.getFurnitureCatalog().getCategories()) {
      if ("Miscellaneous".equals(category.getName())) {
        for (CatalogPieceOfFurniture piece : category.getFurniture()) {
          if ("Box".equals(piece.getName())) {
            return piece;
          }
        }
      }
    }
    fail("Couldn't find Box catalog piece for transform replicate tests");
    return null;
  }

  private HomePieceOfFurniture addSelectedPiece(float x, float y, float width, float depth) {
    HomePieceOfFurniture piece = new HomePieceOfFurniture(catalogPiece);
    piece.setWidth(width);
    piece.setDepth(depth);
    piece.setX(x);
    piece.setY(y);
    home.addPieceOfFurniture(piece);
    home.setSelectedItems(Arrays.asList(piece));
    return piece;
  }

  private void assertCopyAt(float expectedX, float expectedY, int copyIndex) {
    List<HomePieceOfFurniture> furniture = home.getFurniture();
    HomePieceOfFurniture copy = furniture.get(copyIndex);
    assertEquals("Copy " + copyIndex + " X", expectedX, copy.getX(), EPSILON);
    assertEquals("Copy " + copyIndex + " Y", expectedY, copy.getY(), EPSILON);
  }
}
