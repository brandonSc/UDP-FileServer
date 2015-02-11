//
// src/Client.java
// @author Brandon Schurman
//
import java.net.*;
import java.io.*;

public class Client 
{
    public final String ERR_DATA = "$%!err001";
    public final String DONE = "$%done";
    public final int DATA_MAX_LEN = 80;
    public final int TIMEOUT = 10; 

    private String filepath;
    private DatagramSocket socket;
    private InetAddress addr;
    private String host;
    private int port;
    private int senderID;


    /**
     * e.g. 4444
     */
    public Client ( int port ) {
        this("localhost", port);
    }

    public Client ( String host, int port ) {
        this.host = host;
        this.port = port;
        this.filepath = null;
        this.senderID = (int)(Math.random()*10000); 
        
        try {
			addr = InetAddress.getByName(host);
		} catch ( UnknownHostException e ) {
			System.err.println("\nerror resolving host\n");
			e.printStackTrace();
			System.exit(-1);
		}
    }

    public void setFilepath ( String filepath ) {
        this.filepath = filepath;
    }

    public void start ( ) {
        try {
            this.socket = new DatagramSocket();
            
            // set a timeout on the socket
            this.socket.setSoTimeout(TIMEOUT); 

            // prompt user for a file
            File file = requestFile();

            byte[] bytes = new byte[(int)file.length()];
            FileInputStream fin = new FileInputStream(file);
            fin.read(bytes);
            fin.close();

            boolean done_sending = false;
            int pos = 0;

            ////
            // continue sending datagram fragments
            // until entire file has been sent
            while ( !done_sending ) {
                byte[] frag 
                    = (pos > bytes.length - DATA_MAX_LEN) 
                    ?   new byte[bytes.length-pos]
                    :   new byte[DATA_MAX_LEN];

                // copy 80 bytes of the file at a time
                for ( int i=0; i<DATA_MAX_LEN; i++ ){ 

                    if ( i+pos < bytes.length ) 
                        frag[i] = bytes[pos+i];
                    else { 
                        done_sending = true;
                    }

                }
                pos += DATA_MAX_LEN;

                displayProgress(bytes, pos);

                ////
                // wrap data in a custom message class
                Message msg = new Message(
                		senderID, 
                		file.getName(), 
                		frag);

				byte[] data = getBytes(msg);

                boolean msg_rcvd = false;

                ////
                // loop until server notifies of success 
                while ( !msg_rcvd ) {

                	sendRequest(socket, data);
                    System.out.println("message sent");

                    try {
                        DatagramPacket response = receiveResponse();

                        String res = new String(
                                response.getData());

                        if ( res.indexOf(ERR_DATA) > -1 ) {
                            // server reports an error
                            System.err.println("error: "
                                    + "server received "
                                    + "corrupted message. "
                                    + "resending...");
                            msg_rcvd = false;
                        } else {
                            // message send successful
                            System.out.println(
                                    "\n\tResponse from server: "
                                    + res 
                                    + "\n");
                            msg_rcvd = true;
                            if ( done_sending ) {
                                // notify server  
                                msg = new Message(
                                		senderID,
                                        file.getName(), 
                                        (DONE).getBytes());
                                data = getBytes(msg);
                                sendRequest(socket,data);
                                System.out.println("done.");
                            }
                        }

                    } catch ( SocketTimeoutException e ){
                        // timeout:no response from client
                        System.err.println("timeout: "
                                + "no reply from server. "
                                + "resending...");
                        msg_rcvd = false;
                    }
                }
            }

        } catch ( SocketException e ) {
            System.out.println("Socket: " + e.getMessage());
        } catch ( IOException e ) {
            System.out.println("IO: " + e.getMessage());
        } finally {
            if ( socket != null )
                socket.close();
        }

        return;
    }

	private DatagramPacket receiveResponse() throws IOException {
		byte[] buffer = new byte[80];
		DatagramPacket response 
		    = new DatagramPacket(
		            buffer,
		            buffer.length);
		socket.receive(response);
		return response;
	}
    
    private void sendRequest ( DatagramSocket socket, byte[] data ) throws IOException {
    	DatagramPacket request = new DatagramPacket(
                data, 
                data.length,
                addr,
                port);
        socket.send(request);
    }

    /**
     * convert to byte array
     * @param msg
     * @return
     * @throws IOException
     */
	private byte[] getBytes ( Message msg ) throws IOException {
		ByteArrayOutputStream bos 
		    = new ByteArrayOutputStream();
		ObjectOutputStream oos 
		    = new ObjectOutputStream(bos);
		oos.writeObject(msg);
		oos.close();
		bos.close();
		byte[] data = bos.toByteArray();
		return data;
	}

	/**
	 * percentage of file sent 0-100%
	 * @param bytes
	 * @param pos
	 */
	private void displayProgress ( byte[] bytes, int pos ) {
		int progress = (int)(100*(((double)pos)
		        / ((double)(bytes.length))));
		if ( progress > 100 ) progress = 100;
		System.out.println("\t\t-- Progress: "
		        +progress+ "% --\n");
	}

	/**
	 * prompt user for a file path
	 * if they did not specify one via command line arguments
	 * @return
	 * @throws IOException
	 */
	private File requestFile ( ) throws IOException {
		boolean noFile = true; 
		File file = null;
		
		if ( this.filepath != null ) {
			file = new File(filepath);
			if ( file.exists() && !file.isDirectory() ) 
				noFile = false;
			else 
				file = null;
		}
		
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(System.in));   

		String path = "";

		while ( noFile  && !path.equals("exit") ) {
		    if ( path.equals("") ) {
		        System.out.print(
		                "input a valid file path:> ");
		        path = in.readLine();
		    } else {
		    	file = new File(path);
		        if ( file.exists() && !file.isDirectory() ) 
		        	noFile = false;
		        else {
		            System.out.println("\tinvalid: "
		                    + "the file does not exist");
		            path = "";
		            file = null;
		        }
		    }
		}
		return file;
	}

    public static void main ( String args[] ) {
        boolean filegiven = false;
        if ( args.length == 1 ) {
            try { 
                int port = Integer.parseInt(args[0]);
                new Client(port).start();
            } catch ( Exception e ) {
                filegiven = true;
            }
        } else if ( args.length == 2 ) {
            int port = Integer.parseInt(args[1]);
            new Client(args[0], port).start();
        } else if ( filegiven ) {
            Client c = new Client(4000);
            c.setFilepath(args[0]);
            c.start();
        } else {
            //System.out.println("Usage: UDPClient <host> <port>");
            //System.out.println("- or - UDPClient <port>");
            new Client(4000).start();
        }
    }
}
