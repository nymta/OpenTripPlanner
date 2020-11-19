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

import org.junit.Test;
import org.opentripplanner.routing.mta.comparison.test_file_format.ItinerarySummary;
import org.opentripplanner.routing.mta.comparison.test_file_format.Query;
import org.opentripplanner.routing.mta.comparison.test_file_format.Result;

import java.util.*;

import java.io.*;

public class ScoreAgainstIdealComparison {
	
    private String TEST_RESULTS_TXT = "src/test/resources/mta/tripeval_dev_results.txt"; 

    private String BASELINE_RESULTS_TXT = "src/test/resources/mta/tripeval_output_results.txt";
		
    private final String[] metricsDimLabels = new String[] { "APPROVED RESULTS PRESENT", "DISAPPROVED RESULTS PRESENT", "RESULT NOT TAGGED PRESENT" };

	// dimensions: metric
	private int[] resultSummary = new int[3];

	public void setBaselineResultsFile(String f) {
		this.BASELINE_RESULTS_TXT = f;
	}
	
	public void setTestResultsFile(String f) {
		this.TEST_RESULTS_TXT = f;
	}
	
	private List<Result> loadResults(File resultsFile) throws Exception {
    	List<Result> results = new ArrayList<Result>();

    	Scanner resultsReader = new Scanner(resultsFile);

		Query q = null;
		Result r = null;
    	while (resultsReader.hasNextLine()) {
    		String line = resultsReader.nextLine();

    		if(line.startsWith("Q")) {
    	    	if(r != null) {
    				results.add(r);
    	    		r = null;
    	    	}
    			
    			q = new Query(line);
    			r = new Result();    			
    			r.query = q;
    		}

    		if(line.startsWith("S")) {
    			ItinerarySummary s = new ItinerarySummary(line);
    			r.itineraries.add(s);
    		}
    	}
    	
    	if(r != null) {
			results.add(r);
			r = null;
    	}
    	
    	resultsReader.close();
    	
    	return results;
	}
	
    @Test
    public void run() throws IOException, Exception {    	
    	File testResultsFile = new File(TEST_RESULTS_TXT);
    	File baselineResultsFile = new File(BASELINE_RESULTS_TXT);

    	List<Result> testResults = loadResults(testResultsFile);
    	List<Result> baselineResults = loadResults(baselineResultsFile);

    	// ==========================COMPARE RESULTS=====================================
    	int total = 0;
    	for(int i = 0; i < Math.max(baselineResults.size(), testResults.size()); i++) {
    		Result testResult = testResults.get(i);
    		Result baselineResult = baselineResults.get(i);
    		Query baselineQuery = baselineResult.query;

			for(ItinerarySummary testItin : testResult.itineraries) {
				total++;
				
    			boolean foundInRef = false;
        		for(ItinerarySummary refItin : baselineResult.itineraries) {
    				if(ItinerarySummary.RANKER_EQUAL.compare(refItin,  testItin) == 0) {
    	    			if(refItin.approveOfResult == true) {
    	        			resultSummary[0]++;
    					} else if(refItin.approveOfResult == false) {
    		    			resultSummary[1]++;    	    			
    					}
    	    			
    	    			foundInRef = true;
    	    			break;
    				}
        		}
        		
        		// we have a result that isn't in the reference set
        		if(! foundInRef) {
        			resultSummary[2]++;
        		}
    		}
    	}

    	// ==========================PRINT RESULTS=====================================
    	for(int i = 0; i < metricsDimLabels.length; i++) {
    		System.out.println(String.format("%30s", metricsDimLabels[i]) + " ......................... [" + 
    			String.format("%.1f", (float)((resultSummary[i]/(float)total)*100)) 
    			+ "%] " + resultSummary[i] + "/" + total);
    	}
    }

}
