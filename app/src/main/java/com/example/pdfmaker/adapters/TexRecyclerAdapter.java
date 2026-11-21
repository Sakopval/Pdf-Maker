package com.example.pdfmaker.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AlertDialogLayout;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.example.core.ItemsData;
import com.example.core.MainActivityViewModel;
import com.example.core.db.AppDataBase;
import com.example.core.db.WorkSpaceEntity;
import com.example.pdfmaker.R;
import com.example.pdfmaker.databinding.HeaderItemBinding;
import com.example.pdfmaker.databinding.InputNameBinding;
import com.example.pdfmaker.databinding.TexItemBinding;
import com.example.core.db.FileEntity;
import com.example.core.FileTools;
import com.example.pdfmaker.utils.LatexSettings;
import com.example.pdfmaker.utils.SharedFiles;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.rk.App;
import com.rk.activities.main.MainActivity;

import java.io.File;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;


@Singleton
public class TexRecyclerAdapter extends RecyclerView.Adapter<TexRecyclerAdapter.TexItemHolder> {
    private final int TYPE_HEADER = 1;
    private final int TYPE_ITEM = 2;
    private LayoutInflater inflater;
    private MutableLiveData<List<ItemsData>> list;
    private TexItemBinding binding;
    PopupMenu[] popupMenu;
    private final SharedFiles sharedFiles;
    private final FileTools fileTools;
    private final LatexSettings latexSettings;
    private Context context;
    private LifecycleOwner owner;
    private RelativeLayout loadingLay;
    private final ThreadPoolExecutor poolExecutor;
    private final Handler handler;
    private MainActivityViewModel viewModel;
    private final AppDataBase dataBase;

    @Inject
    public TexRecyclerAdapter(@ApplicationContext Context context, SharedFiles sharedFiles, FileTools fileTools, LatexSettings latexSettings,
                              ThreadPoolExecutor poolExecutor, Handler handler, AppDataBase dataBase) {
        this.context = context;
        popupMenu = new PopupMenu[1];
        this.sharedFiles = sharedFiles;
        this.fileTools = fileTools;
        this.latexSettings = latexSettings;
        this.poolExecutor = poolExecutor;
        this.handler = handler;
        this.dataBase = dataBase;
    }

    public class TexItemHolder extends RecyclerView.ViewHolder {
        private  TexItemBinding binding;
        private  TextView name;
        private  TextView lastModified;
        private  TextView size;
        private  LatexSettings latexSettings;
        public TexItemHolder(@NonNull View itemView, PopupMenu[] popupMenu, SharedFiles sharedFiles, List<ItemsData> fileEntities,
                             LatexSettings latexSettings, LifecycleOwner owner, ThreadPoolExecutor poolExecutor, Handler handler, int type) {
            super(itemView);
            if (type == TYPE_ITEM){
                this.binding = TexItemBinding.bind(itemView);
                this.latexSettings = latexSettings;
                name = binding.item;
                lastModified = binding.lastModified;
                size = binding.size;
                binding.getRoot().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = getAbsoluteAdapterPosition();
                        popupMenu[0] = new PopupMenu(itemView.getContext(), v, Gravity.END);
                        popupMenu[0].inflate(R.menu.tex_item_options_menu);
                        popupMenu[0].setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                if (item.getItemId() == R.id.compile) {
                                    FileEntity file = fileEntities.get(TexRecyclerAdapter.TexItemHolder.this.getAbsoluteAdapterPosition()).getFileEntity();
                                    latexSettings.compileTeX(file.getAbsolutePath(),
                                            loadingLay,file.getWorkSpaceOwnerId());
                                } else if (item.getItemId() == R.id.edit) {
                                    Bundle bundle = new Bundle();
                                    bundle.putString("name", fileEntities.get(TexItemHolder.this.getAbsoluteAdapterPosition()).getFileEntity().getName());
                                    bundle.putString("path", fileEntities.get(TexItemHolder.this.getAbsoluteAdapterPosition()).getFileEntity().getAbsolutePath());
                                    bundle.putString("type", "tex");
                                    bundle.putBoolean("openFile", true);
                                    bundle.putBoolean("readOnly", false);
                                    bundle.putLong("owner ws", fileEntities.get(TexItemHolder.this.getAbsoluteAdapterPosition()).getFileEntity().getWorkSpaceOwnerId());
                                    Intent intent = new Intent(context, MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtras(bundle);
                                    handler.post(()->{
                                        context.startActivity(intent);
                                    });
                                } else if (item.getItemId() == R.id.delete) {
                                    poolExecutor.execute(()->{
                                        fileTools.deleteFileFromDB(fileEntities.get(TexItemHolder.this.getAbsoluteAdapterPosition()).getFileEntity());
                                    });
                                }else {
                                    //show dialogue
                                    InputNameBinding nameBinding = InputNameBinding.inflate(inflater);
                                    TextInputLayout inputLayout = nameBinding.inputLay;
                                    nameBinding.editText.setText(fileEntities.get(pos).getFileEntity().getName().replace(
                                            fileEntities.get(pos).getFileEntity().getName().substring(fileEntities.get(pos).getFileEntity().getName().lastIndexOf(".")), ""));
                                    nameBinding.editText.addTextChangedListener(new TextWatcher() {
                                        @Override
                                        public void afterTextChanged(Editable s) {
                                            if (nameBinding.editText.getText() != null){
                                                if (nameBinding.editText.getText().toString().startsWith(".") || nameBinding.editText.getText().toString().contains(".")){
                                                    nameBinding.inputLay.setErrorEnabled(true);
                                                    nameBinding.inputLay.setError("Please remove the dot");
                                                } else if (s.toString().isBlank()) {
                                                    inputLayout.setErrorEnabled(true);
                                                    inputLayout.setError("Can't create a file with no name");
                                                }
                                            }
                                        }

                                        @Override
                                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                                        }

                                        @Override
                                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                                            nameBinding.inputLay.setErrorEnabled(false);
                                        }
                                    });
                                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(nameBinding.inputLay.getContext());
                                    builder.setView(nameBinding.getRoot());
                                    AlertDialog alertDialog = builder.create();
                                    nameBinding.title.setText("Renaming "+list.getValue().get(pos).getFileEntity().getName());
                                    nameBinding.ok.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            poolExecutor.execute(()->{
                                                if (nameBinding.editText.getText() != null && !nameBinding.editText.getText().toString().isEmpty()){
                                                    String name = nameBinding.editText.getText().toString().strip()+".tex";
                                                    long wsId = fileEntities.get(pos).getFileEntity().getWorkSpaceOwnerId();
                                                    String wsName = dataBase.workSpaceDao().getWorkSpaceById(wsId).getName();
                                                    boolean exists = dataBase.workSpaceDao().FileExists(name, wsId);
                                                    Log.d("RenamingATeX", nameBinding.editText.getText().toString()+" "+exists+"ly exists");
                                                    if (exists){
                                                        handler.post(()->{
                                                            Log.d("RenamingATeX", "setting error");
                                                            inputLayout.setErrorEnabled(true);
                                                            inputLayout.setError("A file with the same name already exists in "+wsName);
                                                            Log.d("RenamingATeX", "set error");
                                                        });
                                                    }else {
                                                        FileEntity file = list.getValue().get(pos).getFileEntity();
                                                        File old = new File(file.getAbsolutePath());
                                                        File newOne = new File(old.getParentFile(), name);
                                                        file.setDate(System.currentTimeMillis());
                                                        file.setAbsolutePath(newOne.getAbsolutePath());
                                                        file.setName(name);
                                                        fileTools.updateFile(file, true, old.getAbsolutePath());
                                                        handler.post(alertDialog::dismiss);
                                                    }
                                                }else {
                                                    handler.post(()->{
                                                        inputLayout.setErrorEnabled(true);
                                                        inputLayout.setError("Can't create a file with no name");
                                                    });
                                                }
                                            });
                                        }
                                    });
                                    nameBinding.cancel.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            alertDialog.dismiss();
                                        }
                                    });
                                    try {
                                        alertDialog.show();
                                    }catch (IllegalStateException e){
                                        Toast.makeText(context, "Pls Wait a little", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                return false;
                            }
                        });
                        popupMenu[0].show();
                    }
                });
            }else {
                name = HeaderItemBinding.bind(itemView).item;
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull TexItemHolder holder, int position) {
        if (list.getValue().get(holder.getAbsoluteAdapterPosition()).getType().equals("header")){
            holder.name.setText(list.getValue().get(holder.getAbsoluteAdapterPosition()).getHeaderName());
        }else {
            holder.name.setText(list.getValue().get(holder.getAbsoluteAdapterPosition()).getFileEntity().getName());
            holder.lastModified.setText(fileTools.getHTime(list.getValue().get(holder.getAbsoluteAdapterPosition()).getFileEntity().getLastModified()));
            holder.size.setText(fileTools.getHSize(list.getValue().get(holder.getAbsoluteAdapterPosition()).getFileEntity().getSize()));
        }
    }

    @NonNull
    @Override
    public TexItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_ITEM){
            view = inflater.inflate(R.layout.tex_item, parent, false);
        } else if (viewType == TYPE_HEADER) {
             view= inflater.inflate(R.layout.header_item, parent, false);
        }else {
             view = inflater.inflate(R.layout.tex_item, parent, false);
        }
        return new TexItemHolder(view, popupMenu, sharedFiles, list.getValue(), latexSettings, owner, poolExecutor, handler, viewType);
    }

    @Override
    public int getItemCount() {
        return list.getValue().size();
    }

    @Override
    public int getItemViewType(int position) {
        if (list.getValue().get(position).getType().equals("header")){
            return TYPE_HEADER;
        } else if (list.getValue().get(position).getType().equals("file")) {
            return TYPE_ITEM;
        }else {
            return super.getItemViewType(position);
        }
    }

    public void setInflater(LayoutInflater inflater) {
        this.inflater = inflater;
    }

    public void setOwner(LifecycleOwner owner) {
        this.owner = owner;
    }

    public void setLoadingLay(RelativeLayout loadingLay) {
        this.loadingLay = loadingLay;
    }

    public void setList(MutableLiveData<List<ItemsData>> list) {
        this.list = list;
    }

    public void setViewModel(MainActivityViewModel viewModel) {
        this.viewModel = viewModel;
    }
}
