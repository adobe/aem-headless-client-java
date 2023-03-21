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

/**
 * Allows to use paging for GraphQl queries, create a cursor using
 * {@link AEMHeadlessClient#createCursor(String, int)}.
 *
 */
public interface GraphQlPagingCursor {

	/**
	 * Returns the next page.
	 * 
	 * @return the next page as @{link {@link GraphQlResponse}.
	 */
	public GraphQlResponse next();

	/**
	 * Returns true if a next page exists.
	 * 
	 * @return true if a next page exists
	 */
	public boolean hasNext();

	/**
	 * 
	 * @return the page size used by this cursor
	 */
	public int getPageSize();

}