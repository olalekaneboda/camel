/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.processor;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.util.ObjectHelper;

public class ClaimCheckAggregationStrategy implements AggregationStrategy {

    private final String data;
    // TODO: pattern matching for headers, eg headers:foo*, headers, headers:*, header:foo,header:bar

    public ClaimCheckAggregationStrategy(String data) {
        this.data = data;
    }

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        if (newExchange == null) {
            return oldExchange;
        }

        if (ObjectHelper.isEmpty(data)) {
            // grab everything if data is empty
            return newExchange;
        }

        Iterable it = ObjectHelper.createIterable(data, ",");
        for (Object k : it) {
            String part = k.toString();
            if ("body".equals(part)) {
                oldExchange.getMessage().setBody(newExchange.getMessage().getBody());
            } else if ("headers".equals(part)) {
                oldExchange.getMessage().getHeaders().putAll(newExchange.getMessage().getHeaders());
            }
        }

        return oldExchange;
    }
}
