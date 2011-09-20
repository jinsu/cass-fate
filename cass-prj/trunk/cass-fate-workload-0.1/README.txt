
[[ 1. Notes ]]


If you haven't read the cassandra-fate-system-x.y/FI-README.txt file, then 
stop reading this. Read that FI-README first. Then continue here.


[[ 2. How to Setup the Configuration ]]

To build the cassandra-fate-system-x.y, you need to setup some paths 
to your java and ant jar files. These are the steps:

- Open build.xml

- Search for "please modify"

- Modify the property locations of the files (don't modify the
  property names).  

  The "cassandra.root.dir" is the path to your
  cassandra-fate-system-x.y directory.  If you put
  cassandra-fate-system-x.y and cassandra-fate-workload-x.y in
  the same directory, then the location is
  "../cassandra-fate-system-x.y".


[[ How to Build ]]


You don't need to build -- at this point, the build process is part of
the run process. Just follow the instruction in the next section.


[[ How to Run ]]

Simply, run these steps:

 % make kill
 % ant
 % make kill

   (This will run the experiment specified in wl-config.xml file.  
   You could see /tmp/fi/expResult/ folder to
   see how many experiments have been run so far.

   You can select which experiment to run by changing the content of wl-config.xml.
   wl-config.xml file specifies what experiment will be run by the workload.

   To stop just hit control-C and then make kill).

 % ls /tmp/fi/expResult
   (if you see exp-00XX directories, then you've succesfully run our
   program.)


To run a sample workload experiment, you can try the below:

 % cp wl-config-readrepair3-all.xml wl-config.xml
 % make kill
 % ant
 % make kill

     IMPORTANT NOTE: always run "make kill" before and after running
     each of these policies.

The above should run read repair experiment with readrepair3 filter, where we
inject specific three failures (as defined in the configuration file).

In every experiment directory, you will see some file, such as
fsnN-<failureid>.txt.  N represents the i-th injected failure. If you
inject two failures (see the next section below), then you will find
fsn1-<failureid.txt> and fsn2-<failureid.txt>.  Open the files(s), and
you will see the failure that we injected (i.e. on what I/O call,
what's the stack trace look like, and other information).


    IMPORTANT NOTE: "make kill" will kill all Java programs that are
    running.  Our apology. At this point, we do not provide a clean
    way to kill Java programs run by this framework.


If everything is smooth up to this point, then let's try different experiments.


[[ All experiments ]]

We have provided some configuration files that you could try. Basically, the
configuration files written in xml format, provides suggested failure numbers
and filters for each experiment:

 % cp wl-config-EXPERIMENT_NAME-CONSISTENCY.xml wl-config.xml
    EXPERIMENT_NAME can be 
 {readrepair1, readrepair2, readrepair3, insertion1, insertion2}
    CONSISTENCY can be
 {all, quorum}
    example
 % cp wl-config-insertion1-all.xml wl-config.xml

 % cp wl-config-insertion2-all.xml wl-config.xml

 % cp wl-config-readrepair1-all.xml wl-config.xml

 % cp wl-config-readrepair2-all.xml wl-config.xml

 % cp wl-config-readrepair3-all.xml wl-config.xml

 % cp wl-config-insertion1-quorum.xml wl-config.xml

 % cp wl-config-readrepair1-quorum.xml wl-config.xml
 ...
 you get the pattern.

Here are the explanations:
  
All experiments are Brute-force policy. This runs all possible failure experiments.
ReadRepair experiment:
Injects maximum three failures during the read and read repair operation.
The workload will intercept digest messages and data messages being sent from
replica nodes and corrupt the messages. It will also crash some replica nodes. 
Corrupting the messages will force read repair mechanism among the replica nodes.

Insertion exepriment:
Injects one failure during the write operation.
The workload will crash a replica node when the node tries to
send a callback messages for writing an entry into its storage file/memtable.
 
[[ More Documents ]]

For more, please read our papers available on our project page.

