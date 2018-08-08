package org.outerj.daisy.diff.html.dom;

import java.util.Optional;

/**
 * Class which able to decide whether the given character is a delimiter or not.
 */
public class DelimiterConfigurer {

    public boolean isDelimiter(String text) {
        return Optional.ofNullable(text)
            .map(s -> s.length() == 1 && isDelimiter(s.charAt(0)))
            .orElse(false);
    }

    /**
     * White spaces do not participate in the difference process.
     * If there are two or more whitespaces in a row, they will collapse into single one.
     */
    public boolean isWhiteSpace(char c) {
        switch (c) {
            case ' ':
            case '\t':
            case '\r':
            case '\n':
                return true;
            default:
                return false;
        }
    }

    /**
     * Not whitespace delimiters participate in the difference process, like a usual text nodes.
     */
    public boolean isDelimiter(char c) {
        if (isWhiteSpace(c))
            return true;
        switch (c) {
            // Basic Delimiters
            case '/':
            case '.':
            case '!':
            case ',':
            case ';':
            case '?':
            case '=':
            case '\'':
            case '"':
                // Extra Delimiters
            case '[':
            case ']':
            case '{':
            case '}':
            case '(':
            case ')':
            case '&':
                //case '|':
            case '\\':
            case '-':
                //case '_':
            case '+':
            case '*':
            case ':':
                // &nbsp;
                // nbsp should not count as whitespace
            case '\u00A0':
                return true;
            default:
                return false;
        }
    }

}
