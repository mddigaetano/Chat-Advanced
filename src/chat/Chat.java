package chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * This class is the actual core of the chat.
 * It provides every method needed to write, listen and also understand
 * special commands that trigger an event.
 * 
 * @author Amedeo
 */
public class Chat {

    private static final String END = "(end)";  //stringa di passaggio "facolta' di parola"

    private static final int KEY = 10;  //chiave di cifratura

    private static final String RESET_COLOR = "\u001B[0m";  //stringa per terminare la colorazione
    private static final String YOUR_COLOR = "\u001B[45m";  //stringa per colorare lo sfondo del locale (viola)
    private static final String ITS_COLOR = "\u001B[44m";   //stringa per colorare lo sfondo del remoto (blu)

    private final InputStream is;   //input stream come da parametro costruttore, utile per wrapparlo per ogni esigenza
    private Scanner in; //input stream wrappato per ricevere caratteri

    private final OutputStream os;  //output stream come da parametro costruttore, utile per wrapparlo per ogni esigenza
    private PrintWriter out;    //output stream wrappato per inviare caratteri

    private String yourName;    //nome locale
    private Status yourStatus;  //status locale

    private String itsName; //nome remoto
    private Status itsStatus;   //status remoto

    private ArrayList<String> lastMessage;  //ultimo messaggio ricevuto (diviso in righe)

    private boolean over;   //se la chat e' terminata, settato a true

    /**
     * The constructor needs to have generic InputStream and OutputStream,
     * respectively to read and write. It also wants to know if the caller needs
     * to be a server or a client, in order to synchronize them.
     * 
     * @param inputStream the input stream
     * @param outputStream the output stream
     * @param server true if the caller is a server
     */
    public Chat(InputStream inputStream, OutputStream outputStream, boolean server) {

        this.yourName = "You";  //nome locale di default
        this.yourStatus = Status.AVAILABLE; //status locale di default

        this.itsName = "???";   //nome remoto di default
        this.itsStatus = Status.AVAILABLE;  //status remoto di default

        this.is = inputStream;
        this.os = outputStream;

        this.over = false;  //la chat e' appena iniziata

        if (inputStream != null && outputStream != null) {  //se entrambi gli stream passati sono validi
            //li wrappo per utilizzarli con i caratteri
            this.in = new Scanner(inputStream);
            this.out = new PrintWriter(outputStream);

            if (server) {   //se e' un server, da'(accento, non apostrofo) un messaggio di benvenuto...
                //giusto per sfasare altrimenti sia il client che il server vorrebbero ricevere
                //allo stesso momento
                this.welcome();
            }
            while (!over) { //se la chat non e' terminata
                this.lastMessage = this.receive();  //salvo l'ultimo messaggio ricevuto
                this.menu();    //smisto in caso di comando inviato
            }

            System.out.println("Bye!"); //messaggio di conferma chiusura

            //chiudo gli stream di input e output (i wrapper verranno eliminati dal garbage collector)
            //non che cosi' deallochi gli stream, beninteso...
            in.close();
            out.close();

        } else {
            System.err.println("At least one of the streams is not valid");
        }

    }

    private void welcome() {
        //invio il messaggio di benvenuto (che non e' terminato) e la lista di comandi disponibili
        this.send("Welcome! What would you like to do?", false);
        this.help();
    }

    private ArrayList<String> receive() {
        //istanzio un array inizialmente vuoto (una riga = un elemento)
        ArrayList<String> message = new ArrayList<>();
        //leggo la linea, decifrandola
        String temp = decipher(in.nextLine());

        while (!temp.equals(Chat.END)) {    //se la riga ricevuta non e' il terminatore
            message.add(temp);  //aggiungo la linea
            temp = decipher(in.nextLine()); //ne leggo un'altra, decifrandola
        }

        return message;
    }

    //smistamento dei messaggi ricevuti
    private void menu() {
        if (lastMessage.get(0).equals("/help")) { //comando per listare i comandi
            this.help();
            
        } else if (lastMessage.get(0).equals("/close")) {   //comando per chiudere la connessione
            this.over = true;   //basta settare la variabile per finire la chat
            
        } else if (lastMessage.get(0).startsWith("/name")) {    //comando per cambiare il proprio nome
            System.out.print(this.itsPrompt() + "changed its name in ");    //stampo nome vecchio
            if (lastMessage.get(0).length() > 5) {  //se ho effettivamente scritto qualcosa
                this.itsName = lastMessage.get(0).substring(6); //setto il nuovo nome
            } else {
                this.itsName = "???";   //nome di default in caso di errore
            }
            System.out.println(this.itsName);   //stampo il nuovo nome
            this.send();    //pronto ad inviare
            
        } else if (lastMessage.get(0).startsWith("/status")) {  //comando per cambiare status
            String stat;
            if (lastMessage.get(0).length() > 7) {  //se ho scritto qualcosa dopo il comando
                stat = lastMessage.get(0).substring(8); //vedo che ho scritto
            } else {
                stat = "AVAILABLE"; //stato di default
            }

            if (stat.equalsIgnoreCase("BUSY")) {    //se riconosco busy, bene
                this.itsStatus = Status.BUSY;
            } else {    //altrimenti metto il default
                this.itsStatus = Status.AVAILABLE;
            }

            System.out.println(this.itsPrompt() + "changed its status");    //prompt gia' modificato
            this.send();    //pronto ad inviare
            
        } else if (lastMessage.get(0).startsWith("/file")) {    //comando teasferimento file
            //prendo solo nome file (il mittente ha gia' controllato che mi puo' effettivamente inviare il file)
            String filename = lastMessage.get(0).substring(lastMessage.get(0).lastIndexOf("/") + 1); //a partire dall'ultimo slash
            File file = new File("./" + filename);  //salvo con lo stesso nome nella cartella attuale
            
            if (!file.exists()) {   //se il file non esiste
                this.receiveFile(file); //ricevo il file
                System.out.println(this.itsPrompt() + filename + " received");
            } else {
                //se il file esiste gia' uso nome di default, senza estensione
                System.out.println(this.itsPrompt() + "The file already exists: renaming it to \"file\"");
                file = new File("./file");
                this.receiveFile(file);
            }
            
            this.send();    //pronto a inviare
            
        } else {    //se non ho riconosciuto nessun comando
            for (String temp : lastMessage) {   //stampo tutte le righe
                System.out.println(this.itsPrompt() + temp);
            }
            this.send();    //pronto ad inviare
        }
    }

    //smistamento comandi in invio
    private void send() {
        Scanner keyboard = new Scanner(System.in);

        //stampo il prompt e leggo da tastiera
        System.out.print(this.yourPrompt());
        String line = keyboard.nextLine();

        if (line.startsWith("/close")) {    //comando chiusura connessione
            this.over = true;   //setto variabile chiusura localmente
            this.send(line, true);  //dico a remoto di chiudere
            
        } else if (line.startsWith("/echo")) {  //riinvio l'ultimo messaggio ricevuto
            for (String temp : lastMessage) {   //riga per riga
                this.send(temp, false); //invio senza terminare
            }
            this.endTransfer(); //passo e chiudo
            
        } else if (line.startsWith("/smile")) { //comando per inviare smile
            this.smile();
            
        } else if (line.startsWith("/like")) {  //comando per inviare like
            this.like();
            
        } else if (line.startsWith("/name")) {  //comando per cambiare nome
            if (line.length() > 5) {    //se valido
                this.yourName = line.substring(6);  //lo imposto
            } else {
                this.yourName = "???";  //alrimenti metto quello di default
            }
            this.send(line, true);  //invio il comando a remoto
            
        } else if (line.startsWith("/status")) {    //comanfo per cambiare status
            String stat;

            if (line.length() > 7) {    //se valido
                stat = line.substring(8);   //lo prendo
            } else {
                stat = "AVAILABLE"; //altrimenti default
            }

            if (stat.equalsIgnoreCase("BUSY")) {    //se corrisponde a busy
                this.yourStatus = Status.BUSY;      //lo imposto
            } else {
                this.yourStatus = Status.AVAILABLE; //altrimenti metto disponibile
            }
            this.send(line, true);  //invio comando
            
        } else if (line.startsWith("/file")) {  //comando trasferimento file
            File file;

            if (line.length() > 5) {    //se ho effettivamente scritto qualcosa
                file = new File(line.substring(6)); //prelevo il nome
                if (file.isFile() && file.exists()) {   //se e' un file ed esiste
                    this.send(line, true);  //invio il comando
                    this.sendFile(file);    //invio il file
                } else {
                    System.out.println(this.yourPrompt() + "Can't Access File");    //messaggio di errore locale
                    send("~*Sytax Error*~", true);  //messaggio di errore remoto
                }
            } else {
                System.out.println(this.yourPrompt() + "~*Sytax Error*~");  //messaggio di errore locale...
                this.send("~*Sytax Error*~", true); //... e remoto
            }
            
        } else {    //se non riconosco nessun comando
            this.send(line, true);  //invio normalmente
        }
    }

    private void send(String message, boolean end) {
        out.println(cipher(message));   //invio, cifrando il messaggio
        if (end) {  //se voglio anche passare il testimone
            this.endTransfer(); //invio terminatore
        }
    }

    private String yourPrompt() {
        //prompt composto dal nome (colorato)
        String prompt = Chat.YOUR_COLOR + this.yourName;

        //se lo stato e' occupato lo scrivo
        if (this.yourStatus == Status.BUSY) {
            prompt += " (Busy)";
        }

        //dopo il > si puo' scrivere
        prompt += "> " + Chat.RESET_COLOR;  //termino la colorazione

        return prompt;
    }

    private String itsPrompt() {
        //prompt composto dal nome (colorato)
        String prompt = Chat.ITS_COLOR + this.itsName;

        //se lo stato e' occupato lo scrivo
        if (this.itsStatus == Status.BUSY) {
            prompt += " (Busy)";
        }

        //dopo il > si puo' scrivere
        prompt += "> " + Chat.RESET_COLOR;  //termino la colorazione

        return prompt;
    }

    private void help() {
        this.send("/help: show this list", false);
        this.send("/close: close the connection", false);
        this.send("/echo: send the last received message", false);
        this.send("/smile: send a smile", false);
        this.send("/like: send a thumb up", false);
        this.send("/name NAME: change your current name to NAME (??? if invalid input)", false);
        this.send("/status [AVAILABLE | BUSY]: change your current status", false);
        this.send("/file FILEPATH: send the file located in FILEPATH", true);
    }

    private void smile() {
        this.send(" /000000\\ ", false);
        this.send("|  ^  ^  |", false);
        this.send("| \\____/ |", false);
        this.send(" \\______/ ", true);
    }

    private void like() {
        this.send(" ( ((           ", false);
        this.send("  \\ =\\          ", false);
        this.send(" __\\_  `-\\      ", false);
        this.send("(____))(  \\---- ", false);
        this.send("(____)) _       ", false);
        this.send("(____))         ", false);
        this.send("(___))____/---- ", true);
    }

    private void receiveFile(File path) {

        FileOutputStream fout = null;

        try {
            fout = new FileOutputStream(path);

            DataInputStream receiveSize = new DataInputStream(this.is);
            long size = receiveSize.readLong(); //ricevo la dimensione del file

            for (int i = 0; i < size; i++) {    //fino a quando non e' finito il file
                fout.write(decipher((byte) this.is.read()));    //decifro e scrivo sul file
            }

        } catch (FileNotFoundException fnfe) {
            System.err.println("Couldn't open file");
        } catch (IOException ioe) {
            System.err.println("Couldn't read from input stream");
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException ex) {
                    System.err.println("Couldn't close file");
                }
            }
        }

    }

    /*
     * Perche' invio la dimensione del file usando un altro wrapper?
     * - inizialmente avevo previsto di mandare -1 stesso, ma a causa di
     * - perdita di dati dovuta ad una coercizione (int) -> (byte):
     *
     * - 11111111.11111111.11111111.11111111 (rappresentazione in complemento a 2 su 4 byte) -1
     * - --------.--------.--------.11111111 (trasformazione in byte) 255
     * - 00000000.00000000.00000000.11111111 (casting a int) 255
     *
     * - Se avessi usato come condizione di uscita la ricezione di 255,
     * - non avrei potuto inviare 255 stesso, carattere validissimo(?).
     * - Ho quindi deciso inviare la dimensione del file usando il
     * - wrapper apposito.
    */
    private void sendFile(File path) {

        FileInputStream fin = null;

        try {
            fin = new FileInputStream(path);
            int temp;

            DataOutputStream sendSize = new DataOutputStream(this.os);
            sendSize.writeLong(fin.getChannel().size());    //invio dimensione file

            while ((temp = fin.read()) != -1) { //finche' non e' finito...
                this.os.write(cipher((byte) temp)); //invio cifrando
            }

        } catch (FileNotFoundException ex) {
            System.err.println("Couldn't open file");
        } catch (IOException ex) {
            System.err.println("Couldn't write on output stream");
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException ex) {
                    System.err.println("Couldn't close file");
                }
            }
        }

    }

    private void endTransfer() {
        //invio stringa terminatrice
        out.println(cipher(Chat.END));
        out.flush();
    }

    //in questo momento uso una cifratura simile al codice di cesare;
    //in futuro basta cambiare questo metodo e tutto filera' liscio
    private String cipher(String string) {

        char data[] = string.toCharArray(); //converto stringa in array di caratteri

        for (int i = 0; i < data.length; i++) {
            data[i] += Chat.KEY;    //modifico in chiave
        }

        return new String(data);    //restituisco la nuova stringa cifrata

    }

    private byte cipher(byte data) {    //cifro un solo byte in chiave

        return (byte) (data + (byte) Chat.KEY);

    }

    //metodo inutilizzato ma utile in caso di necessita'
    //di cifratura di tipi complessi (anche oggetti)
    private byte[] cipher(byte[] data) {

        for (int i = 0; i < data.length; i++) {
            data[i] += (byte) Chat.KEY;
        }

        return data;
    }

    //corrispondenti ai rispettivi metodi con i medesimi parametri
    private String decipher(String string) {

        char data[] = string.toCharArray();

        for (int i = 0; i < data.length; i++) {
            data[i] -= Chat.KEY;
        }

        return new String(data);

    }

    private byte decipher(byte data) {
        return (byte) (data - (byte) Chat.KEY);
    }

    private byte[] decipher(byte data[]) {

        for (int i = 0; i < data.length; i++) {
            data[i] -= (byte) Chat.KEY;
        }

        return data;
    }
    
}
