package com.adobe.aem.graphql.execution;

import com.adobe.aem.graphql.client.GraphQlResponse;

public class ExecutionContext {

    ExecutionStrategy executionStrategy;

    public ExecutionContext(ExecutionStrategy t) {
        this.executionStrategy=t;
    }

    public GraphQlResponse execute() {
        return executionStrategy.execute();
    }
}
