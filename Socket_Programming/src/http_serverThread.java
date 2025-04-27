import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Date;

public class http_serverThread implements Runnable
{
    Socket welcomesocket;
    Thread thread;
    File logfile;
    String folderItem="<li><a href=\"{href}\"><b><i>{name}</i></b><a></li>";
    String fileItem="<li><a href=\"{href}\" target=\"_blank\">{name}</i></li>";
    public  http_serverThread(Socket welcomesocket, File logfile){
        this.welcomesocket = welcomesocket;
        this.logfile = logfile;
        this.thread = new Thread(this);
        thread.start();
    }

    public void run() {
        try {
            BufferedReader http_request = new BufferedReader(new InputStreamReader(welcomesocket.getInputStream()));
            OutputStream client_service = welcomesocket.getOutputStream();
            String input = http_request.readLine();
            System.out.println("Received request : " + input);
            if (input == null|| input.isEmpty()) {
            }
            else if (input.length() > 0) {
                if (input.startsWith("GET")) {
                    handleBrowserRequest(input, client_service);
                }
                else if (input.startsWith("UPLOAD"))
                {
                    handleUploadRequest(http_request, client_service, input);
                }
                else
                {
                    System.out.println("Invalid Input");
                }
            }
            try {
                welcomesocket.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    private void handleBrowserRequest(String input, OutputStream ops) throws IOException
    {
        String exract_req = input.split(" ")[1].split("[?]")[0].replaceAll("%20", " ");
        if (exract_req.equals("/"))
        {
            exract_req += "root";
        }
        String server_dir = Paths.get(Paths.get("").toAbsolutePath().toString(),exract_req).toString();
        File reqFile = new File(server_dir);
        FileWriter fileWriter = new FileWriter(logfile, true);
        PrintWriter logWriter = new PrintWriter(fileWriter);
        logWriter.println("HTTP request : " + input);
        logWriter.println("HTTP response: ");
        if (!exract_req.equals("/favicon.ico"))
        {
            if (reqFile.exists())
            {
                if (reqFile.isDirectory())
                {
                    String content = extractDetails("homepage.html");
                    String items = "";
                    for (File file : reqFile.listFiles())
                    {
                        String childRoute = (exract_req + "/" + file.getName()).replaceAll("//", "/");
                        if (file.isDirectory())
                        {
                            items += folderItem.replace("{name}", file.getName()).replace("{href}", childRoute) + "\n";
                        }
                        else
                        {
                            items += fileItem.replace("{name}", file.getName()).replace("{href}", childRoute) + "\n";
                        }
                    }
                    content = content.replace("{items}",items);
                    String result = ServerReeply(content, 200, "text/html", logWriter);
                    ops.write(result.getBytes());
                    ops.flush();
                }
                else
                {
                    if (reqFile.getName().endsWith(".txt"))
                    {
                        String textContent = extractDetails("Text.html");
                        textContent = textContent.replace("{src}", extractDetails(server_dir));
                        String result = ServerReeply(textContent, 200, "text/html", logWriter);
                        ops.write(result.getBytes());
                        ops.flush();
                    }
                    else if (reqFile.getName().endsWith(".png") || reqFile.getName().endsWith(".jpg"))
                    {
                        String imageContent = extractDetails("Image.html");
                        byte[] imagebytes = analysisFileData(reqFile, (int) reqFile.length());
                        String base64 = Base64.getEncoder().encodeToString(imagebytes);
                        String Type=reqFile.getName().endsWith(".png") ? "image/png" : "image/jpg";
                        imageContent = imageContent.replace("{src}", "data:" +Type +";base64," + base64);
                        String result = ServerReeply(imageContent, 200,Type, logWriter);
                        ops.write(result.getBytes());
                        ops.flush();
                    }
                    else
                    {
                        sendFile(ops, reqFile, logWriter);
                        ops.flush();
                        ops.close();
                    }
                }
            } else
            {
                String notFound = "Error 404! Page Not Found";
                String result = ServerReeply(notFound, 404, "text/html", logWriter);
                ops.write(result.getBytes());
                ops.flush();
            }
        }
        try {
            logWriter.close();
            welcomesocket.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    private void handleUploadRequest(BufferedReader in, OutputStream ops, String input) throws IOException
    {
        String validity = in.readLine();
        System.out.println(validity);
        if (validity.split(" ")[0].equalsIgnoreCase("invalid")) {
            try {
                in.close();
                welcomesocket.close();
                return;
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        String rootDirectory = Paths.get(Paths.get("").toAbsolutePath().toString(), "root").toString();
        File theDir = new File(rootDirectory + "/uploaded");
        if (!theDir.exists()) {
            theDir.mkdirs();
        }

        byte[] inpFileBytes = new byte[4096];
        try {
            FileOutputStream fos = new FileOutputStream(new File(rootDirectory + "/uploaded/" + input.substring(7)));
            InputStream is = welcomesocket.getInputStream();
            int c;
            while ((c = is.read(inpFileBytes)) > 0) {
                fos.write(inpFileBytes);
            }
            System.out.println("Upload Complete");
            fos.close();
            is.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        try {
            welcomesocket.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    private void sendFile(OutputStream ops, File reqFile, PrintWriter lr) throws IOException {
        ops.write("HTTP/1.0 200 OK\r\n".getBytes());
        ops.write("Accept-Ranges: bytes\r\n".getBytes());
        ops.write(("Content-Length: "+reqFile.length()+"\r\n").getBytes());
        ops.write("Content-Type: application/octet-stream\r\n".getBytes());
        ops.write("\r\n".getBytes());
        byte[] filebytes = analysisFileData(reqFile, (int)reqFile.length());
        for(byte b:filebytes){
            ops.write(b);
            ops.flush();
        }
        lr.println("HTTP/1.0 200 OK\r\n"+
                "Server: Java HTTP Server: 1.0\r\n"+
                "Date: " + new Date() + "\r\n"+
                "Content-Type: application/octet-stream\r\n"+
                "Content-Length: " + reqFile.length() + "\r\n\r\n");
    }

    private String ServerReeply(String content, int status, String type, PrintWriter lr) {
        String result = "HTTP/1.0 "+status+" OK\r\n"+
                "Server: Java HTTP Server: 1.0\r\n"+
                "Date: " + new Date() + "\r\n"+
                "Content-Type: "+type+"\r\n"+
                "Content-Length: " + content.length() + "\r\n"+
                "\r\n"+ content;
        lr.println("HTTP/1.0 "+status+" OK\r\n"+
                "Server: Java HTTP Server: 1.0\r\n"+
                "Date: " + new Date() + "\r\n"+
                "Content-Type: "+type+"\r\n"+
                "Content-Length: " + content.length() + "\r\n\r\n");
        return result;
    }

    private String extractDetails(String fileName) throws IOException{
        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while(( line = br.readLine()) != null ) {
            sb.append( line );
            sb.append( '\n' );
        }
        br.close();
        return sb.toString();
    }

    public static byte[] analysisFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];
        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if (fileIn != null)
                fileIn.close();
        }
        return fileData;
    }

}
