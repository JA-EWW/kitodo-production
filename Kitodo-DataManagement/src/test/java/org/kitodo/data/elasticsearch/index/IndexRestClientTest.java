/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package org.kitodo.data.elasticsearch.index;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.kitodo.data.elasticsearch.MockEntity;

/**
 * Test class for IndexRestClient.
 */
public class IndexRestClientTest {

    private static IndexRestClient initializeRestClient() {
        IndexRestClient restClient = new IndexRestClient();

        restClient.initiateClient();
        restClient.setIndex("kitodo");
        restClient.setType("test");

        return restClient;
    }

    @Test
    public void shouldAddDocument() throws Exception {
        IndexRestClient restClient = initializeRestClient();
        String result = restClient.addDocument(MockEntity.createEntities().get(1), 1);

        boolean created = result.contains("\"created\":true");
        // if document already exists it is updated and in that case check if
        // update successful
        boolean ok = result.contains("\"successful\":1");
        boolean condition = created || ok;
        assertTrue("Add of document has failed!", condition);
    }

    @Test
    public void shouldAddType() throws Exception {
        IndexRestClient restClient = initializeRestClient();
        String result = restClient.addType(MockEntity.createEntities());

        boolean created = result.contains(
                "requestLine=PUT /kitodo/test/1 HTTP/1.1, host=http://localhost:9200, response=HTTP/1.1 201 Created");
        boolean ok = result.contains(
                "requestLine=PUT /kitodo/test/1 HTTP/1.1, host=http://localhost:9200, response=HTTP/1.1 200 OK");
        boolean condition = created || ok;
        assertTrue("Add of type has failed - document id 1!", condition);

        created = result.contains(
                "requestLine=PUT /kitodo/test/2 HTTP/1.1, host=http://localhost:9200, response=HTTP/1.1 201 Created");
        ok = result.contains(
                "requestLine=PUT /kitodo/test/2 HTTP/1.1, host=http://localhost:9200, response=HTTP/1.1 200 OK");
        condition = created || ok;
        assertTrue("Add of type has failed - document id 2!", condition);
    }

    @Test
    public void shouldDeleteDocument() throws Exception {
        IndexRestClient restClient = initializeRestClient();
        restClient.addType(MockEntity.createEntities());

        String result = restClient.deleteDocument(1);
        boolean condition = result.contains("HTTP/1.1 200 OK");
        assertTrue("Delete of document has failed!", condition);
    }

    @Test
    public void shouldDeleteType() throws Exception {
        IndexRestClient restClient = initializeRestClient();
        restClient.addType(MockEntity.createEntities());

        String result = restClient.deleteType();
        boolean condition = result.contains("HTTP/1.1 200 OK");
        assertTrue("Delete of type has failed!", condition);
    }
}