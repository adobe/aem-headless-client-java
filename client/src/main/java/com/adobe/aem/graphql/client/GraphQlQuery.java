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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.adobe.aem.graphql.client.GraphQlQueryBuilder.Field;

/** Represents a GraphQl query to be used with {@link AEMHeadlessClient}. */
public class GraphQlQuery {

	private static final String SUFFIX_CF_MODEL_FILTER = "ModelFilter";


	/**
	 * Builder that allows to configure all available options of the
	 * {@code AEMHeadlessQuery}
	 * 
	 * @return builder
	 * 
	 */
	public static @NotNull GraphQlQueryBuilder builder() {
		return new GraphQlQueryBuilder();
	}
	
	/**
	 * The sorting order for GraphQL queries. 
	 *
	 */
	public enum SortingOrder {
		ASC, DESC
	}

	/**
	 * The pagination type for GraphQL queries. 
	 */
	public enum PaginationType {
		NONE, CURSOR, OFFSET_LIMIT;

		boolean isCursor() {
			return this == CURSOR;
		}
	}

	static class SortBy {
		final String field;
		final SortingOrder sortByOrder;

		SortBy(String field, SortingOrder sortByOrder) {
			this.field = field;
			this.sortByOrder = sortByOrder;
		}

		SortBy(String fieldWithSortingOrder) {
			String[] fieldAndSortingOrder = fieldWithSortingOrder.split(" ", 2);
			this.field = fieldAndSortingOrder[0];
			this.sortByOrder = fieldAndSortingOrder.length == 2
					? SortingOrder.valueOf(fieldAndSortingOrder[1].toUpperCase())
					: SortingOrder.ASC;
		}
	}
	
	private String contentFragementModelName;
	private PaginationType paginationType = PaginationType.NONE;
	private List<Field> fields = new ArrayList<>();
	private List<SortBy> sortByList = new ArrayList<>();

	private boolean isDeclareContentFragmentModelFilter = false;

	
	GraphQlQuery() {
		// used by builder only
	}

	void setContentFragmentModelName(String contentFragementModelName) {
		this.contentFragementModelName = contentFragementModelName;
	}

	void addField(Field field) {
		this.fields.add(field);
	}

	void setDeclareContentFragmentModelFilter(boolean isDeclareContentFragmentModelFilter) {
		this.isDeclareContentFragmentModelFilter = isDeclareContentFragmentModelFilter;
	}

	void addSorting(SortBy sortBy) {
		sortByList.add(sortBy);
	}

	void setPaginationType(PaginationType paginationType) {
		this.paginationType = paginationType;
	}

	private String getSortParamValue() {
		return sortByList.stream().map(item -> item.field + " " + item.sortByOrder)
				.collect(Collectors.joining(", "));
	}

	public String generateQuery() {
		StringBuilder buf = new StringBuilder();

		Map<String, String> queryVarDeclarations = new LinkedHashMap<>();
		Map<String, Object> effectiveTopLevelQueryArguments = new LinkedHashMap<>();

		switch (paginationType) {
		case CURSOR:
			String varAfter = "$" + GraphQlQueryVars.QUERY_VAR_AFTER;
			String varFirst = "$" + GraphQlQueryVars.QUERY_VAR_FIRST;

			queryVarDeclarations.put(varAfter, "String");
			queryVarDeclarations.put(varFirst, "Int");
			
			effectiveTopLevelQueryArguments.put(GraphQlQueryVars.QUERY_VAR_AFTER, varAfter);
			effectiveTopLevelQueryArguments.put(GraphQlQueryVars.QUERY_VAR_FIRST, varFirst);
			break;
		case OFFSET_LIMIT:
			String varOffset = "$" + GraphQlQueryVars.QUERY_VAR_OFFSET;
			String varLimit = "$" + GraphQlQueryVars.QUERY_VAR_LIMIT;

			queryVarDeclarations.put(varOffset, "Int");
			queryVarDeclarations.put(varLimit, "Int");

			effectiveTopLevelQueryArguments.put(GraphQlQueryVars.QUERY_VAR_OFFSET, varOffset);
			effectiveTopLevelQueryArguments.put(GraphQlQueryVars.QUERY_VAR_LIMIT, varLimit);
			break;
		default: // do nothing
		}

		if (!sortByList.isEmpty()) {
			effectiveTopLevelQueryArguments.put("sort", getSortParamValue());
		}

		if(isDeclareContentFragmentModelFilter) {
			String varFilter = "$" + GraphQlQueryVars.QUERY_VAR_FILTER;
			String modelFilterType = contentFragementModelName.substring(0, 1).toUpperCase() + contentFragementModelName.substring(1) + SUFFIX_CF_MODEL_FILTER;
			queryVarDeclarations.put(varFilter, modelFilterType);
			effectiveTopLevelQueryArguments.put(GraphQlQueryVars.QUERY_VAR_FILTER, varFilter);
			
		}
		
		buf.append("query ");

		if(!queryVarDeclarations.isEmpty()) {
			buf.append("(" + queryVarDeclarations.entrySet().stream().map(e -> (e.getKey() + ": " + e.getValue())).collect(Collectors.joining(", ")) + ")");
		}

		
		buf.append(" { \n");
		buf.append("  " + contentFragementModelName);
		buf.append(paginationType.isCursor() ? "Paginated" : "List");

		if (!effectiveTopLevelQueryArguments.isEmpty()) {
			buf.append("(");
			boolean isFirst = true;
			for (String key : effectiveTopLevelQueryArguments.keySet()) {
				if (isFirst) {
					isFirst = false;
				} else {
					buf.append(", ");
				}
				buf.append(key + ": ");
				Object val = effectiveTopLevelQueryArguments.get(key);
				if (val instanceof Number || ((val instanceof String) && ((String) val).startsWith("$"))) {
					buf.append(val);
				} else {
					buf.append("\"" + val + "\"");
				}
			}
			buf.append(")");
		}
		buf.append(" {\n");

		String fieldsStr = "      " + fields.stream().map(Field::toQueryFragment).collect(Collectors.joining("\n      ")) + "\n";
		if (paginationType.isCursor()) {
			buf.append("    edges { node {\n" + fieldsStr + "    }}\n    pageInfo { hasNextPage endCursor }\n");
		} else {
			buf.append("    items {\n" + fieldsStr + "    }\n");
		}

		buf.append("  }\n");
		buf.append("}\n");
		return buf.toString();
	}
}