package com.voibiz.lanchat.android.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.voibiz.lanchat.android.R;
import com.voibiz.lanchat.core.model.Peer;

import java.util.ArrayList;
import java.util.List;

public class PeerAdapter extends RecyclerView.Adapter<PeerAdapter.PeerViewHolder> {

    private List<Peer> peers = new ArrayList<>();

    public void setPeers(List<Peer> peers) {
        this.peers = peers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PeerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_peer, parent, false);
        return new PeerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PeerViewHolder holder, int position) {
        Peer peer = peers.get(position);
        
        if (holder.textDisplayName != null) {
            String statusText = peer.getStatus() != null ? peer.getStatus().name() : "UNKNOWN";
            holder.textDisplayName.setText(peer.getDisplayName() + " [" + statusText + "]");
        }
    }

    @Override
    public int getItemCount() {
        return peers == null ? 0 : peers.size();
    }

    public static class PeerViewHolder extends RecyclerView.ViewHolder {
        TextView textDisplayName;

        public PeerViewHolder(@NonNull View itemView) {
            super(itemView);
            textDisplayName = itemView.findViewById(R.id.peerName);
        }
    }
}
