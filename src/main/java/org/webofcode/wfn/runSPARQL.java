/* 
 *  Copyright 2014-2019. Maurizio Atzori <atzori@unica.it>
 *
 */
 
package org.webofcode.wfn;

import org.apache.jena.sparql.function.*;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;
import org.apache.jena.sparql.function.FunctionRegistry;


import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.expr.Expr;

import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.rdfconnection.*;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.client.HttpClient;
import org.apache.jena.graph.NodeFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.*;
import static java.util.stream.Collectors.*;

import org.apache.jena.atlas.lib.Lib;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;

/**
 * This class implements the runSPARQL function to be used within SPARQL queries in the Apache Fuseki 2 triplestore.
*/
public class runSPARQL extends FunctionBase {
    static Logger log = LoggerFactory.getLogger(runSPARQL.class);
    
    static final String FN_NAME = "http://webofcode.org/wfn/runSPARQL";
    static String TEMPLATE;


    public static void init() {
        log.info("Registering function {}", FN_NAME);
        FunctionRegistry.get().put(FN_NAME, runSPARQL.class);
        
        // load sparql template file     
        try {
            TEMPLATE = String.join("\n", Files.readAllLines(Paths.get(System.getProperty("sparql_template"))));
        } catch(Exception e) {
            log.error("Error while reading sparql template");
        }
        //log.debug("Template set to {}", TEMPLATE);
        
    }

    @Override
    public void checkBuild(String uri, ExprList args) { 
        if ( args.size() < 2 )
            throw new QueryBuildException("Function '"+Lib.className(this)+"' takes at least 2 arguments (a query snippet and an endpoint url)") ;
    }

    public NodeValue exec(List<NodeValue> args) { 
    
        if ( args == null )
            // The contract on the function interface is that this should not happen.
            throw new ARQInternalErrorException(Lib.className(this)+": Null args list") ;
        
        if ( args.size() < 2 )
            throw new ExprEvalException(Lib.className(this)+": Wrong number of arguments: Wanted 1+, got "+args.size()) ;
        

        String querySnippet = args.get(0).toString();
        String endpoint = args.get(1).toString();
        
        List<NodeValue> fnArgs = new ArrayList<>(args);
        fnArgs.remove(0);
        fnArgs.remove(0);
        log.info("Calling with {}", fnArgs);
            

        String varNames = IntStream.range(0,fnArgs.size()).mapToObj(i -> "?i"+i).collect(Collectors.joining(" "));
        String varValues = args.stream().map(e -> e.toString()).collect(Collectors.joining("\n"));

        String bindings = String.format("VALUES (?query ?endpoint %s) { (\n%s\n)}", varNames, varValues);
        
        String query = TEMPLATE
            .replace("%query_snippet%", querySnippet.trim().replaceAll("^\\\"(.*)\\\"$","$1"))
            .replace("%bindings%", bindings);
                    
                
        NodeValue result = null;  


        List<RDFNode> solutions = executeQuery(query, args.get(1).asUnquotedString());
        
        if (solutions.size()>0) {
            RDFNode node = solutions.get(0); // only the first solution is used
            result =(node == null) ? nodeNone() : NodeValue.makeNode(node.asNode()); // check why node can/could(?) be null
        } else {
	        log.warn("Resultset was empty");
		    result = nodeNone(); //nvNothing; // Expr.NONE; //NodeValue.makeString("no result!");
        }

	    //log.info("Result was {}", result);
	    log.info("Result for {} was {}, solutions={}", fnArgs, result, solutions);
        return result;        

    }
    
    private static List<RDFNode> executeQuery(String query, String service) {
	    List<RDFNode> solutions = null;

        /*
            An HttpClient is necessary since default client only accepts a limited number of connections (no more than 5)
            https://stackoverflow.com/questions/49661698/how-can-i-change-the-number-of-connections-par-route-in-jena-more-than-5
        */
        HttpClient client = HttpClientBuilder.create().build();
	    
	    log.debug("Running query\n"+query);
	    
	    try (QueryExecution qe = 
	            QueryExecutionFactory.sparqlService(service, query, client)
	      ) {
	      
            ResultSet rs = qe.execSelect(); // sparql query is actually executed remotely on the endpoint
            //rs = ResultSetFactory.copyResults(rs) ;
            solutions = resultSet2List(rs);            
        }
        return solutions;
    
    }
    
    //@SuppressWarnings( "deprecation" )
    private static NodeValue nodeNone() {
        return NodeValue.makeNode(NodeFactory.createBlankNode("noResult")); 
    }
    
    /** convert a ResultSet with column "result" into a List of RDFNode */
    private static List<RDFNode> resultSet2List(ResultSet rs) {
        if (rs == null) throw new RuntimeException("null as result");
        
        List<RDFNode> solutions = new ArrayList<>();	        
	    	        
        while(rs.hasNext()) {
	        QuerySolution res = rs.next();
	        if(res == null) throw new RuntimeException("querysolution was null");
	        RDFNode node = res.get("result");
	        solutions.add(node);	        
        }
        
        
        return solutions;
    }        

}
