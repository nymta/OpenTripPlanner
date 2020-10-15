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
package org.opentripplanner.routing.mta;

import org.junit.Test;
import java.util.*;
import java.io.*;

public class CompareODResults {
	
    private static final String ATIS_RESULTS_TXT = "src/test/resources/mta/test_atis_results.txt";

    private static final String OTP_RESULTS_TXT = "src/test/resources/mta/test_otp_results.txt";

    private class Query {
    	
    	public long time;
    	
    	public boolean accessible;

    	public String origin;
    	
    	public String destination;
    	
    	public Query(String line) throws Exception {
    		String parts[] = line.split(" ");
    		
    		if(parts.length != 5 && parts[0].equals("Q"))
    			throw new Exception("Nope.");

    		accessible = parts[1].trim().equals("Y");
    		time = Long.parseLong(parts[2].trim());
    		origin = parts[3].trim();
    		destination = parts[4].trim();
    	}

        @Override
        public boolean equals(Object o) {
        	return this.hashCode() == o.hashCode();
        }

        @Override
        public int hashCode() {
            return (int)(time * 31) * origin.hashCode() 
            		* destination.hashCode() + (accessible ? 3 : 0);
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
    	
    	public String routes;

    	public ItinerarySummary(String line) throws Exception {
    		String parts[] = line.split(" ");
    		
    		if(parts.length != 5 && parts[0].equals("S"))
    			throw new Exception("Nope.");
    		
    		itineraryNumber = Integer.parseInt(parts[1].trim());
    		walkDistance = Double.parseDouble(parts[2].trim());
    		transitTime = Integer.parseInt(parts[3].trim());
    		routes = parts[4].trim();
    	}
    }
    

    @SuppressWarnings("unchecked")
    @Test
    public void run() throws IOException, Exception {

    	File atisResultsFile = new File(ATIS_RESULTS_TXT);
    	File otpResultsFile = new File(OTP_RESULTS_TXT);

    	Scanner atisResultsReader = new Scanner(atisResultsFile);
    	Scanner otpResultsReader = new Scanner(otpResultsFile);

		Query q = null;
		Result r = null;
		
    	List<Result> atisResults = new ArrayList<Result>();
    	while (atisResultsReader.hasNextLine()) {
    		String line = atisResultsReader.nextLine();

    		if(line.startsWith("Q")) {
    	    	if(r != null) {
    				atisResults.add(r);
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
			atisResults.add(r);
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

    	
    	
    	int matches = 0;
    	for(int i = 0; i < otpResults.size(); i++) {
    		Result atisResult = atisResults.get(i);
    		Result otpResult = otpResults.get(i);
    		
			ItinerarySummary ourFirst = otpResult.itineraries.iterator().next();
    		for(ItinerarySummary them : atisResult.itineraries) {
    			System.out.println(ourFirst.routes + " ? " + them.routes);
				if(ourFirst.routes.equals(them.routes)) {
					matches++;
					break;
				}
			}
       	}
    	
    	System.out.println("TOTAL RUNS: " + otpResults.size() + "; MATCHES " + matches);
    	
    	atisResultsReader.close();
    	otpResultsReader.close();
    }
}
