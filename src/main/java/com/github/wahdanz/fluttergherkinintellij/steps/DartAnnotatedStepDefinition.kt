package com.github.wahdanz.fluttergherkinintellij.steps

import com.github.wahdanz.fluttergherkinintellij.CucumberDartUtil.findDartAnnotationText
import com.intellij.psi.PsiElement
import com.github.wahdanz.fluttergherkinintellij.steps.AbstractDartStepDefinition
import com.jetbrains.lang.dart.psi.DartMethodDeclaration
import com.github.wahdanz.fluttergherkinintellij.CucumberDartUtil
import com.jetbrains.lang.dart.psi.DartFunctionDeclarationWithBodyOrNative

open class DartAnnotatedStepDefinition(stepDef: PsiElement, private val myAnnotationClassName: String) :
    AbstractDartStepDefinition(stepDef) {
    override fun getCucumberRegexFromElement(element: PsiElement): String? {
        return if (element !is DartFunctionDeclarationWithBodyOrNative) {
            null
        } else findDartAnnotationText(element)
    }
}