package smtp;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Smtp client that can send mails to a specified smtp server
 *
 * Name : SmtpClient
 * File : SmtpClient.java
 * @author Gaétan Zwick
 * @author Marco Maziero
 * @version 1.0
 * @since 01.05.2021
 */
public class SmtpClient implements ISmtpClient {
    private static final Logger LOG = Logger.getLogger(SmtpClient.class.getName());
    private static final String CRLF = "\r\n";
    private final int port;
    private final String address;

    /**
     * Stores the given address and port for future connexion
     * @param serverAddress The smtp server address
     * @param serverPort The smtp server port
     */
    public SmtpClient(String serverAddress, int serverPort) {
        // Sets the variables
        address = serverAddress;
        port = serverPort;
    }

    /**
     * Sends an email through the client
     * @param mail The mail to send
     * @throws IOException If the mail could not be sent
     * @throws RuntimeException If the smtp server responded with an unexpected smtp code
     */
    @Override
    public void sendMail(Mail mail) throws IOException, RuntimeException {

        // Creates the socket and connects to the smtp server
        System.out.println("Connecting to the SMTP server " + address + ":" + port);
        Socket socket = new Socket(InetAddress.getByName(address), port);

        // Sends the mail
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            // Creates the streams
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            System.out.println("Sending mail: " + mail.getSubject());

            // Server greetings
            readServer(reader, 220);

            // Initiates SMTP talk
            writer.write("EHLO res-lab.com" + CRLF);
            writer.flush();
            readServer(reader, 250);

            // Gives the sender (MAIL FROM)
            writer.write("MAIL FROM: <" + mail.getFrom() + ">" + CRLF);
            writer.flush();
            readServer(reader, 250);

            // Gives the recipients
            List<String> rcpts = new LinkedList<>();
            rcpts.addAll(mail.getTo());
            rcpts.addAll(mail.getCCs());
            for (String rcpt : rcpts) {
                writer.write("RCPT TO: <" + rcpt + ">" + CRLF);
                writer.flush();
                readServer(reader, 250);
            }

            // Starts the data section
            writer.write("DATA" + CRLF);
            writer.flush();
            readServer(reader, 354);

            // Specifies the charset and content type
            writer.write("Content-Type: text/plain; charset=utf-8" + CRLF);
            writer.flush();

            // Specifies the sender
            writer.write("from: " + mail.getFrom() + CRLF);
            writer.flush();

            // Specifies the rcpts
            for (String to : mail.getTo()) {
                writer.write("to: " + to + CRLF);
                writer.flush();
            }

            // Writes the subject
            writer.write("Subject: " + mail.getSubject() + CRLF + CRLF);
            writer.flush();

            // Writes the content
            writer.write(mail.getContent() + CRLF);
            writer.flush();

            // Ends content write
            writer.write("." + CRLF);
            writer.flush();
            readServer(reader, 250);

            // Ends chat with server
            writer.write("QUIT" + CRLF);
            writer.flush();
            readServer(reader, 221);

            System.out.println("Mail sent successfully !");

            // Closes all
            reader.close();
            writer.close();
            socket.close();
            System.out.println("Connexion with SMTP server has ended");
        } finally {
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                LOG.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    /**
     * Reads the server responses until the client should send data
     * @param reader The reader binded to the socket
     * @param expectedSmtpCode The expected SMTP response code from the server
     * @throws IOException If something went wrong with the reader
     * @throws RuntimeException If the response code from the server is unexpected
     */
    private void readServer(BufferedReader reader, int expectedSmtpCode) throws IOException, RuntimeException {
        String line = reader.readLine();
        while (line != null && line.charAt(3) != ' ')
            line = reader.readLine();

        // Checks response code
        if (line == null || Integer.parseInt(line.substring(0, 3)) != expectedSmtpCode)
            throw new RuntimeException(line);
    }
}
