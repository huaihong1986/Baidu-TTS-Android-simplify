package com.baidu.tts.sample.baidutextspeech;

import android.content.Context;
import android.text.TextUtils;

import com.baidu.tts.client.SpeechError;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;

/*
 * 百度语音合成
 * */
class BaiduTextToSpeech implements ITextToSpeech {
    private Context context;

    // 语音合成对象
    private SpeechSynthesizer speechSynthesizer;
    protected boolean isReady = false;

    protected boolean isFinish = true;

    public BaiduTextToSpeech(Context context) {
        this.context = context;
        init();
    }

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
        }

        @Override
        public void onSpeechProgressChanged(String s, int i) {

        }

        @Override
        public void onSpeechFinish(String s) {
            isFinish = true;
        }

        @Override
        public void onError(String s, SpeechError speechError) {
            isFinish = true;
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

}
