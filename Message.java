import java.io.Serializable;

public class Message implements Serializable 
{
    private static final long serialVersionUID = -4507489610617393544L;
    
    private byte[] data;
    private int checksum;
    private String filename;
    private int senderID;
    
    public Message ( int senderID, String filename, byte[] data ) {
    	this.senderID = senderID;
        this.data = data;
        this.checksum = calculateChecksum(data);
        this.filename = filename;
    }

    /**
     * calculate a checksum from the given data
     */ 
    public static int calculateChecksum ( byte[] data ) {
        return (int)(new String(data)).hashCode();
    }

    public void setData ( byte[] data ) {
        this.data = data;
    }
    
    public byte[] getData ( ) {
        return this.data;
    }

    public int getChecksum ( ) {
        return this.checksum;
    }
    
    public String getFilename ( ) {
        return this.filename;
    }
    
    public int getSenderID (  ) {
    	return this.senderID;
    }
}
