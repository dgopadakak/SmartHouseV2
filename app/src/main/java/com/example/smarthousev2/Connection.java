package com.example.smarthousev2;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Connection
{
    private  Socket  mSocket = null;
    private  String  mHost   = null;
    private  int     mPort   = 0;

    public static final String LOG_TAG = "SOCKET";

    public Connection() {}

    public Connection (final String host, final int port)
    {
        this.mHost = host;
        this.mPort = port;
    }

    // Метод открытия сокета
    public void openConnection() throws Exception
    {
        // Если сокет уже открыт, то он закрывается
        closeConnection();
        try {
            // Создание сокета
            mSocket = new Socket(mHost, mPort);
        } catch (IOException e) {
            throw new Exception("Невозможно создать сокет: "
                    + e.getMessage());
        }
    }
    /**
     * Метод закрытия сокета
     */
    public void closeConnection()
    {
        if (mSocket != null && !mSocket.isClosed()) {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Ошибка при закрытии сокета :"
                        + e.getMessage());
            } finally {
                mSocket = null;
            }
        }
        mSocket = null;
    }
    /**
     * Метод отправки данных
     */
    public void sendData(byte[] data) throws Exception
    {
        // Проверка открытия сокета
        if (mSocket == null || mSocket.isClosed())
        {
            throw new Exception("Ошибка отправки данных. " + "Сокет не создан или закрыт");
        }
        // Отправка данных
        try
        {
            mSocket.getOutputStream().write(data);
            mSocket.getOutputStream().flush();
        }
        catch (IOException e)
        {
            throw new Exception("Ошибка отправки данных : " + e.getMessage());
        }
    }

    public String getData() throws Exception
    {
        String result = "";
        if (mSocket == null || mSocket.isClosed())      // Проверка открытия сокета
        {
            throw new Exception("Ошибка получения данных. " + "Сокет не создан или закрыт");
        }
        try
        {
            DataInputStream in = new DataInputStream(new BufferedInputStream(mSocket.getInputStream()));
            int length = in.readInt();
            if (length > 0)
            {
                byte[] messageByte = new byte[length];
                boolean end = false;
                StringBuilder dataString = new StringBuilder(length);
                int totalBytesRead = 0;
                while (!end)
                {
                    int currentBytesRead = in.read(messageByte);
                    totalBytesRead = currentBytesRead + totalBytesRead;
                    if (totalBytesRead <= length)
                    {
                        dataString.append(new String(messageByte, 0, currentBytesRead, StandardCharsets.UTF_8));
                    }
                    else
                    {
                        dataString.append(new String(messageByte, 0, length - totalBytesRead + currentBytesRead, StandardCharsets.UTF_8));
                    }
                    if (dataString.length() >= length)
                    {
                        end = true;
                    }
                }
                result = dataString.toString();
            }
            //DataInputStream inputStream = new DataInputStream(new BufferedInputStream(mSocket.getInputStream()));
            //result = inputStream.toString();
        }
        catch (IOException e)
        {
            throw new Exception("Ошибка получения данных : " + e.getMessage());
        }
        return result;
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        closeConnection();
    }
}
