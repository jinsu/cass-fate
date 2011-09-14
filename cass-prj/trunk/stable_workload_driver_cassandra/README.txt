
[[ 1. Notes ]]


If you haven't read the cass-prefail-x.x-system/FI-README.txt file, then 
stop reading this. Read that FI-README first. Then continue here.


[[ 2. How to Setup the Configuration ]]

To build the cass-prefail-system, you need to setup some paths 
to your java and ant jar files. These are the steps:

- Open build.xml

- Search for "please modify"

- Modify the property locations of the files (don't modify the
  property names).  

  The "cassandra.root.dir" is the path to your
  cass-prefail-system directory.  If you put
  cass-prefail-system and cass-prefail-workload in
  the same directory, then the location is
  "../cass-prefail-system".


[[ How to Build ]]


You don't need to build -- at this point, the build process is part of
the run process. Just follow the instruction in the next section.


[[ How to Run ]]

Simply, run these steps:

 % make kill
 % ./scripts/run-one-fail.py 

   (This will only run 2 or 3 experiments, where in each experiment we
   just run one failure.  You could see /tmp/fi/expResult/ folder to
   see how many experiments have been run so far.  To stop just hit
   control-C and then make kill).

 % make kill

 % ls /tmp/fi/expResult
   (if you see exp-00XX directories, then you've succesfully run our
   program.)


To run two failures per experiment, you can try this policy below:

 % make kill
 % ./scripts/run-two-fail.py

This policy should run 2 experiments, where in each experiment we
inject specific two failures (as defined in the policy).


Finally, if you want to run all combinations of two failures,
you can try this policy:

 % ./scripts/run-brute.py 2

This will run a very long time.  Again, you could see
/tmp/fi/expResult/ folder to see how many experiments have been run so
far.  To stop just hit control-C and then make kill.


In every of the experiment directory, you will see some file, such as
fsnN-<failureid>.txt.  N represents the i-th injected failure. If you
inject two failures (see the next section below), then you will find
fsn1-<failureid.txt> and fsn2-<failureid.txt>.  Open the files(s), and
you will see the failure that we injected (i.e. on what I/O call,
what's the stack trace look like, and other information).


    IMPORTANT NOTE: "make kill" will kill all Java programs that are
    running.  Our apology. At this point, we do not provide a clean
    way to kill Java programs run by this framework.


If everything is smooth up to this point, then let's move on to the
next section.



[[ Test Program and Pruning Policies ]]


We have provided some policies that you could try. Basically, the
policy is written in a python script, and can be run in this format:

% scripts/run-SOMEPOLICY.py  N

where N is the number of failures that you want to inject per experiment.
We have provided six policies that you could try (explained below).
For example, you could try running these policies:


     IMPORTANT NOTE: always run "make kill" before and after running
     each of these policies.

 % scripts/run-brute.py 2

 % scripts/run-newsrc.py 2

 % scripts/run-rec-src.py 2

 % scripts/run-rec-src-stack.py 2
 
 % scripts/run-rec-src-node.py 2
 
 % scripts/run-rec-all.py 2


Here are the explanations:

  
a) Brute-force policy. This runs all possible failure experiments.
   Run ./scripts/run-brute.py <N> for the policy.
   (N is the maximum number of injections in each run)

 
b) New source location policy. This runs experiments that cover 
   unexplored source locations.
   Run ./scripts/run-newsrc.py <N> for the policy.
   (N is the maximum number of injections in each run)

  
c) Recovery clustering policy using source locations. This clusters 
   failure experiments according to their recovery behavior that 
   is characterized using source locations of failure injection tasks.
   Run ./scripts/run-rec-src.py <N> for the policy.
   (N is the maximum number of injections in each run)

  
d) Recovery clustering policy using source locations and stacks. This 
   clusters failure experiments according to their recovery behavior that 
   is characterized using source locations and stacks of failure injection 
   tasks.
   Run ./scripts/run-rec-src-stack.py <N> for the policy.
   (N is the maximum number of injections in each run)


e) Recovery clustering policy using source locations and nodes. This 
   clusters failure experiments according to their recovery behavior that 
   is characterized using source locations and nodes of failure injection 
   tasks.
   Run ./scripts/run-rec-src-node.py <N> for the policy.
   (N is the maximum number of injections in each run)


f) Recovery clustering policy using various fields of failure injection 
   tasks. 

   This clusters failure experiments according to their recovery behavior 
   that is characterized using source locations, stacks, nodes, target 
   nodes, target IO types, RPC contexts, and file types of failure injection
   tasks. 

   Run ./scripts/run-rec-all.py <N> for the policy.
   (N is the maximum number of injections in each run)

[[ More Documents ]]

For more, please read our papers available on our project page.

