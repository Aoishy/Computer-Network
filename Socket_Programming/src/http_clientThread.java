import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

class http_clientThread implements Runnable
{
    private Socket clientsocket;
    private final String fileName;
    private final Thread thread;
    public http_clientThread(String fileName) {
        this.fileName = fileName;
        this.thread = new Thread(this);
        this.thread.start();
    }
    public void run() {
        try {
            connectToServer();
            validateAndUploadFile();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally {
            closeSocket();
        }
    }
    private void connectToServer() throws IOException {
        try {
            clientsocket = new Socket("127.0.0.1", 5109);
        }
        catch (IOException e)
        {
            System.out.println("Error connecting to server.");
            throw e;
        }
    }
    private void validateAndUploadFile() throws IOException {
        String fileDirectory=getAbsolutePath();
        File uploadFile=new File(fileDirectory);
        try (PrintWriter writer=new PrintWriter(clientsocket.getOutputStream());
             OutputStream out = clientsocket.getOutputStream()) {
            writer.println("UPLOAD " + fileName);
            writer.flush();
            if (!validateFile(uploadFile, writer)) {
                return;
            }
            uploadFile(uploadFile, out);
        }
        catch (IOException e)
        {
            System.out.println("Error in file uploading.");
            throw e;
        }
    }

    private String getAbsolutePath()
    {
        Path currentDirectory = Paths.get("").toAbsolutePath();
        return Paths.get(currentDirectory.toString(), fileName).toString();
    }

    private boolean validateFile(File uploadFile, PrintWriter writer) throws IOException
    {
        boolean isValidExtension = fileName.endsWith(".txt") || fileName.endsWith(".jpg") || fileName.endsWith(".png");
        if (!uploadFile.exists()) {
            writer.println("Invalid request!File does not exist");
            writer.flush();
            System.out.println("Invalid request!File does not exist");
            return false;
        }
        if (!isValidExtension) {
            writer.println("Invalid File type not allowed");
            writer.flush();
            System.out.println("Invalid File type not allowed");
            return false;
        }
        writer.println("File is Uploading");
        writer.flush();
        System.out.println("File is uploading");
        return true;
    }
    private void uploadFile(File uploadFile,OutputStream out) throws IOException {
        byte[] fileBytes = new byte[4096];
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(uploadFile))) {
            int bytesRead;
            while ((bytesRead = bis.read(fileBytes)) > 0) {
                out.write(fileBytes, 0, bytesRead);
                out.flush();
            }
        }
    }
    private void closeSocket() {
        try {
            if (clientsocket != null && !clientsocket.isClosed()) {
                clientsocket.close();
            }
        }
        catch (IOException e)
        {
            System.err.println("Error closing socket.");
            e.printStackTrace();
        }
    }
}
