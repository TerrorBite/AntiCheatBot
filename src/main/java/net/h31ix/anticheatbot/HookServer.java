package net.h31ix.anticheatbot;

import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HookServer implements Runnable
{
    ServerSocket serverSocket = null;
    Socket clientSocket = null;
    BufferedReader in;

    @Override
    public void run()
    {
        createServer();
    }

    public void createServer()
    {
        try
        {
            serverSocket = new ServerSocket(8848);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.exit(1);
        }

        try
        {
            clientSocket = serverSocket.accept();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.exit(1);
        }

        getInput();

        close();
    }

    private void close()
    {
        try
        {
            in.close();
            clientSocket.close();
            serverSocket.close();
        }
        catch (IOException ex)
        {
            Logger.getLogger(HookServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void reset()
    {
        close();
        createServer();
    }

    private void getInput()
    {
        try
        {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null)
            {
                 AntiCheatBot.updateCommit(inputLine);
                 reset();
                 break;
            }
        }
        catch (IOException ex)
        {
            Logger.getLogger(HookServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


}