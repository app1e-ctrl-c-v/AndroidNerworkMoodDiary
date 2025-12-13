package com.example.androidnetworkmooddiary.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidnetworkmooddiary.MainActivity;
import com.example.androidnetworkmooddiary.R;
import com.example.androidnetworkmooddiary.models.MoodDiary;

import java.util.ArrayList;
import java.util.List;

public class MoodDiaryAdapter extends RecyclerView.Adapter<MoodDiaryAdapter.MoodDiaryViewHolder> {
    private List<MoodDiary> entries = new ArrayList<>();
    private OnDeleteClickListener onDeleteClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    public interface OnItemLongClickListener {
        void onItemLongClick(int position, long itemId);
    }
    public interface OnDeleteClickListener {
        void onDeleteClick(int position, int itemId);
    }
    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }
    @NonNull
    @Override
    public MoodDiaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item, parent, false);
        return new MoodDiaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MoodDiaryAdapter.MoodDiaryViewHolder holder, int position) {
        MoodDiary entry = entries.get(position);
        holder.textDate.setText(entry.getRecord_date());
        holder.textComment.setText(entry.getComment_day());
        holder.textMoodRating.setText(entry.getMood_assessment()+"/10");
        holder.itemId = entry.getId();
        holder.buttonDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(holder.itemView.getContext())
                    .setTitle("Удалить задачу?")
                    .setMessage(entry.getComment_day())
                    .setPositiveButton("Да", (d,i) ->
                            ((MainActivity)holder.itemView.getContext()).deleteTask(entry.getId(), position))
                    .setNegativeButton("Нет", null)
                    .show();
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (onItemLongClickListener != null) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemLongClickListener.onItemLongClick(adapterPosition, holder.itemId);
                }
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public void setMoodDiaries(List<MoodDiary> entries) {
        this.entries = entries;
        notifyDataSetChanged();
    }
    public List<MoodDiary> getMoodDiaries() {
        return entries;
    }

    public static class MoodDiaryViewHolder extends RecyclerView.ViewHolder {
        TextView textDate, textComment, textMoodRating;
        ImageButton buttonDelete;
        int itemId;
        LinearLayout layout;

        @SuppressLint("WrongViewCast")
        public MoodDiaryViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.textDate);
            textComment = itemView.findViewById(R.id.textComment);
            textMoodRating = itemView.findViewById(R.id.textMoodRating);
            buttonDelete = (ImageButton) itemView.findViewById(R.id.buttonDelete);
            layout = itemView.findViewById(R.id.layout);
        }
    }
}