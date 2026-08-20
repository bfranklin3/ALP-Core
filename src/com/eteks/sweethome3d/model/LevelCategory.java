/*
 * LevelCategory.java
 *
 * ALP CAD — layer purpose metadata (SPIKE-30).
 */
package com.eteks.sweethome3d.model;

/**
 * Describes what kind of content belongs on a home level.
 */
public enum LevelCategory {
  GENERAL,
  REFERENCE,
  SITE,
  PLANTING,
  ANNOTATION;

  /**
   * Parses a persisted XML attribute value.
   */
  public static LevelCategory fromXmlAttribute(String value) {
    if (value != null) {
      try {
        return valueOf(value);
      } catch (IllegalArgumentException ex) {
      }
    }
    return GENERAL;
  }
}
