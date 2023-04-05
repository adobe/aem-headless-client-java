package com.adobe.aem.graphql.execution;

import com.adobe.aem.graphql.client.AEMHeadlessClient;
import com.adobe.aem.graphql.client.GraphQlResponse;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLOutput;

public class Test {
    public static void main(String[] ar) throws URISyntaxException {
        AEMHeadlessClient aemHeadlessClient = AEMHeadlessClient.builder()
                .endpoint(new URI("http://localhost:4502"))
                .basicAuth("admin", "admin")
                .executionStrategy(new AsyncExecutionStrategy())
                .build();

        String query = "{adventureList{items{title}}}";
        GraphQlResponse res = aemHeadlessClient.runQuery(query);
        System.out.println(res.getData());
    }
}
