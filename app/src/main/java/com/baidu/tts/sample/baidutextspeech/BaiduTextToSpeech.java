package com.baidu.tts.sample.baidutextspeech;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.tts.auth.AuthInfo;
import com.baidu.tts.client.SpeechError;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;
import com.baidu.tts.sample.util.OfflineResource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;

/*
 * 百度语音合成
 * */
class BaiduTextToSpeech implements ITextToSpeech {
    private Context context;

    // 语音合成对象
    private SpeechSynthesizer speechSynthesizer;
    protected boolean isReady = false;

    protected boolean isFinish = true;
    // TtsMode.MIX; 离在线融合，在线优先； TtsMode.ONLINE 纯在线； 没有纯离线
    private TtsMode ttsMode = TtsMode.MIX;
    private String appId = "11005757", appKey = "Ovcz19MGzIKoDDb3IsFFncG1", secretKey = "e72ebb6d43387fc7f85205ca7e6706e2";
    protected String offlineVoice = OfflineResource.VOICE_FEMALE;
    OfflineResource offlineResource;

    public BaiduTextToSpeech(Context context) {
        this.context = context;
        initPermission();
//        init();
        initTTs();
    }

    //  下面是android 6.0以上的动态授权

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String[] permissions = {
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_SETTINGS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(context, perm)) {
                toApplyList.add(perm);
                // 进入到这里代表没有权限.
            }
        }
        String[] tmpList = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions((Activity) context, toApplyList.toArray(tmpList), 123);
        }

    }

    //    在线模式下
    private void init() {
        speechSynthesizer = SpeechSynthesizer.getInstance();       // 初始化合成对象
        speechSynthesizer.setContext(context);
        speechSynthesizer.setAppId("11005757"/*这里只是为了让Demo运行使用的APPID,请替换成自己的id。*/);
        speechSynthesizer.setApiKey("Ovcz19MGzIKoDDb3IsFFncG1", "e72ebb6d43387fc7f85205ca7e6706e2");
        speechSynthesizer.auth(TtsMode.ONLINE);  // 纯在线
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "0"); // 设置发声的人声音，在线生效
        int result = speechSynthesizer.initTts(TtsMode.ONLINE);
        if (result == 0) {
            isReady = true;
        }
        speechSynthesizer.setSpeechSynthesizerListener(initListener);
    }

    /**
     * 注意此处为了说明流程，故意在UI线程中调用。
     * 实际集成中，该方法一定在新线程中调用，并且该线程不能结束。具体可以参考NonBlockSyntherizer的写法
     */
    private void initTTs() {
        boolean isMix = ttsMode.equals(TtsMode.MIX);
        boolean isSuccess;
        offlineResource = createOfflineResource(offlineVoice);
        if (isMix) {
            // 检查2个离线资源是否可读
            isSuccess = checkOfflineResources();
            if (!isSuccess) {
                return;
            } else {
            }
        }

        // 1. 获取实例
        speechSynthesizer = SpeechSynthesizer.getInstance();
        speechSynthesizer.setContext(context);

        // 2. 设置listener
        speechSynthesizer.setSpeechSynthesizerListener(initListener);

        // 3. 设置appId，appKey.secretKey
        speechSynthesizer.setAppId(appId);
        speechSynthesizer.setApiKey(appKey, secretKey);

        // 4. 支持离线的话，需要设置离线模型
        if (isMix) {
            // 检查离线授权文件是否下载成功，离线授权文件联网时SDK自动下载管理，有效期3年，3年后的最后一个月自动更新。
            isSuccess = checkAuth();
            if (!isSuccess) {
                return;
            }
            // 文本模型文件路径 (离线引擎使用)， 注意TEXT_FILENAME必须存在并且可读
            speechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, offlineResource.getTextFilename());
            // 声学模型文件路径 (离线引擎使用)， 注意TEXT_FILENAME必须存在并且可读
            speechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE, offlineResource.getModelFilename());
        }

        // 5. 以下setParam 参数选填。不填写则默认值生效
        // 设置在线发声音人： 0 普通女声（默认） 1 普通男声 2 特别男声 3 情感男声<度逍遥> 4 情感儿童声<度丫丫>
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "0");
        // 设置合成的音量，0-9 ，默认 5
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "9");
        // 设置合成的语速，0-9 ，默认 5
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, "5");
        // 设置合成的语调，0-9 ，默认 5
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, "5");

        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_DEFAULT);
        // 该参数设置为TtsMode.MIX生效。即纯在线模式不生效。
        // MIX_MODE_DEFAULT 默认 ，wifi状态下使用在线，非wifi离线。在线状态下，请求超时6s自动转离线
        // MIX_MODE_HIGH_SPEED_SYNTHESIZE_WIFI wifi状态下使用在线，非wifi离线。在线状态下， 请求超时1.2s自动转离线
        // MIX_MODE_HIGH_SPEED_NETWORK ， 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
        // MIX_MODE_HIGH_SPEED_SYNTHESIZE, 2G 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线

        speechSynthesizer.setAudioStreamType(AudioManager.MODE_IN_CALL);

        // x. 额外 ： 自动so文件是否复制正确及上面设置的参数
        Map<String, String> params = new HashMap<>();
        // 离线资源文件， 从assets目录中复制到临时目录，需要在initTTs方法前完成


        // 复制下上面的 speechSynthesizer.setParam参数
        // 上线时请删除AutoCheck的调用
        if (isMix) {
            // 声学模型文件路径 (离线引擎使用), 请确认下面两个文件存在
            params.put(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, offlineResource.getTextFilename());
            params.put(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE,
                    offlineResource.getModelFilename());
        }
        // 6. 初始化
        int result = speechSynthesizer.initTts(ttsMode);
        if (result == 0) {
            isReady = true;
        }
    }


    protected OfflineResource createOfflineResource(String voiceType) {
        OfflineResource offlineResource = null;
        try {
            offlineResource = new OfflineResource(context, voiceType);
        } catch (IOException e) {
            // IO 错误自行处理
            e.printStackTrace();
            print("【error】:copy files from assets failed." + e.getMessage());
        }
        return offlineResource;
    }

    /**
     * 检查appId ak sk 是否填写正确，另外检查官网应用内设置的包名是否与运行时的包名一致。本demo的包名定义在build.gradle文件中
     *
     * @return
     */
    private boolean checkAuth() {
        AuthInfo authInfo = speechSynthesizer.auth(ttsMode);
        if (!authInfo.isSuccess()) {
            // 离线授权需要网站上的应用填写包名。本demo的包名是com.baidu.tts.sample，定义在build.gradle中
            String errorMsg = authInfo.getTtsError().getDetailMessage();
            print("【error】鉴权失败 errorMsg=" + errorMsg);
            return false;
        } else {
            print("验证通过，离线正式授权文件存在。");
            return true;
        }
    }

    /**
     * 检查 TEXT_FILENAME, MODEL_FILENAME 这2个文件是否存在，不存在请自行从assets目录里手动复制
     *
     * @return
     */
    private boolean checkOfflineResources() {
        String[] filenames = {offlineResource.getTextFilename(), offlineResource.getModelFilename()};
        for (String path : filenames) {
            File f = new File(path);
            if (!f.canRead()) {
                print("[ERROR] 文件不存在或者不可读取，请从assets目录复制同名文件到：" + path);
                print("[ERROR] 初始化失败！！！");
                return false;
            }
        }
        return true;
    }

    /**
     * 初始化监听。
     */
    private SpeechSynthesizerListener initListener = new SpeechSynthesizerListener() {
        @Override
        public void onSynthesizeStart(String s) {

        }

        @Override
        public void onSynthesizeDataArrived(String s, byte[] bytes, int i) {

        }

        @Override
        public void onSynthesizeFinish(String s) {

        }

        @Override
        public void onSpeechStart(String s) {
            isFinish = false;
            print("data=onSpeechStart");
        }

        @Override
        public void onSpeechProgressChanged(String s, int i) {

        }

        @Override
        public void onSpeechFinish(String s) {
            isFinish = true;
            print("data=onSpeechFinish");
        }

        @Override
        public void onError(String s, SpeechError speechError) {
            isFinish = true;
            print("data=onError" + speechError.description);
        }
    };

    @Override
    public void start(String text) throws Exception {
        if (TextUtils.isEmpty(text)) return;
        if (!isReady || speechSynthesizer == null) {
            throw new Exception("初始化失败！");
        }
        if (isSpeaking()) return; //正在播放 则不进行
        int code = speechSynthesizer.speak(text);
        if (code != 0) { //语音合成失败,错误码
            throw new Exception("语音合成失败！" + code);
        }
    }

    @Override
    public void stop() {
        if (null != speechSynthesizer) {
            speechSynthesizer.stop();
        }
    }

    @Override
    public boolean isSpeaking() {
        return speechSynthesizer != null && !isFinish;
    }

    @Override
    public void release() {
        stop();
        if (null != speechSynthesizer) {
            speechSynthesizer.release(); // 退出时释放连接
        }
        speechSynthesizer = null;
    }


    private void print(String message) {
        Log.i(TAG, message);
    }
}
