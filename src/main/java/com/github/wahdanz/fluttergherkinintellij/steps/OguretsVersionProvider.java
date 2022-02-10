package com.github.wahdanz.fluttergherkinintellij.steps;

import org.jetbrains.plugins.cucumber.psi.GherkinStep;

public class OguretsVersionProvider {
  public String getVersion(GherkinStep step) {
    return "3.0";
  }
}