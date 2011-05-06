


all: prepare

prepare:
	rm -Rf /tmp/fi
	mkdir /tmp/fi/
	mkdir /tmp/fi/failHistory
	mkdir /tmp/fi/flagsFailure
	mkdir /tmp/fi/expResult

result:
	grep experiment /tmp/workloadOut.txt > /tmp/exp.txt
	gedit /tmp/exp.txt

