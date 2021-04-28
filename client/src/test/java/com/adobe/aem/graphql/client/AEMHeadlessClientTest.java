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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AEMHeadlessClientTest {
	private static final String QUERY = "{ myObjects { items { prop } } } ";
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
		aemHeadlessClient = new AEMHeadlessClient(new URI("http://localhost:4502"));
		aemHeadlessClient.setClient(client);
	}

	@Test
	void testBasicConstructors() throws Exception {

		// default
		assertEquals("http://localhost:4502/content/graphql/global/endpoint.json",
				aemHeadlessClient.getEndpoint().toString());

		aemHeadlessClient = new AEMHeadlessClient(new URI("http://localhost:4502/"));
		assertEquals("http://localhost:4502/content/graphql/global/endpoint.json",
				aemHeadlessClient.getEndpoint().toString(), "trailing slash must still use default URI");

		aemHeadlessClient = new AEMHeadlessClient(new URI("http://localhost:4502/custom/endpoint.json"));
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
	void testPostQueryNoErrors() throws Exception {

		prepareResponse(200, RESPONSE_NO_ERRORS);

		GraphQlResponse response = aemHeadlessClient.postQuery(QUERY);

		HttpRequest capturedRequest = requestCaptor.getValue();

		assertEquals("http://localhost:4502/content/graphql/global/endpoint.json", capturedRequest.uri().toString());
		assertEquals("POST", capturedRequest.method());
		assertEquals(AEMHeadlessClient.CONTENT_TYPE_JSON,
				capturedRequest.headers().firstValue(AEMHeadlessClient.HEADER_CONTENT_TYPE).get());

		assertFalse(response.hasErrors());
		assertEquals(EXAMPLE_DATA, response.getData().toString());

	}

	@Test
	void testPostQueryUnexpectedResponseCode() throws Exception {

		prepareResponse(404, "Not Found");

		AEMHeadlessClientException thrownException = assertThrows(AEMHeadlessClientException.class, () -> {
			aemHeadlessClient.postQuery(QUERY);
		});

		assertNull(thrownException.getGraphQlResponse());
		assertEquals("Unexpected http response code 404: Not Found", thrownException.getMessage());

	}

	@Test
	void testPostQueryErrorsWithoutData() throws Exception {

		prepareResponse(200, RESPONSE_ERRORS);

		AEMHeadlessClientException thrownException = assertThrows(AEMHeadlessClientException.class, () -> {
			aemHeadlessClient.postQuery(QUERY);
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
	void testPostQueryErrorsWithData() throws Exception {

		prepareResponse(200, RESPONSE_ERRORS_AND_DATA);

		AEMHeadlessClientException thrownException = assertThrows(AEMHeadlessClientException.class, () -> {
			aemHeadlessClient.postQuery(QUERY);
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
	void testGetQueryNoErrors() throws Exception {

		prepareResponse(200, RESPONSE_NO_ERRORS);

		GraphQlResponse response = aemHeadlessClient.getQuery(PERSISTED_QUERY_PATH);

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
	void testGetQueryErrorsWithoutData() throws Exception {

		prepareResponse(200, RESPONSE_ERRORS);

		AEMHeadlessClientException thrownException = assertThrows(AEMHeadlessClientException.class, () -> {
			aemHeadlessClient.getQuery(PERSISTED_QUERY_PATH);
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
	void testGetQueryErrorsWithData() throws Exception {

		prepareResponse(200, RESPONSE_ERRORS_AND_DATA);

		AEMHeadlessClientException thrownException = assertThrows(AEMHeadlessClientException.class, () -> {
			aemHeadlessClient.getQuery(PERSISTED_QUERY_PATH);
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
	void testListQueries() throws Exception {

		prepareResponse(200, RESPONSE_LIST_QUERIES);

		List<PersistedQuery> queries = aemHeadlessClient.listQueries(PERSISTED_QUERY_PRJ);

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

}
