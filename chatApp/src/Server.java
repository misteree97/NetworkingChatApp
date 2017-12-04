/***************************************************************************************
 *    Title: Fig 28.4
 *    Author: Java How to Program (early objects) 10th Edition
 *    Date: 03 December 2017
 *    Code version: 10th Edition
 *    Availability: Chapter 28, Fig 28.4
 *
 *    Took this code from the textbook, and modified it to open a text file.
 *
 ***************************************************************************************/

// Modified Fig. 27.5: Multi-threaded Chat Server.java
// Server portion of a client/server stream-socket connection. 

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server extends JFrame
{
    /**
     * Name of the files to store history of the messages.
     */
    private static final String FILENAME = "savechat.txt";
    /**
     * display information to user
     */
    private JTextArea displayArea;
    /**
     * will run clients
     */
    private ExecutorService executor;
    /**
     * server socket
     */
    private ServerSocket server;
    /**
     * Array of objects to be threaded
     */
    private SockServer[] sockServer;
    /**
     * counter of number of connections
     */
    private int counter = 1;

    /**
     * Number of clients connected
     */
    private int nClientsActive = 0;

    /**
     * Writes to the file.
     */
    private FileWriter fileWriter;

    /**
     * Store write value
     */
    private BufferedWriter bWriter;

    /**
     * Stores read value.
     */
    private BufferedReader bReader;

    /**
     * Button to clear values.
     */
    private JButton clearButton;

    /**
     * File where message history is saved.
     */
    private File chatFile;

    /**
     * Variable to story reference to icon.
     */
    private ImageIcon img;


    /**
     * Creates, sets, and adds to gui.
     */
    public Server() {
        super("Server");
        bWriter = null;
        fileWriter = null;
        String strLine;
        img = new ImageIcon("icons/robotIcon.png");
        setIconImage(img.getImage());

        /**
         * checks to see if a file to write to has been created, if not it will create one and set up the writer
         */
        try {
            chatFile = new File(FILENAME);
            if (!chatFile.exists()) //if the file doesn't exist
            {
                chatFile.createNewFile();
                fileWriter = new FileWriter(chatFile, false); //will write to the beginning of the file
            } else {
                fileWriter = new FileWriter(chatFile, true);//will append to the end of the file
            }
            bWriter = new BufferedWriter(fileWriter);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        clearButton = new JButton("Clear");
        clearButton.addActionListener(new ClearListener());

        sockServer = new SockServer[100]; // allocate array for up to 100 server threads
        executor = Executors.newFixedThreadPool(100); // create thread pool
        add(clearButton, BorderLayout.NORTH);

        displayArea = new JTextArea(); // create displayArea
        displayArea.setEditable(false);
        add(new JScrollPane(displayArea), BorderLayout.CENTER);


        setSize(300, 300); // set size of window
        setVisible(true); // show window
        try {
            bReader = new BufferedReader(new FileReader("savechat.txt"));
            while ((strLine = bReader.readLine()) != null) {
                displayMessage(strLine + "\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    } // end Server constructor

    /**
     * Set up, and run server.
     */
    public void runServer() {
        try // set up server to receive connections; process connections
        {
            server = new ServerSocket(23555, 100); // create ServerSocket

            while (true) {
                try {
                    //create a new runnable object to serve the next client to call in
                    sockServer[counter] = new SockServer();
                    // make that new object wait for a connection on that new server object
                    sockServer[counter].waitForConnection();
                    nClientsActive++;
                    // launch that server object into its own new thread
                    executor.execute(sockServer[counter]);
                    // then, continue to create another object and wait (loop)

                } // end try
                catch (EOFException eofException) {
                    //displayMessage("\nServer terminated connection");
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

    /**
     * Manipulates displayArea in the event-dispatch thread
     * @param messageToDisplay is set to append displayArea.
     */
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

    /**
     * Private classes to have each socket thread runnable.
     */
    private class SockServer implements Runnable {
        /**
         * output stream to client
         */
        private ObjectOutputStream output;
        /**
         * input stream from client
         */
        private ObjectInputStream input;
        /**
         * connection to client
         */
        private Socket connection;
        /**
         * Stores true if socket is currently connected.
         */
        private boolean alive = false;

        /**
         * Checks, connects, and process the streams.
         */
        public void run() {
            try {
                alive = true;
                try {
                    getStreams(); // get input & output streams
                    processConnection(); // process connection
                    nClientsActive--;

                } // end try
                catch (EOFException eofException) {
                    //displayMessage("\nServer" + myConID + " terminated connection");
                } finally {
                    closeConnection(); //  close connection
                }// end catch
            } // end try
            catch (IOException ioException) {
                ioException.printStackTrace();
            } // end catch
        } // end try


        /**
         * Waits for connection to arrive, then display connection info
         * @throws IOException if no connection can be accepted.
         */
        private void waitForConnection() throws IOException
        {
            connection = server.accept();
        } // end method waitForConnection

        /**
         * Checks to see if any stream objects can be accepted.
         * @throws IOException is given if fails to set-up and see streams.
         */
        private void getStreams() throws IOException
        {
            // set up output stream for objects
            output = new ObjectOutputStream(connection.getOutputStream());
            output.flush(); // flush output buffer to send header information

            // set up input stream for objects
            input = new ObjectInputStream(connection.getInputStream());
        } // end method getStreams

        /**
         * Process connection with client
         * @throws IOException is given if connection wasn't able to connect successfully.
         */
        private void processConnection() throws IOException {
            String message = "Connection successful";
            do // process messages sent from client
            {
                try // read message and display it
                {
                    message = (String) input.readObject(); // read new message
                    displayMessage(message); // display message //////////
                    String temp = botInputProcess(message);

                    for (int i = 1; i <= counter; i++) {
                        if (sockServer[i].alive == true) {
                            sockServer[i].sendData(message);
                            sockServer[i].sendData(temp);
                        }
                    }
                    bWriter.write(message);
                    bWriter.newLine();
                    bWriter.write(temp);
                    bWriter.newLine();
                } // end try
                catch (ClassNotFoundException classNotFoundException) {
                    displayMessage("\nUnknown object type received");
                } // end catch

            } while (!message.contains("TERMINATE"));
        } // end method processConnection

        /**
         * Close streams and socket
         */
        private void closeConnection()
        {
            alive = false;
            try
            {
                output.close(); // close output stream
                input.close(); // close input stream
                connection.close(); // close socket
                bWriter.close(); //closes writer
                playSound("Oh no sound effect (1).wav");
            } // end try
            catch (IOException ioException) {
                ioException.printStackTrace();
            } // end catch
        } // end method closeConnection

        /**
         * // Send object to client
         * @param message is
         */
        private void sendData(String message) {
            try
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

        /**
         * Process the input from the chat interface.
         * @param s is compared to several Strings.
         * @return msg which is String based on s.
         */
        public String botInputProcess(String s) {
            String msg = "";
            s = s.toLowerCase();
            if (s.contains("hi") || s.contains("hello") || s.contains("hey")) {
                displayMessage("\nAnnoying Chad: What up boss?\n");
                msg = "Annoying Chad: What up boss?";
            } else if (s.contains("how are you")) {
                displayMessage("\nAnnoying Chad: I'm feeling good today.\n");
                msg = "Annoying Chad: I'm feeling good today";
            } else {
                int decider = (int) (Math.random() * 3 + 1);
                if (decider == 1) {
                    displayMessage("\nAnnoying Chad: Oh, interesting...\n");
                    msg = "\nAnnoying Chad: Oh, interesting...\n";
                } else if (decider == 2) {
                    displayMessage("\nAnnoying Chad: Oh, I don't care.\n");
                    msg = "Annoying Chad: Oh, I really don't care.\n";
                } else {
                    displayMessage("\nAnnoying Chad: What, sorry can you repeat that???\n");
                    msg = "Annoying Chad: What, sorry can you repeat that???\n";
                }
            }
            return msg;
        }


    } // end class SockServer

    private class ClearListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            try {
                /**
                 * creates new file to write to
                 */
                chatFile.createNewFile();
                /**
                 * sets up fileWriter to write to the new file
                 */
                fileWriter = new FileWriter(chatFile, false);
                bWriter = new BufferedWriter(fileWriter);

                bWriter.write("");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            displayArea.setText(""); //sets server display to blank
        }
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

} // end class Server
