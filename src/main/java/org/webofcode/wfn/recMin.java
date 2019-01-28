/* 
 *  Copyright 2014-2019. Maurizio Atzori <atzori@unica.it>
 *
 */

package org.webofcode.wfn;

/*
import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.fuseki.FusekiLib;
import org.apache.jena.fuseki.embedded.FusekiServer;
import org.apache.jena.fuseki.system.FusekiLogging;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase1;
import org.apache.jena.sparql.function.FunctionRegistry;

import org.apache.jena.sparql.engine.http.Service;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
*/

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


/**
 * This class implements the recMin function to be used within SPARQL queries in the Apache Fuseki 2 triplestore.
*/
public class recMin extends FunctionBase3 {
    private final static boolean CACHE_ENABLED = true;

    static Logger log = LoggerFactory.getLogger(recMin.class);
    static int nInstances = 0;
    
    static Map<String,NodeValue> cache = new ConcurrentHashMap<>();

    static final NodeValue VISITED = NodeValue.makeNode(NodeFactory.createBlankNode("VISITED"));
    final String TEMPLATE =
          	"PREFIX wfn: <java:org.webofcode.wfn.>\n"+
            "SELECT ?result { \n"+
            "    # bind variables to parameter values\n"+ 
            "    VALUES (?query ?endpoint ?i0) { ( \n"+
            "        %querySnippet% \n"+
            "        %endpoint% \n"+
            "        %i0% \n"+
            "    )} \n"+
            "    # the recursive query \n\n"+
            "    %queryexec% \n"+
            "    FILTER (!isBlank(?result))\n"+
            "} ORDER BY (?result) \n"; // LIMIT 1 is done by List.get(0) -- in combination with DESC means max


    public static void init() {
        log.info("Registering function");
        FunctionRegistry.get().put("http://webofcode.org/wfn/recMin", recMin.class);
    }
    public recMin() {
        nInstances += 1;
        //log.debug("Creating instance #{}", nInstances);
    }

    public NodeValue exec(NodeValue nvQuery, NodeValue nvEndpoint, NodeValue nvInputVar) {
        String querySnippet = nvQuery.toString();
        String i0 = nvInputVar.toString();
        String service = nvEndpoint.asUnquotedString();
        String endpoint = nvEndpoint.toString();
        
        log.info("Calling with {}", i0);

        String query = TEMPLATE
            .replaceAll("%querySnippet%", querySnippet)
            .replaceAll("%endpoint%", endpoint)
            .replaceAll("%i0%", i0)
            .replace("%queryexec%", querySnippet.trim().replaceAll("^\\\"(.*)\\\"$","$1"));
        
        NodeValue result = null;  

        // try to avoid loops    
        String key = String.format("%s|%s", i0, query); // endpoint
        if (CACHE_ENABLED) {
            if(cache.containsKey(key)) {
                result = cache.get(key); 
                log.info("Loop found for i0={}, result={}", i0, result);
                return result==VISITED? nodeNone(): result; 
            } else 
                cache.put(key, VISITED); // mark as "visited (but not resolved yet)" 
        }
        
        List<RDFNode> solutions = executeQuery(query, service);
        //log.info("Result size for i0={} was {}, solutions= {}", i0, solutions.size(), solutions);
        
        if (solutions.size()>0) {
            RDFNode node = solutions.get(0); // only the first solution is used
            result =(node == null) ? nodeNone() : NodeValue.makeNode(node.asNode()); // check why node can/could(?) be null
        } else {
	        log.warn("Resultset was empty");
		    result = nodeNone(); //nvNothing; // Expr.NONE; //NodeValue.makeString("no result!");
        }

	    //log.info("Result was {}", result);
	    log.info("Result for {} was {}, solutions={}", i0, result, solutions);
        if(CACHE_ENABLED) cache.put(key, result);
        return result;        

    }
    
    private static List<RDFNode> executeQuery(String query, String service) {
    
        //QueryExecution qe = QueryExecutionFactory.create(query); // local call not working without specifying a dataset        
        //log.info(""+ARQConstants.sysCurrentDataset ); //symDatasetDefaultGraphs.toString()) ;


	    List<RDFNode> solutions = null;
       /* try ( RDFConnection conn = RDFConnectionFactory.connect("http://127.0.0.1:3030/ds") ) {

            log.info("within try x1");

            ResultSet rs = conn.query(query).execSelect() ;
            log.info("within try x2");
            solutions = resultSet2List(rs);
            //return ResultSetFactory.copyResults(rs) ;


        }*/


        /*
            An HttpClient is necessary since default client only accepts a limited number of connections (no more than 5)
            https://stackoverflow.com/questions/49661698/how-can-i-change-the-number-of-connections-par-route-in-jena-more-than-5
        */
        HttpClient client = HttpClientBuilder.create().build();
	    
	    try (QueryExecution qe = 
	            QueryExecutionFactory.sparqlService(service, query, client)
	            //QueryExecutionFactory.create(query, Dataset.getDefaultModel()) 
	      ) {
	      
            ResultSet rs = qe.execSelect(); // sparql query is actually executed remotely on the endpoint
            //rs = ResultSetFactory.copyResults(rs) ;
            solutions = resultSet2List(rs);            
        }
        return solutions;
    
    }
    
    //@SuppressWarnings( "deprecation" )
    private static NodeValue nodeNone() {
        //return (NodeValue) NodeValue.NONE;
        //return NodeValue.nvNothing; // equivalent to: NodeValue.makeNode(NodeFactory.createBlankNode(strForUnNode)) 
        //return NodeValue.NONE.eval(null,null);
        return NodeValue.makeNode(NodeFactory.createBlankNode("noResult")); // this is working
        //return new NodeValueInteger(-999);
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
