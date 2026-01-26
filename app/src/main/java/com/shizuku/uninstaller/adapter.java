package com.shizuku.uninstaller;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class adapter extends BaseAdapter {
    private final int[] data;
    private final Context mContext;

    public adapter(Context mContext, int[] data) {

        // Set the adapter to receive two parameters: context and int array
        super();
        this.mContext = mContext;
        this.data = data;
    }


    // Fixed writing pattern
    public int getCount() {
        return data.length;
    }

    // Fixed writing pattern
    @Override
    public Object getItem(int position) {
        return null;
    }

    // Fixed writing pattern
    @Override
    public long getItemId(int position) {
        return position;
    }


    // This function defines the display of each item
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView =  LayoutInflater.from(mContext).inflate(R.layout.r, null);
            holder = new ViewHolder();
            holder.texta = convertView.findViewById(R.id.a);
            holder.textb = convertView.findViewById(R.id.b);
            holder.imageButton = convertView.findViewById(R.id.c);
            holder.layout = convertView.findViewById(R.id.l);
            convertView.setTag(holder);
            convertView.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View view, int i, KeyEvent keyEvent) {
                    Toast.makeText(mContext, "sdfsdf", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        } else {

            // For items already loaded, directly reuse them. No need to load again; this is the purpose of ViewHolder
            holder = (ViewHolder) convertView.getTag();
        }

        // Get the user’s settings for this grid item
        SharedPreferences b = mContext.getSharedPreferences(String.valueOf(data[position]), 0);
        init(holder, b);
        return convertView;
    }

    static class ViewHolder {
        TextView texta;
        TextView textb;
        ImageButton imageButton;
        LinearLayout layout;
    }

    void init(ViewHolder holder, SharedPreferences b) {


        // Whether the user has set command content
        boolean existc = b.getString("content", null) == null || b.getString("content", null).length() == 0;

        // Whether the user has set command name
        boolean existn = b.getString("name", null) == null || b.getString("name", null).length() == 0;

        // This click event is for editing the command
        View.OnClickListener voc = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View v = View.inflate(mContext, R.layout.dialog, null);
                final CheckBox cb = v.findViewById(R.id.cb);
                cb.setChecked(b.getBoolean("shell", false));
                final EditText editText = v.findViewById(R.id.e);
                editText.setText(b.getString("content", null));

                editText.setOnKeyListener(new View.OnKeyListener() {
                    @Override
                    public boolean onKey(View view, int i, KeyEvent keyEvent) {

                        if (keyEvent.getKeyCode()==KeyEvent.KEYCODE_ENTER&&keyEvent.getAction()==KeyEvent.ACTION_DOWN){
                            b.edit().putString("content", editText.getText().toString()).putBoolean("shell", cb.isChecked()).apply();
                            init(holder, b);
                        }

                        return false;
                    }
                });
                final EditText editText1 = v.findViewById(R.id.a);
                editText1.setText(b.getString("name", null));

                editText1.setOnKeyListener(new View.OnKeyListener() {
                    @Override
                    public boolean onKey(View view, int i, KeyEvent keyEvent) {

                            if (keyEvent.getKeyCode()==KeyEvent.KEYCODE_ENTER&&keyEvent.getAction()==KeyEvent.ACTION_DOWN){
                                b.edit().putString("name", editText1.getText().toString()).putBoolean("shell", cb.isChecked()).apply();
                                init(holder, b);
                            }

                        return false;
                    }
                });
                editText.requestFocus();
                editText.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ((InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(editText, 0);
                    }
                }, 200);
                new AlertDialog.Builder(mContext).setTitle("Edit Command").setView(v).setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        b.edit().putString("content", editText.getText().toString()).putString("name", editText1.getText().toString()).putBoolean("shell", cb.isChecked()).apply();
                        init(holder, b);
                    }
                }).show();
            }
        };

        // If user has not set command content, show plus icon. Otherwise, show run icon
        holder.imageButton.setImageResource(existc ? R.drawable.plus : R.drawable.run);

        // If user has not set command content, clicking will edit the command. Otherwise, clicking will run the command
        holder.imageButton.setOnClickListener(!existc ? new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Executes different commands depending on whether downgrade is checked
                mContext.startActivity(new Intent(mContext, Exec.class).putExtra("content", b.getBoolean("shell", false) ? "whoami|grep root &> /dev/null && echo 'Tip: Root has been downgraded to shell' 1>&2;" + mContext.getApplicationInfo().nativeLibraryDir + "/libchid.so 2000 " + b.getString("content", " ") + " || " + b.getString("content", " ") : b.getString("content", " ")));
            }
        } : voc);
        holder.texta.setText(existn ? "Empty" : b.getString("name", "Empty"));
        holder.texta.setTextColor(existc ? mContext.getResources().getColor(R.color.b) : mContext.getResources().getColor(R.color.a));
        holder.textb.setText(existc ? "Empty" : b.getString("content", "Empty"));
        holder.layout.setOnClickListener(voc);
        holder.layout.setOnLongClickListener(existc ? null : new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ((ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", b.getString("content", "ls -l")));
                Toast.makeText(mContext, "Command copied to clipboard:\n" + b.getString("content", "ls -l"), Toast.LENGTH_SHORT).show();
                return true;
            }
        });

    }

}