package com.example.androidnetworkmooddiary.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidnetworkmooddiary.R;
import com.example.androidnetworkmooddiary.models.MoodDiary;

import java.util.ArrayList;
import java.util.List;

public class MoodDiaryAdapter extends RecyclerView.Adapter<MoodDiaryAdapter.MoodDiaryViewHolder> {
    private List<MoodDiary> entries = new ArrayList<>();

    @NonNull
    @Override
    public MoodDiaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item, parent, false);
        return new MoodDiaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MoodDiaryViewHolder holder, int position) {
        MoodDiary entry = entries.get(position);
        holder.textDate.setText(entry.getRecord_date());
        holder.textComment.setText(entry.getComment_day());
        holder.textMoodRating.setText(entry.getMood_assessment()+"/10");
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public void setMoodDiaries(List<MoodDiary> entries) {
        this.entries = entries;
        notifyDataSetChanged();
    }

    public static class MoodDiaryViewHolder extends RecyclerView.ViewHolder {
        TextView textDate, textComment, textMoodRating;

        public MoodDiaryViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.textDate);
            textComment = itemView.findViewById(R.id.textComment);
            textMoodRating = itemView.findViewById(R.id.textMoodRating);
        }
    }
}