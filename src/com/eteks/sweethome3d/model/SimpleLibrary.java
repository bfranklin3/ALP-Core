/*
 * SimpleLibrary.java
 *
 * ALP CAD — lightweight {@link Library} implementation.
 */
package com.eteks.sweethome3d.model;

/**
 * Basic immutable library description.
 */
public class SimpleLibrary implements Library {
  private final String location;
  private final String type;
  private final String id;
  private final String name;
  private final String description;
  private final String version;
  private final String license;
  private final String provider;

  public SimpleLibrary(String location, String type,
                       String id, String name, String description,
                       String version, String license, String provider) {
    this.location = location;
    this.type = type;
    this.id = id;
    this.name = name;
    this.description = description;
    this.version = version;
    this.license = license;
    this.provider = provider;
  }

  public String getLocation() {
    return this.location;
  }

  public String getId() {
    return this.id;
  }

  public String getType() {
    return this.type;
  }

  public String getName() {
    return this.name;
  }

  public String getDescription() {
    return this.description;
  }

  public String getVersion() {
    return this.version;
  }

  public String getLicense() {
    return this.license;
  }

  public String getProvider() {
    return this.provider;
  }
}
