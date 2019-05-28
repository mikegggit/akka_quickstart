mci:
	mvn clean install
run:
	mvn exec:exec

testsingle:
	mvn test -Dtest=DeviceTest#testListActiveDevicesAfterOneShutsDown
