package com.aapkabazzaar.abchat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import de.hdodenhof.circleimageview.CircleImageView;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout mLoginEmail;
    private TextInputLayout mLoginPassword;
    private Toolbar mToolbar;
    private TextView textView , mReg_btn, mForgotPassword_btn;
    private CircleImageView logoView;
    private Button mLogin_btn;
    private ProgressDialog mLoginProgress;
    private Animation bottomToTop, topToBottom;
    private FirebaseAuth mAuth;
    private DatabaseReference mUserDatabase;
    private ConstraintLayout constraintLayout;
    private Snackbar snackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        mToolbar = findViewById(R.id.login_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Login Page");

        constraintLayout = findViewById(R.id.constraintLoginLayout);


        if(!isConnectedToInternet(this)){
            showSnackBar("Please check your internet connection",constraintLayout);
        }

        mLoginEmail = findViewById(R.id.login_email);
        mLoginPassword = findViewById(R.id.login_password);
        mLogin_btn = findViewById(R.id.login_btn);
        mForgotPassword_btn = findViewById(R.id.tv_forgot_pass_btn);
        mReg_btn = findViewById(R.id.tv_need_acc_btn);
        textView = findViewById(R.id.textView2);
        logoView = findViewById(R.id.imageView3);
        bottomToTop = AnimationUtils.loadAnimation(this,R.anim.bottom_to_top);
        topToBottom = AnimationUtils.loadAnimation(this,R.anim.top_to_bottom);

        mLogin_btn.setAnimation(bottomToTop);
        mReg_btn.setAnimation(bottomToTop);
        mForgotPassword_btn.setAnimation(bottomToTop);
        textView.setAnimation(topToBottom);
        logoView.setAnimation(topToBottom);
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mUserDatabase.keepSynced(true);

        mLoginProgress = new ProgressDialog(this);

        mForgotPassword_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent forgotPasswordIntent = new Intent(LoginActivity.this,ForgotPasswordActivity.class);
                startActivity(forgotPasswordIntent);
                finish();
            }
        });

        mReg_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent registerIntent = new Intent(LoginActivity.this,RegisterActivity.class);
                startActivity(registerIntent);
                finish();
            }
        });

        mLogin_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mLoginEmail.getEditText().getText().toString();
                String password = mLoginPassword.getEditText().getText().toString();

                if ( !TextUtils.isEmpty(email) || !TextUtils.isEmpty(password)) {

                    mLoginProgress.setTitle("Logging In");
                    mLoginProgress.setMessage("Please wait while we check your credentials");
                    mLoginProgress.setCanceledOnTouchOutside(false);
                    mLoginProgress.show();

                    loginUser(email,password);

                }
            }
        });


    }

    private boolean isConnectedToInternet(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public void showSnackBar(String message, ConstraintLayout constraintLayout)
    {
        snackbar = Snackbar
                .make(constraintLayout, message, Snackbar.LENGTH_INDEFINITE).
                        setAction("Ok", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                snackbar.dismiss();
                            }
                        });
        snackbar.show();
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    mLoginProgress.dismiss();

                    String deviceToken = FirebaseInstanceId.getInstance().getToken();
                    String user_id = mAuth.getCurrentUser().getUid();

                    mUserDatabase.child(user_id).child("device_token").setValue(deviceToken).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Intent mainIntent = new Intent(LoginActivity.this,MainActivity.class);
                            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(mainIntent);
                            finish();
                        }
                    });
                }
                else {
                    mLoginProgress.hide();
                    Toast.makeText(LoginActivity.this,"Cannot Sign In. Please check the form and try again !",Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.help_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        if (item.getItemId()==R.id.help_btn)
        {
            Intent helpIntent = new Intent(LoginActivity.this,HelpActivity.class);
            helpIntent.putExtra("EXTRA_ACTIVITY_CLASS",LoginActivity.class);
            startActivity(helpIntent);
        }
        return true;
    }
}

