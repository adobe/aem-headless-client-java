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

import com.adobe.aem.graphql.client.GraphQlQuery.Operator;
import com.adobe.aem.graphql.client.GraphQlQuery.PaginationType;
import com.adobe.aem.graphql.client.GraphQlQuery.SortBy;
import com.adobe.aem.graphql.client.GraphQlQuery.SortingOrder;
import com.adobe.aem.graphql.client.GraphQlQuery.Type;

/** Builds a GraphQL query. */
public class GraphQlQueryBuilder {

	/** Create sub-selection (allowing for deep structures). Use {@link SubSelection#field(String)} and
	 * {@link SubSelection#subSelection(String)} to create deep structures.
	 * 
	 * @param fieldName the field name for the sub selection
	 * @return the sub selection */
	public static @NotNull SubSelection subSelection(String fieldName) {
		return new SubSelection(fieldName);
	}

	/** Create filter for given field with a static value.
	 * 
	 * @param op the operator to use
	 * @param staticValue the static value to filter for
	 * @param options optional options, e.g. {@link #ignoreCase()} or {@link #sensitiveness(double)}
	 * @return the filter to be used in a field() method */
	public static @NotNull Filter filter(Operator op, Object staticValue, FilterOption... options) {
		return new Filter(op, staticValue, null, null, options);
	}

	/** Create filter for given field creating a variable.
	 * 
	 * @param op the operator to use
	 * @param type the type of the variable
	 * @param variable the variable name to be used with {@link AEMHeadlessClient#runQuery(GraphQlQuery, java.util.Map)}
	 * @param options optional options, e.g. {@link #ignoreCase()} or {@link #sensitiveness(double)}
	 * @return the filter to be used in a field() method */
	public static @NotNull Filter filter(Operator op, Type type, String variable, FilterOption... options) {
		return new Filter(op, null, type, variable, options);
	}

	/**
	 * @return the ignore case filter option
	 */
	public static @NotNull FilterOption ignoreCase() {
		return FilterOption.IGNORE_CASE;
	}
	
	/**
	 * @param sensitiveness the sensitiveness
	 * @return the sensitiveness filter option with the given sensitiveness
	 */
	public static @NotNull FilterOption sensitiveness(double sensitiveness) {
		return new FilterOption("_sensitiveness: " + sensitiveness);
	}

	private final GraphQlQuery headlessQuery;
	private boolean sealed = false;

	/** Use {@link GraphQlQuery#builder()} to create a builder. */
	GraphQlQueryBuilder() {
		headlessQuery = new GraphQlQuery();
	}

	/** @param name the content fragment name to to use for this query
	 * @return the GraphQlQueryBuilder */
	public GraphQlQueryBuilder contentFragmentModelName(@NotNull String name) {
		headlessQuery.setContentFragmentModelName(name);
		return this;
	}

	/** @param field field name to add to the query
	 * @return the GraphQlQueryBuilder */
	public GraphQlQueryBuilder field(@NotNull String field) {
		headlessQuery.addField(new SimpleField(field));
		return this;
	}

	/** @param field field class that can be either a {@link #subSelection(String)}
	 * @return the GraphQlQueryBuilder */
	public GraphQlQueryBuilder field(@NotNull Field field) {
		headlessQuery.addField(field);
		return this;
	}

	/** @param field field name to add to the query
	 * @param filter filter class as created by either {@link #filter(Operator, Object, FilterOption...)} or
	 *            {@link #filter(Operator, Type, String, FilterOption...)}
	 * @return the GraphQlQueryBuilder */
	public GraphQlQueryBuilder field(@NotNull String field, @NotNull Filter filter) {
		headlessQuery.addField(new SimpleField(field));
		useFilter();
		filter.setFieldName(field);
		headlessQuery.addFilter(filter);
		return this;
	}
	
	/** @param field the field to filter for
	 * @param op the operator for the expression
	 * @param staticValue the value to filter for
	 * @param options options, e.g. {@link #ignoreCase()} or {@link #sensitiveness(double)}
	 * @return the GraphQlQueryBuilder */
	public GraphQlQueryBuilder filter(@NotNull String field, Operator op, String staticValue, FilterOption... options) {
		useFilter();
		Filter filter = new Filter(op, staticValue, null, null, options);
		filter.setFieldName(field);
		headlessQuery.addFilter(filter);
		return this;
	}

	/** @param field the field to filter for
	 * @param op the operator for the expression
	 * @param type the type of the variable
	 * @param variable the variable name to be used with {@link AEMHeadlessClient#runQuery(GraphQlQuery, java.util.Map)}
	 * @param options options, e.g. {@link #ignoreCase()} or {@link #sensitiveness(double)}
	 * @return the GraphQlQueryBuilder */
	public GraphQlQueryBuilder filter(@NotNull String field, Operator op, Type type, String variable, FilterOption... options) {
		useFilter();
		Filter filter = new Filter(op, null, type, variable, options);
		filter.setFieldName(field);
		headlessQuery.addFilter(filter);
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
		Arrays.asList(sortByFieldWithOrderClauses).stream()
				.forEach(sortByFieldWithOrder -> headlessQuery.addSorting(new SortBy(sortByFieldWithOrder)));
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

	/** Represents a generic field. */
	public static interface Field {

		/** Creates the query fragment for the field.
		 * 
		 * @return the query fragment as string */
		String toQueryFragment();
	}

	/** Represents a simple field. */
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

	/** Represents a sub selection that may contain other fields and sub selections. */
	public static class SubSelection implements Field {

		private final String fieldName;
		private final List<Field> fieldsInSubSelection;

		SubSelection(String fieldName) {
			this.fieldName = fieldName;
			this.fieldsInSubSelection = new ArrayList<>();
		}

		/** Allows to add a simple field or a sub selection
		 * 
		 * @param field the field
		 * @return the current SubSelection */
		public @NotNull SubSelection field(Field field) {
			fieldsInSubSelection.add(field);
			return this;
		}

		/** Adds a simple field to the sub selection
		 * 
		 * @param fieldName the field name
		 * @return the current SubSelection */
		public @NotNull SubSelection field(String fieldName) {
			fieldsInSubSelection.add(new SimpleField(fieldName));
			return this;
		}

		@Override
		public String toQueryFragment() {
			return fieldName + "{" + fieldsInSubSelection.stream().map(Field::toQueryFragment).collect(Collectors.joining(" ")) + "}";
		}
	}

	/** Represents a simple filter item that can be added per field. */
	static class Filter {

		private String fieldName;
		private final Operator operator;
		private final Object value;
		private final String varName;
		private final Type varType;
		private final FilterOption[] options;

		Filter(Operator operator, Object value, Type varType, String varName, FilterOption... options) {
			this.operator = operator;
			this.value = value;
			this.varName = varName;
			this.varType = varType;
			this.options = options;
		}

		void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}

		String getFieldName() {
			return fieldName;
		}

		Operator getOperator() {
			return operator;
		}

		Object getValue() {
			return value;
		}

		String getVarName() {
			return varName;
		}

		Type getVarType() {
			return varType;
		}

		FilterOption[] getOptions() {
			return options;
		}

	}

	public static class FilterOption {
		
		@NotNull 
		private static final FilterOption IGNORE_CASE = new FilterOption("_ignoreCase: true");
		
		private final String optionString;

		private FilterOption(String optionString) {
			super();
			this.optionString = optionString;
		}
		
		public String toString() { 
			return optionString;
		}
	}
}
