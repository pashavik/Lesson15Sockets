import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private  final ConcurrentMap<String, ClientsInfo> mapClients = new ConcurrentHashMap<>();
    private static volatile int stop = 1;

    public static void main(String[] args) throws IOException {
        Server server= new Server();
        server.work();
    }

    public void work() throws IOException {
        ExecutorService executorService = Executors.newFixedThreadPool(5);  //пул
        try (ServerSocket serverSocket = new ServerSocket(1234)) {
            logger.log(Level.INFO, "server started");
            while (stop != 0) {
                String login;
                Socket socket = serverSocket.accept();
                logger.log(Level.INFO, "Коннект с клиентом, client ip {0} port {1}", new Object[]{socket.getInetAddress(), socket.getPort()});
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 login = in.readUTF();


                ClientsInfo clientsInfo=new ClientsInfo(socket, in, out, login);  //инф о новом клиенте
                mapClients.put(login, clientsInfo);
                executorService.execute(clientsInfo);  //стартуем в отдельном потоке
            }
            logger.log(Level.INFO, "сервер остановлен");
        }
        executorService.shutdown();
    }

    private class ClientsInfo implements Runnable {
        private final Socket socket;
        private final DataInputStream in;
        private final DataOutputStream out;
        private final String login;

        public ClientsInfo(Socket socket, DataInputStream in, DataOutputStream out, String login) {
            this.socket = socket;
            this.in = in;
            this.out = out;
            this.login = login;
        }

        public void serverStop() {
            Server.stop = 0;
        }

        @Override
        public void run() {
            try {
                for (String s : mapClients.keySet()) {
                    mapClients.get(s).sendMessage(serverMessage("Новый клиент " + login));
                }
                String msg;
                do {
                    msg = in.readUTF();
                    if (msg.length() > 0 && msg.charAt(0) == '$') {
                        String expLogin = extpectLogin(msg);
                        if (mapClients.containsKey(expLogin)) {
                            ClientsInfo clientsInfo = mapClients.get(expLogin);
                            String msgSend = msg.substring(expLogin.length()+1, msg.length());
                            clientsInfo.sendMessage(clientsMessage(login, msgSend));
                        }
                    }
                } while (!msg.equals("exit"));
                if (msg.equals("exit")) {
                    for (String key : mapClients.keySet()) {
                        mapClients.get(key).sendMessage(serverMessage("Клиент " + login + "отключился"));
                    }
                }
                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendMessage(String message) throws IOException {
            out.writeUTF(message);
            out.flush();
        }

        private String extpectLogin(String message) { //логин из сообщения

            String[] strMess = message.split(" ");
            StringBuilder loginExternal = new StringBuilder(strMess[0]);
            loginExternal.deleteCharAt(0);
            return loginExternal.toString();
        }

        private String clientsMessage(String login, String message) {
            return "<<" + login + ">> " + message;
        }

        private String serverMessage(String message) {
            return "[SYSTEM] " + message;
        }

    }
}