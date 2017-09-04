package com.cflint.plugins.core;

import com.cflint.CF;
import com.cflint.BugList;
import com.cflint.plugins.CFLintScannerAdapter;
import com.cflint.plugins.Context;

import cfml.parsing.cfscript.script.CFFuncDeclStatement;
import cfml.parsing.cfscript.script.CFScriptStatement;
import net.htmlparser.jericho.Element;
import ro.fortsoft.pf4j.Extension;

/**
 * Check if a function or method name is valid.
 */
@Extension
public class MethodNameChecker extends CFLintScannerAdapter {

    /**
     * Minimum number of characters for a function name.
     */
    private int minMethodLength = ValidName.MIN_METHOD_LENGTH;

    /**
     * Maximum number of characters for a function name.
     */
    private int maxMethodLength = ValidName.MAX_METHOD_LENGTH;

    /**
     * Minimum number of words in a function name.
     */
    private int maxMethodWords = ValidName.MAX_METHOD_WORDS;


    /**
     * Parse a CFScript function declaration to see if the function name is invalid.
     */
    @Override
    public void expression(final CFScriptStatement expression, final Context context, final BugList bugs) {
        if (expression instanceof CFFuncDeclStatement) {
            final CFFuncDeclStatement method = (CFFuncDeclStatement) expression;
            final int lineNo = method.getLine() + context.startLine() - 1;
            checkNameForBugs(context, lineNo);
        }
    }

    /**
     * Parse CF function tag declaration to see if the function name is invalid.
     */
    @Override
    public void element(final Element element, final Context context, final BugList bugs) {
        if (element.getName().equals(CF.CFFUNCTION)) {
            final int lineNo = element.getSource().getRow(element.getBegin());
            checkNameForBugs(context, lineNo);
        }
    }

    /**
     * Parse rule parameters.
     *
     * Parameters include:
     * - minimum length of valid name
     * - maximum length of valid name
     * - maximum number of words in a camel case name
     *
     * See @ValidName for defaults.
     */
    private void parseParameters()  throws ConfigError {
        if (getParameter("MinLength") != null) {
            try {
                minMethodLength = Integer.parseInt(getParameter("MinLength"));
            } catch (final Exception e) {
                throw new ConfigError("Minimum length need to be an integer.");
            }
        }

        if (getParameter("MaxLength") != null) {
            try {
                maxMethodLength = Integer.parseInt(getParameter("MaxLength"));
            } catch (final Exception e) {
                throw new ConfigError("Maximum length need to be an integer.");
            }
        }

        if (getParameter("MaxWords") != null) {
            try {
                maxMethodWords = Integer.parseInt(getParameter("MaxWords"));
            } catch (final Exception e) {
                throw new ConfigError("Maximum no of words need to be an integer.");
            }
        }
    }

    /**
     * Check if an function name is "bad" is some way.
     *
     * Bad argument name include:
     * - Invalid names (contains an invalid character, ends in a number, not camelCase or does not use underscores)
     * - Names all in upper case
     * - Names that are too short
     * - Names that are too long
     * - Names that are too wordy
     * - Names that look like temporary variables
     * - Names having a prefix or postfix
     */
    public void checkNameForBugs(final Context context, final int line) {
        final String method = context.getFunctionName();

        try {
            parseParameters();
        } catch (ConfigError configError) {
            // Carry on with defaults
        }

        final ValidName name = new ValidName(minMethodLength, maxMethodLength, maxMethodWords);

        if (name.isInvalid(method)) {
            context.addMessage("METHOD_INVALID_NAME", null, line);
        }
        if (name.isUpperCase(method)) {
            context.addMessage("METHOD_ALLCAPS_NAME", null, line);
        }
        if (name.tooShort(method)) {
            context.addMessage("METHOD_TOO_SHORT", null, line);
        }
        if (name.tooLong(method)) {
            context.addMessage("METHOD_TOO_LONG", null, line);
        }
        if (!name.isUpperCase(method) && name.tooWordy(method)) {
            context.addMessage("METHOD_TOO_WORDY", null, line);
        }
        if (name.isTemporary(method)) {
            context.addMessage("METHOD_IS_TEMPORARY", null, line);
        }
        if (name.hasPrefixOrPostfix(method)) {
            context.addMessage("METHOD_HAS_PREFIX_OR_POSTFIX", null, line);
        }
    }
}
