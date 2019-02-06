Computing Recursive SPARQL Queries
==================================

Introducing Recursion in SPARQL
-------------------------------

We developed a SPARQL function called `wfn:runSPARQL` that takes a string containing a SPARQL query as input and executes it.
Such SPARQL query **can refer to itself**, thus enabling recursion and extending the range of computable functions within SPARQL.
It allows simple recursive computations such as factorial or graph traversal algorithms to be easily implemented within SPARQL on [Fuseki](https://jena.apache.org/documentation/fuseki2/).

Our work can be found in: 

 * [Computing Recursive SPARQL Queries](https://doi.org/10.1109/ICSC.2014.54). Maurizio Atzori. _8th IEEE International Conference on Semantic Computing_ (2014)




Install and compile *(tested with Fuseki 3.8.0)*
-------------------
1. ensure you have OpenJDK 8 or later correctly installed
2. download and extract [Apache Fuseki 2](https://jena.apache.org/download/#apache-jena-fuseki) on directory `./fuseki`
3. compile *runSPARQL* Java source code: `./compile`
4. run fuseki using testing settings: `./run-fuseki`. The command will use the the following default configurations:
    - fuseki server settings in `config/config.ttl` 
    - initial data in `config/dataset.ttl` 
    - log4j settings in `config/log4j.properties`
5. go to: [http://127.0.0.1:3030](http://127.0.0.1:3030) and run a recursive query (see below for examples)


Usage
-----
Officially, we suggest the use of the following namespace:

    PREFIX wfn: <http://webofcode.org/wfn/>

The function is called with `wfn:runSPARQL`. Please note that if function is not registered, you must use `PREFIX wfn: <java:org.webofcode.wfn.>`.

The usage is the following:

     wfn:runSPARQL(query, endpoint [,i0, i1, ...])
     
where: 
  - `query` is a SPARQL query fragment that will be executed by the `runSPARQL` function
  - `endpoint` is a url pointing to the SPARQL endpoint that must be used to run the SPARQL query
  - `i0`, `i1`, etc are optional parameters to be used in the SPARQL query 


When called, the `runSPARQL` custom function will execute `query` (that may contain references to runSPARQL itself) against `endpoint`, where variables `?i0`, `?i1`, etc are binded to the values passed to `runSPARQL`.

Note that the query is expected to produce one result only. If there are more results, only the first one provided by the endpoint is returned (blank node are filtered out).

Examples
--------

### Computing the Factorial
The following is an example of recursive SPARQL query that computes the factorial of 3, returning `"6"^^xsd:integer`: 

    PREFIX wfn: <http://webofcode.org/wfn/> 

    SELECT ?result { 
            # bind variables to parameter values 
            VALUES (?query ?endpoint) { ( 
                    "BIND ( IF(?i0 <= 0, 1, ?i0 * wfn:runSPARQL(?query,?endpoint, ?i0 -1)) AS ?result)" 
                    "http://127.0.0.1:3030/ds/sparql"
            )}
       
            # actual call of the recursive query 
            BIND( wfn:runSPARQL(?query,?endpoint,3) AS ?result)
    }


At the first iteration, runSPARQL will run the following:

    PREFIX wfn: <java:org.webofcode.wfn.>
    SELECT ?result {
        # bind variables to parameter values
        VALUES (?query ?endpoint ?i0) { (
            "BIND ( IF(?i0 <= 0, 1, ?i0 * wfn:runSPARQL(?query,?endpoint, ?i0 -1)) AS ?result)"
            "http://127.0.0.1:3030/ds/sparql"
            3
        )}
        
        # the recursive query
        BIND ( IF(?i0 <= 0, 1, ?i0 * wfn:runSPARQL(?query,?endpoint, ?i0 -1)) AS ?result)
        FILTER (!isBlank(?result))
    } LIMIT 1

In the following iteration `?i0` will be then set to `2`, ..., until `0` is reached and recursion stops as per the base case specified in `?query`.


### Graph search
An example of recursive SPARQL query that computes the distance between two nodes ([dbo:PopulatedPlace](http://dbpedia.org/ontology/PopulatedPlace) and [dbo:Village](http://dbpedia.org/ontology/Village)) in a hierarchy: 

```
PREFIX wfn: <http://webofcode.org/wfn/>

SELECT ?result { 
        # bind variables to parameter values 
        VALUES (?query ?endpoint) { ( 
                "?i0 <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?next. BIND( IF(?next = <http://dbpedia.org/ontology/PopulatedPlace>, 1 , 1 + wfn:runSPARQL(?query, ?endpoint, ?next)) AS ?result)" 
                "http://127.0.0.1:3030/ds/sparql"
        )}
   
        # actual call of the recursive query 
        BIND( wfn:runSPARQL(?query,?endpoint, <http://dbpedia.org/ontology/Village>) AS ?result)
} 
```

Note that the default dataset is used (in `config/dataset.ttl`) where [dbo:Village](http://dbpedia.org/ontology/Village) and [dbo:PopulatedPlace](http://dbpedia.org/ontology/PopulatedPlace) have distance 2 like in DBpedia: 

    dbr:Village -> dbr:Settlement -> dbr:PopulatedPlace

The call of `runSPARQL` will return `"2"^^xsd:integer`, by generating another SPARQL query, recursively calling `runSPARQL`, that in its first iteration is similar to the following one:

    PREFIX wfn: <java:org.webofcode.wfn.>
    SELECT ?result {
        # bind variables to parameter values
        VALUES (?query ?endpoint ?i0) { (
            "?i0 <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?next. BIND( IF(?next = <http://dbpedia.org/ontology/PopulatedPlace>, 1 , 1 + wfn:runSPARQL(?query, ?endpoint, ?next)) AS ?result)"
            "http://127.0.0.1:3030/ds/sparql"
            <http://dbpedia.org/ontology/Village>
         )}
        
        # the recursive query
        ?i0 <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?next. 
        BIND( IF(?next = <http://dbpedia.org/ontology/PopulatedPlace>, 1 , 1 + wfn:runSPARQL(?query, ?endpoint, ?next)) AS ?result)
        FILTER (!isBlank(?result))
    } LIMIT 1


For reading purposes only, the above can be **loosely** rewritten to the following:

    SELECT ?result 
    { 
       <http://dbpedia.org/ontology/Village> <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?next. 
       BIND( IF(?next = <http://dbpedia.org/ontology/PopulatedPlace>, 1 , ?next) AS ?result)  # otherwise use recursion
    } LIMIT 1


