/*
 * #%L
 * sample-maintenance-task
 * %%
 * Copyright (C) 2014 Adobe
 * %%
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
 * #L%
 */

package com.adobe.granite.samples.maintenance.impl;

import java.io.File;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.maintenance.MaintenanceConstants;

@Component(metatype = true,
        label = "Delete Temp Files Maintenance Task",
        description = "Maintatence Task which deletes files from a configurable temporary directory which have been modified in the last 24 hours.")
@Service
@Properties({
        @Property(name = MaintenanceConstants.PROPERTY_TASK_NAME, value = "DeleteTempFilesTask", propertyPrivate = true),
        @Property(name = MaintenanceConstants.PROPERTY_TASK_TITLE, value = "Delete Temp Files", propertyPrivate = true),
        @Property(name = JobConsumer.PROPERTY_TOPICS, value = MaintenanceConstants.TASK_TOPIC_PREFIX
                + "DeleteTempFilesTask", propertyPrivate = true) })
public class DeleteTempFilesTask implements JobExecutor {

    private static final Logger log = LoggerFactory.getLogger(DeleteTempFilesTask.class);

    @Property(label = "Temporary Directory", description="Temporary Directory. Defaults to the java.io.tmpdir system property.")
    private static final String PROP_TEMP_DIR = "temp.dir";

    private File tempDir;

    @Activate
    private void activate(Map<String, Object> properties) {
        this.tempDir = new File(PropertiesUtil.toString(properties.get(PROP_TEMP_DIR),
                System.getProperty("java.io.tmpdir")));
    }

    @Override
    public JobExecutionResult process(Job job, JobExecutionContext context) {
        log.info("Deleting old temp files from {}.", tempDir.getAbsolutePath());
        Collection<File> files = FileUtils.listFiles(tempDir, new LastModifiedBeforeYesterdayFilter(),
                TrueFileFilter.INSTANCE);
        int counter = 0;
        for (File file : files) {
            log.debug("Deleting file {}.", file.getAbsolutePath());
            counter++;
            file.delete();
            // TODO - capture the output of delete() and do something useful with it
        }
        return context.result().message(String.format("Deleted %s files.", counter)).succeeded();
    }

    /**
     * IOFileFilter which filters out files which have been modified in the last 24 hours.
     *
     */
    private static class LastModifiedBeforeYesterdayFilter implements IOFileFilter {

        private final long minTime;

        private LastModifiedBeforeYesterdayFilter() {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -1);
            this.minTime = cal.getTimeInMillis();
        }

        @Override
        public boolean accept(File dir, String name) {
            // this method is never actually called.
            return false;
        }

        @Override
        public boolean accept(File file) {
            return file.lastModified() <= this.minTime;
        }
    }

}
