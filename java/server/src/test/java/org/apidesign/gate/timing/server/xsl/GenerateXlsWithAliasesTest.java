package org.apidesign.gate.timing.server.xsl;

import java.io.InputStream;

public class GenerateXlsWithAliasesTest extends GenerateXlsTest {
    @Override
    protected InputStream eventStream() {
        return getClass().getResourceAsStream("pizaar_alias.json");
    }
}
