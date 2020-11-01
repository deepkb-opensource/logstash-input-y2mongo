/**
 * Source Code is under Apache License Version 2.0, refer to LICENSE file located under project root directory
 * Contributed By  : Y2 Consulting Inc. (https://deepkb.com)
 */
package com.deepkb.logstash;

import co.elastic.logstash.api.Configuration;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.quartz.*;

import java.util.*;
import java.util.function.Consumer;

//disallow the execution of more than one instances of the same job at the same time
@DisallowConcurrentExecution
public class MongoInputJob implements Job {
    final static Logger log = LogManager.getLogger("y2mongo");

    static void convertDocument(Document doc, Map map) {
        doc.entrySet().forEach(entry -> {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (null == value) {
                map.put(key, null);
                return;
            }
            if (value instanceof ObjectId) {
                map.put(key, ((ObjectId) value).toString());
            } else if (value instanceof Document) {
                Map map1 = new LinkedHashMap();
                convertDocument((Document) value, map1);
                map.put(key, map1);
            } else {
                map.put(key, value);
            }
        });
    }

    /**
     * Query MongoDB, iterate each records and generate events in the pipeline
     *
     * @param y2Mongo  processor passed from Y2Mongo.class
     * @param consumer pipeline event consumer
     */
    public void execute(Y2Mongo y2Mongo, Consumer<Map<String, Object>> consumer) {
        MongoClient mongoClient = null;
        MongoCursor<Document> cursor = null;
        final StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        try {
            Configuration config = y2Mongo.getConfig();
            String connectionString = config.get(Y2Mongo.CONFIG_CONNECTION_STRING);
            String databaseName = config.get(Y2Mongo.CONFIG_DATABASE);
            String collectionName = config.get(Y2Mongo.CONFIG_COLLECTION);
            String query = config.get(Y2Mongo.CONFIG_QUERY);
            String aggregate = config.get(Y2Mongo.CONFIG_AGGREGATE);

            MongoClientURI clientURI = new MongoClientURI(connectionString);
            mongoClient = new MongoClient(clientURI);
            MongoDatabase mongoDatabase = mongoClient.getDatabase(databaseName);
            MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
            if (StringUtils.isNotEmpty(query)) {
                cursor = collection.find(BsonDocument.parse(query)).iterator();
            } else if (StringUtils.isNotEmpty(aggregate)) {
                Iterator<BsonValue> aggItr = BsonArray.parse(aggregate).getValues().iterator();
                List<BsonDocument> aggrList = new ArrayList<>();
                while (aggItr.hasNext()) {
                    BsonValue bsonValue = aggItr.next();
                    if (bsonValue instanceof BsonDocument) {
                        aggrList.add((BsonDocument) bsonValue);
                    }
                }
                cursor = collection.aggregate(aggrList).iterator();
            } else {
                cursor = collection.find().iterator();
            }
            int count = 0;
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Map map = new LinkedHashMap<String, Object>();
                convertDocument(doc, map);
                consumer.accept(map);
                count++;
                //Check if stop signaled
                if (y2Mongo.isStopped()) {
                    break;
                }
            }
            log.info("Scanned records : {}", count);
            log.info("Processing time {}", stopwatch.formatTime());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (mongoClient != null) {
                mongoClient.close();
            }
        }
    }

    /**
     * @param context job execution context
     * @throws JobExecutionException
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            Y2Mongo y2Mongo = (Y2Mongo) context.getScheduler().getContext().get("y2mongo");
            Consumer<Map<String, Object>> consumer = (Consumer<Map<String, Object>>) context.getScheduler().getContext().get("consumer");
            execute(y2Mongo, consumer);
        } catch (SchedulerException ex) {
            throw new JobExecutionException(ex);
        }
    }
}
