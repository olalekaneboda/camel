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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * If you have configured both include and exclude then exclude take precedence over include.
 */
public class ClaimCheckAggregationStrategy implements AggregationStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(ClaimCheckAggregationStrategy.class);
    private String include;
    private String exclude;

    public ClaimCheckAggregationStrategy() {
    }

    public String getInclude() {
        return include;
    }

    public void setInclude(String include) {
        this.include = include;
    }

    public String getExclude() {
        return exclude;
    }

    public void setExclude(String exclude) {
        this.exclude = exclude;
    }

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        if (newExchange == null) {
            return oldExchange;
        }

        if (ObjectHelper.isEmpty(exclude) && (ObjectHelper.isEmpty(include) || "*".equals(include))) {
            // grab everything if include is empty or wildcard (and exclude is not in use)
            return newExchange;
        }

        // if we have include
        if (ObjectHelper.isNotEmpty(include)) {
            Iterable it = ObjectHelper.createIterable(include, ",");
            for (Object k : it) {
                String part = k.toString();
                if ("body".equals(part) && !isExcluded("body")) {
                    oldExchange.getMessage().setBody(newExchange.getMessage().getBody());
                    LOG.trace("Including: body");
                } else if ("headers".equals(part) && !isExcluded("headers")) {
                    oldExchange.getMessage().getHeaders().putAll(newExchange.getMessage().getHeaders());
                    LOG.trace("Including: headers");
                } else if (part.startsWith("header:")) {
                    // pattern matching for headers, eg header:foo, header:foo*, header:(foo|bar)
                    String after = StringHelper.after(part, "header:");
                    Iterable i = ObjectHelper.createIterable(after, ",");
                    for (Object o : i) {
                        String pattern = o.toString();
                        for (Map.Entry<String, Object> header : newExchange.getMessage().getHeaders().entrySet()) {
                            String key = header.getKey();
                            boolean matched = EndpointHelper.matchPattern(key, pattern);
                            if (matched && !isExcluded(key)) {
                                LOG.trace("Including: header:{}", key);
                                oldExchange.getMessage().getHeaders().put(key, header.getValue());
                            }
                        }
                    }
                }
            }
        } else if (ObjectHelper.isNotEmpty(exclude)) {
            // grab body unless its excluded
            if (!isExcluded("body")) {
                oldExchange.getMessage().setBody(newExchange.getMessage().getBody());
                LOG.trace("Including: body");
            }

            // if not all headers is excluded, then check each header one-by-one
            if (!isExcluded("headers")) {
                // check if we exclude a specific headers
                Iterable it = ObjectHelper.createIterable(exclude, ",");
                for (Object k : it) {
                    String part = k.toString();
                    if (part.startsWith("header:")) {
                        // pattern matching for headers, eg header:foo, header:foo*, header:(foo|bar)
                        String after = StringHelper.after(part, "header:");
                        Iterable i = ObjectHelper.createIterable(after, ",");
                        for (Object o : i) {
                            String pattern = o.toString();
                            for (Map.Entry<String, Object> header : newExchange.getMessage().getHeaders().entrySet()) {
                                String key = header.getKey();
                                boolean excluded = EndpointHelper.matchPattern(key, pattern);
                                if (!excluded) {
                                    LOG.trace("Including: header:{}", key);
                                    oldExchange.getMessage().getHeaders().put(key, header.getValue());
                                } else {
                                    LOG.trace("Excluding: header:{}", key);
                                }
                            }
                        }
                    }
                }
            }
        }

        return oldExchange;
    }

    private boolean isExcluded(String key) {
        if (ObjectHelper.isEmpty(exclude)) {
            return false;
        }
        String[] excludes = exclude.split(",");
        for (String pattern : excludes) {
            if (pattern.startsWith("header:")) {
                pattern = StringHelper.after(pattern, "header:");
            }
            if (EndpointHelper.matchPattern(key, pattern)) {
                LOG.trace("Excluding: {}", key);
                return true;
            }
        }
        return false;
    }
}
