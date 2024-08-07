package com.skedgo.tripkit.ui.utils;

import android.os.Handler;
import android.os.Looper;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import io.reactivex.functions.Consumer;

/**
 * A custom {@link Bus} that posts events from any thread and
 * lets subscribers receive them on the main thread.
 */
public class MainThreadBus extends Bus {
    private final Consumer<Throwable> errorHandler;

    /**
     * A Handler used to communicate with the main thread.
     */
    private Handler handler = new Handler(Looper.getMainLooper());

    public MainThreadBus(Consumer<Throwable> errorHandler) {
        super(ThreadEnforcer.ANY);
        this.errorHandler = errorHandler;
    }

    /**
     * Posts an event and expects to handle it on the main thread.
     *
     * @param event The event that we want to post.
     */
    @Override
    public final void post(final Object event) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // We're on the main thread.
            super.post(event);
        } else {
            // The operation inside run() will be called on the main thread.
            handler.post(new Runnable() {
                @Override
                public void run() {
                    MainThreadBus.super.post(event);
                }
            });
        }
    }
}
