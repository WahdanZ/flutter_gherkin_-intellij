// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.wahdanz.fluttergherkinintellij.cucumber.dart.steps.factory;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import com.github.wahdanz.fluttergherkinintellij.cucumber.dart.steps.AbstractDartStepDefinition;
import com.github.wahdanz.fluttergherkinintellij.cucumber.dart.steps.DartStep2XDefinition;

public class DartStep2xDefinitionFactory extends DartStepDefinitionFactory {
  @Override
  public AbstractDartStepDefinition buildStepDefinition(@NotNull PsiElement element, @NotNull String annotationClassName) {
    return new DartStep2XDefinition(element, annotationClassName);
  }
}
