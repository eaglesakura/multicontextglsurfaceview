# Readme

## MultiContextGLSurfaceViewについて

MultiContextGLSurfaceViewは、GLSurfaceViewを拡張して複数スレッドからOpenGL ESのコマンドを呼び出せるようにしたViewです。

Android 2.2以上で動作します。

MainActivityに非同期でテクスチャを読み込むサンプルがあります。動作確認は（シェーダー書くのがメンドウだったので）今のところOpenGL ES 1.1だけです。


MultiContextGLSurfaceView#requestAsyncGLEvent()にRunnableを投げると非同期でGLの処理を行えますが、描画は行えません。shared_contextを利用して別なEGLContextを生成しているので、StateはGLのメインスレッド（GLSurfaceViewが作ったスレッド）とは共有されません。

## 非同期に生成できるリソース

* Texture
* Shader
* Program
* FrameBuffer

=============


## ソースコードライセンス

* ソースコードは自由に利用してもらって構いません。
* NYSLに従います。

<pre>
A. 本ソフトウェアは Everyone'sWare です。このソフトを手にした一人一人が、
   ご自分の作ったものを扱うのと同じように、自由に利用することが出来ます。

  A-1. フリーウェアです。作者からは使用料等を要求しません。
  A-2. 有料無料や媒体の如何を問わず、自由に転載・再配布できます。
  A-3. いかなる種類の 改変・他プログラムでの利用 を行っても構いません。
  A-4. 変更したものや部分的に使用したものは、あなたのものになります。
       公開する場合は、あなたの名前の下で行って下さい。

B. このソフトを利用することによって生じた損害等について、作者は
   責任を負わないものとします。各自の責任においてご利用下さい。

C. 著作者人格権は @eaglesakura に帰属します。著作権は放棄します。

D. 以上の３項は、ソース・実行バイナリの双方に適用されます。
</pre>

### LICENSE(en)

<pre>

A. This software is "Everyone'sWare". It means:
  Anybody who has this software can use it as if he/she is
  the author.

  A-1. Freeware. No fee is required.
  A-2. You can freely redistribute this software.
  A-3. You can freely modify this software. And the source
      may be used in any software with no limitation.
  A-4. When you release a modified version to public, you
      must publish it with your name.

B. The author is not responsible for any kind of damages or loss
  while using or misusing this software, which is distributed
  "AS IS". No warranty of any kind is expressed or implied.
  You use AT YOUR OWN RISK.

C. Copyrighted to @eaglesakura

D. Above three clauses are applied both to source and binary
  form of this software.

</pre>