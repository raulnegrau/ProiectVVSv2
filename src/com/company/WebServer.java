package com.company;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.StringTokenizer;


public class WebServer implements Runnable {
    static final File WEB_ROOT = new File("src/TestSite");
    private static final String METHOD_NOT_SUPPORTED = "not_supported.html";
    private static final String DEFAULT_FILE = "a.html";
    private static final String FILE_MAINTENANCE = "maintenance.html";
    private static final String FILE_NOT_FOUND = "notFound404.html";

    static int MIN_PORT_NUMBER = 1024;
    static int MAX_PORT_NUMBER = 65535;

    static ServerSocket serverSocket;
    static int portNumber = 1500;
    static int serverState = 0;

    private Socket clientSocket;

    public static void main(String[] args) throws IOException {
        if (setPortNumber(portNumber)) {
            setServerState(1);
        }

        while (true) {
            WebServer connection;
            try {
                connection = new WebServer();
                connection.setClientSocket(serverSocket.accept());
                Thread thread = new Thread(connection);
                thread.start();
            } catch (IOException e) {
                System.out.println("Eroare la acceptarea client socket.");
            }
        }
    }


    public Socket getClientSocket () {
        return this.clientSocket;
    }

    public void setClientSocket (Socket socket){
        this.clientSocket = socket;
    }

    public static boolean setPortNumber(int portNumber) throws IOException {
        if (serverSocket != null) {
            serverSocket.close();
        }

        if (portNumber < MIN_PORT_NUMBER) {
            return false;
        }

        if (portNumber > MAX_PORT_NUMBER) {
            return false;
        }

        serverSocket = new ServerSocket(portNumber);

        WebServer.portNumber = portNumber;

        return true;
    }

    public int getServerState () {
        return serverState;
    }

    public static void setServerState(int state) {
        serverState = state;
    }

    public String getFilePath(File path) {
        String name = path.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // empty extension
        }
        String extString = name.substring(lastIndexOf);

        switch(extString) {
            case ".txt":
            case ".html":
                return "text/html";
            case ".css":
                return "text/css";
            case ".jpg":
                return "image/jpeg";
            default:
                return "text/html";
        }
    }


    @Override
    public void run() {
        BufferedReader in = null;
        PrintWriter out = null;
        BufferedOutputStream dataOut;
        dataOut = null;
        String fileRequested = null;

        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream());
            dataOut = new BufferedOutputStream(clientSocket.getOutputStream());

            // get first line of request
            String input = in.readLine();
            StringTokenizer parse= new StringTokenizer(input);
            String method= parse.nextToken().toUpperCase();

            //get file request
            fileRequested = URLDecoder.decode(parse.nextToken().toLowerCase(), "UTF-8");

            System.out.println("+ <- REQUEST :  " + input);

            if(!method.equals("GET") && !method.equals("HEAD")) {
                System.out.println("501 Not implemented: " + method);
                writeFileData(METHOD_NOT_SUPPORTED, out, dataOut,"HTTP/1.1 501 Not Implemented");

            } else {
                if(method.equals("GET")) {
                    if(serverState == 1) {
                        if(fileRequested.endsWith("/")) {
                            fileRequested+=DEFAULT_FILE;
                        }
                        // return content
                        writeFileData(fileRequested, out, dataOut,"HTTP/1.1 200 OK");
                    } else if (serverState == 2) {
                        fileRequested = FILE_MAINTENANCE;
                        writeFileData(fileRequested, out, dataOut,"HTTP/1.1 200 OK");
                    } else if (serverState == 3) {
                        // close  server
                        in.close();
                        out.close();
                        dataOut.close();
                        clientSocket.close();
                    }
                }
            }
        } catch (FileNotFoundException fileNotFound) {
            try {
                fileNotFound(out, dataOut, fileRequested);
                System.err.println(fileNotFound);
            } catch (IOException ioException) {
                System.err.println("+ File : 404.html not found  " + ioException.getMessage());
            }
        } catch(IOException ioException) {
            System.err.println("+ Server error : " + ioException);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }

                if (out != null) {
                    out.close();
                }

                if (dataOut != null) {
                    dataOut.close();
                }

                clientSocket.close();
            } catch(Exception exception) {
                System.err.println("+ Error closing stream:" + exception.getMessage());
            }
        }
    } // end for run method

    public byte[] readFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];
        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if(fileIn != null) {
                fileIn.close();
            }
        }
        return fileData;
    }

    void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
        writeFileData(FILE_NOT_FOUND, out, dataOut,"HTTP/1.1 404 Not found");
        System.out.println("- NOT FOUND: " + fileRequested + " not found! ");
    }

    public void writeFileData(String nameOfFileRequested, PrintWriter sentOut, OutputStream bodyOut, String headerText) throws FileNotFoundException {
        File file= new File(WEB_ROOT,nameOfFileRequested);
        int fileLength = (int)file.length();
        String content = getFilePath(file);

        String inputString = "No file";
        Charset charset = StandardCharsets.US_ASCII;
        byte[] fileData =  inputString.getBytes(charset);
        try {
            fileData = readFileData(file, fileLength);
        } catch (IOException e) {
            // Fixing for relative links
            File wantedFile = search(WEB_ROOT, nameOfFileRequested.split("/")[1]);
            if(wantedFile != null) {
                System.out.println(wantedFile.getAbsolutePath());
                file = wantedFile;
                fileLength = (int)file.length();
                content = getFilePath(file);
                try {
                    fileData = readFileData(file,fileLength);
                } catch (IOException e1) {
                    System.out.println(e1);
                }
            } else {
                System.out.println("- Cannot read this file : " + file);
                throw new FileNotFoundException();
            }
        }

        //send Header
        sentOut.println(headerText);
        sentOut.println("Server : Java HTTP Server ");
        sentOut.println("Date: " + new Date());
        sentOut.println("Content-type: " + content);
        sentOut.println("Content-length: " + fileLength);
        sentOut.println(); //it is very important this println here
        sentOut.flush();

        //send data
        try {
            bodyOut.write(fileData,0,fileLength);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            bodyOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static File search(File file, String filename) {
        if (file.isDirectory()) {
            if (file.canRead()) {
                for (File temp : file.listFiles()) {
                    if (temp.isDirectory()) {
                        File wantedFile = search(temp, filename);
                        if(wantedFile != null) {
                            return wantedFile;
                        }
                    } else {
                        if (filename.equals(temp.getName().toLowerCase())) {
                            return new File(temp.getAbsoluteFile().toString());
                        }
                    }
                }
            }
        }
        return null;
    }
}
