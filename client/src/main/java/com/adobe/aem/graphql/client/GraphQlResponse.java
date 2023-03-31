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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * The client {@link AEMHeadlessClient} returns this class for the operations
 * {@link AEMHeadlessClient#runQuery(String)} and
 * {@link AEMHeadlessClient#runPersistedQuery(PersistedQuery)}. Use
 * {@link #getData()} to retrieve the
 * {@link com.fasterxml.jackson.databind.JsonNode} of the response.
 */
public class GraphQlResponse {

	private final JsonNode data;
	private final List<Error> errors;

	private final JsonNode items;
	private JsonNode pageInfo;

	GraphQlResponse(JsonNode response) {
		this.data = response.get(JSON_KEY_DATA);
		this.errors = readErrors(response);
		this.items = loadItems();
	}

	private List<Error> readErrors(JsonNode response) {
		if (response.has(JSON_KEY_ERRORS)) {
			List<Error> errors = new ArrayList<>();
			Iterator<JsonNode> errorsJsonIt = response.get(JSON_KEY_ERRORS).iterator();
			while (errorsJsonIt.hasNext()) {
				JsonNode errorJson = errorsJsonIt.next();
				errors.add(new Error(errorJson));
			}
			return errors;
		} else {
			return null;
		}
	}

	private JsonNode loadItems() {
		JsonNode items = null;
		if (data != null) {
			Iterator<JsonNode> elements = data.elements();
			while (elements.hasNext()) {
				JsonNode resultNode = elements.next();
				if (resultNode.has(AEMHeadlessClient.JSON_KEY_ITEMS)) {
					items = resultNode.get(AEMHeadlessClient.JSON_KEY_ITEMS);
					break;
				}
				if (resultNode.has(AEMHeadlessClient.JSON_KEY_EDGES)) {
					JsonNode edgesNode = resultNode.get(AEMHeadlessClient.JSON_KEY_EDGES);
					ArrayNode resultArrayNode = new ObjectMapper().createArrayNode();
					for (JsonNode node : (ArrayNode) edgesNode) {
						resultArrayNode.add(node.get(AEMHeadlessClient.JSON_KEY_NODE));
					}
					items = resultArrayNode;
					pageInfo = resultNode.get(AEMHeadlessClient.JSON_KEY_PAGE_INFO);
					break;
				}
			}
		}
		return items;
	}

	/**
	 * @return the String representation of the response.
	 */
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("[GraphQlResponse ");
		if (data != null) {
			buf.append("data: \n" + data.toPrettyString() + "\n");
		}
		if (errors != null) {
			buf.append("errors: " + getErrorsString());
		}
		buf.append("]");
		return buf.toString();
	}

	/**
	 * @return the {@link com.fasterxml.jackson.databind.JsonNode} or {@code null}
	 *         if no data was sent by server.
	 */
	public @Nullable JsonNode getData() {
		return data;
	}

	/**
	 * @return the response items if found at JSON path  {@code "data" ->"...List" ->
	 *         "items"} or null if no items could be found in response.
	 */
	public @Nullable JsonNode getItems() {
		return items;
	}

	/**
	 * Gets list of items mapped to given class (use Jackson annotations to define
	 * the mapping).
	 * 
	 * @param <T>   the type of the items returned
	 * @param clazz the class of the items returned
	 * @return the list of items
	 */
	public @Nullable <T> List<T> getItems(Class<T> clazz) {

		ObjectMapper mapper = new ObjectMapper();

		List<T> result = new ArrayList<>();
		for (JsonNode jsonNode : getItems()) {
			try {
				result.add(mapper.treeToValue(jsonNode, clazz));
			} catch (JsonProcessingException | IllegalArgumentException e) {
				throw new IllegalStateException("Could not convert item " + jsonNode + " to class " + clazz, e);
			}
		}
		return result;
	}

	/**
	 * @return if the result has items
	 */
	public boolean hasItems() {
		return this.items != null && !((ArrayNode) this.items).isEmpty();
	}

	/**
	 * @return true if the server sent errors as part of the GraphQL response.
	 */
	public boolean hasErrors() {
		return errors != null && !errors.isEmpty();
	}

	/**
	 * @return the list of errors as received from server or {@code null} if no
	 *         {@code errors} element was present in response.
	 */
	public @Nullable List<Error> getErrors() {
		return errors;
	}

	String getErrorsString() {
		if (errors != null) {
			return errors.stream().map(Error::getMessage).collect(Collectors.joining(", "));
		} else {
			return null;
		}
	}

	/**
	 * Represents a GraphQL error as sent by server in JSON response.
	 */
	public static class Error {
		private final String message;
		private final JsonNode error;

		private Error(JsonNode errorJson) {
			JsonNode jsonNode = errorJson.get(JSON_KEY_MESSAGE);
			this.message = jsonNode.asText();
			this.error = errorJson;
		}

		/**
		 * @return the error message.
		 */
		public @NotNull String getMessage() {
			return message;
		}

		/**
		 * @return the {@link com.fasterxml.jackson.databind.JsonNode} that contains the
		 *         full error information (e.g. lines etc.)
		 */
		public @NotNull JsonNode getJson() {
			return error;
		}

	}

	static class PagingCursorImpl implements GraphQlPagingCursor {

		private final String query;
		private final String persistedQueryShortPath;
		private final int pageSize;
		private final GraphQlQueryVars variables;

		private final AEMHeadlessClient client;

		private Boolean hasMore = null;
		private GraphQlResponse firstGraphQlResponse = null;
		private String endCursor;

		PagingCursorImpl(@NotNull GraphQlQuery query, int pageSize, GraphQlQueryVars variables, AEMHeadlessClient client) {
			this.query = query.generateQuery();
			this.persistedQueryShortPath = null;
			this.pageSize = pageSize;
			this.variables = variables;
			this.client = client;
		}

		PagingCursorImpl(@NotNull PersistedQuery query, int pageSize, GraphQlQueryVars variables, AEMHeadlessClient client) {
			this.query = null;
			this.persistedQueryShortPath = query.getShortPath();
			this.pageSize = pageSize;
			this.variables = variables;
			this.client = client;
		}

		@Override
		public int getPageSize() {
			return pageSize;
		}

		@Override
		public boolean hasNext() {
			if (hasMore == null) {
				firstGraphQlResponse = nextInternal();
			}
			return hasMore;
		}

		@Override
		public GraphQlResponse next() {

			if (firstGraphQlResponse != null) {
				try {
					return firstGraphQlResponse;
				} finally {
					firstGraphQlResponse = null;
				}
			}

			if (!hasMore) {
				throw new IllegalStateException("There are no more results availalbe");
			}

			return nextInternal();
		}

		private GraphQlResponse nextInternal() {
			GraphQlQueryVars effectiveVars = GraphQlQueryVars.create(variables).first(pageSize).after(endCursor);

			GraphQlResponse response;
			if (query != null) {
				response = client.runQuery(query, effectiveVars);
			} else {
				response = client.runPersistedQuery(persistedQueryShortPath, effectiveVars);
			}

			if (response.pageInfo == null) {
				throw new IllegalStateException("Query does not support paging with a cursor, could not find 'pageInfo' in response data:\n" + response.data);
			}

			hasMore = response.pageInfo.get(AEMHeadlessClient.JSON_KEY_HAS_NEXT_PAGE).asBoolean();
			endCursor = response.pageInfo.get(AEMHeadlessClient.JSON_KEY_END_CURSOR).asText();
			return response;
		}
	}

}
