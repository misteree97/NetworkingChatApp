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

public class Client extends JFrame {
    private JTextField enterField; // enters information from user
    private JTextArea displayArea; // display information to user
    private ObjectOutputStream output; // output stream to server
    private ObjectInputStream input; // input stream from server
    private String messageOut = "";
    private String messageIn = ""; // message from server
    private String chatServer; // host server for this application
    private Socket client; // socket to communicate with server
    private String username = ""; //displayed username for client
    private FileWriter fileWriter;
    private BufferedWriter bWriter;
    private BufferedReader bReader;

    private ImageIcon img; // Contains what image icon is going to be chosen.

    // initialize chatServer and set up GUI
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

    // connect to server and process messages from server
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

    // connect to server
    private void connectToServer() throws IOException {
        //displayMessage("Attempting connection\n");

        // create Socket to make connection to server
        client = new Socket(InetAddress.getByName(chatServer), 23555);

        // display connection information
        //displayMessage("Connected to: " +client.getInetAddress().getHostName());
    } // end method connectToServer

    // get streams to send and receive data
    private void getStreams() throws IOException {
        // set up output stream for objects
        output = new ObjectOutputStream(client.getOutputStream());
        output.flush(); // flush output buffer to send header information

        // set up input stream for objects
        input = new ObjectInputStream(client.getInputStream());

        //displayMessage("\nGot I/O streams\n");
    } // end method getStreams

    // process connection with server
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

    // close streams and socket
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

    // send message to server
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

    // manipulates displayArea in the event-dispatch thread
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

    // manipulates enterField in the event-dispatch thread
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

/**************************************************************************
 * (C) Copyright 1992-2012 by Deitel & Associates, Inc. and               *
 * Pearson Education, Inc. All Rights Reserved.                           *
 *                                                                        *
 * DISCLAIMER: The authors and publisher of this book have used their     *
 * best efforts in preparing the book. These efforts include the          *
 * development, research, and testing of the theories and programs        *
 * to determine their effectiveness. The authors and publisher make       *
 * no warranty of any kind, expressed or implied, with regard to these    *
 * programs or to the documentation contained in these books. The authors *
 * and publisher shall not be liable in any event for incidental or       *
 * consequential damages in connection with, or arising out of, the       *
 * furnishing, performance, or use of these programs.                     *
 *************************************************************************/
