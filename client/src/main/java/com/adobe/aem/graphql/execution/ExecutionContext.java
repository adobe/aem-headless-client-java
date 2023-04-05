package com.adobe.aem.graphql.execution;

import com.adobe.aem.graphql.client.AEMHeadlessClient;
import com.adobe.aem.graphql.client.GraphQlResponse;

import java.net.URI;

public class ExecutionContext {

    ExecutionStrategy executionStrategy;

    public ExecutionContext(ExecutionStrategy executionStrategy) {
        this.executionStrategy = executionStrategy;
    }

    public GraphQlResponse execute(URI endPoint, String query,int expectedCode, AEMHeadlessClient aemHeadlessClient) {
        return executionStrategy.execute(endPoint, query, expectedCode, aemHeadlessClient);
    }
}
