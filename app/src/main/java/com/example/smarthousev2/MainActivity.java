package com.example.smarthousev2;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity
{
    Thread Thread1 = null;
    Thread Thread2 = null;
    String SERVER1_IP = "192.168.1.84";
    int SERVER1_PORT = 80;
    String SERVER2_IP = "192.168.1.89";
    int SERVER2_PORT = 80;

    private ProgressBar progressBar;
    private TextView textViewProgress;
    private RadioButton radioButtonHome;
    private RadioButton radioButtonStreet;
    private Switch switchPump;
    private TextView textViewPumpTimeTitle;
    private TextView textViewPumpTimeNum;
    private Button refreshButton;
    private Button gLAviaryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TabHost tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();

        TabHost.TabSpec tabSpec = tabHost.newTabSpec("tag1");
        tabSpec.setContent(R.id.linearLayout);
        tabSpec.setIndicator("Бак");
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("tag2");
        tabSpec.setContent(R.id.linearLayout2);
        tabSpec.setIndicator("Птичник");
        tabHost.addTab(tabSpec);

        tabHost.setCurrentTab(0);

        progressBar = findViewById(R.id.progress_bar);
        textViewProgress = findViewById(R.id.text_view_progress);
        radioButtonHome = findViewById(R.id.radioButtonModeHome);
        radioButtonStreet = findViewById(R.id.radioButtonModeStreet);
        textViewPumpTimeTitle = findViewById(R.id.textViewPumpTimeTitle);
        textViewPumpTimeNum = findViewById(R.id.textViewPumpTimeNum);
        textViewPumpTimeTitle.setText("Насос работал:");
        textViewPumpTimeNum.setText("0 минут.");

        radioButtonHome.setOnClickListener(radioButtonClickListener);
        radioButtonStreet.setOnClickListener(radioButtonClickListener);

        switchPump = findViewById(R.id.switchPump);
        refreshButton = findViewById(R.id.buttonRefresh);
        gLAviaryButton = findViewById(R.id.buttonGLAviary);

        if (switchPump != null)
        {
            switchPump.setOnCheckedChangeListener(this::onCheckedChanged);
        }

        Thread1 = new Thread(new Thread1Server1());
        Thread1.start();

        Thread2 = new Thread(new Thread1Server2());
        Thread2.start();
    }

    private PrintWriter outputServer1;
    private BufferedReader inputServer1;
    class Thread1Server1 implements Runnable
    {
        public void run()
        {
            Socket socket;
            try
            {
                socket = new Socket(SERVER1_IP, SERVER1_PORT);
                outputServer1 = new PrintWriter(socket.getOutputStream());
                inputServer1 = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                new Thread(new Thread2Server1()).start();
                sendDataToServer1("[R]");
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    class Thread2Server1 implements Runnable
    {
        @Override
        public void run()
        {
            while (true)
            {
                try
                {
                    final String message = inputServer1.readLine();
                    if (message != null)
                    {
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                processingInputStream(message);
                            }
                        });
                    }
                    else
                    {
                        Thread1 = new Thread(new Thread1Server1());
                        Thread1.start();
                        return;
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    class Thread3Server1 implements Runnable
    {
        private String message;
        Thread3Server1(String message)
        {
            this.message = message;
        }
        @Override
        public void run()
        {
            outputServer1.write(message);
            outputServer1.flush();
        }
    }

    private PrintWriter outputServer2;
    private BufferedReader inputServer2;
    class Thread1Server2 implements Runnable
    {
        public void run()
        {
            Socket socket;
            try
            {
                socket = new Socket(SERVER2_IP, SERVER2_PORT);
                outputServer2 = new PrintWriter(socket.getOutputStream());
                inputServer2 = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                new Thread(new Thread2Server2()).start();
                //sendDataToServer2("[R]");
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    class Thread2Server2 implements Runnable
    {
        @Override
        public void run()
        {
            while (true)
            {
                try
                {
                    final String message = inputServer2.readLine();
                    if (message != null)
                    {
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                processingInputStream(message);
                            }
                        });
                    }
                    else
                    {
                        Thread2 = new Thread(new Thread1Server2());
                        Thread2.start();
                        return;
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    class Thread3Server2 implements Runnable
    {
        private String message;
        Thread3Server2(String message)
        {
            this.message = message;
        }
        @Override
        public void run()
        {
            outputServer2.write(message);
            outputServer2.flush();
        }
    }

    private void sendDataToServer1(String text)
    {
        text = text + "\n";
        new Thread(new Thread3Server1(text)).start();
    }

    private void sendDataToServer2(String text)
    {
        text = text + "\n";
        new Thread(new Thread3Server2(text)).start();
    }

    private void processingInputStream(String text)
    {
        if (text.charAt(0) == 'X')
        {
            String percentString = text.substring(1);
            try
            {
                int percentInt = Integer.parseInt(percentString);
                progressBar.setProgress(percentInt);
            }
            catch (Exception e)
            {
                Toast toast = Toast.makeText(getApplicationContext(), "Ошибка распознания процентов!", Toast.LENGTH_SHORT);
                toast.show();
            }
            percentString = percentString + "%";
            textViewProgress.setText(percentString);
        }
        else if (text.charAt(0) == 'T')
        {
            String pumpTimeString = text.substring(1);
            pumpTimeString = pumpTimeString + " минут.";
            textViewPumpTimeNum.setText(pumpTimeString);
        }
        else
        {
            switch (text)
            {
                case "HOME":
                    if (!radioButtonHome.isChecked())
                    {
                        radioButtonHome.setChecked(true);
                        radioButtonStreet.setChecked(false);
                        switchPump.setClickable(false);
                    }
                    break;

                case "STREET":
                    if (!radioButtonStreet.isChecked())
                    {
                        radioButtonHome.setChecked(false);
                        radioButtonStreet.setChecked(true);
                        switchPump.setClickable(true);
                    }
                    break;

                case "PON":
                    switchPump.setChecked(true);
                    textViewPumpTimeTitle.setText("Насос работает:");
                    break;

                case "POFF":
                    switchPump.setChecked(false);
                    textViewPumpTimeTitle.setText("Насос работал:");

                default:
                    break;
            }
        }
    }

    View.OnClickListener radioButtonClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            RadioButton rb = (RadioButton)v;
            switch (rb.getId())
            {
                case R.id.radioButtonModeHome:
                    sendDataToServer1("[HOME]");     // home mode
                    switchPump.setClickable(false);
                    break;

                case R.id.radioButtonModeStreet:
                    sendDataToServer1("[STREET]");       // street mode
                    switchPump.setClickable(true);
                    break;

                default:
                    break;
            }
        }
    };

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        if (isChecked)
        {
            sendDataToServer1("[PON]");      // pump on
        }
        if (!isChecked)
        {
            sendDataToServer1("[POFF]");     // pump off
        }
    }

    public void onClickRefresh(View view)
    {
        sendDataToServer1("[R]");
    }

    public void onClickSwitchGeneralLightInAviary(View view)
    {
        sendDataToServer2("{GLAVIARY}");
    }
}
