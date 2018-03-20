package org.assimbly.connector.impl;


import java.net.URI;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.ServiceStatus;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.assimbly.connector.routes.DefaultRoute;
import org.assimbly.connector.routes.PollingJdbcRoute;
import org.assimbly.connector.service.Connection;

public class CamelConnector extends BaseConnector {

	private CamelContext context;
	private ProducerTemplate template;
	private boolean started = false;
	private int stopTimeout = 30;
	private ServiceStatus status;
	
	private static Logger logger = LoggerFactory.getLogger("org.assimbly.camelconnector.connect.impl.CamelConnector");

	public CamelConnector() {

	}
	
	public CamelConnector(String connectorId, String configuration) throws Exception {
		setFlowConfiguration(convertXMLToFlowConfiguration(connectorId, configuration));
	}

	public CamelConnector(String connectorId, URI configuration) throws Exception {
		setFlowConfiguration(convertXMLToFlowConfiguration(connectorId, configuration));
	}
	
	
	public void start() throws Exception {

		SimpleRegistry registry = new SimpleRegistry();
		context = new DefaultCamelContext(registry);
		context.setStreamCaching(true);
		context.getShutdownStrategy().setSuppressLoggingOnTimeout(true);
		
		// start Camel context
		context.start();
		started = true;
		logger.info("Connector started");

	}

	public void stop() throws Exception {
		super.getConfiguration().clear();
		if (context != null){
			for (Route route : context.getRoutes()) {
				context.stopRoute(route.getId(), stopTimeout, TimeUnit.SECONDS);
				context.removeRoute(route.getId());
			}
			context.stop();
			started = false;
		}
	}	

	public boolean isStarted() {
		return started;
	}
	
	
	public void addRoute(TreeMap<String, String> props) throws Exception {
		for (String key : props.keySet()){
			if (key.contains("connection_id")){
				props = new Connection(context, props).start();
			}
		}
		template = context.createProducerTemplate();
		template.setDefaultEndpointUri(props.get("to.uri"));
		String route  = props.get("route");
		if (route == null){
			logger.info("add default route");
			addDefaultFlow(props);
		}else if(route.equals("default")){
			logger.info("add default route");
			addDefaultFlow(props);			
		}else if(route.equals("fromJdbcTimer")){
			addFlowFromJdbcTimer(props);
		}
		else{
			logger.info("Invalid route.");
		}
	}

	public void addDefaultFlow(final TreeMap<String, String> props) throws Exception {
		logger.info("defaultroutes");
		context.addRoutes(new DefaultRoute(props));
	}
		
	public void addFlowFromJdbcTimer(final TreeMap<String, String> props)	throws Exception {
		context.addRoutes(new PollingJdbcRoute(props));
		
	}

	public boolean hasFlow(String id) {
		boolean routeFound = false;
		if (context != null){
			for (Route route : context.getRoutes()) {
				if (route.getId().equals(id)) {
					routeFound = true;
				}
			}
		}
		return routeFound;
	}


	public void removeFlow(String id) throws Exception {
		context.removeRoute(id);
	}

	public void startFlow(String id) throws Exception {
		if(!hasFlow(id)) {
			for (TreeMap<String, String> props : super.getConfiguration()) {
				if (props.get("id").equals(id)) {
					logger.info("Adding route with ids: " + id);
					addRoute(props);
				}
			}
		}

		context.startRoute(id);
	}

	public void restartFlow(String id) throws Exception {
				
		stopFlow(id);
		
		int count = 1;
		
        do {
        	status = context.getRouteStatus(id);
        	Thread.sleep(10);
        	count++;
        	
        } while (status.isStopping() || count < 3000);
		
        if(count==3000) {
			logger.error("Timed out after 30 seconds while stopping route with id: " + id);
        }else {
        	startFlow(id);	
        }	
	}
	
	public void stopFlow(String id) throws Exception {
		if(hasFlow(id)) {
        	status = context.getRouteStatus(id);
			if(status.isStoppable()) {
				context.stopRoute(id);	
			}
		}
	}

	public void pauseFlow(String id) throws Exception {
		if(hasFlow(id)) {
        	status = context.getRouteStatus(id);
			if(status.isSuspendable()) {
				context.suspendRoute(id);	
			}
		}
	}

	public void resumeFlow(String id) throws Exception {
		if(hasFlow(id)) {
        	status = context.getRouteStatus(id);
			if(status.isSuspended()) {
				context.resumeRoute(id);	
			}
		}
	}	
	
	public String getFlowStatus(String id) {
	
		ServiceStatus status = context.getRouteStatus(id);
		return status.toString();
		
	}
	
	public Object getContext() {
		
		return context;
		
	}	
	
	public void send(Object messageBody, ProducerTemplate template) {
		template.sendBody(messageBody);
	}

	public void sendWithHeaders(Object messageBody,
			TreeMap<String, Object> messageHeaders, ProducerTemplate template) {
		template.sendBodyAndHeaders(messageBody, messageHeaders);
	}

}