package com.pili.pldroid.streaming.camera.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.avos.avoscloud.*;
import org.w3c.dom.Text;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final String url = "http://pili-publish.trysenz.com/bus1?key=0903a8b2-09da-4ea6-b1da-73d9d3f1d5a2.";

    private static boolean isSupportHWEncode() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    private static String streamConfig;

    private String requestStreamJson() {
        try {
//            HttpURLConnection httpConn = (HttpURLConnection) new URL(url).openConnection();
//            httpConn.setRequestMethod("POST");
//            httpConn.setConnectTimeout(5000);
//            httpConn.setReadTimeout(10000);
//            int responseCode = httpConn.getResponseCode();
//            if (responseCode != HttpURLConnection.HTTP_OK) {
//                return null;
//            }
//
//            int length = httpConn.getContentLength();
//            if (length <= 0) {
//                return null;
//            }
//            InputStream is = httpConn.getInputStream();
//            byte[] data = new byte[length];
//            int read = is.read(data);
//            is.close();
//            if (read <= 0) {
//                return null;
//            }
//            return new String(data, 0, read);
            return streamConfig;
//            return "{\"id\":\"z1.schoolbus.572b1017eb6f9259de0004fd\",\"createdAt\":\"2016-05-05T09:19:19.697Z\",\"updatedAt\":\"2016-05-05T09:19:19.697Z\",\"title\":\"572b1017eb6f9259de0004fd\",\"hub\":\"schoolbus\",\"publishKey\":\"0903a8b2-09da-4ea6-b1da-73d9d3f1d5a2\",\"publishSecurity\":\"static\",\"disabled\":false,\"profiles\":null,\"hosts\":{\"publish\":{\"rtmp\":\"pili-publish.trysenz.com\"},\"live\":{\"hdl\":\"pili-live-hdl.trysenz.com\",\"hls\":\"pili-live-hls.trysenz.com\",\"http\":\"pili-live-hls.trysenz.com\",\"rtmp\":\"pili-live-rtmp.trysenz.com\"},\"playback\":{\"hls\":\"pili-playback.trysenz.com\",\"http\":\"pili-playback.trysenz.com\"}}}";
        } catch (Exception e) {
            showToast("Network error!");
        }
        return null;
    }

    void showToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startStreamingActivity(final Intent intent) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String resByHttp = null;

                if (!Config.DEBUG_MODE) {
                    resByHttp = requestStreamJson();
                    Log.i(TAG, "resByHttp:" + resByHttp);
                    if (resByHttp == null) {
                        showToast("Stream Json Got Fail!");
                        return;
                    }
                    intent.putExtra(Config.EXTRA_KEY_STREAM_JSON, resByHttp);
                } else {
                    showToast("Stream Json Got Fail!");
                }

                startActivity(intent);
            }
        }).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AVOSCloud.initialize(this, "hbrtHhNAuaNyp1CJv8sakCfd-gzGzoHsz", "EdTz7FcmochYrRkJpE2JIXBL");
        AVInstallation.getCurrentInstallation().saveInBackground();

//        Button mHWCodecCameraStreamingBtn = (Button) findViewById(R.id.hw_codec_camera_streaming_btn);
//        if (!isSupportHWEncode()) {
//            mHWCodecCameraStreamingBtn.setVisibility(View.INVISIBLE);
//        }
//        mHWCodecCameraStreamingBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(MainActivity.this, HWCodecCameraStreamingActivity.class);
//                startStreamingActivity(intent);
//            }
//        });

        final Button mSWCodecCameraStreamingBtn = (Button) findViewById(R.id.sw_codec_camera_streaming_btn);
        mSWCodecCameraStreamingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SWCodecCameraStreamingActivity.class);
                startStreamingActivity(intent);
            }
        });

//        Button mAudioStreamingBtn = (Button) findViewById(R.id.start_pure_audio_streaming_btn);
//        mAudioStreamingBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(MainActivity.this, AudioStreamingActivity.class);
//                startStreamingActivity(intent);
//            }
//        });

        final Button mFetchFromServerBtn = (Button) findViewById(R.id.fetch_stream_from_server);
        final TextView configText = (TextView) findViewById(R.id.version_info);

        final FunctionCallback callback = new FunctionCallback() {
            @Override
            public void done(Object o, AVException e) {
                if (e == null) {
                    mSWCodecCameraStreamingBtn.setEnabled(true);
                    mFetchFromServerBtn.setEnabled(false);
                    configText.setText(streamConfig);
                } else {
                    mSWCodecCameraStreamingBtn.setEnabled(false);
                    mFetchFromServerBtn.setEnabled(true);
                }
            }
        };

        mFetchFromServerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchStreamConfigFromServer(MainActivity.this, callback);
            }
        });

        streamConfig = readStreamConfig(this);
        if (streamConfig == null) {
            // fetch from cloud
            Toast.makeText(this, "fetch from server", Toast.LENGTH_LONG).show();
            fetchStreamConfigFromServer(MainActivity.this, callback);
        } else {
            Toast.makeText(this, "Load from local", Toast.LENGTH_LONG).show();
            callback.done(streamConfig, null);
        }
    }

    public void fetchStreamConfigFromServer(final Context context, final FunctionCallback callback) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("installation_id", AVInstallation.getCurrentInstallation().getInstallationId());
        AVCloud.callFunctionInBackground("stream", params, new FunctionCallback<Object>() {
            @Override
            public void done(Object o, AVException e) {
                if (e == null) {
                    streamConfig = o.toString();
                    Toast.makeText(context, "成功," + o.toString(), Toast.LENGTH_LONG).show();
                    writeStreamConfig(MainActivity.this, streamConfig);
                    callback.done(o.toString(), null);
                } else {
                    Toast.makeText(context, "获取流配置失败," + e.getMessage(), Toast.LENGTH_LONG).show();
                    callback.done(null, e);
                }
            }
        });
    }

    public String readStreamConfig(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                "PILI_STREAM", Context.MODE_PRIVATE);
        String config = sharedPref.getString("CONFIG", null);
        return config;
    }

    public void writeStreamConfig(Context context, String conf) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                "PILI_STREAM", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("CONFIG", conf);
        editor.commit();
    }
}
