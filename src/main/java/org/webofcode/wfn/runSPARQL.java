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
*/

import org.apache.jena.sparql.function.*;
import org.apache.jena.sparql.expr.NodeValue;
//import org.apache.jena.sparql.engine.http.Service;
//import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.jena.sparql.function.FunctionRegistry;


import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.expr.Expr;

import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.rdfconnection.*;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.client.HttpClient;
import org.apache.jena.graph.NodeFactory;

import java.util.*;


public class runSPARQL extends FunctionBase3 {
    static org.slf4j.Logger logger =  ARQ.getInfoLogger();


   public static void init() {
        // Register with the global function registry
        logger.info("runSPARQL: init called");
        FunctionRegistry.get().put("http://webofcode.org/wfn/runSPARQL", runSPARQL.class) ;        
    }

    public runSPARQL() {
    }


    public NodeValue exec(NodeValue nvQuery, NodeValue nvEndpoint, NodeValue nvInputVar) {
        logger.info("exec called");
        
	    String template = 
              	"PREFIX wfn: <java:org.webofcode.wfn.>\n"+
	            "SELECT ?result \n"+ 
	            //"FROM <dataset.rdf>\n"+
	            "{ \n"+
	            "    # bind variables to parameter values\n"+ 
	            "    VALUES (?query ?endpoint ?i0) { ( \n"+
	            "        %query% \n"+
	            "        %endpoint% \n"+
	            "        %i0% \n"+
	            "    )} \n"+
	            "    # the recursive query \n\n"+
	            "    %queryexec% \n"+
	            "} ORDER BY ASC(?result) \n"; // LIMIT 1 is done by List.get(0) 

	    String query = template
		    .replaceAll("%query%",nvQuery.toString())
		    .replaceAll("%endpoint%",nvEndpoint.toString())
		    .replaceAll("%i0%",nvInputVar.toString())
		    .replace("%queryexec%",nvQuery.toString().trim().replaceAll("^\\\"(.*)\\\"$","$1"));
	    String service = nvEndpoint.asUnquotedString();

            
        //QueryExecution qe = QueryExecutionFactory.create(query); // local call not working without specifying a dataset

        
        logger.info("runSPARQL: i0 = " + nvInputVar.toString());
        logger.info("runSPARQL: service = " + service);
        logger.info("runSPARQL: " + ARQConstants.sysCurrentDataset ); //symDatasetDefaultGraphs.toString()) ;


	    List<RDFNode> solutions = null;
       /* try ( RDFConnection conn = RDFConnectionFactory.connect("http://127.0.0.1:3030/ds") ) {

            logger.info("runSPARQL: within try x1");

            ResultSet rs = conn.query(query).execSelect() ;
            logger.info("runSPARQL: within try x2");
            solutions = resultSet2List(rs);
            //return ResultSetFactory.copyResults(rs) ;


        }*/

        /*
            An HttpClient is necessary since default accept only a limited number of connections (no more than 5)
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
        
        logger.info("runSPARQL: result size was " + solutions.size());
        if (solutions.size()>0) {
            RDFNode node = solutions.get(0); // only the first solution is used

		    logger.info("runSPARQL: result was " + node);
		    if (node == null) 
		        return nodeNone(); //nvNothing; //Expr.NONE; //throw new RuntimeException("runSPARQL: node null");
	        return NodeValue.makeNode(node.asNode());
                    
        } else {
	        logger.info("runSPARQL: resultset was empty");
		    return nodeNone(); //nvNothing; // Expr.NONE; //NodeValue.makeString("no result!");        
        }

    }
    
    @SuppressWarnings( "deprecation" )
    private static NodeValue nodeNone() {
        //return (NodeValue) NodeValue.NONE;
        //return NodeValue.nvNothing; // equivalent to: NodeValue.makeNode(NodeFactory.createBlankNode(strForUnNode)) 
        return NodeValue.makeNode(NodeFactory.createBlankNode("noResult"));
        //return NodeValue.NONE.eval(null,null);
    }
    
    /** convert a ResultSet with column "result" into a List of RDFNode */
    private static List<RDFNode> resultSet2List(ResultSet rs) {
        if (rs == null) throw new RuntimeException("runSPARQL: null as result");
        
        List<RDFNode> solutions = new ArrayList<>();	        
	    	        
        while(rs.hasNext()) {
	        QuerySolution res = rs.next();
	        if(res == null) throw new RuntimeException("runSPARQL: querysolution was null");
	        RDFNode node = res.get("result");
	        solutions.add(node);	        
        }
        
        
        return solutions;
    }        

}
