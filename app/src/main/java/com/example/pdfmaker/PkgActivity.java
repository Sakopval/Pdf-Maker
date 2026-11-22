package com.example.pdfmaker;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.core.FileTools;
import com.example.core.ItemsData;
import com.example.core.db.AppDataBase;
import com.example.core.db.FileEntity;
import com.example.core.db.PkgEntity;
import com.example.pdfmaker.adapters.PkgRecyclerAdapter;
import com.example.pdfmaker.databinding.PkgActivityBinding;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;


@AndroidEntryPoint
public class PkgActivity extends AppCompatActivity {
    @Inject
    AppDataBase dataBase;
    PkgRecyclerAdapter adapter;
    PkgRecyclerAdapter searchAdapter;
    @Inject
    FileTools fileTools;
    @Inject
    ThreadPoolExecutor poolExecutor;
    @Inject
    Handler handler;
    PkgActivityBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new PkgRecyclerAdapter(this, fileTools, poolExecutor, handler, dataBase);
        searchAdapter = new PkgRecyclerAdapter(this, fileTools, poolExecutor, handler, dataBase);
        adapter.setInflater(getLayoutInflater());
        adapter.setViewModel(fileTools.getViewModel());
        searchAdapter.setInflater(getLayoutInflater());
        searchAdapter.setViewModel(fileTools.getViewModel());
        poolExecutor.execute(()->{
            MutableLiveData<List<PkgEntity>> pkgs = new MutableLiveData<>(dataBase.workSpaceDao().getAllPkgs());
            adapter.setPkgEntities(pkgs);
            searchAdapter.setPkgEntities(pkgs);
        });
        binding = PkgActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.pkgsRecycler.setLayoutManager(new LinearLayoutManager(this));
        fileTools.getViewModel().getPkgs().observe(this, new Observer<List<PkgEntity>>() {
            @Override
            public void onChanged(List<PkgEntity> pkgEntities) {
                poolExecutor.execute(()->{
                    if (!pkgEntities.isEmpty()){
                        //if not empty
                        handler.post(()->{
                            binding.pkgsRecycler.setVisibility(View.VISIBLE);
                        });
                    }else {
                        //empty
                        handler.post(()->{
                            binding.noPkgsLay.setVisibility(View.VISIBLE);
                            binding.pkgsRecycler.setVisibility(View.GONE);
                            binding.loadingLay.setVisibility(View.GONE);
                        });
                    }
                    handler.post(()->{
                        binding.pkgsRecycler.setAdapter(adapter);
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

        poolExecutor.execute(()->{
            List<PkgEntity> pkgEntities = dataBase.workSpaceDao().getAllPkgs();
            searchAdapter.setPkgEntities(new MutableLiveData<>(pkgEntities));
            binding.matchesList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
            handler.post(()->{
                binding.matchesList.setAdapter(searchAdapter);
            });
        });

        Thread search = new Thread(()->{
            List<PkgEntity> found = new LinkedList<>();
            for (PkgEntity file : dataBase.workSpaceDao().getAllPkgs()) {
                if (file.getName().toLowerCase().strip().contains(binding.srchView.getEditText().getText().toString().strip().toLowerCase())) {
                    found.add(file);
                }
            }
            if (found.isEmpty()) {
                handler.post(()->{
                    binding.noMatches.setVisibility(View.VISIBLE);
                    binding.matchesList.setVisibility(View.GONE);
                });
            } else {
                handler.post(()->{
                    binding.noMatches.setVisibility(View.GONE);
                    binding.matchesList.setVisibility(View.VISIBLE);
                    searchAdapter.setPkgEntities(new MutableLiveData<>(found));
                    binding.matchesList.setAdapter(searchAdapter);
                });
            }
        });
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

        fileTools.getViewModel().getPkgs().observe(this, new Observer<List<PkgEntity>>() {
            @Override
            public void onChanged(List<PkgEntity> pkgEntities) {
                adapter.setPkgEntities(new MutableLiveData<>(pkgEntities));
                service.schedule(search, 300, TimeUnit.MILLISECONDS);
            }
        });

        binding.srchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                service.schedule(search, 300, TimeUnit.MILLISECONDS);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }
}
