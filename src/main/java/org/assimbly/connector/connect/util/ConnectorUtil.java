package org.assimbly.connector.connect.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.net.ssl.SSLSession;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class ConnectorUtil {
	
    public static final String PEER_CERTIFICATES = "PEER_CERTIFICATES";
	private static Logger logger = LoggerFactory.getLogger("org.assimbly.connector.util.ConnectorUtil");
	
	public static boolean isValidUri(String name) throws Exception {
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

	public static void printTreemap(TreeMap<String, String> treeMap) throws Exception {

	    Map<String, String> id = treeMap.entrySet()
	    	      .stream()
	    	      .filter(map -> map.getKey().equals("id"))
	    	      .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));

		
	    Map<String, String> flow = treeMap.entrySet()
	    	      .stream()
	    	      .filter(map -> map.getKey().startsWith("flow"))
	    	      .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));

	    Map<String, String> from = treeMap.entrySet()
	    	      .stream()
	    	      .filter(map -> map.getKey().startsWith("from"))
	    	      .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
	    
	    Map<String, String> to = treeMap.entrySet()
	    	      .stream()
	    	      .filter(map -> map.getKey().startsWith("to"))
	    	      .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
	    
	    Map<String, String> error = treeMap.entrySet()
	    	      .stream()
	    	      .filter(map -> map.getKey().startsWith("error"))
	    	      .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));

	    Map<String, String> header = treeMap.entrySet()
	    	      .stream()
	    	      .filter(map -> map.getKey().startsWith("header"))
	    	      .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
	    
	    Map<String, String> service = treeMap.entrySet()
	    	      .stream()
	    	      .filter(map -> map.getKey().startsWith("service"))
	    	      .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
	    
		System.out.println("");
		System.out.println("FLOW CONFIGURATION");
		System.out.println("-----------------------------------------------------------\n");

		for(Map.Entry<String,String> entry : id.entrySet()) {

			  String key = entry.getKey();
			  String value = entry.getValue();
			  System.out.printf("%-30s %s\n", key + ":", value);  
			  
		}

		for(Map.Entry<String,String> entry : flow.entrySet()) {

			  String key = entry.getKey();
			  String value = entry.getValue();
			  System.out.printf("%-30s %s\n", key + ":", value);  
			  
		}

		System.out.println("\nENDPOINTS\n");
		
		for(Map.Entry<String,String> entry : from.entrySet()) {

			  String key = entry.getKey();
			  String value = entry.getValue();
			  System.out.printf("%-30s %s\n", key + ":", value);  
			  
		}
		
		for(Map.Entry<String,String> entry : to.entrySet()) {

			  String key = entry.getKey();
			  String value = entry.getValue();
			  System.out.printf("%-30s %s\n", key + ":", value);  
			  
		}

		for(Map.Entry<String,String> entry : error.entrySet()) {

			  String key = entry.getKey();
			  String value = entry.getValue();
			  System.out.printf("%-30s %s\n", key + ":", value);  
			  
		}

		if(!header.isEmpty()) {
			
			System.out.println("\nHEADERS\n");
		
			for(Map.Entry<String,String> entry : header.entrySet()) {
	
				  String key = entry.getKey();
				  String value = entry.getValue();
				  if(key.contains("password"))
					  System.out.printf("%-30s %s\n", key + ":", "***********");
				  else {
					  System.out.printf("%-30s %s\n", key + ":", value);  
				  }
				  
			}
		}

		if(!service.isEmpty()) {

			System.out.println("\nSERVICES\n");
			
			for(Map.Entry<String,String> entry : service.entrySet()) {
	
				  String key = entry.getKey();
				  String value = entry.getValue();
				  if(key.contains("password"))
					  System.out.printf("%-30s %s\n", key + ":", "***********");
				  else {
					  System.out.printf("%-30s %s\n", key + ":", value);  
				  }
				  
			}
		}
		
		System.out.println("-----------------------------------------------------------\n");

	}
	
	public Certificate[] downloadCertificate(String url) throws IOException {

		Certificate[] peercertificates = null;

        // create http response certificate interceptor
        HttpResponseInterceptor certificateInterceptor = (httpResponse, context) -> {
            ManagedHttpClientConnection routedConnection = (ManagedHttpClientConnection)context.getAttribute(HttpCoreContext.HTTP_CONNECTION);
            SSLSession sslSession = routedConnection.getSSLSession();
            if (sslSession != null) {

                // get the server certificates from the {@Link SSLSession}
                java.security.cert.Certificate[] certificates = sslSession.getPeerCertificates();

                // add the certificates to the context, where we can later grab it from
                context.setAttribute(PEER_CERTIFICATES, certificates);
            }
        };

        // create closable http client and assign the certificate interceptor
        CloseableHttpClient httpClient = HttpClients
                .custom()
                .addInterceptorLast(certificateInterceptor)
                .build();

        try {

            // make HTTP GET request to resource server
            HttpGet httpget = new HttpGet(url);
            System.out.println("Downloading certificates " + httpget.getRequestLine());

            // create http context where the certificate will be added
            HttpContext context = new BasicHttpContext();
            httpClient.execute(httpget, context);

            // obtain the server certificates from the context
            peercertificates = (Certificate[])context.getAttribute(PEER_CERTIFICATES);

            // loop over certificates and print meta-data
            for (Certificate certificate : peercertificates){
                X509Certificate real = (X509Certificate) certificate;
                System.out.println("----------------------------------------");
                System.out.println("Type: " + real.getType());
                System.out.println("Signing Algorithm: " + real.getSigAlgName());
                System.out.println("IssuerDN Principal: " + real.getIssuerX500Principal());
                System.out.println("SubjectDN Principal: " + real.getSubjectX500Principal());
                System.out.println("Not After: " + DateUtils.formatDate(real.getNotAfter(), "dd-MM-yyyy"));
                System.out.println("Not Before: " + DateUtils.formatDate(real.getNotBefore(), "dd-MM-yyyy"));
            }

        } finally {
            // close httpclient
            httpClient.close();
        }

        return peercertificates;
		
	}


	public Certificate getCertificate(String keyStorePath, String certificateName) {

    	try {
    		//load keystore
    		File trustStore = new File(keyStorePath);
    		InputStream is = new FileInputStream(trustStore);
    		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
	        String password = "supersecret";
	        keystore.load(is, password.toCharArray());
	        Certificate certificate = keystore.getCertificate(certificateName);
	        is.close();
			
	        // Save the new keystore contents
			FileOutputStream out = new FileOutputStream(trustStore);
	        keystore.store(out, password.toCharArray());
	        out.close();
	    
	        return certificate;
	        
		}catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;   	

	}
	
	
	
	public Map<String,Certificate> importCertificate(String keyStorePath, Certificate[] certificates) {

        System.out.println("Importing certificates");
        Map<String,Certificate> certificateMap = new HashMap<String,Certificate>();

    	try {
    		//load keystore
    		File trustStore = new File(keyStorePath);
    		InputStream is = new FileInputStream(trustStore);
    		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
	        String password = "supersecret";
	        keystore.load(is, password.toCharArray());
	        is.close();
	
	        // Add the certificate to the store
            for (Certificate certificate : certificates){            	
                X509Certificate real = (X509Certificate) certificate;
                System.out.println("----------------------------------------");
                System.out.println("Type: " + real.getType());
                System.out.println("Signing Algorithm: " + real.getSigAlgName());
                System.out.println("IssuerDN Principal: " + real.getIssuerX500Principal());
                System.out.println("SubjectDN Principal: " + real.getSubjectX500Principal());
                System.out.println("Not After: " + DateUtils.formatDate(real.getNotAfter(), "dd-MM-yyyy"));
                System.out.println("Not Before: " + DateUtils.formatDate(real.getNotBefore(), "dd-MM-yyyy"));
                String alias = UUID.randomUUID().toString();
                certificateMap.put(alias, certificate);
                keystore.setCertificateEntry(alias, certificate);
                System.out.println("original alias:" + alias);
                System.out.println("cert alias" + keystore.getCertificateAlias(certificate));
                
            }
			
	        // Save the new keystore contents
			FileOutputStream out = new FileOutputStream(trustStore);
	        keystore.store(out, password.toCharArray());
	        out.close();
	        
		}catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

    	return certificateMap;
	}

	public void deleteCertificate(String keyStorePath, String certificateName) {

    	try {
    		//load keystore
    		File trustStore = new File(keyStorePath);
    		InputStream is = new FileInputStream(trustStore);
    		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
	        String password = "supersecret";
	        keystore.load(is, password.toCharArray());
	        keystore.deleteEntry(certificateName);
	        is.close();
			
	        // Save the new keystore contents
			FileOutputStream out = new FileOutputStream(trustStore);
	        keystore.store(out, password.toCharArray());
	        out.close();
	        
		}catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
