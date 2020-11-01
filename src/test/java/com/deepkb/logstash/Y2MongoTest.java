/**
 * Source Code is under Apache License Version 2.0, refer to LICENSE file located under project root directory
 * Contributed By  : Y2 Consulting Inc. (https://deepkb.com)
 */
package com.deepkb.logstash;

import co.elastic.logstash.api.Configuration;
import org.junit.Test;
import org.logstash.plugins.ConfigurationImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * FIXME: Works on my computer
 */
public class Y2MongoTest {
    @Test
    public void testY2Mongo() {
        Map<String, Object> configValues = new HashMap<>();
        configValues.put(Y2Mongo.CONFIG_CONNECTION_STRING.name(), "mongodb://localhost:27017");
        configValues.put(Y2Mongo.CONFIG_DATABASE.name(), "mktplace");
        configValues.put(Y2Mongo.CONFIG_COLLECTION.name(), "products");
        configValues.put(Y2Mongo.CONFIG_AGGREGATE.name(), "[{$group: {_id: null, count: { $sum: 1 },  priceTotal: { $sum: \"$price\"}}}]");
        Configuration config = new ConfigurationImpl(configValues);
        Y2Mongo input = new Y2Mongo("test-id", config, null);
        TestConsumer testConsumer = new TestConsumer();
        input.start(testConsumer);
        List<Map<String, Object>> events = testConsumer.getEvents();
        for (Map<String, Object> event : events) {
            System.out.println(event);
        }
    }

    private static class TestConsumer implements Consumer<Map<String, Object>> {
        private List<Map<String, Object>> events = new ArrayList<>();

        @Override
        public void accept(Map<String, Object> event) {
            synchronized (this) {
                events.add(event);
            }
        }

        public List<Map<String, Object>> getEvents() {
            return events;
        }
    }

}