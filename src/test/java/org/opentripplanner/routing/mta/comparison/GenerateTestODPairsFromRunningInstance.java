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

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.joda.time.DateTime;
import org.junit.Test;
import org.opentripplanner.routing.graph.Graph;

import flexjson.JSONDeserializer;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GenerateTestODPairsFromRunningInstance {
	
    private static final String PAIRS_TXT = "src/test/resources/mta/test_od_pairs.txt";

//    private static final String OTP_STOPS_URL = "http://localhost:8080/otp/routers/default/index/stops?apikey=z6odKJINMNQww8M1zWfFoTMCUPcfbKnts";
 
    private static final String OTP_STOPS_URL = "http://otp-mta-demo.camsys-apps.com/otp/routers/default/index/stops?apikey=z6odKJINMNQww8M1zWfFoTMCUPcfbKnt";

    private static final int PAIRS_TO_GENERATE = 100;

    private static final String[] optimizations = new String[] { "W", "X", "T" };
    
    protected static Graph graph;

//	@Test
    @SuppressWarnings("unchecked")
    public void run() throws IOException, URISyntaxException {

    	CloseableHttpClient httpClient = HttpClients.createDefault();    	
    	URIBuilder builder = new URIBuilder(OTP_STOPS_URL);
    	HttpGet get = new HttpGet(builder.build());
    	
    	CloseableHttpResponse response = httpClient.execute(get);
    	String responseString = EntityUtils.toString(response.getEntity());

    	ArrayList<Map> stops = (ArrayList<Map>) new JSONDeserializer().deserialize(responseString);             

    	if(stops == null) {
    		System.out.println("Invalid server response when requesting all stops: " + responseString);
    		return;
    	}
    	
    	System.out.print("Generating stop pairs ...");
    	
    	DateTime startTime = new DateTime();
		Long searchPeriod = (long) (2 * 30 * 24 * 60 * 60 * 1000); // 3 months     		
    	
    	FileWriter testOdPairs = new FileWriter(PAIRS_TXT);

    	int i = PAIRS_TO_GENERATE;
    	while(i-- > 0) {
        	int p1 = (int) (Math.random() * stops.size());
    		int p2 = (int) (Math.random() * stops.size());
        		
    		Map s1 = stops.get(p1);
    		Map s2 = stops.get(p2);
    		
    		DateTime randomTime = new DateTime(startTime.getMillis() + (long)(searchPeriod * Math.random()));
    		
    		testOdPairs.write("Q " + ((Math.random() < .5) ? "Y " : "N ") + 
    				randomTime.getMillis() + " " + 
    				s1.get("lat") + "," + s1.get("lon") + " " + 
    				s2.get("lat") + "," + s2.get("lon") + " " +
    				optimizations[(int)Math.round(Math.random() * 2)] + 
    				"\n");

    		System.out.print(".");
    	}

    	System.out.println("done.");

    	testOdPairs.close();
    }
}
