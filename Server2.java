//
// src/Server1.java 
// @author Brandon Schurman 
//
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Server2
{
    public final static int    SERVER1_PORT = 4444;
    public final static String SERVER1_HOST = "localhost";

    private int port;
    private HashMap<Integer,ArrayList<Byte>> all_data;
    private DatagramSocket socket;
    private InetAddress addr;

    /**
     * e.g. 4445
     */ 
    public Server2 ( int port ) {
        this.port = port;
        this.all_data = new HashMap<Integer,ArrayList<Byte>>();
        this.socket = null;
        try {
			addr = InetAddress.getByName(SERVER1_HOST);
		} catch (UnknownHostException e) {
			System.err.println("error resolving conncetion to Server1");
			e.printStackTrace();
		}
    }

    /**
     * Starts the server on the preinitalized port number
     * the server will listen for incoming client connections over UDP
     */
    public void start ( ) {

        System.out.println("running on port "+port);

        byte[] buffer = new byte[576];

        try {
            this.socket = new DatagramSocket(port);

            while ( true ) {
            	// wait for client to send a data fragment
                DatagramPacket request = receiveRequest(buffer);

                service(request);
            }
        } catch ( SocketException e ) {
            System.out.println("Socket: " 
                    + e.getMessage());
        } catch ( IOException e ) {
            System.out.println("IO: " 
                    + e.getMessage());
        } finally {
            if ( socket != null ) {
                socket.close();
            }
        }
    }

    /**
     * Handle incoming client messages
     */ 
    private void service ( DatagramPacket request ) 
        throws SocketException, IOException {

        String filename = "";
        boolean done_receiving = false;

        byte[] buffer = new byte[80];

        buffer = request.getData();
 
        Message msg = null;
        boolean corrupt = false;
        
        try  { 
        	msg = extractMessage(buffer);
        } catch ( Exception e ) {
        	corrupt = true;
        }
        
        int senderID = msg.getSenderID();
        byte[] data = msg.getData();


        String str = new String(data);
        System.out.println("datagram received");

        ////
        // calculate checksum 
        // compare against client's computed checksum
        if ( corrupt || Message.calculateChecksum(data) 
                != msg.getChecksum() ) {
            buffer = ("$%!err001").getBytes();
            System.err.println(
                    "error: checksums do not match");
        } else {
            buffer = ("success").getBytes();

            if ( str.indexOf("$%done") > -1 )
                done_receiving = true; 
            else {
                System.out.println(" -- adding : "
                        + new String(data));
                ArrayList<Byte> file = all_data.get(senderID);
                if ( file == null ) 
                	file = new ArrayList<Byte>();
                for ( byte b : data )
                    file.add(b);
                all_data.put(senderID, file);
            }
        }

        System.out.println("sending client response");
        sendResponse(request.getPort(), buffer);

        System.out.println("Datagram sent back");

        if ( done_receiving ) { 
            filename = msg.getFilename();
            writeFile(senderID, filename);
        }
    }

    /**
     * write the client's data to a file
     * and clear their buffer
     * @param filename
     * @throws FileNotFoundException
     * @throws IOException
     */
	private void writeFile( int senderID, String filename ) 
			throws FileNotFoundException, IOException {
		ArrayList<Byte> file = all_data.get(senderID);
		byte[] fileBytes = new byte[file.size()];
		for ( int i=0; i<fileBytes.length; i++ ) 
		    fileBytes[i] = file.get(i);
		System.out.println(new String(fileBytes));
		System.out.println("creating file: "+filename);
		FileOutputStream fos 
		    = new FileOutputStream(
		            "./"+filename);
		fos.write(fileBytes);
		fos.flush();
		fos.close();
		
		all_data.put(senderID, new ArrayList<Byte>());
	}

	/**
	 * send a message to server1
	 * @param port
	 * @param buffer
	 * @throws IOException
	 */
	private void sendResponse ( int port, byte[] buffer )
			throws IOException {
		DatagramPacket reply = new DatagramPacket(
                buffer,
                buffer.length, 
                addr,
                port);
        socket.send(reply);
	}
    
    /**
     * extract Message class instance from byte array
     * @throws IOException 
     */
    private Message extractMessage ( byte[] data ) throws IOException {
        Message msg = null;
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
            
        try { 

            msg = (Message)ois.readObject();
        } catch ( Exception e ) { 
            e.printStackTrace();
            System.exit(-1);
        } finally {
    		bis.close();
        	ois.close();
        }
        
        return msg;
    }
    
    /**
     * wait for client request
     * @param buffer
     * @return
     * @throws IOException
     */
	private DatagramPacket receiveRequest(byte[] buffer) throws IOException {
		DatagramPacket request = new DatagramPacket(
		        buffer, 
		        buffer.length);
		socket.receive(request);
		return request;
	}


    /**
     * runs the server from the command line
     * @param args pass the port number as the first argument
     */
    public static void main ( String args[] ) {
        if ( args.length < 1 ) {
            //System.out.println("Usage: java Server2 <Port Number>");
            //System.exit(1);
            new Server2(4400).start();
        } else {
            new Server2(Integer.parseInt(args[0])).start();
        }
    }
}
