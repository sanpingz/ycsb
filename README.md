Yahoo! Cloud System Benchmark (YCSB)
====================================
[![Build Status](https://travis-ci.org/sanpingz/ycsb.svg?branch=master)](https://travis-ci.org/sanpingz/ycsb)

Links
-----
http://research.yahoo.com/Web_Information_Management/YCSB/  
http://wiki.github.com/brianfrankcooper/YCSB/  
https://github.com/couchbaselabs/YCSB  
https://github.com/thumbtack-technology/ycsb  
https://github.com/aerospike/ycsb  
ycsb-users@yahoogroups.com  
mceiba@gmail.com  

Getting Started
---------------

1. Download the latest release of YCSB:

    ```sh
    wget https://github.com/sanpingz/ycsb/archive/0.1.5.tar.gz
    tar xfvz 0.1.5.tar.gz
    cd ycsb-0.1.5
    ```
    
2. Set up a database to benchmark. There is a README file under each binding 
   directory.

3. Run YCSB command. 
    
    ```sh
    bin/ycsb load basic -P workloads/workloada
    bin/ycsb run basic -P workloads/workloada
    ```

  Running the `ycsb` command without any argument will print the usage. 
   
  See https://github.com/brianfrankcooper/YCSB/wiki/Running-a-Workload
  for a detailed documentation on how to run a workload.

  See https://github.com/brianfrankcooper/YCSB/wiki/Core-Properties for 
  the list of available workload properties.
