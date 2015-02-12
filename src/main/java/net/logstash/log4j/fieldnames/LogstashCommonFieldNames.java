/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.logstash.log4j.fieldnames;

import java.util.ArrayList;
import java.util.List;

/**
 * Common field names
 */
public abstract class LogstashCommonFieldNames {
    private String timestamp = "@timestamp";
    private String version = "@version";
    private String message = "message";

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> listCommonNames() {
        List<String> namesList = new ArrayList<>();

        namesList.add(getTimestamp());
        namesList.add(getMessage());
        namesList.add(getVersion());

        return  namesList;
    }
}