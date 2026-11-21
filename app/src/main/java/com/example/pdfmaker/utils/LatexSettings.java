package com.example.pdfmaker.utils;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LifecycleOwner;

import com.example.core.FileTools;
import com.example.core.db.PkgEntity;
import com.example.pdfmaker.R;
import com.example.core.db.AppDataBase;
import com.example.core.db.FileEntity;
import com.example.core.db.WorkSpaceEntity;
import com.example.core.MainActivityViewModel;
import com.google.android.material.loadingindicator.LoadingIndicator;
import com.rk.crashhandler.CrashActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class LatexSettings {
    private final Context context;
    private final ThreadPoolExecutor poolExecutor;
    private final FileTools fileTools;
    private String[] TEST_COMMAND;

    private Handler handler;
    private final AppDataBase dataBase;
    private MainActivityViewModel mainActivityViewModel;

    private LifecycleOwner owner;

    @Inject
    public LatexSettings(@ApplicationContext Context context, ThreadPoolExecutor poolExecutor
            , FileTools fileTools, Handler handler, AppDataBase dataBase) {
        this.context = context;
        this.poolExecutor = poolExecutor;
        this.fileTools = fileTools;
        this.handler = handler;
        this.dataBase = dataBase;
    }

    public void setLaTeXDirectoryTree(LoadingIndicator loadingIndicator, TextView settingState, RelativeLayout loadingLay) {
        poolExecutor.execute(() -> {
            handler.post(() -> {
                loadingLay.setVisibility(View.VISIBLE);
                settingState.setVisibility(View.VISIBLE);
                settingState.setText("Setting Directory Tree");
            });
            try {
                String name = fileTools.extractLaTeX(context.getAssets(), loadingIndicator, context.getFilesDir(), false);
                handler.post(() -> {
                    settingState.setText("Set Successfuly");
                });
                Thread.sleep(3000);
                handler.post(() -> {
                    settingState.setText("Building Format Files");
                });
//                buildFmtFiles();
                Thread.sleep(3000);
                handler.post(() -> {
                    settingState.setText("Built Successfuly");
                });
                Thread.sleep(3000);
                handler.post(() -> {
                    loadingIndicator.setVisibility(View.GONE);
                    settingState.setVisibility(View.GONE);
                    loadingLay.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                Log.e("settingLaTeXDirectoryTree", "Failed: ", e);
                handler.post(() -> {
                    settingState.setText("Built Unsuccessful");
                    loadingIndicator.setVisibility(View.GONE);
                    settingState.setVisibility(View.GONE);
                    loadingLay.setVisibility(View.GONE);
                });
            }
        });
    }

    public HashMap<String, String> showPdfTeXVersion() throws Exception {
        HashMap<String, String> result = new HashMap<>();
        TEST_COMMAND = new String[]{
                getPdfTeXBin().getAbsolutePath(), "--version"

        };
        ProcessBuilder processBuilder = new ProcessBuilder(TEST_COMMAND);
        processBuilder.directory(context.getFilesDir());
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        result.put("exitCode", process.waitFor() + "");

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String s;
        while ((s = reader.readLine()) != null) {
            output.append(s).append("\n");
        }
        result.put("output", output.toString());


        Log.d("showingPdfTeXVersion", "exitCodeIs: ", new Throwable(result.get("exitCode")));
        Log.d("showingPdfTeXVersion", "outputIs: ", new Throwable(output.toString()));


        return result;
    }

    public File getPdfTeXBin() {
        File path = new File(context.getApplicationInfo().nativeLibraryDir, "pdflatex.so");
        return path;
    }

    public File getTmpDir() {
        File tmpDir = new File(context.getCacheDir(), "tmp");
        if (!tmpDir.exists()) {
            if (!tmpDir.mkdirs()) {
                Log.e("CreateTmp", "Failed to create: ", new Throwable(tmpDir.getAbsolutePath()));
            }
        }
        return tmpDir;
    }

    public HashMap<String, String> getDefaultEnv() {
        HashMap<String, String> env = new HashMap<>();
//        env.put("PATH", "/bin");
        env.put("TEXMFROOT", Paths.get(context.getFilesDir().getAbsolutePath(), "texmf").toString());
        env.put("APP_FILES_DIR", context.getFilesDir().getAbsolutePath());
        env.put("TEXFORMATS", Paths.get(context.getFilesDir().getAbsolutePath(), "texmf","texmf-var","web2c","pdftex").toString());
        env.put("HOME", context.getFilesDir().getAbsolutePath());
        env.put("TEXMFLOCAL", Paths.get(context.getFilesDir().getAbsolutePath(), "texmf","texmf-local").toString());
        env.put("TEXMFDIST", Paths.get(context.getFilesDir().getAbsolutePath(), "texmf","texmf-dist").toString());
        env.put("TEXMFCNF", Paths.get(context.getFilesDir().getAbsolutePath(), "texmf").toString());
        if(Boolean.TRUE.equals(mainActivityViewModel.getKpathsea_depug().getValue())){
            env.put("KPATHSEA_DEBUG", "-1");
        }
        return env;
    }

    public static void logLargeString(String tag, String content) {
        if (content == null) return;
        int maxLogSize = 3000;
        for (int i = 0; i <= content.length() / maxLogSize; i++) {
            int start = i * maxLogSize;
            int end = Math.min((i + 1) * maxLogSize, content.length());
            Log.d(tag, content.substring(start, end));
        }
    }

    public void buildFmtFiles() throws Exception {
        String[] command = new String[]{getPdfTeXBin().getAbsolutePath(),
                "-ini",
                "-etex",
                "-interaction=nonstopmode",
                "\\input latex.ltx \\dump"
        };

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(context.getFilesDir());
        processBuilder.environment().putAll(getDefaultEnv());
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        String s;
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        while ((s = reader.readLine()) != null) {
            output.append(s);
        }

        int exitCode = process.waitFor();

        Log.d("buildingFmtFile", "output: ", new Throwable(output.toString()));
        Log.d("buildingFmtFile", "exitCode: " + exitCode);

    }

    public boolean texmfExists() {
        return new File(context.getFilesDir(), "texmf").exists();
    }

    public void compileTeX(String path, RelativeLayout loadingLay, long workSpaceID) {
        poolExecutor.execute(()->{
            File file = new File(path);
            String name = file.getName().replace(".tex","");
            handler.post(()->{
                loadingLay.setVisibility(View.VISIBLE);
                loadingLay.findViewById(R.id.loading).setVisibility(View.VISIBLE);
                loadingLay.findViewById(R.id.state_text).setVisibility(View.VISIBLE);
                ((TextView) loadingLay.findViewById(R.id.state_text)).setText("Compiling "+name);
            });
            try {
                Thread.sleep(3000);
            } catch (Exception e) {
                Log.e("sleepingThread","",e);
            }
            try {
                String[] compileCommand = new String[]{
                        getPdfTeXBin().getAbsolutePath(),
                        "-fmt=latex",
                        "-output-format=pdf",
                        "-interaction=nonstopmode",
                        path
                };

                ProcessBuilder processBuilder = new ProcessBuilder(compileCommand);
                processBuilder.directory(new File(path).getParentFile());
                processBuilder.environment().putAll(getDefaultEnv());
                processBuilder.redirectErrorStream(true);

                Process process = processBuilder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String s;
                while ((s = reader.readLine()) != null) {
                    output.append(s).append("\n");
                }

                int exitCode = process.waitFor();


                logLargeString("compilingTeX", output.toString());
//                Log.d("compilingTeX", "output: ", new Throwable(output.toString()));
                Log.d("compilingTeX", "exitCode: " + exitCode);

                if (exitCode == 0){
                    handler.post(()->{
                        ((TextView) loadingLay.findViewById(R.id.state_text)).setText("Compiled "+name+" Successfuly");
                    });
                    Thread.sleep(3000);
                    handler.post(()->{
                        loadingLay.setVisibility(View.GONE);
                        loadingLay.findViewById(R.id.loading).setVisibility(View.GONE);
                        loadingLay.findViewById(R.id.state_text).setVisibility(View.GONE);
                    });
                    addFilesToDB(new File(file.getParentFile(), name+".pdf"), workSpaceID);
                    addFilesToDB(new File(file.getParentFile(), name+".log"), workSpaceID);
                }else {
                    File pdf;
                    if ((pdf = new File(file.getParentFile(), name+".pdf")).isFile() && pdf.exists()){
                        addFilesToDB(pdf, workSpaceID);
                    }
                    addFilesToDB(new File(file.getParentFile(), name+".log"), workSpaceID);
                    handler.post(()->{
                        String p = "Compiling "+name+" Unsuccessful";
                        ((TextView) loadingLay.findViewById(R.id.state_text)).setText(p);
                    });
                    Thread.sleep(3000);
                    handler.post(()->{
                        loadingLay.setVisibility(View.GONE);
                        loadingLay.findViewById(R.id.loading).setVisibility(View.GONE);
                        loadingLay.findViewById(R.id.state_text).setVisibility(View.GONE);
                    });
                }
                File output_log = new File(file.getParentFile(), name+"_output.log");
                boolean created = false;
                if (!output_log.exists()){
                    created = output_log.createNewFile();
                }else {
                    created = true;
                }
                if (created){
                    fileTools.writeToFile(output.append("\n\n\n\n\nExitCode is: ").append(exitCode).toString(), output_log);
                    addFilesToDB(output_log, workSpaceID);
                }
            } catch (Exception e) {
                Log.e("compilingTeX", "Failed: ", e);
                handler.post(()->{
                    ((TextView) loadingLay.findViewById(R.id.state_text)).setText("Compiling "+name+" Unsuccessful");
                });
                try {
                    Thread.sleep(3000);
                } catch (Exception ex) {
                    Log.e("sleepingThread","",ex);
                }
                handler.post(()->{
                    loadingLay.setVisibility(View.GONE);
                    loadingLay.findViewById(R.id.loading).setVisibility(View.GONE);
                    loadingLay.findViewById(R.id.state_text).setVisibility(View.GONE);
                });
            }
        });
    }

    public void buildLSRFiles(RelativeLayout loadingLay){
        poolExecutor.execute(()->{
            handler.post(()->{
                loadingLay.setVisibility(View.VISIBLE);
                loadingLay.findViewById(R.id.loading).setVisibility(View.VISIBLE);
                loadingLay.findViewById(R.id.state_text).setVisibility(View.VISIBLE);
                ((TextView) loadingLay.findViewById(R.id.state_text)).setText("Making ls-R Files");
            });
            try {
                Thread.sleep(3000);
            } catch (Exception e) {
                Log.e("sleepingThread","",e);
            }
            try {
                String[] command = new String[]{
                        getPdfTeXBin().getAbsolutePath()+
                        " mktexlsr",
                        Paths.get(getDefaultEnv().get("TEXMFROOT"), "texmf-local").toString(),
                        Paths.get(getDefaultEnv().get("TEXMFROOT"), "texmf-local").toString()
                };

                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.directory(context.getFilesDir());
                processBuilder.environment().putAll(getDefaultEnv());
                processBuilder.redirectErrorStream(true);

                Process process = processBuilder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String s;

                while ((s = reader.readLine()) != null){
                    output.append(s);
                }

                int exitCode = process.waitFor();

                Log.d("BuildingLSR", "output: ", new Throwable(output.toString()));
                Log.d("BuildingLSR", "exitCode: " + exitCode);

                if (exitCode == 0){
                    handler.post(()->{
                        ((TextView) loadingLay.findViewById(R.id.state_text)).setText("Made ls-R Files Successfuly");
                    });
                    Thread.sleep(3000);
                    handler.post(()->{
                        loadingLay.setVisibility(View.GONE);
                        loadingLay.findViewById(R.id.loading).setVisibility(View.GONE);
                        loadingLay.findViewById(R.id.state_text).setVisibility(View.GONE);
                    });
                }else {
                    handler.post(()->{
                        ((TextView) loadingLay.findViewById(R.id.state_text)).setText("Making ls-R Files Unsuccessful");
                    });
                    Thread.sleep(3000);
                    handler.post(()->{
                        loadingLay.setVisibility(View.GONE);
                        loadingLay.findViewById(R.id.loading).setVisibility(View.GONE);
                        loadingLay.findViewById(R.id.state_text).setVisibility(View.GONE);
                    });
                }
            } catch (Exception e) {
                Log.e("BuildingLSR", "Failed: ", e);
                handler.post(()->{
                    ((TextView) loadingLay.findViewById(R.id.state_text)).setText("Making ls-R Files Unsuccessful");
                });
                try {
                    Thread.sleep(3000);
                } catch (Exception ex) {
                    Log.e("sleepingThread","",ex);
                }
                handler.post(()->{
                    loadingLay.setVisibility(View.GONE);
                    loadingLay.findViewById(R.id.loading).setVisibility(View.GONE);
                    loadingLay.findViewById(R.id.state_text).setVisibility(View.GONE);
                });
            }
        });
    }
    
    public void addFilesToDB(File file, long workSpaceID){
        String name = file.getName();
        String suffix;
        WorkSpaceEntity ownerWorkSpace = null;
        boolean exists = false;
        if (name.endsWith(".pdf") || name.endsWith(".PDF")){
            ownerWorkSpace = dataBase.workSpaceDao().getWorkSpaceById(workSpaceID);
            FileEntity newPdfFile = new FileEntity(file.getAbsolutePath(), System.currentTimeMillis(), 0,
                    file.lastModified(),name,file.length(),name.substring(name.lastIndexOf(".")), ownerWorkSpace.getWorkSpaceId());
            exists = fileTools.fileEntityExists(newPdfFile, workSpaceID);
            if (exists){
                FileEntity existingFile = dataBase.workSpaceDao().getFileByName(file.getName());
                existingFile.setDate(System.currentTimeMillis());
                existingFile.setSize(file.length());
                existingFile.setLastModified(file.lastModified());
                dataBase.workSpaceDao().updateFileEntity(existingFile);
            }else {
                dataBase.workSpaceDao().insertFile(newPdfFile);
            }
        } else if (name.endsWith(".log") || name.endsWith(".LOG")) {
            ownerWorkSpace = dataBase.workSpaceDao().getWorkSpaceById(workSpaceID);
            FileEntity newLogFile = new FileEntity(file.getAbsolutePath(), System.currentTimeMillis(), 0,
                    file.lastModified(),name,file.length(),name.substring(name.lastIndexOf(".")), ownerWorkSpace.getWorkSpaceId());
            exists = fileTools.fileEntityExists(newLogFile, workSpaceID);
            if (exists){
                FileEntity existingFile = dataBase.workSpaceDao().getFileByName(file.getName());
                existingFile.setDate(System.currentTimeMillis());
                existingFile.setSize(file.length());
                existingFile.setLastModified(file.lastModified());
                dataBase.workSpaceDao().updateFileEntity(existingFile);
            }else {
                dataBase.workSpaceDao().insertFile(newLogFile);
            }
        }
        //update workspace files number
        if (ownerWorkSpace != null) {
            if (!exists){
                ownerWorkSpace.setFilesNumber(ownerWorkSpace.getFilesNumber() + 1);
            }
            ownerWorkSpace.setLastModified(System.currentTimeMillis());
            dataBase.workSpaceDao().updateWorkSpace(ownerWorkSpace);
        }
        //change viewModel
        if (mainActivityViewModel != null){
            mainActivityViewModel.getPdfsList().postValue(dataBase.workSpaceDao().getAllPdfs());
            mainActivityViewModel.getAllFiles().postValue(dataBase.workSpaceDao().getAllFiles());
            mainActivityViewModel.getWorkSpaces().postValue(dataBase.workSpaceDao().getAllWorkSpaces());
        }
    }

    public void setMainActivityViewModel(MainActivityViewModel mainActivityViewModel) {
        this.mainActivityViewModel = mainActivityViewModel;
    }

    public void setOwner(LifecycleOwner owner) {
        this.owner = owner;
    }

    public File getTexLocalDir(){
        Path path = Paths.get(context.getFilesDir().getAbsolutePath(), "texmf","texmf-local", "tex","latex", "local");
        return path.toFile();
    }

    public void copyPkg(DocumentFile pkg, RelativeLayout loading) {
        loading.setVisibility(View.VISIBLE);
        ((TextView) loading.findViewById(R.id.state_text)).setText("Copying");
        File target = getTexLocalDir();
        if (!pkg.isDirectory()){
            Toast.makeText(context, "the pkg must be in a folder named with pkg name", Toast.LENGTH_LONG).show();
            loading.setVisibility(View.GONE);
            return;
        }
        if (pkg.listFiles().length == 0){
            Toast.makeText(context, "Folder is empty", Toast.LENGTH_SHORT).show();
            loading.setVisibility(View.GONE);
            return;
        }
        poolExecutor.execute(()->{
            try {
                //check if first file is dir or not if not copy the whole folder else we should copy the directories inside
                //as it should every folder to contain only files
                //the folder should either contain the pkg itself (.sty file) or multiple folders each one resembles a pkg
                for (DocumentFile file : pkg.listFiles()){
                    if (file.isDirectory()){
                        Log.d("CopyingPkgs", file.getName()+" copying to: "+target.getAbsolutePath());
                        boolean pkgExists = dataBase.workSpaceDao().pkgExists(file.getName());
                        File copyTo = new File(target, file.getName());
                        if (!copyTo.exists()){
                            if (!copyTo.mkdirs()){
                                Log.d("CopyingPkgs", "Failed to copy " + file.getName());
                                continue;
                            }
                        }
                        fileTools.copyRecursive(file, copyTo);
                        if (pkgExists){
                            PkgEntity pkgEntity = dataBase.workSpaceDao().getPkgByName(file.getName());
                            pkgEntity.setDateAdded(System.currentTimeMillis());
                            pkgEntity.setSize(file.length());
                            dataBase.workSpaceDao().updatePkgEntity(pkgEntity);
                            Log.d("CopyingPkgs", file.getName()+" copying to: "+copyTo.getAbsolutePath()+" its Path is: "+pkgEntity.getPath()+" updated it");
                        }else {
                            PkgEntity pkgEntity = new PkgEntity(file.lastModified(), file.getName(), file.length(), Paths.get(getTexLocalDir().getAbsolutePath(), file.getName()).toString());
                            dataBase.workSpaceDao().insertPkg(pkgEntity);
                            Log.d("CopyingPkgs", file.getName()+" copying to: "+copyTo.getAbsolutePath()+" its Path is: "+pkgEntity.getPath()+ " newly created");
                        }
                    }else {
                        //first file is regular file so it is one pkg so copy the folder only
                        File copyTo = new File(target, pkg.getName());
                        if (!copyTo.exists()){
                            if(!copyTo.mkdirs()){
                                Log.e("CopyingPkg", "Failed to copy " + pkg.getName());
                                break;
                            }
                        }
                        fileTools.copyRecursive(pkg, copyTo);
                        boolean pkgExists = dataBase.workSpaceDao().pkgExists(pkg.getName());
                        if (pkgExists){
                            PkgEntity pkgEntity = dataBase.workSpaceDao().getPkgByName(pkg.getName());
                            pkgEntity.setDateAdded(System.currentTimeMillis());
                            pkgEntity.setSize(pkg.length());
                            dataBase.workSpaceDao().updatePkgEntity(pkgEntity);
                            Log.d("CopyingOnePkg", file.getName()+" copying to: "+copyTo.getAbsolutePath()+" its Path is: "+pkgEntity.getPath()+" updated it");
                        }else {
                            PkgEntity pkgEntity = new PkgEntity(pkg.lastModified(), pkg.getName(), pkg.length(), Paths.get(getTexLocalDir().getAbsolutePath(), pkg.getName()).toString());
                            dataBase.workSpaceDao().insertPkg(pkgEntity);
                            Log.d("CopyingOnePkg", file.getName()+" copying to: "+copyTo.getAbsolutePath()+" its Path is: "+pkgEntity.getPath()+ " newly created");
                        }
                        break;
                    }
                }
                mainActivityViewModel.getPkgs().postValue(dataBase.workSpaceDao().getAllPkgs());
                handler.post(()->{
                    ((TextView) loading.findViewById(R.id.state_text)).setText("Copied Succesfully");
                });
                Thread.sleep(3000);
                handler.post(()->{
                    loading.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                handler.post(()->{
                    loading.setVisibility(View.GONE);
                });
                Toast.makeText(context, "Filed to copy pkg", Toast.LENGTH_SHORT).show();
                Log.e("CopyingPkg","", e);
            }
        });
    }
}
