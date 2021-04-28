# AEM Headless Client for Java

See [aem-headless-client-js](https://github.com/adobe/aem-headless-client-js) for the JavaScript version of this client.

## Setup

### Maven Dependency

Add the following dependency to your classpath:

```xml
<dependency>
	<groupId>com.adobe.aem.graphql</groupId>
	<artifactId>aem-headless-client-java</artifactId>
	<version>1.0.0</version>
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

### Creating an AEMHeadless Client

Basic creation of the client looks as follows: 

```java
import com.adobe.aem.graphql.client.AEMHeadlessClient
...
AEMHeadlessClient aemHeadlessClient 
                     = new AEMHeadlessClient(new URI("http://localhost:4503"));
...
```

If a non-standard GraphQL endpoint is used on AEM side, the endpoint may contain a full path:

```java
new AEMHeadlessClient(new URI("http://localhost:4503/content/graphql-custom"));
```
### Using Authorization

If authorization is required, additional constructor arguments can be added:

```java
// ... or for basic auth
// new AEMHeadlessClient(new URI("http://localhost:4502"), "admin", "admin")
// ... or for bearer token
// new AEMHeadlessClient(new URI("http://localhost:4502"), token)
```

### Running Queries 

To execute a simple GraphQL POST query:

```java
String query = "{\n" + 
				"  articleList{\n" + 
				"    items{ \n" + 
				"      _path\n" + 
				"    } \n" + 
				"  }\n" + 
				"}";

try {
	GraphQlResponse response = aemHeadlessClient.postQuery(query);
	JsonNode data = response.getData();
	... use the data
} catch(AEMHeadlessClientException e) {
	// e.getMessage() will contain an error message (independent of type of error)
	// if a response was received, e.getGraphQlResponse() will return it (otherwise null)
}
```
To execute a persisted query:

```java

try {
   // for this to work, "/myProj/queryName" needs to be set up on AEM side
	GraphQlResponse response = aemHeadlessClient.getQuery("/myProj/queryName");
	JsonNode data = response.getData();
	... use the data
} catch(AEMHeadlessClientException e) {
	// e.getMessage() will contain an error message (independent of type of error)
	// if a response was received, e.getGraphQlResponse() return it (otherwise null)
}
```

To list available persisted queries for a configuration name:

```java
List<PersistedQuery> queries = client.listQueries("myProj");
queries.stream().forEach( persistedQuery -> { 
    /* use e.g. persistedQuery.getShortPath()... or  persistedQuery.getQuery() */ 
});
```

## API Reference

See generated Javadoc as published to Maven Central.


## Contributing

Contributions are welcome! Read the [Contributing Guide](./.github/CONTRIBUTING.md) for more information.

## Licensing

This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.
