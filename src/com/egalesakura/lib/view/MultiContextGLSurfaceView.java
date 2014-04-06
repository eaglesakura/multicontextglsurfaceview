package com.egalesakura.lib.view;

import static javax.microedition.khronos.egl.EGL10.*;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

public class MultiContextGLSurfaceView extends GLSurfaceView {

    /**
     * EGLContextの参照数
     */
    private int mEGLContextRef = 0;

    /**
     * SharedContextのメイン用
     */
    private EGLContext mMasterContext = null;

    /**
     * EGLContext操作時のロック
     */
    private Object mEGLContextLock = new Object();

    /**
     * def OpenGL ES 1.x
     */
    private int mEglClientVersion = 1;

    private EGL10 mEGL = null;

    private EGLConfig mEGLConfig = null;

    private EGLDisplay mEGLDisplay = null;

    public MultiContextGLSurfaceView(Context context) {
        super(context);
    }

    public MultiContextGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * このメソッドは必ず呼び出されるため、加工して独自Factoryをセットアップする
     */
    @Override
    public void setRenderer(Renderer renderer) {
        setEGLContextFactory(mEGLContextFactory);
        super.setRenderer(renderer);
    }

    /**
     * set OpenGL ES Version.
     */
    @Override
    public void setEGLContextClientVersion(int version) {
        super.setEGLContextClientVersion(version);
        this.mEglClientVersion = version;
    }

    /**
     * GL動作を非同期で行う
     * Textureの読み込み等を想定
     * @param event
     */
    public void requestAsyncGLEvent(final GLRunnable event) {
        Thread thread = (new Thread() {
            @Override
            public void run() {
                // initialize EGL async device.
                final EGLDisplay display = mEGLDisplay;
                final EGLContext context = newSlaveContext();
                Log.d("EGL", "context id :: " + context);
                final EGLSurface surface = newDummySurface();

                try {
                    mEGL.eglMakeCurrent(display, surface, surface, context);

                    // call event
                    event.run(context, (GL10) context.getGL());
                } finally {
                    mEGL.eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);

                    destroySlaveContext(context);
                    destroyDummySurface(surface);
                }
            }
        });

        thread.setName("GL-Background");
        thread.start();
    }

    /**
     * 用途に合わせたEGLContext attributeを生成する
     * @return
     */
    private int[] getContextAttributes() {
        if (mEglClientVersion == 1) {
            return new int[] {
                    0x3098 /* EGL_CONTEXT_CLIENT_VERSION */, 1 /* EGL_OPENGL_ES_BIT */, EGL_NONE
            };
        } else {
            return new int[] {
                    0x3098 /* EGL_CONTEXT_CLIENT_VERSION */, 4 /* EGL_OPENGL_ES2_BIT */, EGL_NONE
            };
        }
    }

    /**
     * マスターアクセス用Contextを取得する
     * @return
     */
    private EGLContext getMasterContext(EGL10 egl, EGLDisplay display, EGLConfig config) {
        synchronized (mEGLContextLock) {
            if (mMasterContext == null) {
                mMasterContext = egl.eglCreateContext(display, config, EGL_NO_CONTEXT, getContextAttributes());

                this.mEGL = egl;
                this.mEGLDisplay = display;
                this.mEGLConfig = config;
            }

            return mMasterContext;
        }
    }

    /**
     * EGLの参照数を追加する
     */
    private void retainEGL() {
        synchronized (mEGLContextLock) {
            ++mEGLContextRef;
        }
    }

    /**
     * EGLの参照数を減らす
     */
    private void releaseEGL() {
        synchronized (mEGLContextLock) {
            --mEGLContextRef;

            if (mEGLContextRef == 0) {
                // マスターContextが不要になった
                mEGL.eglDestroyContext(mEGLDisplay, mMasterContext);
                mMasterContext = null;
                Log.d("MCGLSV", "Destroy Master Context");
            }
        }
    }

    /**
     * eglMakeCurrent用のDummyを作成する
     * @return
     */
    public EGLSurface newDummySurface() {
        return mEGL.eglCreatePbufferSurface(mEGLDisplay, mEGLConfig, new int[] {
                //
                EGL_WIDTH, 1, EGL_HEIGHT, 1,
                //
                EGL_NONE,
        });
    }

    /**
     * eglMakeCurrent用のdummyを廃棄する
     * @param surface
     */
    public void destroyDummySurface(EGLSurface surface) {
        mEGL.eglDestroySurface(mEGLDisplay, surface);
    }

    /**
     * スレイブContextを生成する
     * @return
     */
    public EGLContext newSlaveContext() {
        synchronized (mEGLContextLock) {
            mEGL.eglInitialize(mEGLDisplay, new int[2]);

            EGLContext master = getMasterContext(mEGL, mEGLDisplay, mEGLConfig);
            EGLContext result = mEGL.eglCreateContext(mEGLDisplay, mEGLConfig, master, getContextAttributes());
            if (result == EGL_NO_CONTEXT) {
                throw new IllegalStateException("Not support shared_context");
            }
            retainEGL();
            return result;
        }
    }

    /**
     * スレイブContextを廃棄する
     * @param context
     */
    public void destroySlaveContext(EGLContext context) {
        synchronized (mEGLContextLock) {
            mEGL.eglDestroyContext(mEGLDisplay, context);
            releaseEGL();
            //            mEGL.eglTerminate(mEGLDisplay);
        }
    }

    /**
     * default factory
     */
    private EGLContextFactory mEGLContextFactory = new EGLContextFactory() {

        @Override
        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
            // EGL参照数を一つ下げる
            releaseEGL();
        }

        @Override
        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config) {
            EGLContext context = getMasterContext(egl, display, config);
            retainEGL();
            // マスターContextをそのまま帰す
            return context;
        }
    };

    public interface GLRunnable {
        void run(EGLContext slave, GL10 gl);
    }

}
