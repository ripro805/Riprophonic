package com.example.riprophonic;

import android.content.ContentUris;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.riprophonic.databinding.ItemSongBinding;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewholder> {
    private final List<Song> songs;
    private final OnItemClickListerner listerner;
    public interface  OnItemClickListerner{
        void onItemClick(int position);
    }

    public SongAdapter(List<Song> songs, OnItemClickListerner listerner) {
        this.songs = songs;
        this.listerner = listerner;
    }

    @NonNull
    @Override
    public SongAdapter.SongViewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSongBinding binding=ItemSongBinding.inflate(
                LayoutInflater.from(parent.getContext()),parent,false);
        return new SongViewholder(binding,listerner);
    }

    @Override
    public void onBindViewHolder(@NonNull SongAdapter.SongViewholder holder, int position) {
    Song song=songs.get(position);
    holder.binding.textTitle.setText(song.title);
    holder.binding.textArtist.setText(song.artist);

        Uri albumArtUri= ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),song.albumId);

        Glide.with(holder.binding.getRoot().getContext())
                .load(albumArtUri)
                .circleCrop()
                .placeholder(R.drawable.ic_music_note_24)
                .error(R.drawable.ic_music_note_24)
                .into(holder.binding.imageAlbumArt);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public class SongViewholder extends RecyclerView.ViewHolder {
        final ItemSongBinding binding;
        final OnItemClickListerner listener;
        public SongViewholder(ItemSongBinding binding,OnItemClickListerner listerner) {

            super(binding.getRoot());
            this.binding=binding;
            this.listener=listerner;
            binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   if (listerner!=null){
                       int pos=getAdapterPosition();
                       if(pos!=RecyclerView.NO_POSITION){
                           listener.onItemClick(pos);
                       }
                   }
                }
            });
        }
    }
}
