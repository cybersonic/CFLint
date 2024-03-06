package com.cflint;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Before;
import org.junit.Test;

import com.cflint.api.CFLintAPI;
import com.cflint.api.CFLintResult;
import com.cflint.config.ConfigBuilder;
import com.cflint.exception.CFLintScanException;

public class TestCFBugs_ImplicitScope {

    private CFLintAPI cfBugs;

    @Before
    public void setUp() throws Exception {
        final ConfigBuilder configBuilder = new ConfigBuilder().include("IMPLICIT_SCOPE");
        cfBugs = new CFLintAPI(configBuilder.build());
    }

    @Test
    public void testImplicitScope() throws CFLintScanException {
        final String cfcSrc = "<cfcomponent>\r\n" 
                + "<cffunction name=\"test\">\r\n"
                + "	<cfset URL.test1=\"xyz\">\r\n"
                + "<cfif test1></cfif>\r\n"
                + "<cfoutput>#test3#</cfoutput>\r\n"
                + "<cfif URL.test2></cfif>\r\n" 
                + "<cfset URL.test5 = \"\">\r\n" 
                // + "<cfif test5></cfif>\r\n" 
                // + "<cfset VARIABLES.test5 = \"\">\r\n" 
                // + "<cfif URL.test6></cfif>\r\n" 
                // + "<cfif test6></cfif>\r\n" 
                // + "<cfset VARIABLES.test6 = \"\">\r\n" 
                + "</cffunction>\r\n"
                + "</cfcomponent>";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        final List<BugInfo> result = lintresult.getIssues().values().iterator().next();
        assertEquals(2, result.size());
        assertEquals("IMPLICIT_SCOPE", result.get(0).getMessageCode());
        assertEquals("IMPLICIT_SCOPE", result.get(1).getMessageCode());
        assertEquals(4, result.get(0).getLine());
        assertEquals(5, result.get(1).getLine());
    }
    
}
