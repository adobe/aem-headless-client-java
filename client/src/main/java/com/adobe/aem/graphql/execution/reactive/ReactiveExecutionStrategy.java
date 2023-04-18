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
package com.adobe.aem.graphql.execution.reactive;

import com.adobe.aem.graphql.client.AEMHeadlessClient;
import com.adobe.aem.graphql.client.GraphQlResponse;
import com.adobe.aem.graphql.execution.AbstractExecutionStrategy;
import org.eclipse.jetty.http.HttpHeader;
import org.glassfish.jersey.client.rx.rxjava.RxObservableInvoker;
import org.glassfish.jersey.client.rx.rxjava.RxObservableInvokerProvider;
import org.jetbrains.annotations.NotNull;
import rx.Observable;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import java.net.URI;

public class ReactiveExecutionStrategy extends AbstractExecutionStrategy {

    @Override
    public Observable<GraphQlResponse> execute(@NotNull URI endPoint, @NotNull String query, int expectedCode, AEMHeadlessClient aemHeadlessClient) {

        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client
                .target(endPoint)
                .register(GraphQLMessageBodyReader.class)
                .register(RxObservableInvokerProvider.class);
        Invocation.Builder invocationBuilder = webTarget.request();
        if (!AEMHeadlessClient.isBlank(aemHeadlessClient.getAuthorizationHeader())) {
            invocationBuilder.header(HttpHeader.AUTHORIZATION.asString(), aemHeadlessClient.getAuthorizationHeader());
        }
        Observable<GraphQlResponse> observable = invocationBuilder
                .rx(RxObservableInvoker.class)
                .post(Entity.entity(query, MediaType.APPLICATION_JSON), GraphQlResponse.class);

        return  observable;

    }
}
