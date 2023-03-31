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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

class GraphQlQueryVarsTest {

	@Test
	void testGraphQlQueryVars() {
		GraphQlQueryVars vars1 = GraphQlQueryVars.create().after("x").first(1);
		assertEquals(2, vars1.size());
		assertEquals("x", vars1.get("after"));
		assertEquals(1, vars1.get("first"));

		
		GraphQlQueryVars vars2 = GraphQlQueryVars.create().limit(10).offset(5);
		assertEquals(2, vars2.size());
		assertEquals(10, vars2.get("limit"));
		assertEquals(5, vars2.get("offset"));
		
		
		GraphQlQueryVars vars3 = GraphQlQueryVars.create();
		vars3.addVar("var", "val");
		assertEquals(1, vars3.size());
		assertEquals("val", vars3.get("var"));
	}
	
	@Test
	void testCheckQueryForVar() {
		
		GraphQlQueryVars.checkQueryForVars("query($after: String, $first: Int) {}", new HashSet<>(Arrays.asList("after", "first")));
		GraphQlQueryVars.checkQueryForVars("query($offset: Int, $limit: Int) {}", new HashSet<>(Arrays.asList("offset", "limit")));
		// assert no exception above
		
		assertThrows(IllegalArgumentException.class, () -> {
			GraphQlQueryVars.checkQueryForVars("query {}", new HashSet<>(Arrays.asList("offset", "limit")));
		});
	}

}
