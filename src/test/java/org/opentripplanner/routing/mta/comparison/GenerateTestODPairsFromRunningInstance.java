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
 
//    private static final String OTP_GRAPH_INFO_URL = "http://localhost:8080/otp/routers/default?apikey=z6odKJINMNQww8M1zWfFoTMCUPcfbKnts";

    private static final String OTP_STOPS_URL = "http://otp-mta-demo.camsys-apps.com/otp/routers/default/index/stops?apikey=z6odKJINMNQww8M1zWfFoTMCUPcfbKnts";

    private static final String OTP_GRAPH_INFO_URL = "http://otp-mta-demo.camsys-apps.com/otp/routers/default?apikey=z6odKJINMNQww8M1zWfFoTMCUPcfbKnts";

    private static final int PAIRS_TO_GENERATE = 100;

    private static final String[] optimizations = new String[] { "W", "X", "T" };
    
    protected static Graph graph;

    @SuppressWarnings("unchecked")
//	@Test
    public void run() throws IOException, URISyntaxException {

    	CloseableHttpClient httpClient = HttpClients.createDefault();
    	
    	URIBuilder builder = new URIBuilder(OTP_STOPS_URL);
    	HttpGet get = new HttpGet(builder.build());
    	
    	CloseableHttpResponse response = httpClient.execute(get);
    	String responseString = EntityUtils.toString(response.getEntity());

    	ArrayList<Map> stops = (ArrayList<Map>) new JSONDeserializer().deserialize(responseString);             

    	builder = new URIBuilder(OTP_GRAPH_INFO_URL);
    	get = new HttpGet(builder.build());
    	
    	CloseableHttpResponse response2 = httpClient.execute(get);
    	String responseString2 = EntityUtils.toString(response2.getEntity());

    	HashMap<String, Object> graphInfo =  (HashMap<String, Object>) new JSONDeserializer().deserialize(responseString2);             

//    	DateTime startTime = new DateTime((Integer)graphInfo.get("transitServiceStarts") * 1000L);    	
    	DateTime startTime = new DateTime();
    	DateTime endTime = new DateTime((Integer)graphInfo.get("transitServiceEnds") * 1000L);
    	
    	FileWriter testOdPairs = new FileWriter(PAIRS_TXT);

    	int i = PAIRS_TO_GENERATE;
    	while(i-- > 0) {
        	int p1 = (int) (Math.random() * stops.size());
    		int p2 = (int) (Math.random() * stops.size());
        		
    		Map s1 = stops.get(p1);
    		Map s2 = stops.get(p2);
    		
//    		Long searchPeriod = endTime.getMillis() - startTime.getMillis();     		
    		Long searchPeriod = (long) (2 * 30 * 24 * 60 * 60 * 1000); // 3 months     		

    		DateTime randomTime = new DateTime(startTime.getMillis() + (long)(searchPeriod * Math.random()));
    		
    		testOdPairs.write("Q " + ((Math.random() < .5) ? "Y " : "N ") + 
    				randomTime.getMillis() + " " + 
    				s1.get("lat") + "," + s1.get("lon") + " " + 
    				s2.get("lat") + "," + s2.get("lon") + " " +
    				optimizations[(int)Math.round(Math.random() * 2)] + 
    				"\n");

    		System.out.print(".");
    	}
    
    	testOdPairs.close();
 
    }

}
