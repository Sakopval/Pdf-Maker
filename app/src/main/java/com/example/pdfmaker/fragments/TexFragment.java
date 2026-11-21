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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.core.ItemsData;
import com.example.pdfmaker.adapters.TexRecyclerAdapter;
import com.example.pdfmaker.databinding.TexFragmentLayBinding;
import com.example.core.db.AppDataBase;
import com.example.core.db.FileEntity;
import com.example.core.db.WorkSpaceDao;
import com.example.core.FileTools;
import com.example.pdfmaker.utils.LatexSettings;
import com.example.pdfmaker.utils.SharedFiles;
import com.example.core.MainActivityViewModel;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TexFragment extends Fragment {
    @Inject
    FileTools fileTools;
    @Inject
    AppDataBase dataBase;
    @Inject
    ThreadPoolExecutor poolExecutor;
    @Inject
    Handler handler;
    @Inject
    SharedFiles sharedFiles;
    @Inject
    TexRecyclerAdapter adapter;
    @Inject
    LatexSettings latexSettings;
    private WorkSpaceDao workSpaceDao;
    MainActivityViewModel viewModel;
    private TexFragmentLayBinding binding;

    public TexFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainActivityViewModel.class);
        poolExecutor.execute(()->{
            adapter.setList(new MutableLiveData<>(dataBase.workSpaceDao().getItemsData("tex", null)));
        });
        adapter.setOwner(requireActivity());
        adapter.setViewModel(viewModel);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = TexFragmentLayBinding.inflate(inflater);
        adapter.setInflater(inflater);
        adapter.setLoadingLay(binding.loadingLay);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel.getTexList().observe(getViewLifecycleOwner(), new Observer<List<FileEntity>>() {
            @Override
            public void onChanged(List<FileEntity> texList) {
                if (!texList.isEmpty()) {
                    //if not empty
                    if (workSpaceDao == null){workSpaceDao = dataBase.workSpaceDao();}
                    Log.d("fromMutable","value is ", new Throwable(texList.isEmpty()+""));
                    poolExecutor.execute(()->{
                        handler.post(()->{
                            binding.loadingLay.setVisibility(View.GONE);
                            binding.noDocLayTex.setVisibility(View.GONE);
                            binding.texList.setVisibility(View.VISIBLE);
                        });
                        List<ItemsData> fileEntities = dataBase.workSpaceDao().getItemsData("tex", null);
                        adapter.setList(new MutableLiveData<>(fileEntities));
                        Log.d("filesInDB", "are", new Throwable(fileEntities.toString()));
                        handler.post(()->{
                            binding.texList.setLayoutManager(new LinearLayoutManager(getContext()));
                            binding.texList.setAdapter(adapter);
                        });
                    });
                }else {
                    //if empty
                    binding.loadingLay.setVisibility(View.GONE);
                    binding.texList.setVisibility(View.GONE);
                    binding.noDocLayTex.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}
