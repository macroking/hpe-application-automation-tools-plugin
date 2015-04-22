package com.hp.octane.plugins.jenkins.notifications;

import com.google.inject.Inject;
import com.hp.octane.plugins.jenkins.model.processors.parameters.ParameterProcessors;
import com.hp.octane.plugins.jenkins.model.processors.scm.SCMProcessors;
import com.hp.octane.plugins.jenkins.model.snapshots.SnapshotResult;
import com.hp.octane.plugins.jenkins.model.causes.CIEventCausesFactory;
import com.hp.octane.plugins.jenkins.model.events.CIEventFinished;
import com.hp.octane.plugins.jenkins.model.events.CIEventStarted;
import com.hp.octane.plugins.jenkins.tests.TestListener;
import hudson.Extension;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 24/08/14
 * Time: 17:21
 * To change this template use File | Settings | File Templates.
 */

@Extension
public final class RunListenerImpl extends RunListener<Run> {
	private static Logger logger = Logger.getLogger(RunListenerImpl.class.getName());

	@Inject
	private TestListener testListener;

	@Override
	@SuppressWarnings("unchecked")
	public void onStarted(Run r, TaskListener listener) {
		if (r instanceof AbstractBuild) {
			AbstractBuild build = (AbstractBuild) r;
			CIEventStarted event = new CIEventStarted(
					build.getProject().getName(),
					build.getNumber(),
					build.getStartTimeInMillis(),
					build.getEstimatedDuration(),
					CIEventCausesFactory.processCauses(build.getCauses()),
					ParameterProcessors.getInstances(build)
			);
			EventsDispatcher.getExtensionInstance().dispatchEvent(event);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onCompleted(Run r, @Nonnull TaskListener listener) {
		if (r instanceof AbstractBuild) {
			AbstractBuild build = (AbstractBuild) r;
			SnapshotResult result;
			if (build.getResult() == Result.SUCCESS) {
				result = SnapshotResult.SUCCESS;
			} else if (build.getResult() == Result.ABORTED) {
				result = SnapshotResult.ABORTED;
			} else if (build.getResult() == Result.FAILURE) {
				result = SnapshotResult.FAILURE;
			} else if (build.getResult() == Result.UNSTABLE) {
				result = SnapshotResult.UNSTABLE;
			} else {
				result = SnapshotResult.UNAVAILABLE;
			}
			CIEventFinished event = new CIEventFinished(
					build.getProject().getName(),
					build.getNumber(),
					build.getStartTimeInMillis(),
					build.getEstimatedDuration(),
					CIEventCausesFactory.processCauses(build.getCauses()),
					ParameterProcessors.getInstances(build),
					result,
					build.getDuration(),
					SCMProcessors
							.getAppropriate(build.getProject().getScm().getClass().getName())
							.getSCMChanges(build)
			);
			EventsDispatcher.getExtensionInstance().dispatchEvent(event);

			testListener.processBuild(build);
		}
	}
}
