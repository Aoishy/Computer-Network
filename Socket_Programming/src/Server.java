import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;

public class Server {
    private static final int PORT = 5109;
    private static final String LOG_FILE = "logFile.log";

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
    public void start(){
        System.out.println();
        System.out.println("Server started ");
        System.out.println("Waiting for connections on port: "+ PORT);
        System.out.println();
        File logFile = initializeLogFile();

        try (ServerSocket serverSocket = new ServerSocket(PORT))
        {
            while (true)
            {
                acceptClientConnection(serverSocket, logFile);
            }
        }
        catch (IOException e)
        {
            System.out.println("Error while starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private File initializeLogFile()
    {
        try {
            String logDirectory = Paths.get("").toAbsolutePath().resolve("log").toString();
            File logFile = new File(logDirectory,LOG_FILE);
            if (logFile.getParentFile() != null && !logFile.getParentFile().exists())
            {
                logFile.getParentFile().mkdirs();
            }
            try (FileWriter logWriter = new FileWriter(logFile))
            {
                logWriter.write("");
            }
            return logFile;
        }
        catch (IOException e)
        {
            System.out.println("Error initializing logFile: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    private void acceptClientConnection(ServerSocket serverSocket, File logFile)
    {
        try {
            Socket welcomeSocket = serverSocket.accept();
            System.out.println("Connection Established:" + welcomeSocket.toString());
            new http_serverThread(welcomeSocket,logFile);
        }
        catch (IOException e)
        {
            System.out.println("Error accepting client connection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
