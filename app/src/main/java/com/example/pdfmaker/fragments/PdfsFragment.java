package com.example.pdfmaker.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.core.ItemsData;
import com.example.pdfmaker.adapters.PdfsRecyclerAdapter;
import com.example.pdfmaker.databinding.PdfsFragmentLayBinding;
import com.example.core.db.AppDataBase;
import com.example.core.db.FileEntity;
import com.example.core.db.WorkSpaceDao;
import com.example.core.FileTools;
import com.example.pdfmaker.utils.SharedFiles;
import com.example.core.MainActivityViewModel;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PdfsFragment extends Fragment {
    @Inject AppDataBase dataBase;
    @Inject ThreadPoolExecutor poolExecutor;
    @Inject Handler handler;
    @Inject SharedFiles sharedFiles;
    @Inject FileTools fileTools;
    @Inject PdfsRecyclerAdapter adapter;
    private WorkSpaceDao workSpaceDao;
    MainActivityViewModel viewModel;
    private PdfsFragmentLayBinding binding;
    public PdfsFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainActivityViewModel.class);
        poolExecutor.execute(()->{
            adapter.setList(new MutableLiveData<>(dataBase.workSpaceDao().getItemsData("pdf", null)));
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = PdfsFragmentLayBinding.inflate(inflater);
        adapter.setInflater(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel.getPdfsList().observe(getViewLifecycleOwner(), new Observer<List<FileEntity>>() {
            @Override
            public void onChanged(List<FileEntity> pdfs) {
                if (!pdfs.isEmpty()) {
                    //if not empty
                    if (workSpaceDao == null){workSpaceDao = dataBase.workSpaceDao();}
                    Log.d("fromMutable","value is ", new Throwable(pdfs.isEmpty()+""));
                    poolExecutor.execute(()->{
                        handler.post(()->{
                            binding.loadingLay.setVisibility(View.GONE);
                            binding.noDocLay.setVisibility(View.GONE);
                            binding.pdfsList.setVisibility(View.VISIBLE);
                        });
                        adapter.setList(new MutableLiveData<List<ItemsData>>(dataBase.workSpaceDao().getItemsData("pdf", null)));
                        Log.d("filesInDB", "are", new Throwable(pdfs.toString()));
                        handler.post(()->{
                            binding.pdfsList.setLayoutManager(new LinearLayoutManager(getContext()));
                            binding.pdfsList.setAdapter(adapter);
                        });
                    });
                }else {
                    //if empty
                    binding.loadingLay.setVisibility(View.GONE);
                    binding.pdfsList.setVisibility(View.GONE);
                    binding.noDocLay.setVisibility(View.VISIBLE);
                }
            }
        });
    }

}
