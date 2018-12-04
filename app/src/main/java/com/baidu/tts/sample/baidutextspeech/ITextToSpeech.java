package com.baidu.tts.sample.baidutextspeech;

public interface ITextToSpeech {

    public void start(String text) throws Exception;

    public void stop();

    public boolean isSpeaking();

    public void release();
}
