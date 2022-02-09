package com.github.wahdanz.fluttergherkinintellij.cucumber.dart;

import com.intellij.CommonBundle;
import org.jetbrains.annotations.NotNull;

public class CucumberDartBundle {

  public static String message(@NotNull String key, @NotNull Object... params) {
    return CommonBundle.message(key, params);
  }

  private CucumberDartBundle() {
  }
}
