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
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.adobe.aem.graphql.client.GraphQlQuery.PaginationType;
import com.adobe.aem.graphql.client.GraphQlQuery.SortBy;
import com.adobe.aem.graphql.client.GraphQlQuery.SortingOrder;

/**
 * Builds a GraphQL query.
 */
public class GraphQlQueryBuilder {

	/**
	 * Create sub-selection (allowing for deep structures). Use
	 * {@link SubSelection#field(String)} and
	 * {@link SubSelection#subSelection(String)} to create deep structures.
	 * 
	 * @param fieldName the field name for the sub selection
	 * @return the sub selection
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

	/**
	 * @param name the content fragment name to to use for this query
	 * @return the GraphQlQueryBuilder
	 */
	public GraphQlQueryBuilder contentFragmentModelName(@NotNull String name) {
		headlessQuery.setContentFragmentModelName(name);
		return this;
	}
	
	/**
	 * @param field field name to add to the query
	 * @return the GraphQlQueryBuilder
	 */
	public GraphQlQueryBuilder field(@NotNull String field) {
		headlessQuery.addField(new SimpleField(field));
		return this;
	}

	/**
	 * @param field field class that can be either a {@link #subSelection(String)}
	 * @return the GraphQlQueryBuilder
	 */
	public GraphQlQueryBuilder field(@NotNull Field field) {
		headlessQuery.addField(field);
		return this;
	}

	public GraphQlQueryBuilder useFilter() {
		headlessQuery.setDeclareContentFragmentModelFilter(true);
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
		Arrays.asList(sortByFieldWithOrderClauses).stream().forEach(sortByFieldWithOrder -> headlessQuery.addSorting(new SortBy(sortByFieldWithOrder)));
		return this;
	}

	public @NotNull GraphQlQuery build() {
		assertNotSealed();
		sealed = true;
		return headlessQuery;
	}

	private void assertNotSealed() {
		if (sealed) {
			throw new IllegalStateException("Builder can only be used to create one instance of AEMHeadlessClient");
		}
	}

	/**
	 * Represents a generic field.
	 */
	public static interface Field {

		/**
		 * Creates the query fragment for the field.
		 * 
		 * @return the query fragment as string
		 */
		String toQueryFragment();
	}

	/**
	 * Represents a simple field.
	 */
	public static class SimpleField implements Field {

		private final String fieldName;

		SimpleField(String fieldName) {
			super();
			this.fieldName = fieldName;
		}

		@Override
		public String toQueryFragment() {
			return fieldName;
		}
	}

	/**
	 * Represents a sub selection that may contain other fields and sub selections.
	 */
	public static class SubSelection implements Field {

		private final String fieldName;
		private final List<Field> fieldsInSubSelection;

		SubSelection(String fieldName) {
			this.fieldName = fieldName;
			this.fieldsInSubSelection = new ArrayList<>();
		}

		/**
		 * Allows to add a simple field or a sub selection
		 * 
		 * @param field the field 
		 * @return the current SubSelection
		 */
		public @NotNull SubSelection field(Field field) {
			fieldsInSubSelection.add(field);
			return this;
		}

		/**
		 * Adds a simple field to the sub selection
		 * 
		 * @param fieldName the field name
		 * @return the current SubSelection
		 */
		public @NotNull SubSelection field(String fieldName) {
			fieldsInSubSelection.add(new SimpleField(fieldName));
			return this;
		}

		@Override
		public String toQueryFragment() {
			return fieldName + "{" + fieldsInSubSelection.stream().map(Field::toQueryFragment).collect(Collectors.joining(" ")) + "}";
		}
	}

}
