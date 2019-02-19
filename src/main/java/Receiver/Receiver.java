package Receiver;

import com.rabbitmq.client.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;

public class Receiver {
    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    private String queueName;
    private String DirectoryPath;
    private OutputStream outputStream;

    private DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        BasicProperties properties = delivery.getProperties();
        if (properties.getContentType() != null && properties.getContentType().equals("file")) {
            getFile(delivery);
        } else {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            outputStream.write(delivery.getBody());
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
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    public void closeConnections(){
        try {
            channel.close();
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    public void exchangeDeclare(String exchangeName, BuiltinExchangeType exchangeType) {
        try {
            channel.exchangeDeclare(exchangeName, exchangeType);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void queueBind(String exchangeName, String bindingKey) {
        try {
            channel.queueBind(queueName, exchangeName, bindingKey);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startReceiving() {
        try {
            channel.basicConsume(queueName, true, deliverCallback, consumerTag->{});
        } catch (IOException e) {
            e.printStackTrace();
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
            outputStream.write(("File '" + fileName + "' received").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
