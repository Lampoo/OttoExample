package com.yuneec.otto;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.otto.ThreadEnforcer;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();

    public interface IBus {
        void register(Object object);
        void unregister(Object object);
        void post(Object object);
        void postSync(Object object);
    }

    private static final int MSG_REGISTER = 1;
    private static final int MSG_UNREGISTER = 2;
    private static final int MSG_POST = 3;

    private static final class OttoBus extends Thread implements IBus {
        private Looper mLooper = null;
        private OttoHandler mHandler = null;

        private static final class OttoHandler extends Handler {
            private Bus mBus;

            public OttoHandler() {
                super();
                mBus = new Bus(ThreadEnforcer.ANY);
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_REGISTER:
                        mBus.register(msg.obj);
                        break;
                    case MSG_UNREGISTER:
                        mBus.unregister(msg.obj);
                        break;
                    case MSG_POST:
                        mBus.post(msg.obj);
                        break;
                }
            }

            public void register(Object object) {
                mBus.register(object);
            }

            public void unregister(Object object) {
                mBus.unregister(object);
            }

            public void post(Object object) {
                mBus.post(object);
            }
        }

        public OttoBus(String name) {
            super(name);
            start();
            getLooper();
        }

        public OttoBus() {
            super("OttoBus");
            start();
            Looper looper = getLooper();
        }

        public Looper getLooper() {
            synchronized (this) {
                if (mLooper == null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {}
                }
            }
            return mLooper;
        }

        @Override
        public void run() {
            Looper.prepare();
            synchronized (this) {
                mLooper = Looper.myLooper();
                mHandler = new OttoHandler();
                notifyAll();
            }
            Looper.loop();
        }

       @Override
        public void register(Object object) {
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_REGISTER;
            msg.obj = object;
            msg.sendToTarget();
        }

        @Override
        public void unregister(Object object) {
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_UNREGISTER;
            msg.obj = object;
            msg.sendToTarget();
        }

        @Override
        public void post(Object object) {
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_POST;
            msg.obj = object;
            msg.sendToTarget();
        }

        @Override
        public void postSync(Object object) {
            mHandler.post(object);
        }
    }

    private OttoBus mBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate");

        mBus = new OttoBus("BUS");
        mBus.register(this);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        produceStringEventSync("onPause");
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        produceStringEvent("onResume");
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
        produceStringEvent("onStop");
    }

    public void produceStringEvent(String event) {
        mBus.post(event);
    }

    public void produceStringEventSync(String event) {
        mBus.postSync(event);
    }

    @Subscribe
    public void onStringEvent(String event) {
        if (Looper.getMainLooper() == Looper.myLooper())
            Log.e(TAG, "MainLooper " + event);
        else
            Log.e(TAG, "MyLooper " + event);
    }
}
