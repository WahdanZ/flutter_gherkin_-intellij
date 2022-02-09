package com.github.wahdanz.fluttergherkinintellij.steps.snippets

import com.github.wahdanz.fluttergherkinintellij.steps.snippets.ParamSnippet
import gherkin.pickles.PickleStep
import cucumber.runtime.snippets.FunctionNameGenerator
import gherkin.pickles.PickleTable
import cucumber.runtime.snippets.ArgumentPattern
import java.lang.StringBuffer
import com.github.wahdanz.fluttergherkinintellij.steps.snippets.ParamSnippet.ArgumentParam
import cucumber.api.DataTable
import gherkin.pickles.PickleString
import java.lang.StringBuilder
import java.text.MessageFormat
import java.util.ArrayList
import java.util.regex.Matcher
import java.util.regex.Pattern

class SnippetGenerator(private val snippet: ParamSnippet) {
    fun getSnippet(step: PickleStep, keyword: String, functionNameGenerator: FunctionNameGenerator?): String {
        val x =
            "StepDefinitionGeneric {2}() '{'\n return{0}<{3}FlutterWidgetTesterWorld>('\n {1},({4},context) async async '{'\n \n'}'\n"
        return MessageFormat.format(
            snippet.template(),
            getGerkinKeyWord(keyword, argumentTypes(step).size),
            snippet.escapePattern(patternFor(step.text)),
            functionName(step.text, functionNameGenerator),
            snippet.paramArguments(argumentTypes(step)),
            REGEXP_HINT,
            if (!step.argument.isEmpty() && step.argument[0] is PickleTable) snippet.tableHint() else ""
        )
    }

    fun getGerkinKeyWord(keyword: String, count: Int): String {
        return keyword + if (count > 0) count else ""
    }

    fun patternFor(stepName: String?): String? {
        var pattern = stepName
        for (escapePattern in ESCAPE_PATTERNS) {
            val m = escapePattern.matcher(pattern)
            val replacement = Matcher.quoteReplacement(escapePattern.toString())
            pattern = m.replaceAll(replacement)
        }
        for (argumentPattern in argumentPatterns()) {
            pattern = argumentPattern.replaceMatchesWithGroups(pattern)
        }
        if (snippet.namedGroupStart() != null) {
            pattern = withNamedGroups(pattern)
        }
        return pattern
    }

    private fun functionName(sentence: String, functionNameGenerator: FunctionNameGenerator?): String? {
        var sentence: String? = sentence
        if (functionNameGenerator == null) {
            return null
        }
        for (argumentPattern in argumentPatterns()) {
            sentence = argumentPattern.replaceMatchesWithSpace(sentence)
        }
        return functionNameGenerator.generateFunctionName(sentence)
    }

    private fun withNamedGroups(snippetPattern: String?): String {
        val m = GROUP_PATTERN.matcher(snippetPattern)
        val sb = StringBuffer()
        var n = 1
        while (m.find()) {
            m.appendReplacement(sb, "(" + snippet.namedGroupStart() + n++ + snippet.namedGroupEnd())
        }
        m.appendTail(sb)
        return sb.toString()
    }

    private fun argumentTypes(step: PickleStep): List<ArgumentParam> {
        val name = step.text
        val argTypes: MutableList<ArgumentParam> = ArrayList()
        val matchers = arrayOfNulls<Matcher>(argumentPatterns().size)
        for (i in argumentPatterns().indices) {
            matchers[i] = argumentPatterns()[i].pattern().matcher(name)
        }
        var pos = 0
        do {
            var matchedLength = 1
            for (i in matchers.indices) {
                val m = matchers[i]!!.region(pos, name.length)
                if (m.lookingAt()) {
                    val typeForSignature = argumentPatterns()[i].type()
                    matchedLength = m.group().length
                    val pName = name.subSequence(pos, pos + matchedLength).toString()
                    val param = ArgumentParam.Builder().clazz(typeForSignature)
                    if (pName.startsWith("\"<")) {
                        param.name(pName.substring(2, pName.length - 2))
                    } else if (pName.startsWith("<")) {
                        param.name(pName.substring(1, pName.length - 1))
                    }
                    argTypes.add(param.build())
                    break
                }
            }
            pos += matchedLength
        } while (pos != name.length)
        if (!step.argument.isEmpty()) {
            val arg = step.argument[0]
            if (arg is PickleString) {
                argTypes.add(ArgumentParam.Builder().clazz(String::class.java).build())
            }
            if (arg is PickleTable) {
                argTypes.add(ArgumentParam.Builder().clazz(DataTable::class.java).build())
            }
        }
        return argTypes
    }

    fun argumentPatterns(): Array<ArgumentPattern> {
        return DEFAULT_ARGUMENT_PATTERNS
    }

    companion object {
        private val DEFAULT_ARGUMENT_PATTERNS = arrayOf(
            ArgumentPattern(Pattern.compile("([-+]?\\d+)"), "{int}", Integer.TYPE),
            ArgumentPattern(
                Pattern.compile("([+-]?([0-9]*[.])?[0-9]+)"),
                "{float}",
                java.lang.Float.TYPE
            ),  //		new ArgumentPattern(Pattern.compile("([[-+]?\\d+|<\\w+?>])"), "{int}",Integer.TYPE),
            //		new ArgumentPattern(Pattern.compile("([[-+]?[0-9]*\\.?[0-9]+|<\\w+?>])"), "{float}",Float.TYPE),
            ArgumentPattern(Pattern.compile("\"([^\"]*)\""), "{string}", String::class.java),
            ArgumentPattern(Pattern.compile("<([^>]*)>"), "{string}", String::class.java)
        )
        private val GROUP_PATTERN = Pattern.compile("\\(")
        private val ESCAPE_PATTERNS = arrayOf(
            Pattern.compile("\\$"),
            Pattern.compile("\\("),
            Pattern.compile("\\)"),
            Pattern.compile("\\["),
            Pattern.compile("\\]"),
            Pattern.compile("\\?"),
            Pattern.compile("\\*"),
            Pattern.compile("\\+"),
            Pattern.compile("\\."),
            Pattern.compile("\\^")
        )
        private const val REGEXP_HINT = "Write code here that turns the phrase above into concrete actions"
        fun untypedArguments(argumentTypes: List<Class<*>?>): String {
            val sb = StringBuilder()
            for (n in argumentTypes.indices) {
                if (n > 0) {
                    sb.append(", ")
                }
                sb.append("arg").append(n + 1)
            }
            return sb.toString()
        }
    }
}