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
    String SERVER_IP = "192.168.1.84";
    int SERVER_PORT = 80;

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

        Thread1 = new Thread(new Thread1());
        Thread1.start();
    }

    private PrintWriter output;
    private BufferedReader input;
    class Thread1 implements Runnable
    {
        public void run()
        {
            Socket socket;
            try
            {
                socket = new Socket(SERVER_IP, SERVER_PORT);
                output = new PrintWriter(socket.getOutputStream());
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                new Thread(new Thread2()).start();
                sendData("[R]");
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    class Thread2 implements Runnable
    {
        @Override
        public void run()
        {
            while (true)
            {
                try
                {
                    final String message = input.readLine();
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
                        Thread1 = new Thread(new Thread1());
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

    class Thread3 implements Runnable
    {
        private String message;
        Thread3(String message)
        {
            this.message = message;
        }
        @Override
        public void run()
        {
            output.write(message);
            output.flush();
        }
    }

    private void sendData(String text)
    {
        text = text + "\n";
        new Thread(new Thread3(text)).start();
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
                    sendData("[HOME]");     // home mode
                    switchPump.setClickable(false);
                    break;

                case R.id.radioButtonModeStreet:
                    sendData("[STREET]");       // street mode
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
            sendData("[PON]");      // pump on
        }
        if (!isChecked)
        {
            sendData("[POFF]");     // pump off
        }
    }

    public void onClickRefresh(View view)
    {
        sendData("[R]");
    }

    public void onClickSwitchGeneralLightInAviary(View view)
    {
        sendData("[GLAVIARY]");
    }
}
