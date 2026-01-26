package com.shizuku.uninstaller;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Service;
import android.app.UiModeManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.LayoutAnimationController;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    boolean b, c;
    Button B, C;
    int m;
    ListView d, e;
    EditText e1;
    ImageView iv;
    SharedPreferences sp;
    //shizuku listens for authorization result
    private final Shizuku.OnRequestPermissionResultListener RL = this::onRequestPermissionsResult;


    private void onRequestPermissionsResult(int i, int i1) {
        check();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Automatically switch the app's dark/light theme based on system dark mode
        if (((UiModeManager) getSystemService(Service.UI_MODE_SERVICE)).getNightMode() == UiModeManager.MODE_NIGHT_NO)
            setTheme(android.R.style.Theme_DeviceDefault_Light_Dialog);
        sp = getSharedPreferences("data", 0);
        //If first time opening, display help interface
        if (sp.getBoolean("first", true)) {
            showHelp();
            sp.edit().putBoolean("first", false).apply();
        }
        //Read user setting “hide background”, and hide from recents
        ((ActivityManager) getSystemService(Service.ACTIVITY_SERVICE)).getAppTasks().get(0).setExcludeFromRecents(sp.getBoolean("hide", true));
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        //Limit the window width in landscape so it doesn't fill the full screen - otherwise it looks ugly
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            getWindow().getAttributes().width = (getWindowManager().getDefaultDisplay().getHeight());


        B = findViewById(R.id.b);
        C = findViewById(R.id.c);
        iv = findViewById(R.id.iv);

        //Set long-press on the cat icon to show help interface
        iv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showHelp();
                return false;
            }
        });

        //When Shizuku returns authorization result, run RL()
        Shizuku.addRequestPermissionResultListener(RL);

        //m stores the original text color of the Shizuku status buttons, to support Monet colors and restore later
        m = B.getCurrentTextColor();

        //Check Shizuku status and request Shizuku permission
        check();
        d = findViewById(R.id.list);
        e = findViewById(R.id.lista);

        //Bind the layout and item count for the two listViews
        initlist();
    }

    private void showHelp() {
        //Display help interface
        View v = LayoutInflater.from(MainActivity.this).inflate(R.layout.help, null);
        ((TextView) v.findViewById(R.id.t3)).setText(Html.fromHtml("&nbsp;&nbsp;This app <u><b><big>will NOT</big></b></u> collect any of your information and contains no networking functions at all.<br>&nbsp;&nbsp;Continuing means you agree to the above privacy policy.<br>&nbsp;&nbsp;Using this app requires Shizuku to be installed and activated on your device.<br>&nbsp;&nbsp;Later, you can <u><b><big>long press</big></b></u> the cat icon on the main title bar (as shown below) to reopen this help page."));
        ((TextView) v.findViewById(R.id.t4)).setText(Html.fromHtml("&nbsp;&nbsp;--Tap to edit an item; long press to copy the command saved in that item.<br><br>&nbsp;&nbsp;--<u><b><big>Single-tap</big></b></u> the cat icon on the title to switch the APP to one-time execution mode.<br><br>&nbsp;&nbsp;--Tap either of the two Shizuku-status buttons on the main page to <u><b><big>refresh Shizuku status</big></b></u>. Of course, closing and reopening the APP also refreshes it.<br><br>&nbsp;&nbsp;--If Shizuku is started via root on your device, this APP will also have root permission when executing commands. If you do not want to <u><b><big>run commands with such high privileges</big></b></u>, you may check “Drop root to Shell” when editing a command so the app executes with only shell permission.<br><br>&nbsp;&nbsp;--You can tap the settings button at the bottom of this page to explore more features!"));
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Help")
                .setView(v)
                .setNegativeButton("OK", null)
                .setNeutralButton("Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).create();
                        dialog.getWindow().getAttributes().alpha = 0.85f;
                        dialog.getWindow().setGravity(Gravity.BOTTOM);

                        View v = View.inflate(MainActivity.this, R.layout.set, null);
                        Switch S = v.findViewById(R.id.s);

                        S.setChecked(sp.getBoolean("hide", true));
                        S.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                sp.edit().putBoolean("hide", b).apply();
                                ((ActivityManager) getSystemService(Service.ACTIVITY_SERVICE)).getAppTasks().get(0).setExcludeFromRecents(b);
                            }
                        });
                        Switch S1 = v.findViewById(R.id.s1);

                        S1.setChecked(sp.getBoolean("20", false));
                        S1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                sp.edit().putBoolean("20", b).apply();
                                Toast.makeText(MainActivity.this, "Takes effect after APP restart", Toast.LENGTH_SHORT).show();
                            }
                        });
                        dialog.setView(v);
                        dialog.show();
                    }
                })
                .create().show();

    }

    private void check() {

        //Check Shizuku status. b = whether Shizuku is running, c = whether Shizuku is authorized
        b = true;
        c = false;
        try {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
                Shizuku.requestPermission(0);
            else c = true;
        } catch (Exception e) {
            if (checkSelfPermission("moe.shizuku.manager.permission.API_V23") == PackageManager.PERMISSION_GRANTED)
                c = true;
            if (e.getClass() == IllegalStateException.class) {
                b = false;
                Toast.makeText(this, "Shizuku is not running", Toast.LENGTH_SHORT).show();
            }
        }
        B.setText(b ? "Shizuku\nRunning" : "Shizuku\nNot Running");
        B.setTextColor(b ? m : 0x77ff0000);
        C.setText(c ? "Shizuku\nAuthorized" : "Shizuku\nNot Authorized");
        C.setTextColor(c ? m : 0x77ff0000);
    }

    @Override
    protected void onDestroy() {
        //Remove permission callback on exit — required by Shizuku
        Shizuku.removeRequestPermissionResultListener(RL);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        //Directly exit APP on back press — small app, no need for double-press exit logic
        finish();
    }

    public void ch(View view) {
        //This function is bound to the two Shizuku status buttons
        check();
    }

    public void ex(View view) {
        //Single-tap on cat icon: hide list, show EditText instead

        flipAnimation(view);
        d.setVisibility(View.INVISIBLE);
        e.setVisibility(View.INVISIBLE);
        d.setAdapter(new adapter(this, new int[]{}));
        e.setAdapter(new adapter(this, new int[]{}));
        findViewById(R.id.l1).setVisibility(View.VISIBLE);
        e1 = findViewById(R.id.e);
        e1.setEnabled(true);
        e1.requestFocus();
        e1.postDelayed(new Runnable() {
            @Override
            public void run() {
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(e1, 0);
            }
        }, 200);
        e1.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    exe(v);
                }
                return false;
            }
        });
        e1.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN)
                    exe(view);
                return false;
            }
        });

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flipAnimation(view);
                d.setVisibility(View.VISIBLE);
                e.setVisibility(View.VISIBLE);
                e1.setEnabled(false);
                initlist();
                findViewById(R.id.l1).setVisibility(View.GONE);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ex(view);
                    }
                });
            }
        });
    }


    private void flipAnimation(View view) {
        //A lightweight flip animation — pretty fun
        ObjectAnimator a2 = ObjectAnimator.ofFloat(view, "rotationY", 90f, 0f);
        a2.setDuration(300).setInterpolator(new LinearInterpolator());
        a2.start();

    }


    public void exe(View view) {

        //Execute button to run entered command
        if (e1.getText().length() > 0)
            startActivity(new Intent(this, Exec.class).putExtra("content", e1.getText().toString()));
    }


    public void initlist() {
        //Show 10 slots or more depending on user setting
        int[] e1 = sp.getBoolean("20", false) ? new int[]{5, 6, 7, 8, 9, 15, 16, 17, 18, 19, 25, 26, 27, 28, 29, 35, 36, 37, 38, 39, 45, 46, 47, 48, 49} : new int[]{5, 6, 7, 8, 9};
        int[] d1 = sp.getBoolean("20", false) ? new int[]{0, 1, 2, 3, 4, 10, 11, 12, 13, 14, 20, 21, 22, 23, 24, 30, 31, 32, 33, 34, 40, 41, 42, 43, 44} : new int[]{0, 1, 2, 3, 4};
        e.setAdapter(new adapter(this, e1));
        d.setAdapter(new adapter(this, d1));

        //Add some animation — super silky~
        TranslateAnimation animation = new TranslateAnimation(-50f, 0f, -30f, 0f);
        animation.setDuration(500);
        LayoutAnimationController controller = new LayoutAnimationController(animation, 0.1f);
        controller.setOrder(LayoutAnimationController.ORDER_NORMAL);
        d.setLayoutAnimation(controller);
        animation = new TranslateAnimation(50f, 0f, -30f, 0f);
        animation.setDuration(500);
        controller = new LayoutAnimationController(animation, 0.1f);
        e.setLayoutAnimation(controller);
    }
}