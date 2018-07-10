package com.aapkabazzaar.abchat;

import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatsFragment extends Fragment {

    private RecyclerView mChatList;
    private DatabaseReference mMessagesDatabase, mUsersDatabase,mRootRef;
    private View mMainView;
    private String mCurrent_user_id;
    private FirebaseAuth mAuth;
    private Query chatsQuery;
    private String lastMessageKeyValue;
    private static final String IMAGE_MESSAGE = "  Image";
    private FirebaseRecyclerAdapter<Chats, ChatsFragment.ChatsViewHolder> firebaseRecyclerAdapter;

    public ChatsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mMainView = inflater.inflate(R.layout.fragment_chats, container, false);

        mChatList = mMainView.findViewById(R.id.chatList);
        mAuth = FirebaseAuth.getInstance();
        mCurrent_user_id = mAuth.getCurrentUser().getUid();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mMessagesDatabase = FirebaseDatabase.getInstance().getReference().child("messages").child(mCurrent_user_id);
        mUsersDatabase =FirebaseDatabase.getInstance().getReference().child("Users");
        mRootRef.keepSynced(true);
        mMessagesDatabase.keepSynced(true);
        mUsersDatabase.keepSynced(true);
        chatsQuery = mRootRef.child("lastMessage").child(mCurrent_user_id).orderByChild("lastMessageKey");
        mChatList.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        mChatList.setLayoutManager(layoutManager);
       // mChatList.setLayoutManager(new LinearLayoutManager(getContext()));
        return mMainView;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions chatsOptions = new FirebaseRecyclerOptions.Builder<Chats>().setQuery(chatsQuery, Chats.class).build();
        firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Chats, ChatsFragment.ChatsViewHolder>(chatsOptions) {
            @NonNull
            @Override
            public ChatsFragment.ChatsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.user_single_chat_fragment, parent, false);

                return new ChatsFragment.ChatsViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(final ChatsFragment.ChatsViewHolder holder, final int position, Chats model) {
                final String list_user_id = getRef(position).getKey();
                mUsersDatabase.child(list_user_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        final String userName = dataSnapshot.child("name").getValue().toString();
                        String userThumb = dataSnapshot.child("thumb_image").getValue().toString();
                        if (dataSnapshot.hasChild("online"))
                        {
                            String userOnline = dataSnapshot.child("online").getValue().toString();
                            holder.setUserOnline(userOnline);
                        }
                        if (userName.length()<20) {
                            holder.setName(userName);
                        }
                        else {
                            holder.setName(userName.substring(0,17)+"...");
                        }
                        holder.setImage(userThumb, getContext());

                        holder.mView.setOnClickListener(new View.OnClickListener() {
                            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                            @Override
                            public void onClick(View v) {

                                //For shared transition animation to Chat Activity
                                Intent chatSharedIntent = new Intent(getContext(), ChatActivity.class);

                                Pair[] pairs = new Pair[2];
                                pairs[0] = new Pair<View, String>(holder.mView.findViewById(R.id.user_single_image),"imageTransition");
                                pairs[1] = new Pair<View, String>(holder.mView.findViewById(R.id.user_single_name),"nameTransition");

                                ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(getActivity(), pairs);

                                chatSharedIntent.putExtra("user_id", list_user_id);
                                chatSharedIntent.putExtra("chatUserName",userName);
                                startActivity(chatSharedIntent,options.toBundle());
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
                mMessagesDatabase.child(list_user_id).child(model.getLastMessageKey()).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String message = dataSnapshot.child("message").getValue().toString();
                        String type = dataSnapshot.child("type").getValue().toString();
                        Long time = (Long) dataSnapshot.child("time").getValue();
                        GetMessageTimeAgo gmta = new GetMessageTimeAgo();
                        String date = gmta.getMessageTimeAgo(time,getContext());
                        holder.setDate(date);

                        String filteredMessage = filterMessage(message);

                        if (type.equals("text")) {
                            if (filteredMessage.length()<35) {
                                holder.setLastMessageKey(filteredMessage,type);
                            }
                            else {
                                holder.setLastMessageKey(filteredMessage.substring(0,32).trim()+"...",type);
                            }
                        }
                        else if (type.equals("image")){
                            holder.setLastMessageKey(IMAGE_MESSAGE,type);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        };

        mChatList.setAdapter(firebaseRecyclerAdapter);
        //To prevent getting null context Objects.requireNonNull is added
        mChatList.addItemDecoration(new SimpleDividerItemDecoration(Objects.requireNonNull(getContext())));
        firebaseRecyclerAdapter.startListening();
    }

    private String filterMessage(String message) {
        //Replacing new line characters
        String filteredMessage= message.replaceAll("\\r\\n|\\r|\\n", " ");
        return filteredMessage;
    }

    @Override
    public void onStop() {
        super.onStop();
        firebaseRecyclerAdapter.stopListening();
    }

    public static class ChatsViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public ChatsViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
        }

        public void setName(String name) {
            TextView userNameView =  mView.findViewById(R.id.user_single_name);
            userNameView.setText(name);
        }

        public void setImage(String thumb_image, Context ctx) {
            CircleImageView userImageView =  mView.findViewById(R.id.user_single_image);

            Picasso.with(ctx).load(thumb_image).placeholder(R.drawable.default_avatar).into(userImageView);

        }

        public void setDate(String date) {
            TextView timeView = mView.findViewById(R.id.user_single_message_time);
            timeView.setText(date);
        }

        public void setUserOnline(String userOnline) {

            CircleImageView imageView = mView.findViewById(R.id.user_single_online_image);
            if (userOnline.equals("true"))
            {
                imageView.setVisibility(View.VISIBLE);
            }
            else {
                imageView.setVisibility(View.INVISIBLE);
            }
        }

        public void setLastMessageKey(String lastMessageKey, String messageType)
        {
            TextView userMessageView = mView.findViewById(R.id.user_single_status);
            if (messageType.equals("image")) {
                userMessageView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_image_black_24dp, 0, 0, 0);
            }
            userMessageView.setText(lastMessageKey);
        }
    }


}
