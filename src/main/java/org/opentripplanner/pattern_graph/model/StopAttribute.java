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
import java.util.HashSet;
import java.util.Set;

public class StopAttribute {

    private Set<String> colors = new HashSet<>(); //Strings representing HEX Colors
    private Set<String> routes = new HashSet<>(); //Strings representing route names
    private String name;
    private Boolean isTerminal = false;

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

    public Set<String> getRoutes() {
        return routes;
    }

    public void addRoute(String route) {
        routes.add(route);
    }

    public Boolean getIsTerminal() {
        return isTerminal;
    }

    public void setIsTerminal(Boolean isTerminal){
        this.isTerminal =isTerminal;
    }

}
