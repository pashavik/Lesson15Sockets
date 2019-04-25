import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {
    private static final Logger logger = Logger.getLogger(Client.class.getName());
    private final DataOutputStream out;
    private final DataInputStream in;
    private String login;
    public Client(DataOutputStream out, DataInputStream in) {
        this.out = out;
        this.in = in;
    }

    public static void main(String[] args) {
        try(Socket socket=new Socket("localhost", 1234)) {
            InputStream in=socket.getInputStream();
            OutputStream out=socket.getOutputStream();
            Client client=new Client( new DataOutputStream(out),new DataInputStream(in));
            logger.log(Level.INFO, "Коннект с сервером, ip: {0}, port: {1}", new Object[]{socket.getInetAddress(), socket.getPort()});
            client.startMessageFromServer();

            Scanner sc=new Scanner(System.in);  //считаем логин
            client.login=client.inputLogin(sc);

            client.out.writeUTF(client.login);
            client.out.flush();

            Thread.sleep(1000);
            client.outputMessage(sc);
        } catch (IOException | InterruptedException e) {
            System.out.println("Конец сеанса");
        }
    }
    private void startMessageFromServer() {
        Thread threadMesg = new Thread(new MessageFromServ());
        threadMesg.start();
    }
    private String inputLogin(Scanner scanner) {
        logger.log(Level.INFO,"Введите логин: ");
        String login = scanner.next();
//        while (login.length() == 0 ||  login.equals(" ")){
//            logger.log(Level.INFO,"Логин не может быть пустым ");
//            login = scanner.next();
//        }
        if(login.length()==0 || login.equals(" ")|| login.equals("\n")){
            logger.log(Level.INFO,"Логин не может быть пустым ");
            login = scanner.next();
        }
        return login;
    }

    private void outputMessage(Scanner scanner) throws IOException {
        String message = "";
        do {
            logger.log(Level.INFO,"[ {0} ] > ",login);
            message = scanner.nextLine();
            out.writeUTF(message);
            out.flush();
        } while (!message.equals("exit"));
    }
    private class MessageFromServ implements Runnable{
        @Override
        public void run() {
            try{
                while (!Thread.currentThread().isInterrupted()){
                    logger.log(Level.INFO, "Пришло: {0}", in.readUTF());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
