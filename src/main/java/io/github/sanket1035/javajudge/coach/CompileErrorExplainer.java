package io.github.sanket1035.javajudge.coach;

import io.github.sanket1035.javajudge.model.Hint;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Turns javac's error text into short explanations, each pointing at a line. */
public final class CompileErrorExplainer {

    // Main.java:5: error: cannot find symbol
    private static final Pattern ERROR_LINE = Pattern.compile("^\\S+\\.java:(\\d+): error: (.+)$", Pattern.MULTILINE);
    private static final Pattern SYMBOL = Pattern.compile("symbol:\\s+(?:variable|method|class)\\s+(\\S+)");

    private CompileErrorExplainer() { }

    public static List<Hint> explain(String javacOutput) {
        List<Hint> hints = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        Matcher m = ERROR_LINE.matcher(javacOutput);
        while (m.find() && hints.size() < 5) {
            int line = Integer.parseInt(m.group(1));
            String message = m.group(2).trim();
            Hint h = translate(message, javacOutput.substring(m.end())).atLine(line);
            if (seen.add(h.code() + ":" + line)) {
                hints.add(h);
            }
        }
        return hints;
    }

    private static Hint translate(String msg, String rest) {
        if (msg.startsWith("cannot find symbol")) {
            Matcher s = SYMBOL.matcher(rest);
            String name = s.find() ? s.group(1) : "a name";
            return Hint.of("UNKNOWN_SYMBOL", "Java doesn't know \"" + name + "\"",
                    "Check the spelling and capital letters, that the variable is declared before this line "
                            + "and inside the same { } block, and that you imported the class "
                            + "(for example import java.util.*; for Scanner, ArrayList, HashMap).");
        }
        if (msg.endsWith("expected")) {
            return Hint.of("SYNTAX", "Syntax error: " + msg,
                    "Usually a missing ; at the end of the line above, or a missing ) or }. "
                            + "javac often reports it one line after the real mistake.");
        }
        if (msg.startsWith("incompatible types: possible lossy conversion")) {
            return Hint.of("LOSSY_CONVERSION", "Putting a bigger number type into a smaller one",
                    "For example long into int, or double into int. Change the variable type, or cast "
                            + "on purpose with (int) if you are sure the value fits.");
        }
        if (msg.startsWith("incompatible types")) {
            return Hint.of("TYPE_MISMATCH", "Wrong type: " + msg,
                    "A String is not a number and a char is not a String. Use Integer.parseInt(s), "
                            + "String.valueOf(x) or s.charAt(i) to convert.");
        }
        if (msg.startsWith("missing return statement")) {
            return Hint.of("MISSING_RETURN", "A path through the method doesn't return a value",
                    "Every if/else branch must return, or add a return after the loop.");
        }
        if (msg.contains("might not have been initialized")) {
            return Hint.of("UNINITIALIZED", "Variable used before it has a value",
                    "Give it a starting value when you declare it, for example int sum = 0;");
        }
        if (msg.startsWith("unreported exception")) {
            return Hint.of("CHECKED_EXCEPTION", "This call can throw a checked exception",
                    "Add throws Exception to main, or wrap the call in try/catch.");
        }
        if (msg.contains("is already defined")) {
            return Hint.of("DUPLICATE_VARIABLE", "Same variable name declared twice",
                    "Rename one, or remove the type the second time if you meant to reuse it.");
        }
        if (msg.contains("non-static") && msg.contains("static context")) {
            return Hint.of("STATIC_CONTEXT", "Calling a non-static method or field from main",
                    "main is static. Make the helper static, or create an object first: new Main().helper().");
        }
        if (msg.startsWith("unreachable statement")) {
            return Hint.of("UNREACHABLE", "Code after return, break or an endless loop never runs",
                    "Move or delete the line.");
        }
        if (msg.contains("cannot be applied to")) {
            return Hint.of("WRONG_ARGUMENTS", "Method called with the wrong number or type of arguments",
                    "Compare the call with the method's parameter list.");
        }
        return Hint.of("COMPILE_ERROR", msg, "Read the line javac points to and the line just above it.");
    }
}
