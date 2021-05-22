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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Client that simplifies interaction with AEM GraphQL endpoints.
 */
public class AEMHeadlessClient {

	static final String ENDPOINT_DEFAULT_GRAPHQL = "/content/graphql/global/endpoint.json";
	static final String ENDPOINT_PERSISTED_QUERIES_PERSIST = "/graphql/persist.json";
	static final String ENDPOINT_PERSISTED_QUERIES_EXECUTE = "/graphql/execute.json";
	static final String ENDPOINT_PERSISTED_QUERIES_LIST = "/graphql/list.json/";

	static final String HEADER_CONTENT_TYPE = "Content-Type";
	static final String HEADER_ACCEPT = "Accept";
	static final String HEADER_AUTHORIZATION = "Authorization";
	static final String AUTH_BEARER = "Bearer";
	static final String AUTH_BASIC = "Basic";
	static final String CONTENT_TYPE_JSON = "application/json";
	static final String SPACE = " ";
	static final String SLASH = "/";

	static final String JSON_KEY_QUERY = "query";
	static final String JSON_KEY_QUERIES = "queries";
	static final String JSON_KEY_LONG_FORM = "longForm";
	static final String JSON_KEY_SHORT_FORM = "shortForm";
	static final String JSON_KEY_SHORT_PATH = "shortPath";
	static final String JSON_KEY_DATA = "data";
	static final String JSON_KEY_ERRORS = "errors";
	static final String JSON_KEY_MESSAGE = "message";
	static final String JSON_KEY_PATH = "path";
	static final String JSON_KEY_VARIABLES = "variables";

	static final Duration CONNECTION_TIMEOUT_DEFAULT = Duration.ofMinutes(1);

	private URI endpoint;
	private String authorizationHeader = null;

	private HttpClient httpClient = null;

	/**
	 * Builder that allows to configure all available options of the
	 * {@code AEMHeadlessClient}
	 * 
	 * @return builder
	 * 
	 */
	public static @NotNull AEMHeadlessClientBuilder builder() {
		return new AEMHeadlessClientBuilder();
	}

	AEMHeadlessClient() {
		// used by builder only
	}

	/**
	 * Creates a simple client (no authentication or other configuration). For more
	 * complex cases use {@link #builder()}.
	 * 
	 * If the endpoint points to a server only without path (e.g.
	 * {@code http:/myserver:8080}), then the default endpoint for GraphQL queries
	 * {@code /content/graphql/global/endpoint.json} is taken.
	 * 
	 * @param endpoint the endpoint to run queries against
	 * @throws URISyntaxException if the endpoint is an invalid URI
	 * 
	 */
	public AEMHeadlessClient(@NotNull String endpoint) throws URISyntaxException {
		setEndpoint(new URI(endpoint));
	}

	/**
	 * Runs the given GraphQL query on server.
	 * 
	 * @param query the query to execute
	 * @return the {@link GraphQlResponse}
	 * @throws AEMHeadlessClientException if the query cannot be executed
	 */
	public @NotNull GraphQlResponse runQuery(@NotNull String query) {
		return runQuery(query, null);
	}

	/**
	 * Runs the given GraphQL query on server.
	 * 
	 * @param query the query to execute
	 * @return the {@link GraphQlResponse}
	 * @throws AEMHeadlessClientException if the query cannot be executed
	 */
	public @NotNull GraphQlResponse runQuery(@NotNull String query, Map<String, Object> variables) {

		String queryStr = createQuery(query, variables);

		HttpRequest request = getDefaultRequestBuilder() //
				.uri(endpoint) //
				.POST(BodyPublishers.ofString(queryStr)) //
				.build();

		HttpResponse<String> responseStr = executeRequest(request, 200);
		JsonNode responseJson = stringToJson(responseStr.body());
		GraphQlResponse graphQlResponse = new GraphQlResponse(responseJson);
		if (graphQlResponse.hasErrors()) {
			throw new AEMHeadlessClientException(graphQlResponse);
		}
		return graphQlResponse;
	}

	/**
	 * Lists all persisted queries for the given configuration name.
	 * 
	 * @param configurationName the configuration name to list queries for
	 * @return list of {@link PersistedQuery}s
	 * @throws AEMHeadlessClientException if the persisted queries cannot be
	 *                                    retrieved
	 */
	public @NotNull List<PersistedQuery> listPersistedQueries(@NotNull String configurationName) {

		HttpRequest request = getDefaultRequestBuilder() //
				.uri(getUriForPath(endpoint, ENDPOINT_PERSISTED_QUERIES_LIST + configurationName)) //
				.GET() //
				.build();

		HttpResponse<String> response = executeRequest(request, 200);

		JsonNode persistedQueriesJson = stringToJson(response.body());

		JsonNode persistedQueriesNode = persistedQueriesJson.get(0).get(JSON_KEY_QUERIES);

		List<PersistedQuery> persistedQueries = new ArrayList<>();

		Iterator<JsonNode> persistedQueriesNodeIt = persistedQueriesNode.elements();
		while (persistedQueriesNodeIt.hasNext()) {
			JsonNode persistedQueryNode = persistedQueriesNodeIt.next();
			persistedQueries.add(new PersistedQuery(persistedQueryNode));
		}
		return persistedQueries;
	}

	/**
	 * Runs a persisted query on the server.
	 * 
	 * @param persistedQuery a {@link PersistedQuery} as retrieved by
	 *                       {@link #listPersistedQueries(String)}.
	 * @return the {@link GraphQlResponse}
	 * @throws AEMHeadlessClientException if the persisted query cannot be executed
	 */
	public @NotNull GraphQlResponse runPersistedQuery(@NotNull PersistedQuery persistedQuery) {
		return runPersistedQuery(persistedQuery.getShortPath(), null);
	}

	/**
	 * Runs a persisted query on the server.
	 * 
	 * @param persistedQuery a {@link PersistedQuery} as retrieved by
	 *                       {@link #listPersistedQueries(String)}. * @param
	 *                       variables variables for the persisted query
	 * @return the {@link GraphQlResponse}
	 * @throws AEMHeadlessClientException if the persisted query cannot be executed
	 */
	public @NotNull GraphQlResponse runPersistedQuery(@NotNull PersistedQuery persistedQuery,
			Map<String, Object> variables) {
		return runPersistedQuery(persistedQuery.getShortPath(), variables);
	}

	/**
	 * Runs a persisted query on the server.
	 * 
	 * @param persistedQueryPath the persisted query path, e.g.
	 *                           {@code /myproj/myquery}
	 * @return the {@link GraphQlResponse}
	 * @throws AEMHeadlessClientException if the persisted query cannot be executed
	 */
	public @NotNull GraphQlResponse runPersistedQuery(@NotNull String persistedQueryPath) {
		return runPersistedQuery(persistedQueryPath, null);
	}

	/**
	 * Runs a persisted query on the server.
	 * 
	 * @param persistedQueryPath the persisted query path, e.g.
	 *                           {@code /myproj/myquery}
	 * @param variables          variables for the persisted query
	 * @return the {@link GraphQlResponse}
	 * @throws AEMHeadlessClientException if the persisted query cannot be executed
	 */
	public @NotNull GraphQlResponse runPersistedQuery(@NotNull String persistedQueryPath,
			Map<String, Object> variables) {

		validatePersistedQueryPath(persistedQueryPath);

		HttpRequest request = getDefaultRequestBuilder() //
				.uri(getUriForPath(endpoint, ENDPOINT_PERSISTED_QUERIES_EXECUTE + persistedQueryPath)) //
				.GET() //
				.build();

		HttpResponse<String> responseStr = executeRequest(request, 200);
		JsonNode responseJson = stringToJson(responseStr.body());
		GraphQlResponse graphQlResponse = new GraphQlResponse(responseJson);
		if (graphQlResponse.hasErrors()) {
			throw new AEMHeadlessClientException(graphQlResponse);
		}
		return graphQlResponse;
	}

	/**
	 * Adds a new persisted query on the server.
	 * 
	 * @param queryToPersist     the query
	 * @param persistedQueryPath the path to persist the query, e.g.
	 *                           {@code /myproj/myquery}
	 * @return the saved {@link PersistedQuery}
	 * @throws AEMHeadlessClientException if the query cannot be persisted
	 */
	public @NotNull PersistedQuery persistQuery(@NotNull String queryToPersist, @NotNull String persistedQueryPath) {
		validatePersistedQueryPath(persistedQueryPath);

		HttpRequest request = getDefaultRequestBuilder() //
				.uri(getUriForPath(endpoint, ENDPOINT_PERSISTED_QUERIES_PERSIST + persistedQueryPath)) //
				.PUT(BodyPublishers.ofString(queryToPersist))//
				.build();

		HttpResponse<String> responseStr = executeRequest(request, 201);
		JsonNode responseJson = stringToJson(responseStr.body());
		return new PersistedQuery(responseJson.get(JSON_KEY_SHORT_PATH).asText(),
				responseJson.get(JSON_KEY_PATH).asText(), queryToPersist);
	}

	URI getEndpoint() {
		return endpoint;
	}

	void setEndpoint(URI endpoint) {
		if (isBlank(endpoint.getPath()) || SLASH.equals(endpoint.getPath())) {
			this.endpoint = getUriForPath(endpoint, ENDPOINT_DEFAULT_GRAPHQL);
		} else {
			this.endpoint = endpoint;
		}
	}

	String getAuthorizationHeader() {
		return authorizationHeader;
	}

	void setAuthorizationHeader(String authorizationHeader) {
		this.authorizationHeader = authorizationHeader;
	}

	HttpClient getHttpClient() {
		return httpClient;
	}

	void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	String createQuery(String query, Map<String, Object> variables) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode queryNode = mapper.createObjectNode();
		queryNode.put(JSON_KEY_QUERY, query);
		queryNode.set(JSON_KEY_VARIABLES, mapper.valueToTree(variables));
		return queryNode.toString();
	}

	static String basicAuthHeaderVal(String username, String password) {
		try {
			return AUTH_BASIC + SPACE + Base64.getEncoder()
					.encodeToString((username + ":" + password).getBytes(StandardCharsets.ISO_8859_1.name()));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Charset StandardCharsets.ISO_8859_1 unexpectedly does not exist", e);
		}
	}

	static String tokenAuthHeaderVal(String token) {
		return AUTH_BEARER + SPACE + token;
	}

	private URI getUriForPath(URI endpoint, String path) {
		try {
			return new URI(endpoint.getScheme(), endpoint.getUserInfo(), endpoint.getHost(), endpoint.getPort(), path,
					null, null);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid GraphQL URI " + endpoint, e);
		}
	}

	private Builder getDefaultRequestBuilder() {
		java.net.http.HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
				.header(HEADER_ACCEPT, CONTENT_TYPE_JSON).header(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
		if (!isBlank(authorizationHeader)) {
			requestBuilder = requestBuilder.header(HEADER_AUTHORIZATION, authorizationHeader);
		}
		return requestBuilder;
	}

	HttpResponse<String> executeRequest(HttpRequest request, int expectedCode) {
		HttpResponse<String> response;
		try {
			response = httpClient.send(request, BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			throw new AEMHeadlessClientException("Http error: " + e, e);
		}

		if (response.statusCode() != expectedCode) {
			throw new AEMHeadlessClientException(
					"Unexpected http response code " + response.statusCode() + ": " + response.body());
		}

		return response;
	}

	private JsonNode stringToJson(String jsonResponseStr) {
		ObjectMapper jsonMapper = new ObjectMapper();
		try {
			JsonNode json = jsonMapper.readTree(jsonResponseStr);
			return json;
		} catch (JsonProcessingException e) {
			throw new AEMHeadlessClientException("Could not parse GraphQL response from AEM Server: " + e, e);
		}
	}

	void validatePersistedQueryPath(String persistedQueryPath) {
		if (persistedQueryPath == null || !persistedQueryPath.startsWith(SLASH)
				|| persistedQueryPath.split(SLASH).length != 3) {
			throw new IllegalArgumentException("Invalid path for persisted query: " + persistedQueryPath);
		}
	}

	private static boolean isBlank(final CharSequence cs) {
		return cs == null || cs.chars().allMatch(Character::isWhitespace);
	}
}
