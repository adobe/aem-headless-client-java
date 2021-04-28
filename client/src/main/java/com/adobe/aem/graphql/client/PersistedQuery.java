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

import org.jetbrains.annotations.NotNull;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a GraphQL persisted query. Used as value object by
 * {@link AEMHeadlessClient}.
 **/
public class PersistedQuery {

	private final String shortPath;
	private final String longPath;
	private final String query;

	PersistedQuery(JsonNode persistedQueryNode) {
		this(persistedQueryNode.get(JSON_KEY_PATH).get(JSON_KEY_SHORT_FORM).asText(), //
				persistedQueryNode.get(JSON_KEY_PATH).get(JSON_KEY_LONG_FORM).asText(), //
				persistedQueryNode.get(JSON_KEY_DATA).get(JSON_KEY_QUERY).asText());
	}

	PersistedQuery(String shortPath, String longPath, String query) {
		this.shortPath = shortPath;
		this.longPath = longPath;
		this.query = query;
	}

	/**
	 * @return the short path of a persisted query, e.g. {@code /myproj/myquery}.
	 */
	public @NotNull String getShortPath() {
		return shortPath;
	}

	/**
	 * @return the long path of a persisted query, e.g.
	 *         {@code /myproj/settings/graphql/persistentQueries/myquery}.
	 */
	public @NotNull String getLongPath() {
		return longPath;
	}

	/**
	 * @return the associated query
	 */
	public @NotNull String getQuery() {
		return query;
	}

	/**
	 * @return the String representation of the persisted query.
	 */
	@Override
	public String toString() {
		return "[PersistedQuery shortPath=" + shortPath + ", longPath=" + longPath + ", query=" + query + "]";
	}
}
