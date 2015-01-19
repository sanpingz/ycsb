/**
 * Redis client binding for YCSB.
 *
 * All YCSB records are mapped to a Redis *hash field*.  For scanning
 * operations, all keys are saved (by an arbitrary hash) in a sorted set.
 */

package com.yahoo.ycsb.db;
import com.yahoo.ycsb.DB;
import com.yahoo.ycsb.DBException;
import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.StringByteIterator;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.HashSet;
import java.util.Vector;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.exceptions.JedisException;

public class RedisCluster extends DB {

    private JedisCluster jedis;

    public static final String HOST_PROPERTY = "redis.host";
    public static final String HOST_PROPERTY_DEFAULT = "127.0.0.1";
    public static final String PORT_PROPERTY = "redis.port";
    public static final int PORT_PROPERTY_DEFAULT = Protocol.DEFAULT_PORT;
    public static final String PASSWORD_PROPERTY = "redis.password";

    public void init() throws DBException {
        Properties props = getProperties();
		String host = props.getProperty(HOST_PROPERTY, HOST_PROPERTY_DEFAULT);
        int port = Integer.parseInt(props.getProperty(PORT_PROPERTY, Integer.toString(Protocol.DEFAULT_PORT)));

		Set<HostAndPort> jedisClusterNodes = new HashSet<HostAndPort>();
		//Jedis Cluster will attempt to discover cluster nodes automatically
		jedisClusterNodes.add(new HostAndPort(host, port));
		try {
			jedis = new JedisCluster(jedisClusterNodes);
		} catch (Exception e) {
			throw new DBException(String.format("Failed to init cluster through %s:%d.", host, port));
		}

        //jedis = new Jedis(host, port);
        //jedis.connect();

        String password = props.getProperty(PASSWORD_PROPERTY);
        if (password != null) {
            jedis.auth(password);
        }
    }

    public void cleanup() throws DBException {
        jedis.close();
    }

    /* Calculate a hash for a key to store it in an index.  The actual return
     * value of this function is not interesting -- it primarily needs to be
     * fast and scattered along the whole space of doubles.  In a real world
     * scenario one would probably use the ASCII values of the keys.
     */
    private double hash(String key) {
        return key.hashCode();
    }

    //XXX jedis.select(int index) to switch to `table`

    @Override
    public int read(String table, String key, Set<String> fields,
            HashMap<String, ByteIterator> result) {
        if (fields == null) {
            StringByteIterator.putAllAsByteIterators(result, jedis.hgetAll(key));
        }
        else {
            String[] fieldArray = (String[])fields.toArray(new String[fields.size()]);
            List<String> values = jedis.hmget(key, fieldArray);

            Iterator<String> fieldIterator = fields.iterator();
            Iterator<String> valueIterator = values.iterator();

            while (fieldIterator.hasNext() && valueIterator.hasNext()) {
                result.put(fieldIterator.next(),
			   new StringByteIterator(valueIterator.next()));
            }
            assert !fieldIterator.hasNext() && !valueIterator.hasNext();
        }
        return result.isEmpty() ? 1 : 0;
    }

    @Override
    public int delete(String table, String key) {
        try{
            return jedis.del(key) == 0 ? 1 : 0;
        } catch (JedisException e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }

    @Override
    public int update(String table, String key, HashMap<String, ByteIterator> values) {
        try{
            return jedis.hmset(key, StringByteIterator.getStringMap(values)).equals("OK") ? 0 : 1;
        } catch (JedisException e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }

    @Override
    public int insert(String table, String key, HashMap<String, ByteIterator> values) {
        return update(table, key, values);
    }

    @Override
    public int scan(String table, String startkey, int recordcount,
            Set<String> fields, Vector<HashMap<String, ByteIterator>> result) {
        //don't support scan
        return -1;
    }

}
