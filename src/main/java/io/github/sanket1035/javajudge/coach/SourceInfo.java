package io.github.sanket1035.javajudge.coach;

import java.util.List;
import java.util.regex.Pattern;

/** The user's code split into lines, with small helpers to find patterns and their line numbers. */
final class SourceInfo {

    private final List<String> lines;

    SourceInfo(String code) {
        this.lines = code.replace("\r\n", "\n").lines().toList();
    }

    /** 1-based line of the first match, or null. Comments are skipped. */
    Integer firstLine(Pattern p) {
        for (int i = 0; i < lines.size(); i++) {
            String line = stripComment(lines.get(i));
            if (p.matcher(line).find()) {
                return i + 1;
            }
        }
        return null;
    }

    boolean contains(Pattern p) {
        return firstLine(p) != null;
    }

    String line(int oneBased) {
        return oneBased >= 1 && oneBased <= lines.size() ? lines.get(oneBased - 1).strip() : "";
    }

    private static String stripComment(String line) {
        int i = line.indexOf("//");
        return i >= 0 ? line.substring(0, i) : line;
    }
}
