package com.example.pdfmaker.adapters;

import android.content.Context;
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
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.example.core.db.AppDataBase;
import com.example.core.db.FileEntity;
import com.example.pdfmaker.R;
import com.example.pdfmaker.databinding.InputNameBinding;
import com.example.pdfmaker.databinding.WorkspaceItemBinding;
import com.example.core.db.WorkSpaceEntity;
import com.example.core.FileTools;
import com.example.pdfmaker.viewmodels.AllWorkSpacesActivityViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.rk.activities.main.MainActivity;

import java.io.File;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class WorkSpaceRecyclerAdapter extends RecyclerView.Adapter<WorkSpaceRecyclerAdapter.WorkSpaceViewHolder> {
    private LayoutInflater inflater;
    private MutableLiveData<List<WorkSpaceEntity>> workSpaceEntities;
    private WorkspaceItemBinding binding;
    private final FileTools fileTools;
    private final PopupMenu[] popupMenus;
    private final ThreadPoolExecutor poolExecutor;
    private final Handler handler;
    private AllWorkSpacesActivityViewModel viewModel;
    private final AppDataBase dataBase;
    private final Context context;

    @Inject
    public WorkSpaceRecyclerAdapter(@ApplicationContext Context context, FileTools fileTools, ThreadPoolExecutor poolExecutor,
                                    Handler handler, AppDataBase dataBase) {
        this.fileTools = fileTools;
        popupMenus = new PopupMenu[1];
        this.poolExecutor = poolExecutor;
        this.handler = handler;
        this.dataBase = dataBase;
        this.context = context;
    }

    @NonNull
    @Override
    public WorkSpaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.workspace_item, parent, false);
        binding = WorkspaceItemBinding.bind(view);
        return new WorkSpaceViewHolder(binding.getRoot(), context);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkSpaceViewHolder holder, int position) {
        holder.name.setText(workSpaceEntities.getValue().get(holder.getAbsoluteAdapterPosition()).getName());
        holder.filesNumber.setText(String.valueOf(workSpaceEntities.getValue().get(holder.getAbsoluteAdapterPosition()).getFilesNumber()));
        holder.lastModified.setText(fileTools.getHTime(workSpaceEntities.getValue().get(holder.getAbsoluteAdapterPosition()).getLastModified()));
        AtomicReference<String> total = new AtomicReference<>();
        poolExecutor.execute(()->{
            total.set(fileTools.getSizeOfWorkSpace(workSpaceEntities.getValue().get(holder.getAbsoluteAdapterPosition())));
            handler.post(()->holder.size.setText(total.get()));
        });

    }

    @Override
    public int getItemCount() {
        return workSpaceEntities.getValue().size();
    }

    public class WorkSpaceViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView lastModified;
        private final  TextView filesNumber;
        private final TextView size;
        private final WorkspaceItemBinding binding;

        public WorkSpaceViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            binding = WorkspaceItemBinding.bind(itemView);
            name = binding.item;
            lastModified = binding.lastModified;
            filesNumber = binding.filesnumber;
            size = binding.size;
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAbsoluteAdapterPosition();
                    popupMenus[0] = new PopupMenu(itemView.getContext(), v, Gravity.END);
                    popupMenus[0].inflate(R.menu.workspace_options_menu);
                    popupMenus[0].setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (item.getItemId() == R.id.delete) {
                                poolExecutor.execute(()->{
                                    fileTools.deleteWorkSpace(workSpaceEntities.getValue().get(WorkSpaceViewHolder.this.getAbsoluteAdapterPosition()));
                                    viewModel.setWorkSpaces(dataBase.workSpaceDao().getAllWorkSpaces());
                                });
                            } else if (item.getItemId() == R.id.rename) {
                                //show dialogue
                                InputNameBinding nameBinding = InputNameBinding.inflate(inflater);
                                TextInputLayout inputLayout = nameBinding.inputLay;
                                nameBinding.editText.setText(workSpaceEntities.getValue().get(pos).getName());
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
                                AlertDialog dialog = builder.create();
                                inputLayout.setSuffixText(null);
                                nameBinding.title.setText("Renaming "+workSpaceEntities.getValue().get(pos).getName());
                                nameBinding.ok.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        poolExecutor.execute(()->{
                                            if (nameBinding.editText.getText() != null && !nameBinding.editText.getText().toString().isEmpty()){
                                                String name = nameBinding.editText.getText().toString().strip();
                                                boolean exists = dataBase.workSpaceDao().workSpaceExists(name);
                                                Log.d("RenamingAWorkSpace", nameBinding.editText.getText().toString()+" "+exists+"ly exists");
                                                if (exists){
                                                    handler.post(()->{
                                                        Log.d("RenamingAWorkSpace", "setting error");
                                                        inputLayout.setErrorEnabled(true);
                                                        inputLayout.setError("A file with the same name already exists");
                                                        Log.d("RenamingAWorkSpace", "set error");
                                                    });
                                                }else {
                                                    WorkSpaceEntity workSpace = workSpaceEntities.getValue().get(pos);
                                                    File old = new File(workSpace.getAbsolutePath());
                                                    Log.d("RenamingAWorkSpace", "before is"+old.getAbsolutePath());
                                                    File newOne = new File(old.getParentFile(), name);
                                                    Log.d("RenamingAWorkSpace", "newOne is"+newOne.getAbsolutePath());
                                                    workSpace.setAbsolutePath(newOne.getAbsolutePath());
                                                    workSpace.setName(name);
                                                    fileTools.updateFile(workSpace, true, old.getAbsolutePath());
                                                    handler.post(dialog::dismiss);
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
                                        dialog.dismiss();
                                    }
                                });
                                try {
                                    dialog.show();
                                }catch (IllegalStateException e){
                                    Toast.makeText(dialog.getContext(), "Pls Wait a little", Toast.LENGTH_SHORT).show();
                                }
                            } else if (item.getItemId() == R.id.view) {
                                Bundle bundle = new Bundle();
                                bundle.putString("name", workSpaceEntities.getValue().get(getAbsoluteAdapterPosition()).getName());
                                bundle.putString("path", workSpaceEntities.getValue().get(getAbsoluteAdapterPosition()).getAbsolutePath());
                                bundle.putString("type", "Dir");
                                bundle.putBoolean("openTree", true);
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
        }
    }

    public void setInflater(LayoutInflater inflater) {
        this.inflater = inflater;
    }

    public void setWorkSpaceEntities(MutableLiveData<List<WorkSpaceEntity>> workSpaceEntities) {
        this.workSpaceEntities = workSpaceEntities;
    }

    public void setViewModel(AllWorkSpacesActivityViewModel viewModel) {
        this.viewModel = viewModel;
    }
}
