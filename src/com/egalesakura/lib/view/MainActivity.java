package com.egalesakura.lib.view;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Bundle;
import android.util.Log;

import com.egalesakura.lib.view.MultiContextGLSurfaceView.GLRunnable;

public class MainActivity extends Activity {

    MultiContextGLSurfaceView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        view = new MultiContextGLSurfaceView(this);

        {
            view.setEGLContextClientVersion(1);
            view.setRenderer(rendererGL11);
        }

        setContentView(view);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        view.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        view.onResume();
    }

    Bitmap loadImage(int drawable) {
        return BitmapFactory.decodeResource(getResources(), drawable);
    }

    private GLSurfaceView.Renderer rendererGL11 = new Renderer() {
        int asyncLoadedTextureId0 = 0;
        int asyncLoadedTextureId1 = 0;

        Buffer wrap(float[] buffer) {
            ByteBuffer bb = ByteBuffer.allocateDirect(buffer.length * 4);
            bb.order(ByteOrder.nativeOrder());
            FloatBuffer fb = bb.asFloatBuffer();
            fb.put(buffer);
            fb.position(0);
            return fb;
        }

        Object contextLock = new Object();

        void texImage2D(GL10 gl, Bitmap image) {
            final int image_width = image.getWidth();
            final int image_height = image.getHeight();

            // ピクセル情報の格納先
            ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(image_width * image_height * 4);
            Log.d("RawPixelImage", String.format("image size(%d x %d)", image_width, image_height));

            final int[] temp = new int[image_width];
            final byte[] pixel_temp = new byte[4];
            for (int i = 0; i < image_height; ++i) {
                // 1ラインずつ読み込む
                image.getPixels(temp, 0, image_width, 0, i, image_width, 1);
                // 結果をByteArrayへ書き込む
                for (int k = 0; k < image_width; ++k) {
                    final int pixel = temp[k];

                    pixel_temp[0] = (byte) ((pixel >> 16) & 0xFF);
                    pixel_temp[1] = (byte) ((pixel >> 8) & 0xFF);
                    pixel_temp[2] = (byte) ((pixel) & 0xFF);
                    pixel_temp[3] = (byte) ((pixel >> 24) & 0xFF);

                    pixelBuffer.put(pixel_temp);
                }
            }

            // 書き込み位置をリセットする
            pixelBuffer.position(0);

            gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGBA, image.getWidth(), image.getHeight(), 0, GL10.GL_RGBA,
                    GL10.GL_UNSIGNED_BYTE, pixelBuffer);
        }

        void sleep(int ms) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {

            }
        }

        @Override
        public void onSurfaceCreated(final GL10 gl, EGLConfig config) {

        }

        private int screenWidth;
        private int screenHeight;

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            this.screenWidth = width;
            this.screenHeight = height;
            gl.glViewport(0, 0, width, height);

            if (asyncLoadedTextureId0 != 0) {
                return;
            }

            {
                GLRunnable loadEvent = new GLRunnable() {
                    @Override
                    public void run(EGLContext slave, GL10 gl) {

                        sleep(1000);

                        // async GL Thread
                        int[] tex = new int[1];
                        gl.glGenTextures(1, tex, 0);

                        final int texId = tex[0];
                        gl.glBindTexture(GL10.GL_TEXTURE_2D, texId);
                        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
                        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
                        texImage2D(gl, loadImage(R.drawable.cat));
                        gl.glFinish();

                        asyncLoadedTextureId1 = texId;

                        Log.i("Async", "Texture Loaded :: " + texId);
                    }
                };

                //            view.queueEvent(loadEvent); // Freeze GL Thread...
                view.requestAsyncGLEvent(loadEvent); // Not Freeze GL Thread !!
            }

            {
                GLRunnable loadEvent = new GLRunnable() {
                    @Override
                    public void run(EGLContext slave, GL10 gl) {
                        sleep(500);

                        // async GL Thread
                        int[] tex = new int[1];
                        gl.glGenTextures(1, tex, 0);

                        final int texId = tex[0];
                        gl.glBindTexture(GL10.GL_TEXTURE_2D, texId);
                        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
                        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
                        texImage2D(gl, loadImage(R.drawable.cow));
                        gl.glFinish();

                        asyncLoadedTextureId0 = texId;

                        Log.i("Async", "Texture Loaded :: " + texId);

                    }
                };

                //            view.queueEvent(loadEvent); // Freeze GL Thread...
                view.requestAsyncGLEvent(loadEvent); // Not Freeze GL Thread !!
            }

        }

        private void drawQuad(GL10 gl10, int x, int y, int w, int h) {
            float left = ((float) x / (float) screenWidth) * 2.0f - 1.0f;
            float top = ((float) y / (float) screenHeight) * 2.0f - 1.0f;
            float right = left + ((float) w / (float) screenWidth) * 2.0f;
            float bottom = top + ((float) h / (float) screenHeight) * 2.0f;
            // ! 上下を反転させる
            top = -top;
            bottom = -bottom;
            // ! 位置情報
            float positions[] = { // ! x y z
                    left, top, 0.0f, //
                    left, bottom, 0.0f, // !< 左下 
                    right, top, 0.0f, // !< 右上 
                    right, bottom, 0.0f, // !< 右下
            };
            gl10.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl10.glVertexPointer(3, GL10.GL_FLOAT, 0, wrap(positions));
            gl10.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
        }

        @Override
        public void onDrawFrame(GL10 gl) {

            if (asyncLoadedTextureId0 == 0 && asyncLoadedTextureId1 == 0) {
                gl.glClearColor(0, 1, (float) Math.random(), 1);
                gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
                return;
            }

            gl.glClearColor(0, 1, 1, 1);
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
            gl.glEnable(GL10.GL_TEXTURE_2D);

            final Buffer uvBuffer;
            {
                // UV座標設定
                final float uv[] = {
                        0.0f, 0.0f, // !< 左上
                        0.0f, 1.0f, // !< 左下
                        1.0f, 0.0f, // !< 右上
                        1.0f, 1.0f, // !< 右下
                };

                gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
                uvBuffer = wrap(uv);
                gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, uvBuffer);
            }

            if (asyncLoadedTextureId0 != 0) {
                // async load completed cat
                gl.glBindTexture(GL10.GL_TEXTURE_2D, asyncLoadedTextureId0);
                drawQuad(gl, 0, 0, 256, 256);
            }

            if (asyncLoadedTextureId1 != 0) {
                // async load completed cow
                gl.glBindTexture(GL10.GL_TEXTURE_2D, asyncLoadedTextureId1);
                drawQuad(gl, 256, 0, 256, 256);
            }
        }
    };
}
