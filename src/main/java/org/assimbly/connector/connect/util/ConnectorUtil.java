package org.assimbly.connector.connect.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.StringWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;


public final class ConnectorUtil {

	
    public static String convertStreamToString(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static InputStream convertStringToStream(String str) throws UnsupportedEncodingException {
        return new ByteArrayInputStream(str.getBytes("UTF-8"));
    }
    
    public static String getContentFromJdbc(String connectorID, URI configurationUri){
    	
    	return "";
    }

	public static List<String> getXMLParameters(XMLConfiguration conf, String prefix) throws ConfigurationException {
	  	   
	  	   Iterator<String> keys;
	  	   
	  	   if(prefix == null || prefix.isEmpty()){
	  		 keys = conf.getKeys();
	  	   }else{
	  		 keys = conf.getKeys(prefix);
	  	   }
	  	     
	  	   List<String> keyList = new ArrayList<String>();
	    
	  	   while(keys.hasNext()){
	  		   keyList.add(keys.next());
	  	   }

	  	   return keyList;
	}

	
	public static boolean isValidURI(String name) throws Exception {
		try {
		    URI uri = new URI(name);
		    
		    if(uri.getScheme() == null){
		    	return false;
		    }else{ 
		    	return true;
		    }
		    
		} catch (URISyntaxException e) {
			return false;
		}
		
	}
	
	public static String convertDocToString(Document doc) {
	    try {
	        StringWriter sw = new StringWriter();
	        TransformerFactory tf = TransformerFactory.newInstance();
	        Transformer transformer = tf.newTransformer();
	        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
	        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

	        transformer.transform(new DOMSource(doc), new StreamResult(sw));
	        return sw.toString();
	    } catch (Exception ex) {
	        throw new RuntimeException("Error converting to String", ex);
	    }
	}
}
