// Copyright 2000-2018 JetBrains s.r.o.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.github.wahdanz.fluttergherkinintellij.steps.search

import com.intellij.util.QueryExecutor
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.jetbrains.lang.dart.psi.DartMethodDeclaration
import com.github.wahdanz.fluttergherkinintellij.CucumberDartUtil
import com.intellij.openapi.util.Computable
import com.intellij.util.Processor
import org.jetbrains.plugins.cucumber.CucumberUtil

class CucumberJavaStepDefinitionSearch : QueryExecutor<PsiReference?, ReferencesSearch.SearchParameters> {
    override fun execute(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference?>
    ): Boolean {
        return ApplicationManager.getApplication().runReadAction(Computable {
            val myElement = queryParameters.elementToSearch
            if (myElement.parent is DartMethodDeclaration) {
                val dc = myElement.parent as DartMethodDeclaration
                val regexp = CucumberDartUtil.findDartAnnotationText(dc)
                if (regexp != null) {
                    return@Computable CucumberUtil.findGherkinReferencesToElement(
                        myElement,
                        regexp,
                        consumer,
                        queryParameters.effectiveSearchScope
                    )
                }
            }
            true
        })
    }
}