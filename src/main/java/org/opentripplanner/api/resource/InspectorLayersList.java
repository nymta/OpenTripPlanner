/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package org.opentripplanner.api.resource;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opentripplanner.inspector.TileRenderer;

/**
 *
 * @author mabu
 */
public class InspectorLayersList {
    
    public List<InspectorLayer> layers;

    InspectorLayersList(Map<String, TileRenderer> renderers) {
        layers = new ArrayList<>(renderers.size());
        for (Map.Entry<String, TileRenderer> layerInfo : renderers.entrySet()) {
            String layer_key = layerInfo.getKey();
            TileRenderer layer = layerInfo.getValue();
            layers.add(new InspectorLayer(layer_key, layer.getName()));
        }
    }

    private static class InspectorLayer {
        
        @JsonSerialize
        String key;
        @JsonSerialize
        String name;

        private InspectorLayer(String layer_key, String layer_name) {
            this.key = layer_key;
            this.name = layer_name;
        }
    }

   
}
