package com.example.pdfmaker.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.core.ItemsData;
import com.example.core.MainActivityViewModel;
import com.example.core.db.AppDataBase;
import com.example.pdfmaker.R;
import com.example.core.db.FileEntity;
import com.example.core.FileTools;
import com.example.pdfmaker.databinding.HeaderItemBinding;
import com.example.pdfmaker.databinding.InputNameBinding;
import com.example.pdfmaker.databinding.ResultItemBinding;
import com.example.pdfmaker.utils.LatexSettings;
import com.example.pdfmaker.utils.SharedFiles;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.rk.activities.main.MainActivity;

import java.io.File;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;


@Singleton
public class MatchesRecyclerAdapter extends RecyclerView.Adapter<MatchesRecyclerAdapter.MatchesItemHolder> {
    private final int TYPE_HEADER = 1;
    private final int TYPE_ITEM = 2;
    private LayoutInflater inflater;
    private MutableLiveData<List<ItemsData>> list;
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
    private final AppDataBase database;
    private int itemCount = 0;

    @Inject
    public MatchesRecyclerAdapter(@ApplicationContext Context context, SharedFiles sharedFiles, FileTools fileTools, LatexSettings latexSettings,
                                  ThreadPoolExecutor poolExecutor, Handler handler, AppDataBase dataBase) {
        this.context = context;
        popupMenu = new PopupMenu[1];
        this.sharedFiles = sharedFiles;
        this.fileTools = fileTools;
        this.latexSettings = latexSettings;
        this.poolExecutor = poolExecutor;
        this.handler = handler;
        this.database =dataBase;
    }

    public class MatchesItemHolder extends RecyclerView.ViewHolder {
        private  TextView name;
        private  TextView lastModified;
        private  TextView size;
        private  LatexSettings latexSettings;
        public MatchesItemHolder(@NonNull View itemView, PopupMenu[] popupMenu, SharedFiles sharedFiles, List<ItemsData> fileEntities,
                             LatexSettings latexSettings, LifecycleOwner owner, ThreadPoolExecutor poolExecutor, Handler handler, int type, AppDataBase dataBase) {
            super(itemView);
            if (type == TYPE_ITEM){
                ResultItemBinding binding = ResultItemBinding.bind(itemView);
                this.latexSettings = latexSettings;
                name = binding.item;
                lastModified = binding.lastModified;
                size = binding.size;
                binding.getRoot().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final int pos = getAbsoluteAdapterPosition();
                        final String type = list.getValue().get(pos).getFileEntity().getType();
                        Log.d("ClickinInMatchesList","clicked at pos: "+pos+" with type: "+type);
                        if (type.equals("tex") || type.equals(".tex") ||type.equals("TEX") || type.equals(".TEX")){
                            popupMenu[0] = new PopupMenu(itemView.getContext(), v, Gravity.END);
                            popupMenu[0].inflate(R.menu.tex_item_options_menu);
                            popupMenu[0].setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    if (item.getItemId() == R.id.compile) {
                                        FileEntity file = fileEntities.get(pos).getFileEntity();
                                        latexSettings.compileTeX(file.getAbsolutePath(),
                                                loadingLay,file.getWorkSpaceOwnerId());
                                    } else if (item.getItemId() == R.id.edit) {
                                        Bundle bundle = new Bundle();
                                        bundle.putString("name", fileEntities.get(pos).getFileEntity().getName());
                                        bundle.putString("path", fileEntities.get(pos).getFileEntity().getAbsolutePath());
                                        bundle.putString("type", "tex");
                                        bundle.putBoolean("openFile", true);
                                        bundle.putLong("owner ws", fileEntities.get(pos).getFileEntity().getWorkSpaceOwnerId());
                                        Intent intent = new Intent(context, MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        intent.putExtras(bundle);
                                        handler.post(()->{
                                            context.startActivity(intent);
                                        });
                                    } else if (item.getItemId() == R.id.delete) {
                                        poolExecutor.execute(()->{
                                            fileTools.deleteFileFromDB(fileEntities.get(pos).getFileEntity());
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
                        if (type.equals("pdf") || type.equals(".pdf") ||type.equals("PDF") || type.equals(".PDF")){
                            popupMenu[0] = new PopupMenu(itemView.getContext(), v, Gravity.END);
                            popupMenu[0].inflate(R.menu.pdf_item_menu);
                            popupMenu[0].setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    if (item.getItemId() == R.id.view){
                                        sharedFiles.sharePdf(new File(fileEntities.get(pos).getFileEntity().getAbsolutePath()));
                                    }else if (item.getItemId() == R.id.delete) {
                                        poolExecutor.execute(()->{
                                            fileTools.deleteFileFromDB(fileEntities.get(pos).getFileEntity());
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
                                                if (nameBinding.editText.getText() != null) {
                                                    if (nameBinding.editText.getText().toString().startsWith(".") || nameBinding.editText.getText().toString().contains(".")) {
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
                                        AlertDialog dialog = builder.create();
                                        inputLayout.setSuffixText(".pdf");
                                        nameBinding.title.setText("Renaming " + fileEntities.get(pos).getFileEntity().getName());
                                        nameBinding.ok.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                poolExecutor.execute(() -> {
                                                    if (nameBinding.editText.getText() != null && !nameBinding.editText.getText().toString().isEmpty()) {
                                                        String name = nameBinding.editText.getText().toString().strip() + ".pdf";
                                                        long wsId = fileEntities.get(pos).getFileEntity().getWorkSpaceOwnerId();
                                                        String wsName = dataBase.workSpaceDao().getWorkSpaceById(wsId).getName();
                                                        boolean exists = dataBase.workSpaceDao().FileExists(name, wsId);
                                                        Log.d("RenamingAPDF", nameBinding.editText.getText().toString() + " " + exists + "ly exists");
                                                        if (exists) {
                                                            handler.post(() -> {
                                                                Log.d("RenamingAPDF", "setting error");
                                                                inputLayout.setErrorEnabled(true);
                                                                inputLayout.setError("A file with the same name already exists in " + wsName);
                                                                Log.d("RenamingAPDF", "set error");
                                                            });
                                                        } else {
                                                            FileEntity file = fileEntities.get(pos).getFileEntity();
                                                            File old = new File(file.getAbsolutePath());
                                                            File newOne = new File(old.getParentFile(), name);
                                                            file.setDate(System.currentTimeMillis());
                                                            file.setAbsolutePath(newOne.getAbsolutePath());
                                                            file.setName(name);
                                                            fileTools.updateFile(file, true, old.getAbsolutePath());
                                                            handler.post(dialog::dismiss);
                                                        }
                                                    } else {
                                                        handler.post(() -> {
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
                                                dialog.dismiss();
                                            }
                                        });
                                        try {
                                            dialog.show();
                                        } catch (IllegalStateException e) {
                                            Toast.makeText(dialog.getContext(), "Pls Wait a little", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    return false;
                                }
                            });
                            popupMenu[0].show();
                        }
                        if (type.equals("log") || type.equals(".log") ||type.equals("LOG") || type.equals(".LOG")){
                            popupMenu[0] = new PopupMenu(itemView.getContext(), v, Gravity.END);
                            popupMenu[0].inflate(R.menu.log_item_options);
                            popupMenu[0].setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    if (item.getItemId() == R.id.delete) {
                                        poolExecutor.execute(()->{
                                            fileTools.deleteFileFromDB(list.getValue().get(pos).getFileEntity());
                                        });
                                    }else if (item.getItemId() == R.id.view) {
                                        Bundle bundle = new Bundle();
                                        bundle.putString("name", list.getValue().get(pos).getFileEntity().getName());
                                        bundle.putString("path", list.getValue().get(pos).getFileEntity().getAbsolutePath());
                                        bundle.putString("type", "tex");
                                        bundle.putBoolean("openFile", true);
                                        bundle.putBoolean("readOnly", true);
                                        bundle.putLong("owner ws", list.getValue().get(pos).getFileEntity().getWorkSpaceOwnerId());
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
                            popupMenu[0].show();
                        }
                    }
                });
            }else {
                HeaderItemBinding headerItemBinding = HeaderItemBinding.bind(itemView);
                this.name = headerItemBinding.item;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (list.getValue().get(position).getType().equals("header")) {
            return TYPE_HEADER;
        } else if (list.getValue().get(position).getType().equals("file")) {
            return TYPE_ITEM;
        } else {
            return super.getItemViewType(position);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MatchesItemHolder holder, int position) {
        if (list.getValue().get(holder.getAbsoluteAdapterPosition()).getType().equals("header")) {
            holder.name.setText(list.getValue().get(holder.getAbsoluteAdapterPosition()).getHeaderName());
        } else {
            holder.name.setText(list.getValue().get(holder.getAbsoluteAdapterPosition()).getFileEntity().getName());
            holder.lastModified.setText(fileTools.getHTime(list.getValue().get(holder.getAbsoluteAdapterPosition()).getFileEntity().getLastModified()));
            holder.size.setText(fileTools.getHSize(list.getValue().get(holder.getAbsoluteAdapterPosition()).getFileEntity().getSize()));
        }
    }

    @NonNull
    @Override
    public MatchesItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_ITEM) {
            view = inflater.inflate(R.layout.result_item, parent, false);
        } else if (viewType == TYPE_HEADER) {
            view = inflater.inflate(R.layout.header_item, parent, false);
        } else {
            view = inflater.inflate(R.layout.result_item, parent, false);
        }
        return new MatchesItemHolder(view, popupMenu, sharedFiles, list.getValue(), latexSettings, owner, poolExecutor, handler, viewType, database);
    }

    @Override
    public int getItemCount() {
        return list.getValue().size();
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

    public MutableLiveData<List<ItemsData>> getList() {
        return list;
    }

    public void setViewModel(MainActivityViewModel viewModel) {
        this.viewModel = viewModel;
    }
}
