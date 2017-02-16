package chat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 *
 * @author Amedeo
 */
public class Client {
    
    private String ip;  //ip al quale si connettera'
    private int port;   //porta alla quale effettuare la richiesta
    
    private Socket sock;    //socket della connessione
    private boolean link;   //sara' true se la connessione andra' a buon fine
    
    private Client(String ip, int port){
        
        this.ip = ip;
        this.port = port;
        
        //verra' settata true se e solo se andra' TUTTO a buon fine
        this.link = false;
        
        try {
            //richiedo una connessione
            this.sock = new Socket(this.ip, this.port);
            //tutto andato a buon fine
            this.link = true;
        } catch (UnknownHostException uhe) {
            System.err.println("Couldn't find address");
        } catch (SecurityException se) {
            System.err.println("Couldn't obtain permissions");
        } catch (IOException ioe) {
            System.err.println("Couldn't create socket");
        }
    }
    
    private boolean isLinked(){
        return this.link;
    }
    
    private void close(){
        if(sock != null){
            try {
                sock.close();
            } catch (IOException ioe) {
                System.err.println("Couldn't close socket");
            }
        }
    }
    
    private InputStream getInputStream(){
        try {
            return sock.getInputStream();
        } catch (IOException ioe) {
            System.err.println("Input Socket is not linked");
        }
        return null;
    }
    
    private OutputStream getOutputStream(){
        try {
            return sock.getOutputStream();
        } catch (IOException ex) {
            System.err.println("Output Socket is not linked");
        }
        return null;
    }
    
    public static void main(String[] args){
        
        //creo un client
        Client client = new Client(Server.IP, Server.PORT);
        
        //se non ci sono stati problemi ad instaurare una connessione
        if(client.isLinked()){
            //inizio una chat
            new Chat(client.getInputStream(), client.getOutputStream(), false);
        }
        
        //termino il client
        client.close();
        
    }
    
}
