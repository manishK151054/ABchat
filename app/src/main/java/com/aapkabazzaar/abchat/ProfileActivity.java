package com.aapkabazzaar.abchat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    TextView mDisplayName, mDisplayStatus, mDisplayTotalFriends;
    Button mSendReqBtn, mDeclineReqBtn;
    CircleImageView mDisplayImage;
    DatabaseReference mUsersDatabase, mFriendReqDatabase, mFriendDatabase, mNotificationDatabase, mRootRef;
    ProgressDialog mProgressDialog;
    String req_type;
    private FirebaseAuth mAuth;
    FirebaseUser mCurrentUser;

    private String mCurrent_state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final String user_id = getIntent().getStringExtra("user_id");
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
        mFriendReqDatabase = FirebaseDatabase.getInstance().getReference().child("Friend_req");
        mFriendDatabase = FirebaseDatabase.getInstance().getReference().child("Friends");
        mNotificationDatabase = FirebaseDatabase.getInstance().getReference().child("notifications");
        mRootRef.keepSynced(true);
        mUsersDatabase.keepSynced(true);
        mFriendDatabase.keepSynced(true);
        mFriendReqDatabase.keepSynced(true);
        mNotificationDatabase.keepSynced(true);
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();

        mDisplayName = findViewById(R.id.profile_displayName);
        mDisplayStatus = findViewById(R.id.profile_status);
        mDisplayTotalFriends = findViewById(R.id.profile_total_friends);
        mSendReqBtn = findViewById(R.id.profile_send_req_btn);
        mDeclineReqBtn = findViewById(R.id.profile_decline_req_btn);
        mDisplayImage = findViewById(R.id.profile_image);

        mCurrent_state = "not_friends";

        mDeclineReqBtn.setVisibility(View.INVISIBLE);
        mDeclineReqBtn.setEnabled(false);

        //PROGRESS DIALOG HERE
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Loading User Data");
        mProgressDialog.setMessage("Please wait while we load user data");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();


        mUsersDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String display_Name = dataSnapshot.child("name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();
                String thumb = dataSnapshot.child("thumb_image").getValue().toString();

                mDisplayName.setText(display_Name);
                mDisplayStatus.setText(status);

                Picasso.with(ProfileActivity.this).load(thumb).placeholder(R.drawable.default_avatar2).into(mDisplayImage);
                mProgressDialog.dismiss();

                //-------------------------Friends List / Request Feature-------------------//
                mFriendReqDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(user_id)) {
                            try {
                                req_type = dataSnapshot.child(user_id).child("request_type").getValue().toString();
                                if (req_type.equals("received")) {
                                    mCurrent_state = "req_received";
                                    mSendReqBtn.setText(R.string.accept_friend_request);

                                    mDeclineReqBtn.setVisibility(View.VISIBLE);
                                    mDeclineReqBtn.setEnabled(true);

                                } else if (req_type.equals("sent")) {
                                    mCurrent_state = "req_sent";
                                    mSendReqBtn.setText(R.string.cancel_friend_request);

                                    mDeclineReqBtn.setVisibility(View.INVISIBLE);
                                    mDeclineReqBtn.setEnabled(false);
                                }
                                mProgressDialog.dismiss();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        //if the user is already a friend with some1 then show the unfriend button instead
                        else {
                            mFriendDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.hasChild(user_id)) {
                                        mCurrent_state = "friends";
                                        mSendReqBtn.setText("Unfriend this Person");

                                        mDeclineReqBtn.setVisibility(View.INVISIBLE);
                                        mDeclineReqBtn.setEnabled(false);
                                    }
                                    mProgressDialog.dismiss();
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    mProgressDialog.dismiss();
                                }
                            });
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                mSendReqBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSendReqBtn.setEnabled(false);

                        //----------------NOT FRIENDS STATE----------------

                        if (mCurrent_state.equals("not_friends")) {

                            DatabaseReference mNotificationRef = mRootRef.child("notifications").child(user_id).push();
                            String notificationId = mNotificationRef.getKey();
                            HashMap<String, String> notificationData = new HashMap<>();
                            notificationData.put("from", mCurrentUser.getUid());
                            notificationData.put("type", "request");

                            Map requestMap = new HashMap();
                            requestMap.put("Friend_req/" + mCurrentUser.getUid() + "/" + user_id + "/request_type", "sent");
                            requestMap.put("Friend_req/" + user_id + "/" + mCurrentUser.getUid() + "/request_type", "received");
                            requestMap.put("notifications/" + user_id + "/" + notificationId, notificationData);
                            mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                    if (databaseError != null) {
                                        Toast.makeText(ProfileActivity.this, "There was some error sending the request", Toast.LENGTH_SHORT).show();
                                    }
                                    mSendReqBtn.setEnabled(true);
                                    mCurrent_state = "req_sent";
                                    mSendReqBtn.setText("Cancel Friend Request");
                                }
                            });
                        }
                        //-------------------------------Cancel Request STATE---------------------
                        if (mCurrent_state.equals("req_sent")) {
                            mFriendReqDatabase.child(mCurrentUser.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    mFriendReqDatabase.child(user_id).child(mCurrentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            mSendReqBtn.setEnabled(true);
                                            mCurrent_state = "not_friends";
                                            mSendReqBtn.setText(R.string.send_friend_request);

                                            mDeclineReqBtn.setVisibility(View.INVISIBLE);
                                            mDeclineReqBtn.setEnabled(false);
                                        }
                                    });
                                }
                            });
                        }

                        //-------------------------Request Recieved State-----------------------------
                        if (mCurrent_state.equals("req_received")) {
                            final String currentDate = DateFormat.getDateInstance().format(new Date());
                            Map friendsMap = new HashMap();
                            friendsMap.put("Friends/" + mCurrentUser.getUid() + "/" + user_id + "/date", currentDate);
                            friendsMap.put("Friends/" + user_id + "/" + mCurrentUser.getUid() + "/date", currentDate);
                            friendsMap.put("Friend_req/" + mCurrentUser.getUid() + "/" + user_id, null);
                            friendsMap.put("Friend_req/" + user_id + "/" + mCurrentUser.getUid(), null);

                            mRootRef.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                    if (databaseError == null) {
                                        mSendReqBtn.setEnabled(true);
                                        mCurrent_state = "friends";
                                        mSendReqBtn.setText("Unfriend this Person");

                                        mDeclineReqBtn.setVisibility(View.INVISIBLE);
                                        mDeclineReqBtn.setEnabled(false);
                                    } else {
                                        String errorMessage = databaseError.getMessage();
                                        Toast.makeText(ProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                        //-----------------------------Unfriend Feature -------------------------------
                        if (mCurrent_state.equals("friends")) {
                            Map unfriendMap = new HashMap();
                            unfriendMap.put("Friends/" + mCurrentUser.getUid() + "/" + user_id, null);
                            unfriendMap.put("Friends/" + user_id + "/" + mCurrentUser.getUid(), null);

                            mRootRef.updateChildren(unfriendMap, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                    if (databaseError == null) {
                                        mCurrent_state = "not_friends";
                                        mSendReqBtn.setText("Send Friend Request");
                                        mDeclineReqBtn.setVisibility(View.INVISIBLE);
                                        mDeclineReqBtn.setEnabled(false);
                                    } else {
                                        String errorMessage = databaseError.getMessage();
                                        Toast.makeText(ProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                    }
                                    mSendReqBtn.setEnabled(true);
                                }
                            });
                        }
                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        if (mCurrentUser == null) {
            sendToStart();
        }
        else {
            mRootRef.child("Users").child(mCurrentUser.getUid()).child("online").setValue("true");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mCurrentUser != null) {
            mRootRef.child("Users").child(mCurrentUser.getUid()).child("online").setValue(ServerValue.TIMESTAMP);
        }
    }

    private void sendToStart() {

        Intent startIntent = new Intent(ProfileActivity.this,StartActivity.class);
        startActivity(startIntent);
        finish();
    }
}

