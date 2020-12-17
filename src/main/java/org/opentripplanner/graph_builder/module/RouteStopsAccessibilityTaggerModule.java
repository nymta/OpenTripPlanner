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

package org.opentripplanner.graph_builder.module;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteStopsAccessibilityTaggerModule implements GraphBuilderModule {

	public enum ADAFlag {
		UNKNOWN,
		NOT_ACCESSIBLE,
		ACCESSIBLE,
		PARTLY_ACCESSIBLE,
	}
	
	public class RouteStopTag implements Serializable {
		
		private static final long serialVersionUID = 3915566835621828907L;

		public ADAFlag accessibilityFlag;
			
		public String accessibilityNotes;

		public String routeId;
	}
	
    private static final Logger LOG = LoggerFactory.getLogger(RouteStopsAccessibilityTaggerModule.class);
	
	public List<File> csvFiles = Collections.emptyList();
	
    /** An set of ids which identifies what stages this graph builder provides (i.e. streets, elevation, transit) */
    public List<String> provides() {
        return Collections.emptyList();
    }

    /** A list of ids of stages which must be provided before this stage */
    public List<String> getPrerequisites() {
        return Arrays.asList("transit");
    }
    
    public RouteStopsAccessibilityTaggerModule(List<File> csvFiles) {
    	this.csvFiles = csvFiles;
    }
    
    private ArrayList<HashMap<String, String>> readCSV(File inputFile) throws FileNotFoundException {
		ArrayList<String> headers = new ArrayList<String>();    	
		ArrayList<HashMap<String, String>> rows = new ArrayList<>();
		
		Scanner scanner = new Scanner(inputFile);
		
		while(scanner.hasNextLine()) {
        	String data = scanner.nextLine();
        	
        	if(headers.isEmpty()) {
        		headers.addAll(Arrays.asList(data.split(",")));
        		
        		if(!headers.contains("stop_id")) {
	        		LOG.error("File {} does not contain a column stop_id, which is required for indexing. Skipping.", inputFile);
	        		return rows;
        		} 
        		
        		continue;
        	}
        	
        	HashMap<String, String> newLine = new HashMap<>();
        	int i = 0;
        	for(String element : data.split(",")) {
        		newLine.put(headers.get(i), element.trim());
        		i++;
        	}
        	
        	rows.add(newLine);
        }	 

		scanner.close();
		
		return rows;
    }
    
    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
		for(File inputFile : csvFiles) {
	    	try {
	    		ArrayList<HashMap<String, String>> rows = readCSV(inputFile);

	    		int i = 0;
	    		for(HashMap<String, String> row : rows) {
	    			String stopId = row.get("stop_id");

	    			ArrayList<RouteStopTag> tags = graph.routeStopTagsByStopId.get(stopId);
	    			if(tags == null)
	    				tags = new ArrayList<RouteStopTag>();
	    			
	    			RouteStopTag newTag = new RouteStopTag();
	    			newTag.routeId = row.get("route_id");
	    			newTag.accessibilityNotes = row.get("Notes");
	    			
	    			try {
	    				newTag.accessibilityFlag = ADAFlag.values()[Integer.parseInt(row.get("ada")) + 1];
	    			} catch(NumberFormatException e) {
	    	    		LOG.error("ADA value of '{}' in row {} is not valid.", row.get("ada"), i);
	    				newTag.accessibilityFlag = ADAFlag.UNKNOWN;
	    			}
	    			
	    			tags.add(newTag);
	    			
	    			graph.routeStopTagsByStopId.put(stopId, tags);
	    			i++;
	    		}
	    	} catch (Exception e) {
	    		LOG.error("Error processing row in {}", inputFile);
	    		e.printStackTrace();
	    	}
	    }
    }

    @Override
    public void checkInputs() {
    	LOG.info("Found {} CSV files.", csvFiles.size());
    }
}
