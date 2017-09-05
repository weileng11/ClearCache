package com.pythoncat.clearcache;

import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvShowCaches, tvAppCache;
    private Button btnScanCache, btnClearAll;

    private PackageManager pm;
    StringBuilder sb = new StringBuilder();
    StringBuilder sbCache = new StringBuilder();

    private long cacheS;
    Handler mHadler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnScanCache = (Button) findViewById(R.id.btn_scanCache);
        btnClearAll = (Button) findViewById(R.id.btn_clearAll);
        tvShowCaches = (TextView) findViewById(R.id.tv_showAppInfo);
        tvAppCache = (TextView) findViewById(R.id.tv_appCache);
        sbCache.append("所有缓存：\n");
        tvAppCache.setText(sbCache.toString());
        btnScanCache.setOnClickListener(this);
        btnClearAll.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        cacheS = 0;
        if (v.getId() == btnScanCache.getId()) {
            getCaches();
//            ==========获取每个app的缓存
        } else if (v.getId() == btnClearAll.getId()) {
            cleanAll(v);
            getCaches();
        }
    }

    class MyPackageStateObserver extends IPackageStatsObserver.Stub {

        @Override
        public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) throws RemoteException {
            String packageName = pStats.packageName;
            long cacheSize = pStats.cacheSize;
            long codeSize = pStats.codeSize;
            long dataSize = pStats.dataSize;
            cacheS += cacheSize;
//            sb.delete(0, sb.length());
            if (cacheSize > 0) {
                sb.append("packageName = " + packageName + "\n")
                        .append("   cacheSize: " + cacheSize + "\n")
                        .append("   dataSize: " + dataSize + "\n")
                        .append("-----------------------\n")
                ;

                Log.e("aaaa", sb.toString());
            }

        }
    }


    class ClearCacheObj extends IPackageDataObserver.Stub {

        @Override
        public void onRemoveCompleted(String packageName, final boolean succeeded) throws RemoteException {
            mHadler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "清楚状态： " + succeeded, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * 清理全部应用程序缓存的点击事件
     *
     * @param view
     */
    public void cleanAll(View view) {
        //freeStorageAndNotify
        Method[] methods = PackageManager.class.getMethods();
        for (Method method : methods) {
            if ("freeStorageAndNotify".equals(method.getName())) {
                try {
                    method.invoke(pm, Long.MAX_VALUE, new ClearCacheObj());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
        }
    }

    private void getCaches(){
        // scan
        pm = getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0);

        int max = packages.size();
        int current = 0;
        sb.delete(0, sb.length());
        sb.append("所有已安装的app信息：\n");
        sb.append("所有App 总和：" + max + " \n");
        tvShowCaches.setText(sb.toString());
        for (PackageInfo pinfo : packages) {
            String packageName = pinfo.packageName;
            try {

                Method getPackageSizeInfo = PackageManager.class
                        .getDeclaredMethod("getPackageSizeInfo", String.class, IPackageStatsObserver.class);
                getPackageSizeInfo.invoke(pm, packageName, new MyPackageStateObserver());
                current++;
            } catch (Exception e) {
                current++;
                e.printStackTrace();
            }

        }
        //===到这里，数据准备完成
        mHadler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),"缓存信息获取完成",Toast.LENGTH_SHORT).show();
                sbCache.append(Formatter.formatFileSize(getApplicationContext(),cacheS)+"\n");
                tvShowCaches.setText(sb.toString());
                tvAppCache.setText(sbCache.toString());
                sbCache.delete(0,sbCache.length());
            }
        }, 1000);
        //ok,所有应用程序信息显示完成
    }
}
