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

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.expr.Expr;
import java.util.*;

public class runSPARQL extends FunctionBase3 {


    public runSPARQL() {
        super(); 
    }


    public NodeValue exec(NodeValue nvQuery, NodeValue nvEndpoint, NodeValue nvInputVar) {
        
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
	    "} ORDER BY DESC(?result) \n"; // LIMIT 1 is done by List.get(0) 

	    String query = template
		    .replaceAll("%query%",nvQuery.toString())
		    .replaceAll("%endpoint%",nvEndpoint.toString())
		    .replaceAll("%i0%",nvInputVar.toString())
		    .replace("%queryexec%",nvQuery.toString().trim().replaceAll("^\\\"(.*)\\\"$","$1"));

	    //System.out.println("\n====================\n"+query+"\n====================\n");
	    //query = "select * from <Cagliari.rdf> where {?s ?result ?o } limit 1";
            
        //QueryExecution qe = QueryExecutionFactory.create(query); // local call not working without specifying a dataset

	    String service = nvEndpoint.asUnquotedString();
        QueryExecution qe = QueryExecutionFactory.sparqlService(service, query);
        ResultSet rs = qe.execSelect();
        
        List<RDFNode> solutions = resultSet2List(rs);
        
        System.out.println("runSPARQL: result size was" + solutions.size());
        if (solutions.size()>0) {
            RDFNode node = solutions.get(0); // only the first solution is used

		    System.out.println("runSPARQL: result was " + node);
		    if (node == null) 
		        return nodeNone(); //nvNothing; //Expr.NONE; //throw new RuntimeException("runSPARQL: node null");
	        return NodeValue.makeNode(node.asNode());
                    
        } else {
	        System.out.println("runSPARQL: resultset was empty");
		    return nodeNone(); //nvNothing; // Expr.NONE; //NodeValue.makeString("no result!");        
        }

    }
    
    @SuppressWarnings( "deprecation" )
    private static NodeValue nodeNone() {
        //return (NodeValue) NodeValue.NONE;
        return NodeValue.nvNothing;
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
