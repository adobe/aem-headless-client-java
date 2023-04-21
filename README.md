# AEM Headless Client for Java

See [aem-headless-client-js](https://github.com/adobe/aem-headless-client-js) for the JavaScript version of this client.

## Setup

### Maven Dependency

Add the following dependency to your classpath:

```xml
<dependency>
	<groupId>com.adobe.aem.headless</groupId>
	<artifactId>aem-headless-client-java</artifactId>
	<version>0.8.0</version>
</dependency>
```

The client comes with a transitive dependency to the Jackson JSON library that also needs to be available at runtime:
 
```xml
<dependency>
	<groupId>com.fasterxml.jackson.core</groupId>
	<artifactId>jackson-databind</artifactId>
</dependency>
```

## Usage


### Reactive - Creating reactive client
```Java
 AEMHeadlessClient<Observable<GraphQlResponse>> aemHeadlessClient = AEMHeadlessClient.<Observable<GraphQlResponse>>builder()
                .endpoint(new URI("http://localhost:4502"))
                .basicAuth("admin", "admin")
                .executionStrategy(ReactiveExecutionStrategy.class)
                .build();

        Observable<GraphQlResponse> observable =aemHeadlessClient.executeQuery(query);
        observable.subscribe((item) -> System.out.println("Reactive :: " + item.getData()));

```

### Async - Creating Async client
```Java
       AEMHeadlessClient<GraphQlResponse> aemHeadlessClient = AEMHeadlessClient.<GraphQlResponse>builder()
        .endpoint(new URI("http://localhost:4502"))
        .basicAuth("admin", "admin")
        .executionStrategy(AsyncExecutionStrategy.class)
        .build();
        GraphQlResponse graphQlResponse= aemHeadlessClient.executeQuery(query);

```

### Creating an AEMHeadless Client

The easiest way to create a client looks as follows: 

```java
import com.adobe.aem.graphql.client.AEMHeadlessClient
...
AEMHeadlessClient aemHeadlessClient = new AEMHeadlessClient("http://localhost:4503");
...
```

If a non-standard GraphQL endpoint is used on AEM side, the endpoint may contain a full path:

```java
aemHeadlessClient = new AEMHeadlessClient("http://localhost:4503/content/graphql-custom");
```

For more complex configurations, the builder pattern is available:

```java
AEMHeadlessClient aemHeadlessClient = AEMHeadlessClient.builder().
   .endpoint("http://localhost:4503")
   // ... further configuration
   .build();
```

To create a client with explicitly set timeouts:

```java
AEMHeadlessClient aemHeadlessClient = AEMHeadlessClient.builder().
   .endpoint("http://localhost:4503")
   .connectTimeout(10000)
   .readTimeout(30000)
   .build();
```

If timeouts are not set explicitly a default of 15 seconds is used. To disable timeouts (not recommended) use the value `0`.

### Using Authorization

If authorization is required, it can be added using the builder:

```java

// basic auth
AEMHeadlessClient aemHeadlessClient = AEMHeadlessClient.builder().
   .endpoint(new URI("http://localhost:4503/content/graphql-custom"))
   .basicAuth("user", "password")
   .build();
// token auth   
AEMHeadlessClient aemHeadlessClient = AEMHeadlessClient.builder().
   .endpoint(new URI("http://localhost:4503/content/graphql-custom"))
   .tokenAuth("token")
   .build();
```


### Running Queries 

To execute a simple GraphQL query:

```java
String query = "{\n" + 
				"  articleList{\n" + 
				"    items{ \n" + 
				"      _path\n" + 
				"    } \n" + 
				"  }\n" + 
				"}";

try {
	GraphQlResponse response = aemHeadlessClient.runQuery(query);
	JsonNode data = response.getData();
	// ... use the data
} catch(AEMHeadlessClientException e) {
	// e.getMessage() will contain an error message (independent of type of error)
	// if a response was received, e.getGraphQlResponse() will return it (otherwise null)
}
```

It is also possible to pass in query parameters using a simple map as second parameter of `runQuery()`:

```
GraphQlResponse response = client.runQuery(query, Map.of("author", "Ian Provo"));
```


### Using Persisted Queries

To execute a persisted query the the method `runPersistedQuery` is used:

```java

try {
   // for this to work, "/myProj/queryName" needs to be set up on AEM side
	GraphQlResponse response = aemHeadlessClient.runPersistedQuery("/myProj/queryName");
	JsonNode data = response.getData();
	// ... use the data
} catch(AEMHeadlessClientException e) {
	// e.getMessage() will contain an error message (independent of type of error)
	// if a response was received, e.getGraphQlResponse() return it (otherwise null)
}
```

Also for persisted queries, query parameters can be passed in as second argument:

```
GraphQlResponse response = client.runPersistedQuery("/myProj/articles", Map.of("author", "Ian Provo"));
```


### Inspecting available persisted queries

To list available persisted queries for a configuration name the following snippet can be used:

```java
List<PersistedQuery> queries = client.listPersistedQueries("myProj");
queries.stream().forEach( persistedQuery -> { 
    /* use e.g. persistedQuery.getShortPath()... or  persistedQuery.getQuery() */ 
});
```

It is best practice to deploy persisted queries as part of the software in content packages as part of the overall configuration in `/conf`.


## API Reference

See generated Javadoc as published to Maven Central.


## Contributing

Contributions are welcome! Read the [Contributing Guide](./.github/CONTRIBUTING.md) for more information.

## Licensing

This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.
