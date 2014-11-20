from fabric.api import env
import pytz

CLIENT_PREFIX = "client"
SERVER_PREFIX = "node"
CLIENT_NUM = 11
SERVER_NUM = 20

#user name to ssh to hosts
env.user = 'root'
#user password (the better is to use pubkey authentication)
env.password = 'iscuser'

env.show = ['debug']

env.roledefs = {
    #list of client hosts
    'client': ["%s%02d" % (CLIENT_PREFIX,x) for x in range(1,CLIENT_NUM+1)],

    #list of server hosts
    'server': ["%s%02d" % (SERVER_PREFIX,x) for x in range(1,SERVER_NUM+1)],

    #list of all available client hosts
    'all_client': ["%s%02d" % (CLIENT_PREFIX,x) for x in range(1,CLIENT_NUM+1)],
}

#hosts timezone (required to correctly schedule ycsb tasks)
timezone = pytz.timezone('Asia/Shanghai')
