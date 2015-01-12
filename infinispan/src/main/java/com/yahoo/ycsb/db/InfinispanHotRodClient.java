package com.yahoo.ycsb.db;

import com.yahoo.ycsb.DB;
import com.yahoo.ycsb.DBException;
import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.StringByteIterator;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.logging.Log;
import org.infinispan.client.hotrod.logging.LogFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Properties;

/**
 * This is a client implementation for Infinispan 5.x.
 *
 * Some settings:
 *
 * @author Manik Surtani (manik AT jboss DOT org)
 */
public class InfinispanHotRodClient extends DB {

   private static final int OK = 0;
   private static final int ERROR = -1;
   private static final int NOT_FOUND = -2;

   private String cacheName;

   private RemoteCacheManager cacheManager;
   private RemoteCache<String, Map<String, String>> cache;

   private static final Log logger = LogFactory.getLog(InfinispanHotRodClient.class);
   private Properties props;

   public static final String CACHE_NAME_PROPERTY = "infinispan.cacheName";
   public static final String CACHE_NAME_PROPERTY_DEFAULT = "default";

   public static final String MAX_RETRIES_PROPERTY = "recordcount";
   public static final String MAX_RETRIES_PROPERTY_DEFAULT = "1000";


   public void init() throws DBException {
      props = getProperties();
	  cacheName = props.getProperty(CACHE_NAME_PROPERTY, CACHE_NAME_PROPERTY_DEFAULT);
      String server_list = "";
      String hosts = props.getProperty("hosts", "127.0.0.1");	//"127.0.0.1:11222"
      for(String host: hosts.split(",|;")) {
         server_list += host+";";
      }
      server_list = server_list.substring(0, server_list.length()-1);

      Configuration config = new ConfigurationBuilder()
         .addServers(server_list)
         .maxRetries(Integer.parseInt(props.getProperty(MAX_RETRIES_PROPERTY, MAX_RETRIES_PROPERTY_DEFAULT)))
         .build();
      try {
         cacheManager = new RemoteCacheManager(config);
		 cache = cacheManager.getCache(cacheName);
      } catch (Exception e) {
         throw new DBException(e);
      }
      //System.out.println(cacheManager.getProperties());
   }

   public void cleanup() {
      cacheManager.stop();
      cacheManager = null;
   }

   public int read(String table, String key, Set<String> fields, HashMap<String, ByteIterator> result) {
      try {
         //RemoteCache<String, Map<String, String>> cache = cacheManager.getCache(cacheName);
         Map<String, String> row = cache.get(key);
         if (row != null) {
            result.clear();
            if (fields == null || fields.isEmpty()) {
               StringByteIterator.putAllAsByteIterators(result, row);
            } else {
               for (String field : fields) result.put(field, new StringByteIterator(row.get(field)));
            }
         }
         return OK;
      } catch (Exception e) {
         return ERROR;
      }
   }

   public int scan(String table, String startkey, int recordcount, Set<String> fields, Vector<HashMap<String, ByteIterator>> result) {
      logger.warn("Infinispan does not support scan semantics");
      return OK;
   }

   public int update(String table, String key, HashMap<String, ByteIterator> values) {
      return insert(table, key, values);
   }

   public int insert(String table, String key, HashMap<String, ByteIterator> values) {
      try {
         Map<String, String> row = StringByteIterator.getStringMap(values);
         //cacheManager.getCache(cacheName).withFlags(Flag.FORCE_RETURN_VALUE).put(key, row);
         //cacheManager.getCache(cacheName).put(key, row);
		 cache.put(key, row);
         return OK;
      } catch (Exception e) {
         //System.err.println(e);
         return ERROR;
      }
   }

   public int delete(String table, String key) {
      try {
         //cacheManager.getCache(cacheName).withFlags(Flag.FORCE_RETURN_VALUE).remove(key);
         //cacheManager.getCache(cacheName).remove(key);
		 cache.remove(key);
         return OK;
      } catch (Exception e) {
         return ERROR;
      }
   }
}
