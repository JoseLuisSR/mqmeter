package co.signal.mqmeter;

import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQMsg2;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.MQPutMessageOptions;
import com.ibm.mq.MQGetMessageOptions;
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

public class MQClientSampler extends AbstractJavaSamplerClient {


    private static final Logger log = LoggingManager.getLoggerForClass();

    /**
     *  Parameter for setting the MQ Manager.
     */
    private static final String PARAMETER_MQ_MANAGER = "mq_manager";

    /**
     * Parameter for setting MQ QUEUE to put message, could be LOCAL or REMOTE.
     */
    private static final String PARAMETER_MQ_QUEUE_RQST = "mq_queue_rqst";

    /**
     * Parameter for setting MQ QUEUE for get response message, could be LOCAL or REMOTE.
     */
    private static final String PARAMETER_MQ_QUEUE_RSPS = "mq_queue_rsps";

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

    private MQQueueManager mqMgr;


    /**
     * Initial values for test parameter. They are show in Java Request test sampler.
     * @return Arguments to set as default.
     */
    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameter = new Arguments();
        defaultParameter.addArgument(PARAMETER_MQ_MANAGER, "${MQ_MANAGER}");
        defaultParameter.addArgument(PARAMETER_MQ_QUEUE_RQST, "${MQ_QUEUE_RQST}");
        defaultParameter.addArgument(PARAMETER_MQ_QUEUE_RSPS, "");
        defaultParameter.addArgument(PARAMETER_MQ_HOSTNAME, "${MQ_HOSTNAME}");
        defaultParameter.addArgument(PARAMETER_MQ_PORT, "${MQ_PORT}");
        defaultParameter.addArgument(PARAMETER_MQ_CHANNEL, "${MQ_CHANNEL}");
        defaultParameter.addArgument(PARAMETER_MQ_USER_ID, "");
        defaultParameter.addArgument(PARAMETER_MQ_USER_PASSWORD,"");
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
        Hashtable properties = new Hashtable<String, Object>();
        properties.put(MQConstants.HOST_NAME_PROPERTY, context.getParameter(PARAMETER_MQ_HOSTNAME));
        properties.put(MQConstants.PORT_PROPERTY, Integer.parseInt(context.getParameter(PARAMETER_MQ_PORT)));
        properties.put(MQConstants.CHANNEL_PROPERTY, context.getParameter(PARAMETER_MQ_CHANNEL));
        properties.put(MQConstants.USE_MQCSP_AUTHENTICATION_PROPERTY, "true");
        String userID = context.getParameter(PARAMETER_MQ_USER_ID);
        if( userID != null && !userID.isEmpty())
            properties.put(MQConstants.USER_ID_PROPERTY, userID);
        String password = context.getParameter(PARAMETER_MQ_USER_PASSWORD);
        if( password != null && !password.isEmpty() )
            properties.put(MQConstants.PASSWORD_PROPERTY, password);
        log.info("MQ Manager properties are hostname: " + properties.get(MQConstants.HOST_NAME_PROPERTY) + " port: " +
                properties.get(MQConstants.PORT_PROPERTY) + " channel: " + properties.get(MQConstants.CHANNEL_PROPERTY));
        String mq_Manager = context.getParameter(PARAMETER_MQ_MANAGER);

        try {
            log.info("Connecting to queue manager " + mq_Manager);
            mqMgr = new MQQueueManager(mq_Manager, properties);
        }catch (Exception e){
            log.info("setupTest" + e.getMessage());
        }
    }

    /**
     * Close and disconnect MQ variables.
     * @param context to get the arguments values on Java Sampler.
     */
    @Override
    public void teardownTest(JavaSamplerContext context) {
        try{
            log.info("Disconnecting from the Queue Manager");
            mqMgr.disconnect();
            log.info("Done!");
        }catch (Exception e){
            log.info("teardownTest" + e.getMessage());
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
        String response;
        sampleResultStart(result, message);

        try{
            putMQMessage(context, message);
            response = getMQMessage(context);
            sampleResultSuccess(result, response);
        }catch (Exception e){
            sampleResultFail(result, "500", e);
            log.info("runTest" + e.getMessage());
        }
        return result;
    }


    /**
     * Method to open a mq queue, put message and close mq queue.
     * @param context to get the arguments values on Java Sampler.
     * @param message to put on mq queue.
     * @throws Exception
     */
    private void putMQMessage(JavaSamplerContext context, String message) throws Exception{
        String mq_Queue = context.getParameter(PARAMETER_MQ_QUEUE_RQST);
        String encodingMsg = context.getParameter(PARAMETER_MQ_ENCODING_MESSAGE);
        MQMessage mqMessage = new MQMessage();
        MQQueue mqQueue;

        log.info("Accessing queue: " + mq_Queue);
        mqQueue = mqMgr.accessQueue(mq_Queue, MQConstants.MQOO_OUTPUT);
        log.info("Sending a message...");
        mqMessage.write(message.getBytes(encodingMsg));
        mqQueue.put(mqMessage, new MQPutMessageOptions());
        log.info("Closing the queue");
        mqQueue.close();
    }

    /**
     * Method to open mq queue, get message and close mq queue.
     * @param context to get the arguments values on Java Sampler.
     * @return String, message on mq queue.
     * @throws Exception
     */
    private String getMQMessage(JavaSamplerContext context) throws Exception{
        String mq_Queue = context.getParameter(PARAMETER_MQ_QUEUE_RSPS);
        String response = null;

        if( mq_Queue != null && !mq_Queue.isEmpty()){
            String encodingMsg = context.getParameter(PARAMETER_MQ_ENCODING_MESSAGE);
            MQMsg2 mqMsg2 = new MQMsg2();
            MQQueue mqQueue;

            log.info("Accessing queue: " + mq_Queue);
            mqQueue = mqMgr.accessQueue(mq_Queue, MQConstants.MQOO_INPUT_AS_Q_DEF);
            log.info("Getting a message...");
            mqQueue.getMsg2(mqMsg2,new MQGetMessageOptions());
            response = new String(mqMsg2.getMessageData(),encodingMsg);
            log.info("Closing the queue");
            mqQueue.close();
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
        result.setResponseMessage("Exception: " + exception);
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        result.setResponseData(stringWriter.toString(), ENCODING);
    }

}
