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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Client that simplifies interaction with AEM GraphQL endpoints.
 */
public class AEMHeadlessClient {

	static final String ENDPOINT_DEFAULT_GRAPHQL = "/content/cq:graphql/global/endpoint.json";

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
	static final String METHOD_GET = "GET";
	static final String METHOD_PUT = "PUT";
	static final String METHOD_POST = "POST";

	static final String JSON_KEY_QUERY = "query";
	static final String JSON_KEY_QUERIES = "queries";
	static final String JSON_KEY_LONG_FORM = "longForm";
	static final String JSON_KEY_SHORT_FORM = "shortForm";
	static final String JSON_KEY_SHORT_PATH = "shortPath";
	static final String JSON_KEY_DATA = "data";
	static final String JSON_KEY_ITEMS = "items";
	static final String JSON_KEY_EDGES = "edges";
	static final String JSON_KEY_NODE = "node";
	static final String JSON_KEY_PAGE_INFO = "pageInfo";
	static final String JSON_KEY_HAS_NEXT_PAGE = "hasNextPage";
	static final String JSON_KEY_END_CURSOR = "endCursor";
	
	static final String JSON_KEY_ERRORS = "errors";
	static final String JSON_KEY_MESSAGE = "message";
	static final String JSON_KEY_PATH = "path";
	static final String JSON_KEY_VARIABLES = "variables";

	static final int DEFAULT_TIMEOUT = 15000;

	private URI endpoint;
	private String authorizationHeader = null;
	private int connectTimeout = DEFAULT_TIMEOUT;
	private int readTimeout = DEFAULT_TIMEOUT;

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
	 * @param query     the query to execute
	 * @param variables variables for the query
	 * @return the {@link GraphQlResponse}
	 * @throws AEMHeadlessClientException if the query cannot be executed
	 */
	public @NotNull GraphQlResponse runQuery(@NotNull String query, Map<String, Object> variables) {

		String queryPayload = createQueryRequestPayload(query, variables);

		String responseStr = executeRequest(endpoint, METHOD_POST, queryPayload, 200);

		JsonNode responseJson = stringToJson(responseStr);
		GraphQlResponse graphQlResponse = new GraphQlResponse(responseJson);
		if (graphQlResponse.hasErrors()) {
			throw new AEMHeadlessClientException(graphQlResponse);
		}
		return graphQlResponse;
	}
	
	/**
	 * Runs the given {@link GraphQlQuery} on server.
	 * 
	 * @param query  the query that has to declare the variables $next and $after
	 * @return the {@link GraphQlResponse}
	 * @throws AEMHeadlessClientException if the query cannot be executed
	 */
	public @NotNull GraphQlResponse runQuery(@NotNull GraphQlQuery query) {
		return runQuery(query.generateQuery(), GraphQlQueryVars.create());
	}

	/**
	 * Runs the given {@link GraphQlQuery} on server with given variables.
	 * 
	 * @param query     the query that has to declare the variables $next and $after
	 * @param variables variables for the query
	 * @return the {@link GraphQlResponse}
	 * @throws AEMHeadlessClientException if the query cannot be executed
	 */
	public @NotNull GraphQlResponse runQuery(@NotNull GraphQlQuery query, Map<String, Object> variables) {
		return runQuery(query.generateQuery(), GraphQlQueryVars.create(variables));
	}

	/**
	 * Runs the query with the paging variables. Expects 'offset' and 'limit' to
	 * exist as query variables, {@link GraphQlQueryBuilder} helps creating query in
	 * the correct format.
	 * 
	 * @param query  the query that has to declare the variables $next and $after
	 * @param offset the paging offset to run the query with
	 * @param limit  the paging limit to run the query with
	 * @return the {@link GraphQlResponse}
	 * @throws AEMHeadlessClientException if the query cannot be executed
	 */
	public @NotNull GraphQlResponse runQuery(@NotNull GraphQlQuery query, int offset, int limit) {
		return runQuery(query, GraphQlQueryVars.create().offset(offset).limit(limit));
	}

	/**
	 * Create cursor to retrieve paged responses. By convention, the query needs to
	 * define the query variables $next and $after.
	 * 
	 * @param query    the query that defines the query variables $next and $after
	 * @param pageSize the page size for the cursor
	 * @return a {@link GraphQlPagingCursor}
	 */
	public @NotNull GraphQlPagingCursor createPagingCursor(@NotNull GraphQlQuery query, int pageSize) {
		return new GraphQlResponse.PagingCursorImpl(query, pageSize, GraphQlQueryVars.create(), this);
	}

	/**
	 * Create cursor to retrieve paged responses. By convention, the query needs to
	 * define the query variables $next and $after. Allows to specify additional
	 * variables the query requires.
	 * 
	 * @param query     query the query that defines the query variables $next and
	 *                  $after
	 * @param pageSize  pageSize the page size for the cursor
	 * @param variables additional variables as required by query
	 * @return a {@link GraphQlPagingCursor}
	 */
	public @NotNull GraphQlPagingCursor createPagingCursor(@NotNull GraphQlQuery query, int pageSize, Map<String, Object> variables) {
		return new GraphQlResponse.PagingCursorImpl(query, pageSize, GraphQlQueryVars.create(variables), this);
	}

	/**
	 * Create cursor to retrieve paged responses for a {@link PersistedQuery}. By convention, the {@link PersistedQuery} needs to define the query variables $next and $after. 
	 * 
	 * @param persistedQuery persisted query that defines the query variables $next and $after 
	 * @param pageSize pageSize the page size for the cursor
	 * @return a {@link GraphQlPagingCursor}
	 */
	public @NotNull GraphQlPagingCursor createPagingCursor(@NotNull PersistedQuery persistedQuery, int pageSize) {
		return new GraphQlResponse.PagingCursorImpl(persistedQuery, pageSize, GraphQlQueryVars.create(), this);
	}

	/**
	 * Create cursor to retrieve paged responses for a {@link PersistedQuery}. By convention, the {@link PersistedQuery} needs to define the query variables $next and $after. Allows to specify additional variables the query requires.
	 * 
	 * @param persistedQuery persisted query that defines the query variables $next and $after 
	 * @param pageSize pageSize the page size for the cursor
	 * @param variables additional variables as required by persisted query
	 * @return a {@link GraphQlPagingCursor}
	 */
	public @NotNull GraphQlPagingCursor createPagingCursor(@NotNull PersistedQuery persistedQuery, int pageSize, Map<String, Object> variables) {
		return new GraphQlResponse.PagingCursorImpl(persistedQuery, pageSize, GraphQlQueryVars.create(variables), this);
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

		String responseStr = executeRequest(
				getUriForPath(endpoint, ENDPOINT_PERSISTED_QUERIES_LIST + configurationName), METHOD_GET, null, 200);

		JsonNode persistedQueriesJson = stringToJson(responseStr);

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
	 *                       {@link #listPersistedQueries(String)}
	 * @param variables      variables for the persisted query
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

		String requestPath = ENDPOINT_PERSISTED_QUERIES_EXECUTE + persistedQueryPath;
		if (variables != null) {
			requestPath += variables.entrySet().stream().map(this::mapEntryToReqParam).collect(Collectors.joining());
		}

		String responseStr = executeRequest(getUriForPath(endpoint, requestPath), METHOD_GET, null, 200);

		JsonNode responseJson = stringToJson(responseStr);
		GraphQlResponse graphQlResponse = new GraphQlResponse(responseJson);
		if (graphQlResponse.hasErrors()) {
			throw new AEMHeadlessClientException(graphQlResponse);
		}
		return graphQlResponse;
	}

	private String mapEntryToReqParam(Map.Entry<String, Object> entry) {
		try {
			return ";" + entry.getKey() + "="
					+ URLEncoder.encode(String.valueOf(entry.getValue()), StandardCharsets.ISO_8859_1.name());
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Encoding " + StandardCharsets.ISO_8859_1 + " not supported", e);
		}
	}

	/**
	 * Adds a new persisted query on the server. Usually it is better to deploy persisted queries along with the code with filevault packages.
	 * 
	 * @param queryToPersist     the query
	 * @param persistedQueryPath the path to persist the query, e.g.
	 *                           {@code /myproj/myquery}
	 * @return the saved {@link PersistedQuery}
	 * @throws AEMHeadlessClientException if the query cannot be persisted
	 */
	public @NotNull PersistedQuery persistQuery(@NotNull String queryToPersist, @NotNull String persistedQueryPath) {
		validatePersistedQueryPath(persistedQueryPath);

		String responseStr = executeRequest(
				getUriForPath(endpoint, ENDPOINT_PERSISTED_QUERIES_PERSIST + persistedQueryPath), METHOD_PUT,
				queryToPersist, 201);
		JsonNode responseJson = stringToJson(responseStr);
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

	int getConnectTimeout() {
		return connectTimeout;
	}

	void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	int getReadTimeout() {
		return readTimeout;
	}

	void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	String createQueryRequestPayload(String query, Map<String, Object> variables) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode queryNode = mapper.createObjectNode();
		queryNode.put(JSON_KEY_QUERY, query);
		if (variables != null) {
			GraphQlQueryVars.checkQueryForVars(query, variables.keySet());
			queryNode.set(JSON_KEY_VARIABLES, mapper.valueToTree(variables));
		}
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

	String executeRequest(URI uri, String method, String entity, int expectedCode) {

		try {
			HttpURLConnection httpCon = openHttpConnection(uri);
			httpCon.setConnectTimeout(connectTimeout);
			httpCon.setReadTimeout(readTimeout);
			httpCon.setRequestMethod(method);
			httpCon.setRequestProperty(HEADER_ACCEPT, CONTENT_TYPE_JSON);
			httpCon.setRequestProperty(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
			if (!isBlank(authorizationHeader)) {
				httpCon.setRequestProperty(HEADER_AUTHORIZATION, authorizationHeader);
			}
			httpCon.setDoOutput(true);

			if (entity != null) {
				try (OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream(),
						StandardCharsets.UTF_8)) {
					out.write(entity);
				}
			}

			String responseBody;
			try (InputStream inputStream = httpCon.getInputStream()) {
				responseBody = inputStreamToString(inputStream);
			}

			int responseCode = httpCon.getResponseCode();

			if (responseCode != expectedCode) {
				throw new AEMHeadlessClientException(
						"Unexpected http response code " + responseCode + ": " + responseBody);
			}

			return responseBody;
		} catch (AEMHeadlessClientException e) {
			throw e;
		} catch (Exception e) {
			throw new AEMHeadlessClientException("Could not execute " + method + " request to " + uri + ": " + e, e);
		}
	}

	HttpURLConnection openHttpConnection(URI uri) throws IOException {
		return (HttpURLConnection) uri.toURL().openConnection();
	}

	private String inputStreamToString(InputStream inputStream) throws IOException {
		int bufferSize = 1024;
		char[] buffer = new char[bufferSize];
		StringBuilder out = new StringBuilder();
		Reader in = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
		for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0;) {
			out.append(buffer, 0, numRead);
		}
		return out.toString();
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