/*
 * LevelPlantTakeoff.java
 *
 * ALP CAD — plant schedule participation for a level (SPIKE-30).
 */
package com.eteks.sweethome3d.model;

/**
 * Describes how plant symbols on a level participate in takeoff.
 */
public enum LevelPlantTakeoff {
  EXCLUDE,
  PROPOSED,
  EXISTING;

  /**
   * Parses a persisted XML attribute value.
   */
  public static LevelPlantTakeoff fromXmlAttribute(String value) {
    if (value != null) {
      try {
        return valueOf(value);
      } catch (IllegalArgumentException ex) {
      }
    }
    return EXCLUDE;
  }
}
