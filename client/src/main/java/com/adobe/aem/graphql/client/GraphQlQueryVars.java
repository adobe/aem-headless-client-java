/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2023 Adobe
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

/**
 * Represents GraphQl query vars that is a map of String keys to object values.
 * Adds convenience methods.
 */
public class GraphQlQueryVars extends HashMap<String, Object> implements Map<String, Object> {

	static final String QUERY_VAR_AFTER = "after";
	static final String QUERY_VAR_FIRST = "first";
	static final String QUERY_VAR_OFFSET = "offset";
	static final String QUERY_VAR_LIMIT = "limit";

	/**
	 * Create a query variables object that can be used to set default query
	 * variables like {@link #after(String)} or {@link #first(int)}.
	 * 
	 * @return
	 */
	public static GraphQlQueryVars create() {
		return new GraphQlQueryVars();
	}

	static GraphQlQueryVars create(@Nullable Map<String, Object> initialQueryVars) {
		GraphQlQueryVars graphQlQueryVars = new GraphQlQueryVars();
		if (initialQueryVars != null) {
			graphQlQueryVars.putAll(initialQueryVars);
		}
		return graphQlQueryVars;
	}

	public GraphQlQueryVars after(String after) {
		put(QUERY_VAR_AFTER, after);
		return this;
	}

	public GraphQlQueryVars first(int first) {
		put(QUERY_VAR_FIRST, first);
		return this;
	}

	public GraphQlQueryVars offset(int offset) {
		put(QUERY_VAR_OFFSET, offset);
		return this;
	}

	public GraphQlQueryVars limit(int limit) {
		put(QUERY_VAR_LIMIT, limit);
		return this;
	}

	// very simple sanity check validation (no proper query parsing)
	static void checkQueryForVars(String query, Set<String> requiredVarNames) {
		for (String requiredVarName : requiredVarNames) {
			String queryVarName = "$" + requiredVarName;
			if (!query.contains(queryVarName)) {
				throw new IllegalStateException("Required query variable " + queryVarName + " is not contained in query:\n" + query);
			}
		}
	}

}
