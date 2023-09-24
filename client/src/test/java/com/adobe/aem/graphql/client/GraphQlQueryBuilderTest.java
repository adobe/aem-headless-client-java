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

import static com.adobe.aem.graphql.client.GraphQlQueryBuilder.filter;
import static com.adobe.aem.graphql.client.GraphQlQueryBuilder.sensitiveness;
import static com.adobe.aem.graphql.client.GraphQlQueryBuilder.ignoreCase;
import static com.adobe.aem.graphql.client.GraphQlQueryBuilder.subSelection;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.adobe.aem.graphql.client.GraphQlQuery.Operator;
import com.adobe.aem.graphql.client.GraphQlQuery.PaginationType;
import com.adobe.aem.graphql.client.GraphQlQuery.Type;

class GraphQlQueryBuilderTest {

	@Test
	void testSimpleQueryNoPaging() {

		assertEquals("query  { \n"
				+ "  articleList(sort: \"title ASC, _path DESC\") {\n"
				+ "    items {\n"
				+ "      _path\n"
				+ "      title\n"
				+ "      authorFragment{firstName lastName}\n"
				+ "    }\n"
				+ "  }\n"
				+ "}\n", GraphQlQuery.builder()
				.contentFragmentModelName("article")
				.field("_path")
				.field("title")
				.field(subSelection("authorFragment").field("firstName").field("lastName"))
				.sortBy("title ASC", "_path DESC")
				.build().generateQuery());
	}

	@Test
	void testSimpleQueryFiltering() {

		String expectedQuery = "query  { \n"
				+ "  adventureList(sort: \"title ASC\", filter: {\n"
				+ "price: { _expressions: [ { _operator: LOWER, value: 154}]}\n"
				+ "}) {\n"
				+ "    items {\n"
				+ "      _path\n"
				+ "      title\n"
				+ "      price\n"
				+ "    }\n"
				+ "  }\n"
				+ "}\n";

		// using explicit filter on top level
		assertEquals(expectedQuery, GraphQlQuery.builder()
				.contentFragmentModelName("adventure")
				.field("_path")
				.field("title") 
				.field("price")
				.filter("price", Operator.LOWER, 154)
				.sortBy("title ASC")
				.build().generateQuery());
		
		// using second parameter as shortcut (with exact same result
		assertEquals(expectedQuery, GraphQlQuery.builder()
				.contentFragmentModelName("adventure")
				.field("_path")
				.field("title") 
				.field("price", filter(Operator.LOWER, 154))
				.sortBy("title ASC")
				.build().generateQuery());
	}
	
	@Test
	void testQueryFilteringWithOptions() {

		// using second parameter as shortcut (with exact same result
		assertEquals("query  { \n"
				+ "  adventureList(sort: \"title ASC\", filter: {\n"
				+ "title: { _expressions: [ { _operator: CONTAINS, _ignoreCase: true, value: \"gastronomic\"}]},\n"
				+ "price: { _expressions: [ { _operator: LOWER, _sensitiveness: 0.1, value: 154}]}\n"
				+ "}) {\n"
				+ "    items {\n"
				+ "      _path\n"
				+ "      title\n"
				+ "      price\n"
				+ "    }\n"
				+ "  }\n"
				+ "}\n", GraphQlQuery.builder()
				.contentFragmentModelName("adventure")
				.field("_path")
				.field("title", filter(Operator.CONTAINS, "gastronomic", ignoreCase())) 
				.field("price", filter(Operator.LOWER, 154, sensitiveness(0.1)))
				.sortBy("title ASC")
				.build().generateQuery());

	}
	
	@Test
	void testSimpleQueryFilteringWithVariable() {

		String expectedQuery = "query ($priceThreshold: Float) { \n"
				+ "  adventureList(sort: \"title ASC\", filter: {\n"
				+ "price: { _expressions: [ { _operator: LOWER, value: $priceThreshold}]}\n"
				+ "}) {\n"
				+ "    items {\n"
				+ "      _path\n"
				+ "      title\n"
				+ "      price\n"
				+ "    }\n"
				+ "  }\n"
				+ "}\n";

		// using explicit filter on top level
		assertEquals(expectedQuery, GraphQlQuery.builder()
				.contentFragmentModelName("adventure")
				.field("_path")
				.field("title") 
				.field("price")
				.filter("price", Operator.LOWER, Type.Float, "priceThreshold")
				.sortBy("title ASC")
				.build().generateQuery());
		
		// using second parameter as shortcut (with exact same result
		assertEquals(expectedQuery, GraphQlQuery.builder()
				.contentFragmentModelName("adventure")
				.field("_path")
				.field("title") 
				.field("price", filter(Operator.LOWER, Type.Float, "priceThreshold"))
				.sortBy("title ASC")
				.build().generateQuery());
	}
	
	@Test
	void testQueryWithGenericFilterVar() {

		String expectedQuery = "query ($filter: AdventureModelFilter) { \n"
				+ "  adventureList(sort: \"title ASC\", filter: $filter) {\n"
				+ "    items {\n"
				+ "      _path\n"
				+ "      title\n"
				+ "      price\n"
				+ "    }\n"
				+ "  }\n"
				+ "}\n";

		// using explicit filter on top level
		assertEquals(expectedQuery, GraphQlQuery.builder()
				.contentFragmentModelName("adventure")
				.field("_path")
				.field("title") 
				.field("price")
				.useFilter()
				.sortBy("title ASC")
				.build().generateQuery());

	}
	
	@Test
	void testSimpleQueryCursorPaging() {

		assertEquals("query ($after: String, $first: Int) { \n"
				+ "  articlePaginated(after: $after, first: $first, sort: \"title ASC, _path DESC\") {\n"
				+ "    edges { node {\n"
				+ "      _path\n"
				+ "      title\n"
				+ "      authorFragment{firstName lastName}\n"
				+ "    }}\n"
				+ "    pageInfo { hasNextPage endCursor }\n"
				+ "  }\n"
				+ "}\n", GraphQlQuery.builder()
				.contentFragmentModelName("article")
				.field("_path")
				.field("title")
				.field(subSelection("authorFragment").field("firstName").field("lastName"))
				.paginated()
				.sortBy("title ASC", "_path DESC")
				.build().generateQuery());
	}
	
	@Test
	void testSimpleQueryOffsetPaging() {

		assertEquals("query ($offset: Int, $limit: Int) { \n"
				+ "  articleList(offset: $offset, limit: $limit, sort: \"title ASC, _path DESC\") {\n"
				+ "    items {\n"
				+ "      _path\n"
				+ "      title\n"
				+ "      authorFragment{firstName lastName}\n"
				+ "    }\n"
				+ "  }\n"
				+ "}\n", GraphQlQuery.builder()
				.contentFragmentModelName("article")
				.field("_path")
				.field("title")
				.field(subSelection("authorFragment").field("firstName").field("lastName"))
				.paginated(PaginationType.OFFSET_LIMIT)
				.sortBy("title ASC", "_path DESC")
				.build().generateQuery());
	}

}
