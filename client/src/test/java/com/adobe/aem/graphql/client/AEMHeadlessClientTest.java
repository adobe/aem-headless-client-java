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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AEMHeadlessClientTest {
	private static final String QUERY = "{ myObjects { items { prop } } } ";
	private static final String QUERY_WITH_VAR = "query($prop: String) { myObjects(prop: $prop) { items { prop } } } ";
	private static final String EXAMPLE_DATA = "[{\"prop\":\"value1\"},{\"prop\":\"value2\"}]";
	private static final String EXAMPLE_ERROR = "Example Error";
	private static final String RESPONSE_NO_ERRORS = "{\"data\":" + EXAMPLE_DATA + "}";
	private static final String RESPONSE_ERRORS = "{\"errors\":[{\"message\":\"" + EXAMPLE_ERROR + "\"}]}";
	private static final String RESPONSE_ERRORS_AND_DATA = "{\"errors\":[{\"message\":\"" + EXAMPLE_ERROR
			+ "\"}],\"data\":" + EXAMPLE_DATA + "}";

	private static final String PERSISTED_QUERY_PRJ = "proj";
	private static final String PERSISTED_QUERY_PATH = "/" + PERSISTED_QUERY_PRJ + "/queryName";

	private static final String RESPONSE_LIST_QUERIES = "[ { \"configurationName\": \"proj\", \"queries\": [ { \"path\": { \"shortForm\": \""
			+ PERSISTED_QUERY_PATH
			+ "\",\"longForm\":\"/prj/settings/graphql/persistentQueries/queryName\"},\"data\": {\"query\":\"" + QUERY
			+ "\"}}]}]";

	AEMHeadlessClient aemHeadlessClient;

	@Captor
	ArgumentCaptor<HttpRequest> requestCaptor;

	@Mock
	HttpResponse<String> response;

	@Mock
	HttpClient client;

	@BeforeEach
	void setup() throws Exception {
		aemHeadlessClient = new AEMHeadlessClient("http://localhost:4502");
		aemHeadlessClient.setHttpClient(client);
	}

	@Test
	void testBasicConstructor() throws Exception {

		// default
		assertEquals("http://localhost:4502/content/graphql/global/endpoint.json",
				aemHeadlessClient.getEndpoint().toString());

		aemHeadlessClient = new AEMHeadlessClient("http://localhost:4502/");
		assertEquals("http://localhost:4502/content/graphql/global/endpoint.json",
				aemHeadlessClient.getEndpoint().toString(), "trailing slash must still use default URI");

		aemHeadlessClient = new AEMHeadlessClient("http://localhost:4502/custom/endpoint.json");
		assertEquals("http://localhost:4502/custom/endpoint.json", aemHeadlessClient.getEndpoint().toString(),
				"Custom URI not recognized");
	}

	@Test
	void testValidatePersistedQueryPath() throws Exception {
		// no exception - valid path
		aemHeadlessClient.validatePersistedQueryPath(PERSISTED_QUERY_PATH);

		assertThrows(IllegalArgumentException.class, () -> {
			aemHeadlessClient.validatePersistedQueryPath("test");
		});
		assertThrows(IllegalArgumentException.class, () -> {
			aemHeadlessClient.validatePersistedQueryPath("/test");
		});
		assertThrows(IllegalArgumentException.class, () -> {
			aemHeadlessClient.validatePersistedQueryPath("/path1/path2/path3");
		});
		assertThrows(IllegalArgumentException.class, () -> {
			aemHeadlessClient.validatePersistedQueryPath("/path1/path2/path3/path4");
		});
		assertThrows(IllegalArgumentException.class, () -> {
			aemHeadlessClient.validatePersistedQueryPath("/myproj/settings/graphql/persistentQueries/myquery");
		}, "long form of path is not allowed to be used directly");
	}

	@Test
	void testRunQueryNoErrors() throws Exception {

		prepareResponse(200, RESPONSE_NO_ERRORS);

		GraphQlResponse response = aemHeadlessClient.runQuery(QUERY);

		HttpRequest capturedRequest = requestCaptor.getValue();

		JsonNode bodyJson = getBodyAsJson(capturedRequest);
		assertEquals(QUERY, bodyJson.get(AEMHeadlessClient.JSON_KEY_QUERY).asText());
		assertFalse(bodyJson.has(AEMHeadlessClient.JSON_KEY_VARIABLES));

		assertEquals("http://localhost:4502/content/graphql/global/endpoint.json", capturedRequest.uri().toString());
		assertEquals("POST", capturedRequest.method());
		assertEquals(AEMHeadlessClient.CONTENT_TYPE_JSON,
				capturedRequest.headers().firstValue(AEMHeadlessClient.HEADER_CONTENT_TYPE).get());

		assertFalse(response.hasErrors());
		assertEquals(EXAMPLE_DATA, response.getData().toString());

	}

	@Test
	void testRunQueryWithVars() throws Exception {

		prepareResponse(200, RESPONSE_NO_ERRORS);

		Map<String, Object> vars = new HashMap<>();
		vars.put("prop", "test");
		GraphQlResponse response = aemHeadlessClient.runQuery(QUERY_WITH_VAR, vars);

		HttpRequest capturedRequest = requestCaptor.getValue();

		JsonNode bodyJson = getBodyAsJson(capturedRequest);
		assertEquals(QUERY_WITH_VAR, bodyJson.get(AEMHeadlessClient.JSON_KEY_QUERY).asText());
		assertTrue(bodyJson.has(AEMHeadlessClient.JSON_KEY_VARIABLES));
		assertEquals("{\"prop\":\"test\"}", bodyJson.get(AEMHeadlessClient.JSON_KEY_VARIABLES).toString());

		assertEquals("http://localhost:4502/content/graphql/global/endpoint.json", capturedRequest.uri().toString());
		assertEquals("POST", capturedRequest.method());
		assertEquals(AEMHeadlessClient.CONTENT_TYPE_JSON,
				capturedRequest.headers().firstValue(AEMHeadlessClient.HEADER_CONTENT_TYPE).get());

		assertFalse(response.hasErrors());
		assertEquals(EXAMPLE_DATA, response.getData().toString());

	}

	@Test
	void testRunQueryUnexpectedResponseCode() throws Exception {

		prepareResponse(404, "Not Found");

		AEMHeadlessClientException thrownException = assertThrows(AEMHeadlessClientException.class, () -> {
			aemHeadlessClient.runQuery(QUERY);
		});

		assertNull(thrownException.getGraphQlResponse());
		assertEquals("Unexpected http response code 404: Not Found", thrownException.getMessage());

	}

	@Test
	void testRunQueryErrorsWithoutData() throws Exception {

		prepareResponse(200, RESPONSE_ERRORS);

		AEMHeadlessClientException thrownException = assertThrows(AEMHeadlessClientException.class, () -> {
			aemHeadlessClient.runQuery(QUERY);
		});

		GraphQlResponse response = thrownException.getGraphQlResponse();

		assertTrue(response.hasErrors());
		assertEquals(EXAMPLE_ERROR, response.getErrors().get(0).getMessage());
		assertNull(response.getData());

		HttpRequest capturedRequest = requestCaptor.getValue();

		assertEquals("http://localhost:4502/content/graphql/global/endpoint.json", capturedRequest.uri().toString());
		assertEquals("POST", capturedRequest.method());
		assertEquals(AEMHeadlessClient.CONTENT_TYPE_JSON,
				capturedRequest.headers().firstValue(AEMHeadlessClient.HEADER_CONTENT_TYPE).get());

	}

	@Test
	void testRunQueryErrorsWithData() throws Exception {

		prepareResponse(200, RESPONSE_ERRORS_AND_DATA);

		AEMHeadlessClientException thrownException = assertThrows(AEMHeadlessClientException.class, () -> {
			aemHeadlessClient.runQuery(QUERY);
		});

		GraphQlResponse response = thrownException.getGraphQlResponse();

		assertTrue(response.hasErrors());
		assertEquals(EXAMPLE_ERROR, response.getErrors().get(0).getMessage());
		assertEquals(EXAMPLE_DATA, response.getData().toString());

		HttpRequest capturedRequest = requestCaptor.getValue();

		assertEquals("http://localhost:4502/content/graphql/global/endpoint.json", capturedRequest.uri().toString());
		assertEquals("POST", capturedRequest.method());
		assertEquals(AEMHeadlessClient.CONTENT_TYPE_JSON,
				capturedRequest.headers().firstValue(AEMHeadlessClient.HEADER_CONTENT_TYPE).get());

	}

	@Test
	void testRunPersistedQueryNoErrors() throws Exception {

		prepareResponse(200, RESPONSE_NO_ERRORS);

		GraphQlResponse response = aemHeadlessClient.runPersistedQuery(PERSISTED_QUERY_PATH);

		HttpRequest capturedRequest = requestCaptor.getValue();

		assertEquals("http://localhost:4502/graphql/execute.json" + PERSISTED_QUERY_PATH,
				capturedRequest.uri().toString());
		assertEquals("GET", capturedRequest.method());
		assertEquals(AEMHeadlessClient.CONTENT_TYPE_JSON,
				capturedRequest.headers().firstValue(AEMHeadlessClient.HEADER_CONTENT_TYPE).get());

		assertFalse(response.hasErrors());
		assertEquals(EXAMPLE_DATA, response.getData().toString());

	}

	@Test
	void testRunPersistedQueryErrorsWithoutData() throws Exception {

		prepareResponse(200, RESPONSE_ERRORS);

		AEMHeadlessClientException thrownException = assertThrows(AEMHeadlessClientException.class, () -> {
			aemHeadlessClient.runPersistedQuery(PERSISTED_QUERY_PATH);
		});

		GraphQlResponse response = thrownException.getGraphQlResponse();

		assertTrue(response.hasErrors());
		assertEquals(EXAMPLE_ERROR, response.getErrors().get(0).getMessage());
		assertNull(response.getData());

		HttpRequest capturedRequest = requestCaptor.getValue();

		assertEquals("http://localhost:4502/graphql/execute.json" + PERSISTED_QUERY_PATH,
				capturedRequest.uri().toString());
		assertEquals("GET", capturedRequest.method());
		assertEquals(AEMHeadlessClient.CONTENT_TYPE_JSON,
				capturedRequest.headers().firstValue(AEMHeadlessClient.HEADER_CONTENT_TYPE).get());

	}

	@Test
	void testRunPersistedQueryErrorsWithData() throws Exception {

		prepareResponse(200, RESPONSE_ERRORS_AND_DATA);

		AEMHeadlessClientException thrownException = assertThrows(AEMHeadlessClientException.class, () -> {
			aemHeadlessClient.runPersistedQuery(PERSISTED_QUERY_PATH);
		});

		GraphQlResponse response = thrownException.getGraphQlResponse();

		assertTrue(response.hasErrors());
		assertEquals(EXAMPLE_ERROR, response.getErrors().get(0).getMessage());
		assertEquals(EXAMPLE_DATA, response.getData().toString());

		HttpRequest capturedRequest = requestCaptor.getValue();

		assertEquals("http://localhost:4502/graphql/execute.json" + PERSISTED_QUERY_PATH,
				capturedRequest.uri().toString());
		assertEquals("GET", capturedRequest.method());
		assertEquals(AEMHeadlessClient.CONTENT_TYPE_JSON,
				capturedRequest.headers().firstValue(AEMHeadlessClient.HEADER_CONTENT_TYPE).get());

	}

	@Test
	void testRunPersistedQueryWithVars() throws Exception {

		prepareResponse(200, RESPONSE_NO_ERRORS);

		Map<String, Object> vars = new HashMap<>();
		vars.put("prop", "test");
		GraphQlResponse response = aemHeadlessClient.runPersistedQuery(PERSISTED_QUERY_PATH, vars);

		HttpRequest capturedRequest = requestCaptor.getValue();

		assertEquals("http://localhost:4502/graphql/execute.json" + PERSISTED_QUERY_PATH + ";prop=test",
				capturedRequest.uri().toString());
		assertEquals("GET", capturedRequest.method());
		assertEquals(AEMHeadlessClient.CONTENT_TYPE_JSON,
				capturedRequest.headers().firstValue(AEMHeadlessClient.HEADER_CONTENT_TYPE).get());

		assertFalse(response.hasErrors());
		assertEquals(EXAMPLE_DATA, response.getData().toString());

	}

	@Test
	void testListPersistedQueries() throws Exception {

		prepareResponse(200, RESPONSE_LIST_QUERIES);

		List<PersistedQuery> queries = aemHeadlessClient.listPersistedQueries(PERSISTED_QUERY_PRJ);

		HttpRequest capturedRequest = requestCaptor.getValue();

		assertEquals("http://localhost:4502/graphql/list.json/" + PERSISTED_QUERY_PRJ,
				capturedRequest.uri().toString());
		assertEquals("GET", capturedRequest.method());
		assertEquals(AEMHeadlessClient.CONTENT_TYPE_JSON,
				capturedRequest.headers().firstValue(AEMHeadlessClient.HEADER_CONTENT_TYPE).get());

		assertEquals(1, queries.size());
		assertEquals(PERSISTED_QUERY_PATH, queries.get(0).getShortPath());
		assertEquals(QUERY, queries.get(0).getQuery());

	}

	private void prepareResponse(int statusCode, String body) throws Exception {

		doReturn(response).when(client).send(requestCaptor.capture(), eq(BodyHandlers.ofString()));

		when(response.statusCode()).thenReturn(statusCode);
		when(response.body()).thenReturn(body);
	}

	private JsonNode getBodyAsJson(HttpRequest capturedRequest) throws JsonProcessingException, JsonMappingException {
		assertTrue(capturedRequest.bodyPublisher().isPresent());
		BodyPublisher bodyPublisher = capturedRequest.bodyPublisher().get();
		String bodyString = getBodyString(bodyPublisher);
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode bodyJson = objectMapper.readTree(bodyString);
		return bodyJson;
	}

	private String getBodyString(HttpRequest.BodyPublisher bodyPublisher) {
		final List<ByteBuffer> bodyItems = new ArrayList<>();
		bodyPublisher.subscribe(new Flow.Subscriber<ByteBuffer>() {
			public void onSubscribe(Flow.Subscription subscription) {
				subscription.request(1);
			}

			public void onNext(ByteBuffer item) {
				bodyItems.add(item);
			}

			public void onError(Throwable throwable) {
			}

			public void onComplete() {
			}
		});

		return new String(bodyItems.get(0).array(), StandardCharsets.UTF_8);
	}

}
