package com.example.pdfmaker;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pdfmaker.adapters.LogsRecyclerAdapter;
import com.example.pdfmaker.databinding.LogsActivtyBinding;
import com.example.core.db.AppDataBase;
import com.example.core.db.FileEntity;
import com.example.core.FileTools;
import com.example.pdfmaker.viewmodels.LogsActivityViewModel;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LogsActivity extends AppCompatActivity {

    @Inject
    ThreadPoolExecutor poolExecutor;
    @Inject
    AppDataBase dataBase;
    @Inject
    Handler handler;
    @Inject
    FileTools fileTools;
    @Inject LogsRecyclerAdapter adapter;
    LogsActivityViewModel viewModel;
    LogsActivtyBinding binding;
    private RecyclerView logsRecycler;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = LogsActivtyBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(this).get(LogsActivityViewModel.class);
        adapter.setViewModel(viewModel);
        setContentView(binding.workspacesActivity);
        adapter.setInflater(getLayoutInflater());
        poolExecutor.execute(()->{
            adapter.setLogFileEntities(new MutableLiveData<>(dataBase.workSpaceDao().getItemsData("log", null)));
        });
        poolExecutor.execute(()->{
            viewModel.setLogsList(dataBase.workSpaceDao().getAllLogs());
        });
        logsRecycler = binding.logsRecycler;
        logsRecycler.setLayoutManager(new LinearLayoutManager(this));
        viewModel.getLogsList().observe(this, new Observer<List<FileEntity>>() {
            @Override
            public void onChanged(List<FileEntity> fileEntities) {
                poolExecutor.execute(()->{
                    if (!fileEntities.isEmpty()){
                        //if not empty
                        handler.post(()->{
                            logsRecycler.setVisibility(View.VISIBLE);
                        });
                    }else {
                        //empty
                        handler.post(()->{
                            binding.noLogsLay.setVisibility(View.VISIBLE);
                            binding.loadingLay.setVisibility(View.GONE);
                        });
                    }
                    handler.post(()->{
                        logsRecycler.setAdapter(adapter);
                        binding.loadingLay.setVisibility(View.GONE);
                    });
                });
            }
        });
        fileTools.getViewModel().getLogsList().observe(this, new Observer<List<FileEntity>>() {
            @Override
            public void onChanged(List<FileEntity> fileEntities) {
                poolExecutor.execute(()->{
                    adapter.setLogFileEntities(new MutableLiveData<>(dataBase.workSpaceDao().getItemsData("log", null)));
                });
            }
        });

        binding.toolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
