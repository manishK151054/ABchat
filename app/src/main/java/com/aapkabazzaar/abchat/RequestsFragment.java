package com.aapkabazzaar.abchat;

import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

public class RequestsFragment extends Fragment {

    private RecyclerView mFriendRequestList;
    private DatabaseReference mFriendReqDatabase, mUsersDatabase;
    private View mMainView;
    private String mCurrent_user_id;
    private FirebaseAuth mAuth;
    private Query friendsQuery;
    private FirebaseRecyclerAdapter<Requests, RequestsFragment.FriendReqViewHolder> firebaseRecyclerAdapter;

    public RequestsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mMainView = inflater.inflate(R.layout.fragment_requests, container, false);

        mFriendRequestList = mMainView.findViewById(R.id.friendRequestList);
        mAuth = FirebaseAuth.getInstance();
        mCurrent_user_id = mAuth.getCurrentUser().getUid();
        mFriendReqDatabase = FirebaseDatabase.getInstance().getReference().child("Friend_req").child(mCurrent_user_id);
        mFriendReqDatabase.keepSynced(true);
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mUsersDatabase.keepSynced(true);
        friendsQuery = mFriendReqDatabase.orderByKey();
        mFriendRequestList.setHasFixedSize(true);
        mFriendRequestList.setLayoutManager(new LinearLayoutManager(getContext()));
        return mMainView;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions friendsOptions = new FirebaseRecyclerOptions.Builder<Requests>().setQuery(friendsQuery, Requests.class).build();
        firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Requests,FriendReqViewHolder>(friendsOptions) {
            @NonNull
            @Override
            public FriendReqViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.users_single_layout, parent, false);

                return new FriendReqViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(final FriendReqViewHolder holder, final int position, Requests model) {
                holder.setRequestType(model.getReq_type());
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
                        if (userName.length()<27) {
                            holder.setName(userName);
                        }
                        else {
                            holder.setName(userName.substring(0,24)+"...");
                        }
                        holder.setImage(userThumb, getContext());

                        holder.mView.setOnClickListener(new View.OnClickListener() {
                            @android.support.annotation.RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                            @Override
                            public void onClick(View v) {
                                //For shared transition animation
                                Intent profileSharedIntent = new Intent(getContext(),ProfileActivity.class);

                                Pair[] pairs = new Pair[2];
                                pairs[0] = new Pair<View, String>(holder.mView.findViewById(R.id.user_single_image),"imageTransition");
                                pairs[1] = new Pair<View, String>(holder.mView.findViewById(R.id.user_single_name),"nameTransition");

                                ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(getActivity(), pairs);

                                profileSharedIntent.putExtra("user_id", list_user_id);
                                startActivity(profileSharedIntent,options.toBundle());
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        };

        mFriendRequestList.setAdapter(firebaseRecyclerAdapter);
        //To prevent getting null context Objects.requireNonNull is added
        mFriendRequestList.addItemDecoration(new SimpleDividerItemDecoration(Objects.requireNonNull(getContext())));
        firebaseRecyclerAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        firebaseRecyclerAdapter.stopListening();
    }

    public class FriendReqViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public FriendReqViewHolder(View itemView) {
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

        public void setRequestType(String req_type) {
            TextView userStatusView = mView.findViewById(R.id.user_single_status);
            userStatusView.setText(req_type);
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

    }
}

