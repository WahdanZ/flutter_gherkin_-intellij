package com.github.wahdanz.fluttergherkinintellij.steps

import com.github.wahdanz.fluttergherkinintellij.CucumberDartUtil.findDartAnnotationText
import com.intellij.psi.PsiElement
import com.github.wahdanz.fluttergherkinintellij.steps.AbstractDartStepDefinition
import com.jetbrains.lang.dart.psi.DartMethodDeclaration
import com.github.wahdanz.fluttergherkinintellij.CucumberDartUtil

open class DartAnnotatedStepDefinition(stepDef: PsiElement, private val myAnnotationClassName: String) :
    AbstractDartStepDefinition(stepDef) {
    override fun getCucumberRegexFromElement(element: PsiElement): String? {
        if (element == null) {
            return null
        }
        return if (element !is DartMethodDeclaration) {
            null
        } else findDartAnnotationText(element)
    }
}