package com.adobe.aem.graphql.execution;

import com.adobe.aem.graphql.client.AEMHeadlessClient;
import com.adobe.aem.graphql.client.AEMHeadlessClientException;
import com.adobe.aem.graphql.client.GraphQlResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.jetty.http.HttpHeader;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class AsyncExecutionStrategy implements ExecutionStrategy {

    ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Override
    public GraphQlResponse execute(@NotNull URI endPoint, @NotNull String query, int expectedCode, AEMHeadlessClient aemHeadlessClient) {

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(aemHeadlessClient.getEndpoint())
                .header(HttpHeader.CONTENT_TYPE.asString(), AEMHeadlessClient.CONTENT_TYPE_JSON)
                .POST(HttpRequest.BodyPublishers.ofByteArray(query.getBytes()));
        if (!AEMHeadlessClient.isBlank(aemHeadlessClient.getAuthorizationHeader())) {
            requestBuilder.header(HttpHeader.AUTHORIZATION.asString(), aemHeadlessClient.getAuthorizationHeader());
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
        executorService.shutdown();
        return graphQlResponse;
    }
}
