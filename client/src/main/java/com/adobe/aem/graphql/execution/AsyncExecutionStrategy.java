package com.adobe.aem.graphql.execution;

import com.adobe.aem.graphql.client.GraphQlResponse;

import java.util.concurrent.CompletableFuture;

public class AsyncExecutionStrategy implements ExecutionStrategy{

    @Override
    public GraphQlResponse execute() {
        System.out.println("---------AsyncExecutionStrategy----------");
        return null;
    }
}
