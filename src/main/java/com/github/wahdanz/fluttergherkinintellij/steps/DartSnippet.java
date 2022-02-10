package com.github.wahdanz.fluttergherkinintellij.steps;

import com.github.wahdanz.fluttergherkinintellij.steps.snippets.ParamSnippet;
import com.google.common.primitives.Primitives;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DartSnippet implements ParamSnippet {
  protected String getArgType(Class<?> argType) {
    return argType.isPrimitive() ? Primitives.wrap(argType).getSimpleName() : argType.getSimpleName();
  }

  @Override
  public String template() {

    return "StepDefinitionGeneric {0}() '{' \n" +
            "return {1}<{2}FlutterWidgetTesterWorld>(\"{3}\"\n    ,({4} context)" +
            " async '{'\n    // {4}\n//{5}\n\t\t'}');\n \t'}";

  }

  @Override
  public String tableHint() {
    return "";
  }

  @Override
  public String arguments(List<Class<?>> argumentTypes) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < argumentTypes.size(); i++) {
      Class<?> arg = argumentTypes.get(i);
      if (i > 0) {
        result.append(", ");
      }
      result.append(getArgType(arg));
    }

    return result.toString();
  }

  @Override
  public String namedGroupStart() {
    return null;
  }

  @Override
  public String namedGroupEnd() {
    return null;
  }

  @Override
  public String escapePattern(String pattern) {
    return pattern.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  @Override
  public String paramArguments(List<ArgumentParam> argumentTypes) {
    StringBuilder result = new StringBuilder();

    Set<String> argNames = new HashSet<>();

    for (int i = 0; i < argumentTypes.size(); i++) {
      ArgumentParam arg = argumentTypes.get(i);

      result.append(arg.clazz.getSimpleName()).append(" ");

      if (arg.name == null) {
        result.append("arg").append(i);
      } else if (argNames.contains(arg.name)) {
        result.append(arg.clazz.getSimpleName()).append(i);
      } else {
        result.append(arg.clazz.getSimpleName());
        argNames.add(arg.clazz.getSimpleName());
      }
      result.append(", ");

    }

    return result.toString();
  }
}
