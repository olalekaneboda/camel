/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.processor;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.util.EndpointHelper;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.StringHelper;

/**
 * Default {@link AggregationStrategy} used by the {@link ClaimCheckProcessor} EIP.
 * <p/>
 * This strategy supports the following include rules syntax:
 * <ul>
 *     <li>body</li> - to aggregate the message body
 *     <li>headers</li> - to aggregate all the message headers
 *     <li>header:pattern</li> - to aggregate all the message headers that matches the pattern.
 *     The pattern syntax is documented by: {@link EndpointHelper#matchPattern(String, String)}.
 * </ul>
 * You can specify multiple rules separated by comma. For example to include the message body and all headers starting with foo
 * <tt>body,header:foo*</tt>.
 * If the include rule is specified as empty or as wildcard then everything is merged.
 */
public class ClaimCheckAggregationStrategy implements AggregationStrategy {

    private String include;

    public ClaimCheckAggregationStrategy() {
    }

    public String getInclude() {
        return include;
    }

    public void setInclude(String include) {
        this.include = include;
    }

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        if (newExchange == null) {
            return oldExchange;
        }

        if (ObjectHelper.isEmpty(include) || "*".equals(include)) {
            // grab everything if data is empty or wildcard
            return newExchange;
        }

        Iterable it = ObjectHelper.createIterable(include, ",");
        for (Object k : it) {
            String part = k.toString();
            if ("body".equals(part)) {
                oldExchange.getMessage().setBody(newExchange.getMessage().getBody());
            } else if ("headers".equals(part)) {
                oldExchange.getMessage().getHeaders().putAll(newExchange.getMessage().getHeaders());
            } else if (part.startsWith("header:")) {
                // pattern matching for headers, eg header:foo, header:foo*, header:(foo|bar)
                String after = StringHelper.after(part, "header:");
                Iterable i = ObjectHelper.createIterable(after, ",");
                for (Object o : i) {
                    String pattern = o.toString();
                    for (Map.Entry<String, Object> header : newExchange.getMessage().getHeaders().entrySet()) {
                        String key = header.getKey();
                        if (EndpointHelper.matchPattern(key, pattern)) {
                            oldExchange.getMessage().getHeaders().put(key, header.getValue());
                        }
                    }
                }
            }
        }

        return oldExchange;
    }
}
