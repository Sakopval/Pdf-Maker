package com.example.pdfmaker;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.core.MainActivityViewModel;
import com.example.pdfmaker.adapters.WorkSpaceRecyclerAdapter;
import com.example.pdfmaker.databinding.WorkspaceActivityBinding;
import com.example.core.db.AppDataBase;
import com.example.core.db.WorkSpaceEntity;
import com.example.core.FileTools;
import com.example.pdfmaker.viewmodels.AllWorkSpacesActivityViewModel;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AllWorkSpacesActivity extends AppCompatActivity {
    @Inject
    ThreadPoolExecutor poolExecutor;
    @Inject
    AppDataBase dataBase;
    @Inject
    Handler handler;
    @Inject
    FileTools fileTools;
    @Inject WorkSpaceRecyclerAdapter adapter;
    private AllWorkSpacesActivityViewModel viewModel;
    WorkspaceActivityBinding binding;
    private RecyclerView workSpaceRecycler;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = WorkspaceActivityBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(this).get(AllWorkSpacesActivityViewModel.class);
        adapter.setViewModel(viewModel);
        setContentView(binding.workspacesActivity);
        adapter.setInflater(getLayoutInflater());
        adapter.setWorkSpaceEntities(viewModel.getWorkSpaces());
        poolExecutor.execute(()->{
            viewModel.setWorkSpaces(dataBase.workSpaceDao().getAllWorkSpaces());
        });
        workSpaceRecycler = binding.workspaces;
        workSpaceRecycler.setLayoutManager(new LinearLayoutManager(this));
        viewModel.getWorkSpaces().observe(this, new Observer<List<WorkSpaceEntity>>() {
            @Override
            public void onChanged(List<WorkSpaceEntity> workSpaceEntities) {
                poolExecutor.execute(()->{
                    if (!workSpaceEntities.isEmpty()){
                        //if not empty
                        handler.post(()->{
                            workSpaceRecycler.setVisibility(View.VISIBLE);
                        });
                    }else {
                        //empty
                        handler.post(()->{
                            binding.noWspLay.setVisibility(View.VISIBLE);
                            binding.loadingLay.setVisibility(View.GONE);
                        });
                    }
                    handler.post(()->{
                        workSpaceRecycler.setAdapter(adapter);
                        binding.loadingLay.setVisibility(View.GONE);
                    });
                });
            }
        });

        binding.toolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        fileTools.getViewModel().getWorkSpaces().observe(this, (workSpaceEntities)->{
            viewModel.setWorkSpaces(workSpaceEntities);
        });
    }
}
