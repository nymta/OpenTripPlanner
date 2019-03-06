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
package org.opentripplanner.pattern_graph.model;

import org.opentripplanner.pattern_graph.model.RouteStop;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;


public class StopAttribute {

    private Set<String> colors = new HashSet<>(); //Strings representing HEX Colors
    private List<RouteStop> routes = new ArrayList<>();
    private String name;
    private Boolean isTerminal = false;
    private Boolean shuttling = false;

    public Set<String> getColor() {
        return colors;
    }

    public void addColor(String color) {
        colors.add(color);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<RouteStop> getRoutes() {
        return routes;
    }

    public void addRoute(RouteStop route) {
        List<RouteStop> routes = getRoutes();
        for(int i = 0; i < routes.size(); i++){
            if(routes.get(i).getRoute() == route.getRoute() && routes.get(i).getStop() == route.getStop())
                return;
        }
        routes.add(route);
    }

    public Boolean getIsTerminal() {
        return isTerminal;
    }

    public void setIsTerminal(Boolean isTerminal){
        this.isTerminal=isTerminal;
    }

    public Boolean getShuttling() {
        return shuttling;
    }

    public void setShuttling(Boolean shuttling){
        this.shuttling=shuttling;
    }

}
