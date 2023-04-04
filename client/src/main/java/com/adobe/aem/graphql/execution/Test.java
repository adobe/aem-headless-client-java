package com.adobe.aem.graphql.execution;

import com.adobe.aem.graphql.client.AEMHeadlessClient;

import java.net.URI;
import java.net.URISyntaxException;

public class Test {
    public static void main(String[] ar) throws URISyntaxException {
        AEMHeadlessClient aemHeadlessClient = AEMHeadlessClient.builder()
                .endpoint(new URI("http://localhost:4503/content/graphql-custom"))
                .basicAuth("user", "password")
                .executionStrategy(new ReactiveExecutionStrategy())
                .build();

        String query = "{\n" +
                "  articleList{\n" +
                "    items{ \n" +
                "      _path\n" +
                "    } \n" +
                "  }\n" +
                "}";

        aemHeadlessClient.runQuery(query);
    }
}
