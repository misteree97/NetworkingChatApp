// Modified Fig. 27.5: Multi-threaded Chat Server.java
// Server portion of a client/server stream-socket connection. 

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public class Server extends JFrame {
    //private JTextField enterField; // inputs message from user
    private JTextArea displayArea; // display information to user
    private ExecutorService executor; // will run players
    private ServerSocket server; // server socket
    private SockServer[] sockServer; // Array of objects to be threaded
    private int counter = 1; // counter of number of connections
    private int nClientsActive = 0; // Number of clients connected
    private static final String FILENAME = "savechat.txt"; // Name of the files to store history of the messages.
    private FileWriter fileWriter; // Writes to the file.
    private BufferedWriter bWriter;
    private BufferedReader bReader;
    private JButton clearButton;
    private File chatFile;

    private ImageIcon img;


    // set up GUI
    public Server() {
        super("Server");
        bWriter = null;
        fileWriter = null;
        String strLine;
        img = new ImageIcon("icons/robotIcon.png");
        setIconImage(img.getImage());

        try
        {
            chatFile = new File(FILENAME);
            if(!chatFile.exists())
            {
                chatFile.createNewFile();
                fileWriter = new FileWriter(chatFile, false);
            }
            else
            {
                fileWriter = new FileWriter(chatFile, true);
            }
            bWriter = new BufferedWriter(fileWriter);
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
        clearButton = new JButton("Clear");
        clearButton.addActionListener(new ClearListener());

        sockServer = new SockServer[100]; // allocate array for up to 100 server threads
        executor = Executors.newFixedThreadPool(100); // create thread pool
/*
        enterField = new JTextField(); // create enterField
        enterField.setEditable(false);
        enterField.addActionListener(
                new ActionListener() {
                    // send message to client
                    public void actionPerformed(ActionEvent event) {
                        // Just got text from Server GUI Textfield
                        // Now send this to each client -- broadcast mode
                        for (int i = 1; i <= counter; i++) {
                            if (sockServer[i].alive == true)
                                sockServer[i].sendData(event.getActionCommand());
                        }
                        enterField.setText("");
                    } // end method actionPerformed
                } // end anonymous inner class
        ); // end call to addActionListener

        add(enterField, BorderLayout.SOUTH);
        */
        add(clearButton, BorderLayout.NORTH);

        displayArea = new JTextArea(); // create displayArea
        displayArea.setEditable(false);
        add(new JScrollPane(displayArea), BorderLayout.CENTER);


        setSize(300, 300); // set size of window
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
    } // end Server constructor

    // set up and run server
    public void runServer() {
        try // set up server to receive connections; process connections
        {
            server = new ServerSocket(23555, 100); // create ServerSocket

            while (true) {
                try {
                    //create a new runnable object to serve the next client to call in
                    sockServer[counter] = new SockServer(counter);
                    // make that new object wait for a connection on that new server object
                    sockServer[counter].waitForConnection();
                    nClientsActive++;
                    // launch that server object into its own new thread
                    executor.execute(sockServer[counter]);
                    // then, continue to create another object and wait (loop)

                } // end try
                catch (EOFException eofException) {
                    displayMessage("\nServer terminated connection");
                } // end catch
                finally {
                    ++counter;
                } // end finally
            } // end while
        } // end try
        catch (IOException ioException) {
            ioException.printStackTrace();
        } // end catch
    } // end method runServer

    // manipulates displayArea in the event-dispatch thread
    private void displayMessage(final String messageToDisplay) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() // updates displayArea
                    {
                        displayArea.append(messageToDisplay); // append message
                    } // end method run
                } // end anonymous inner class
        ); // end call to SwingUtilities.invokeLater
    } // end method displayMessage

    /*
    // manipulates enterField in the event-dispatch thread
    private void setTextFieldEditable(final boolean editable) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() // sets enterField's editability
                    {
                        //enterField.setEditable(editable);
                    } // end method run
                }  // end inner class
        ); // end call to SwingUtilities.invokeLater
    } // end method setTextFieldEditable
    */

    /* This new Inner Class implements Runnable and objects instantiated from this
     * class will become server threads each serving a different client
     */
    private class SockServer implements Runnable {
        private ObjectOutputStream output; // output stream to client
        private ObjectInputStream input; // input stream from client
        private Socket connection; // connection to client
        private int myConID;
        private String myUserID;
        private boolean alive = false;

        public SockServer(int num) {
            myConID = num;
        }

        public void run() {
            try {
                alive = true;
                try {
                    getStreams(); // get input & output streams
                    processConnection(); // process connection
                    nClientsActive--;

                } // end try
                catch (EOFException eofException) {
                    displayMessage("\nServer" + myConID + " terminated connection");
                } finally {
                    closeConnection(); //  close connection
                }// end catch
            } // end try
            catch (IOException ioException) {
                ioException.printStackTrace();
            } // end catch
        } // end try

        // wait for connection to arrive, then display connection info
        private void waitForConnection() throws IOException {

            //displayMessage("Waiting for users" + "\n");
            connection = server.accept(); // allow server to accept connection
            //displayMessage("\n Connection " + myConID + " received from: " +
            //connection.getInetAddress().getHostName());
            //displayMessage("\nConnection Received");
        } // end method waitForConnection

        private void getStreams() throws IOException {
            // set up output stream for objects
            output = new ObjectOutputStream(connection.getOutputStream());
            output.flush(); // flush output buffer to send header information

            // set up input stream for objects
            input = new ObjectInputStream(connection.getInputStream());

            //displayMessage("\nGot I/O streams\n");
        } // end method getStreams

        // process connection with client
        private void processConnection() throws IOException {
            String message = "Connection successful";
            //sendData(message); // send connection successful message

            // enable enterField so server user can send messages
            //setTextFieldEditable(true);

            do // process messages sent from client
            {
                try // read message and display it
                {
                    message = (String) input.readObject(); // read new message
                    displayMessage("\n" +  message); // display message //////////
                    String temp = botInputProcess(message);

                    for (int i = 1; i <= counter; i++) {
                        if (sockServer[i].alive == true)
                        {
                            sockServer[i].sendData(message);
                            sockServer[i].sendData(temp);
                        }
                    }

                    bWriter.write(message);
                    bWriter.newLine();
                } // end try
                catch (ClassNotFoundException classNotFoundException) {
                    displayMessage("\nUnknown object type received");
                } // end catch

            } while (!message.contains("TERMINATE"));
        } // end method processConnection

        // close streams and socket
        private void closeConnection() {
            //displayMessage("\nTerminating connection " + myConID + "\n");
            //displayMessage("\nNumber of connections = " + nClientsActive + "\n");
            alive = false;
            /*
            if (nClientsActive == 0) {
                setTextFieldEditable(false); // disable enterField
            }
            */

            try {
                output.close(); // close output stream
                input.close(); // close input stream
                connection.close(); // close socket
                bWriter.close(); //closes writer
            } // end try
            catch (IOException ioException) {
                ioException.printStackTrace();
            } // end catch
        } // end method closeConnection

        private void sendData(String message) {
            try // send object to client
            {
                output.writeObject(message);
                //output.writeObject("SERVER" + myConID + ": " + message);
                output.flush(); // flush output to client
                //displayMessage("\nSERVER" + myConID + ": " + message);
            } // end try
            catch (IOException ioException) {
                displayArea.append("\nError writing object");
            } // end catch
        } // end method sendData

        /*
        public void botSay(String s)
        {
            displayMessage("\nAnnoying Chad: " + s + "\n");
            sendData("Annoying Chad: " + s + "\n");
        }
        */

        public String botInputProcess(String s)
        {
            String msg = "";
            if(s.contains("Hi") || s.contains("Hello") || s.contains("hi") || s.contains("hello") || s.contains("hey") || s.contains("Hey"))
            {
                displayMessage("\nAnnoying Chad: What up boss?\n");
                msg = "Annoying Chad: What up boss?";
            }
            else if(s.contains("how are you"))
            {
                displayMessage("\nAnnoying Chad: I'm feeling good today.\n");
                msg = "Annoying Chad: I'm feeling good today";
            }
            else
            {
                int decider = (int)(Math.random()*3+1);
                if(decider ==1)
                {
                    displayMessage("\nAnnoying Chad: Oh, interesting...\n");
                    msg = "\nAnnoying Chad: Oh, interesting...\n";
                }
                else if (decider ==2)
                {
                    displayMessage("\nAnnoying Chad: Oh, I don't care.\n");
                    msg = "Annoying Chad: Oh, I really don't care.\n";
                }
                else
                {
                    displayMessage("\nAnnoying Chad: What, sorry can you repeat that???\n");
                    msg = "Annoying Chad: What, sorry can you repeat that???\n";
                }
            }
            return msg;
        }


    } // end class SockServer
    private class ClearListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            try
            {
                chatFile.createNewFile();
                fileWriter = new FileWriter(chatFile, false);
                bWriter = new BufferedWriter(fileWriter);
                bWriter.write("");
            }
            catch(IOException ex)
            {
                ex.printStackTrace();
            }
            displayArea.setText(""); //sets server display to blank
        }
    }

} // end class Server


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