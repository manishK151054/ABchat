package com.aapkabazzaar.abchat;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Messages> mMessagesList;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    public MessageAdapter(List<Messages> mMessagesList) {
        this.mMessagesList = mMessagesList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_single_image_layout,parent,false);
        return new MessageViewHolder(v);
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder
    {
        public TextView mMessageTextView,mUserTextView,mTimeTextView;
        public CircleImageView mMessageCircleImageView;
        public RelativeLayout messageLayout;
        public ImageView messageImage;

        public MessageViewHolder(View itemView) {
            super(itemView);

            mMessageTextView = itemView.findViewById(R.id.message_single_text_view);
            mUserTextView = itemView.findViewById(R.id.chat_userName_single_text_view);
            mTimeTextView = itemView.findViewById(R.id.time_single_text_view);
            mMessageCircleImageView = itemView.findViewById(R.id.message_single_profile_image);
            messageLayout = itemView.findViewById(R.id.message_layout);
            messageImage = itemView.findViewById(R.id.message_image);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder holder, int position) {
        Messages c = mMessagesList.get(position);
        mAuth = FirebaseAuth.getInstance();
        String current_user_id = mAuth.getCurrentUser().getUid();

        String from = c.getFrom();
        String messageType = c.getType();
        long timeOfMessage = c.getTime();

        if (messageType.equals("text"))
        {
            holder.mMessageTextView.setVisibility(View.VISIBLE);
            holder.messageImage.setVisibility(View.INVISIBLE);
        }
        else
        {
            holder.mMessageTextView.setVisibility(View.INVISIBLE);
            Picasso.with(holder.messageImage.getContext()).load(c.getMessage())
                    .placeholder(R.drawable.default_avatar).into(holder.messageImage);
        }

        userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(from);
        userRef.keepSynced(true);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String image_url = dataSnapshot.child("thumb_image").getValue().toString();
                String name = dataSnapshot.child("name").getValue().toString();

                holder.mUserTextView.setText(name);
                // holder.messageImage.setVisibility(View.INVISIBLE);
                Picasso.with(holder.mMessageCircleImageView.getContext()).load(image_url).placeholder(R.drawable.default_avatar2).into(holder.mMessageCircleImageView);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        if (current_user_id.equals(from))
        {
            holder.messageLayout.setBackgroundResource(R.drawable.background_of_message_view_from);
            holder.mMessageTextView.setTextColor(Color.BLACK);
            holder.mUserTextView.setTextColor(Color.BLACK);
            holder.mTimeTextView.setTextColor(Color.BLACK);
            // holder.mMessageCircleImageView.setVisibility(View.INVISIBLE);
        }
        else
        {
            holder.messageLayout.setBackgroundResource(R.drawable.background_of_message_view);
            holder.mMessageTextView.setTextColor(Color.WHITE);
            holder.mTimeTextView.setTextColor(Color.WHITE);
            holder.mUserTextView.setTextColor(Color.WHITE);
            //  holder.mMessageCircleImageView.setForegroundGravity(View.FOCUS_RIGHT);
        }
        holder.mMessageTextView.setText(c.getMessage());
        holder.mTimeTextView.setText(getMessageTime(timeOfMessage));
    }

    private String getMessageTime(long timeOfMessage) {
        String time;

        SimpleDateFormat sfd = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        time = sfd.format(new Date(timeOfMessage)).toString();
        return time;
    }

    @Override
    public int getItemCount() {
        return mMessagesList.size();
    }

}

