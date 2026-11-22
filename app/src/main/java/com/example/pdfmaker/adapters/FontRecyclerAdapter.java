package com.example.pdfmaker.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.core.MainActivityViewModel;
import com.example.core.db.AppDataBase;
import com.example.core.db.FontEntity;
import com.example.core.db.PkgEntity;
import com.example.pdfmaker.R;
import com.example.core.FileTools;
import com.example.pdfmaker.databinding.FontActivityBinding;
import com.example.pdfmaker.databinding.FontPreviewListBinding;
import com.example.pdfmaker.databinding.ResultItemBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import dagger.hilt.android.qualifiers.ApplicationContext;


public class FontRecyclerAdapter extends RecyclerView.Adapter<FontRecyclerAdapter.FontViewHolder> {

    private final int TYPE_HEADER = 1;
    private final int TYPE_ITEM = 2;
    private LayoutInflater inflater;
    private MutableLiveData<List<FontEntity>> fontEntities;
    private final FileTools fileTools;
    private final PopupMenu[] popupMenus;
    private final ThreadPoolExecutor poolExecutor;
    private final Handler handler;
    private final Context context;
    private MainActivityViewModel viewModel;
    private final AppDataBase dataBase;


    public FontRecyclerAdapter(@ApplicationContext Context context, FileTools fileTools, ThreadPoolExecutor poolExecutor,
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
    public FontViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.result_item, parent, false);
        return new FontViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull FontViewHolder holder, int position) {
        holder.name.setText(fontEntities.getValue().get(holder.getAbsoluteAdapterPosition()).getName());
        holder.lastModified.setText(fileTools.getHTime(fontEntities.getValue().get(holder.getAbsoluteAdapterPosition()).getDateAdded()));
        holder.size.setText(fileTools.getHSize(fontEntities.getValue().get(holder.getAbsoluteAdapterPosition()).getSize()));
    }

    @Override
    public int getItemCount() {
        return fontEntities.getValue().size();
    }

    public class FontViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private TextView lastModified;
        private TextView size;
        private ResultItemBinding binding;

        public FontViewHolder(@NonNull View itemView, Context context) {
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
                                    fileTools.deleteFont(fontEntities.getValue().get(pos));
                                    viewModel.getFonts().postValue(dataBase.workSpaceDao().getAllFonts());
                                });
                            } else if (item.getItemId() == R.id.view) {
                                FontPreviewListBinding listBinding = FontPreviewListBinding.inflate(inflater);
                                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(listBinding.fontList.getContext());
                                AlertDialog dialog = builder.create();
                                dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Ok", (d,w)->{});
                                dialog.setView(listBinding.getRoot());
                                dialog.setTitle("Fonts");
                                File fontDir = new File(fontEntities.getValue().get(pos).getPath());
                                if (fontDir.listFiles() == null || fontDir.listFiles().length == 0){
                                    Toast.makeText(context, "empty font folder", Toast.LENGTH_SHORT).show();
                                    return false;
                                }
                                if (!fontDir.exists()){
                                    Toast.makeText(context, "Doesn't exist", Toast.LENGTH_SHORT).show();
                                    return false;
                                }
                                poolExecutor.execute(()->{
                                    HashMap<String, Typeface> fonts = new HashMap<>();
                                    for (File f : fontDir.listFiles()){
                                        if (!f.exists()){
                                            continue;
                                        }
                                        fonts.put(f.getName(), Typeface.createFromFile(f.getAbsolutePath()));
                                    }
                                    handler.post(()->{
                                        FontPreviewAdapter previewAdapter = new FontPreviewAdapter(fonts);
                                        listBinding.fontList.setLayoutManager(new LinearLayoutManager(context));
                                        listBinding.fontList.setAdapter(previewAdapter);
                                        dialog.show();
                                    });
                                });
                                dialog.show();
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

    public void setFontEntities(MutableLiveData<List<FontEntity>> fontEntities) {
        this.fontEntities = fontEntities;
    }

    public void setViewModel(MainActivityViewModel viewModel) {
        this.viewModel = viewModel;
    }
}
