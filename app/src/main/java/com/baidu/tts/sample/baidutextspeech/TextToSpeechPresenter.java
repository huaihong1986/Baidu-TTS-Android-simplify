package com.baidu.tts.sample.baidutextspeech;

import android.content.Context;
import android.text.TextUtils;

/**
 * Created by banketree
 * on 2018/11/30.
 */

public class TextToSpeechPresenter {

    private Context context;

    private volatile static ITextToSpeech iTextToSpeech; //静态  控制只输出一次
    private volatile Thread localThread = null;//控制当前线程，只创建一个线程

    public TextToSpeechPresenter(Context context) {
        this.context = context;
    }

    public void play(final String text) {
        if (TextUtils.isEmpty(text) || context == null || localThread != null)
            return;
        if (iTextToSpeech != null && iTextToSpeech.isSpeaking()) //正在播放 则不进行
            return;
        localThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {//记忆上次
                    if (iTextToSpeech != null) {
                        iTextToSpeech.start(text);
                        localThread = null;
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    release();
                }
//                优先系统，如果系统失败再科大讯飞
                try {
                    //其次 系统播放
                    iTextToSpeech = new BaiduTextToSpeech(context);
                    Thread.sleep(3000);//初始化 需要一定的时间
                    iTextToSpeech.start(text);
                    localThread = null;
                    return;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    release();
                }

                localThread = null;
            }
        });
        localThread.start();
    }

    public void release() {
        if (iTextToSpeech != null)
            iTextToSpeech.release();
        iTextToSpeech = null;
    }
}
