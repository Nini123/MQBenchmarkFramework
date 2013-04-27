/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.r9labs.mq.benchmark.drivers.jms;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.NamingException;
import org.r9labs.mq.benchmark.drivers.ConsumingDriver;

/**
 *
 * @author jpbarto
 */
public class JMSConsumer implements ConsumingDriver {

    private String username = null;
    private String password = null;
    private String topicName = null;
    private Topic topic = null;
    private String queueName = null;
    private Queue queue = null;
    private Context context = null;
    private TopicSession session = null;
    private TopicConnection conn = null;
    private TopicSubscriber cin = null;

    public JMSConsumer(Context context) {
        this.context = context;
    }

    public void setLogin(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void setTopic(String topic) {
        topicName = topic;
    }

    public void setQueue(String queue) {
        queueName = queue;
    }

    @Override
    public void start() {
       if (topicName != null) {
            TopicConnectionFactory connF;
            try {
                connF = (TopicConnectionFactory) context.lookup("ConnectionFactory");
            } catch (NamingException ex) {
                Logger.getLogger(JMSProducer.class.getName()).log(Level.SEVERE, "Error retrieving connection factory from context", ex);
                return;
            }

            try {
                if (username != null) {
                    conn = connF.createTopicConnection(username, password);
                } else {
                    conn = connF.createTopicConnection();
                }
                conn.start();
                session = conn.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
                topic = session.createTopic(topicName);
                cin = session.createSubscriber(topic);
            } catch (JMSException ex) {
                Logger.getLogger(JMSProducer.class.getName()).log(Level.SEVERE, "Error creating message consumer", ex);
            }
        }
    }

    @Override
    public void stop() {
        try {
            cin.close();
            session.close();
            conn.stop();
            conn.close();
        } catch (JMSException ex) {
            Logger.getLogger(JMSProducer.class.getName()).log(Level.WARNING, "An error occurred stopping consumer connection to broker", ex);
        }
    }

    @Override
    public byte[] getMessage() {
        BytesMessage msg;
        try {
            msg = (BytesMessage) cin.receive(500);
        } catch (JMSException ex) {
            Logger.getLogger(JMSConsumer.class.getName()).log(Level.SEVERE, "Error receiving BytesMessage", ex);
            return null;
        }
        
        if (msg != null) {
            try {
                byte[] ret = new byte[(int)msg.getBodyLength()];
                msg.readBytes(ret);
                msg.acknowledge();
                return ret;
            } catch (JMSException ex) {
                Logger.getLogger(JMSConsumer.class.getName()).log(Level.SEVERE, "Error reading received BytesMessage", ex);
            }
        }
        
        return null;
    }
}
