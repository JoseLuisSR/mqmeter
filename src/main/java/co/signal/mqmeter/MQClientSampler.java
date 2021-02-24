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

import static com.ibm.mq.constants.CMQC.CHANNEL_PROPERTY;
import static com.ibm.mq.constants.CMQC.HOST_NAME_PROPERTY;
import static com.ibm.mq.constants.CMQC.MQGMO_WAIT;
import static com.ibm.mq.constants.CMQC.PASSWORD_PROPERTY;
import static com.ibm.mq.constants.CMQC.PORT_PROPERTY;
import static com.ibm.mq.constants.CMQC.USER_ID_PROPERTY;
import static com.ibm.mq.constants.CMQC.USE_MQCSP_AUTHENTICATION_PROPERTY;
import static java.lang.String.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Objects;

import com.ibm.mq.MQException;
import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQMsg2;
import com.ibm.mq.MQPutMessageOptions;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;

/**
 * This class is to put and get message on WebSphere MQ queue.
 * @author JoseLuisSR
 * @since 01/13/2019
 * @see "https://github.com/JoseLuisSR/mqmeter"
 */
public class MQClientSampler extends AbstractJavaSamplerClient {

    public static final String SETUP_TEST_ERROR_MESSAGE = "setupTest %s %s";

    public static final String CLOSING_QUEUE_MESSAGE = "Closing queue %s";

    private Logger log;

    /**
     *  Parameter for setting the MQ Manager.
     */
    private static final String PARAMETER_MQ_MANAGER = "mq_manager";

    /**
     * Parameter for setting MQ QUEUE to put message, could be LOCAL or REMOTE.
     */
    private static final String PARAMETER_MQ_QUEUE_PUT = "mq_queue_put";

    /**
     * Parameter for setting MQ QUEUE for get message, could be LOCAL or REMOTE.
     */
    private static final String PARAMETER_MQ_QUEUE_GET = "mq_queue_get";

    /**
     * Parameter for setting correlate response message with request message.
     */
    private static final String PARAMETER_MQ_CORRELATE_MSG = "mq_correlate_msg";

    /**
     * Constant to correlate response message with messageID.
     */
    private static final String MESSAGE_ID = "messageId";

    /**
     * Constant to correlate response message with correlationID.
     */
    private static final String CORRELATION_ID = "correlationId";

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
     * Parameter for setting MQ Message type like byte, string, object.
     */
    private static final String PARAMETER_MQ_MESSAGE_TYPE = "mq_message_type";

    /**
     * Parameter to set wait interval to get message on queue.
     */
    private static final String PARAMETER_MQ_WAIT_INTERVAL = "mq_wait_interval";

    /**
     * Parameter for encoding.
     */
    private static final String ENCODING = "UTF-8";

    /**
     * Default encoding message ASCII constant.
     */
    private static final String DEFAULT_MESSAGE_ENCODING = "ASCII";

    /**
     * Default message type constant.
     */
    private static final String DEFAULT_MESSAGE_TYPE = "Byte";

    public static final String MQ_MANAGER = "${MQ_MANAGER}";

    public static final String MQ_QUEUE_PUT = "${MQ_QUEUE_PUT}";

    public static final String EMPTY = "";

    public static final String MQ_HOSTNAME = "${MQ_HOSTNAME}";

    public static final String MQ_PORT = "${MQ_PORT}";

    public static final String MQ_CHANNEL = "${MQ_CHANNEL}";

    public static final String MQ_USE_MQCSP_AUTHENTICATION = "${MQ_USE_MQCSP_AUTHENTICATION}";

    public static final String MQ_ENCODING_MESSAGE = "${MQ_ENCODING_MESSAGE}";

    public static final String MQ_MESSAGE = "${MQ_MESSAGE}";

    private static final String PARAMETER_MQ_HEADER = "mq_headers";

    /**
     * MQQueueManager variable.
     */
    private MQQueueManager mqMgr;

    /**
     * MQQueue request variable.
     */
    private MQQueue mqQueuePut;

    /**
     * MQQueue response variable.
     */
    private MQQueue mqQueueGet;

    /**
     * Encoding message variable.
     */
    private String encodingMessage;

    /**
     * Message type variable
     */
    private String messageType;

    /**
     * Properties variable.
     */
    private Hashtable<String, Object> properties;

    /**
     * Initial values for test parameter. They are show in Java Request test sampler.
     * @return Arguments to set as default.
     */
    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameter = new Arguments();
        defaultParameter.addArgument(PARAMETER_MQ_MANAGER, MQ_MANAGER);
        defaultParameter.addArgument(PARAMETER_MQ_QUEUE_PUT, MQ_QUEUE_PUT);
        defaultParameter.addArgument(PARAMETER_MQ_QUEUE_GET, EMPTY);
        defaultParameter.addArgument(PARAMETER_MQ_CORRELATE_MSG, EMPTY);
        defaultParameter.addArgument(PARAMETER_MQ_WAIT_INTERVAL, EMPTY);
        defaultParameter.addArgument(PARAMETER_MQ_HOSTNAME, MQ_HOSTNAME);
        defaultParameter.addArgument(PARAMETER_MQ_PORT, MQ_PORT);
        defaultParameter.addArgument(PARAMETER_MQ_CHANNEL, MQ_CHANNEL);
        defaultParameter.addArgument(PARAMETER_MQ_USER_ID, EMPTY);
        defaultParameter.addArgument(PARAMETER_MQ_USER_PASSWORD, EMPTY);
        defaultParameter.addArgument(PARAMETER_MQ_MESSAGE_TYPE, DEFAULT_MESSAGE_TYPE);
        defaultParameter.addArgument(PARAMETER_MQ_ENCODING_MESSAGE, MQ_ENCODING_MESSAGE);
        defaultParameter.addArgument(PARAMETER_MQ_MESSAGE, MQ_MESSAGE);
        defaultParameter.addArgument(PARAMETER_MQ_HEADER, "${MQ_HEADER}");
        return defaultParameter;
    }

    /**
     * Read the test parameter and initialize your test client.
     * @param context to get the arguments values on Java Sampler.
     */
    @Override
    public void setupTest(JavaSamplerContext context) {
        log = getNewLogger();

        // SET MQ Manager properties to connection.
        properties = new Hashtable<>();
        properties.put(HOST_NAME_PROPERTY, context.getParameter(PARAMETER_MQ_HOSTNAME));
        properties.put(PORT_PROPERTY, Integer.parseInt(context.getParameter(PARAMETER_MQ_PORT)));
        properties.put(CHANNEL_PROPERTY, context.getParameter(PARAMETER_MQ_CHANNEL));

        properties.put(USE_MQCSP_AUTHENTICATION_PROPERTY, mqUseMqcspAuthentication.equals("true"));

        String userID = context.getParameter(PARAMETER_MQ_USER_ID);
        if ( userID != null && !userID.isEmpty())
            properties.put(USER_ID_PROPERTY, userID);

        String password = context.getParameter(PARAMETER_MQ_USER_PASSWORD);
        if ( password != null && !password.isEmpty())
            properties.put(PASSWORD_PROPERTY, password);

        encodingMessage = context.getParameter(PARAMETER_MQ_ENCODING_MESSAGE);
        messageType = context.getParameter(PARAMETER_MQ_MESSAGE_TYPE);

        log.info(format("MQ Manager properties are hostname: %s  port: %s channel: %s  user: %s",
            properties.get(HOST_NAME_PROPERTY),
            properties.get(PORT_PROPERTY),
            properties.get(CHANNEL_PROPERTY),
            properties.get(USER_ID_PROPERTY)));

        //Connecting to MQ Manager.
        String mqManager = context.getParameter(PARAMETER_MQ_MANAGER);
        log.info("Connecting to queue manager " + mqManager);
        try{
            mqMgr = new MQQueueManager(mqManager, properties);
        }catch (MQException e){
            log.info(String.format(SETUP_TEST_ERROR_MESSAGE, e.getMessage(), MQConstants.lookupReasonCode(e.getReason())));
        }

        //Open mq queue to put message.
        String queuePutName = context.getParameter(PARAMETER_MQ_QUEUE_PUT);
        if (!MQ_QUEUE_PUT.equalsIgnoreCase(queuePutName) && !StringUtils.isBlank(queuePutName)) {
            log.info(String.format("Accessing queue: %s to put message.", queuePutName));
            try {
                mqQueuePut = mqMgr.accessQueue(queuePutName, MQConstants.MQOO_OUTPUT);
            } catch (MQException e) {
                log.info(String.format(SETUP_TEST_ERROR_MESSAGE, e.getMessage(), MQConstants.lookupReasonCode(e.getReason())));
            }
        }

        //Open mq queue to get message
        String queueGetName = context.getParameter(PARAMETER_MQ_QUEUE_GET);
        if(!StringUtils.isBlank(queueGetName)){
            log.info(String.format("Accessing queue: %s to get message.", queueGetName));
            try{
                mqQueueGet = mqMgr.accessQueue(queueGetName, MQConstants.MQOO_INPUT_AS_Q_DEF);
            }catch (MQException e){
                log.info(String.format(SETUP_TEST_ERROR_MESSAGE, e.getMessage(), MQConstants.lookupReasonCode(e.getReason())));
            }
        }

    }

    /**
     * Close and disconnect MQ variables.
     * @param context to get the arguments values on Java Sampler.
     */
    @Override
    public void teardownTest(JavaSamplerContext context) {
        try {
            if( mqQueuePut != null && mqQueuePut.isOpen() ) {
                log.info(String.format(CLOSING_QUEUE_MESSAGE, mqQueuePut.getName()));
                mqQueuePut.close();
                log.info("Done!");
            }
            if( mqQueueGet != null && mqQueueGet.isOpen() ) {
                log.info(String.format(CLOSING_QUEUE_MESSAGE, mqQueueGet.getName()));
                mqQueueGet.close();
                log.info("Done!");
            }
            if( mqMgr != null && mqMgr.isConnected() ) {
                log.info("Disconnecting from the Queue Manager");
                mqMgr.disconnect();
                log.info("Done!");
            }
        } catch (MQException e) {
            log.info("teardownTest " + e.getCause());
        }
    }

    /**
     * Main method to execute the test on single thread.
     * @param context to get the arguments values on Java Sampler.
     * @return SampleResult, captures data such as whether the test was successful,
     * the response code and message, any request or response data, and the test start/end times
     */
    @Override
    public SampleResult runTest(JavaSamplerContext context) {

        SampleResult result = newSampleResult();
        String message = context.getParameter(PARAMETER_MQ_MESSAGE);
        String response = "";
        byte[] messageId = {};
        MQMessage mqMessage = new MQMessage();

        String headers = context.getParameter(PARAMETER_MQ_HEADER);

        try{

            mqMessage.write(message.getBytes(encodingMessage));
            for(String header : headers.split(",")) {
                String[] keyValue = header.split(":");
                mqMessage.setStringProperty(keyValue[0], keyValue[1]);
            }
            sampleResultStart(result, mqMessage);

            if (Objects.nonNull(mqQueuePut)) {
                //Put message on queue.
                messageId = putMQMessage(context, mqMessage);
            }
            //Get message on queue.
            if (Objects.nonNull(mqQueueGet)) {
                do {
                    response = getMQMessage(context, messageId);
                } while (null == response);
            }
            sampleResultSuccess(result, response);
        }catch (MQException e){
            sampleResultFail(result, "500", e);
            log.info(String.format("runTest %s %s", e.getMessage(), MQConstants.lookupReasonCode(e.getReason())));
        } catch (Exception e) {
            sampleResultFail(result, "500", e);
            log.info(String.format("runTest %s", e.getMessage()));
        }
        return result;
    }


    /**
     * Method to put message on IBM mq queue.
     * @param context to get the arguments values on Java Sampler.
     * @param message to put on mq queue.
     * @return messageId generate by MQ Manager.
     * @throws Exception
     */
    private byte[] putMQMessage(JavaSamplerContext context, String message) throws MQException, IOException {

        MQMessage mqMessage = new MQMessage();

        log.info("Sending a message...");
        switch (messageType){
            case "Object":
                mqMessage.writeObject(message);
                break;
            case "String":
                mqMessage.writeString(message);
                break;
            default:
                mqMessage.write(message.getBytes(encodingMessage));
                break;
        }
        mqQueuePut.put(mqMessage, new MQPutMessageOptions());
        return mqMessage.messageId;
    }

    /**
     * Method to get message from IBM mq queue.
     * @param context to get the arguments values on Java Sampler.
     * @param messageId to correlate response message with request message.
     * @return String, message on mq queue.
     * @throws Exception
     */
    private String getMQMessage(JavaSamplerContext context, byte[] messageId) throws MQException, UnsupportedEncodingException {
        MQGetMessageOptions mqGMO = new MQGetMessageOptions();
        String response = null;

        if (mqQueueGet!= null && mqQueueGet.isOpen() ){
            String correlateMsg = context.getParameter(PARAMETER_MQ_CORRELATE_MSG);
            MQMsg2 mqMsg2 = new MQMsg2();

            if (Objects.nonNull(messageId)) {
                // Set message id from request message to get response message
                if(correlateMsg == null || correlateMsg.isEmpty())
                    mqMsg2.setMessageId(messageId);
                else if(correlateMsg.equals(MESSAGE_ID))
                    mqMsg2.setMessageId(messageId);
                else if(correlateMsg.equals(CORRELATION_ID))
                    mqMsg2.setCorrelationId(messageId);
            }
            //Set wait Interval to get message on queue.
            String waitInterval = context.getParameter(PARAMETER_MQ_WAIT_INTERVAL);
            if ( waitInterval != null && !waitInterval.isEmpty() && StringUtils.isNumeric(waitInterval) ) {
                mqGMO.options = MQGMO_WAIT;
                mqGMO.waitInterval = Integer.parseInt(waitInterval);
            }

            log.info("Getting a message...");
            try {
                mqQueueGet.getMsg2(mqMsg2, mqGMO);
                response = new String(mqMsg2.getMessageData(), encodingMessage);
            } catch (MQException e) {
                if (2033 != e.getReason()) {
                   throw e;
                }
            }
        }

        return response;
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
    private void sampleResultStart(SampleResult result, MQMessage data){
        result.setSamplerData(data.toString());
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
        if (response != null)
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
        responseMessage += exception.getClass().equals(MQException.class) ? " MQ Reason Code: " + MQConstants.lookupReasonCode(((MQException)exception).getReason()) : EMPTY;
        responseMessage += exception.getCause() != null ? " Cause: " + exception.getCause() : EMPTY;
        result.setResponseMessage(responseMessage);

        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        result.setResponseData(stringWriter.toString(), ENCODING);
    }

}