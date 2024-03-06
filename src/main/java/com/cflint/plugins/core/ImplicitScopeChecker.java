package com.cflint.plugins.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;

import com.cflint.BugList;
import com.cflint.CF;
import com.cflint.plugins.CFLintScannerAdapter;
import com.cflint.plugins.Context;
import cfml.parsing.cfscript.CFExpression;
import cfml.parsing.cfscript.CFFullVarExpression;
import cfml.parsing.cfscript.CFIdentifier;
import cfml.parsing.cfscript.CFMember;
import cfml.parsing.cfscript.CFVarDeclExpression;
import net.htmlparser.jericho.Element;

public class ImplicitScopeChecker extends CFLintScannerAdapter {

	/**
	 * Report each occurrence once per file/function
	 */
	// private Set<String> alreadyReportedExpression = new HashSet<>();
	// private Set<String> unqualifiedExpression = new HashSet<>();
	// private Set<String> alreadyReportedFullExpression = new HashSet<>();

    // LinkedHashMap is ordered.
    protected Set<String> implicitScopedVariables = new HashSet<>();
    protected Set<String> scopedVariables = new HashSet<>();
    protected Set<String> variableScopedVariables = new HashSet<>();
    
    protected Map<String, VariableInfo> unscopedAssignedVariables = new LinkedHashMap<>();
    protected Map<String, VariableInfo> implicitIdentifierVariables = new LinkedHashMap<>();

    private final Collection<String> scopes = Arrays.asList(CF.APPLICATION, CF.CGI, CF.COOKIE, CF.FORM, CF.REQUEST,
        CF.SERVER, CF.SESSION, CF.URL, CF.CFTHREAD);

    private final Collection<String> implicitscopes = Arrays.asList(CF.FORM, CF.URL);
    private final Collection<String> variablescopes = Arrays.asList(CF.VARIABLES);

	@Override
	public void expression(final CFExpression expression, final Context context, final BugList bugs) {
        if(expression instanceof CFFullVarExpression) {
            checkFullExpression((CFFullVarExpression) expression, context, bugs);
        } else if (expression instanceof CFVarDeclExpression) {
            checkExpression((CFVarDeclExpression)expression, context);
        } else if (expression instanceof CFIdentifier && !context.isInAssignmentExpression()) {
            final String name = ((CFIdentifier) expression).getName();
            if (name != null) {
                implicitIdentifierVariables.put(name.toLowerCase(), new VariableInfo(name, expression, context));
            }
        }
	}

	private void checkFullExpression(final CFFullVarExpression expression, final Context context, final BugList bugs) {
        final CFExpression variable = expression.getExpressions().get(0);
        if (variable instanceof CFIdentifier) {
            final CFIdentifier cfIdentifier1 = (CFIdentifier) variable;
			final CFIdentifier cfIdentifier2 = expression.getExpressions().size() > 1
					&& expression.getExpressions().get(1) instanceof CFIdentifier
							? (CFIdentifier) expression.getExpressions().get(1)
							: null;
            if ( isImplicitScope(cfIdentifier1.getName()) ) {
                final String name = ((CFIdentifier) cfIdentifier2).getName();
                if ( name != null ) {
                    implicitScopedVariables.add(name.toLowerCase());
                }
            } else if ( isVariableScope(cfIdentifier1.getName()) ) {
                final String name = ((CFIdentifier) cfIdentifier2).getName();
                if ( name != null ) {
                    variableScopedVariables.add(name.toLowerCase());
                }
            } else if ( isScope(cfIdentifier1.getName()) ) {
                final String name = ((CFIdentifier) cfIdentifier2).getName();
                if ( name != null ) {
                    scopedVariables.add(name.toLowerCase());
                }
            } else if ( expression.getExpressions().size() == 1 ) {
                final String name = ((CFIdentifier) cfIdentifier1).getName();
                if ( name != null ) {
                    unscopedAssignedVariables.put(name.toLowerCase(), new VariableInfo(name,cfIdentifier1,context));
                }
            }
        }

        for (CFExpression subexpr : expression.getExpressions()) {
            if (subexpr instanceof CFMember) {
                CFMember memberExpr = (CFMember) subexpr;
                if (memberExpr.getExpression() != null) {
                    expression(memberExpr.getExpression(), context, bugs);
                }
            }
        }

	}

	private void checkExpression(final CFVarDeclExpression expression, final Context context) {
        final String name = expression.getName();
        implicitIdentifierVariables.put(name.toLowerCase(), new VariableInfo(name, expression, context));
	}

	@Override
	public void startFunction(Context context, BugList bugs) {
        implicitScopedVariables.clear();
        implicitIdentifierVariables.clear();
        unscopedAssignedVariables.clear();
        scopedVariables.clear();
	}

    @Override
    public void endFunction(final Context context, final BugList bugs) {
        // sort by line number
        for (final VariableInfo variable : implicitIdentifierVariables.values()) {
            // Doesn't exist as unscoped or VARIABLES, or is known as an implicit scope
            if ( ( unscopedAssignedVariables.get(variable.name.toLowerCase()) == null
            && variableScopedVariables.contains(variable.name.toLowerCase()) != true )
            || implicitScopedVariables.contains(variable.name.toLowerCase()) == true ) {
            	context.addMessage(
                    "IMPLICIT_SCOPE",
                    variable.name,
                    this,
                    variable.lineNumber,
                    variable.offset,
                    variable.expression,
                    variable.context);
            }
        }
    }
    
    @Override
    public void element(final Element element, final Context context, final BugList bugs) {
    }

    private boolean isImplicitScope(final String nameVar) {
        return nameVar != null && implicitscopes.contains(nameVar.toLowerCase().trim());
    }

    private boolean isScope(final String nameVar) {
        return nameVar != null && scopes.contains(nameVar.toLowerCase().trim());
    }

    private boolean isVariableScope(final String nameVar) {
        return nameVar != null && variablescopes.contains(nameVar.toLowerCase().trim());
    }

    public static class VariableInfo {
        private Integer lineNumber;
        private Integer offset;
        private String name;
        CFExpression expression;
        final Context context;

        public VariableInfo(final String name) {
            this.name = name;
            this.context=null;
        }

        public VariableInfo(final String name, final CFExpression expression,final Context context) {
            super();
            this.name = name;
            this.expression = expression;
            this.context=context;
        }
    }
}
