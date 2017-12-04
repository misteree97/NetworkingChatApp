/***************************************************************************************
 *    Title: Fig 28.4
 *    Author: Java How to Program (early objects) 10th Edition
 *    Date: 03 December 2017
 *    Code version: 10th Edition
 *    Availability: Chapter 28, Fig 28.4
 *    Took this code from the textbook, and modified it to open a text file.
 *
 ***************************************************************************************/

// Fig. 27.7: Client.java
// Client portion of a stream-socket connection between client and server.

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ThreadLocalRandom;

public class Client extends JFrame
{
    /**
     * enters information from user
     */
    private JTextField enterField;
    /**
     * display information to user
     */
    private JTextArea displayArea;
    /**
     * output stream to server
     */
    private ObjectOutputStream output;
    /**
     * input stream from server
     */
    private ObjectInputStream input;
    /**
     * String message that is sent.
     */
    private String messageOut = "";
    /**
     * String message from server.
     */
    private String messageIn = "";
    /**
     * host server for this application
     */
    private String chatServer;
    /**
     * socket to communicate with server.
     */
    private Socket client;
    /**
     * displayed username for client
     */
    private String username = "";
    /**
     * Variable to write to file.
     */
    private FileWriter fileWriter;
    /**
     * Variable to hold writer.
     */
    private BufferedWriter bWriter;
    /**
     * Variable to hold read-in String from file.
     */
    private BufferedReader bReader;
    /**
     * Contains what image icon is going to be chosen.
     */
    private ImageIcon img;

    /**
     * initialize chatServer and set up GUI
     * @param host hold the reference to other clients.
     */
    public Client(String host) {
        super("Client");
        bWriter = null;
        fileWriter = null;
        String strLine;

        int randomNum = ThreadLocalRandom.current().nextInt(1, 4);
        switch (randomNum)
        {
            case 1: img = new ImageIcon("icons/batmanIcon.png");
                    break;

            case 2: img = new ImageIcon("icons/ironmanIcon.png");
                    break;

            case 3: img = new ImageIcon("icons/decepticonsIcon.png");
                    break;
        }
        setIconImage(img.getImage());

        chatServer = host; // set server to which this client connects

        enterField = new JTextField(); // create enterField
        enterField.setEditable(false);
        enterField.addActionListener(
                new ActionListener() {
                    // send message to server
                    public void actionPerformed(ActionEvent event) {
                        sendData(event.getActionCommand());
                        enterField.setText("");
                        //playSound("koolAidSound.wav");

                    } // end method actionPerformed
                } // end anonymous inner class
        ); // end call to addActionListener

        add(enterField, BorderLayout.SOUTH);

        displayArea = new JTextArea(); // create displayArea
        displayArea.setEditable(false);
        add(new JScrollPane(displayArea), BorderLayout.CENTER);

        setSize(300, 450); // set size of window

        setVisible(true); // show window
        try
        {
            bReader = new BufferedReader(new FileReader("savechat.txt"));
            while((strLine = bReader.readLine()) != null)
            {
                displayMessage(strLine + "\n");
            }

        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    } // end Client constructor

    /**
     * connect to server and process messages from server
     */
    public void runClient() {

        setUsername();
        try // connect to server, get streams, process connection
        {
            connectToServer(); // create a Socket to make connection
            getStreams(); // get the input and output streams
            processConnection(); // process connection
        } // end try
        catch (EOFException eofException) {
            displayMessage("\nClient terminated connection");
        } // end catch
        catch (IOException ioException) {
            ioException.printStackTrace();
        } // end catch
        finally {
            closeConnection(); // close connection
        } // end finally
    } // end method runClient

    /**
     * connect to server
     * @throws IOException when connection fails.
     */
    private void connectToServer() throws IOException {
        // create Socket to make connection to server
        client = new Socket(InetAddress.getByName(chatServer), 23555);
    } // end method connectToServer

    /**
     * get streams to send and receive data
     * @throws IOException if failed to receive streams.
     */
    private void getStreams() throws IOException {
        // set up output stream for objects
        output = new ObjectOutputStream(client.getOutputStream());
        output.flush(); // flush output buffer to send header information

        // set up input stream for objects
        input = new ObjectInputStream(client.getInputStream());
    } // end method getStreams

    /**
     * process connection with server
     * @throws IOException if connections fail to process.
     */
    private void processConnection() throws IOException {
        // enable enterField so client user can send messages
        setTextFieldEditable(true);

        do // process messages sent from server
        {
            try // read message and display it
            {
                messageIn = (String) input.readObject();// read new message
                if (!messageIn.equals(messageOut)){
                    displayMessage("\n" + messageIn); // display message
                    playSound("koolAidSound.wav");
                    messageOut = "";
                }
                else {
                    displayMessage("\n\t\t"+messageIn);
                }
            } // end try
            catch (ClassNotFoundException classNotFoundException) {
                displayMessage("\nUnknown object type received");
            } // end catch

        } while (!messageIn.equals("SERVER: TERMINATE"));
    } // end method processConnection

    /**
     * close streams and socket
     */
    private void closeConnection() {
        displayMessage("\nClosing connection");
        setTextFieldEditable(false); // disable enterField

        try {
            output.close(); // close output stream
            input.close(); // close input stream
            client.close(); // close socket
        } // end try
        catch (IOException ioException) {
            ioException.printStackTrace();
        } // end catch
    } // end method closeConnection

    /**
     * send message to server
     * @param message is sent to server.
     */
    private void sendData(String message) {
        try // send object to server
        {
            messageOut = username+": "+message;
            output.writeObject(messageOut);
            output.flush(); // flush data to output
        } // end try
        catch (IOException ioException) {
            displayArea.append("\nError writing object");
        } // end catch
    } // end method sendData

    /**
     * manipulates displayArea in the event-dispatch thread
     * @param messageToDisplay appends displayArea.
     */
    private void displayMessage(final String messageToDisplay) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() // updates displayArea
                    {
                        displayArea.append(messageToDisplay);
                    } // end method run
                }  // end anonymous inner class
        ); // end call to SwingUtilities.invokeLater
    } // end method displayMessage

    /**
     * manipulates enterField in the event-dispatch thread
     * @param editable makes the field editable.
     */
    private void setTextFieldEditable(final boolean editable) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() // sets enterField's editability
                    {
                        enterField.setEditable(editable);
                    } // end method run
                } // end anonymous inner class
        ); // end call to SwingUtilities.invokeLater
    } // end method setTextFieldEditable

    /**
     * Stores the user name inputted from user.
     */
    private void setUsername() {
        username = JOptionPane.showInputDialog(null,"What is your username?");
        if (username == null) {
            System.exit(0); // cancel closes program
        }
        else if (username.isEmpty()) {
            JOptionPane.showMessageDialog(null, "You need a username", "Error",JOptionPane.ERROR_MESSAGE);
            setUsername();
        }
        super.setTitle(username);
        //displayMessage("Your username is "+username+"\n");
    }

    /**
     * play audio file
     * @param path is the path to the audio file on the machine
     */
    private void playSound(String path) {
        try {
            File audioFile = new File(path);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(audioFile);

            Clip audioClip = AudioSystem.getClip();
            audioClip.open(audioIn);
            audioClip.start();
        }
        catch (UnsupportedAudioFileException exception){
            exception.printStackTrace();
        }
        catch (IOException exception) {
            exception.printStackTrace();
        }
        catch (LineUnavailableException exception) {
            exception.printStackTrace();
        }
    }

} // end class Client
