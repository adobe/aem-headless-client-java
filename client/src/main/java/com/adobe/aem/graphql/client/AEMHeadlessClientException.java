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

/**
 * Thrown for any errors during interaction with {@link AEMHeadlessClient}. If a
 * GraphQL error response was received from server,
 * {@link #getGraphQlResponse()} can be used to retrieve the response.
 *
 */
public class AEMHeadlessClientException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final GraphQlResponse graphQlResponse;

	AEMHeadlessClientException(String message) {
		super(message);
		this.graphQlResponse = null;
	}

	AEMHeadlessClientException(String message, Throwable cause) {
		super(message, cause);
		this.graphQlResponse = null;
	}

	AEMHeadlessClientException(GraphQlResponse graphQlResponse) {
		super("GraphQL Response has error(s): " + graphQlResponse.getErrorsString());
		this.graphQlResponse = graphQlResponse;
	}

	/**
	 * Returns the {@link GraphQlResponse} containing the list of errors or
	 * {@code null} if the error happened before a response could be received from
	 * server.
	 * 
	 * @return the {@link GraphQlResponse} or {@code null}
	 */
	public GraphQlResponse getGraphQlResponse() {
		return graphQlResponse;
	}

}
