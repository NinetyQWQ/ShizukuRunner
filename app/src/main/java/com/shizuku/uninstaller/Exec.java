package com.shizuku.uninstaller;

import android.app.Activity;
import android.app.Service;
import android.app.UiModeManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import rikka.shizuku.Shizuku;

public class Exec extends Activity {

    TextView t1, t2;
    Process p;
    Thread h1, h2, h3;
    boolean br = false;


    //mHandler is used for weak reference and UI updates on the main thread. Why must it be done this way? Simply put, otherwise it will crash or cause memory leaks.
    protected MyHandler mHandler = new MyHandler(this);

    public static class MyHandler extends Handler {
        private final WeakReference<Exec> mOuter;

        public MyHandler(Exec activity) {
            mOuter = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {

            //msg.what = 1 means error message, 2 means normal message
            mOuter.get().t2.append(msg.what == 1 ? (SpannableString)msg.obj : String.valueOf(msg.obj));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Executing");

        //Dynamically switch dark theme based on system dark mode
        if (((UiModeManager) getSystemService(Service.UI_MODE_SERVICE)).getNightMode() == UiModeManager.MODE_NIGHT_NO)
            setTheme(android.R.style.Theme_DeviceDefault_Light_Dialog);

        //Semi-transparent background
        getWindow().getAttributes().alpha = 0.85f;
        setContentView(R.layout.exec);
        t1 = findViewById(R.id.t1);
        t2 = findViewById(R.id.t2);
t2.requestFocus();
        t2.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (keyEvent.getKeyCode()==KeyEvent.KEYCODE_ENTER&&keyEvent.getAction()==KeyEvent.ACTION_DOWN)
                    finish();
                return false;
            }
        });
        //Execute command in a child thread, otherwise running in UI thread will cause UI freeze
        h1 = new Thread(new Runnable() {
            @Override
            public void run() {
                ShizukuExec(getIntent().getStringExtra("content"));
            }
        });
        h1.start();
    }

    public void ShizukuExec(String cmd) {
        try {

            //Record start time of execution
            long time = System.currentTimeMillis();

            //Execute command using Shizuku
            p = Shizuku.newProcess(new String[]{"sh"}, null, null);
            OutputStream out = p.getOutputStream();
            out.write((cmd + "\nexit\n").getBytes());
            out.flush();
            out.close();

            //Start a new thread to read command output in real time
            h2 = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        BufferedReader mReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        String inline;
                        while ((inline = mReader.readLine()) != null) {

                            //If TextView content becomes too long (which causes lag), or user exits output screen (br = true), then stop reading
                            if (t2.length() > 2000 || br) break;
                            Message msg = new Message();
                            msg.what = 0;
                            msg.obj = inline.equals("") ? "\n" : inline + "\n";
                            mHandler.sendMessage(msg);
                        }
                        mReader.close();
                    } catch (Exception ignored) {
                    }
                }
            });
            h2.start();

            //Start a new thread to read command error output in real time
            h3 = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        BufferedReader mReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                        String inline;
                        while ((inline = mReader.readLine()) != null) {

                            //If TextView content becomes too long (which causes lag), or user exits output screen (br = true), then stop reading
                            if (t2.length() > 2000 || br) break;
                            Message msg = new Message();
                            msg.what = 1;
                            if (inline.equals(""))
                                msg.obj = null;
                            else {
                                SpannableString ss = new SpannableString(inline+"\n");
                                ss.setSpan(new ForegroundColorSpan(Color.RED), 0, ss.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                msg.obj = ss;
                            }
                            mHandler.sendMessage(msg);
                        }
                        mReader.close();
                    } catch (Exception ignored) {
                    }
                }
            });
            h3.start();

            //Wait for command to finish
            p.waitFor();

            //Get return value
            String exitValue = String.valueOf(p.exitValue());

            //Display return value and execution duration
            t1.post(new Runnable() {
                @Override
                public void run() {
                    t1.setText(String.format("Return value: %s\nExecution time: %.2f seconds", exitValue, (System.currentTimeMillis() - time) / 1000f));
                    setTitle("Execution Finished");
                }
            });
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onDestroy() {

        //Close all I/O streams, destroy process, prevent memory leaks
        br = true;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (Build.VERSION.SDK_INT >= 26) {
                        p.destroyForcibly();
                    } else {
                        p.destroy();
                    }
                    h1.interrupt();
                    h2.interrupt();
                    h3.interrupt();
                } catch (Exception ignored) {
                }
            }
        }, 1000);
        super.onDestroy();
    }


}