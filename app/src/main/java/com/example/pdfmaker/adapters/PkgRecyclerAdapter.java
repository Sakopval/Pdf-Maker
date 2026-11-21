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
import com.example.core.MainActivityViewModel;
import com.example.core.db.AppDataBase;
import com.example.core.db.PkgEntity;
import com.example.pdfmaker.R;
import com.example.pdfmaker.databinding.HeaderItemBinding;
import com.example.pdfmaker.databinding.LogItemBinding;
import com.example.core.FileTools;
import com.example.pdfmaker.databinding.ResultItemBinding;
import com.example.pdfmaker.viewmodels.LogsActivityViewModel;
import com.rk.activities.main.MainActivity;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.multibindings.Multibinds;


public class PkgRecyclerAdapter extends RecyclerView.Adapter<PkgRecyclerAdapter.PkgViewHolder> {

    private final int TYPE_HEADER = 1;
    private final int TYPE_ITEM = 2;
    private LayoutInflater inflater;
    private MutableLiveData<List<PkgEntity>> pkgEntities;
    private final FileTools fileTools;
    private final PopupMenu[] popupMenus;
    private final ThreadPoolExecutor poolExecutor;
    private final Handler handler;
    private final Context context;
    private MainActivityViewModel viewModel;
    private final AppDataBase dataBase;


    public PkgRecyclerAdapter(@ApplicationContext Context context, FileTools fileTools, ThreadPoolExecutor poolExecutor,
                              Handler handler, AppDataBase dataBase) {
        this.fileTools = fileTools;
        popupMenus = new PopupMenu[1];
        this.poolExecutor = poolExecutor;
        this.handler = handler;
        this.context = context;
        this.dataBase = dataBase;
    }


    @NonNull
    @Override
    public PkgViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.result_item, parent, false);
        return new PkgViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull PkgViewHolder holder, int position) {
        holder.name.setText(pkgEntities.getValue().get(holder.getAbsoluteAdapterPosition()).getName());
        holder.lastModified.setText(fileTools.getHTime(pkgEntities.getValue().get(holder.getAbsoluteAdapterPosition()).getDateAdded()));
        holder.size.setText(fileTools.getHSize(pkgEntities.getValue().get(holder.getAbsoluteAdapterPosition()).getSize()));
    }

    @Override
    public int getItemCount() {
        return pkgEntities.getValue().size();
    }

    public class PkgViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private TextView lastModified;
        private TextView size;
        private ResultItemBinding binding;

        public PkgViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            binding = ResultItemBinding.bind(itemView);
            name = binding.item;
            lastModified = binding.lastModified;
            size = binding.size;
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAbsoluteAdapterPosition();
                    popupMenus[0] = new PopupMenu(itemView.getContext(), v, Gravity.END);
                    popupMenus[0].inflate(R.menu.log_item_options);
                    popupMenus[0].setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (item.getItemId() == R.id.delete) {
                                poolExecutor.execute(() -> {
                                    fileTools.deletePkg(pkgEntities.getValue().get(pos));
                                    viewModel.getPkgs().postValue(dataBase.workSpaceDao().getAllPkgs());
                                });
                            }
                            return false;
                        }
                    });
                    popupMenus[0].show();
                }
            });
        }
    }

    public void setInflater(LayoutInflater inflater) {
        this.inflater = inflater;
    }

    public void setPkgEntities(MutableLiveData<List<PkgEntity>> pkgEntities) {
        this.pkgEntities = pkgEntities;
    }

    public void setViewModel(MainActivityViewModel viewModel) {
        this.viewModel = viewModel;
    }
}
