package com.example.pdfmaker.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.example.core.ItemsData;
import com.example.core.db.AppDataBase;
import com.example.pdfmaker.R;
import com.example.pdfmaker.databinding.HeaderItemBinding;
import com.example.pdfmaker.databinding.LogItemBinding;
import com.example.core.db.FileEntity;
import com.example.core.FileTools;
import com.example.pdfmaker.viewmodels.LogsActivityViewModel;
import com.rk.activities.main.MainActivity;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class LogsRecyclerAdapter extends RecyclerView.Adapter<LogsRecyclerAdapter.LogViewHolder> {

    private final int TYPE_HEADER = 1;
    private final int TYPE_ITEM = 2;
    private LayoutInflater inflater;
    private MutableLiveData<List<ItemsData>> logFileEntities;
    private final FileTools fileTools;
    private final PopupMenu[] popupMenus;
    private final ThreadPoolExecutor poolExecutor;
    private final Handler handler;
    private final  Context context;
    private  LogsActivityViewModel viewModel;
    private final AppDataBase dataBase;

    @Inject
    public LogsRecyclerAdapter(@ApplicationContext Context context, FileTools fileTools, ThreadPoolExecutor poolExecutor,
                               Handler handler, AppDataBase dataBase) {
        this.fileTools = fileTools;
        popupMenus = new PopupMenu[1];
        this.poolExecutor = poolExecutor;
        this.handler = handler;
        this.context =context;
        this.dataBase = dataBase;
    }

    @Override
    public int getItemViewType(int position) {
        if (logFileEntities.getValue().get(position).getType().equals("header")) {
            return TYPE_HEADER;
        } else if (logFileEntities.getValue().get(position).getType().equals("file")) {
            return TYPE_ITEM;
        } else {
            return super.getItemViewType(position);
        }
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_ITEM) {
            view = inflater.inflate(R.layout.log_item, parent, false);
        } else if (viewType == TYPE_HEADER) {
            view = inflater.inflate(R.layout.header_item, parent, false);
        } else {
            view = inflater.inflate(R.layout.log_item, parent, false);
        }
        return new LogViewHolder(view, context, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        if (logFileEntities.getValue().get(holder.getAbsoluteAdapterPosition()).getType().equals("header")) {
            holder.name.setText(logFileEntities.getValue().get(holder.getAbsoluteAdapterPosition()).getHeaderName());
        } else {
            holder.name.setText(logFileEntities.getValue().get(holder.getAbsoluteAdapterPosition()).getFileEntity().getName());
            holder.lastModified.setText(fileTools.getHTime(logFileEntities.getValue().get(holder.getAbsoluteAdapterPosition()).getFileEntity().getLastModified()));
            holder.size.setText(fileTools.getHSize(logFileEntities.getValue().get(holder.getAbsoluteAdapterPosition()).getFileEntity().getSize()));
        }
    }

    @Override
    public int getItemCount() {
        return logFileEntities.getValue().size();
    }

    public class LogViewHolder extends RecyclerView.ViewHolder {
        private  TextView name;
        private TextView lastModified;
        private TextView size;
        private LogItemBinding binding;

        public LogViewHolder(@NonNull View itemView, Context context, int type) {
            super(itemView);
            if (type == TYPE_ITEM){
                binding = LogItemBinding.bind(itemView);
                name = binding.item;
                lastModified = binding.lastModified;
                size = binding.size;
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupMenus[0] = new PopupMenu(itemView.getContext(), v, Gravity.END);
                        popupMenus[0].inflate(R.menu.log_item_options);
                        popupMenus[0].setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                if (item.getItemId() == R.id.delete) {
                                    poolExecutor.execute(()->{
                                        fileTools.deleteFileFromDB(logFileEntities.getValue().get(LogViewHolder.this.getAbsoluteAdapterPosition()).getFileEntity());
                                        viewModel.setLogsList(dataBase.workSpaceDao().getAllLogs());
                                    });
                                }else if (item.getItemId() == R.id.view) {
                                    Bundle bundle = new Bundle();
                                    bundle.putString("name", logFileEntities.getValue().get(LogViewHolder.this.getAbsoluteAdapterPosition()).getFileEntity().getName());
                                    bundle.putString("path", logFileEntities.getValue().get(LogViewHolder.this.getAbsoluteAdapterPosition()).getFileEntity().getAbsolutePath());
                                    bundle.putString("type", "tex");
                                    bundle.putBoolean("openFile", true);
                                    bundle.putBoolean("readOnly", true);
                                    bundle.putLong("owner ws", logFileEntities.getValue().get(LogViewHolder.this.getAbsoluteAdapterPosition()).getFileEntity().getWorkSpaceOwnerId());
                                    Intent intent = new Intent(context, MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtras(bundle);
                                    handler.post(()->{
                                        context.startActivity(intent);
                                    });
                                }
                                return false;
                            }
                        });
                        popupMenus[0].show();
                    }
                });
            }else {
                HeaderItemBinding binding = HeaderItemBinding.bind(itemView);
                name = binding.item;
            }
        }
    }

    public void setInflater(LayoutInflater inflater) {
        this.inflater = inflater;
    }

    public void setLogFileEntities(MutableLiveData<List<ItemsData>> logFileEntities) {
        this.logFileEntities = logFileEntities;
    }

    public void setViewModel(LogsActivityViewModel viewModel) {
        this.viewModel = viewModel;
    }
}
