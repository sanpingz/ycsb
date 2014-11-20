import hosts

databases = {

    'aerospike' : {
        'name': 'aerospike',    #name of the database (used to form the logfile name)
        'home': '/local/logs/aerospike',     #database home, to put logs there
        'command': 'aerospike', #database name to pass to ycsb command
        'properties': {         #properties to pass to ycsb command as -p name=value
            'host': 'node01',  #database connection params
            'port': 3000,
            'ns': 'test',
            'set': 'demo',
        },
        'status': {
            'hosts': hosts.env.roledefs['server'][0:1],     #hosts on which to run the status command
            'command': '/local/apps/aerospike/bin/asmonitor -e info'  #the status command
        },
        'failover': {
            'files': [],
            'kill_command': '/usr/bin/killall -9 asd',
            'start_command': '/etc/init.d/aerospike start',
        },
    },

    'couchbase' : {
        'name': 'couchbase',
        'home': '/local/logs/couchbase',
        'command': 'couchbase',
        'properties': {
            'couchbase.hosts': hosts.env.roledefs['server']
            'couchbase.bucket': 'test',
            'couchbase.ddocs': '',
            'couchbase.views': '',
            'couchbase.user': '',
            'couchbase.password': '',
            'couchbase.opTimeout': 1000,
            #'couchbase.failureMode': 'Retry',
            #'couchbase.persistTo': 'ONE',
            #'couchbase.replicateTo': 'ONE',
            'couchbase.checkOperationStatus': 'true',
            },
        'failover': {
            'files': [],
            'kill_command': '',
            'start_command': '',
        }
    },

    'cassandra' : {
        'name': 'cassandra',
        'home': '/local/logs/cassandra',
        'command': 'cassandra',
        'properties': {
            'hosts': hosts.env.roledefs['server'],
            'cassandra.readconsistencylevel': 'ONE',
            'cassandra.writeconsistencylevel': 'ONE', #ALL-sync/ONE-async
        },
        'failover': {
            'files': [],
            'kill_command': '/usr/bin/killall -9 java',
            'start_command': '/local/apps/cassandra/bin/cassandra',
        },
    },

    'mongodb' : {
        'name': 'mongodb',
        'home': '/local/logs/mongodb',        
        'command': 'mongodb',
        'properties': {
            'mongodb.url': 'mongodb://node01:27018',
            'mongodb.database': 'test',
            'mongodb.writeConcern': 'normal',
            #'mongodb.writeConcern': 'replicas_safe',
            'mongodb.readPreference': 'primaryPreferred',
        },
        'configdb': 'node02',
        'failover': {
            'files': [],
            'kill_command': '/usr/bin/killall -9 mongod',
            'start_command': '~/mongo_run.sh',
        },
    },

    'hbase' : {
        'name': 'hbase',
        'home': '/local/logs/hbase',
        'command': 'hbase',
        'properties': {
            'columnfamily': 'test',
        }
    },


    'basic' : { #fake database
        'name': 'basic',
        'home': '/local/logs/basic',
        'command': 'basic',
        'properties': {
            'basicdb.verbose': 'false',
        }
    },

}
