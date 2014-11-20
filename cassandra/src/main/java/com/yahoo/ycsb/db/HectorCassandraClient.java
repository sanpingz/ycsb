/**
 * Copyright (c) 2013 Thumbtack Technology, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */

package com.yahoo.ycsb.db;

import com.yahoo.ycsb.*;
import me.prettyprint.cassandra.model.ConfigurableConsistencyLevel;
import me.prettyprint.cassandra.model.HSlicePredicate;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.template.ColumnFamilyResult;
import me.prettyprint.cassandra.service.template.ColumnFamilyTemplate;
import me.prettyprint.cassandra.service.template.ColumnFamilyUpdater;
import me.prettyprint.cassandra.service.template.ThriftColumnFamilyTemplate;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.HConsistencyLevel;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;
import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.Mutation;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * Cassandra client for YCSB framework which uses Hector instead of Thrift client
 */
public class HectorCassandraClient extends DB {

    static Random random = new Random();

    public static final int OK = 0;
    public static final int ERROR = -1;

    public static final String OPERATION_RETRY_PROPERTY = "cassandra.operationretries";
    public static final String OPERATION_RETRY_PROPERTY_DEFAULT = "1";

    public static final String USERNAME_PROPERTY = "cassandra.username";
    public static final String PASSWORD_PROPERTY = "cassandra.password";

    public static final String COLUMN_FAMILY_PROPERTY = "cassandra.columnfamily";
    public static final String COLUMN_FAMILY_PROPERTY_DEFAULT = "data";

    public static final String READ_CONSISTENCY_LEVEL_PROPERTY = "cassandra.readconsistencylevel";
    public static final String READ_CONSISTENCY_LEVEL_PROPERTY_DEFAULT = "ONE";

    public static final String WRITE_CONSISTENCY_LEVEL_PROPERTY = "cassandra.writeconsistencylevel";
    public static final String WRITE_CONSISTENCY_LEVEL_PROPERTY_DEFAULT = "ONE";

    public static final String SCAN_CONSISTENCY_LEVEL_PROPERTY = "cassandra.scanconsistencylevel";
    public static final String SCAN_CONSISTENCY_LEVEL_PROPERTY_DEFAULT = "ONE";

    public static final String DELETE_CONSISTENCY_LEVEL_PROPERTY = "cassandra.deleteconsistencylevel";
    public static final String DELETE_CONSISTENCY_LEVEL_PROPERTY_DEFAULT = "ONE";

    public static final String CLUSTER_NAME = "test-cluster";

    int operationRetries;
    String columnFamily;

    Cluster cluster;
    Keyspace keyspace;
    ColumnFamilyTemplate<String, String> template;

    boolean _debug = false;

    String _table = "";
    Exception errorexception = null;

    List<Mutation> mutations = new ArrayList<Mutation>();
    Map<String, List<Mutation>> mutationMap = new HashMap<String, List<Mutation>>();
    Map<ByteBuffer, Map<String, List<Mutation>>> record = new HashMap<ByteBuffer, Map<String, List<Mutation>>>();

    ColumnParent parent;
 
    HConsistencyLevel readConsistencyLevel = HConsistencyLevel.ONE;
    HConsistencyLevel writeConsistencyLevel = HConsistencyLevel.ONE;
    HConsistencyLevel scanConsistencyLevel = HConsistencyLevel.ONE;
    HConsistencyLevel deleteConsistencyLevel = HConsistencyLevel.ONE;

    /**
     * Initialize any state for this DB. Called once per DB instance; there is one
     * DB instance per client thread.
     */
    public void init() throws DBException {
        String hosts = getProperties().getProperty("hosts");
        if (hosts == null) {
            throw new DBException("Required property \"hosts\" missing for CassandraClient");
        }

        this.columnFamily = getProperties().getProperty(COLUMN_FAMILY_PROPERTY, COLUMN_FAMILY_PROPERTY_DEFAULT);

        this.operationRetries = Integer.parseInt(getProperties().getProperty(OPERATION_RETRY_PROPERTY,
                OPERATION_RETRY_PROPERTY_DEFAULT));

        String username = getProperties().getProperty(USERNAME_PROPERTY);
        String password = getProperties().getProperty(PASSWORD_PROPERTY);

        this.readConsistencyLevel = HConsistencyLevel.valueOf(
                getProperties().getProperty(READ_CONSISTENCY_LEVEL_PROPERTY, READ_CONSISTENCY_LEVEL_PROPERTY_DEFAULT));
        this.writeConsistencyLevel = HConsistencyLevel.valueOf(
                getProperties().getProperty(WRITE_CONSISTENCY_LEVEL_PROPERTY, WRITE_CONSISTENCY_LEVEL_PROPERTY_DEFAULT));
        this.scanConsistencyLevel = HConsistencyLevel.valueOf(
                getProperties().getProperty(SCAN_CONSISTENCY_LEVEL_PROPERTY, SCAN_CONSISTENCY_LEVEL_PROPERTY_DEFAULT));
        this.deleteConsistencyLevel = HConsistencyLevel.valueOf(
                getProperties().getProperty(DELETE_CONSISTENCY_LEVEL_PROPERTY, DELETE_CONSISTENCY_LEVEL_PROPERTY_DEFAULT));

        this._debug = Boolean.parseBoolean(getProperties().getProperty("debug", "false"));

        String[] allhosts = hosts.split(",");
        String myhost = allhosts[random.nextInt(allhosts.length)];

        this.cluster = HFactory.getOrCreateCluster(CLUSTER_NAME, myhost);

        if (username != null && password != null) {
            //TODO: login by username and password
        }
    }

    /**
     *  Initializes keyspace by the specified table name.
     *  If this table is already initialized, does nothing.
     */
    private void initKeyspace(String table) {
        if (this.keyspace != null && this.keyspace.getKeyspaceName().equals(table)) {
            return;
        }
        ConfigurableConsistencyLevel consistency = new ConfigurableConsistencyLevel();
        consistency.setDefaultReadConsistencyLevel(readConsistencyLevel);
        consistency.setDefaultWriteConsistencyLevel(writeConsistencyLevel);
        //TODO: consistency levels for scan and delete
        this.keyspace = HFactory.createKeyspace(table, this.cluster, consistency);
        this.template =
                new ThriftColumnFamilyTemplate<String, String>(this.keyspace,
                        this.columnFamily,
                        StringSerializer.get(),
                        StringSerializer.get());
    }

    /**
     * Cleanup any state for this DB. Called once per DB instance; there is one DB
     * instance per client thread.
     */
    public void cleanup() throws DBException {
        //TODO: should anything done here?
    }

    /**
     * Read a record from the database. Each field/value pair from the result will
     * be stored in a HashMap.
     *
     * @param table
     *          The name of the table
     * @param key
     *          The record key of the record to read.
     * @param fields
     *          The list of fields to read, or null for all of them
     * @param result
     *          A HashMap of field/value pairs for the result
     * @return Zero on success, a non-zero error code on error
     */
    public int read (String table, String key, Set<String> fields, HashMap<String, ByteIterator> result) {
        initKeyspace(table);

        for (int i = 0; i < operationRetries; i++) {
            if (_debug) {
                System.out.print("Reading key: " + key);
            }

            try {

                ColumnFamilyResult<String, String> row;
                if (fields == null) {
                    row = template.queryColumns(key);
                } else {
                    List<String> columns = new ArrayList(fields);
                    row = template.queryColumns(key, columns);
                }

                if (!row.hasResults()) {
                    return ERROR;
                }

                Collection<String> names = row.getColumnNames();

                for (String name : names) {
                    byte[] value = row.getByteArray(name);
                    result.put(name, new ByteArrayByteIterator(value));
                    if (_debug) {
                        System.out.print("(" + name + "=" + value + ")");
                    }
                }

                return OK;

            } catch (Exception e) {
                errorexception = e;
            }

        }
        //TODO make error logging level configurable
        //errorexception.printStackTrace();
        //errorexception.printStackTrace(System.out);
        System.err.println(errorexception);
        return ERROR;
    }

    /**
     * Perform a range scan for a set of records in the database. Each field/value
     * pair from the result will be stored in a HashMap.
     *
     * @param table
     *          The name of the table
     * @param startkey
     *          The record key of the first record to read.
     * @param recordcount
     *          The number of records to read
     * @param fields
     *          The list of fields to read, or null for all of them
     * @param result
     *          A Vector of HashMaps, where each HashMap is a set field/value
     *          pairs for one record
     * @return Zero on success, a non-zero error code on error
     */
    public int scan (String table, String startkey, int recordcount, Set<String> fields,
                    Vector<HashMap<String, ByteIterator>> result) {
        initKeyspace(table);

        for (int i = 0; i < operationRetries; i++) {

            if (_debug) {
                System.out.println("Scanning startkey: " + startkey);
            }

            try {
                HSlicePredicate<String> predicate = new HSlicePredicate<String>(this.template.getTopSerializer());
                if (fields != null) {
                    //TODO: verify without set column names all columns will return
                    predicate.setColumnNames(fields);
                }
                predicate.setStartOn(startkey);
                predicate.setCount(recordcount);

                ColumnFamilyResult<String, String> rows = template.queryColumns(startkey, predicate);

                HashMap<String, ByteIterator> tuple;
                while (rows.hasNext()) {
                    tuple = new HashMap<String, ByteIterator>();

                    Collection<String> names = rows.getColumnNames();
                    for (String name : names) {
                        byte[] value = rows.getByteArray(name);
                        tuple.put(name, new ByteArrayByteIterator(value));
                        if (_debug) {
                            System.out.print("(" + name + "=" + value + ")");
                        }
                    }

                    result.add(tuple);
                }

                return OK;

            } catch (Exception e) {
                errorexception = e;
            }
        }
        //TODO make error logging level configurable
        //errorexception.printStackTrace();
        //errorexception.printStackTrace(System.out);
        System.err.println(errorexception);
        return ERROR;
    }

    /**
     * Update a record in the database. Any field/value pairs in the specified
     * values HashMap will be written into the record with the specified record
     * key, overwriting any existing values with the same field name.
     *
     * @param table
     *          The name of the table
     * @param key
     *          The record key of the record to write.
     * @param values
     *          A HashMap of field/value pairs to update in the record
     * @return Zero on success, a non-zero error code on error
     */
    public int update(String table, String key, HashMap<String, ByteIterator> values) {
        return insert(table, key, values);
    }

    /**
     * Insert a record in the database. Any field/value pairs in the specified
     * values HashMap will be written into the record with the specified record
     * key.
     *
     * @param table
     *          The name of the table
     * @param key
     *          The record key of the record to insert.
     * @param values
     *          A HashMap of field/value pairs to insert in the record
     * @return Zero on success, a non-zero error code on error
     */
    public int insert(String table, String key, HashMap<String, ByteIterator> values) {
        initKeyspace(table);

        for (int i = 0; i < operationRetries; i++) {
            if (_debug) {
                System.out.println("Inserting key: " + key);
            }

            try {

                ColumnFamilyUpdater<String, String> updater = template.createUpdater(key);
                for (Map.Entry<String, ByteIterator> value : values.entrySet()) {
                    updater.setByteArray(value.getKey(), value.getValue().toArray());
                }
                template.update(updater);

                return OK;
            } catch (Exception e) {
                errorexception = e;
            }
        }

        //TODO make error logging level configurable
        //errorexception.printStackTrace();
        //errorexception.printStackTrace(System.out);
        System.err.println(errorexception);
        return ERROR;
    }

    /**
     * Delete a record from the database.
     *
     * @param table
     *          The name of the table
     * @param key
     *          The record key of the record to delete.
     * @return Zero on success, a non-zero error code on error
     */
    public int delete(String table, String key) {
        initKeyspace(table);

        for (int i = 0; i < operationRetries; i++) {
            try {
                if (_debug) {
                    System.out.println("Delete key: " + key);
                }

                template.deleteRow(key);

                return OK;
            } catch (Exception e) {
                errorexception = e;
            }
        }
        //TODO make error logging level configurable
        //errorexception.printStackTrace();
        //errorexception.printStackTrace(System.out);
        System.err.println(errorexception);
        return ERROR;
    }

    public static void main(String[] args) {
        HectorCassandraClient cli = new HectorCassandraClient();

        Properties props = new Properties();

        props.setProperty("hosts", args[0]);
        cli.setProperties(props);

        try {
            cli.init();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

        HashMap<String, ByteIterator> vals = new HashMap<String, ByteIterator>();
        vals.put("age", new StringByteIterator("57"));
        vals.put("middlename", new StringByteIterator("bradley"));
        vals.put("favoritecolor", new StringByteIterator("blue"));
        int res = cli.insert("usertable", "BrianFrankCooper", vals);
        System.out.println("Result of insert: " + res);

        HashMap<String, ByteIterator> result = new HashMap<String, ByteIterator>();
        HashSet<String> fields = new HashSet<String>();
        fields.add("middlename");
        fields.add("age");
        fields.add("favoritecolor");
        res = cli.read("usertable", "BrianFrankCooper", null, result);
        System.out.println("Result of read: " + res);
        for (String s : result.keySet()) {
            System.out.println("[" + s + "]=[" + result.get(s) + "]");
        }

        res = cli.delete("usertable", "BrianFrankCooper");
        System.out.println("Result of delete: " + res);
    }

}
