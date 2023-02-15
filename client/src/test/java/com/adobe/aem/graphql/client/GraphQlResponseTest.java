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

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

class GraphQlResponseTest {

	private static final String EXAMPLE_ERROR = "Example Error";

	@Test
	void testRegularGraphQlResponse() {

		ObjectNode rootNode = createDefaultResponseData();

		GraphQlResponse graphQlResponse = new GraphQlResponse(rootNode);
		
		assertEquals("{\"entityList\":{\"items\":[{\"entityProp\":\"value1\"},{\"entityProp\":\"value2\"}]}}", graphQlResponse.getData().toString());
		assertFalse(graphQlResponse.hasErrors());
		assertNull(graphQlResponse.getErrors());
		assertEquals(2, graphQlResponse.getItems().size());
		assertEquals("value1", graphQlResponse.getItems().get(0).get("entityProp").asText());
		assertEquals("value2", graphQlResponse.getItems().get(1).get("entityProp").asText());
		assertTrue(graphQlResponse.toString().contains(AEMHeadlessClient.JSON_KEY_DATA));
	}
	
	@Test
	void testCursorPaginatedGraphQlResponse() {

		ObjectNode rootNode = createCursorPagingResponseData();
		
		GraphQlResponse graphQlResponse = new GraphQlResponse(rootNode);
		
		assertEquals("{\"entityPaginated\":{\"edges\":["
				+ "{\"node\":{\"entityProp\":\"value1\"},\"cursor\":\"Q3ljbGluZyBTb3V0aGVybiBVdGFoCmQ0ZjcxNjkwLWJjNDQtNGYzNi1iOGZmLThkNmQ3OWQzNzhjZA==\"},"
				+ "{\"node\":{\"entityProp\":\"value2\"},\"cursor\":\"Q2xpbWJpbmcgTmV3IFplYWxhbmQKZWIwMTc1ZDMtYjAxMS00MjFiLWIxYmUtMGVmNWIyYjY0NTE4\"}"
				+ "]}}", graphQlResponse.getData().toString());
		assertFalse(graphQlResponse.hasErrors());
		assertNull(graphQlResponse.getErrors());
		assertEquals(2, graphQlResponse.getItems().size());
		assertEquals("value1", graphQlResponse.getItems().get(0).get("entityProp").asText());
		assertEquals("value2", graphQlResponse.getItems().get(1).get("entityProp").asText());
		assertTrue(graphQlResponse.toString().contains(AEMHeadlessClient.JSON_KEY_DATA));
	}

	@Test
	void testRegularGraphQlResponseWithPojoMapping() {
		ObjectNode rootNode = createDefaultResponseData();
		GraphQlResponse graphQlResponse = new GraphQlResponse(rootNode);
		
		List<EntityPojo> items = graphQlResponse.getItems(EntityPojo.class);
		assertEquals(2, items.size());
		assertEquals("value1", items.get(0).getFirstEntityProp());
		assertEquals("value2", items.get(1).getFirstEntityProp());
	}
	
	@Test
	void testGraphQlResponseWithErrorsAndData() {
		
		ObjectNode rootNode = createDefaultResponseData();
		
		ArrayNode errorsNode = rootNode.putArray(JSON_KEY_ERRORS);
		errorsNode.addObject().put(JSON_KEY_MESSAGE, EXAMPLE_ERROR);

		GraphQlResponse graphQlResponse = new GraphQlResponse(rootNode);

		assertEquals("{\"entityList\":{\"items\":[{\"entityProp\":\"value1\"},{\"entityProp\":\"value2\"}]}}", graphQlResponse.getData().toString());

		assertTrue(graphQlResponse.hasErrors());
		assertEquals(1, graphQlResponse.getErrors().size());
		assertEquals(EXAMPLE_ERROR, graphQlResponse.getErrors().get(0).getMessage());
	}

	@Test
	void testGraphQlResponseWithErrorsOnly() {

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode rootNode = mapper.createObjectNode();

		ArrayNode errorsNode = rootNode.putArray(JSON_KEY_ERRORS);
		errorsNode.addObject().put(JSON_KEY_MESSAGE, EXAMPLE_ERROR);

		GraphQlResponse graphQlResponse = new GraphQlResponse(rootNode);

		assertNull(graphQlResponse.getData());

		assertTrue(graphQlResponse.hasErrors());
		assertEquals(1, graphQlResponse.getErrors().size());
		assertEquals(EXAMPLE_ERROR, graphQlResponse.getErrors().get(0).getMessage());
	}

	@Test
	void testGraphQlResponseWithUnexpectedFormatInData() {

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

		assertNull(graphQlResponse.getItems());
		assertEquals("[{\"prop1\":\"value1\"},{\"prop2\":\"value2\"}]", graphQlResponse.getData().toString());
		assertFalse(graphQlResponse.hasErrors());
		assertNull(graphQlResponse.getErrors());

	}
	

	private ObjectNode createDefaultResponseData() {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode rootNode = mapper.createObjectNode();

		ObjectNode dataNode = mapper.createObjectNode();
		rootNode.set(JSON_KEY_DATA, dataNode);

		ObjectNode exampleDataEntityList = dataNode.putObject("entityList");
		ArrayNode exampleDataItems = exampleDataEntityList.putArray("items");
			
		ObjectNode obj1 = exampleDataItems.addObject();
		obj1.put("entityProp", "value1");
		ObjectNode obj2 = exampleDataItems.addObject();
		obj2.put("entityProp", "value2");
		return rootNode;
	}

	private ObjectNode createCursorPagingResponseData() {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode rootNode = mapper.createObjectNode();

		ObjectNode dataNode = mapper.createObjectNode();
		rootNode.set(JSON_KEY_DATA, dataNode);

		ObjectNode exampleDataEntityList = dataNode.putObject("entityPaginated");
		ArrayNode exampleDataEdges = exampleDataEntityList.putArray(AEMHeadlessClient.JSON_KEY_EDGES);
			
		ObjectNode edgeObject1 = exampleDataEdges.addObject();
		edgeObject1.putObject(AEMHeadlessClient.JSON_KEY_NODE).put("entityProp", "value1");
		edgeObject1.put("cursor", "Q3ljbGluZyBTb3V0aGVybiBVdGFoCmQ0ZjcxNjkwLWJjNDQtNGYzNi1iOGZmLThkNmQ3OWQzNzhjZA==");

		ObjectNode edgeObject2 = exampleDataEdges.addObject();
		edgeObject2.putObject(AEMHeadlessClient.JSON_KEY_NODE).put("entityProp", "value2");
		edgeObject2.put("cursor", "Q2xpbWJpbmcgTmV3IFplYWxhbmQKZWIwMTc1ZDMtYjAxMS00MjFiLWIxYmUtMGVmNWIyYjY0NTE4");
		return rootNode;
	}
	
	@JsonIgnoreProperties
	public static class EntityPojo {
		private String firstEntityProp;
		private String otherEntityProp;
		
		String getFirstEntityProp() {
			return firstEntityProp;
		}
		
		@JsonSetter("entityProp")
		void setFirstEntityProp(String firstEntityProp) {
			this.firstEntityProp = firstEntityProp;
		}
		String getOtherEntityProp() {
			return otherEntityProp;
		}
		void setOtherEntityProp(String otherEntityProp) {
			this.otherEntityProp = otherEntityProp;
		}
		
	}
}
