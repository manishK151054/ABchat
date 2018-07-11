package com.aapkabazzaar.abchat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

public class HelpActivity extends Activity {

    private Toolbar toolbar;
    private Button homeBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        toolbar = findViewById(R.id.help_toolbar);
        toolbar.setTitle("Help Page");
        homeBtn = findViewById(R.id.button_home);

        Bundle extras = getIntent().getExtras();
        final Class nextActivityClass = (Class<Activity>) extras.getSerializable("EXTRA_ACTIVITY_CLASS");

        homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent homeIntent = new Intent(HelpActivity.this,nextActivityClass);
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(homeIntent);
                finish();
            }
        });
    }
}
