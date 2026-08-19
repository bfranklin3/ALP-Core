/*
 * AlpOutputPresets.java
 *
 * Sweet Home 3D / ALP CAD — draft vs presentation output presets (SPIKE-20).
 */
package com.eteks.sweethome3d.model;

import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterJob;
import java.util.List;

/**
 * Helpers for ALP site-plan print and export presets.
 */
public final class AlpOutputPresets {
  /**
   * Output preset types.
   */
  public enum Preset {
    /** B&amp;W-friendly plan output with minimal chrome. */
    DRAFT,
    /** Color plan view with area fills for client deliverables. */
    PRESENTATION
  }

  private AlpOutputPresets() {
  }

  /**
   * Returns whether the given preset enables draft (monochrome) plan view.
   */
  public static boolean isDraftMode(Preset preset) {
    return preset == Preset.DRAFT;
  }

  /**
   * Builds {@link HomePrint} settings for a preset, preserving page size and
   * orientation from {@code currentPrint} when present.
   */
  public static HomePrint createPresetPrint(HomePrint currentPrint, Preset preset) {
    HomePrint base = currentPrint != null ? currentPrint : createDefaultPrint();
    boolean draft = preset == Preset.DRAFT;
    return new HomePrint(
        base.getPaperOrientation(),
        base.getPaperWidth(),
        base.getPaperHeight(),
        base.getPaperTopMargin(),
        base.getPaperLeftMargin(),
        base.getPaperBottomMargin(),
        base.getPaperRightMargin(),
        false,
        true,
        (List<Level>) null,
        false,
        null,
        draft ? "" : formatOrEmpty(base.getHeaderFormat()),
        draft ? "" : formatOrEmpty(base.getFooterFormat()));
  }

  private static String formatOrEmpty(String format) {
    return format != null ? format : "";
  }

  /**
   * Returns landscape ALP defaults when the home has no saved print settings yet.
   */
  private static HomePrint createDefaultPrint() {
    PageFormat pageFormat = PrinterJob.getPrinterJob().defaultPage();
    pageFormat.setOrientation(PageFormat.LANDSCAPE);
    pageFormat = PrinterJob.getPrinterJob().validatePage(pageFormat);
    return fromPageFormat(pageFormat);
  }

  private static HomePrint fromPageFormat(PageFormat pageFormat) {
    HomePrint.PaperOrientation paperOrientation;
    switch (pageFormat.getOrientation()) {
      case PageFormat.LANDSCAPE :
        paperOrientation = HomePrint.PaperOrientation.LANDSCAPE;
        break;
      case PageFormat.REVERSE_LANDSCAPE :
        paperOrientation = HomePrint.PaperOrientation.REVERSE_LANDSCAPE;
        break;
      default :
        paperOrientation = HomePrint.PaperOrientation.PORTRAIT;
        break;
    }
    Paper paper = pageFormat.getPaper();
    return new HomePrint(paperOrientation, (float) paper.getWidth(), (float) paper.getHeight(),
        (float) paper.getImageableY(), (float) paper.getImageableX(),
        (float) (paper.getHeight() - paper.getImageableHeight() - paper.getImageableY()),
        (float) (paper.getWidth() - paper.getImageableWidth() - paper.getImageableX()),
        false, true, null, false, null, "", "");
  }
}
