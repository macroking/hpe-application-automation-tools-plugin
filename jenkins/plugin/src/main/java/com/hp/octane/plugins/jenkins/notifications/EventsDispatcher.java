package com.hp.octane.plugins.jenkins.notifications;

import com.google.inject.Inject;
import com.hp.octane.plugins.jenkins.client.JenkinsMqmRestClientFactory;
import com.hp.octane.plugins.jenkins.client.JenkinsMqmRestClientFactoryImpl;
import com.hp.octane.plugins.jenkins.configuration.ServerConfiguration;
import com.hp.octane.plugins.jenkins.model.events.CIEventBase;
import hudson.Extension;
import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 31/08/14
 * Time: 14:07
 * To change this template use File | Settings | File Templates.
 */

@Extension
public final class EventsDispatcher {
	private static EventsDispatcher extensionInstance;
	private JenkinsMqmRestClientFactory clientFactory;
	private final List<EventsClient> clients = new ArrayList<EventsClient>();

	public static EventsDispatcher getExtensionInstance() {
		List<EventsDispatcher> extensions;
		if (extensionInstance == null) {
			extensions = Jenkins.getInstance().getExtensionList(EventsDispatcher.class);
			if (extensions.isEmpty()) {
				throw new RuntimeException("Events Dispatcher was not initialized properly");
			} else {
				extensionInstance = extensions.get(0);
			}
		}
		return extensionInstance;
	}

	public void updateClient(ServerConfiguration conf) {
		updateClient(conf, null);
	}

	public void updateClient(ServerConfiguration conf, ServerConfiguration oldConf) {
		synchronized (clients) {
			if (oldConf != null && !conf.location.equals(oldConf.location)) {
				for (EventsClient client : clients) {
					if (client.getUrl().equals(oldConf.location)) {
						clients.remove(client);
						break;
					}
				}
			}
			if (conf != null &&
					conf.location != null && !conf.location.equals("") &&
					conf.domain != null && !conf.domain.equals("") &&
					conf.project != null && !conf.project.equals("") &&
					conf.username != null && !conf.username.equals("") && conf.password != null) {
				updateClient(conf.location, conf.domain, conf.project, conf.username, conf.password);
			}
		}
	}

	public void updateClient(String url, String domain, String project, String username, String password) {
		EventsClient client = null;
		synchronized (clients) {
			for (EventsClient c : clients) {
				if (c.getUrl().equals(url)) {
					client = c;
					client.update(url, domain, project, username, password, clientFactory);
				}
			}
			if (client == null) {
				client = new EventsClient(url, domain, project, username, password, clientFactory);
				clients.add(client);
			}
		}
		client.activate();
	}

	public void wakeUpClients() {
		synchronized (clients) {
			for (EventsClient c : clients) {
				c.activate();
			}
		}
	}

	public void dispatchEvent(CIEventBase event) {
		for (EventsClient c : clients) {
			if (c.isActive()) c.pushEvent(event);
		}
	}

	public List<EventsClient> getStatus() {
		return clients;
	}

	@Inject
	public void setMqmRestClientFactory(JenkinsMqmRestClientFactoryImpl clientFactory) {
		this.clientFactory = clientFactory;
	}
}
