package com.github.pao11.libffmpeg;

import android.content.Context;

/** Bridges the library's package-private response base type to the app. */
public final class FFmpegRunner {
    public interface Callback {
        void onSuccess();
        void onFailure(String message);
    }

    private FFmpegRunner() {
    }

    public static void execute(Context context, String[] arguments, Callback callback) {
        try {
            FFmpeg.getInstance(context).execute(arguments, new FFmpegExecuteResponseHandler() {
                @Override
                public void onSuccess(String message) {
                    callback.onSuccess();
                }

                @Override
                public void onFailure(String message) {
                    callback.onFailure(message);
                }

                @Override
                public void onProgress(String message) {
                }

                @Override
                public void onStart() {
                }

                @Override
                public void onFinish() {
                }
            });
        } catch (Exception error) {
            callback.onFailure(error.getMessage() == null ? "No se pudo iniciar FFmpeg" : error.getMessage());
        }
    }
}
