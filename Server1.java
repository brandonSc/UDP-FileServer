//
// src/Server1.java 
// @author Brandon Schurman 
//
import java.net.*;
import java.io.*;

public class Server1 
{
    public final static int    SERVER2_PORT = 4400;
    public final static String SERVER2_HOST = "localhost";
    
    private int port;
    private DatagramSocket socket;
    private InetAddress addr;

    /**
     * e.g. 4000
     */ 
    public Server1 ( int port ) {
        this.port = port;
        this.socket = null;
        try {
			this.addr = InetAddress.getByName(SERVER2_HOST);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(-1);
		}
    }

    /**
     * Starts the server on the preinitalized port number
     * the server will listen for incoming client connections over UDP
     */
    public void start ( ) {

        System.out.println("running on port "+port);


        try {
            socket = new DatagramSocket(port);
            byte[] buffer = new byte[576];
           
            while ( true ) {
                DatagramPacket request = new DatagramPacket(
                        buffer, 
                        buffer.length);
                socket.receive(request);

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
        System.out.println("client disconnect");
    }

    /**
     * Handle incoming client messages
     */ 
    private void service ( DatagramPacket request ) 
        throws SocketException, IOException {

        DatagramSocket server2_conn = new DatagramSocket();

        byte[] buffer = request.getData();
        Message msg = extractMessage(buffer);

        if ( msg == null ) {
        	return;
        }
        
        byte[] data =  msg.getData();
        String str = new String(data);
        boolean done = str.indexOf("$%done") > -1;
        System.out.println("Datagram received");

        double r = Math.random();

        ////
        // only send the message to server2 75% of time
        if ( r < 0.75 || done ) {

            ////
            // 50% of the time, corrupt the data
            if ( r < 0.5  && !done ) {
                System.out.println("corrupting data..");
                data = messWith(data);
                msg.setData(data);
                buffer = getBytes(msg);
            }            

            // send the message to server2
            System.out.println("forwarding to server2...");
            forwardServer2(server2_conn, buffer);
            
            // wait for response from server 2
            System.out.println("receiving from server2...");
            DatagramPacket server_req = receiveResponse(server2_conn);
            
            // pass response back to client
            System.out.println("sending message back to client...");
            DatagramPacket reply = new DatagramPacket(
                    server_req.getData(),
                    server_req.getLength(), 
                    request.getAddress(),
                    request.getPort());
            socket.send(reply);
        } 
        else System.out.println("dropping packet"); // r > 0.75
    }

	private DatagramPacket receiveResponse(DatagramSocket server2_conn)
			throws IOException {
		byte[] server_buff = new byte[80];
		DatagramPacket server_res = new DatagramPacket(
		        server_buff,
		        server_buff.length);
		server2_conn.receive(server_res);
		return server_res;
	}

	private void forwardServer2 ( DatagramSocket server2_conn, 
			byte[] buffer ) throws IOException {
		DatagramPacket server_req = new DatagramPacket(
		        buffer, 
		        buffer.length,
		        addr,
		        SERVER2_PORT);
		server2_conn.send(server_req);
	}

	private byte[] getBytes(Message msg) throws IOException {
		byte[] buffer;
		ByteArrayOutputStream bos 
		    = new ByteArrayOutputStream();
		ObjectOutputStream oos 
		    = new ObjectOutputStream(bos);
		oos.writeObject(msg);
		oos.close();
		bos.close();
		buffer = bos.toByteArray();
		return buffer;
	}

    /**
     * extract Message class from byte array
     * @param buffer
     * @param msg
     * @return
     */
	private Message extractMessage(byte[] buffer ) {
		Message msg = null;
		try { 
            ByteArrayInputStream bis 
                = new ByteArrayInputStream(buffer);
            ObjectInputStream ois 
                = new ObjectInputStream(bis);

            msg = (Message)ois.readObject();
        } catch ( Exception e ) {
            System.err.println(e);
        }
		return msg;
	}

    private byte[] messWith ( byte[] data ) {
        String str = new String(data);
        int len = str.length();
        int j = (int)(Math.random() * len);
        int i = (int)(Math.random() * j);
        str = str.substring(0, i) + str.substring(j, str.length()-1);
        return str.getBytes();
    }

    /**
     * runs the server from the command line
     * defualt is port 4000 on localhost
     * @param args pass the port number as the first argument
     */
    public static void main ( String args[] ) {
        if ( args.length < 1 ) {
            //System.out.println("Usage: java Server1 <Port Number>");
            //System.exit(1);
            new Server1(4000).start();
        } else {
            new Server1(Integer.parseInt(args[0])).start();
        }
    }
}
