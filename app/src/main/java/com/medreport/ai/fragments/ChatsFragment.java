package com.medreport.ai.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.medreport.ai.activities.ChatActivity;
import com.medreport.ai.adapters.ChatAdapter;
import com.medreport.ai.databinding.FragmentChatsBinding;
import com.medreport.ai.models.*;
import com.medreport.ai.network.ApiClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class ChatsFragment extends Fragment implements ChatAdapter.Listener {
    private FragmentChatsBinding b;
    private ChatAdapter adapter;
    private final List<ChatModel> chats = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        b = FragmentChatsBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapter = new ChatAdapter(chats, this);
        b.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recyclerView.setAdapter(adapter);
        b.swipeRefresh.setOnRefreshListener(this::loadChats);
        loadChats();
    }

    private void loadChats() {
        b.swipeRefresh.setRefreshing(true);
        ApiClient.get().getChats().enqueue(new Callback<ResponseModels.ChatsResponse>() {
            @Override public void onResponse(Call<ResponseModels.ChatsResponse> c, Response<ResponseModels.ChatsResponse> r) {
                b.swipeRefresh.setRefreshing(false);
                if (r.isSuccessful() && r.body() != null && r.body().chats != null) {
                    chats.clear(); chats.addAll(r.body().chats);
                    adapter.notifyDataSetChanged();
                    b.tvEmpty.setVisibility(chats.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
            @Override public void onFailure(Call<ResponseModels.ChatsResponse> c, Throwable t) { b.swipeRefresh.setRefreshing(false); }
        });
    }

    @Override public void onChatClick(ChatModel chat) {
        Intent i = new Intent(requireContext(), ChatActivity.class);
        i.putExtra(ChatActivity.EXTRA_REPORT_ID, chat.reportId);
        i.putExtra(ChatActivity.EXTRA_CHAT_TITLE, chat.reportName != null ? chat.reportName : "Chat");
        startActivity(i);
    }
}
