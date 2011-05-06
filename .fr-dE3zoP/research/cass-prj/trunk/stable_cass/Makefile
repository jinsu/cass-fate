init:
	mkdir /tmp/fi
	mkdir /tmp/fi/logs/
	mkdir /tmp/fi/pids/
	mkdir /tmp/fi/ipHistory/
run:
	bin/cfi
	bin/allCnode 4

client:
	bin/cassandra-cli -host localhost -port 9160

clean:
	rm -rf /tmp/fi
	killall java
kill:
	killall java
logs:
	cat /tmp/fi/logs/*

  
