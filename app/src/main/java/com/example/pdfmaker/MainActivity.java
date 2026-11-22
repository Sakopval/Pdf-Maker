package com.example.pdfmaker;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.ThemeUtils;
import androidx.appcompat.widget.Toolbar;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.core.FileTools;
import com.example.core.ItemsData;
import com.example.core.db.FileEntity;
import com.example.core.db.WorkSpaceEntity;
import com.example.pdfmaker.adapters.AutoCompleteAdapter;
import com.example.pdfmaker.adapters.MatchesRecyclerAdapter;
import com.example.pdfmaker.adapters.ViewPagerAdapter;
import com.example.pdfmaker.databinding.ActivityMainBinding;
import com.example.core.db.AppDataBase;
import com.example.pdfmaker.databinding.NewFileDialogBinding;
import com.example.pdfmaker.fragments.PdfsFragment;
import com.example.pdfmaker.fragments.TexFragment;
import com.example.pdfmaker.utils.ImportWorkSpace;
import com.example.pdfmaker.utils.LatexSettings;
import com.example.core.MainActivityViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;


@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding activityMainBinding;
    private ViewPager2 pager;
    private TabLayout tabLayout;
    @Inject
    FileTools fileTools;
    @Inject
    ImportWorkSpace importWorkSpace;
    @Inject
    LatexSettings latexSettings;
    @Inject
    AppDataBase dataBase;
    @Inject
    MatchesRecyclerAdapter matchesRecyclerAdapter;
    @Inject
    AutoCompleteAdapter autoCompleteAdapter;
    MainActivityViewModel viewModel;
    private ActivityResultLauncher<Uri> launcher;
    @Inject
    ThreadPoolExecutor poolExecutor;
    @Inject
    Handler handler;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        launcher = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), (uri) -> {
                    if (uri != null) {
                        importWorkSpace.importWorkSpace(this, DocumentFile.fromTreeUri(getApplicationContext(), uri), viewModel, activityMainBinding.loadingLay);
                    }
                });
        setContentView(activityMainBinding.main);
        if (!latexSettings.texmfExists()) {
            latexSettings.setLaTeXDirectoryTree(activityMainBinding.loading, activityMainBinding.stateText, activityMainBinding.loadingLay);
        }
        latexSettings.setOwner(this);
        viewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        matchesRecyclerAdapter.setInflater(getLayoutInflater());
        matchesRecyclerAdapter.setOwner(this);
        autoCompleteAdapter.setOwner(this);
        matchesRecyclerAdapter.setLoadingLay(activityMainBinding.loadingLay);
        matchesRecyclerAdapter.setViewModel(viewModel);
        latexSettings.setMainActivityViewModel(viewModel);
        fileTools.setViewModel(viewModel);
        poolExecutor.execute(() -> {
            viewModel.setWorkSpaces(dataBase.workSpaceDao().getAllWorkSpaces());
            viewModel.setTexList(dataBase.workSpaceDao().getAllTex());
            viewModel.setPdfsList(dataBase.workSpaceDao().getAllPdfs());
            viewModel.setLogsList(dataBase.workSpaceDao().getAllLogs());
            viewModel.getPkgs().postValue(dataBase.workSpaceDao().getAllPkgs());
            viewModel.getFonts().postValue(dataBase.workSpaceDao().getAllFonts());
            viewModel.getAllFiles().postValue(dataBase.workSpaceDao().getOnlyImportant());
            autoCompleteAdapter.setList(viewModel.getWorkSpaces());
        });
        poolExecutor.execute(()->{
            File jsonFile = new File(getFilesDir(), "settings");
            String s;
            if (( s = fileTools.getJsonString(jsonFile, "kpathsea_depug")) != null){
                if (!s.isEmpty()){
                    handler.post(()->{
                        if (s.equals("true")){
                            viewModel.getKpathsea_depug().setValue(true);
                        }else {
                            viewModel.getKpathsea_depug().setValue(false);
                        }
                    });
                    Log.d("Settingkpathsea_debugInViewModel", ""+viewModel.getKpathsea_depug().getValue());
                }
            }
        });
        pager = activityMainBinding.viewPager;
        tabLayout = activityMainBinding.tabLayout;
        activityMainBinding.srchView.hide();
        activityMainBinding.srchView.handleBackInvoked();
        activityMainBinding.srchView.setupWithSearchBar(activityMainBinding.srchBar);
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        adapter.addFragment(new PdfsFragment(), getResources().getString(R.string.tab_1));
        adapter.addFragment(new TexFragment(), getResources().getString(R.string.tab_2));
        pager.setAdapter(adapter);
        new TabLayoutMediator(tabLayout, pager, ((tab, pos) -> {
            tab.setText(adapter.getFragmentTitle(pos));
            tab.setText(adapter.getFragmentTitle(pos));
            pager.setPageTransformer(new ViewPager2.PageTransformer() {
                @Override
                public void transformPage(@NonNull View page, float position) {

                }
            });
        })).attach();


        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, activityMainBinding.drawerLay, activityMainBinding.toolBar, 0, 0);
        activityMainBinding.drawerLay.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        activityMainBinding.navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.settings) {
                    startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                } else if (menuItem.getItemId() == R.id.workspaces) {
                    startActivity(new Intent(getApplicationContext(), AllWorkSpacesActivity.class));
                } else if (menuItem.getItemId() == R.id.logs) {
                    startActivity(new Intent(getApplicationContext(), LogsActivity.class));
                } else if (menuItem.getItemId() == R.id.txt_editor) {
                    startActivity(new Intent(getApplicationContext(), com.rk.activities.main.MainActivity.class));
                } else if (menuItem.getItemId() == R.id.pkgs) {
                    startActivity(new Intent(getApplicationContext(), PkgActivity.class));
                } else if (menuItem.getItemId() == R.id.fonts) {
                    startActivity(new Intent(getApplicationContext(), FontsActivity.class));
                }
                return false;
            }
        });

        Drawable drawable = AppCompatResources.getDrawable(this, R.drawable.editor);
        activityMainBinding.navigationView.getMenu().findItem(R.id.txt_editor).setIcon(drawable);
        activityMainBinding.navigationView.getMenu().findItem(R.id.txt_editor).getIcon().setTintMode(null);
        activityMainBinding.navigationView.getMenu().findItem(R.id.txt_editor).getIcon().setTint(0);
        activityMainBinding.navigationView.getMenu().findItem(R.id.txt_editor).getIcon().setTintList(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activityMainBinding.navigationView.getMenu().findItem(R.id.txt_editor).getIcon().setTintBlendMode(null);
        }
        Drawable logdrawable = AppCompatResources.getDrawable(this, R.drawable.alllogs);
        activityMainBinding.navigationView.getMenu().findItem(R.id.logs).setIcon(logdrawable);
        activityMainBinding.navigationView.getMenu().findItem(R.id.logs).getIcon().setTintMode(null);
        activityMainBinding.navigationView.getMenu().findItem(R.id.logs).getIcon().setTint(0);
        activityMainBinding.navigationView.getMenu().findItem(R.id.logs).getIcon().setTintList(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activityMainBinding.navigationView.getMenu().findItem(R.id.logs).getIcon().setTintBlendMode(null);
        }

        Drawable workSpaceIcon = AppCompatResources.getDrawable(this, R.drawable.computermonitor);
        activityMainBinding.navigationView.getMenu().findItem(R.id.workspaces).setIcon(workSpaceIcon);
        activityMainBinding.navigationView.getMenu().findItem(R.id.workspaces).getIcon().setTintMode(null);
        activityMainBinding.navigationView.getMenu().findItem(R.id.workspaces).getIcon().setTint(0);
        activityMainBinding.navigationView.getMenu().findItem(R.id.workspaces).getIcon().setTintList(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activityMainBinding.navigationView.getMenu().findItem(R.id.workspaces).getIcon().setTintBlendMode(null);
        }

        Drawable pkgIcon = AppCompatResources.getDrawable(this, R.drawable.pkg);
        activityMainBinding.navigationView.getMenu().findItem(R.id.pkgs).setIcon(pkgIcon);
        activityMainBinding.navigationView.getMenu().findItem(R.id.pkgs).getIcon().setTintMode(null);
        activityMainBinding.navigationView.getMenu().findItem(R.id.pkgs).getIcon().setTint(0);
        activityMainBinding.navigationView.getMenu().findItem(R.id.pkgs).getIcon().setTintList(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activityMainBinding.navigationView.getMenu().findItem(R.id.pkgs).getIcon().setTintBlendMode(null);
        }

        Drawable fontIcon = AppCompatResources.getDrawable(this, R.drawable.font);
        activityMainBinding.navigationView.getMenu().findItem(R.id.fonts).setIcon(fontIcon);
        activityMainBinding.navigationView.getMenu().findItem(R.id.fonts).getIcon().setTintMode(null);
        activityMainBinding.navigationView.getMenu().findItem(R.id.fonts).getIcon().setTint(0);
        activityMainBinding.navigationView.getMenu().findItem(R.id.fonts).getIcon().setTintList(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activityMainBinding.navigationView.getMenu().findItem(R.id.fonts).getIcon().setTintBlendMode(null);
        }

        activityMainBinding.toolBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.new_file){
                    NewFileDialogBinding binding = NewFileDialogBinding.inflate(getLayoutInflater());
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(binding.editText.getContext());
                    builder.setView(binding.getRoot());
                    autoCompleteAdapter.setAutoCompleteTextView(binding.autoComplete);
                    binding.autoComplete.setAdapter(autoCompleteAdapter);
                    final String[] ws = {binding.autoComplete.getText().toString()};
                    final String[] wsPath = new String[1];
                    final String[] filePath = new String[1];
                    binding.autoComplete.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void afterTextChanged(Editable s) {
                            if (s.equals("New One")){
                                binding.inputLay2.setVisibility(View.VISIBLE);
                            }else {
                                if (binding.inputLay2.getVisibility() == View.VISIBLE){
                                    binding.inputLay2.setVisibility(View.GONE);
                                }
                            }
                            filePath[0] = Paths.get(wsPath[0], binding.editText.getText().toString()+".tex").toString();
                        }

                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            ws[0] = s.toString();
                            wsPath[0] = Paths.get(getExternalFilesDir("").getAbsolutePath(), s.toString()).toString();
                        }
                    });
                    binding.editText2.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void afterTextChanged(Editable s) {
                            if (binding.editText2.getText() != null) {
                                if (binding.editText2.getText().toString().startsWith(".") || binding.editText2.getText().toString().contains(".")) {
                                    binding.inputLay2.setErrorEnabled(true);
                                    binding.inputLay2.setError("Please remove the dot");
                                } else if (s.toString().isBlank()) {
                                    binding.inputLay2.setErrorEnabled(true);
                                    binding.inputLay2.setError("Can't create a workSpace with no name");
                                }
                                ws[0] = s.toString();
                                filePath[0] = Paths.get(wsPath[0], binding.editText.getText().toString()+".tex").toString();
                            }
                        }

                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            binding.inputLay.setErrorEnabled(false);
                            wsPath[0] = Paths.get(getExternalFilesDir("").getAbsolutePath(), s.toString()).toString();
                        }
                    });
                    binding.editText.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void afterTextChanged(Editable s) {
                            if (binding.editText.getText() != null) {
                                if (binding.editText.getText().toString().startsWith(".") || binding.editText.getText().toString().contains(".")) {
                                    binding.inputLay.setErrorEnabled(true);
                                    binding.inputLay.setError("Please remove the dot");
                                } else if (s.toString().isBlank()) {
                                    binding.inputLay.setErrorEnabled(true);
                                    binding.inputLay.setError("Can't create a file with no name");
                                }
                            }
                        }

                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            binding.inputLay.setErrorEnabled(false);
                            filePath[0] = Paths.get(wsPath[0], s.toString()+".tex").toString();
                        }
                    });
                    binding.newWs.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                            if (isChecked){
                                binding.autoComplete.setEnabled(false);
                                binding.inputLay2.setVisibility(View.VISIBLE);
                            }else {
                                binding.autoComplete.setEnabled(true);
                                if (binding.inputLay2.getVisibility() == View.VISIBLE){
                                    binding.inputLay2.setVisibility(View.GONE);
                                }
                            }
                        }
                    });
                    AlertDialog dialog = builder.create();

                    binding.ok.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (binding.editText.getText().toString().isEmpty()){
                                binding.inputLay.setErrorEnabled(true);
                                binding.inputLay.setError("File name field is empty");
                                return;
                            } else if (binding.editText2.getText().toString().isEmpty() && binding.newWs.isChecked()) {
                                binding.inputLay2.setErrorEnabled(true);
                                binding.inputLay2.setError("workspace name field is empty");
                                return;
                            } else if (binding.autoComplete.getText().toString().isEmpty() && !binding.newWs.isChecked()) {
                                binding.wsChooser.setErrorEnabled(true);
                                binding.wsChooser.setError("Choose a valid workSpace");
                                return;
                            }
                            poolExecutor.execute(()->{
                                boolean createdWorkSpace = false;
                                boolean fileCreated = false;
                                File newFile = new File(filePath[0]);
                                File newWorkSpace = new File(wsPath[0]);
                                //check if new one and if work space exists
                                if (binding.newWs.isChecked()){
                                    newWorkSpace = new File(wsPath[0]);
                                    boolean exist = dataBase.workSpaceDao().workSpaceExists(newWorkSpace.getName());
                                    if (exist){
                                        handler.post(()->{
                                            binding.inputLay2.setErrorEnabled(true);
                                            binding.inputLay2.setError("A Work Space with the same name already exists");
                                        });
                                        return;
                                    }else {
                                        createdWorkSpace = newWorkSpace.mkdirs();
                                        if (createdWorkSpace){
                                            WorkSpaceEntity workSpace = new WorkSpaceEntity(0, newWorkSpace.getName(), newWorkSpace.lastModified(),
                                                    0, newWorkSpace.getAbsolutePath());
                                            dataBase.workSpaceDao().insertWorkSpace(workSpace);
                                            fileTools.updateViewModel();
                                        }
                                        Log.d("CreatingWorkSpace", newWorkSpace.getAbsolutePath()+" "+newWorkSpace.exists()+" exists");
                                    }
                                }
                                else {
                                    //not new workSpace so proceed
                                    createdWorkSpace = true;
                                    Log.d("CreatingWorkSpace", newWorkSpace.getAbsolutePath()+" "+newWorkSpace.exists()+" exists");
                                }
                                //check if file exists or not
                                if (binding.newWs.isChecked()){
                                    if (createdWorkSpace){
                                        newFile = new File(filePath[0]);
                                        try {
                                            fileCreated = newFile.createNewFile();
                                            FileEntity file = new FileEntity(newFile.getAbsolutePath(), System.currentTimeMillis(), 0, newFile.lastModified(), newFile.getName(),
                                                    newFile.length(), "tex", dataBase.workSpaceDao().getWorkSpaceByName(newWorkSpace.getName()).getWorkSpaceId());
                                            dataBase.workSpaceDao().insertFileEntity(file);
                                            fileTools.updateViewModel();
                                        } catch (Exception e) {
                                            handler.post(()->{
                                                Toast.makeText(binding.editText.getContext(), "Failed to create file", Toast.LENGTH_LONG).show();
                                            });
                                            Log.e("CreatingWorkSpace", newFile.getAbsolutePath()+" Failed "+newFile.exists()+" exists", e);
                                        }
                                    }
                                }
                                else {
                                    newFile = new File(filePath[0]);
                                    boolean exist = dataBase.workSpaceDao().FileExists(newFile.getName(), dataBase.workSpaceDao().getWorkSpaceByName(ws[0]).getWorkSpaceId());
                                    if (exist){
                                        handler.post(()->{
                                            binding.inputLay.setErrorEnabled(true);
                                            binding.inputLay.setError("A file with the same name exist in "+ ws[0]);
                                        });
                                        return;
                                    }else {
                                        //not exist
                                        try {
                                            fileCreated = newFile.createNewFile();
                                            FileEntity file = new FileEntity(newFile.getAbsolutePath(), System.currentTimeMillis(), 0, newFile.lastModified(), newFile.getName(),
                                                    newFile.length(), "tex", dataBase.workSpaceDao().getWorkSpaceByName(newWorkSpace.getName()).getWorkSpaceId());
                                            dataBase.workSpaceDao().insertFileEntity(file);
                                            fileTools.updateViewModel();
                                        } catch (Exception e) {
                                            handler.post(()->{
                                                Toast.makeText(binding.editText.getContext(), "Failed to create file", Toast.LENGTH_LONG).show();
                                            });
                                            Log.e("CreatingWorkSpace", newFile.getAbsolutePath()+" Failed "+newFile.exists()+" exists", e);
                                        }
                                    }
                                }
                                //add to database and open it in editor
                                if (createdWorkSpace){
                                    if (fileCreated){
                                        Bundle bundle = new Bundle();
                                        bundle.putBoolean("openFile", true);
                                        bundle.putBoolean("readOnly", false);
                                        bundle.putString("path", newFile.getAbsolutePath());
                                        bundle.putLong("owner ws", dataBase.workSpaceDao().getWorkSpaceByName(ws[0]).getWorkSpaceId());
                                        Intent intent = new Intent(getApplicationContext(), com.rk.activities.main.MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        intent.putExtras(bundle);
                                        startActivity(intent);
                                        dialog.dismiss();
                                    }
                                    else {
                                        handler.post(()->{
                                            Toast.makeText(binding.editText.getContext(), "Failed to create file", Toast.LENGTH_LONG).show();
                                        });
                                        Log.e("CreatingWorkSpace", newFile.getAbsolutePath()+" Failed "+newFile.exists()+" exists");
                                    }
                                }
                                else {
                                    handler.post(()->{
                                        Toast.makeText(binding.editText.getContext(), "Failed to create work space", Toast.LENGTH_LONG).show();
                                    });
                                    Log.e("CreatingWorkSpace", newWorkSpace.getAbsolutePath()+" Failed "+newWorkSpace.exists()+" exists");
                                }
                            });
                        }
                    });
                    binding.cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                } else if (item.getItemId() == R.id.import_id) {
                    launcher.launch(null);
                }
                return false;
            }
        });

        poolExecutor.execute(()->{
            List<ItemsData> itemsData = dataBase.workSpaceDao().getItemsData("all", null);
            matchesRecyclerAdapter.setList(new MutableLiveData<>(itemsData));
            activityMainBinding.matchesList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
            handler.post(()->{
                activityMainBinding.matchesList.setAdapter(matchesRecyclerAdapter);
            });
        });

        Thread search = new Thread(()->{
            List<FileEntity> found = new LinkedList<>();
            for (FileEntity file : dataBase.workSpaceDao().getOnlyImportant()) {
                if (file.getName().toLowerCase().strip().contains(activityMainBinding.srchView.getEditText().getText().toString().strip().toLowerCase())) {
                    found.add(file);
                }
            }
            if (found.isEmpty()) {
                handler.post(()->{
                    activityMainBinding.noMatches.setVisibility(View.VISIBLE);
                    activityMainBinding.matchesList.setVisibility(View.GONE);
                });
            } else {
                List<ItemsData> itemsData = dataBase.workSpaceDao().getItemsData("custom", found);
                handler.post(()->{
                    activityMainBinding.noMatches.setVisibility(View.GONE);
                    activityMainBinding.matchesList.setVisibility(View.VISIBLE);
                    matchesRecyclerAdapter.setList(new MutableLiveData<>(itemsData));
                    activityMainBinding.matchesList.setAdapter(matchesRecyclerAdapter);
                });
            }
        });
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

        viewModel.getAllFiles().observe(this, new Observer<List<FileEntity>>() {
            @Override
            public void onChanged(List<FileEntity> fileEntities) {
                service.schedule(search, 300, TimeUnit.MILLISECONDS);
            }
        });

        activityMainBinding.srchView.getEditText().addTextChangedListener(new TextWatcher() {
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
