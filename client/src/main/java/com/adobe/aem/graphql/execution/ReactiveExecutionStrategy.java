package com.adobe.aem.graphql.execution;

import com.adobe.aem.graphql.client.AEMHeadlessClient;
import com.adobe.aem.graphql.client.GraphQlResponse;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class ReactiveExecutionStrategy implements ExecutionStrategy{

    @Override
    public GraphQlResponse execute(@NotNull URI endPoint, @NotNull String query, int expectedCode, AEMHeadlessClient aemHeadlessClient) {
        return null;
    }
}
