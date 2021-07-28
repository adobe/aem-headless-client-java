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

import java.net.URI;
import java.net.URISyntaxException;

import org.jetbrains.annotations.NotNull;

/**
 * Builder to configure and create an instance of {@code AEMHeadlessClient}.
 */
public class AEMHeadlessClientBuilder {

	private final AEMHeadlessClient headlessClient;
	private boolean sealed = false;

	/**
	 * Use {@link AEMHeadlessClient#builder()} to create a builder.
	 */
	AEMHeadlessClientBuilder() {
		headlessClient = new AEMHeadlessClient();
	}

	/**
	 * Sets the endpoint for the GraphQL client.
	 *
	 * If the endpoint points to a server only without path (e.g.
	 * {@code http:/myserver:8080}), then the default endpoint for GraphQL queries
	 * {@code /content/graphql/global/endpoint.json} is taken.
	 * 
	 * @param uri the endpoint URI
	 * @return the builder
	 */
	public @NotNull AEMHeadlessClientBuilder endpoint(@NotNull URI uri) {
		assertNotSealed();
		headlessClient.setEndpoint(uri);
		return this;
	}

	/**
	 * Convenience method to allow to set the URI as string. See
	 * {@link #endpoint(URI)} on how URIs are interpreted.
	 * 
	 * @param uri the uri in string format
	 * @return the builder
	 * @throws URISyntaxException if the uri is invalid
	 */
	public @NotNull AEMHeadlessClientBuilder endpoint(@NotNull String uri) throws URISyntaxException {
		assertNotSealed();
		headlessClient.setEndpoint(new URI(uri));
		return this;
	}

	/**
	 * Configures the client for basic authentication.
	 * 
	 * @param user     the user
	 * @param password the password
	 * @return the builder
	 */
	public @NotNull AEMHeadlessClientBuilder basicAuth(@NotNull String user, @NotNull String password) {
		assertNotSealed();
		if (headlessClient.getAuthorizationHeader() != null) {
			throw new IllegalStateException("Authentication is already configured");
		}
		headlessClient.setAuthorizationHeader(AEMHeadlessClient.basicAuthHeaderVal(user, password));
		return this;
	}

	/**
	 * Configures the client for token authentication.
	 * 
	 * @param token the bearer token
	 * @return the builder
	 */
	public @NotNull AEMHeadlessClientBuilder tokenAuth(@NotNull String token) {
		assertNotSealed();
		if (headlessClient.getAuthorizationHeader() != null) {
			throw new IllegalStateException("Authentication is already configured");
		}
		headlessClient.setAuthorizationHeader(AEMHeadlessClient.tokenAuthHeaderVal(token));
		return this;
	}

	public @NotNull AEMHeadlessClient build() {
		assertNotSealed();
		sealed = true;
		return headlessClient;
	}

	private void assertNotSealed() {
		if (sealed) {
			throw new IllegalStateException("Builder can only be used to create one instance of AEMHeadlessClient");
		}
	}

}
