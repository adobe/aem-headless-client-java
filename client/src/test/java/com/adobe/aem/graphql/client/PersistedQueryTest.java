/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.aem.graphql.client;

import static com.adobe.aem.graphql.client.AEMHeadlessClient.JSON_KEY_DATA;
import static com.adobe.aem.graphql.client.AEMHeadlessClient.JSON_KEY_LONG_FORM;
import static com.adobe.aem.graphql.client.AEMHeadlessClient.JSON_KEY_PATH;
import static com.adobe.aem.graphql.client.AEMHeadlessClient.JSON_KEY_QUERY;
import static com.adobe.aem.graphql.client.AEMHeadlessClient.JSON_KEY_SHORT_FORM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

class PersistedQueryTest {

	private static final String PATH_SHORT = "/myproj/myquery";
	private static final String PATH_LONG = "/myproj/settings/graphql/persistentQueries/myquery";
	private static final String QUERY = "{ articleList { items { author, main{plaintext} } } } ";

	@Test
	void testPersistedQuery() {

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode rootNode = mapper.createObjectNode();

		ObjectNode pathNode = mapper.createObjectNode();
		rootNode.set(JSON_KEY_PATH, pathNode);

		pathNode.put(JSON_KEY_LONG_FORM, PATH_LONG);
		pathNode.put(JSON_KEY_SHORT_FORM, PATH_SHORT);

		ObjectNode dataNode = mapper.createObjectNode();
		rootNode.set(JSON_KEY_DATA, dataNode);
		dataNode.put(JSON_KEY_QUERY, QUERY);

		PersistedQuery persistedQuery = new PersistedQuery(rootNode);

		assertEquals(PATH_SHORT, persistedQuery.getShortPath());
		assertEquals(PATH_LONG, persistedQuery.getLongPath());
		assertEquals(QUERY, persistedQuery.getQuery());
		assertTrue(persistedQuery.toString().contains(PATH_SHORT));
		assertTrue(persistedQuery.toString().contains(PATH_LONG));
	}

}
