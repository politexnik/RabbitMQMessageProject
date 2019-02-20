package Receiver;

import com.rabbitmq.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;

public class Receiver {
    private static final Logger log = LogManager.getLogger("Receiver");
    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    private String queueName;
    private String DirectoryPath;
    private OutputStream outputStream;

    private DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        BasicProperties properties = delivery.getProperties();
        //splitting if a message or file
        if (properties.getContentType() != null && properties.getContentType().equals("file")) {
            getFile(delivery);
        } else {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            outputStream.write(delivery.getBody());
            log.info(message);
        }
    };

    private Receiver() {
        factory = new ConnectionFactory();
    }

    public static Receiver buildLocalReceiver(){
        Receiver receiver = new Receiver();
        receiver.factory.setHost("localhost");
        return receiver;
    }

    public static Receiver buildReceiver(String userName, String password, String virtualHost, String hostName, int portNumber){
        Receiver receiver = new Receiver();
        receiver.factory.setUsername(userName);
        receiver.factory.setPassword(password);
        receiver.factory.setVirtualHost(virtualHost);
        receiver.factory.setHost(hostName);
        receiver.factory.setPort(portNumber);

        return receiver;
    }

    public void openConnections(){
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
            queueName = channel.queueDeclare().getQueue();  //1 application - 1 queue.
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public void closeConnections(){
        try {
            channel.close();
            connection.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void exchangeDeclare(String exchangeName, BuiltinExchangeType exchangeType) {
        try {
            channel.exchangeDeclare(exchangeName, exchangeType);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void queueBind(String exchangeName, String bindingKey) {
        try {
            channel.queueBind(queueName, exchangeName, bindingKey);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void startReceiving() {
        try {
            channel.basicConsume(queueName, true, deliverCallback, consumerTag->{});
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void setDirectoryPath(String directoryPath) {
        DirectoryPath = directoryPath;
    }

    private void getFile(Delivery delivery) {
            String fileName = delivery.getProperties().getHeaders().get("fileName").toString();   //because String casting is not working somehow, i don't know why yet
            Path receivedFile = Paths.get(DirectoryPath, fileName);
            //checking if file with name already exists
            while (true) {
                if (Files.exists(receivedFile)) {
                    fileName = fileName + "0";
                    receivedFile = Paths.get(DirectoryPath, fileName);
                } else {
                    break;
                }
            }
        try(FileOutputStream fileOutputStream = new FileOutputStream(receivedFile.toFile())) {
            fileOutputStream.write(delivery.getBody());
            String message = "File '" + fileName + "' received\n";
            outputStream.write(message.getBytes(StandardCharsets.UTF_8));
            log.info(message);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
