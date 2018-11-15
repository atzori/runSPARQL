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
	"} LIMIT 1\n";

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
        
	if (rs == null) throw new RuntimeException("runSPARQL: null as result");
	if (!rs.hasNext()) throw new RuntimeException("runSPARQL: no result");
	if (rs.hasNext()) {
		QuerySolution res = rs.next();
		if(res == null) throw new RuntimeException("runSPARQL: qs null");
		RDFNode node = res.get("result");
		if (node == null) throw new RuntimeException("runSPARQL: node null");
		System.out.println("node " + node.toString());
	        return NodeValue.makeNode(node.asNode());
	} else
		return NodeValue.makeString("no result!");

	//return NodeValue.makeString("ciao");
    }

}
