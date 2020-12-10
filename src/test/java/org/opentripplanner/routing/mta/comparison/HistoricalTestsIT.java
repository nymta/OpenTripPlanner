/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package org.opentripplanner.routing.mta.comparison;

import org.opentripplanner.routing.mta.comparison.test_file_format.Result;
import org.opentripplanner.routing.mta.comparison.test_file_format.ItinerarySummary;

import org.joda.time.DateTime;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.graph_builder.GraphBuilder;

import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.CommandLineParameters;
import org.opentripplanner.standalone.OTPMain;
import org.opentripplanner.standalone.Router;

public class HistoricalTestsIT extends RoutingResource {
	
	private static String ALL_TESTS_DIR = "src/test/resources/mta/comparison/"; 

	private Graph graph;
	
	private Router router;
	
	private void buildGraph(File graphDir) {
		if(graphDir.exists()) {
			File graphFile = new File(graphDir + "/Graph.obj");
			if(graphFile.exists()) {
				try {
					graph = Graph.load(graphFile);
				} catch (Exception e) {
					GraphBuilder builder = GraphBuilder.forDirectory(new CommandLineParameters(), graphDir);
					builder.run();
					graph = builder.getGraph();
				}
			} else {
				GraphBuilder builder = GraphBuilder.forDirectory(new CommandLineParameters(), graphDir);
				builder.run();
				graph = builder.getGraph();
			}
			
    		router = new Router(graphDir.getParent(), graph);
    		router.startup(OTPMain.loadJson(new File(graphDir, Router.ROUTER_CONFIG_FILENAME)));
		}
	}
	
    private List<File> findTestDirs() {
    	List<File> data = new ArrayList<File>();
    
    	File allTests = new File(ALL_TESTS_DIR);
    	for(String testDirPath : allTests.list()) {
    		File testDir = new File(ALL_TESTS_DIR + "/" + testDirPath);    		
    		if(!testDir.isDirectory())
    			continue;
    		
    		for(String idealResultPath : testDir.list()) {
        		File idealResultFile = new File(testDir + "/" + idealResultPath);
    			
        		if(!idealResultFile.isFile() || !idealResultFile.getName().endsWith(".txt") || idealResultFile.getName().endsWith("_results.txt"))
        			continue;

        		data.add(testDir); 
    		}
    	}
    	
    	return data;
    }
    	  
    private void runThroughGraph(File input, File output) throws Exception {
		FileWriter resultsFileWriter = new FileWriter(output);
		
		List<Result> ideals = Result.loadResults(input);			
		for(Result result : ideals) {
			RoutingRequest request = 
					super.buildRequest(router.defaultRoutingRequest, graph.getTimeZone());
			
			request.wheelchairAccessible = result.query.accessible;
			request.setDateTime(new DateTime(result.query.time).toDate());
			request.setFrom(Double.parseDouble(result.query.origin.split(",")[0]), 
					Double.parseDouble(result.query.origin.split(",")[1]));
			request.setTo(Double.parseDouble(result.query.destination.split(",")[0]), 
					Double.parseDouble(result.query.destination.split(",")[1]));
			request.ignoreRealtimeUpdates = true;
			request.numItineraries = 6;
			request.hardPathBanning = true; // once we use a set of routes, don't use it again

	  		switch(result.query.optimizeFlag) {
    			case "W":
    				request.optimize = OptimizeType.WALKING; 
    				break;
    			case "X":
    				request.optimize = OptimizeType.TRANSFERS;
    				break;
    			case "T":
    				request.optimize = OptimizeType.QUICK;
    				break;
	  		}
	  		
	  		try {
	  			GraphPathFinder gpFinder = new GraphPathFinder(router);
	  			List<GraphPath> paths = gpFinder.graphPathFinderEntryPoint(request);

	  			TripPlan plan = GraphPathToTripPlanConverter.generatePlan(paths, request);

                String optimizeFlag = null;
        		switch(request.optimize) {
    			case WALKING:
    				optimizeFlag = "W";
    				break;
    			case TRANSFERS:
    				optimizeFlag = "X";
    				break;
    			case QUICK:
    				optimizeFlag = "T";
    				break;
				default:
					break;
        		}
        		
                resultsFileWriter.write("Q " + ((request.wheelchairAccessible) ? "Y " : "N ") + 
        				request.dateTime*1000 + " " + 
        				request.from.lat + "," + request.from.lng + " " + 
        				request.to.lat + "," + request.to.lng + " " + 
        				optimizeFlag + 
        				"\n");
                
                int i = 1;
                for(Itinerary itin : plan.itinerary) {
                	ItinerarySummary is = ItinerarySummary.fromItinerary(itin);
                	is.itineraryNumber = i;
                	i++;
                    
                	resultsFileWriter.write(
                     		 "S " + is.itineraryNumber + " " + String.format("%.2f",is.walkDistance)
                     		 		+ " " + is.transitTime + " " + is.routes + "\n");
                }
	  		} catch (PathNotFoundException e) {
	  			resultsFileWriter.write("**** NOT FOUND ****");
	  		}
            
		} // for result

		resultsFileWriter.close();
    }
    
    private void runQueries(File testDir) throws Exception {    	
    	if(!testDir.isDirectory())
    		return;
    		
		File idealFile = new File(testDir + "/ideal.txt");
		File idealResultsFile = new File(testDir + "/ideal_results.txt");
		runThroughGraph(idealFile, idealResultsFile);
		
		File baselineFile = new File(testDir + "/baseline.txt");
		File baselineResultsFile = new File(testDir + "/baseline_results.txt");
		runThroughGraph(baselineFile, baselineResultsFile);
    }
    
	@TestFactory
	@Test
	public Collection<DynamicTest> runTests() throws Exception {		
		List<DynamicTest> generatedTests = new ArrayList<>();

		for(File testDir : this.findTestDirs()) {
			System.out.println("***************************************************************");
    		System.out.println("                TEST DIR: " + testDir.getName());
    		System.out.println("***************************************************************");

			buildGraph(new File(testDir + "/graph"));
			runQueries(testDir);
			
			File idealFile = new File(testDir.getAbsolutePath() + "/ideal.txt");
			if(idealFile.exists()) {
				File idealResultsFile = new File(testDir.getAbsolutePath() + "/ideal_results.txt");
			
				ScoreAgainstIdealComparison t2 = new ScoreAgainstIdealComparison();
				t2.setIdealFile(idealFile.getPath());
				t2.setTestResultsFile(idealResultsFile.getPath());			
	    		generatedTests.addAll(t2.getTests());
			}
			
			File baselineFile = new File(testDir.getAbsolutePath() + "/baseline.txt");
			if(baselineFile.exists()) {
				File baselineResultsFile = new File(testDir.getAbsolutePath() + "/baseline_results.txt");

				QualitativeMultiDimInstanceComparison t1 = new QualitativeMultiDimInstanceComparison();
				t1.setBaselineResultsFile(baselineFile.getPath());
				t1.setTestResultsFile(baselineResultsFile.getPath());
				generatedTests.addAll(t1.getTests());
			}
		}

		return generatedTests;
	}
    
}

