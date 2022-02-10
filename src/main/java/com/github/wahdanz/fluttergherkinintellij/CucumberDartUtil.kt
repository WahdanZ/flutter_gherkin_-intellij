package com.github.wahdanz.fluttergherkinintellij

import com.intellij.openapi.project.guessProjectDir
import org.jetbrains.plugins.cucumber.MapParameterTypeManager
import com.github.wahdanz.fluttergherkinintellij.CucumberDartUtil
import com.intellij.psi.PsiElement
import com.github.wahdanz.fluttergherkinintellij.steps.reference.CucumberJavaAnnotationProvider
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.PsiModificationTracker
import com.jetbrains.lang.dart.psi.*
import java.util.Collections
import java.util.HashMap
import java.util.regex.Pattern

object CucumberDartUtil {
    const val PARAMETER_TYPE_CLASS = "io.cucumber.cucumberexpressions.ParameterType"
    private var DART_PARAMETER_TYPES = mutableMapOf<String, String>()
    private val BEGIN_ANCHOR = Pattern.compile("^\\^.*")
    private val END_ANCHOR = Pattern.compile(".*\\$$")
    private val SCRIPT_STYLE_REGEXP = Pattern.compile("^/(.*)/$")
    private val PARENTHESIS = Pattern.compile("\\(([^)]+)\\)")
    private val ALPHA = Pattern.compile("[a-zA-Z]+")
    private var STANDARD_PARAMETER_TYPES = mutableMapOf<String, String>()
    private var DEFAULT: MapParameterTypeManager? = null

    /**
     * Checks if expression should be considered as a CucumberExpression or as a RegEx
     * @see [https://github.com/cucumber/cucumber/blob/master/cucumber-expressions/java/heuristics.adoc](http://google.com)
     */
    @JvmStatic
    fun isCucumberExpression(expression: String): Boolean {
        var m = BEGIN_ANCHOR.matcher(expression)
        if (m.find()) {
            return false
        }
        m = END_ANCHOR.matcher(expression)
        if (m.find()) {
            return false
        }
        m = SCRIPT_STYLE_REGEXP.matcher(expression)
        if (m.find()) {
            return false
        }
        m = PARENTHESIS.matcher(expression)
        if (m.find()) {
            val insideParenthesis = m.group(1)
            return ALPHA.matcher(insideParenthesis).lookingAt()
        }
        return true
    }

    @JvmStatic
    fun getCucumberPendingExceptionFqn(context: PsiElement): String {
        return "PendingException"
    }

    @JvmStatic
    fun isStepDefinition(method: DartMethodDeclaration): Boolean {
        return findDartCucumberAnnotation(method) != null
    }

    fun isHook(method: DartMethodDeclaration): Boolean {
        return CucumberJavaAnnotationProvider.HOOK_MARKERS.contains(findDartCucumberAnnotation(method))
    }

    @JvmStatic
    fun isStepDefinitionClass(clazz: DartClassDefinition): Boolean {
        return clazz.classBody != null && clazz.classBody!!.classMembers != null &&
                clazz.classBody!!.classMembers!!
                    .methodDeclarationList.stream()
                    .anyMatch { m: DartMethodDeclaration -> findDartCucumberAnnotation(m) != null }
    }

    @JvmStatic
    fun findDartAnnotationText(dc: DartFunctionDeclarationWithBodyOrNative): String? {
        return dc.metadataList
            .filter { isDartMetadataCucumberAnnotation(it) }
            .map { meta: DartMetadata -> stripQuotes(refExpression(meta)) }
            .firstOrNull()
    }

    // this is where we figure out what is inside the @Given/When/Then
    // so we can allow IDEA to cross link it with the cucumber file
    fun refExpression(meta: DartMetadata): String {
        return meta.referenceExpression.nextSibling.text
    }

    @JvmStatic
    fun isDartMetadataCucumberAnnotation(meta: DartMetadata): Boolean {
        return CucumberJavaAnnotationProvider.HOOK_MARKERS.contains(meta.referenceExpression.firstChild.text) ||
                CucumberJavaAnnotationProvider.STEP_MARKERS.contains(meta.referenceExpression.firstChild.text)
    }

    @JvmStatic
    fun isTextOfCucumberAnnotation(d: DartStringLiteralExpression?): Boolean {
        return d != null && d.parent != null && d.parent.parent != null && d.parent.parent.parent != null &&
                d.parent.parent.parent is DartMetadata
    }

    internal fun stripQuotes(str: String): String {
        var str = str
        val original = str
        str = str.trim { it <= ' ' }
        // strip off brackets
        if (str.startsWith("(") && str.endsWith(")")) {
            str = str.substring(1, str.length - 1)
        }
        str = str.replace("\n", " ").replace("\r", "").trim { it <= ' ' }
        println(String.format("`%s` vs `%s`", original, str))
        // raw string?
        if (str.startsWith("r\"") || str.startsWith("r'")) {
            str = str.substring(2)
        }
        if (str.startsWith("\"") || str.startsWith("'")) {
            str = str.substring(1)
        }
        if (str.endsWith("\"") || str.endsWith("'")) {
            str = str.substring(0, str.length - 1)
        }

        // undo the backslash that prevents escaping in Dart
        str = str.replace("\\$", "$")
        return str
    }

    @JvmStatic
    fun findDartCucumberAnnotation(dc: DartMethodDeclaration): String? {
        return dc.metadataList.stream().filter { isDartMetadataCucumberAnnotation(it) }
            .map { meta: DartMetadata -> meta.referenceExpression.firstChild.text }
            .findFirst()
            .orElse(null)
    }

    @JvmStatic
    fun getAllParameterTypes(module: Module): MapParameterTypeManager? {
        val project = module.project
        val manager = PsiManager.getInstance(project)
        val projectDir = project.guessProjectDir()
        val psiDirectory = if (projectDir != null) manager.findDirectory(projectDir) else null
        return if (psiDirectory != null) {
            CachedValuesManager.getCachedValue(
                psiDirectory
            ) {
                CachedValueProvider.Result.create(
                    doGetAllParameterTypes(module),
                    PsiModificationTracker.MODIFICATION_COUNT
                )
            }
        } else DEFAULT
    }

    private fun doGetAllParameterTypes(module: Module): MapParameterTypeManager {
        val values: MutableMap<String, String> = HashMap()
        values.putAll(STANDARD_PARAMETER_TYPES)
        values.putAll(DART_PARAMETER_TYPES)
        return MapParameterTypeManager(values)
    }

    /**
     * Checks if library with CucumberExpressions library attached to the project.
     * @return true if step definitions should be written in Cucumber Expressions (since Cucumber v 3.0),
     * false in case of old-style Regexp step definitions.
     */
    fun isCucumberExpressionsAvailable(context: PsiElement): Boolean {
        return true
    }

    init {
        val standardParameterTypes  = mutableMapOf<String, String>()
        standardParameterTypes["int"] = "-?\\d+"
        standardParameterTypes["float"] = "-?\\d*[.,]?\\d+"
        standardParameterTypes["word"] = "[^\\s]+"
        standardParameterTypes["string"] = "\"(?:[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"|'(?:[^'\\\\]*(?:\\\\.[^'\\\\]*)*)'"
        standardParameterTypes[""] = "(.*)"
        STANDARD_PARAMETER_TYPES = standardParameterTypes
        val dartParameterTypes = mutableMapOf<String, String>()
        // only add the things that aren't there
        dartParameterTypes["double"] = STANDARD_PARAMETER_TYPES["float"] ?: "-?\\d*[.,]?\\d+"
        DART_PARAMETER_TYPES = dartParameterTypes
        DEFAULT = MapParameterTypeManager(STANDARD_PARAMETER_TYPES)
    }
}