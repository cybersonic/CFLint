package com.cflint;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class HTMLOutput {

    final String htmlStyle;

    public void output(final BugList bugList, final Writer writer, final CFLintStats stats)
            throws IOException, TransformerException {

        // 1. Instantiate a TransformerFactory.
        final javax.xml.transform.TransformerFactory tFactory = javax.xml.transform.TransformerFactory.newInstance();

        // 2. Use the TransformerFactory to process the stylesheet Source and
        // generate a Transformer.
        javax.xml.transform.Transformer transformer;
        try {
            final InputStream is = getClass().getResourceAsStream("/findbugs/" + htmlStyle);
            transformer = tFactory.newTransformer(new StreamSource(is));
        } catch (final TransformerConfigurationException e) {
            final InputStream is = getClass().getResourceAsStream("/" + htmlStyle);
            transformer = tFactory.newTransformer(new StreamSource(is));
        }

        final StringWriter sw = new StringWriter();
        new XMLOutput().outputFindBugs(bugList, sw, stats);

        // 3. Use the Transformer to transform an XML Source and send the
        // output to a Result object.
        transformer.transform(new StreamSource(new StringReader(sw.toString())),
                new StreamResult(writer));

        writer.close();
    }

    public HTMLOutput(final String htmlStyle) {
        super();
        this.htmlStyle = htmlStyle;
    }
}
