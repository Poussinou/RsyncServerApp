package com.github.ktsr42.rsyncserver;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.github.ktsr42.yajsynclib.LibServer;

import java.io.IOException;
import java.net.InetAddress;

public class RsyncReceiver extends Service {
    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        private LibServer srv;

        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            if(srv != null) return;

            srv = new LibServer((String)msg.obj, msg.arg1);
            try {
                srv.initServer(InetAddress.getLocalHost());
                srv.run();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static final String TGT_MODULE_NAME = "targetModuleName";
    public static final String TGT_PORT = "targetPort";

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        // either fork of new thread here or in onStartCommand()
        // need to send the port number back and forth
        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work doesn't disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "RsyncReceiver service starting", Toast.LENGTH_SHORT).show();

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();
        msg.obj = intent.getStringExtra(TGT_MODULE_NAME);
        msg.arg1 = intent.getIntExtra(TGT_PORT, 0);
        serviceHandler.sendMessage(msg);

        // Do not restart this if we get killed after returning from here
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "RsyncReceiver service stopping.", Toast.LENGTH_SHORT).show();
    }
}

