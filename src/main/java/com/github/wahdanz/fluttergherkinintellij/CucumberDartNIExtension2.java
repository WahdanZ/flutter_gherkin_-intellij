package com.github.wahdanz.fluttergherkinintellij;

import com.github.wahdanz.fluttergherkinintellij.steps.DartStepDefinitionCreator;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.lang.dart.DartFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.BDDFrameworkType;
import org.jetbrains.plugins.cucumber.StepDefinitionCreator;
import org.jetbrains.plugins.cucumber.psi.GherkinFile;
import org.jetbrains.plugins.cucumber.steps.AbstractCucumberExtension;
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition;

import java.util.Collection;
import java.util.List;

public class CucumberDartNIExtension2 extends AbstractCucumberExtension {

    @Override
    public boolean isStepLikeFile(@NotNull PsiElement psiElement, @NotNull PsiElement psiElement1) {
        return false;
    }

    @Override
    public boolean isWritableStepLikeFile(@NotNull PsiElement psiElement, @NotNull PsiElement psiElement1) {
        return false;
    }

    @NotNull
    @Override
    public BDDFrameworkType getStepFileType() {
        return new BDDFrameworkType(DartFileType.INSTANCE, "Dart 2");
    }

    @NotNull
    @Override
    public StepDefinitionCreator getStepDefinitionCreator() {
        return new DartStepDefinitionCreator();
    }

    @Override
    public List<AbstractStepDefinition> loadStepsFor(@Nullable PsiFile psiFile, @NotNull Module module) {
        return null;
    }

    @Override
    public Collection<? extends PsiFile> getStepDefinitionContainers(@NotNull GherkinFile gherkinFile) {
        return null;
    }
}
