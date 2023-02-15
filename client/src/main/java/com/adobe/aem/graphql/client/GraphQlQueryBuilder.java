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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

/**
 * Builds a GraphQL query.
 */
public class GraphQlQueryBuilder {

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
	 * Create sub-selection (allowing for deep structures). Use {@link SubSelection#field(String)} and {@link SubSelection#subSelection(String)} to create deep structures.
	 * 
	 * @return the sub selectioon
	 * 
	 */
	public static @NotNull SubSelection subSelection(String fieldName) {
		return new SubSelection(fieldName);
	}

	private final GraphQlQuery headlessQuery;
	private boolean sealed = false;

	/**
	 * Use {@link GraphQlQuery#builder()} to create a builder.
	 */
	GraphQlQueryBuilder() {
		headlessQuery = new GraphQlQuery();
	}

	public GraphQlQueryBuilder contentFragmentModelName(@NotNull String name) {
		headlessQuery.setContentFragementModelName(name);
		return this;
	}

	public GraphQlQueryBuilder field(@NotNull String field) {
		headlessQuery.addField(new SimpleField(field));
		return this;
	}

	public GraphQlQueryBuilder field(@NotNull Field field) {
		headlessQuery.addField(field);
		return this;
	}

	public GraphQlQueryBuilder paginated() {
		headlessQuery.setPaginationType(PaginationType.CURSOR);
		return this;
	}

	public GraphQlQueryBuilder paginated(PaginationType type) {
		headlessQuery.setPaginationType(type);
		return this;
	}

	public GraphQlQueryBuilder sortBy(@NotNull String sortByField, @NotNull SortingOrder order) {
		headlessQuery.addSorting(new SortBy(sortByField, order));
		return this;
	}

	public GraphQlQueryBuilder sortBy(@NotNull String... sortByFieldWithOrderClauses) {
		Arrays.asList(sortByFieldWithOrderClauses).stream()
				.forEach(sortByFieldWithOrder -> headlessQuery.addSorting(new SortBy(sortByFieldWithOrder)));
		return this;
	}

	public @NotNull GraphQlQuery build() {
		assertNotSealed();
		sealed = true;
		return headlessQuery;
	}

	public @NotNull String generate() {
		return build().generateQuery();
	}

	private void assertNotSealed() {
		if (sealed) {
			throw new IllegalStateException("Builder can only be used to create one instance of AEMHeadlessClient");
		}
	}

	public static interface Field {
		public String toQueryFragment();
	}

	public static class SimpleField implements Field {

		private final String fieldName;

		public SimpleField(String fieldName) {
			super();
			this.fieldName = fieldName;
		}

		@Override
		public String toQueryFragment() {
			return fieldName;
		}
	}

	public static class SubSelection implements Field {

		private final String fieldName;
		private final List<Field> subSelectionFields;

		public SubSelection(String fieldName) {
			this.fieldName = fieldName;
			this.subSelectionFields = new ArrayList<>();
		}

		public @NotNull SubSelection subSelection(String fieldName) {
			SubSelection newSubSelection = new SubSelection(fieldName);
			subSelectionFields.add(newSubSelection);
			return newSubSelection;
		}

		public @NotNull SubSelection field(Field field) {
			subSelectionFields.add(field);
			return this;
		}

		public @NotNull SubSelection field(String fieldName) {
			subSelectionFields.add(new SimpleField(fieldName));
			return this;
		}

		@Override
		public String toQueryFragment() {
			return fieldName + "{"
					+ subSelectionFields.stream().map(Field::toQueryFragment).collect(Collectors.joining(" ")) + "}";
		}
	}

	public static class SortBy {
		private final String field;
		private final SortingOrder sortByOrder;

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

	static class GraphQlQuery {

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

		String generateQuery() {
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
}
