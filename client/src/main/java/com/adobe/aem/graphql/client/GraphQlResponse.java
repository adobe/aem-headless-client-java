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

import com.fasterxml.jackson.databind.JsonNode;

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

	GraphQlResponse(JsonNode response) {
		super();
		this.data = response.get(JSON_KEY_DATA);
		this.errors = readErrors(response);
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

}
