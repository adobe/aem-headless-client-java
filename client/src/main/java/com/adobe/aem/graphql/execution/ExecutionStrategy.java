package com.adobe.aem.graphql.execution;

import com.adobe.aem.graphql.client.GraphQlResponse;

import java.util.concurrent.CompletableFuture;

public interface ExecutionStrategy {
    GraphQlResponse execute();
}
