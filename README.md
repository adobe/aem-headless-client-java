# AEM Headless Client for Java

See [aem-headless-client-js](https://github.com/adobe/aem-headless-client-js) for the JavaScript version of this client.

## Setup

### Maven Dependency

Add the following dependency to your classpath:

```xml
<dependency>
    <groupId>com.adobe.aem.headless</groupId>
    <artifactId>aem-headless-client-java</artifactId>
    <version>1.2.0</version>
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
### Mapping results to Java POJOs 

To not have to deal with JSON maps and arrays in Java, it is easy to define POJOs and let Jackson (the JSON API used by this library) do the mapping. For non-default mapping any default annotatinos from the Jackson library can be used (e.g. `@JsonSetter`). Results of sub selections can be mapped by using POJOs as member field.

Example POJOs:

```
@JsonIgnoreProperties(ignoreUnknown = true)
public static class Article {
    private String path;
    private String title;
    private Author author;

    String getPath() {
        return path;
    }

    @JsonSetter("_path")
    void setPath(String path) {
        this.path = path;
    }

    String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    Author getAuthor() {
        return author;
    }

    @JsonSetter("authorFragment")
    void setAuthor(Author author) {
        this.author = author;
    }

    @Override
    public String toString() {
        return "Article [path=" + path + ", title=" + title + ", author=" + author + "]";
}
    
public static class Author {
    private String firstName;
    private String lastName;

    String getFirstName() {
        return firstName;
    }

    void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    String getLastName() {
        return lastName;
    }

    void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public String toString() {
        return firstName + " " + lastName;
    }
}
```

To use the automatic mapping, the `GraphQlResponse ` provides the `getItems(Class<?>)` method:

```java
GraphQlResponse response = aemHeadlessClient.runQuery(query);
List<Article> listOfArticles = response.getItems(Article.class);        
```
 

### Using the Query Builder

There is a query builder that simplifies creating the queries using the builder pattern:

```java
GraphQlQuery queryCursorPaging = GraphQlQuery.builder()
        .contentFragmentModelName("article")
        .field("_path")
        .field("title")
        .build();
GraphQlResponse response = aemHeadlessClient.runQuery(query);
List<Article> data = response.getItems(Article.class);        
```

It is also possible to create queries with sub selections and add sorting as follows:

```java
import static com.adobe.aem.graphql.client.GraphQlQueryBuilder.subSelection;
...
GraphQlQuery queryCursorPaging = GraphQlQuery.builder()
        .contentFragmentModelName("article")
        .field("_path")
        .field("title")
        .field(subSelection("authorFragment").field("firstName").field("lastName"))
        .sortBy("title ASC", "_path DESC")
        .build();
```
Sub selections can be nested (the `field` method of the sub selection also sub selections in the same way as the top-level `field` method does).

### Using Filtering

The query builder supports basic filtering as follows:

```java
GraphQlQuery queryWithFiltering = GraphQlQuery.builder()
		.contentFragmentModelName("adventure")
		.field("_path")
		.field("title") 
		.field("price")
		.filter("price", Operator.LOWER, 154)
		.sortBy("title ASC")
		.build()
```
For simple cases, the filter can be given directly to the field method to keep the code DRY (no need to mention 'price' twice):

```java
import static com.adobe.aem.graphql.client.GraphQlQueryBuilder.filter;
...
GraphQlQuery queryWithFiltering = GraphQlQuery.builder()
		.contentFragmentModelName("adventure")
		.field("_path")
		.field("title") 
		.field("price", filter(Operator.LOWER, 154))
		.sortBy("title ASC")
		.build()
```
It is also possible to pass in options as supported by the AEM backend:

```java
import static com.adobe.aem.graphql.client.GraphQlQueryBuilder.ignoreCase;
import static com.adobe.aem.graphql.client.GraphQlQueryBuilder.sensitiveness;
...
GraphQlQuery queryWithFiltering = GraphQlQuery.builder()
		.contentFragmentModelName("adventure")
		.field("_path")
		.field("title", filter(Operator.CONTAINS, "gastronomic", ignoreCase())) 
		.field("price", filter(Operator.LOWER, 154, sensitiveness(0.1)))
		.sortBy("title ASC")
		.build()
```
A filter value can also be set by a query variable:

```java
GraphQlQuery queryWithFilterVar = GraphQlQuery.builder()
		.contentFragmentModelName("adventure")
		.field("_path")
		.field("title") 
		.field("price", filter(Operator.LOWER, Type.Float, "maxPrice")) 
		.sortBy("title ASC", "_path DESC")
		.build();

GraphQlQueryVars vars = GraphQlQueryVars.create();
vars.put("maxPrice", 200);
GraphQlResponse response = client.runQuery(queryWithFilterVar, vars);
...
```
Finally it is also possible to pass the whole filter as variable:

```java
GraphQlQuery queryWithFilter = GraphQlQuery.builder()
		.contentFragmentModelName("article")
		.field("_path")
		.field("title")
		.useFilter()
		.field(subSelection("authorFragment").field("firstName").field("lastName"))
		.sortBy("title ASC", "_path DESC")
		.build();

Map<String,Object> filter = new HashMap<>();
Map<String,Object> filterTitle = new HashMap<>();
filter.put("title", filterTitle);
Map<String,Object> filterTitleExpressions = new HashMap<>();
filterTitle.put("_expressions", filterTitleExpressions);
filterTitleExpressions.put("value", "surf");
filterTitleExpressions.put("_operator", "CONTAINS");
filterTitleExpressions.put("_ignoreCase", true);

GraphQlQueryVars vars = GraphQlQueryVars.create().addVar("filter", filter);
GraphQlResponse response = client.runQuery(queryWithFilter, vars);
...
```


### Using Pagination

The query builder supports building queries with pagination:

```java
GraphQlQuery queryCursorPaging = GraphQlQuery.builder()
        .contentFragmentModelName("article")
        .field("_path")
        .field("title")
        .paginated()
        .sortBy("title ASC")
        .build();
```
To run those queries, the client allows to retrieve a paging cursor and use the `hasNext()` and `next()` methods on it:

```java
GraphQlPagingCursor respCursor = client.createPagingCursor(queryCursorPaging, /* page size */ 10);
while (respCursor.hasNext()) {
    GraphQlResponse resp = respCursor.next();
    for (Article article : resp.getItems(Article.class)) {
        // do something with the article
    }
}

```

There is also the option to use offset/limit-based pagination - this is supported but whenever possible the cursor based approach above should be preferred. Here is an example of the offset based approach:

```java
GraphQlQuery queryOffsetPaging = GraphQlQuery.builder()
        .contentFragmentModelName("article")
        .field("_path")
        .field("title")
        .paginated(GraphQlQuery.PaginationType.OFFSET_LIMIT)
        .sortBy("title ASC")
        .build();
GraphQlResponse respPagingOffset;
for(int pageNo = 0; (respPagingOffset = client.runQuery(queryOffsetPaging, pageNo * pageSize, pageSize)).hasItems(); pageNo++) {
    for (Article article : respPagingOffset.getItems(Article.class)) {
        // do something with the article
    }
}
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
