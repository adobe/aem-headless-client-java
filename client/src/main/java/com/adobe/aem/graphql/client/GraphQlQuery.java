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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.adobe.aem.graphql.client.GraphQlQueryBuilder.Field;

public class GraphQlQuery {

	public static class SortBy {
		final String field;
		final SortingOrder sortByOrder;

		public SortBy(String field, SortingOrder sortByOrder) {
			this.field = field;
			this.sortByOrder = sortByOrder;
		}

		public SortBy(String fieldWithSortingOrder) {
			String[] fieldAndSortingOrder = fieldWithSortingOrder.split(" ", 2);
			this.field = fieldAndSortingOrder[0];
			this.sortByOrder = fieldAndSortingOrder.length == 2
					? SortingOrder.valueOf(fieldAndSortingOrder[1].toUpperCase())
					: SortingOrder.ASC;
		}
	}

	public enum SortingOrder {
		ASC, DESC
	}

	public enum PaginationType {
		NONE, CURSOR, OFFSET_LIMIT;

		boolean isCursor() {
			return this == CURSOR;
		}
	}
	
	private String contentFragementModelName;
	private PaginationType paginationType = PaginationType.NONE;
	private List<Field> fields = new ArrayList<>();
	private List<SortBy> sortByList = new ArrayList<>();

	GraphQlQuery() {
		// used by builder only
	}

	void setContentFragementModelName(String contentFragementModelName) {
		this.contentFragementModelName = contentFragementModelName;
	}

	void addField(Field field) {
		this.fields.add(field);
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

		Map<String, Object> effectiveTopLevelQueryArguments = new HashMap<>();

		if (!sortByList.isEmpty()) {
			effectiveTopLevelQueryArguments.put("sort", getSortParamValue());
		}

		buf.append("query ");
		switch (paginationType) {
		case CURSOR:
			String varAfter = "$" + GraphQlQueryVars.QUERY_VAR_AFTER;
			String varFirst = "$" + GraphQlQueryVars.QUERY_VAR_FIRST;
			buf.append("(" + varAfter + ": String, " + varFirst + ": Int) ");
			effectiveTopLevelQueryArguments.put(GraphQlQueryVars.QUERY_VAR_AFTER, varAfter);
			effectiveTopLevelQueryArguments.put(GraphQlQueryVars.QUERY_VAR_FIRST, varFirst);
			break;
		case OFFSET_LIMIT:
			String varOffset = "$" + GraphQlQueryVars.QUERY_VAR_OFFSET;
			String varLimit = "$" + GraphQlQueryVars.QUERY_VAR_LIMIT;
			buf.append("(" + varOffset + ": Int, " + varLimit + ": Int) ");
			effectiveTopLevelQueryArguments.put(GraphQlQueryVars.QUERY_VAR_OFFSET, varOffset);
			effectiveTopLevelQueryArguments.put(GraphQlQueryVars.QUERY_VAR_LIMIT, varLimit);
			break;
		default: // do nothing
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

		String fieldsStr = "      "
				+ fields.stream().map(Field::toQueryFragment).collect(Collectors.joining("\n      ")) + "\n";
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