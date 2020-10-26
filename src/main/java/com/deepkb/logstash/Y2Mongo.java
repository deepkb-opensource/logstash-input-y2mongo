package com.deepkb.logstash;

import co.elastic.logstash.api.*;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

@LogstashPlugin(name = "y2mongo")
public class Y2Mongo implements Input {
    final static Logger logger = LogManager.getLogger("y2mongo");
    public static final PluginConfigSpec<String> CONFIG_CONNECTION_STRING = PluginConfigSpec.stringSetting("connection_string", "mongodb://localhost:27017", false, true);
    public static final PluginConfigSpec<String> CONFIG_DATABASE = PluginConfigSpec.stringSetting("database", "", false, true);
    public static final PluginConfigSpec<String> CONFIG_COLLECTION = PluginConfigSpec.stringSetting("collection", "", false, false);
    public static final PluginConfigSpec<String> CONFIG_QUERY = PluginConfigSpec.stringSetting("query", "", false, false);
    public static final PluginConfigSpec<String> CONFIG_AGGREGATE = PluginConfigSpec.stringSetting("aggregate", "", false, false);

    private String id;
    private String connectionString;
    private String databaseName;
    private String collectionName;
    private String query;
    private String aggregate;
    private final CountDownLatch done = new CountDownLatch(1);
    private volatile boolean stopped;

    public Y2Mongo(String id, Configuration config, Context context) {
        this.id = id;
        connectionString = config.get(CONFIG_CONNECTION_STRING);
        databaseName = config.get(CONFIG_DATABASE);
        collectionName = config.get(CONFIG_COLLECTION);
        query = config.get(CONFIG_QUERY);
        aggregate = config.get(CONFIG_AGGREGATE);
    }

    static void convertDocument(Document doc, Map map) {
        for (Map.Entry<String, Object> entry : doc.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                map.put(key, null);
                continue;
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
        }
    }

    @Override
    public void start(Consumer<Map<String, Object>> consumer) {
        MongoClient mongoClient = null;
        MongoCursor<Document> cursor = null;
        try {
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
                    if (bsonValue instanceof  BsonDocument) {
                        aggrList.add((BsonDocument) bsonValue);
                    }
                }
                cursor = collection.aggregate(aggrList).iterator();
            } else {
                cursor = collection.find().iterator();
            }
            int eventCount = 0;
            while (!stopped && cursor.hasNext() && eventCount < 5) {
                Document doc = cursor.next();
                Map map = new LinkedHashMap<String, Object>();
                convertDocument(doc, map);
                consumer.accept(map);
                eventCount++;
            }
        } catch (Exception ex) {
            logger.error("Error process mongodb collection", ex);
        } finally {
            stopped = true;
            done.countDown();
            if (cursor != null) {
                cursor.close();
            }
            mongoClient.close();
        }
    }

    @Override
    public void stop() {
        stopped = true; // set flag to request cooperative stop of input
    }

    @Override
    public void awaitStop() throws InterruptedException {
        done.await(); // blocks until input has stopped
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        return Arrays.asList(CONFIG_CONNECTION_STRING, CONFIG_DATABASE, CONFIG_COLLECTION, CONFIG_QUERY, CONFIG_AGGREGATE);
    }

    @Override
    public String getId() {
        return this.id;
    }
}