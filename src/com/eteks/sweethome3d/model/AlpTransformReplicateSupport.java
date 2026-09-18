/*
 * AlpTransformReplicateSupport.java
 *
 * ALP CAD — plan-space vector math for SPIKE-49 precision placement.
 */
package com.eteks.sweethome3d.model;

/**
 * Computes replication vectors for SPIKE-49 move and linear replicate commands.
 */
public final class AlpTransformReplicateSupport {
  private AlpTransformReplicateSupport() {
  }

  /**
   * Returns the center-to-center step vector for a given spacing and angle.
   * Angle {@code 0} points east/right on the plan.
   */
  public static float [] computeStepVector(float spacing, float angleInDegrees) {
    float angleInRadians = (float)Math.toRadians(angleInDegrees);
    return new float [] {
        (float)Math.cos(angleInRadians) * spacing,
        (float)Math.sin(angleInRadians) * spacing
    };
  }

  /**
   * Returns center-to-center spacing when distributing {@code totalCount} items
   * (including the source at the start) across {@code distance}.
   */
  public static float computeFitSpacing(float distance, int totalCount) {
    if (totalCount < 2 || distance <= 0) {
      return 0f;
    }
    return distance / (totalCount - 1);
  }

  /**
   * Returns the plan-space offset for copy {@code copyIndex} along one axis,
   * where {@code copyIndex} is {@code 1..copies}.
   */
  public static float computeCopyAxisOffset(int copyIndex, float stepComponent) {
    return stepComponent * copyIndex;
  }
}
