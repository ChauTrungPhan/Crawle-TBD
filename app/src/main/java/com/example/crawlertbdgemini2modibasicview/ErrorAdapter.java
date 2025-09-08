package com.example.crawlertbdgemini2modibasicview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
//import com.example.crawlertbdgemini2modibasicview.R;
//import com.example.crawlertbdgemini2modibasicview.models.ErrorUrl;

import java.util.List;

public class ErrorAdapter extends RecyclerView.Adapter<ErrorAdapter.ErrorViewHolder> {

    private List<ErrorUrl> errorList;

    public ErrorAdapter(List<ErrorUrl> errorList) {
        this.errorList = errorList;
    }

    @NonNull
    @Override
    public ErrorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_error_url, parent, false);
        return new ErrorViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ErrorViewHolder holder, int position) {
        ErrorUrl errorUrl = errorList.get(position);
        holder.txtUrl.setText(errorUrl.getUrl());
        //holder.txtParent.setText(String.format("%d", errorUrl.getParentId()));
        //holder.txtLevel.setText(String.valueOf(errorUrl.getLevel()));
        holder.txtStatus.setText(String.format("Status: %d", errorUrl.getStatus()));
        holder.txtError.setText(String.format("Error: %s", errorUrl.getErrorMessage()==null?"":errorUrl.getErrorMessage()));
    }

    @Override
    public int getItemCount() {
        return errorList.size();
    }

    static class ErrorViewHolder extends RecyclerView.ViewHolder {
        TextView txtUrl, txtStatus, txtError; //txtParent, txtLevel, txtStatus, txtError;

        public ErrorViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUrl = itemView.findViewById(R.id.txtUrl);
            //txtParent = itemView.findViewById(R.id.txtParent);
            //txtLevel = itemView.findViewById(R.id.txtLevel);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtError = itemView.findViewById(R.id.txtError);
        }
    }
}

