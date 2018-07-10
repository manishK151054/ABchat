package com.aapkabazzaar.abchat;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.squareup.picasso.Picasso;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class UsersActivity extends AppCompatActivity {

    Toolbar mToolbar;
    private RecyclerView mUsersList;
    private DatabaseReference mUsersDatabase;
    Query usersQuery;
    FirebaseRecyclerAdapter<Users, UsersViewHolder> firebaseRecyclerAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        mToolbar = findViewById(R.id.users_appBar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("All Users");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mUsersDatabase.keepSynced(true);
        usersQuery = mUsersDatabase.orderByKey();

        mUsersList = findViewById(R.id.users_list);
        mUsersList.setHasFixedSize(true);
        mUsersList.setLayoutManager(new LinearLayoutManager(this));

    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseRecyclerOptions userOptions = new FirebaseRecyclerOptions.Builder<Users>().setQuery(usersQuery, Users.class).build();
        firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Users, UsersViewHolder>(userOptions) {
            @NonNull
            @Override
            public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.users_single_layout, parent, false);

                return new UsersViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(final UsersViewHolder holder, int position, Users model) {

                if (model.getName().length()<27) {
                    holder.setName(model.getName());
                }
                else {
                    holder.setName(model.getName().substring(0,24)+"...");
                }

                if (model.getStatus().length()<35) {
                    holder.setStatus(model.getStatus());
                }
                else {
                    holder.setStatus(model.getStatus().substring(0,32)+"...");
                }
                holder.setImage(model.getThumb_image(), getApplicationContext());
                final String user_id = getRef(position).getKey();
                Log.v("user id", user_id);
                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onClick(View v) {

                        //For shared transition animation
                        Intent profileSharedIntent = new Intent(UsersActivity.this, ProfileActivity.class);

                        Pair[] pairs = new Pair[3];
                        pairs[0] = new Pair<View, String>(holder.mView.findViewById(R.id.user_single_image),"imageTransition");
                        pairs[1] = new Pair<View, String>(holder.mView.findViewById(R.id.user_single_name),"nameTransition");
                        pairs[2] = new Pair<View, String>(holder.mView.findViewById(R.id.user_single_status),"statusTransition");

                        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(UsersActivity.this, pairs);

                        profileSharedIntent.putExtra("user_id", user_id);
                        startActivity(profileSharedIntent,options.toBundle());

                    }
                });
            }
        };
        mUsersList.setAdapter(firebaseRecyclerAdapter);
        mUsersList.addItemDecoration(new SimpleDividerItemDecoration(this));
        firebaseRecyclerAdapter.startListening();

    }

    @Override
    protected void onStop() {
        super.onStop();
        firebaseRecyclerAdapter.stopListening();

    }

    public static class UsersViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public UsersViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
        }

        public void setName(String name) {
            TextView userNameView =  mView.findViewById(R.id.user_single_name);
            userNameView.setText(name);
        }

        void setStatus(String status) {
            TextView userStatusView = mView.findViewById(R.id.user_single_status);
            userStatusView.setText(status);
        }

        void setImage(String thumb_image, Context ctx) {
            CircleImageView userImageView =  mView.findViewById(R.id.user_single_image);

            Picasso.with(ctx).load(thumb_image).placeholder(R.drawable.default_avatar).into(userImageView);

        }
    }
}

