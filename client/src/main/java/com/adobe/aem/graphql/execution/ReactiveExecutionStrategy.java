package com.adobe.aem.graphql.execution;

import com.adobe.aem.graphql.client.GraphQlResponse;

import java.util.concurrent.CompletableFuture;

public class ReactiveExecutionStrategy implements ExecutionStrategy{


    @Override
    public GraphQlResponse execute() {
        System.out.println("--------ReactiveExecutionStrategy--------");
        return null;
    }
}
