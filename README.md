Computing Recursive SPARQL Queries
==================================

**NOTE: THIS IS A DEVELOPMENT VERSION** devoted to the implementation of the loop-aggregate feature inspired by the work of Prof. Carlo Zaniolo on Datalog.

Function is being refactored heavily and renamed `wfn:recMin`


Introducing Recursion in SPARQL
-------------------------------

We developed a SPARQL function called `wfn:recMin` that takes a string containing a SPARQL query as input and executes it.
Such SPARQL query **can refer to itself**, thus enabling recursion and extending the range of computable functions within SPARQL.
It allows simple recursive computations such as factorial or graph traversal algorithms to be easily implemented within SPARQL on [Fuseki](https://jena.apache.org/documentation/fuseki2/).

Our work can be found in: 

 * [Computing Recursive SPARQL Queries](https://doi.org/10.1109/ICSC.2014.54). Maurizio Atzori. _8th IEEE International Conference on Semantic Computing_ (2014)


Name space
----------
Officially, we suggest the use of the following namespace:

    wfn: <http://webofcode.org/wfn/>

Function will be called with `wfn:recMin`. Please note that if function is not registered, you must use `PREFIX wfn: <java:org.webofcode.wfn.>` as in the examples section.


Install and compile *(tested with Fuseki 3.8.0)*
-------------------
1. ensure you have OpenJDK 8 or later correctly installed
2. download and extract [Apache Fuseki 2](https://jena.apache.org/download/#apache-jena-fuseki) on directory `./fuseki`
3. compile recMin Java source code: `./compile`
4. run fuseki: `./run-fuseki` *(this will use settings in `./config/config.ttl`)*


Usage
-----

`recMin(query, endpoint, initValue)`


Examples
--------

### Computing the Factorial
The following is an example of recursive SPARQL query that computes the factorial of 3: 
```
PREFIX wfn: <http://webofcode.org/wfn/>  # alternatively: PREFIX wfn: <java:org.webofcode.wfn.>

SELECT ?result 
{ 
        # bind variables to parameter values 
        VALUES (?query ?endpoint) { ( 
                "BIND ( IF(?i0 <= 0, 1, ?i0 * wfn:recMin(?query,?endpoint, ?i0 -1)) AS ?result)" 
                "http://127.0.0.1:3030/ds/sparql"
        )}
  
   
        # actual call of the recursive query 
        BIND( wfn:recMin(?query,?endpoint,3) AS ?result)
}
```

### Graph search: node distance
An example of recursive SPARQL query that computes the distance between two nodes ([dbo:PopulatedPlace](http://dbpedia.org/ontology/PopulatedPlace) and [dbo:Village](http://dbpedia.org/ontology/Village)) in a hierarchy: 

```
PREFIX wfn: <http://webofcode.org/wfn/>  # alternatively: PREFIX wfn: <java:org.webofcode.wfn.>
PREFIX db: <http://dbpedia.org/>
PREFIX dbo: <http://dbpedia.org/ontology/>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?result 
{ 
        # bind variables to parameter values 
        VALUES (?query ?endpoint) { ( 
                "?i0 <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?next. BIND( IF(?next = <http://dbpedia.org/ontology/PopulatedPlace>, 1 , 1 + wfn:recMin(?query, ?endpoint, ?next)) AS ?result)" 
                "http://127.0.0.1:3030/ds/sparql"
        )}
   
        # actual call of the recursive query 
        BIND( wfn:recMin(?query,?endpoint, <http://dbpedia.org/ontology/Village>) AS ?result)
} 
```

The call of `recMin` will generate another SPARQL query, recursively calling `recMin`, similar to the following one:
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


#### Node Distance with a different base case
This is better for testing:

```
PREFIX wfn: <http://webofcode.org/wfn/>  # alternatively: PREFIX wfn: <java:org.webofcode.wfn.>
PREFIX db: <http://dbpedia.org/>
PREFIX dbo: <http://dbpedia.org/ontology/>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?result 
{ 
        # bind variables to parameter values 
        VALUES (?query ?endpoint) { ( 
                "{?i0 <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?next.} UNION { BIND(<fake> AS ?next)  } BIND( IF(?i0 = <http://dbpedia.org/ontology/Place>, 0 , 1 + wfn:recMin(?query, ?endpoint, ?next)) AS ?result)" 
                "http://127.0.0.1:3030/ds/sparql"
        )}
   
        # actual call of the recursive query 
        BIND( wfn:recMin(?query,?endpoint, <http://dbpedia.org/ontology/Village>) AS ?result)
} 
```
