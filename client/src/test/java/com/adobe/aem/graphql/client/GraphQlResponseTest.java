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
import static com.adobe.aem.graphql.client.AEMHeadlessClient.JSON_KEY_ERRORS;
import static com.adobe.aem.graphql.client.AEMHeadlessClient.JSON_KEY_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

class GraphQlResponseTest {

	private static final String EXAMPLE_ERROR = "Example Error";

	@Test
	void testGraphQlResponseWithoutErrors() {

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode rootNode = mapper.createObjectNode();

		ArrayNode dataNode = mapper.createArrayNode();
		rootNode.set(JSON_KEY_DATA, dataNode);

		ObjectNode exampleData1 = mapper.createObjectNode();
		exampleData1.put("prop1", "value1");
		dataNode.add(exampleData1);

		ObjectNode exampleData2 = mapper.createObjectNode();
		exampleData2.put("prop2", "value2");
		dataNode.add(exampleData2);

		GraphQlResponse graphQlResponse = new GraphQlResponse(rootNode);

		assertEquals("[{\"prop1\":\"value1\"},{\"prop2\":\"value2\"}]", graphQlResponse.getData().toString());
		assertFalse(graphQlResponse.hasErrors());
		assertNull(graphQlResponse.getErrors());
		assertTrue(graphQlResponse.toString().contains("data"));

	}

	@Test
	void testGraphQlResponseWithErrorsAndData() {

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode rootNode = mapper.createObjectNode();

		ArrayNode dataNode = mapper.createArrayNode();
		rootNode.set(JSON_KEY_DATA, dataNode);

		ObjectNode exampleData1 = mapper.createObjectNode();
		exampleData1.put("prop1", "value1");
		dataNode.add(exampleData1);

		ObjectNode exampleData2 = mapper.createObjectNode();
		exampleData2.put("prop2", "value2");
		dataNode.add(exampleData2);

		ArrayNode errorsNode = mapper.createArrayNode();
		rootNode.set(JSON_KEY_ERRORS, errorsNode);

		ObjectNode exampleError = mapper.createObjectNode();
		exampleError.put(JSON_KEY_MESSAGE, EXAMPLE_ERROR);
		errorsNode.add(exampleError);

		GraphQlResponse graphQlResponse = new GraphQlResponse(rootNode);

		assertEquals("[{\"prop1\":\"value1\"},{\"prop2\":\"value2\"}]", graphQlResponse.getData().toString());

		assertTrue(graphQlResponse.hasErrors());
		assertEquals(1, graphQlResponse.getErrors().size());
		assertEquals(EXAMPLE_ERROR, graphQlResponse.getErrors().get(0).getMessage());
	}

	@Test
	void testGraphQlResponseWithErrorsOnly() {

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode rootNode = mapper.createObjectNode();

		ArrayNode errorsNode = mapper.createArrayNode();
		rootNode.set(JSON_KEY_ERRORS, errorsNode);

		ObjectNode exampleError = mapper.createObjectNode();
		exampleError.put(JSON_KEY_MESSAGE, EXAMPLE_ERROR);
		errorsNode.add(exampleError);

		GraphQlResponse graphQlResponse = new GraphQlResponse(rootNode);

		assertNull(graphQlResponse.getData());

		assertTrue(graphQlResponse.hasErrors());
		assertEquals(1, graphQlResponse.getErrors().size());
		assertEquals(EXAMPLE_ERROR, graphQlResponse.getErrors().get(0).getMessage());
	}

}
