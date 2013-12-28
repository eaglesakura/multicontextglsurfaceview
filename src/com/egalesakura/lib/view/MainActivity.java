package com.egalesakura.lib.view;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.util.Log;

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

    Bitmap loadImage() {
        return BitmapFactory.decodeResource(getResources(), R.drawable.cat);
    }

    private GLSurfaceView.Renderer rendererGL11 = new Renderer() {
        int asyncLoadedTextureId = 0;

        Buffer wrap(float[] buffer) {
            ByteBuffer bb = ByteBuffer.allocateDirect(buffer.length * 4);
            bb.order(ByteOrder.nativeOrder());
            FloatBuffer fb = bb.asFloatBuffer();
            fb.put(buffer);
            fb.position(0);
            return fb;
        }

        @Override
        public void onSurfaceCreated(final GL10 gl, EGLConfig config) {

            Runnable loadEvent = new Runnable() {
                @Override
                public void run() {
                    // async GL Thread

                    try {
                        // test sleep 
                        Thread.sleep(5000);
                    } catch (Exception e) {

                    }

                    int[] tex = new int[1];
                    gl.glGenTextures(1, tex, 0);

                    final int texId = tex[0];
                    gl.glEnable(GL10.GL_TEXTURE_2D);
                    gl.glBindTexture(GL10.GL_TEXTURE_2D, texId);
                    GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, loadImage(), 0);
                    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
                    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);

                    asyncLoadedTextureId = texId;

                    Log.i("Async", "Texture Loaded :: " + texId);
                }
            };

            //            view.queueEvent(loadEvent); // Freeze GL Thread...
            view.requestAsyncGLEvent(loadEvent); // Not Freeze GL Thread !!
        }

        private int screenWidth;
        private int screenHeight;

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            this.screenWidth = width;
            this.screenHeight = height;
            gl.glViewport(0, 0, width, height);
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
            gl.glClearColor(0, 1, (float) Math.random(), 1);
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

            if (asyncLoadedTextureId != 0) {
                // async load completed
                gl.glEnable(GL10.GL_TEXTURE_2D);
                gl.glBindTexture(GL10.GL_TEXTURE_2D, asyncLoadedTextureId);

                float uv[] = {
                        0.0f, 0.0f, // !< 左上
                        0.0f, 1.0f, // !< 左下
                        1.0f, 0.0f, // !< 右上
                        1.0f, 1.0f, // !< 右下
                };

                gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
                gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, wrap(uv));

            }
            drawQuad(gl, 0, 0, 512, 512);
        }
    };
}
