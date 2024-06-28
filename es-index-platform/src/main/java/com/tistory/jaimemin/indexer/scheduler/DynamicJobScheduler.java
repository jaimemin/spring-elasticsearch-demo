package com.tistory.jaimemin.indexer.scheduler;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tistory.jaimemin.indexer.batch.DynamicIndexJob;

@Component
public class DynamicJobScheduler {

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private DynamicIndexJob dynamicIndexJob;

	@Scheduled(cron = "0/10 * * * * *")
	public void runJob() throws
		JobInstanceAlreadyCompleteException,
		JobExecutionAlreadyRunningException,
		JobParametersInvalidException,
		JobRestartException {
		JobParameters jobParameters = new JobParametersBuilder()
			.addString("jobName", "dynamicIndexJob")
			.addLong("time", System.currentTimeMillis())
			.toJobParameters();

		jobLauncher.run(dynamicIndexJob.dynamicIndexJobBuild(dynamicIndexJob.dynamicIndexJobStep()), jobParameters);
	}
}
