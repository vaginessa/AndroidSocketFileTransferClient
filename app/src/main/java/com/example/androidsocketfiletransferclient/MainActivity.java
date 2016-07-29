package com.example.androidsocketfiletransferclient;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;


/**
 * Created by arvind on 19/7/16.
 */
public class MainActivity extends ActionBarActivity {

    private Button mButtonSend,mButtonReceived;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButtonReceived=(Button)findViewById(R.id.btn_file_received);
        mButtonReceived.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FileReceivedActivity.class);
                startActivity(intent);

            }
        });


        mButtonSend=(Button)findViewById(R.id.btn_file_send);
        mButtonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainActivity.this,FileSendActivity.class);
                startActivity(intent);

            }
        });

    }
}
