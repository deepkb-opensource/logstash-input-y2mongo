/**
 * Source Code is under Apache License Version 2.0, refer to LICENSE file located under project root directory
 * Contributed By  : Y2 Consulting Inc. (https://deepkb.com)
 */
package com.deepkb.logstash;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

/**
 * Scheduled job listened, trace job progress
 */
public class InputJobListener implements JobListener {
    final static Logger log = LogManager.getLogger("y2mongo");

    @Override
    public String getName() {
        return InputJobListener.class.getSimpleName();
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        try {
            String jobName = context.getJobDetail().getKey().toString();
            log.info("{} is about to be executed", jobName);
        } catch (Exception e) {
            log.error("Exception before job execution in listener", e);
        }
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        try {
            String jobName = context.getJobDetail().getKey().toString();
            log.info("{} finished ", jobName);

            log.info("Next fire time {}", context.getNextFireTime());
        } catch (Exception e) {
            log.error("Exception after job execution in listener", e);
        }
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        try {
            String jobName = context.getJobDetail().getKey().toString();
            log.info("{} was about to be executed but a TriggerListener vetoed it's execution", jobName);
        } catch (Exception e) {
            log.error("Exception during job execution veto in listener", e);
        }
    }
}