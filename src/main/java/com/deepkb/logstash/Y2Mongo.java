/**
 * Source Code is under Apache License Version 2.0, refer to LICENSE file located under project root directory
 * Contributed By  : Y2 Consulting Inc. (https://deepkb.com)
 */
package com.deepkb.logstash;

import co.elastic.logstash.api.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Import from MongoDB Pipeline Implementation
 * Execute query or aggregate against a MongoDB database and inject the records into the logstash pipeline
 * Configurations:
 *  connection_string : mongodb connections string, Eg: mongodb://localhost:27017 , if authentication needed, put it in the connection string
 *  database : name of database
 *  collection : name of collection
 *  query : query string
 *  aggregate : aggregate string
 *  schedule : cron like scheduler, syntax please refer to http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html
 *
 */
@LogstashPlugin(name = "y2mongo")
public class Y2Mongo implements Input {
    final static Logger log = LogManager.getLogger("y2mongo");
    public static final PluginConfigSpec<String> CONFIG_CONNECTION_STRING = PluginConfigSpec.stringSetting("connection_string", "mongodb://localhost:27017", false, true);
    public static final PluginConfigSpec<String> CONFIG_DATABASE = PluginConfigSpec.stringSetting("database", "", false, true);
    public static final PluginConfigSpec<String> CONFIG_COLLECTION = PluginConfigSpec.stringSetting("collection", "", false, false);
    public static final PluginConfigSpec<String> CONFIG_QUERY = PluginConfigSpec.stringSetting("query", "", false, false);
    public static final PluginConfigSpec<String> CONFIG_AGGREGATE = PluginConfigSpec.stringSetting("aggregate", "", false, false);
    public static final PluginConfigSpec<String> CONFIG_SCHEDULE = PluginConfigSpec.stringSetting("schedule", "", false, false);

    private String id;
    private Configuration config;
    private final CountDownLatch done = new CountDownLatch(1);
    private volatile boolean stopped;

    public Y2Mongo(String id, Configuration config, Context context) {
        this.id = id;
        this.config = config;
    }

    @Override
    public void start(Consumer<Map<String, Object>> consumer) {
        try {
            String cron = config.get(CONFIG_SCHEDULE);
            if (StringUtils.isEmpty(cron)) {
                //No scheduler defined
                MongoInputJob mongoInputJob = new MongoInputJob();
                mongoInputJob.execute(this, consumer);
                return;
            }
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            Scheduler scheduler = schedulerFactory.getScheduler();
            JobDetail job = newJob(MongoInputJob.class)
                    .withIdentity("MongoInputJob", "MongoInputJobGroup")
                    .build();

            CronTrigger trigger = newTrigger()
                    .withIdentity("MongoInputTrigger", "MongoInputJobGroup")
                    .withSchedule(cronSchedule(cron))
                    .startNow()
                    .build();

            //Add listener to trace
            scheduler.getListenerManager().addJobListener(new InputJobListener());

            scheduler.scheduleJob(job, trigger);
            scheduler.getContext().put("y2mongo", this);
            scheduler.getContext().put("consumer", consumer);
            scheduler.start();
            log.info("Mongo Input Job Scheduled ... " + trigger.getCronExpression());
            log.info("============================================================");
            log.info("Cron Job Summary ");
            log.info(trigger.getExpressionSummary());
            log.info("============================================================");
            log.info("Next fire time " + trigger.getNextFireTime());
            while (!stopped) {
                //Just sleep, nothing to do.
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    log.error("Error Y2Mongo ", ie);
                }
            }
        } catch (SchedulerException ex) {
            log.error("Error Y2Mongo ", ex);
        } finally {
            stopped = true;
            done.countDown();
        }
    }

    public boolean isStopped() {
        return stopped;
    }

    public Configuration getConfig() {
        return config;
    }

    @Override
    public void stop() {
        stopped = true;
    }

    @Override
    public void awaitStop() throws InterruptedException {
        done.await();
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        return Arrays.asList(CONFIG_CONNECTION_STRING, CONFIG_DATABASE, CONFIG_COLLECTION, CONFIG_QUERY,
                CONFIG_AGGREGATE, CONFIG_SCHEDULE);
    }

    @Override
    public String getId() {
        return this.id;
    }
}