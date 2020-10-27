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

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Min;
import java.util.*;

import static org.junit.Assert.assertTrue;

import java.io.*;

public class CompareODResults {
	
    private String DEV_RESULTS_TXT = null; 

    private String BASELINE_RESULTS_TXT = null;

    private final String[] optimizationDimLabels = new String[] { "WALKING", "TRANSFERS", "TIME" };
    
	private enum optimizationDim { W, X, T };
	
    private final String[] metricsDimLabels = new String[] { "WALKING (km)", "TRANSFERS", "TIME (min)", "PRODUCED A RESULT", "BASELINE TOP IN DEV RESULTS**" };

    private enum metricsDim { W, X, T, hasResults, match };

	private enum platformDim { BASELINE, DEV, TIE };

	// dimensions: optimization
	private int[] totalByOptimization = new int[3];

	// dimensions: optimization | metric | metric
	private List<Double>[][][]resultWinMargin = new ArrayList[3][5][3];

	// dimensions: optimization | metric | platform
	private int[][][] resultSummary = new int[3][5][3];
		
	public void setBaselineResultsFile(String f) {
		this.BASELINE_RESULTS_TXT = f;
	}
	
	public void setTestResultsFile(String f) {
		this.DEV_RESULTS_TXT = f;
	}
	
	private void scorePlatform(optimizationDim optimization, metricsDim metric, 
			List<ItinerarySummary> sortedResults, Comparator<ItinerarySummary> ranker) {

		sortedResults.sort(ranker);	    				
		
		if(sortedResults.size() > 0) {
			ItinerarySummary winningResult = sortedResults.get(0);
			platformDim winningPlatform = sortedResults.get(0).platform;	

			// the other platform's first result, equal or not to our top result
			ItinerarySummary otherPlatformFirstResult = null;
			
			// the first result that is less than our result
			ItinerarySummary runnerUpResult = null; 						
			
			for(int i = 1; i < sortedResults.size(); i++) {
				ItinerarySummary p_Result = sortedResults.get(i);
				platformDim p_Platform = sortedResults.get(i).platform;

				if(winningPlatform != p_Platform && otherPlatformFirstResult == null) {
					otherPlatformFirstResult = p_Result;
				}
				
				int compareResult = ranker.compare(winningResult, p_Result);
				if(compareResult < 0 && runnerUpResult == null) {
					runnerUpResult = p_Result;
				}
			} // for sortedResults, looking for runner up

			// other platform has no first result = one winner
			if(otherPlatformFirstResult == null) {
				this.resultSummary
				[optimization.ordinal()]
				[metric.ordinal()]
				[winningPlatform.ordinal()]++;	
			} else {
				// other platform produced a result--is it equal to the winner? If so, tie
				if(ranker.compare(otherPlatformFirstResult, winningResult) == 0) {
					this.resultSummary
					[optimization.ordinal()]
					[metric.ordinal()]
					[platformDim.TIE.ordinal()]++;	
				} else {
				// one winner
					this.resultSummary
					[optimization.ordinal()]
					[metric.ordinal()]
					[winningPlatform.ordinal()]++;						
				}
			}
			
			// if we won and the other platform produced a losing result
			if(otherPlatformFirstResult != null 
					&& ranker.compare(otherPlatformFirstResult, winningResult) != 0) {
				switch(metric) {
				case T:
					this.resultWinMargin
						[optimization.ordinal()]
						[metric.ordinal()]
						[winningResult.platform.ordinal()].add((double)(winningResult.transitTime - otherPlatformFirstResult.transitTime));
					break;
				case W:
					this.resultWinMargin
						[optimization.ordinal()]
						[metric.ordinal()]
						[winningResult.platform.ordinal()].add(winningResult.walkDistance - otherPlatformFirstResult.walkDistance);
					break;
				case X:
					this.resultWinMargin
						[optimization.ordinal()]
						[metric.ordinal()]
						[winningResult.platform.ordinal()].add((double)(winningResult.routes.split(">").length - otherPlatformFirstResult.routes.split(">").length));
					break;
					
				// nothing to do here
				case hasResults:
				case match:
				default:
					break;					
				}					
			}
			
			
		}
	}
	
	private List<Result> loadResults(File resultsFile, platformDim platform) throws Exception {
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
    			s.platform = platform;
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
	
    //@Test
    public void run() throws IOException, Exception {    	
    	File devResultsFile = new File(DEV_RESULTS_TXT);
    	File baselineResultsFile = new File(BASELINE_RESULTS_TXT);

    	List<Result> devResults = loadResults(devResultsFile, platformDim.DEV);
    	List<Result> baselineResults = loadResults(baselineResultsFile, platformDim.BASELINE);

    	// initialize stats storage array
    	for(int i = 0; i < resultWinMargin.length; i++) {
        	for(int z = 0; z < resultWinMargin[i].length; z++)
        		for(int a = 0; a < resultWinMargin[i][z].length; a++)
            		resultWinMargin[i][z][a] = new ArrayList();
    	}
    	
    	
    	// ==========================COMPARE RESULTS=====================================
    	List<Query> noResultQueries = new ArrayList<Query>();

    	for(int i = 0; i < Math.max(baselineResults.size(), devResults.size()); i++) {
    		Result devResult = devResults.get(i);
    		Result baselineResult = baselineResults.get(i);

    		Query query = baselineResult.query;

    		int o = optimizationDim.valueOf(query.optimizeFlag).ordinal();

    		// add all system's itineraries to an array to sort based on metric and score
    		List<ItinerarySummary> sortedResults = new ArrayList<ItinerarySummary>();
    		sortedResults.addAll(baselineResult.itineraries);
    		sortedResults.addAll(devResult.itineraries);

    		// both systems produced nothing; skip?
    		if(sortedResults.isEmpty()) {
    			noResultQueries.add(baselineResult.query);
    			
    			continue;
    		}

    		totalByOptimization[o]++;

    		for(int m = 0; m < metricsDim.values().length; m++) {
    			switch(metricsDim.values()[m]) {
    			case T:
					// time, walk, transfers
    				Comparator<ItinerarySummary> timeRanker = new Comparator<ItinerarySummary>() {
    					@Override
    					public int compare(ItinerarySummary o1, ItinerarySummary o2) {
    						if(o1.transitTime == o2.transitTime) {
    							return 0;
    						} else if(o1.transitTime < o2.transitTime) {
    							return -1;
    						} else {
    							return 1;
    						}
    					}
    				};

    				scorePlatform(optimizationDim.values()[o], 
    						metricsDim.values()[m], sortedResults, timeRanker);

    				break;
    			case W:
					// walk, transit time, transfers
    				Comparator<ItinerarySummary> walkingRanker = new Comparator<ItinerarySummary>() {
    					@Override
    					public int compare(ItinerarySummary o1, ItinerarySummary o2) {
    						if(o1.walkDistance == o2.walkDistance) {
    							return 0;
    						} else if(o1.walkDistance < o2.walkDistance) { 
    							return -1;
    						} else {
    							return 1;
    						}
    					}
    				};

    				scorePlatform(optimizationDim.values()[o], 
    						metricsDim.values()[m], sortedResults, walkingRanker);

    				break;
    			case X:
					// transfers, time, walk distance
    				Comparator<ItinerarySummary> transfersRanker = new Comparator<ItinerarySummary>() {
    					@Override
    					public int compare(ItinerarySummary o1, ItinerarySummary o2) {
    						int o1x = o1.routes.split(">").length;
    						int o2x = o2.routes.split(">").length;

    						if(o1x == o2x) {
    							return 0;
    						} else if(o1x < o2x) {
    							return -1;
    						} else {
    							return 1;
    						}
    					}
    				};

    				scorePlatform(optimizationDim.values()[o], 
    						metricsDim.values()[m], sortedResults, transfersRanker);

    				break;
    				
    			case match:
    				if(baselineResult.itineraries.size() == 0)
    					break;
    				
					ItinerarySummary ourTopResult = baselineResult.itineraries.get(0);

    				for(int z = 0; z < devResult.itineraries.size(); z++) {
    					ItinerarySummary theirResult = devResult.itineraries.get(z);

    					if(ourTopResult.routes.equals(theirResult.routes)) { 
        					this.resultSummary
        					[o]
        					[m]	
        					[platformDim.TIE.ordinal()]++;
    						break;
    					}
    				}

    				break;
    				
    			case hasResults:

    				if(!baselineResult.itineraries.isEmpty() && !devResult.itineraries.isEmpty()) {
    					this.resultSummary
    					[o]
    					[m]
   						[platformDim.TIE.ordinal()]++;
    					
    				} else if(!baselineResult.itineraries.isEmpty()) {
    					this.resultSummary
    					[o]
    					[m]
   						[platformDim.BASELINE.ordinal()]++;
    				} else if(!devResult.itineraries.isEmpty()) {
    					this.resultSummary
    					[o]
    					[m]
    					[platformDim.DEV.ordinal()]++;
    				}

    				break;

    			// will never get here
    			default:
    				break; 
    			} // end switch
    		} // for each metric
    	}

    	// ==========================PRINT RESULTS=====================================

    	boolean overallResult = true;
    	    	
    	System.out.println("\n\nTOTAL RESULTS: " + baselineResults.size() + "\n\n");
  
    	for(int o = 0; o < optimizationDim.values().length; o++) {
    		String header = "OPTIMIZATION: " + String.format("%-10s (n=%3d)", optimizationDimLabels[o], totalByOptimization[o]) + 
    				"         BASELINE           TIE           TEST      BASELINE SUCCESS MARGIN STATS*          TEST SUCCESS MARGIN STATS*";
        	System.out.println(header);
        	System.out.println(header.replaceAll("[^\\s]", "-"));
        	
        	for(int m = 0; m < metricsDim.values().length; m++) {
        		int total = totalByOptimization[o];
        		
        		double[] baselineValuesAsPrimitive = new double[this.resultWinMargin[o][m][platformDim.BASELINE.ordinal()].size()];
        		for(int ii = 0; ii < this.resultWinMargin[o][m][platformDim.BASELINE.ordinal()].size(); ii++) {
        			baselineValuesAsPrimitive[ii] = this.resultWinMargin[o][m][platformDim.BASELINE.ordinal()].get(ii);   
        		}
        		
        		double[] devValuesAsPrimitive = new double[this.resultWinMargin[o][m][platformDim.DEV.ordinal()].size()];
        		for(int ii = 0; ii < this.resultWinMargin[o][m][platformDim.DEV.ordinal()].size(); ii++) {
        			devValuesAsPrimitive[ii] = this.resultWinMargin[o][m][platformDim.DEV.ordinal()].get(ii);   
        		}

        		Mean meanStat = new Mean();
        		Max maxStat = new Max();        		  
        		Min minStat = new Min();        		  
        		
            	System.out.print(String.format("%-30s", metricsDimLabels[m]) + 
            		"           " + 
            		String.format("%-3d (%-3.0f%%)", 
            			this.resultSummary
        				[o]
        				[m]
        				[platformDim.BASELINE.ordinal()], 
        				((float)this.resultSummary
        				[o]
        				[m]
        				[platformDim.BASELINE.ordinal()]/total)*100)
        			+ "    " + 
            		String.format("%-3d (%-3.0f%%)", 
                			this.resultSummary
            				[o]
            				[m]
            				[platformDim.TIE.ordinal()], 
            				((float)this.resultSummary
            				[o]
            				[m]
            				[platformDim.TIE.ordinal()]/total)*100)
        			+ "    " + 
            		String.format("%-3d (%-3.0f%%)", 
            			this.resultSummary
        				[o]
        				[m]
        				[platformDim.DEV.ordinal()], 
        				((float)this.resultSummary
        				[o]
        				[m]
        				[platformDim.DEV.ordinal()]/total)*100)
            		+ "   " + 
        			(m != metricsDim.match.ordinal() && m != metricsDim.hasResults.ordinal() 
        				? String.format(" M=%-6.2f n=%3d,[%6.2f,%-6.2f]", meanStat.evaluate(baselineValuesAsPrimitive), 
        						this.resultWinMargin[o][m][platformDim.BASELINE.ordinal()].size(),
        						minStat.evaluate(baselineValuesAsPrimitive), 
        						maxStat.evaluate(baselineValuesAsPrimitive)) + "    " 
        						: "                                   ")
            		+ 
        			(m != metricsDim.match.ordinal() && m != metricsDim.hasResults.ordinal() 
        				? String.format(" M=%-6.2f n=%3d,[%6.2f,%-6.2f]", meanStat.evaluate(devValuesAsPrimitive), 
        						this.resultWinMargin[o][m][platformDim.DEV.ordinal()].size(),
        						minStat.evaluate(devValuesAsPrimitive), 
        						maxStat.evaluate(devValuesAsPrimitive)) + "    " 
        						: "                                   ")

            	);

            	
            	// if this is the metric for the optimization--e.g. it's the WALKING results for the optimization WALKING, make 
            	// it one of the things that makes the whole test pass/fail
        		float ourPercentage = 
        				((float)((this.resultSummary[o][m][platformDim.DEV.ordinal()] + 
        						this.resultSummary[o][m][platformDim.TIE.ordinal()])
        				/ (float)totalByOptimization[o])) * 100;

        		// for each optimization, require our result to be the winner 80% of the time
        		if(metricsDimLabels[m].startsWith(optimizationDimLabels[o])) {
            		if(ourPercentage <= 80) {
            			overallResult = false;
                		System.out.println(" [FAIL; have " + String.format("%.0f",  ourPercentage) + "% need 80%+]");
            		} else {
                		System.out.println(" [PASS with " + String.format("%.0f",  ourPercentage) + "%]");
            		}
            	} else {
            		// for the other two metrics (has results and matches), require 100% and 80%+ respectively
            		if(m == metricsDim.hasResults.ordinal()) {
                		if(ourPercentage <= 95) {
                			overallResult = false;
                    		System.out.println(" [FAIL; have " + String.format("%.0f",  ourPercentage) + "% need 95%+]");
                		} else {
                    		System.out.println(" [PASS with " + String.format("%.0f",  ourPercentage) + "%]");
                		}
            		} else if(m == metricsDim.match.ordinal()) {
                		if(ourPercentage <= 65) {
                			overallResult = false;
                    		System.out.println(" [FAIL; have " + String.format("%.0f",  ourPercentage) + "% need 65%+]");
                		} else {
                    		System.out.println(" [PASS with " + String.format("%.0f",  ourPercentage) + "%]");
                		}
            		} else {
            			System.out.println("");
            		}
            	}
        	}    
        	
        	System.out.println("");
       	}
    	
    	System.out.println("* success margin stats are computed against the opposing platform's top result, if it produces one. Ties are not included.\n"
    			+ "** 'no result' matches are included.\n");
    	
    	System.out.println("NO RESULT QUERIES (ACROSS BOTH SYSTEMS):");
    	for(Query q : noResultQueries) {
    		System.out.println(q);
    	}
    	
    	System.out.println("");
    	System.out.println("");

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
        
        public String toString() { // TODO: make into BASELINE URLs
        	return origin + " -> " + destination;
        }
    }
    
    private class Result {

        public Query query;

        List<ItinerarySummary> itineraries = new ArrayList<ItinerarySummary>();

    }
   
    private class ItinerarySummary {
    
    	public int itineraryNumber;
    	
    	public double walkDistance;
    	
    	public int transitTime;
    	
    	public String routes = "";
    	
    	public platformDim platform;

    	public String toString() {
    		return platform.toString() + "" + itineraryNumber + ": Walk=" + walkDistance + ", transit=" + transitTime + ", Routes = " + routes;
    	}
    	
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
