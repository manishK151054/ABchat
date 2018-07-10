package com.aapkabazzaar.abchat;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private String chat_user_id,chat_user_name,current_user_id;
    private Toolbar mChatToolbar;
    private FirebaseAuth mAuth;
    private DatabaseReference mUsersRef,mRootRef;
    private TextView chatUserName,chatUserLastSeen;
    private CircleImageView chatUserImage;
    private EditText chatMessageEditText;
    private ImageButton chatMessageAddBtn,chatMessageSendBtn;
    private RecyclerView mMessageList;
    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager mLinearLayout;
    private MessageAdapter mAdapter;
    private LinearLayout mMessageLinearLayout;
    private boolean exist ;
    private static final int TOTAL_ITEMS_TO_LOAD = 10;
    private static final int GALLERY_PICK =1;
    private StorageReference mImageStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chat_user_id = getIntent().getStringExtra("user_id");
        chat_user_name = getIntent().getStringExtra("chatUserName");
        mChatToolbar = findViewById(R.id.chat_bar_layout);
        mImageStorage = FirebaseStorage.getInstance().getReference();

        setSupportActionBar(mChatToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        mAuth = FirebaseAuth.getInstance();
        if (mAuth!=null)
        {
            current_user_id = mAuth.getCurrentUser().getUid();
        }

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View cutom_view = inflater.inflate(R.layout.chat_custom_bar,null);
        actionBar.setCustomView(cutom_view);

        chatUserName = findViewById(R.id.custom_bar_display_name);
        chatUserLastSeen = findViewById(R.id.custom_bar_last_seen);
        chatUserImage = findViewById(R.id.custom_bar_image);
        if (chat_user_name.length()<20) {
            chatUserName.setText(chat_user_name);
        }
        else
        {
            chat_user_name = chat_user_name.substring(0,17)+"...";
            chatUserName.setText(chat_user_name);
        }

        mAdapter = new MessageAdapter(messagesList);
        mMessageList = findViewById(R.id.message_list_recycler_view);
        mMessageLinearLayout = findViewById(R.id.chat_message_linear_layout);
        mLinearLayout = new LinearLayoutManager(this);
        mMessageList.setHasFixedSize(true);
        mMessageList.setLayoutManager(mLinearLayout);
       // mMessageList.getLayoutManager().setMeasurementCacheEnabled(false);
        mMessageList.setAdapter(mAdapter);

        mRootRef = FirebaseDatabase.getInstance().getReference();
        mUsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        mRootRef.keepSynced(true);
        mUsersRef.keepSynced(true);
        loadMessages();
       // Toast.makeText(this, "messages loading...", Toast.LENGTH_SHORT).show();
        mRootRef.child("Friends").child(current_user_id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
              //  if (dataSnapshot.child(chat_user_id).exists())
              //  {
                    mMessageLinearLayout.setVisibility(View.VISIBLE);
                    int optionId = dataSnapshot.child(chat_user_id).exists() ? R.layout.send_message : R.layout.unable_to_send_message;

                    View C = findViewById(R.id.chat_message_linear_layout);
                    ViewGroup parent = (ViewGroup) C.getParent();
                    int index = parent.indexOfChild(C);
                    parent.removeView(C);
                    C = getLayoutInflater().inflate(optionId, parent, false);
                    parent.addView(C, index);

                    if (optionId==R.layout.send_message) {
                        chatMessageEditText = C.findViewById(R.id.chat_message_edit_text);
                        chatMessageAddBtn = C.findViewById(R.id.chat_message_add_btn);
                        chatMessageSendBtn = C.findViewById(R.id.chat_message_send_btn);

                        chatMessageSendBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                sendMessage();
                            }
                        });


                        chatMessageAddBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent galleryIntent = new Intent();
                                galleryIntent.setType("image/*");
                                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                                startActivityForResult(Intent.createChooser(galleryIntent, "Select Image"), GALLERY_PICK);
                            }
                        });
                    }
              //  }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mUsersRef.child(chat_user_id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String thumbImage = dataSnapshot.child("thumb_image").getValue().toString();
                Picasso.with(ChatActivity.this).load(thumbImage).placeholder(R.drawable.default_avatar2).into(chatUserImage);
                String online = dataSnapshot.child("online").getValue().toString();

                if (online.equals("true"))
                {
                    chatUserLastSeen.setText("Online");
                }
                else
                {
                    GetTimeAgo getTimeAgo = new GetTimeAgo();
                    long last_seen = Long.parseLong(online);
                    String last_seen_time = getTimeAgo.getTimeAgo(last_seen,getApplicationContext());
                    chatUserLastSeen.setText(last_seen_time);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        /*chatMessageSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        }); */


       /* chatMessageAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(galleryIntent,"Select Image"),GALLERY_PICK);
            }
        }); */

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();

            final String currentUserRef = "messages/" + current_user_id + "/" + chat_user_id;
            final String chatUserRef = "messages/" + chat_user_id + "/" + current_user_id;

            final String currentUserMessageRef = "lastMessage/" + current_user_id + "/" + chat_user_id;
            final String chatUserMessageRef = "lastMessage/" + chat_user_id + "/" + current_user_id;

            DatabaseReference messageUserRef = mRootRef.child("messages").child(current_user_id).child(chat_user_id).push();
            final String pushId = messageUserRef.getKey();

            StorageReference filePath = mImageStorage.child("message_images").child(pushId+".jpg");

            filePath.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                    if (task.isSuccessful()) {
                        String downloadUrl = task.getResult().getDownloadUrl().toString();

                        Map messageMap = new HashMap();
                        messageMap.put("message", downloadUrl);
                        messageMap.put("seen", false);
                        messageMap.put("type", "image");
                        messageMap.put("time", ServerValue.TIMESTAMP);
                        messageMap.put("from", current_user_id);

                        Map messageUserMap = new HashMap();
                        messageUserMap.put(currentUserRef + "/" + pushId, messageMap);
                        messageUserMap.put(chatUserRef + "/" + pushId, messageMap);

                        Map lastMessageMap = new HashMap();
                        lastMessageMap.put("lastMessageKey",pushId);

                        Map lastMessageUserMap = new HashMap();
                        lastMessageUserMap.put(currentUserMessageRef ,lastMessageMap);
                        lastMessageUserMap.put(chatUserMessageRef ,lastMessageMap);

                        chatMessageEditText.setText("");

                        mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if (databaseError != null) {
                                    Toast.makeText(ChatActivity.this, databaseError.getMessage().toString(), Toast.LENGTH_SHORT).show();
                                } else {
                                    loadMessages();
                                }
                            }
                        });

                        mRootRef.updateChildren(lastMessageUserMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if (databaseError != null)
                                {
                                    Toast.makeText(ChatActivity.this, databaseError.getMessage().toString(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                    }
                }
            });



        }
    }

    private void loadMessages() {
            //
            DatabaseReference messagesRef = mRootRef.child("messages").child(current_user_id).child(chat_user_id);
            messagesRef.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Messages messages = dataSnapshot.getValue(Messages.class);
                    messagesList.add(messages);
                    mAdapter.notifyDataSetChanged();

                    mMessageList.scrollToPosition(messagesList.size());

                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
      //  }

    }

    private void sendMessage() {
        String message = chatMessageEditText.getText().toString();
        if (!TextUtils.isEmpty(message)) {
            String currentUserRef = "messages/" + current_user_id + "/" + chat_user_id;
            String chatUserRef = "messages/" + chat_user_id + "/" + current_user_id;

            String currentUserMessageRef = "lastMessage/" + current_user_id + "/" + chat_user_id;
            String chatUserMessageRef = "lastMessage/" + chat_user_id + "/" + current_user_id;

            DatabaseReference messageUserRef = mRootRef.child("messages").child(current_user_id).child(chat_user_id).push();
            String pushId = messageUserRef.getKey();

            Map messageMap = new HashMap();
            messageMap.put("message", message);
            messageMap.put("seen", false);
            messageMap.put("type", "text");
            messageMap.put("time", ServerValue.TIMESTAMP);
            messageMap.put("from",current_user_id);

            Map messageUserMap = new HashMap();
            messageUserMap.put(currentUserRef + "/" + pushId,messageMap);
            messageUserMap.put(chatUserRef + "/" + pushId,messageMap);

            Map lastMessageMap = new HashMap();
            lastMessageMap.put("lastMessageKey",pushId);

            Map lastMessageUserMap = new HashMap();
            lastMessageUserMap.put(currentUserMessageRef ,lastMessageMap);
            lastMessageUserMap.put(chatUserMessageRef ,lastMessageMap);

            chatMessageEditText.setText("");

            mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError != null)
                    {
                        Toast.makeText(ChatActivity.this, databaseError.getMessage().toString() , Toast.LENGTH_SHORT).show();
                    }
                    else {
                        loadMessages();
                    }
                }
            });

            mRootRef.updateChildren(lastMessageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError != null)
                    {
                        Toast.makeText(ChatActivity.this, databaseError.getMessage().toString(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            sendToStart();
        }
        else {
            mUsersRef.child(currentUser.getUid()).child("online").setValue("true");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            mUsersRef.child(currentUser.getUid()).child("online").setValue(ServerValue.TIMESTAMP);
        }
    }

    private void sendToStart() {

        Intent startIntent = new Intent(ChatActivity.this,StartActivity.class);
        startActivity(startIntent);
        finish();
    }


}
