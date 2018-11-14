Computing Recursive SPARQL Queries
==================================

Introducing Recursion in SPARQL
-------------------------------

We developed a SPARQL function called `wfn:runSPARQL` that takes as a parameter a string containing a SPARQL query and executes it.
Such SPARQL query can refer to itself, thus enabling recursion and extending the range of computable functions within SPARQL.

Our work can be found in: 

 * [Computing Recursive SPARQL Queries](https://doi.org/10.1109/ICSC.2014.54). Maurizio Atzori. _8th IEEE International Conference on Semantic Computing_ (2014)


Name space
----------
Officially, we suggest the use of the following namespace:

    wfn: <http://webofcode.org/wfn/>


Install and compile
-------------------
- Apache Fuseki
- Apache Jena
- javac
 
 
Usage
-----

`runSPARQL(query, endpoint, initValue)`


Examples
--------

###Computing the Factorial
The following is an example of recursive SPARQL query that computes the factorial of 3: 
```
PREFIX wfn: <java:org.webofcode.wfn.>
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

### Graph search: node distance
An example of recursive SPARQL query that computes the distance between two nodes ([dbo:PopulatedPlace](http://dbpedia.org/ontology/PopulatedPlace) and [dbo:Village](http://dbpedia.org/ontology/Village)) in a hierarchy: 

```
PREFIX wfn: <java:org.webofcode.wfn.>
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

The call of `runSPARQL` will generate another SPARQL query, recursively calling `runSPARQL`:
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

