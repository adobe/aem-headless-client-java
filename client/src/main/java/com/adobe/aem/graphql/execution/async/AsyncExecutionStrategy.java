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
package com.adobe.aem.graphql.execution.async;

import com.adobe.aem.graphql.client.AEMHeadlessClient;
import com.adobe.aem.graphql.client.AEMHeadlessClientException;
import com.adobe.aem.graphql.client.GraphQlResponse;
import com.adobe.aem.graphql.execution.AbstractExecutionStrategy;
import com.adobe.aem.graphql.execution.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.function.Function;

public class AsyncExecutionStrategy extends AbstractExecutionStrategy {

    ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public GraphQlResponse execute(@NotNull URI endPoint, @NotNull String query, int expectedCode, AEMHeadlessClient aemHeadlessClient) throws InterruptedException, ExecutionException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(aemHeadlessClient.getEndpoint())
                .header(Constants.CONTENT_TYPE.asString(), MediaType.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofByteArray(query.getBytes(StandardCharsets.UTF_8)));
        if (!AEMHeadlessClient.isBlank(aemHeadlessClient.getAuthorizationHeader())) {
            requestBuilder.header(Constants.AUTHORIZATION.asString(), aemHeadlessClient.getAuthorizationHeader());
        }
        HttpRequest request = requestBuilder.build();

        CompletableFuture<GraphQlResponse> graphQlResponseCompletableFuture = HttpClient.newBuilder()
                .executor(executorService)
                .build()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(new Function<HttpResponse<String>, GraphQlResponse>() {
                    public GraphQlResponse apply(HttpResponse<String> response) {
                        if (response.statusCode() != expectedCode) {
                            throw new AEMHeadlessClientException(
                                    "Unexpected http response code " + response.statusCode() + ": " + response.body());
                        }
                        JsonNode responseJson = aemHeadlessClient.stringToJson(response.body());
                        GraphQlResponse graphQlResponse = new GraphQlResponse(responseJson);
                        return graphQlResponse;
                    }
                }, executorService);
        GraphQlResponse graphQlResponse = graphQlResponseCompletableFuture.join();
        return graphQlResponse;
    }
}
