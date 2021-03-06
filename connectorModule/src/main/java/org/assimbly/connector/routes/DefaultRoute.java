package org.assimbly.connector.routes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.camel.LoggingLevel;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import static org.apache.camel.language.groovy.GroovyLanguage.groovy;

import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.connector.processors.ConvertProcessor;
import org.assimbly.connector.processors.FailureProcessor;
import org.assimbly.connector.processors.HeadersProcessor;
import org.assimbly.util.EncryptionUtil;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DefaultRoute extends RouteBuilder {
	
	TreeMap<String, String> props;
	private DefaultErrorHandlerBuilder routeErrorHandler;
	private static Logger logger = LoggerFactory.getLogger("org.assimbly.connector.routes.DefaultRoute");
	private String flowId;
	private int maximumRedeliveries;
	private int redeliveryDelay;
	private int maximumRedeliveryDelay;
	private int backOffMultiplier;
	private String logMessage;

	private List<String> onrampUriKeys;
	private List<String> offrampUriKeys;
	private String[] offrampUriList;
	private List<String> responseUriKeys;
	int index = 0;

	public DefaultRoute(final TreeMap<String, String> props){
		this.props = props;
	}

	public DefaultRoute() {}

	public interface FailureProcessorListener {
		 public void onFailure();
  	}

	@Override
	public void configure() throws Exception {
			
		logger.info("Configuring default route");
		
		getContext().setTracing(true);
		EncryptableProperties decryptedProperties = decryptProperties(props);

		Processor headerProcessor = new HeadersProcessor(props);
		Processor failureProcessor = new FailureProcessor(props);
		Processor convertProcessor = new ConvertProcessor();

		flowId = props.get("id");
		onrampUriKeys = getUriKeys("from");
		offrampUriKeys = getUriKeys("to");
		offrampUriList = getOfframpUriList();
		responseUriKeys = getUriKeys("response");

		if (this.props.containsKey("flow.maximumRedeliveries")){
			String maximumRedeliveriesAsString = props.get("flow.maximumRedeliveries");
			if(StringUtils.isNumeric(maximumRedeliveriesAsString)) {
				maximumRedeliveries = Integer.parseInt(maximumRedeliveriesAsString);
			}else {
				maximumRedeliveries = 0;
			}
		}else {
			maximumRedeliveries = 0;
		}
		
		if (this.props.containsKey("flowredeliveryDelay")){
			String RedeliveryDelayAsString = props.get("flow.redeliveryDelay");
			if(StringUtils.isNumeric(RedeliveryDelayAsString)) {
				redeliveryDelay = Integer.parseInt(RedeliveryDelayAsString);
				maximumRedeliveryDelay = redeliveryDelay * 10;
			}else {
				redeliveryDelay = 3000;
				maximumRedeliveryDelay = 60000;
			}
		}else {
			redeliveryDelay = 3000;
			maximumRedeliveryDelay = 60000;
		}
		
		if (this.props.containsKey("flow.backOffMultiplier")){
			String backOffMultiplierAsString = props.get("flow.backOffMultiplier");
			if(StringUtils.isNumeric(backOffMultiplierAsString)) {
				backOffMultiplier = Integer.parseInt(backOffMultiplierAsString);
			}else {
				backOffMultiplier = 0;
			}
		}else {
			backOffMultiplier = 0;
		}
		
		if (this.props.containsKey("flow.logLevel")){
			String logLevelAsString = props.get("flow.logLevel");
			String routeName = props.get("flow.name");
			logMessage = "log:RouteName." + routeName + "?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed";
		}else {
			String routeName = props.get("flow.name");
			logMessage = "log:RouteName." + routeName + "level=OFF&showAll=true&multiline=true&style=Fixed";
		}		
		
		if (this.props.containsKey("error.uri")){
			routeErrorHandler = deadLetterChannel(props.get("error.uri"))
			.allowRedeliveryWhileStopping(false)
			.asyncDelayedRedelivery()			
			.maximumRedeliveries(maximumRedeliveries)
			.redeliveryDelay(redeliveryDelay)
			.maximumRedeliveryDelay(maximumRedeliveryDelay)			
			.backOffMultiplier(backOffMultiplier)
			.retriesExhaustedLogLevel(LoggingLevel.ERROR)
			.retryAttemptedLogLevel(LoggingLevel.DEBUG)
			.onExceptionOccurred(failureProcessor)
			.log("This is a log message")
			.log(log)
			.logRetryStackTrace(false)
			.logStackTrace(true)
			.logHandled(true)
			.logExhausted(true)
			.logExhaustedMessageHistory(true);					
		}
		else{
			routeErrorHandler = defaultErrorHandler()
			.allowRedeliveryWhileStopping(false)
			.asyncDelayedRedelivery()
			.maximumRedeliveries(maximumRedeliveries)
			.redeliveryDelay(redeliveryDelay)
			.maximumRedeliveryDelay(maximumRedeliveryDelay)
			.backOffMultiplier(backOffMultiplier)
			.retriesExhaustedLogLevel(LoggingLevel.ERROR)
			.retryAttemptedLogLevel(LoggingLevel.DEBUG)
			.onExceptionOccurred(failureProcessor)
			.logRetryStackTrace(false)
			.logStackTrace(true)
			.logHandled(true)
			.logExhausted(true)
			.logExhaustedMessageHistory(true)
			.log(logger);
		}

		routeErrorHandler.setAsyncDelayedRedelivery(true);

		//The default Camel route (onramp)

		for(String onrampUriKey : onrampUriKeys){

			String uri = props.get(onrampUriKey);
			uri = DecryptValue(uri);

			String endpointId = StringUtils.substringBetween(onrampUriKey, "from.", ".uri");
            String headerId = props.get("from." + endpointId + ".header.id");

			//The default Camel route (onramp)
			from(uri)
					.errorHandler(routeErrorHandler)
					.setHeader("AssimblyFlowID", constant(flowId))
					.setHeader("AssimblyHeaderId", constant(headerId))
					.setHeader("AssimblyFrom", constant(props.get("from." + endpointId + ".uri")))
					.setHeader("AssimblyCorrelationId", simple("${date:now:yyyyMMdd}${exchangeId}"))
					.setHeader("AssimblyFromTimestamp", groovy("new Date().getTime()"))
					.to(logMessage)
					.process(headerProcessor)
					.id("headerProcessor" + flowId + "-" + endpointId)
					.multicast()
					.shareUnitOfWork()
					.parallelProcessing()
					.to(offrampUriList)
					.routeId(flowId + "-" + endpointId).description("from");
		}
        
        //The default Camel route (offramp)		
		for (String offrampUriKey : offrampUriKeys)
		{

			String uri = props.get(offrampUriKey);
			uri = DecryptValue(uri);
			String offrampUri = offrampUriList[index++];
			String endpointId = StringUtils.substringBetween(offrampUriKey, "to.", ".uri");
			String headerId = props.get("to." + endpointId + ".header.id");
			String responseId = props.get("to." + endpointId + ".response.id");

			Predicate hasResponseEndpoint = PredicateBuilder.constant(responseId != null && !responseId.isEmpty());
			Predicate hasDynamicEndpoint = PredicateBuilder.constant(uri.contains("${"));

			from(offrampUri)
			.errorHandler(routeErrorHandler)
			.setHeader("AssimblyHeaderId", constant(headerId))
			.setHeader("AssimblyTo", constant(uri))
			.setHeader("AssimblyToTimestamp", groovy("new Date().getTime()"))
			.process(headerProcessor)
			.id("headerProcessor" + flowId + "-" + endpointId)
			.process(convertProcessor)
			.id("convertProcessor" + flowId + "-" + endpointId)
			.log(hasResponseEndpoint.toString())
     	    .choice()
				.when(hasResponseEndpoint)
					.to(logMessage)
					.choice()
						.when(hasDynamicEndpoint)
							.toD(uri)
							.to("direct:flow=" + flowId + "endpoint=" + responseId)
						.otherwise()
							.to(uri)
							.to("direct:flow=" + flowId + "endpoint=" + responseId)
					.endChoice()
	  		    .when(header("ReplyTo").convertToString().contains(":"))
	  		    	.to(logMessage)
					.choice()
						.when(hasDynamicEndpoint)
							.toD(uri)
							.toD("${header.ReplyTo}")
						.otherwise()
							.to(uri)
							.toD("${header.ReplyTo}")
						.endChoice()
	  		    .when(header("ReplyTo").isNotNull())
	  		    	.to(logMessage)
					.choice()
						.when(hasDynamicEndpoint)
							.toD(uri)
							.toD("vm://${header.ReplyTo}")
						.otherwise()
							.to(uri)
							.toD("vm://${header.ReplyTo}")
					.endChoice()
				.when(hasDynamicEndpoint)
					.toD(uri)
				.otherwise()
					.to(uri)
	  		 .end()
	  		 .to(logMessage)
	  		 .routeId(flowId + "-" + endpointId).description("to");

		}

		for(String responseUriKey : responseUriKeys){
			String uri = props.get(responseUriKey);
			String endpointId = StringUtils.substringBetween(responseUriKey, "response.", ".uri");
			String headerId = props.get("response." + endpointId + ".header.id");
			String responseId = props.get("response." + endpointId + ".response.id");

			from("direct:flow=" + flowId + "endpoint=" + responseId)
					.errorHandler(routeErrorHandler)
					.setHeader("AssimblyFlowID", constant(flowId))
					.setHeader("AssimblyHeaderId", constant(headerId))
					.setHeader("AssimblyResponse", constant(props.get("response." + endpointId + ".uri")))
					.setHeader("AssimblyCorrelationId", simple("${date:now:yyyyMMdd}${exchangeId}"))
					.setHeader("AssimblyResponseTimestamp", groovy("new Date().getTime()"))
					.to(logMessage)
					.process(headerProcessor)
					.id("headerProcessor" + flowId + "-" + endpointId)
					.toD(uri)
					.routeId(flowId + "-" + endpointId).description("response");

		}
	}

	//create a string array for all offramps
	private String[] getOfframpUriList() {

		String offrampUri = props.get("offramp.uri.list");

		return offrampUri.split(",");

	}

	//create a string array for all of a specific endpointType
	private List<String> getUriKeys(String endpointType) {

		List<String> keys = new ArrayList<>();

		for(String prop : props.keySet()){
			if(prop.startsWith(endpointType) && prop.endsWith("uri")){
				keys.add(prop);
			}
		}

		return keys;

	}

	//
	private EncryptableProperties decryptProperties(TreeMap<String, String> properties) {
		EncryptableProperties decryptedProperties = (EncryptableProperties) ((PropertiesComponent) getContext().getPropertiesComponent()).getInitialProperties();
		decryptedProperties.putAll(properties);
		return decryptedProperties;
	}

	private String DecryptValue(String value){

		EncryptableProperties encryptionProperties = (EncryptableProperties) ((PropertiesComponent) getContext().getPropertiesComponent()).getInitialProperties();
		String[] encryptedList = StringUtils.substringsBetween(value, "ENC(", ")");

		if(encryptedList !=null && encryptedList.length>0){
			for (String encrypted: encryptedList) {
				encryptionProperties.setProperty("temp","ENC(" + encrypted + ")");
				String decrypted = encryptionProperties.getProperty("temp");
				value = StringUtils.replace(value, "ENC(" + encrypted + ")",decrypted);
			}
		}

		return value;

	}

}
