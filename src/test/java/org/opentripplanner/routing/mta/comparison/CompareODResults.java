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
import java.util.*;

import static org.junit.Assert.assertTrue;

import java.io.*;

public class CompareODResults {
	
    private static final String BRANDX_RESULTS_TXT = "src/test/resources/mta/test_atis_results.txt";

    private static final String OTP_RESULTS_TXT = "src/test/resources/mta/test_otp_results.txt";

    private final String[] optimizationDimLabels = new String[] { "WALKING", "TRANSFERS", "TIME" };
    
	private enum optimizationDim { W, X, T };
	
    private final String[] metricsDimLabels = new String[] { "NO RESULTS", "OTP RESULT IS IN BRAND X", "WALKING", "TRANSFERS", "TIME" };

    private enum metricsDim { noResults, match, W, X, T };

	private enum platformDim { OTP, BRANDX };
		
	private int[][][] resultSummary = new int[3][5][2];
	
//    @Test
    public void run() throws IOException, Exception {

    	File BrandXResultsFile = new File(BRANDX_RESULTS_TXT);
    	File otpResultsFile = new File(OTP_RESULTS_TXT);

    	Scanner BrandXResultsReader = new Scanner(BrandXResultsFile);
    	Scanner otpResultsReader = new Scanner(otpResultsFile);

		Query q = null;
		Result r = null;
		
    	List<Result> BrandXResults = new ArrayList<Result>();
    	while (BrandXResultsReader.hasNextLine()) {
    		String line = BrandXResultsReader.nextLine();

    		if(line.startsWith("Q")) {
    	    	if(r != null) {
    				BrandXResults.add(r);
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
			BrandXResults.add(r);
    		r = null;
    	}
    	
    	List<Result> otpResults = new ArrayList<Result>();
    	while (otpResultsReader.hasNextLine()) {
    		String line = otpResultsReader.nextLine();

    		if(line.startsWith("Q")) {
    	    	if(r != null) {
    				otpResults.add(r);
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
			otpResults.add(r);
    		r = null;
    	}

    	// ===============================================================
    	
    	for(int i = 0; i < otpResults.size(); i++) {
    		Result BrandXResult = BrandXResults.get(i);
    		Result otpResult = otpResults.get(i);
    		Query query = otpResult.query;
    		
    		int bestOTPTransitTime = Integer.MAX_VALUE;
    		double bestOTPWalkDistance = Double.MAX_VALUE;
    		int bestOTPTransfers = Integer.MAX_VALUE;
    		ItinerarySummary usFirst = null;

    		if(otpResult.itineraries.isEmpty()) {
    			this.resultSummary
    				[optimizationDim.valueOf(query.optimizeFlag).ordinal()]
    				[metricsDim.noResults.ordinal()]
    				[platformDim.OTP.ordinal()]++;
    		}
    		
    		for(ItinerarySummary us : otpResult.itineraries) {
    			if(usFirst == null)
    				usFirst = us;

    			if(us.routes.split(">").length < bestOTPTransfers)
    				bestOTPTransfers = us.routes.split(">").length;

    			if(us.transitTime < bestOTPTransitTime)
    				bestOTPTransitTime = us.transitTime;

    			if(us.walkDistance < bestOTPWalkDistance)
    				bestOTPWalkDistance = us.walkDistance;
    		}
    		
    		
    		int bestBrandXTransitTime = Integer.MAX_VALUE;
    		double bestBrandXWalkDistance = Double.MAX_VALUE;
    		int bestBrandXTransfers = Integer.MAX_VALUE;
    		ItinerarySummary themFirst = null;
    		
    		if(BrandXResult.itineraries.isEmpty()) {
    			this.resultSummary
    				[optimizationDim.valueOf(query.optimizeFlag).ordinal()]
    				[metricsDim.noResults.ordinal()]
    				[platformDim.BRANDX.ordinal()]++;
    		}
    		
    		for(ItinerarySummary them : BrandXResult.itineraries) {
    			if(themFirst == null)
    				themFirst = them;
    			
    			if(them.routes.split(">").length < bestBrandXTransfers)
    				bestBrandXTransfers = them.routes.split(">").length;

    			if(them.transitTime < bestBrandXTransitTime)
    				bestBrandXTransitTime = them.transitTime;

    			if(them.walkDistance < bestBrandXWalkDistance)
    				bestBrandXWalkDistance = them.walkDistance;

    			// is our first result in the BrandX results? If so, that's a "match"
				if(usFirst.routes.equals(them.routes)) {
	    			this.resultSummary
						[optimizationDim.valueOf(query.optimizeFlag).ordinal()]
						[metricsDim.match.ordinal()]
						[platformDim.OTP.ordinal()]++;
					break;
				}
			}

        	if(bestOTPTransfers < bestBrandXTransfers) 
    			this.resultSummary
					[optimizationDim.valueOf(query.optimizeFlag).ordinal()]
					[metricsDim.X.ordinal()]
					[platformDim.OTP.ordinal()]++;
        	else
    			this.resultSummary
					[optimizationDim.valueOf(query.optimizeFlag).ordinal()]
					[metricsDim.X.ordinal()]
					[platformDim.BRANDX.ordinal()]++;

        	if(bestOTPTransitTime < bestBrandXTransitTime) 
    			this.resultSummary
					[optimizationDim.valueOf(query.optimizeFlag).ordinal()]
					[metricsDim.T.ordinal()]
					[platformDim.OTP.ordinal()]++;
        	else
    			this.resultSummary
					[optimizationDim.valueOf(query.optimizeFlag).ordinal()]
					[metricsDim.T.ordinal()]
					[platformDim.BRANDX.ordinal()]++;

        	if(bestOTPWalkDistance < bestBrandXWalkDistance)
    			this.resultSummary
					[optimizationDim.valueOf(query.optimizeFlag).ordinal()]
					[metricsDim.W.ordinal()]
					[platformDim.OTP.ordinal()]++;
        	else
    			this.resultSummary
					[optimizationDim.valueOf(query.optimizeFlag).ordinal()]
					[metricsDim.W.ordinal()]
					[platformDim.BRANDX.ordinal()]++;
    	}

    	// ===============================================================

    	boolean overallResult = true;
    	
    	System.out.println("\n\nTOTAL RUNS: " + otpResults.size());
  
    	for(int o = 0; o < optimizationDim.values().length; o++) {
    		String header = "\nOPTIMIZATION: " + String.format("%-30s", optimizationDimLabels[o]) + "                     OTP                BRAND X";
        	System.out.println(header);

        	for(int m = 0; m < metricsDim.values().length; m++) {
        		int total = 
        				this.resultSummary
        				[o]
        				[m]
        				[platformDim.OTP.ordinal()] + 
        				this.resultSummary
        				[o]
        				[m]
        				[platformDim.BRANDX.ordinal()];
        		
            	System.out.print(String.format("%-30s", metricsDimLabels[m]) + 
            		"                                   " + 
            		String.format("%-3d (%-3.0f%%)", 
            			this.resultSummary
        				[o]
        				[m]
        				[platformDim.OTP.ordinal()], 
        				((float)this.resultSummary
        				[o]
        				[m]
        				[platformDim.OTP.ordinal()]/total)*100)
        			+ "         " + 
            		String.format("%-3d (%-3.0f%%)", 
            			this.resultSummary
        				[o]
        				[m]
        				[platformDim.BRANDX.ordinal()], 
        				((float)this.resultSummary
        				[o]
        				[m]
        				[platformDim.BRANDX.ordinal()]/total)*100)
            	);

            	// if this is the metric for the optmization--e.g. it's the WALKING results for the optimization WALKING
            	if(optimizationDimLabels[o].equals(metricsDimLabels[m])) {
            		float ourPercentage = (float)(this.resultSummary[o][m][platformDim.OTP.ordinal()] / (float)total) * 100;
            		if(ourPercentage < 80) {
            			overallResult = false;
                		System.out.println(" [FAIL]");
            		} else {
                		System.out.println(" [PASS]");
            		}
            	} else {
            		System.out.print("\n");
            	}
        	}    
       	}

    	BrandXResultsReader.close();
    	otpResultsReader.close();
    	
    	assertTrue(overallResult);
    }

    private class Query {
    	
    	public long time;
    	
    	public boolean accessible;

    	public String origin;
    	
    	public String destination;
    	
    	public String optimizeFlag;
    	
    	public Query(String line) throws Exception {
    		String parts[] = line.split(" ");
    		
    		if(parts.length != 6 && parts[0].equals("Q"))
    			throw new Exception("Nope.");

    		accessible = parts[1].trim().equals("Y");
    		time = Long.parseLong(parts[2].trim());
    		origin = parts[3].trim();
    		destination = parts[4].trim();
    		optimizeFlag = parts[5].trim();
    	}

        @Override
        public boolean equals(Object o) {
        	return this.hashCode() == o.hashCode();
        }

        @Override
        public int hashCode() {
            return (int)(time * 31) * origin.hashCode() 
            		* destination.hashCode() + 
            		optimizeFlag.hashCode() + 
            		(accessible ? 3 : 0);
        }
    }
    
    private class Result {

        public Query query;

        List<ItinerarySummary> itineraries = new ArrayList<ItinerarySummary>();

    }
   
    private class ItinerarySummary {
    
    	public Integer itineraryNumber;
    	
    	public Double walkDistance;
    	
    	public Integer transitTime;
    	
    	public String routes = "";

    	public ItinerarySummary(String line) throws Exception {
    		String parts[] = line.split(" ");
    		
    		if(parts.length < 4 && parts[0].equals("S"))
    			throw new Exception("Nope.");
    		
    		itineraryNumber = Integer.parseInt(parts[1].trim());
    		walkDistance = Double.parseDouble(parts[2].trim());
    		transitTime = Integer.parseInt(parts[3].trim());

    		// some results have no routes
    		if(parts.length > 4)
    			routes = parts[4].trim();
    	}
    }
}
