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

import com.csvreader.CsvReader;
import org.apache.commons.io.IOUtils;
import org.onebusaway.csv_entities.CsvInputSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represent a feed id in a GTFS feed.
 */
public class GtfsFeedId {
    private static final Logger LOG = LoggerFactory.getLogger(GtfsFeedId.class);
    /**
     * A counter that will increase for each created feed id.
     */
    private static int FEED_ID_COUNTER = 1;

    /**
     * The id for the feed
     */
    private final String id;
    
    /**
     * The version for the feed
     */
    private final String version;

    /**
     * Constructs a new feed id.
     *
     * If the passed id is null or an empty string a unique feed id will be generated.
     *
     * @param id The feed id
     */
    private GtfsFeedId(String id, String version) {
        this.id = id;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public static class Builder {

        private String id;
        private String version;

        public Builder id(String id, String version) {
            this.id = id;
            this.version = version;
            return this;
        }

        /**
         * Extracts a feed_id from the passed source for a GTFS feed.
         * <p/>
         * This will try to fetch the experimental feed_id field from the feed_info.txt file.
         * <p/>
         * If the feed does not contain a feed_info.txt or a feed_id field a default GtfsFeedId will be created.
         *
         * @param source the input source
         * @return A GtfsFeedId
         * @throws IOException
         * @see <a href="http://developer.trimet.org/gtfs_ext.shtml">http://developer.trimet.org/gtfs_ext.shtml</a>
         */
        public Builder fromGtfsFeed(CsvInputSource source) {
            try {
                if (source.hasResource("feed_info.txt")) {
                    InputStream feedInfoInputStream = source.getResource("feed_info.txt");
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(feedInfoInputStream, writer, "UTF-8");
                    String feedInfoCsv = writer.toString();
                    CsvReader result = CsvReader.parse(feedInfoCsv);
                    result.readHeaders();
                    result.readRecord();
                    this.id = result.get("feed_id");
                    this.version = result.get("feed_version");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        /**
         * Cleans the id before it is set. This method ensures that the id is a valid id.
         *
         * @param id The feed id
         * @return The cleaned id.
         */
        protected String cleanId(String id) {
            if (id == null || id.trim().length() == 0) {
                return id;
            }
            // 1. Underscore is used as an separator by OBA.
            // 2. Colon is used as an separator in OTP.
            return id.replaceAll("_", "")
                    .replaceAll(":", "");
        }
        
        protected String cleanVersion(String version) {
            if (version == null || version.trim().length() == 0) {
                return version;
            }
            String result = version;
            int prefixIndex = version.indexOf("-");
            if (prefixIndex > 0) {
                String prefix = version.substring(0, prefixIndex);
                if (prefix.equals("Merged")) {
                    int index = version.lastIndexOf("-");
                    if (index > 0) {
                        result = version.substring(index+1);
                    }
                } else {
                    result = prefix;
                }
            }
            return result;
        }

        /**
         * Creates a new GtfsFeedId.
         *
         * @return A GtfsFeedId
         */
        public GtfsFeedId build() {
            id = cleanId(id);
            if (id == null || id.trim().length() == 0) {
                id = String.valueOf(FEED_ID_COUNTER);
            }
            FEED_ID_COUNTER++;
            return new GtfsFeedId(id, cleanVersion(version));
        }
    }
}
