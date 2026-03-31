package com.skillconnect.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.skillconnect.ChatActivity;
import com.skillconnect.R;
import com.skillconnect.adapters.ChatThreadAdapter;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;
import com.skillconnect.models.ChatThread;
import java.util.ArrayList;
import java.util.List;

public class ChatListFragment extends Fragment {

    private RecyclerView rvChats;
    private TextView tvEmpty;
    private ChatThreadAdapter adapter;
    private FirebaseRepository repo;
    private SessionManager session;
    private com.google.firebase.firestore.ListenerRegistration chatListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat_list, container, false);

        rvChats = v.findViewById(R.id.rvChats);
        tvEmpty = v.findViewById(R.id.tvEmpty);

        rvChats.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ChatThreadAdapter(new ArrayList<>(), thread -> {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra("chat_partner_id",   thread.getPartnerId());
            intent.putExtra("chat_partner_name", thread.getPartnerName());
            startActivity(intent);
        });
        rvChats.setAdapter(adapter);

        repo    = FirebaseRepository.getInstance();
        session = new SessionManager(getContext());

        loadChats();
        return v;
    }

    private void loadChats() {
        if (chatListener != null) chatListener.remove();

        String uid = session.getUserId();
        if (uid == null || uid.isEmpty()) {
            showEmpty("Please log in to see your chats.");
            return;
        }

        chatListener = repo.listenChatThreads(uid, new FirebaseRepository.Callback<List<ChatThread>>() {
            @Override
            public void onSuccess(List<ChatThread> threads) {
                if (!isAdded()) return; // Fragment may be detached
                if (threads == null || threads.isEmpty()) {
                    showEmpty("No conversations yet.\nAccept a bid to start chatting!");
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvChats.setVisibility(View.VISIBLE);
                    adapter.updateThreads(threads);
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                // On a fresh install with no chat history, show empty state instead of crash
                showEmpty("No conversations yet.\nAccept a bid to start chatting!");
            }
        });
    }

    private void showEmpty(String message) {
        tvEmpty.setText(message);
        tvEmpty.setVisibility(View.VISIBLE);
        rvChats.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chatListener != null) {
            chatListener.remove();
            chatListener = null;
        }
    }
}
