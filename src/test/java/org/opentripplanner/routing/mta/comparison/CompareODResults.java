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
	
    private final String[] metricsDimLabels = new String[] { "WALKING", "TRANSFERS", "TIME", "PRODUCED A RESULT", "OTP RESULT IS IN BRAND X" };

    private enum metricsDim { W, X, T, hasResults, match };

	private enum platformDim { OTP, BRANDX };
		
	private int[] totalByOptimization = new int[3];
	
	private int[][][] resultSummary = new int[3][5][2];
	
//    @Test
    public void run() throws IOException, Exception {

    	File brandXResultsFile = new File(BRANDX_RESULTS_TXT);
    	File otpResultsFile = new File(OTP_RESULTS_TXT);

    	Scanner brandXResultsReader = new Scanner(brandXResultsFile);
    	Scanner otpResultsReader = new Scanner(otpResultsFile);

		Query q = null;
		Result r = null;
		
    	// ==========================LOAD RESULTS=====================================

    	List<Result> brandXResults = new ArrayList<Result>();
    	while (brandXResultsReader.hasNextLine()) {
    		String line = brandXResultsReader.nextLine();

    		if(line.startsWith("Q")) {
    	    	if(r != null) {
    				brandXResults.add(r);
    	    		r = null;
    	    	}
    			
    			q = new Query(line);
    			r = new Result();    			
    			r.query = q;
    		}

    		if(line.startsWith("S")) {
    			ItinerarySummary s = new ItinerarySummary(line);
    			s.platform = platformDim.BRANDX;
    			r.itineraries.add(s);
    		}
    	}
    	if(r != null) {
			brandXResults.add(r);
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
    			s.platform = platformDim.OTP;
    			r.itineraries.add(s);
    		}
    	}
    	if(r != null) {
			otpResults.add(r);
    		r = null;
    	}

    	// ==========================COMPARE RESULTS=====================================
    	
    	for(int i = 0; i < otpResults.size(); i++) {
    		Result brandXResult = brandXResults.get(i);
    		Result otpResult = otpResults.get(i);

    		Query query = otpResult.query;
    		
    		// add all system's results to an array to sort based on metric and score
    		List<ItinerarySummary> sortedResults = new ArrayList<ItinerarySummary>();
    		sortedResults.addAll(otpResult.itineraries);
    		sortedResults.addAll(brandXResult.itineraries);

    		// both systems produced nothing; skip
    		if(sortedResults.isEmpty())
    			continue;
    		
    		// go through each metric for each result and see who won
    		for(int o = 0; o < optimizationDim.values().length; o++) {
    			// ignore queries not made with this optimization
    			if(optimizationDim.valueOf(query.optimizeFlag).ordinal() != o)
    				continue;
    			
    			totalByOptimization[o]++;
    			
    			for(int m = 0; m < metricsDim.values().length; m++) {
    				switch(metricsDim.values()[m]) {
	    				case T:
	    					sortedResults.sort(new Comparator<ItinerarySummary>() {
	    						@Override
	    						public int compare(ItinerarySummary o1, ItinerarySummary o2) {
	    							if(o1.transitTime == o2.transitTime) {
	    								return 0;
	    							} else if(o1.transitTime > o2.transitTime) {
	    								return 1;
	    							} else {
	    								return -1;
	    							}
	    						}
	    					});
	
	    					platformDim winningPlatform = sortedResults.get(0).platform;
	    					this.resultSummary
	    						[o]
	    						[m]
	    						[winningPlatform.ordinal()]++;
	
	    					break;
	    				case W:
	    					sortedResults.sort(new Comparator<ItinerarySummary>() {
	    						@Override
	    						public int compare(ItinerarySummary o1, ItinerarySummary o2) {
	    							if(o1.walkDistance == o2.walkDistance) {
	    								return 0;
	    							} else if(o1.walkDistance > o2.walkDistance) {
	    								return 1;
	    							} else {
	    								return -1;
	    							}
	    						}
	    					});
	
	    					platformDim winningPlatform2 = sortedResults.get(0).platform;
	    					this.resultSummary
	    						[o]
	    						[m]
	    						[winningPlatform2.ordinal()]++;
	
	    					break;
	    				case X:
	    					sortedResults.sort(new Comparator<ItinerarySummary>() {
	    						@Override
	    						public int compare(ItinerarySummary o1, ItinerarySummary o2) {
	    							int o1x = o1.routes.split(">").length;
	    							int o2x = o1.routes.split(">").length;
	
	    							if(o1x == o2x) {
	    								return 0;
	    							} else if(o1x > o2x) {
	    								return 1;
	    							} else {
	    								return -1;
	    							}
	    						}
	    					});
	
	    					platformDim winningPlatform3 = sortedResults.get(0).platform;
	    					this.resultSummary
	    						[o]
	    						[m]
	    						[winningPlatform3.ordinal()]++;
	
	    					break;
	    				case match:
	    					ItinerarySummary ourTopResult = otpResult.itineraries.get(0);
	    					for(int z = 0; i < brandXResult.itineraries.size(); z++) {
	    						ItinerarySummary theirResult = brandXResult.itineraries.get(z);
	    						
	    						// give this to both since it's a comparison between the two
	    						if(ourTopResult.routes.equals(theirResult.routes)) {
	    	    					this.resultSummary
	    	    						[o]
	    	    						[m]
	    	    						[platformDim.OTP.ordinal()]++;
	    						}
	    						
	    						if(ourTopResult.routes.equals(theirResult.routes)) {
	    	    					this.resultSummary
	    	    						[o]
	    	    						[m]
	    	    						[platformDim.BRANDX.ordinal()]++;
	    						}

	    						break;
	    					}
	    					
	    					break;
	    					
	    				case hasResults:
	    					if(!otpResult.itineraries.isEmpty()) {
	    						this.resultSummary
	    						[o]
	    						[m]
	    						[platformDim.OTP.ordinal()]++;
	    					}
	
	    					if(!brandXResult.itineraries.isEmpty()) {
	    						this.resultSummary
	    						[o]
	    						[m]
	    						[platformDim.BRANDX.ordinal()]++;
	    					}
	    					break;
	
	    					// will never get here
	    				default:
	    					break;    				
	    				}
	    			}
    			}
    	}

    	// ==========================PRINT RESULTS=====================================

    	boolean overallResult = true;
    	
    	System.out.println("\n\nTOTAL RUNS: " + otpResults.size());
  
    	for(int o = 0; o < optimizationDim.values().length; o++) {
    		String header = "\nOPTIMIZATION: " + String.format("%-30s", optimizationDimLabels[o]) + "                     OTP                BRAND X";
        	System.out.println(header);

        	for(int m = 0; m < metricsDim.values().length; m++) {
        		int total = totalByOptimization[o];
        		
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

            	// if this is the metric for the optimization--e.g. it's the WALKING results for the optimization WALKING, make 
            	// it one of the things that makes the whole test pass/fail
            	if(optimizationDimLabels[o].equals(metricsDimLabels[m])) {
            		float ourPercentage = 
            				(float)(this.resultSummary[o][m][platformDim.OTP.ordinal()] 
            				/ (float)totalByOptimization[o]) * 100;

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

    	brandXResultsReader.close();
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
    	
    	public platformDim platform;

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
