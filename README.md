# mqmeter

MQ JMeter Extension.

A [JMeter](http://jmeter.apache.org/) Plugin to put and get message on [IBM MQ](https://www.ibm.com/products/mq) Queue, also publish message on Topic. It connect to MQ Server through server channel using ip address, port number, userID and password (if the channel has CHLAUTH rules).

## Install

Build the extension:

    mvn package

Install the extension `mqmeter-x.y.z.jar` into 

    `$JMETER_HOME/lib/ext`.

## Usage

After installing `mqmeter`, you can choose two kind of Java Sampler, these are:

### MQClientSampler

Use it to put and get message (optional) on MQ queue. On JMeter add a Java Request Sampler and select the `MQClientSampler` class name. The following parameter are necessary.

* **mq_manager**: MQ Manager name. You can find it through IBM WebSphere MQ Explore or console.
* **mq_queue_rqst**: MQ Queue request name to put message. Could be Local or Remote queue.
* **mq_queue_rsps**: MQ Queue response name to get message. Could be Local or Remote queue. Leave it empty if you don't want get response message.
* **mq_correlate_rsps_msg**: Correlate the response message with request message to get the right message from response queue. Put 'messageId' or 'correlationId' values. Leave it empty if you don't want get response message.
* **mq_wait_interval**: Set wait interval that the get message call waits for a suitable message to arrive. Similar to time-out to get response message on queue.
* **mq_hostname**: Host name or ip address where MQ Server is running.
* **mq_port**: Port number of the MQ Server listener.
* **mq_channel**: The Server channel name on MQ Server.
* **mq_user_id**: The userID to connect to MQ server channel. Leave it empty if you don't need user id to connect to MQ.
* **mq_user_password**: The user password to connect to MQ server channel. Leave it empty if you don't need user id and password to connect to MQ.
* **mq_encoding_message**: Character encoding standard for your message: For EBCDIC put Cp1047. ASCII just put ASCII.
* **mq_message**: The content of the message that you want.

![Screenshot](https://github.com/JoseLuisSR/img2/blob/master/mqmeter/MQClientSampler.png)

### MQPublishSampler

MQ can manage topics also and you can publish and subscribe to it, use this class to publish message on MQ Topic.
On JMeter add a Java Request Sampler and select the `MQPublishSampler` class name. The following parameters are necessary.

* **mq_manager**: MQ Manager name. You can find it through IBM WebSphere MQ Explore or console.
* **mq_topic**: MQ topic name to publish message.
* **mq_hostname**: Host name or ip address where MQ Server is running.
* **mq_port**: Port number of the MQ Server listener.
* **mq_channel**: The Server channel name on MQ Server.
* **mq_user_id**: The userID to connect to MQ server channel. Leave it empty if you don't need user id to connect to MQ.
* **mq_user_password**: The user password to connect to MQ server channel. Leave it empty if you don't need user id and password to connect to MQ.
* **mq_encoding_message**: Character encoding standard for your message: For EBCDIC put Cp1047. ASCII just put ASCII.
* **mq_message**: The content of the message that you want.

![Screenshot](https://github.com/JoseLuisSR/img2/blob/master/mqmeter/MQPublishSampler.png)

## IBM WebSphere MQ

The below images show where find the values for some of the above properties

* **MQ Manager**

![Screenshot](https://github.com/JoseLuisSR/img2/blob/master/mqmeter/MQManager.png)

* **MQ Server channel**

![Screenshot](https://github.com/JoseLuisSR/img2/blob/master/mqmeter/MQServerChanel.png)

* **MQ Server listener**

![Screenshot](https://github.com/JoseLuisSR/img2/blob/master/mqmeter/MQServerListener.png)

* **MQ Chlauth**

You can find the steps to add users access to MQ Manager through Channel Authentication (CHLAUTH) with this tutorial
[IBM CHLAUTH](http://www-01.ibm.com/support/docview.wss?uid=swg27041997&aid=1)

![Screenshot](https://github.com/JoseLuisSR/img2/blob/master/mqmeter/MQChlauth.png)

## Troubleshooting

When use the MQPublishSampler Java Sampler is possible that you get the below exception:

java.lang.NoSuchMethodError: com.ibm.mq.MQQueueManager.accessTopic(Ljava/lang/String;Ljava/lang/String;II)Lcom/ibm/mq/MQTopic;
	at co.signal.mqmeter.MQPublishSampler.setupTest(MQPublishSampler.java:123) ~[mqmeter-1.4.jar:?]
	at org.apache.jmeter.protocol.java.sampler.JavaSampler.sample(JavaSampler.java:194) ~[ApacheJMeter_java.jar:3.3 r1808647]
	at org.apache.jmeter.threads.JMeterThread.executeSamplePackage(JMeterThread.java:498) ~[ApacheJMeter_core.jar:3.3 r1808647]
	at org.apache.jmeter.threads.JMeterThread.processSampler(JMeterThread.java:424) ~[ApacheJMeter_core.jar:3.3 r1808647]
	at org.apache.jmeter.threads.JMeterThread.run(JMeterThread.java:255) ~[ApacheJMeter_core.jar:3.3 r1808647]
	at java.lang.Thread.run(Unknown Source) [?:?]

It is throw because there is com.ibm.mq* jar file  that is upload by JMeter and it does not have the methods related with topic or other MQ objects. It jar file isn't related with mqmeter plugin but JMeter calls it first.

Checking JMeter lib and ext folders to remove com.ibm.mq* jar files that do not have methods or mq topic objects.

