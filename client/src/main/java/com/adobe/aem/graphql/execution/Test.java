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
package com.adobe.aem.graphql.execution;

import com.adobe.aem.graphql.client.AEMHeadlessClient;
import com.adobe.aem.graphql.client.GraphQlResponse;
import com.adobe.aem.graphql.execution.async.AsyncExecutionStrategy;
import com.adobe.aem.graphql.execution.reactive.ReactiveExecutionStrategy;
import rx.Observable;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;

public class Test {
    public static void main(String[] ar) throws URISyntaxException, UnsupportedEncodingException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        String query = "{adventureList{items{title}}}";


       AEMHeadlessClient<Observable<GraphQlResponse>> aemHeadlessClient1 = AEMHeadlessClient.<Observable<GraphQlResponse>>builder()
                .endpoint(new URI("http://localhost:4502"))
                .basicAuth("admin", "admin")
                .executionStrategy(ReactiveExecutionStrategy.class)
                .build();

        Observable<GraphQlResponse> observable =aemHeadlessClient1.executeQuery(query);
        observable.subscribe((item) -> System.out.println("Reactive :: " + item.getData()));

        AEMHeadlessClient<GraphQlResponse> aemHeadlessClient2 = AEMHeadlessClient.<GraphQlResponse>builder()
                .endpoint(new URI("http://localhost:4502"))
                .basicAuth("admin", "admin")
                .executionStrategy(AsyncExecutionStrategy.class)
                .build();
        GraphQlResponse graphQlResponse= aemHeadlessClient2.executeQuery(query);
        System.out.println("Async result:: " + graphQlResponse.getData());

          }

}
