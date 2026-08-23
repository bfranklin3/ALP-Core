/*
 * PlanAssembly.java
 *
 * Sweet Home 3D, Copyright (c) 2024 Space Mushrooms <info@sweethome3d.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */
package com.eteks.sweethome3d.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A lightweight plan assembly for architectural objects (SPIKE-27).
 * Members remain in their native home collections; this object stores membership only.
 */
public class PlanAssembly extends HomeObject implements Cloneable {
  private static final long serialVersionUID = 1L;

  public static final String ID_PREFIX = "planAssembly";

  private String       name;
  private List<String> memberIds;

  /**
   * Creates an assembly with a generated id.
   */
  public PlanAssembly(String name, List<String> memberIds) {
    this(createId(ID_PREFIX), name, memberIds);
  }

  /**
   * Creates an assembly with the given id.
   */
  public PlanAssembly(String id, String name, List<String> memberIds) {
    super(id);
    setName(name);
    setMemberIds(memberIds);
  }

  /**
   * Returns the assembly name.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Sets the assembly name.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns member object ids in assembly order.
   */
  public List<String> getMemberIds() {
    return Collections.unmodifiableList(this.memberIds);
  }

  /**
   * Sets member object ids.
   */
  public void setMemberIds(List<String> memberIds) {
    this.memberIds = memberIds != null
        ? new ArrayList<String>(memberIds)
        : new ArrayList<String>();
  }

  /**
   * Returns a clone of this assembly.
   */
  @Override
  public PlanAssembly clone() {
    PlanAssembly clone = (PlanAssembly)super.clone();
    clone.memberIds = new ArrayList<String>(this.memberIds);
    return clone;
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    if (this.memberIds == null) {
      this.memberIds = new ArrayList<String>();
    }
  }
}
