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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

class AEMHeadlessClientBuilderTest {

	@Test
	void testBuilderBasicAuth() throws URISyntaxException {
		AEMHeadlessClient client = new AEMHeadlessClientBuilder().basicAuth("user", "password")
				.endpoint("http://host:1234").build();
		assertEquals("http://host:1234/content/graphql/global/endpoint.json", client.getEndpoint().toString());
		assertEquals("Basic dXNlcjpwYXNzd29yZA==", client.getAuthorizationHeader());
	}

	@Test
	void testBuilderTokenAuth() throws URISyntaxException {
		AEMHeadlessClient client = new AEMHeadlessClientBuilder().tokenAuth("token")
				.endpoint(new URI("http://host:1234")).build();
		assertEquals("http://host:1234/content/graphql/global/endpoint.json", client.getEndpoint().toString());
		assertEquals("Bearer token", client.getAuthorizationHeader());
	}

	@Test
	void testBuilderCannotBeUsedTwice() throws URISyntaxException {

		AEMHeadlessClientBuilder builder = new AEMHeadlessClientBuilder().endpoint(new URI("http://host:1234"));
		AEMHeadlessClient client = builder.build();
		assertNotNull(client);

		IllegalStateException thrownException = assertThrows(IllegalStateException.class, () -> {
			builder.build();
		});

		assertEquals("Builder can only be used to create one instance of AEMHeadlessClient",
				thrownException.getMessage());
	}

	
	@Test
	void testAuthCannotBeSetTwice() throws URISyntaxException {
		
		IllegalStateException thrownException = assertThrows(IllegalStateException.class, () -> {
			new AEMHeadlessClientBuilder()
					.endpoint("http://host:1234")
					.basicAuth("user", "password")
					.tokenAuth("token") // token and basic auth can obviously not be used in parallel
					.build();
		});

		assertEquals("Authentication is already configured",
				thrownException.getMessage());
		
		
	}

}
