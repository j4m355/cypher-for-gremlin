/*
 * Copyright (c) 2018-2019 "Neo4j, Inc." [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opencypher.gremlin.console;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.opencypher.gremlin.console.jsr223.CypherGremlinPlugin.NAME;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.opencypher.gremlin.rules.GremlinConsoleExternalResource;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.server.EmbeddedGremlinServer;
import org.opencypher.gremlin.test.TestCommons;

public class GremlinConsoleTest {

    @ClassRule
    public static final GremlinServerExternalResource server =
        new GremlinServerExternalResource(TestCommons::modernGraph, () -> EmbeddedGremlinServer
            .builder()
            .defaultParameters()
            .propertiesPath("graph2","../testware-common/src/main/resources/tinkergraph-empty.properties")
            .scriptPath("../testware-common/src/main/resources/generate-multiple.groovy")
            .build()
        );

    private static final String PERSON_NAMES_QUERY = "MATCH (p:person) RETURN p.name AS name";
    private static final String[] PERSON_NAMES_RESULT = {
        "==>[name:marko]",
        "==>[name:vadas]",
        "==>[name:josh]",
        "==>[name:peter]"};

    private static final String[] CREW_NAMES_RESULT = {
        "==>[name:marko]",
        "==>[name:stephen]",
        "==>[name:matthias]",
        "==>[name:daniel]"};

    @Rule
    public final GremlinConsoleExternalResource console = new GremlinConsoleExternalResource();

    @Rule
    public final SystemOutRule systemOut = new SystemOutRule().enableLog();



    @Before
    public void before() {
        systemOut.clearLog();
        waitForPrompt();
    }

    @Test
    public void pluginInstall() throws Exception {
        String pluginList = eval(":plugin list");
        assertThat(pluginList).contains("==>" + NAME);
    }

    @Test
    public void remoteCypher() throws Exception {
        String usePlugin = eval(":plugin use " + NAME);
        assertThat(usePlugin).contains("==>" + NAME + " activated");

        String remoteConnect = eval(":remote connect " + NAME + " " + server.remoteConfiguration());
        assertThat(remoteConnect).contains("==>Configured localhost/127.0.0.1:" + server.getPort());

        String queryResult = eval(":> " + PERSON_NAMES_QUERY);
        assertThat(queryResult)
            .contains("CypherOpProcessor")
            .contains("Cypher: " + PERSON_NAMES_QUERY)
            .contains(PERSON_NAMES_RESULT);
    }

    @Test
    public void remoteTranslatingCypher() throws Exception {
        String usePlugin = eval(":plugin use " + NAME);
        assertThat(usePlugin).contains("==>" + NAME + " activated");

        String remoteConnect = eval(":remote connect " + NAME + " " + server.remoteConfiguration() + " translate");
        assertThat(remoteConnect).contains("==>Configured localhost/127.0.0.1:" + server.getPort());

        String queryResult = eval(":> " + PERSON_NAMES_QUERY);
        assertThat(queryResult)
            .doesNotContain("CypherOpProcessor")
            .contains(PERSON_NAMES_RESULT);
    }

    @Test
    public void remoteCypherMultiple() throws Exception {
        String usePlugin = eval(":plugin use " + NAME);
        assertThat(usePlugin).contains("==>" + NAME + " activated");

        String remoteConnect = eval(":remote connect " + NAME + " " + server.remoteConfiguration());
        assertThat(remoteConnect).contains("==>Configured localhost/127.0.0.1:" + server.getPort());

        List<String> queryResult1 = resultLines(eval(":> RETURN 1"));
        List<String> queryResult2 = resultLines(eval(":> RETURN 1"));
        List<String> queryResult3 = resultLines(eval(":> RETURN 1"));

        assertThat(queryResult1)
            .isEqualTo(queryResult2)
            .isEqualTo(queryResult3)
            .containsExactly("==>[1:1]");
    }

    @Test
    public void remoteTranslatingCypherMultiple() throws Exception {
        String usePlugin = eval(":plugin use " + NAME);
        assertThat(usePlugin).contains("==>" + NAME + " activated");

        String remoteConnect = eval(":remote connect " + NAME + " " + server.remoteConfiguration() + " translate");
        assertThat(remoteConnect).contains("==>Configured localhost/127.0.0.1:" + server.getPort());

        List<String> queryResult1 = resultLines(eval(":> RETURN 1"));
        List<String> queryResult2 = resultLines(eval(":> RETURN 1"));
        List<String> queryResult3 = resultLines(eval(":> RETURN 1"));

        assertThat(queryResult1)
            .isEqualTo(queryResult2)
            .isEqualTo(queryResult3)
            .containsExactly("==>[1:1]");
    }

    @Test
    public void remoteTranslatingCypher33x() throws Exception {
        String usePlugin = eval(":plugin use " + NAME);
        assertThat(usePlugin).contains("==>" + NAME + " activated");

        String remoteConnect = eval(":remote connect " + NAME + " " + server.remoteConfiguration() + " translate gremlin33x");
        assertThat(remoteConnect).contains("==>Configured localhost/127.0.0.1:" + server.getPort());

        String queryResult = eval(":> " + PERSON_NAMES_QUERY + " ORDER BY p.name");
        assertThat(queryResult)
            .doesNotContain("CypherOpProcessor")
            .doesNotContain("asc")
            .contains("incr")
            .contains(PERSON_NAMES_RESULT);
    }

    @Test
    public void remoteConsole() throws Exception {
        String usePlugin = eval(":plugin use " + NAME);
        assertThat(usePlugin).contains("==>" + NAME + " activated");

        String remoteConnect = eval(":remote connect " + NAME + " " + server.remoteConfiguration());
        assertThat(remoteConnect).contains("==>Configured localhost/127.0.0.1:" + server.getPort());

        String remoteConsole = eval(":remote console");
        assertThat(remoteConsole).contains("All scripts will now be sent to Gremlin Server");

        String queryResult = eval(PERSON_NAMES_QUERY);
        assertThat(queryResult)
            .contains("CypherOpProcessor")
            .contains("Cypher: " + PERSON_NAMES_QUERY)
            .contains(PERSON_NAMES_RESULT);

        remoteConsole = eval(":remote console");
        assertThat(remoteConsole).contains("All scripts will now be evaluated locally");
    }

    @Test
    public void remoteConfigAlias() throws Exception {
        String usePlugin = eval(":plugin use " + NAME);
        assertThat(usePlugin)
            .contains("==>" + NAME + " activated");

        String remoteConnect = eval(":remote connect " + NAME + " " + server.remoteConfiguration());
        assertThat(remoteConnect)
            .contains("==>Configured localhost/127.0.0.1:" + server.getPort());

        String remoteAlias = eval(":remote config alias g g2");
        assertThat(remoteAlias)
            .contains("==>g=g2");

        String crewGraphResult = eval(":> " + PERSON_NAMES_QUERY);
        assertThat(crewGraphResult)
            .contains(CREW_NAMES_RESULT);

        String remoteAliasReset = eval(":remote config alias reset");
        assertThat(remoteAliasReset)
            .contains("==>Aliases cleared");

        String personGraphResult = eval(":> MATCH (p:person) RETURN p.name AS name");
        assertThat(personGraphResult)
            .contains(PERSON_NAMES_RESULT);
    }

    @Test
    public void remoteCypherTraversalSource() throws Exception {
        String usePlugin = eval(":plugin use " + NAME);
        assertThat(usePlugin).contains("==>" + NAME + " activated");

        String remoteConnect = eval("g = AnonymousTraversalSource.traversal(CypherTraversalSource.class).traversal(CypherTraversalSource.class).withRemote('" + server.driverRemoteConfiguration() + "')");
        assertThat(remoteConnect).contains("==>cyphertraversalsource[emptygraph[empty], standard]");

        String queryResult = eval("g.cypher('" + PERSON_NAMES_QUERY + "')");
        assertThat(queryResult).contains(PERSON_NAMES_RESULT);
    }

    /**
     * @see org.opencypher.gremlin.translation.ir.rewrite.CosmosDbFlavor#rewriteValues
     */
    @Test
    public void remoteCypherTraversalSourceFlavor() throws Exception {
        String usePlugin = eval(":plugin use " + NAME);
        assertThat(usePlugin).contains("==>" + NAME + " activated");

        String remoteConnect = eval("g = EmptyGraph.instance().traversal(CypherTraversalSource.class).withRemote('" + server.driverRemoteConfiguration() + "')");
        assertThat(remoteConnect).contains("==>cyphertraversalsource[emptygraph[empty], standard]");

        String queryResult = eval("g.cypher('" + PERSON_NAMES_QUERY + "', 'cosmosdb')");

        assertThat(queryResult)
            .contains("properties(), hasKey(name)")
            .doesNotContain("[values(name)]");
    }

    private void waitForPrompt() {
        Awaitility.await()
            .atMost(10, SECONDS)
            .until(() -> systemOut.getLog().contains("gremlin>"));
    }

    private String eval(String command) {
        systemOut.clearLog();
        console.writeln(command);
        try {
            waitForPrompt();
        } catch (ConditionTimeoutException e) {
            console.writeln("y");
            waitForPrompt();
            throw new RuntimeException(e);
        }
        return systemOut.getLog()
            .replaceAll("\u001B\\[m", "");
    }

    private List<String> resultLines(String queryResult) {
        return Arrays.stream(queryResult.split("\n"))
            .filter(n -> n.startsWith("==>"))
            .collect(Collectors.toList());
    }
}
