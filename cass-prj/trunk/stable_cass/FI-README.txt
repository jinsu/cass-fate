


1. Search for "please modify" in these two files
    - bin/cassandra.in.sh 
    - fi-build.xml
   and change those entries if needed.

2. bin/extra-ifconfig-up.sh

   (Just run this once, after machine reboots.  This is for just
   aliasing some IP addresses to localhost.  But need ROOT permission,
   unfortunately. Hopefully you have root permission. If not, ask your
   root to run the script).
   *have to run this for mac os


3. export ANT_OPTS="-Xms1024m -Xmx1024m"

4. ant
    (compiles cassandra project)

5. ant firt

   (You should NOT get any warning!)


6. ant fi
    (compiles fi related)
   (It's okay to see lots of warnings!)


7. Then see cass-prefail-workload/README.txt



