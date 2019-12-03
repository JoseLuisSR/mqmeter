/*
 * Copyright 2019 JoseLuisSR
 *
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing, software
 *distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *See the License for the specific language governing permissions and
 *limitations under the License.
 */
package co.signal.mqmeter;

import com.ibm.mq.*;
import com.ibm.mq.constants.MQConstants;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Hashtable;

/**
 * This class is to publish message on WebSphere MQ topic.
 * @author JoseLuisSR
 * @since 01/13/2019
 * @see "https://github.com/JoseLuisSR/mqmeter"
 */
public class MQPublishSampler extends AbstractJavaSamplerClient {

    private static final Logger log = LoggingManager.getLoggerForClass();

    /**
     *  Parameter for setting the MQ Manager.
     */
    private static final String PARAMETER_MQ_MANAGER = "mq_manager";

    /**
     * Parameter for setting the topic name.
     */
    private static final String PARAMETER_MQ_TOPIC = "mq_topic";

    /**
     * Parameter for setting MQ Hostname where MQ Server is deploying.
     */
    private static final String PARAMETER_MQ_HOSTNAME = "mq_hostname";

    /**
     * Parameter for setting MQ Channel, it should be server connection channel.
     */
    private static final String PARAMETER_MQ_CHANNEL = "mq_channel";

    /**
     * Parameter for setting MQ USER ID.
     */
    private static final String PARAMETER_MQ_USER_ID = "mq_user_id";

    /**
     * Parameter for setting MQ User password.
     */
    private static final String PARAMETER_MQ_USER_PASSWORD = "mq_user_password";

    /**
     * Parameter for setting MQ PORT, is the Listener port.
     */
    private static final String PARAMETER_MQ_PORT = "mq_port";

    /**
     * Parameter for setting MQ Message.
     */
    private static final String PARAMETER_MQ_MESSAGE = "mq_message";

    /**
     * Parameter for setting MQ Encoding Message.
     */
    private static final String PARAMETER_MQ_ENCODING_MESSAGE = "mq_encoding_message";

    /**
     * Parameter for encoding.
     */
    private static final String ENCODING = "UTF-8";

    /**
     * MQQueueManager variable.
     */
    private MQQueueManager mqMgr;

    /**
     * Properties variable.
     */
    private Hashtable properties;

    /**
     * Initial values for test parameter. They are show in Java Request test sampler.
     * @return Arguments to set as default.
     */
    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameter = new Arguments();
        defaultParameter.addArgument(PARAMETER_MQ_MANAGER, "${MQ_MANAGER}");
        defaultParameter.addArgument(PARAMETER_MQ_TOPIC,"${MQ_TOPIC}");
        defaultParameter.addArgument(PARAMETER_MQ_HOSTNAME, "${MQ_HOSTNAME}");
        defaultParameter.addArgument(PARAMETER_MQ_PORT, "${MQ_PORT}");
        defaultParameter.addArgument(PARAMETER_MQ_CHANNEL, "${MQ_CHANNEL}");
        defaultParameter.addArgument(PARAMETER_MQ_USER_ID, "");
        defaultParameter.addArgument(PARAMETER_MQ_USER_PASSWORD, "");
        defaultParameter.addArgument(PARAMETER_MQ_ENCODING_MESSAGE, "${MQ_ENCODING_MESSAGE}");
        defaultParameter.addArgument(PARAMETER_MQ_MESSAGE, "${MQ_MESSAGE}");
        return defaultParameter;
    }

    /**
     * Read the test parameter and initialize your test client.
     * @param context to get the arguments values on Java Sampler.
     */
    @Override
    public void setupTest(JavaSamplerContext context) {

        // SET MQ Manager properties to connection.
        properties = new Hashtable<String, Object>();
        properties.put(MQConstants.HOST_NAME_PROPERTY, context.getParameter(PARAMETER_MQ_HOSTNAME));
        properties.put(MQConstants.PORT_PROPERTY, Integer.parseInt(context.getParameter(PARAMETER_MQ_PORT)));
        properties.put(MQConstants.CHANNEL_PROPERTY, context.getParameter(PARAMETER_MQ_CHANNEL));
        properties.put(MQConstants.USE_MQCSP_AUTHENTICATION_PROPERTY, true);

        String userID = context.getParameter(PARAMETER_MQ_USER_ID);
        if( userID != null && !userID.isEmpty())
            properties.put(MQConstants.USER_ID_PROPERTY, userID);

        String password = context.getParameter(PARAMETER_MQ_USER_PASSWORD);
        if( password != null && !password.isEmpty() )
            properties.put(MQConstants.PASSWORD_PROPERTY, password);

        log.info("MQ Manager properties are hostname: " + properties.get(MQConstants.HOST_NAME_PROPERTY) + " port: " +
                properties.get(MQConstants.PORT_PROPERTY) + " channel: " + properties.get(MQConstants.CHANNEL_PROPERTY));

        //Connecting to MQ Manager.
        String mq_Manager = context.getParameter(PARAMETER_MQ_MANAGER);
        log.info("Connecting to queue manager " + mq_Manager);
        try{
            mqMgr = new MQQueueManager(mq_Manager, properties);
        }catch (MQException e){
            log.info("setupTest " + e.getMessage() + " " + MQConstants.lookupReasonCode(e.getReason()) );
        }
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        try {
            log.info("Disconnecting from the Queue Manager");
            mqMgr.disconnect();
            log.info("Done!");
        } catch (MQException e) {
            log.info("teardownTest " + e.getCause());
        }
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = newSampleResult();
        String encodingMessage = context.getParameter(PARAMETER_MQ_ENCODING_MESSAGE);
        String message = context.getParameter(PARAMETER_MQ_MESSAGE);
        String topicName = context.getParameter(PARAMETER_MQ_TOPIC);
        MQTopic publisher = null;
        String response = null;
        MQMessage mqMessage = new MQMessage();

        sampleResultStart(result, message);

        try{
            //Access topic.
            log.info("Access topic: " + topicName);
            publisher = mqMgr.accessTopic(null,topicName, MQConstants.MQTOPIC_OPEN_AS_PUBLICATION, MQConstants.MQOO_OUTPUT);
            //Publish message on topic.
            mqMessage.write(message.getBytes(encodingMessage));
            log.info("Publishing..");
            publisher.put(mqMessage, new MQPutMessageOptions());
            log.info("Done!");
            sampleResultSuccess(result, response);
        }catch (MQException e){
            sampleResultFail(result, "500", e);
            log.info("runTest " + e.getMessage() + " " + MQConstants.lookupReasonCode(e.getReason()) );
        }catch (Exception e){
            sampleResultFail(result, "500", e);
            log.info("runTest " + e.getMessage());
        }finally {
            try{
                log.info("Close the topic");
                publisher.close();
                log.info("Done!");
            }catch (MQException e){
                sampleResultFail(result, "500", e);
                log.info("runTest " + e.getMessage() + " " + MQConstants.lookupReasonCode(e.getReason()) );
            }
        }

        return result;
    }

    /**
     *
     * @return SampleResult, captures data such as whether the test was successful,
     * the response code and message, any request or response data, and the test start/end times
     */
    private SampleResult newSampleResult(){
        SampleResult result = new SampleResult();
        result.setDataEncoding(ENCODING);
        result.setDataType(SampleResult.TEXT);
        return result;
    }

    /**
     * Start the sample request and set the <code>samplerData</code> to the
     * requestData.
     *
     * @param result
     *          the sample result to update
     * @param data
     *          the request to set as <code>samplerData</code>
     */
    private void sampleResultStart(SampleResult result, String data){
        result.setSamplerData(data);
        result.sampleStart();
    }

    /**
     * Set the sample result as <code>sampleEnd()</code>,
     * <code>setSuccessful(true)</code>, <code>setResponseCode("OK")</code> and if
     * the response is not <code>null</code> then
     * <code>setResponseData(response.toString(), ENCODING)</code> otherwise it is
     * marked as not requiring a response.
     *
     * @param result
     *          sample result to change
     * @param response
     *          the successful result message, may be null.
     */
    private void sampleResultSuccess(SampleResult result, String response){
        result.sampleEnd();
        result.setSuccessful(true);
        result.setResponseCodeOK();
        if(response != null)
            result.setResponseData(response, ENCODING);
        else
            result.setResponseData("No response required", ENCODING);
    }

    /**
     * Mark the sample result as <code>sampleEnd</code>,
     * <code>setSuccessful(false)</code> and the <code>setResponseCode</code> to
     * reason.
     *
     * @param result
     *          the sample result to change
     * @param reason
     *          the failure reason
     */
    private void sampleResultFail(SampleResult result, String reason, Exception exception){
        result.sampleEnd();
        result.setSuccessful(false);
        result.setResponseCode(reason);
        String responseMessage;

        responseMessage = "Exception: " + exception.getMessage();
        responseMessage += exception.getClass().equals(MQException.class) ? " MQ Reason Code: " + MQConstants.lookupReasonCode(((MQException)exception).getReason()) : "";
        responseMessage += exception.getCause() != null ? " Cause: " + exception.getCause() : "";
        result.setResponseMessage(responseMessage);

        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        result.setResponseData(stringWriter.toString(), ENCODING);
    }
}