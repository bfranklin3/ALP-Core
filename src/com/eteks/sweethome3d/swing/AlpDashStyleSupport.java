/*
 * AlpDashStyleSupport.java
 *
 * ALP CAD — landscape line type presets (SPIKE-34).
 */
package com.eteks.sweethome3d.swing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.eteks.sweethome3d.model.Polyline;
import com.eteks.sweethome3d.model.UserPreferences;

/**
 * Curated dash-style lists and display names for ALP landscape presets.
 */
final class AlpDashStyleSupport {

  private static final Polyline.DashStyle[] ALP_PRESETS = {
      Polyline.DashStyle.SOLID,
      Polyline.DashStyle.PROPERTY_LINE,
      Polyline.DashStyle.SETBACK,
      Polyline.DashStyle.UTILITY,
      Polyline.DashStyle.PHASE,
      Polyline.DashStyle.HIDDEN,
  };

  private static final Polyline.DashStyle[] LEGACY_STYLES = {
      Polyline.DashStyle.DOT,
      Polyline.DashStyle.DASH,
      Polyline.DashStyle.DASH_DOT,
      Polyline.DashStyle.DASH_DOT_DOT,
  };

  private AlpDashStyleSupport() {
  }

  /**
   * Returns dash styles for combo boxes: ALP presets first, stock patterns at the bottom.
   * {@link Polyline.DashStyle#CUSTOMIZED} is included only when it is the current value.
   */
  static List<Polyline.DashStyle> getDashStyleChoices(Polyline.DashStyle current) {
    List<Polyline.DashStyle> choices = new ArrayList<Polyline.DashStyle>();
    choices.addAll(Arrays.asList(ALP_PRESETS));
    choices.addAll(Arrays.asList(LEGACY_STYLES));
    if (current == Polyline.DashStyle.CUSTOMIZED && !choices.contains(current)) {
      choices.add(current);
    }
    return choices;
  }

  /**
   * Same as {@link #getDashStyleChoices(Polyline.DashStyle)} with a leading
   * <code>null</code> entry for mixed multi-selection in the inspector.
   */
  static List<Polyline.DashStyle> getDashStyleChoicesForCombo(Polyline.DashStyle current,
                                                              boolean includeNull) {
    List<Polyline.DashStyle> choices = getDashStyleChoices(current);
    if (includeNull) {
      choices.add(0, null);
    }
    return choices;
  }

  static String getDisplayName(UserPreferences preferences, Polyline.DashStyle dashStyle) {
    if (dashStyle == null) {
      return "";
    }
    String key = "DashStyle." + dashStyle.name() + ".text";
    String localized = preferences.getLocalizedString(PolylinePanel.class, key);
    if (localized.equals(key)) {
      return dashStyle.name();
    }
    return localized;
  }
}
