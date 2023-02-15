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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AEMHeadlessClientTest {
	private static final String DEFAULT_ENDPOINT = "http://localhost:4502/content/cq:graphql/global/endpoint.json";

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

	@Mock
	HttpURLConnection httpURLConnection;

	@Captor
	ArgumentCaptor<URI> endpointCaptor;

	@BeforeEach
	void setup() throws Exception {
		aemHeadlessClient = spy(new AEMHeadlessClient("http://localhost:4502"));
	}

	@Test
	void testBasicConstructor() throws Exception {

		// default
		assertEquals("http://localhost:4502/content/cq:graphql/global/endpoint.json",
				aemHeadlessClient.getEndpoint().toString());

		aemHeadlessClient = new AEMHeadlessClient("http://localhost:4502/");
		assertEquals("http://localhost:4502/content/cq:graphql/global/endpoint.json",
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

		URI endpoint = new URI(DEFAULT_ENDPOINT);

		String expectedQuery = aemHeadlessClient.createQueryRequestPayload(QUERY, null);
		doReturn(RESPONSE_NO_ERRORS).when(aemHeadlessClient).executeRequest(endpoint, AEMHeadlessClient.METHOD_POST,
				expectedQuery, 200);

		GraphQlResponse response = aemHeadlessClient.runQuery(QUERY);

		verify(aemHeadlessClient, times(1)).executeRequest(endpoint, AEMHeadlessClient.METHOD_POST, expectedQuery, 200);

		assertFalse(response.hasErrors());
		assertEquals(EXAMPLE_DATA, response.getData().toString());

	}

	@Test
	void testRunQueryWithVars() throws Exception {

		URI endpoint = new URI(DEFAULT_ENDPOINT);

		Map<String, Object> vars = new HashMap<>();
		vars.put("prop", "test");
		String expectedQuery = aemHeadlessClient.createQueryRequestPayload(QUERY_WITH_VAR, vars);

		doReturn(RESPONSE_NO_ERRORS).when(aemHeadlessClient).executeRequest(endpoint, AEMHeadlessClient.METHOD_POST,
				expectedQuery, 200);

		GraphQlResponse response = aemHeadlessClient.runQuery(QUERY_WITH_VAR, vars);

		verify(aemHeadlessClient, times(1)).executeRequest(endpoint, AEMHeadlessClient.METHOD_POST, expectedQuery, 200);

		assertFalse(response.hasErrors());
		assertEquals(EXAMPLE_DATA, response.getData().toString());

	}

	@Test
	void testRunQueryErrorsWithoutData() throws Exception {

		URI endpoint = new URI(DEFAULT_ENDPOINT);

		String expectedQuery = aemHeadlessClient.createQueryRequestPayload(QUERY, null);
		doReturn(RESPONSE_ERRORS).when(aemHeadlessClient).executeRequest(endpoint, AEMHeadlessClient.METHOD_POST,
				expectedQuery, 200);

		AEMHeadlessClientException thrownException = assertThrows(AEMHeadlessClientException.class, () -> {
			aemHeadlessClient.runQuery(QUERY);
		});

		GraphQlResponse response = thrownException.getGraphQlResponse();

		assertTrue(response.hasErrors());
		assertEquals(EXAMPLE_ERROR, response.getErrors().get(0).getMessage());
		assertNull(response.getData());

	}

	@Test
	void testRunQueryErrorsWithData() throws Exception {

		URI endpoint = new URI(DEFAULT_ENDPOINT);

		String expectedQuery = aemHeadlessClient.createQueryRequestPayload(QUERY, null);
		doReturn(RESPONSE_ERRORS_AND_DATA).when(aemHeadlessClient).executeRequest(endpoint,
				AEMHeadlessClient.METHOD_POST, expectedQuery, 200);

		AEMHeadlessClientException thrownException = assertThrows(AEMHeadlessClientException.class, () -> {
			aemHeadlessClient.runQuery(QUERY);
		});

		GraphQlResponse response = thrownException.getGraphQlResponse();

		assertTrue(response.hasErrors());
		assertEquals(EXAMPLE_ERROR, response.getErrors().get(0).getMessage());
		assertEquals(EXAMPLE_DATA, response.getData().toString());

	}

	@Test
	void testRunPersistedQueryNoErrors() throws Exception {

		URI endpoint = new URI("http://localhost:4502/graphql/execute.json" + PERSISTED_QUERY_PATH);

		doReturn(RESPONSE_NO_ERRORS).when(aemHeadlessClient).executeRequest(endpoint, AEMHeadlessClient.METHOD_GET,
				null, 200);

		GraphQlResponse response = aemHeadlessClient.runPersistedQuery(PERSISTED_QUERY_PATH);

		assertFalse(response.hasErrors());
		assertEquals(EXAMPLE_DATA, response.getData().toString());

	}

	@Test
	void testRunPersistedQueryErrorsWithoutData() throws Exception {

		URI endpoint = new URI("http://localhost:4502/graphql/execute.json" + PERSISTED_QUERY_PATH);

		doReturn(RESPONSE_ERRORS).when(aemHeadlessClient).executeRequest(endpoint, AEMHeadlessClient.METHOD_GET, null,
				200);

		AEMHeadlessClientException thrownException = assertThrows(AEMHeadlessClientException.class, () -> {
			aemHeadlessClient.runPersistedQuery(PERSISTED_QUERY_PATH);
		});

		GraphQlResponse response = thrownException.getGraphQlResponse();

		assertTrue(response.hasErrors());
		assertEquals(EXAMPLE_ERROR, response.getErrors().get(0).getMessage());
		assertNull(response.getData());
	}

	@Test
	void testRunPersistedQueryErrorsWithData() throws Exception {

		URI endpoint = new URI("http://localhost:4502/graphql/execute.json" + PERSISTED_QUERY_PATH);

		doReturn(RESPONSE_ERRORS_AND_DATA).when(aemHeadlessClient).executeRequest(endpoint,
				AEMHeadlessClient.METHOD_GET, null, 200);

		AEMHeadlessClientException thrownException = assertThrows(AEMHeadlessClientException.class, () -> {
			aemHeadlessClient.runPersistedQuery(PERSISTED_QUERY_PATH);
		});

		GraphQlResponse response = thrownException.getGraphQlResponse();

		assertTrue(response.hasErrors());
		assertEquals(EXAMPLE_ERROR, response.getErrors().get(0).getMessage());
		assertEquals(EXAMPLE_DATA, response.getData().toString());

	}

	@Test
	void testRunPersistedQueryWithVars() throws Exception {

		doReturn(RESPONSE_NO_ERRORS).when(aemHeadlessClient).executeRequest(any(URI.class),
				eq(AEMHeadlessClient.METHOD_GET), isNull(), eq(200));

		Map<String, Object> vars = new HashMap<>();
		vars.put("prop", "test");
		GraphQlResponse response = aemHeadlessClient.runPersistedQuery(PERSISTED_QUERY_PATH, vars);

		assertFalse(response.hasErrors());
		assertEquals(EXAMPLE_DATA, response.getData().toString());

		verify(aemHeadlessClient).executeRequest(endpointCaptor.capture(), eq(AEMHeadlessClient.METHOD_GET), any(),
				eq(200));
		URI actualUri = endpointCaptor.getValue();
		assertEquals("http://localhost:4502/graphql/execute.json/proj/queryName;prop=test", actualUri.toString());

	}

	@Test
	void testListPersistedQueries() throws Exception {

		doReturn(RESPONSE_LIST_QUERIES).when(aemHeadlessClient).executeRequest(any(URI.class),
				eq(AEMHeadlessClient.METHOD_GET), isNull(), eq(200));

		List<PersistedQuery> queries = aemHeadlessClient.listPersistedQueries(PERSISTED_QUERY_PRJ);

		verify(aemHeadlessClient).executeRequest(endpointCaptor.capture(), eq(AEMHeadlessClient.METHOD_GET), any(),
				eq(200));

		assertEquals("http://localhost:4502/graphql/list.json/" + PERSISTED_QUERY_PRJ,
				endpointCaptor.getValue().toString());

		assertEquals(1, queries.size());
		assertEquals(PERSISTED_QUERY_PATH, queries.get(0).getShortPath());
		assertEquals(QUERY, queries.get(0).getQuery());

	}

	@Test
	void testExecuteRequest() throws Exception {

		doReturn(httpURLConnection).when(aemHeadlessClient).openHttpConnection(any(URI.class));

		when(httpURLConnection.getResponseCode()).thenReturn(200);
		ByteArrayOutputStream requestEntityOutputStream = new ByteArrayOutputStream();
		when(httpURLConnection.getOutputStream()).thenReturn(requestEntityOutputStream);
		InputStream responseEntityInputStream = new ByteArrayInputStream(
				RESPONSE_NO_ERRORS.getBytes(StandardCharsets.UTF_8));
		when(httpURLConnection.getInputStream()).thenReturn(responseEntityInputStream);

		URI endpoint = new URI("http://localhost:4502/content/graphql/global/endpoint.json");

		String expectedQuery = aemHeadlessClient.createQueryRequestPayload(QUERY, null);
		String response = aemHeadlessClient.executeRequest(endpoint, AEMHeadlessClient.METHOD_POST, expectedQuery, 200);
		assertEquals(RESPONSE_NO_ERRORS, response);
		assertEquals(expectedQuery, requestEntityOutputStream.toString(StandardCharsets.UTF_8.name()));

		verify(httpURLConnection, times(1)).setRequestProperty(AEMHeadlessClient.HEADER_ACCEPT,
				AEMHeadlessClient.CONTENT_TYPE_JSON);
		verify(httpURLConnection, times(1)).setRequestProperty(AEMHeadlessClient.HEADER_CONTENT_TYPE,
				AEMHeadlessClient.CONTENT_TYPE_JSON);

		verify(httpURLConnection, never()).setRequestProperty(eq(AEMHeadlessClient.HEADER_AUTHORIZATION), any());

	}

	@Test
	void testExecuteRequestWithAuthentication() throws Exception {

		doReturn(httpURLConnection).when(aemHeadlessClient).openHttpConnection(any(URI.class));

		aemHeadlessClient.setAuthorizationHeader("test");
		when(httpURLConnection.getResponseCode()).thenReturn(200);
		when(httpURLConnection.getOutputStream()).thenReturn(new ByteArrayOutputStream());
		when(httpURLConnection.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

		URI endpoint = new URI("http://localhost:4502/content/graphql/global/endpoint.json");

		String expectedQuery = aemHeadlessClient.createQueryRequestPayload(QUERY, null);
		aemHeadlessClient.executeRequest(endpoint, AEMHeadlessClient.METHOD_POST, expectedQuery, 200);

		verify(httpURLConnection).setRequestProperty(AEMHeadlessClient.HEADER_ACCEPT,
				AEMHeadlessClient.CONTENT_TYPE_JSON);
		verify(httpURLConnection).setRequestProperty(AEMHeadlessClient.HEADER_CONTENT_TYPE,
				AEMHeadlessClient.CONTENT_TYPE_JSON);
		verify(httpURLConnection).setRequestProperty(AEMHeadlessClient.HEADER_AUTHORIZATION, "test");

		verify(httpURLConnection).setConnectTimeout(AEMHeadlessClient.DEFAULT_TIMEOUT);
		verify(httpURLConnection).setReadTimeout(AEMHeadlessClient.DEFAULT_TIMEOUT);

	}

	@Test
	void testExecuteRequestUnexpectedResponseCode() throws Exception {

		doReturn(httpURLConnection).when(aemHeadlessClient).openHttpConnection(Mockito.any(URI.class));

		when(httpURLConnection.getResponseCode()).thenReturn(404);
		ByteArrayOutputStream requestEntityOutputStream = new ByteArrayOutputStream();
		when(httpURLConnection.getOutputStream()).thenReturn(requestEntityOutputStream);
		InputStream responseEntityInputStream = new ByteArrayInputStream("Not Found".getBytes(StandardCharsets.UTF_8));
		when(httpURLConnection.getInputStream()).thenReturn(responseEntityInputStream);

		URI endpoint = new URI("http://localhost:4502/content/graphql/global/endpoint.json");

		AEMHeadlessClientException thrownException = assertThrows(AEMHeadlessClientException.class, () -> {
			aemHeadlessClient.executeRequest(endpoint, AEMHeadlessClient.METHOD_POST,
					aemHeadlessClient.createQueryRequestPayload(QUERY, null), 200);
		});

		assertNull(thrownException.getGraphQlResponse());
		assertEquals("Unexpected http response code 404: Not Found", thrownException.getMessage());

	}

}
