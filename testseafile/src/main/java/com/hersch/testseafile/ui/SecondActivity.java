package com.hersch.testseafile.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.hersch.testseafile.CustomProcess;
import com.hersch.testseafile.R;
import com.hersch.testseafile.files.ConfigList;
import com.hersch.testseafile.files.FileSM;
import com.hersch.testseafile.net.HttpRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.hersch.testseafile.files.FileRooter;
import com.hersch.testseafile.files.FileSnapshot;
import com.hersch.testseafile.files.common;

public class SecondActivity extends AppCompatActivity {
    static String strToken = MainActivity.strToken;
    static String strCookie = MainActivity.strCookie;
    public final static int MSG_COMPLETE_BACKUP = 1;
    public final static int MSG_COMPLETE_SYNC = 2;
    public final static int MSG_BACKUP_FILE_INFO = 3;
    public final static int MSG_FILE_SELECT_CODE = 4;
    public final static int MSG_NOT_SYNC = 5;
    public static List<Integer>chmodIntList = new ArrayList<>();
    public static List<String>chmodFileList = new ArrayList<>();
    public static List<String>deleteZipList = new ArrayList<>();
    static String processName = "com.tencent.mm";
    static String strProcessPath = "/data/data/com.tencent.mm";
    static String strIpAddress = HttpRequest.strIpAddress;//"10.108.20.142";//
    static String strFirstFile = "------WebKitFormBoundaryWwA1f0fjjPetVzQa\r\nContent-Disposition: form-data; name=\"parent_dir\"\r\n\r\n";
    static String strTargetFile = "\r\n------WebKitFormBoundaryWwA1f0fjjPetVzQa\r\nContent-Disposition: form-data; name=\"target_file\"\r\n\r\n";
    static String strDirFile = "\r\n------WebKitFormBoundaryWwA1f0fjjPetVzQa\r\nContent-Disposition: form-data; name=\"file\"; filename=\"";
    static String strEndFile = "\r\n------WebKitFormBoundaryWwA1f0fjjPetVzQa--\r\n";
    static String strMiddleFile = "\"\r\nContent-Type: application/octet-stream\r\n\r\n";
    public static String strRootId = MainActivity.strRootId;
    public static byte[] m_binArray = null;
    public static String strCurrentPath = "/data/data/com.hersch.testseafile";
    public static int fileSize=0;
    Button btnSync;
    Button btnTest;
    Button btnSnapshot;
    Button btnZip;
    Button btnUnZip;
    TextView tvFileScanner;
    Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        context = getApplicationContext();
        findView();
    }
    void initPreDirectoryOnCloud(){
        Context context = SecondActivity.this;
        FileSnapshot.createDirectory("/data");
        FileSnapshot.createDirectory("/data/data");
        FileSnapshot.createDirectory("/data/data/com.tencent.mm");
        FileSnapshot.createDirectory("/storage");
        FileSnapshot.createDirectory("/storage/emulated");
        FileSnapshot.createDirectory("/storage/emulated/0");
        FileSnapshot.createDirectory("/storage/emulated/0/tencent");
    }
    void findView() {
        tvFileScanner=(TextView)findViewById(R.id.tvFileScanner);
        btnSnapshot = (Button) findViewById(R.id.btnSnapshot);
        btnSnapshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CustomProcess.isProcessRunning(context) || CustomProcess.isServiceRunning(context)) {
                    //当前微信正在运行
                    createBackUpDialg();//弹出确认框
                } else {
                    backupToSeafile();
                    //zip();
                }
            }
        });
        btnSync = (Button) findViewById(R.id.btnSync);
        btnSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getApplicationContext();
                if (CustomProcess.isProcessRunning(context) || CustomProcess.isServiceRunning(context)) {
                    //当前微信正在运行
                    createSyncDialg();//弹出确认框
                } else {
                    //syncFileToMsg();
                    //unzip();
                    syncToMsg();
                }
            }
        });
        btnZip = (Button)findViewById(R.id.btnZip);
        btnZip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String srcFilePath = "/data/data/com.tencent.mm/MicroMsg/weixin.apk";//压缩包形式
                        FileRooter.cmdZip(srcFilePath, srcFilePath + ".gz");
                        List<String>subFileList = FileSM.split(srcFilePath + ".gz");
                        for(int i=0;i<subFileList.size();i++){
                            File tempFile = new File(subFileList.get(i));
                            if(isFileExistOnCloud(tempFile.getAbsolutePath())){
                                uploadFile("update",tempFile.getAbsolutePath(),tempFile.getName(),tempFile.getParent());
                            }
                            else{
                                uploadFile("upload",tempFile.getAbsolutePath(),tempFile.getName(),tempFile.getParent());
                            }
                        }
                    }
                }).start();
            }
        });
        btnUnZip = (Button)findViewById(R.id.btnUnZip);
        btnUnZip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        List<String>subFileList = FileSM.getSplitListFromCloud("/data/data/com.tencent.mm/MicroMsg/weixin.apk");
                        for(int i=0;i<subFileList.size();i++){
                            downloadFile(strCurrentPath+subFileList.get(i),subFileList.get(i));
                        }
                        FileSM.merge(strCurrentPath+"/out/weixin.apk.gz",subFileList);
                        String srcFilePath = strCurrentPath + "/out/weixin.apk.gz";
                        String desFilePath = strCurrentPath + "/out/weixin.apk";
                        FileRooter.cmdUnZip(srcFilePath, desFilePath);
                    }
                }).start();
//                if (CustomProcess.isProcessRunning(context) || CustomProcess.isServiceRunning(context)) {
//                    //当前微信正在运行
//                    createSyncDialg();//弹出确认框
//                } else {
//                    unzip();
//                }
            }
        });
        btnTest = (Button)findViewById(R.id.btnTest);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        uploadFile("upload", strCurrentPath + "/EnMicroMsg.db.gz", "EnMicroMsg.db.gz", "/");
                        downloadFile(strCurrentPath + "/out/EnMicroMsg.db.gz", "/EnMicroMsg.db.gz");
                        System.out.println("下载成功");
                    }
                }).start();
            }
        });
    }
    //退出微信提示框
    void createBackUpDialg(){
        AlertDialog.Builder builder = new AlertDialog.Builder(SecondActivity.this);
        builder.setMessage("检测到微信正在运行,确认退出微信开始备份数据吗？");
        builder.setTitle("提示");
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                CustomProcess.kill(processName);
                backupToSeafile();
                //zip();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }
    /**
     * 弹出确认关闭微信的窗口
     */
    void createSyncDialg() {
        AlertDialog.Builder builder = new AlertDialog.Builder(SecondActivity.this);
        builder.setMessage("检测到微信正在运行,确认退出微信开始同步数据吗？");
        builder.setTitle("提示");
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                CustomProcess.kill(processName);
                //syncFileToMsg();
                //unzip();
                syncToMsg();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }
    void backupToSeafile(){
        btnSnapshot.setText("数据备份中.....");
        btnSnapshot.setEnabled(false);
        List<String>listTraverseFile = ConfigList.getList("com.tencent.mm");
        if (listTraverseFile.size() > 0) {
            //选择备份的文件夹
            new Thread(new Runnable() {
                @Override
                public void run() {
                    initPreDirectoryOnCloud();
                    List<String>listTraverseFile = ConfigList.getList("com.tencent.mm");
                    chmodFileList.clear();
                    chmodIntList.clear();
                    deleteZipList.clear();
                    FileRooter.chmodRootDirectory(getApplicationContext(), 777, "chmodAccess", strProcessPath);
                    for (String s : listTraverseFile) {
                        FileRooter.chmodRootDirectory(context, 777, "chmodAccess", s);
                        FileSnapshot.createDirectory(s);//在云端创建目录
                        FileSnapshot.traverseFileCy(SecondActivity.this, s, myHandler);
                        FileSnapshot.rollBackChmodFile(getApplicationContext(),s);
                    }
                    System.out.println("----->还原文件权限中");
                    FileSnapshot.rollBackChmodFile(getApplicationContext(), strProcessPath);
                    FileRooter.rollBackChmodFiles(chmodIntList, chmodFileList);
                    System.out.println("还原文件权限成功");

                    System.out.println("----->删除文件压缩包中<------");
                    FileRooter.deleteZipsOfFiles(deleteZipList);
                    System.out.println("----->删除文件压缩包成功<------");


                    System.out.println("---->备份xml到云端....");
                    syncSharedPrefsToCloud("backupMd5.xml");//将备份后的md文件备份到云端
                    syncSharedPrefsToCloud("changeMd5.xml");
                    //syncSharedPrefsToCloud("chmodAccess.xml");
                    System.out.println("----->备份xml完成");

                    sendMsg(MSG_COMPLETE_BACKUP);
                }
            }).start();
        }
        else {
            Toast.makeText(SecondActivity.this, "请选择备份文件夹", Toast.LENGTH_SHORT).show();
        }
    }
    void syncToMsg(){
        btnSync.setText("同步数据中....");
        btnSync.setEnabled(false);
        final Context context = getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                File changeMd5File = new File(strCurrentPath +"/shared_prefs/changeMd5.xml");
                File backupMd5File = new File(strCurrentPath+"/shared_prefs/backupMd5.xml");
                SharedPreferences backupMd5Prefs = context.getSharedPreferences("backupMd5", Context.MODE_PRIVATE);
                SharedPreferences changeMd5Prefs = context.getSharedPreferences("changeMd5", Context.MODE_PRIVATE);
                SharedPreferences.Editor backupMd5Editor = backupMd5Prefs.edit();
                SharedPreferences.Editor changeMd5Editor = changeMd5Prefs.edit();
                if(isFileExistOnCloud("/changeMd5.xml")) {
                    //在云端进行过备份
                    if (!changeMd5File.exists()) {
                        List<String>srcZipFilePath = new ArrayList<String>();
                        List<String>desZipFilePath = new ArrayList<String>();
                        //本地不存在changeMd5说明未进行过备份
                        backupMd5Editor.commit();
                        changeMd5Editor.commit();
                        //本地不存在说明还未进行备份,从服务端下载上次备份到云端的文件覆盖本地
                        downloadFile(changeMd5File.getAbsolutePath(), "/changeMd5.xml");
                        downloadFile(backupMd5File.getAbsolutePath(), "/backupMd5.xml");
//                        backupMd5Editor.commit();
//                        changeMd5Editor.commit();
                        Map<String, ?> map = backupMd5Prefs.getAll();//存放所有文件的md5值
                        for (String key : map.keySet()) {
                            String zipName = key+".gz";
                            String unZipName = key;
                            new File(strCurrentPath+unZipName).getParentFile().mkdirs();
                            downloadFile(strCurrentPath+zipName, zipName);//将云端的文件存入当前app中
                            srcZipFilePath.add(strCurrentPath + zipName);//再将压缩文件存入微信中
                            desZipFilePath.add(unZipName);
                        }
                        System.out.println("----解压到微信目录----");
                        System.out.println(srcZipFilePath + "---->" + desZipFilePath);
                        FileRooter.cmdUnzips(srcZipFilePath,desZipFilePath);
                        System.out.println("----sync to Msg successfully!");
                    }
                    else { //代表云端的文件和本地相同
                        Map<String, ?> map = changeMd5Prefs.getAll();
                        //FileRooter.chmodFiles(777, map);//将同步的文件批量chmod
                        List<String>srcZipFilePath = new ArrayList<String>();
                        List<String>desZipFilePath = new ArrayList<String>();
                        for (String key : map.keySet()) {
                            String zipName = key+".gz";
                            String unZipName = key;
                            File parentFile = new File(strCurrentPath+unZipName).getParentFile();
                            parentFile.mkdirs();
                            if(isFileExistOnCloud(zipName)) {
                                downloadFile(strCurrentPath + zipName, zipName);//将云端的文件存入当前app中
                                srcZipFilePath.add(strCurrentPath + zipName);//记录暂时的压缩包路径和在微信目录中的路径
                                desZipFilePath.add(unZipName);
                                System.out.println(zipName + " is downloaded");
                            }
                            else{
                                List<String>splitList = FileSM.getSplitListFromCloud(zipName);//列表存放在云端上的路径
                                if(FileSM.isCompleteSplitNum(splitList)){
                                    for(int i=0;i<splitList.size();i++){
                                        String splitZipName = splitList.get(i);
                                        downloadFile(strCurrentPath + splitZipName, splitZipName);
                                        System.out.println(splitZipName + " is downloaded");
                                    }
                                    FileSM.merge(strCurrentPath + zipName, splitList);
                                }
                                else{
                                    Toast.makeText(SecondActivity.this,"合并时文件序号丢失",Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                        System.out.println("----解压到微信目录----");
                        FileRooter.cmdUnzips(srcZipFilePath,desZipFilePath);
                    }
                    sendMsg(MSG_COMPLETE_SYNC);
                }
                else{
                    //服务器上未进行过备份
                    sendMsg(MSG_NOT_SYNC);
                }
            }
        }).start();
    }
    /**
     * 每次备份文件后将期间以及以前发生变化的文件都存入sharedPrefs并将xml上传至云平台
     */
    void syncSharedPrefsToCloud(String fileName){
        //将期间发生变化的ChangeMd5文件传到云平台供以后的同步所用,在云端根目录下
        String strSharedPrefsPath = strCurrentPath + "/shared_prefs/"+fileName;//在本地的目录
        if(isFileExistOnCloud("/"+fileName)){
            uploadFile("update", strSharedPrefsPath, fileName, "/");
        }
        else{
            uploadFile("upload", strSharedPrefsPath, fileName, "/");
        }
    }
    /**
     * 子线程更新UI消息
     * @param msgType
     */
    private void sendMsg(int msgType){
        Message msg = Message.obtain();
        msg.what = msgType;
        myHandler.sendMessage(msg);
    }
    /**
     * 上传文件(需要保证是文件而不是文件夹,否则出错)
     * @param strFileDir(本地绝对路径)
     * @param strFileName
     * @param uploadPath(云平台父亲目录路径)
     */
    public static void uploadFile(String cmd,String strFileDir, String strFileName,
                                  String uploadPath) {
        try {
            String strFile = HttpRequest.sendGet("http://" + strIpAddress
                            + ":8000/ajax/repo/" + strRootId + "/file_op_url/",
                    "op_type=" + cmd + "&path=" + uploadPath + "&_=14815507370953",
                    strCookie);
            System.out.println("****" + strFile);
            String strFilePath = strFile.substring(9, strFile.length() - 2);
            System.out.println(strFilePath);
            String strUpload="";
            if(cmd.equals("update")){
                strUpload = HttpRequest.uploadFile(strFilePath,
                        mergeBodyUpdate(strFileDir, strFileName, uploadPath),
                        strCookie);
            }
            else{
                strUpload = HttpRequest.uploadFile(strFilePath,
                        mergeBodyUpload(strFileDir, strFileName,uploadPath),
                        strCookie);
            }
            System.out.println(strUpload);
        } catch (Exception e) {
            System.out.println("======== uploadFile Error ======= "
                    + strFileDir);
            e.printStackTrace();
        }
    }
    /**
     * 更新文件(比上传文件多了一个参数:在云平台上的目标更新文件Target)
     * @param strFileDir
     * @param strFileName
     * @param strRelativeDirpath
     * @return
     */
    public static byte[] mergeBodyUpdate(String strFileDir,String strFileName,String strRelativeDirpath){
        byte[] temp1 = common.readFile(strFileDir);
        strFileDir = strRelativeDirpath + "/" + strFileName;//strFileDir是在手机上的路径/data/data/packgname/sharedPrefs
        //云平台没有sharedPrefs的文件夹
        m_binArray = new byte[strFirstFile.length()
                + strRelativeDirpath.length()+ strTargetFile.length()
                + strFileDir.length() + strDirFile.length()
                + strFileName.length() + strMiddleFile.length()
                + strEndFile.length() + temp1.length];
        System.arraycopy(strFirstFile.getBytes(), 0, m_binArray, 0,
                strFirstFile.length());
        System.arraycopy(strRelativeDirpath.getBytes(), 0, m_binArray,
                strFirstFile.length(), strRelativeDirpath.length());
        System.arraycopy(strTargetFile.getBytes(), 0, m_binArray,
                strFirstFile.length() + strRelativeDirpath.length(),
                strTargetFile.length());
        System.arraycopy(strFileDir.getBytes(), 0,m_binArray,
                strFirstFile.length() + strRelativeDirpath.length() + strTargetFile.length()
                ,strFileDir.length());
        System.arraycopy(strDirFile.getBytes(), 0, m_binArray,
                strFirstFile.length() + strRelativeDirpath.length() + strTargetFile.length()
                        + strFileDir.length(),
                strDirFile.length());
        System.arraycopy(strFileName.getBytes(), 0, m_binArray,
                strFirstFile.length() + strRelativeDirpath.length() + strTargetFile.length()
                        + strFileDir.length() + strDirFile.length(),
                strFileName.length());
        System.arraycopy(strMiddleFile.getBytes(), 0, m_binArray,
                strFirstFile.length() + strRelativeDirpath.length() + strTargetFile.length()
                        + strFileDir.length() + strDirFile.length() + strFileName.length(),
                strMiddleFile.length());
        System.arraycopy(temp1, 0, m_binArray,
                strFirstFile.length() + strRelativeDirpath.length() + strTargetFile.length()
                        + strFileDir.length() + strDirFile.length() + strFileName.length()
                        + strMiddleFile.length(),
                temp1.length);
        System.arraycopy(strEndFile.getBytes(), 0, m_binArray,
                strFirstFile.length() + strRelativeDirpath.length() + strTargetFile.length()
                        + strFileDir.length() + strDirFile.length() + strFileName.length()
                        + strMiddleFile.length() + temp1.length,
                strEndFile.length());
        return m_binArray;
    }
    /**
     * 上传文件
     * @param strFileDir
     * @param strFileName
     * @param strRelativeDirpath
     * @return
     */
    public static byte[] mergeBodyUpload(String strFileDir, String strFileName,
                                         String strRelativeDirpath) {
        byte[] temp1 = common.readFile(strFileDir);
        m_binArray = new byte[strFirstFile.length()
                + strRelativeDirpath.length() + strDirFile.length()
                + strFileName.length() + strMiddleFile.length()
                + strEndFile.length() + temp1.length];
        System.arraycopy(strFirstFile.getBytes(), 0, m_binArray, 0,
                strFirstFile.length());
        System.arraycopy(strRelativeDirpath.getBytes(), 0, m_binArray,
                strFirstFile.length(), strRelativeDirpath.length());
        System.arraycopy(strDirFile.getBytes(), 0, m_binArray,
                strFirstFile.length() + strRelativeDirpath.length(),
                strDirFile.length());
        System.arraycopy(strFileName.getBytes(), 0, m_binArray,
                strFirstFile.length() + strRelativeDirpath.length()
                        + strDirFile.length(), strFileName.length());
        System.arraycopy(strMiddleFile.getBytes(), 0, m_binArray,
                strFirstFile.length() + strRelativeDirpath.length()
                        + strDirFile.length() + strFileName.length(),
                strMiddleFile.length());
        System.arraycopy(temp1, 0, m_binArray, strFirstFile.length()
                + strRelativeDirpath.length() + strDirFile.length()
                + strFileName.length() + strMiddleFile.length(), temp1.length);
        System.arraycopy(strEndFile.getBytes(), 0, m_binArray,
                strFirstFile.length() + strRelativeDirpath.length()
                        + strDirFile.length() + strFileName.length()
                        + strMiddleFile.length() + temp1.length,
                strEndFile.length());
        return m_binArray;
    }
    /**
     * 下载文件存放在源文件路径下命名为.backup文件
     * @param strFileDir
     * @param strFileCloudPath
     */
    public static void downloadFile(String strFileDir, String strFileCloudPath) {
        byte[] fileArray = HttpRequest.downloadFile("http://" + strIpAddress
                        + ":8000/lib/" + strRootId + "/file" + strFileCloudPath, "dl=1",
                strCookie);///file后面应该跟云平台对应的路径名,当前所下载的文件是在根目录下
        common.writeFile(strFileDir, fileArray);
    }
    /**
     * 创建文件的post包格式必须包含XCRSToken！！
     * %2F后跟文件夹名字代表在该文件夹下/dir
     * 创建文件夹
     */
    public static void createDirToCloud(final String strFilePath){
        int i1 = strFilePath.lastIndexOf("/");
        String strParentPath = strFilePath.substring(0,i1+1);
        String strFileName = strFilePath.substring(i1 + 1);
        String strContent = HttpRequest.sendPost1("http://" + strIpAddress + ":8000/ajax/repo/" + strRootId +
                        "/dir/new/?parent_dir=" + strParentPath, "dirent_name=" + strFileName, strToken, strCookie,
                "application/x-www-form-urlencoded; charset=UTF-8");
        System.out.println(strContent);
    }
    /**
     * 判断当前云端是否存在该文件(为upload和update服务)
     * @param strFilePath(在云端的绝对路径)
     * @return
     */
    public static boolean isFileExistOnCloud(String strFilePath){
        int i1 = strFilePath.lastIndexOf("/");
        String strParentPath = strFilePath.substring(0,i1+1);//上层目录
        String fileName = strFilePath.substring(i1+1);
        String strFile = HttpRequest.sendGet("http://" + strIpAddress
                        + ":8000/ajax/lib/" + strRootId + "/dir/",
                "p=" + strParentPath + "&thumbnail_size=48&&_=14815507370953",
                strCookie);
        if(strFile.contains("\""+fileName+"\"")){
            //存在一个问题就是前缀的问题 csd先创建,到后来cs就默认为存在了
            return true;
        }
        return false;
    }
    public Handler myHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_COMPLETE_BACKUP:
                    btnSnapshot.setEnabled(true);
                    btnSnapshot.setText("备份");
                    Toast.makeText(SecondActivity.this,"数据成功备份到云端",Toast.LENGTH_SHORT).show();
                    break;
                case MSG_COMPLETE_SYNC:
                    btnSync.setEnabled(true);
                    btnSync.setText("同步");
                    Toast.makeText(SecondActivity.this,"微信数据同步完成",Toast.LENGTH_SHORT).show();
                    break;
                case MSG_BACKUP_FILE_INFO:
                    tvFileScanner.setText(msg.getData().getString("filePath"));
                    break;
                case MSG_NOT_SYNC:
                    Toast.makeText(SecondActivity.this,"云端不存在备份文件,请先备份",Toast.LENGTH_SHORT).show();
                    btnSync.setEnabled(true);
                    btnSync.setText("同步");
                    break;
            }
        }
    };
    boolean clickFlag = false;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK&&event.getAction()==KeyEvent.ACTION_DOWN) {
            if (clickFlag == false) {
                clickFlag = true;//第一次点击
                Toast.makeText(SecondActivity.this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                final Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        //计时器两秒后自动把clickFlag改为false
                        clickFlag = false;
                    }
                }, 2000);
                return true;
            } else {
                finish();
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
