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
  - `i0`, `i1`, etc. are optional parameter to be used in the SPARQL query 


When called, the `runSPARQL` custom function will execute `query` (that may contain references to runSPARQL itself) against `endpoint`, where variables `?i0`, `?i1`, etc are binded to the values passed to `runSPARQL`.


Examples
--------

### Computing the Factorial
The following is an example of recursive SPARQL query that computes the factorial of 3: 
```
PREFIX wfn: <http://webofcode.org/wfn/> 

SELECT ?result 
{ 
        # bind variables to parameter values 
        VALUES (?query ?endpoint) { ( 
                "BIND ( IF(?i0 <= 0, 1, ?i0 * wfn:runSPARQL(?query,?endpoint, ?i0 -1)) AS ?result)" 
                "http://127.0.0.1:3030/ds/sparql"
        )}
  
   
        # actual call of the recursive query 
        BIND( wfn:runSPARQL(?query,?endpoint,3) AS ?result)
}
```

### Graph search
An example of recursive SPARQL query that computes the distance between two nodes ([dbo:PopulatedPlace](http://dbpedia.org/ontology/PopulatedPlace) and [dbo:Village](http://dbpedia.org/ontology/Village)) in a hierarchy: 

```
PREFIX wfn: <http://webofcode.org/wfn/>
PREFIX db: <http://dbpedia.org/>
PREFIX dbo: <http://dbpedia.org/ontology/>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?result 
{ 
        # bind variables to parameter values 
        VALUES (?query ?endpoint) { ( 
                "?i0 <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?next. BIND( IF(?next = <http://dbpedia.org/ontology/PopulatedPlace>, 1 , 1 + wfn:runSPARQL(?query, ?endpoint, ?next)) AS ?result)" 
                "http://127.0.0.1:3030/ds/sparql"
        )}
   
        # actual call of the recursive query 
        BIND( wfn:runSPARQL(?query,?endpoint, <http://dbpedia.org/ontology/Village>) AS ?result)
} 
```

The call of `runSPARQL` will generate another SPARQL query, recursively calling `runSPARQL`, similar to the following one:
```
PREFIX wfn: <java:org.webofcode.wfn.>
PREFIX db: <http://dbpedia.org/>
PREFIX dbo: <http://dbpedia.org/ontology/>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?result 
{ 
   <http://dbpedia.org/ontology/Village> <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?next. 
   BIND( IF(?next = <http://dbpedia.org/ontology/Place>, 1 , ?next) AS ?result)
} LIMIT 1
```


