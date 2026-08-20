/*
 * AlpTextureCatalogSupport.java
 *
 * ALP CAD — texture library/category helpers (SPIKE-32 Phase 2).
 */
package com.eteks.sweethome3d.swing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.eteks.sweethome3d.model.CatalogTexture;
import com.eteks.sweethome3d.model.HomeTexture;
import com.eteks.sweethome3d.model.LevelCategory;
import com.eteks.sweethome3d.model.Library;
import com.eteks.sweethome3d.model.SimpleLibrary;
import com.eteks.sweethome3d.model.TexturesCatalog;
import com.eteks.sweethome3d.model.TexturesCategory;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.viewcontroller.TextureChoiceController;

/**
 * Helpers for texture library and category selection in the inspector.
 */
final class AlpTextureCatalogSupport {
  /** Expected id for the ALP landscape plan texture library when it is installed. */
  static final String ALP_LANDSCAPE_PLAN_LIBRARY_ID = "ALP#LandscapePlan";

  private static String sessionLibraryId;
  private static String sessionCategoryName;

  private AlpTextureCatalogSupport() {
  }

  static void rememberSessionSelection(String libraryId, String categoryName) {
    sessionLibraryId = libraryId;
    sessionCategoryName = categoryName;
  }

  static String getAllCategoriesLabel(UserPreferences preferences) {
    return preferences.getLocalizedString(AlpTextureCatalogSupport.class, "allCategories.text");
  }

  static String getUserImportedLibraryName(UserPreferences preferences) {
    return preferences.getLocalizedString(AlpTextureCatalogSupport.class, "userImportedLibrary.text");
  }

  static List<Library> getTextureLibraries(UserPreferences preferences) {
    List<Library> libraries = new ArrayList<Library>();
    for (Library library : preferences.getLibraries()) {
      if (UserPreferences.TEXTURES_LIBRARY_TYPE.equals(library.getType())) {
        libraries.add(library);
      }
    }
    if (hasUserImportedTextures(preferences.getTexturesCatalog())
        && findLibraryById(libraries, CatalogTexture.USER_IMPORTED_LIBRARY_ID) == null) {
      libraries.add(new SimpleLibrary(null, UserPreferences.TEXTURES_LIBRARY_TYPE,
          CatalogTexture.USER_IMPORTED_LIBRARY_ID,
          getUserImportedLibraryName(preferences), null, null, null, null));
    }
    return libraries;
  }

  static boolean hasUserImportedTextures(TexturesCatalog catalog) {
    for (TexturesCategory category : catalog.getCategories()) {
      for (CatalogTexture texture : category.getTextures()) {
        if (texture.isModifiable()) {
          return true;
        }
      }
    }
    return false;
  }

  static List<String> getCategoryNamesForLibrary(UserPreferences preferences, String libraryId) {
    if (libraryId == null) {
      return Collections.emptyList();
    }
    Set<String> categoryNames = new LinkedHashSet<String>();
    for (TexturesCategory category : preferences.getTexturesCatalog().getCategories()) {
      for (CatalogTexture texture : category.getTextures()) {
        if (libraryId.equals(texture.getLibraryId())) {
          categoryNames.add(category.getName());
          break;
        }
      }
    }
    return new ArrayList<String>(categoryNames);
  }

  static CatalogTexture findCatalogTexture(UserPreferences preferences, String catalogId) {
    if (catalogId == null) {
      return null;
    }
    for (TexturesCategory category : preferences.getTexturesCatalog().getCategories()) {
      for (CatalogTexture texture : category.getTextures()) {
        if (catalogId.equals(texture.getId())) {
          return texture;
        }
      }
    }
    return null;
  }

  static Library findLibraryById(List<Library> libraries, String libraryId) {
    if (libraryId == null) {
      return null;
    }
    for (Library library : libraries) {
      if (libraryId.equals(library.getId())) {
        return library;
      }
    }
    return null;
  }

  static Library findAlpLandscapePlanLibrary(List<Library> libraries) {
    for (Library library : libraries) {
      if (isAlpLandscapePlanLibrary(library)) {
        return library;
      }
    }
    return null;
  }

  static boolean isAlpLandscapePlanLibrary(Library library) {
    if (library == null) {
      return false;
    }
    if (ALP_LANDSCAPE_PLAN_LIBRARY_ID.equals(library.getId())) {
      return true;
    }
    String name = library.getName();
    return name != null && name.contains("ALP Landscape");
  }

  static String getPreferredCategoryName(LevelCategory levelCategory) {
    if (levelCategory == LevelCategory.SITE) {
      return "Hardscape";
    } else if (levelCategory == LevelCategory.PLANTING) {
      return "Planting";
    } else {
      return null;
    }
  }

  static Library resolveDefaultLibrary(UserPreferences preferences,
                                       HomeTexture floorTexture,
                                       LevelCategory levelCategory) {
    List<Library> libraries = getTextureLibraries(preferences);
    if (libraries.isEmpty()) {
      return null;
    }

    if (floorTexture != null && floorTexture.getCatalogId() != null) {
      CatalogTexture catalogTexture = findCatalogTexture(preferences, floorTexture.getCatalogId());
      if (catalogTexture != null && catalogTexture.getLibraryId() != null) {
        Library library = findLibraryById(libraries, catalogTexture.getLibraryId());
        if (library != null) {
          return library;
        }
      }
    }

    if (sessionLibraryId != null) {
      Library library = findLibraryById(libraries, sessionLibraryId);
      if (library != null) {
        return library;
      }
    }

    if (levelCategory == LevelCategory.SITE || levelCategory == LevelCategory.PLANTING) {
      Library alpLibrary = findAlpLandscapePlanLibrary(libraries);
      if (alpLibrary != null) {
        return alpLibrary;
      }
    }

    String preferredLibraryId = preferences.getDefaultTexturesLibraryId();
    if (preferredLibraryId != null) {
      Library library = findLibraryById(libraries, preferredLibraryId);
      if (library != null) {
        return library;
      }
    }

    if (libraries.size() == 1) {
      return libraries.get(0);
    }

    return libraries.get(0);
  }

  static String resolveDefaultCategoryName(UserPreferences preferences,
                                           HomeTexture floorTexture,
                                           LevelCategory levelCategory,
                                           String libraryId) {
    if (floorTexture != null && floorTexture.getCatalogId() != null) {
      CatalogTexture catalogTexture = findCatalogTexture(preferences, floorTexture.getCatalogId());
      if (catalogTexture != null
          && catalogTexture.getCategory() != null
          && libraryId != null
          && libraryId.equals(catalogTexture.getLibraryId())) {
        return catalogTexture.getCategory().getName();
      }
    }

    if (sessionCategoryName != null
        && categoryExistsInLibrary(preferences, libraryId, sessionCategoryName)) {
      return sessionCategoryName;
    }

    String preferredCategoryName = getPreferredCategoryName(levelCategory);
    if (preferredCategoryName != null
        && categoryExistsInLibrary(preferences, libraryId, preferredCategoryName)) {
      return preferredCategoryName;
    }

    return null;
  }

  static boolean categoryExistsInLibrary(UserPreferences preferences,
                                         String libraryId,
                                         String categoryName) {
    if (libraryId == null || categoryName == null) {
      return false;
    }
    for (String name : getCategoryNamesForLibrary(preferences, libraryId)) {
      if (categoryName.equals(name)) {
        return true;
      }
    }
    return false;
  }

  static String getCategoryNameForSelection(UserPreferences preferences, Object selectedItem) {
    if (selectedItem == null) {
      return null;
    }
    String allCategoriesLabel = getAllCategoriesLabel(preferences);
    String selectedCategory = selectedItem.toString();
    return allCategoriesLabel.equals(selectedCategory) ? null : selectedCategory;
  }

  static void applyTextureFilters(TextureChoiceController controller,
                                  String libraryId,
                                  String categoryName) {
    controller.setLibraryIdFilter(libraryId);
    controller.setCategoryNameFilter(categoryName);
  }
}
