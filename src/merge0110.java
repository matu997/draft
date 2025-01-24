// Todo: 得点管理について
// Todo: ノーツの速度と判定位置を変数にしたい
// Todo: KeyStatusの管理方法について（これTAに聞く。）
// Todo: 判定調整機能を実装。多分ノーツの描画位置をずらすだけでいいはず
// Todo: mainクラスに譜面読み込み機能を実装。CSVを一行ずつ読み込んでAddNoteしていく。
// Todo: なんか思ったより順調に作れてしまった。。。オリジナル要素が必要な気がする。

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.LineBorder; // LPanelの境界線を描画するために必要

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Array;
import java.util.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import javax.sound.sampled.*; // 音楽を流すためのパッケージ
import java.io.*; // CSVを読み込むためのパッケージ

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

abstract class Note {
    // 描画用の変数。centerX, centerYは中心座標、radiusは半径、colorは色。（のはずだけどX,Yが中心座標じゃない気もする）
    protected double centerX, centerY, radius = 10;
    protected Color color;
    protected Image image;

    protected int width = 150;
    protected int height = 50;

    // ゲーム用の変数
    protected double time; // timeは判定ラインに到達するまでの時間ms。定数。
    protected double offset = 1000; // offsetは判定ラインに到達するまでの残り時間ms。随時更新。

    public Note(double x, double y) {
        centerX = x;
        centerY = y;
    }

    public Note() {
        this(50, -1000); // 一旦描画範囲外に配置
    }

    public void setX(double x) {
        centerX = x;
    }

    public void setY(double y) {
        centerY = y;
    }

    public void setRadius(double r) {
        radius = r;
    }

    public void setTime(double t) {
        time = t;
    }

    public void setOffset(double o) {
        offset = o;
    }

    public double getOffset() {
        return offset;
    }
    public void setImage(String path) {
        this.image = new ImageIcon(path).getImage();
    }
    public void setPosition(double x, double y) {
        centerX = x;
        centerY = y;
    }

    public abstract void draw(Graphics g);
}

class TapNote extends Note {
    public TapNote(double x, double y) {
        super(x, y);
        setImage("images/tap_note.png"); // ノーツ画像を設定
    }

    // 引数なしのコンストラクタ
    public TapNote() {
        this(75, -1000); // デフォルトの座標を設定
    }

    public void draw(Graphics g) {
        if (image != null) {
            int drawX = (int) (centerX - width / 2);
            int drawY = (int) (centerY - height / 2);
            g.drawImage(image, drawX, drawY, width, height, null);
        } else {
            System.out.println("TapNote image is null");
        }
    }
}

class TraceNote extends Note {
    public TraceNote(double x, double y) {
        super(x, y);
        this.image = Toolkit.getDefaultToolkit().getImage("images/trace_note.png");
    }

    public TraceNote() {
        super();
        color = Color.BLUE;
    }

    public void draw(Graphics g) {
        g.setColor(color);
        g.fillOval((int) (centerX), (int) (centerY), (int) (2 * radius), (int) (2 * radius));
    }
}

///////////////////////////////////////////////
// Model
class NotesModel implements ActionListener {
    private ArrayList<Note> notes;
    Timer timer;
    double beginTime;
    int laneIndex;

    private ViewLane view;
    private double noteSpeedFactor = 0.75;

    public NotesModel(int laneIndex, ViewLane view) {
        notes = new ArrayList<Note>();
        this.laneIndex = laneIndex;
        this.view = view; // 正しいViewLaneを渡す
    }

    public ArrayList<Note> getNotes() {
        return notes;
    }

    public void addNote(double time, int type) {
        Note n = null;
        switch (type) {
            case 0:
                n = new TapNote();
                break;
            case 1:
                n = new TraceNote();
                break;
        }
        n.setTime(time);
        notes.add(n);
    }

    // ゲームスタートメソッド。ミリ秒で現在時刻を保存し、概ね60FPSになるように16ミリ秒ごとにタイマーを設定。
    public void gameStart(double t) {
        beginTime = t;
        timer = new Timer(16, this);
        timer.start(); 
        // timer.start(); // 16msごとにactionPerformedを呼び出す
    }

    // ゲームストップメソッド。タイマーを止める。描画の更新もストップ。
    public void gameStop() {
        timer.stop();
    }

    // Noteのoffsetを更新するメソッド。ついでにTraceNoteの判定も行う。
    public void noteUpdate(double currentTime) {
        for (Note n : notes) {
            n.setOffset(n.time - (currentTime - beginTime)); // ノーツのoffsetを更新
            int judgmentLineY = view.getJudgmentLineY();
            int laneHeight = view.getHeight();


            n.setY(judgmentLineY - laneHeight * noteSpeedFactor * n.getOffset() / 1000);

            if (Math.abs(n.getOffset()) <= 50 && n instanceof TraceNote) {
                // if (2 ^ laneIndex & keyStatus != 0) {
                // System.out.println("TraceNote judged!");
                // }
            }
        }
    }
    public void setView(ViewLane view) {
    this.view = view;
}

    // TapNoteの判定を行うメソッド
    public void tapJudge() {
        // double currentTime = System.currentTimeMillis(); //完全にデバッグ用
        // System.out.printf("now time: %fms\n", (currentTime - beginTime));

        // 変数listのIteratorを作成。要素を削除するためにはIteratorを使う必要がある。
        Iterator<Note> itr = notes.iterator();
        while (itr.hasNext()) {
            Note n = itr.next();
            if (Math.abs(n.offset) <= 50) {
                itr.remove();
                System.out.println("Perfect!!!!!!");
                break; // タイプ1回につきに一つのノーツしか判定しない。
            } else if (Math.abs(n.offset) <= 120) {
                itr.remove();
                System.out.println("Great!!!!");
                break; // タイプ1回につきに一つのノーツしか判定しない。
            } else if (Math.abs(n.offset) <= 300) {
                itr.remove();
                System.out.println("Miss....");
                break; // タイプ1回につきに一つのノーツしか判定しない。
            }
        }
        // System.out.printf("ラグは%f\n\n", System.currentTimeMillis() - currentTime);
    }

    // timerによって呼び出されるメソッド。
    public void actionPerformed(ActionEvent e) {
        // noteUpdate(); // ノートの位置を更新
        // setChanged(); // 描画を更新するためにフラグを立てる
        // notifyObservers(); // ViewのUpdateメソッドを呼ぶ
    }
}


class SoundPlayer {
    protected Clip currentClip;
    protected File file;

    //constructor
    public SoundPlayer(String path) {
        file = new File(path);
        currentClip = createClip();
    }
    
    protected Clip createClip() {
        // 指定されたURLのオーディオ入力ストリームを取得
        /*
        * 引用
        * https://nompor.com/2017/12/14/post-128/
        */
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) {
        
            // ファイルの形式取得
            AudioFormat af = ais.getFormat();

            // 単一のオーディオ形式を含む指定した情報からデータラインの情報オブジェクトを構築
            DataLine.Info dataLine = new DataLine.Info(Clip.class, af);

            // 指定された Line.Info オブジェクトの記述に一致するラインを取得
            Clip c = (Clip) AudioSystem.getLine(dataLine);

            // 再生準備完了
            c.open(ais);

            return c;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    //音を鳴らす
    public void startSound() {
        if (currentClip == null) {
            System.out.println("Clipが空です");
        }else{
            currentClip.stop();
            currentClip.flush();
            currentClip.setFramePosition(0);
            currentClip.start();
        }
    }

    //音を止める
    public void stopSound() {
        currentClip.stop();
        currentClip.flush();
    }

    //ファイル名を取得
    public String getFileAsString() {
        return file.toString();
    }
}

class SEPlayer extends SoundPlayer{

    private ArrayList<Clip> clipPool = new ArrayList<Clip>();
    private int currentClipIndex = 0;

    /**
     * 指定したSEを用意し、いつでもstartSE()を呼んで再生できる。
     * @param path 音声ファイルの相対アドレス（wavのみ）
     * @return null
     */

    public SEPlayer(String path, int poolSize) {
        super(path);
        for (int i = 0; i < poolSize; i++) {
            Clip clip = createClip(); // pathで指定したオーディオのClipが作成される
            clipPool.add(clip); // プールにClipを格納していく
        }
    }
    
    // SEの再生（clipPoolに格納されたClipを１つずつ再生する）
    public void startSE() {
        if(clipPool.isEmpty()){
            System.out.println("Clipプールが空です");
        }else{
            currentClip = clipPool.get(currentClipIndex);
            currentClip.stop();
            currentClip.flush();
            currentClip.setFramePosition(0);
            currentClip.start();
            currentClipIndex = (currentClipIndex + 1) % clipPool.size();

            //debug
            System.out.println("currentClipIndex:"+currentClipIndex);
        }
    }
}

class MusicPlayer extends SoundPlayer{
    private int miliTime_beginMusic = 0;
    private int miliTime_living = -1;

    public MusicPlayer(String path) {
        super(path);
    }
    public MusicPlayer(String path, int startTime) {
        super(path);
        miliTime_beginMusic = startTime;
    }
    public MusicPlayer(String path, double bpm, int measureNum, int beats_per_measure) {
        super(path);
        double music_per_beat = 60000 / bpm;
        double music_per_measure = music_per_beat * beats_per_measure;
        miliTime_beginMusic = (int)music_per_measure * measureNum;
    }
    public MusicPlayer(String path, double bpm, int measureNum, int beats_per_measure, int miliTime_living) {
        this(path, bpm, measureNum, beats_per_measure);
        this.miliTime_living = miliTime_living;
    }



}


///////////////////////////////////////////////
/// View
@SuppressWarnings("deprecation")
class ViewLane extends JPanel implements Observer {

    private ArrayList<Image> effectImages; // エフェクト画像を格納するリスト
    private int currentEffectFrame = -1;   // 現在のエフェクトフレーム (-1 で非再生)
    private Timer effectTimer;             // エフェクト再生用タイマー
    private boolean effectActive = false;  // エフェクトが再生中かどうか

    private Image laneImage;

    //キーの状態を管理する変数
    private boolean keyPressed = false; // キー押下状態
    private Image defaultKeyImage; // 通常時のキー画像
    private Image pressedKeyImage; // 押下時のキー画像

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private int judgmentLineY;// 判定ラインのY座標
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected NotesModel model;
    private ArrayList<Image> backgroundImages;
    private int currentFrame = 0;
    private Timer animationTimer;

    public ViewLane(NotesModel m, Controller c, ViewUpdate vu) {
        super(true);

        this.setOpaque(false);
        this.setBackground(new Color(0,0,0,0)); 
        this.addKeyListener(c);
        this.setPreferredSize(new Dimension(120, 500)); // レーンサイズを設定。ノーツのサイズを変えたときはここも変える。
        LineBorder border = new LineBorder(Color.RED, 2, true);
        this.setBorder(border);
        vu.addObserver(this);
        this.setBorder(null); 
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        judgmentLineY = 410; // 判定ラインのY座標を設定
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        model = m;

        // 連番画像をロード
        backgroundImages = new ArrayList<>();
        try {
            for (int i = 1; i <= 120; i++) { // ファイル数に応じて調整
                String filename = String.format("video/0001-0120.avi%04d.png", i); // フォーマットを適切に変更
                Image img = Toolkit.getDefaultToolkit().getImage(filename);
                backgroundImages.add(img);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //エフェクトをロード
        effectImages = new ArrayList<>();
        try {
            for (int i = 1; i <=15; i++) { // エフェクトフレーム数に応じて調整
                String filename = String.format("effects/effect_%02d.png", i); // パスを修正
                Image img = Toolkit.getDefaultToolkit().getImage(filename);
                effectImages.add(img);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // エフェクト再生用タイマーを初期化
        effectTimer = new Timer(30, e -> updateEffectFrame());

        // タイマーでフレーム更新
        animationTimer = new Timer(60, e -> updateFrame()); // 100ms間隔（10FPS相当）
        animationTimer.start();
    }
    public void setLaneImage(String imagePath) {
        this.laneImage = new ImageIcon(imagePath).getImage();
    }
    public void setKeyImages(String defaultImagePath, String pressedImagePath) {
        this.defaultKeyImage = new ImageIcon(defaultImagePath).getImage();
        this.pressedKeyImage = new ImageIcon(pressedImagePath).getImage();
    }
    public void setKeyPressed(boolean pressed) {
        this.keyPressed = pressed;
        repaint(); // 再描画して状態を反映
    }
    // エフェクト再生を開始
    public void startEffect() {
        if (!effectImages.isEmpty()) {
            // アニメーションをリセットして再スタート
            effectTimer.stop(); // タイマーを停止
            currentEffectFrame = 0; // フレームを最初にリセット
            effectActive = true; // 再生フラグをオン
            effectTimer.start(); // 再スタート
            System.out.println("Effect started/restarted on this lane."); // デバッグ表示
        } else {
            System.out.println("No effect images loaded."); // デバッグ表示
        }
    }
    // エフェクトフレームの更新
    private void updateEffectFrame() {
        if (currentEffectFrame >= 0 && currentEffectFrame < effectImages.size() - 1) {
            currentEffectFrame++;
        } else {
            effectTimer.stop();
            effectActive = false;
            currentEffectFrame = -1; // 再生終了
        }
        repaint(); // 再描画をトリガー
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void updateJudgmentLine(int panelHeight) {
        this.judgmentLineY = panelHeight - 200; // レーンの下部から100px上に判定ライン
    }
    public int getJudgmentLineY() {
        return judgmentLineY;
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void updateFrame() {
        if (!backgroundImages.isEmpty()) {
            currentFrame = (currentFrame + 1) % backgroundImages.size(); // フレームをループ
            repaint(); // 再描画をトリガー
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g); // 背景をクリア（透明設定を保持）
    
        Graphics2D g2d = (Graphics2D) g.create();
        //レーンの描画
        if (laneImage != null) {
            float alpha = 0.7f; // 半透明（0.0f = 完全透明, 1.0f = 完全不透明）
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.drawImage(laneImage, 0, 0, getWidth(), getHeight(), null);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)); // 不透明度を元に戻す
        }
        // ノートを描画
        for (Note n : model.getNotes()) {
            n.draw(g2d); // Graphics2D を使用してノートを描画
        }

            // キー画像を描画
        Image keyImage = keyPressed ? pressedKeyImage : defaultKeyImage;
        if (keyImage != null) {
            int keyWidth = 150; // 例: キー画像の幅
            int keyHeight = 200; // 例: キー画像の高さ
            int keyX = (getWidth() - keyWidth) / 2; // レーンの中央
            int keyY = getHeight() - 200; // レーンの下部
            g.drawImage(keyImage, keyX, keyY, keyWidth, keyHeight, null);
        }

        // 後続のノート描画などは合成モードを戻す
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

    
        // 判定ラインの描画（完全不透明）
        g2d.setColor(Color.BLACK);
        g2d.drawLine(0, judgmentLineY, this.getWidth(), judgmentLineY);

    // 半透明でエフェクトを描画
        if (effectActive && currentEffectFrame >= 0) {
            Image effect = effectImages.get(currentEffectFrame);
            if (effect != null) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f)); // 70%の不透明度
                int effectX = (this.getWidth() - effect.getWidth(null)) / 2;
                int effectY = judgmentLineY - 1000;
                g2d.drawImage(effect, effectX, effectY, null);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)); // 不透明度を元に戻す
            }
        }
    
        g2d.dispose();
    } 

    // Timerによって16msごとに呼ばれるメソッド。描画を更新する。
    public void update(Observable o, Object arg) {
        repaint();
    }
}
class BackgroundPanel extends JPanel {
    private Image backgroundImage;

    // コンストラクタで背景画像を受け取り、フィールドに保持
    public BackgroundPanel(Image backgroundImage) {
        this.backgroundImage = backgroundImage;
        // レイアウトマネージャを使わず自由配置する場合は
        // setLayout(null); などとするとよい
    }

    @Override
    protected void paintComponent(Graphics g) {
        // 親クラス(JPanel)の背景描画などを処理
        super.paintComponent(g);

        // 背景画像がある場合に描画
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}

@SuppressWarnings("deprecation")
class ViewUpdate extends Observable implements ActionListener{
    
    ArrayList<NotesModel> models;
    Timer timer;
    public ViewUpdate(ArrayList<NotesModel> models){
        timer = new Timer(16, this);
        this.models = models;
    }
    public void start(){
        timer.start();
    }

    public void actionPerformed(ActionEvent e) {
        double currentTime = System.currentTimeMillis();
        for (NotesModel m : models) {
            m.noteUpdate(currentTime);
        }
        setChanged();
        notifyObservers();
    }

}

////////////////////////////////////////////////
// Controller (C)
class Controller implements KeyListener {
    protected ArrayList<NotesModel> models;
    protected ArrayList<ViewLane> views;
    protected ViewUpdate vu;
    protected int dragStartX, dragStartY;
    protected int keyStatus = 0; // 2bitで、1bit目が1ならd、2bit目が1ならf, 3bit目が1ならj, 4bit目が1ならkが押されている

    protected SEPlayer tapNote;

    // コンストラクタでモデルとビューのリストを受け取る
    public Controller(ArrayList<NotesModel> models, ArrayList<ViewLane> views, ViewUpdate vu) {
        this.models = models;
        this.views = views; // views を初期化
        this.vu = vu;
        tapNote = new SEPlayer("hit1.wav", 50);
    }

    // キーが押されたときの処理。スペースキーでゲームスタート。
    public void keyPressed(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            double currentTime = System.currentTimeMillis();
            for (NotesModel m : models) {
                m.gameStart(currentTime);
            }
            vu.start();
        }

        char c = e.getKeyChar();
        switch (c) {
            case 'd':
                views.get(0).setKeyPressed(true); // キー押下状態を更新
                models.get(0).tapJudge();
                break;
            case 'f':
                views.get(1).setKeyPressed(true);
                models.get(1).tapJudge();
                break;
            case 'j':
                views.get(2).setKeyPressed(true);
                models.get(2).tapJudge();
                break;
            case 'k':
                views.get(3).setKeyPressed(true);
                models.get(3).tapJudge();
                break;
        }
    }

    // キーがタイプされたときの処理。d,f,j,kキーで判定。
    public void keyTyped(KeyEvent e) {
        char c = e.getKeyChar();
        switch (c) {
            case 'd':
                models.get(0).tapJudge();
                views.get(0).startEffect(); // エフェクト再生
                new Thread(() -> tapNote.startSE()).start();
                break;
            case 'f':
                models.get(1).tapJudge();
                views.get(1).startEffect(); // エフェクト再生
                new Thread(() -> tapNote.startSE()).start();
                break;
            case 'j':
                models.get(2).tapJudge();
                views.get(2).startEffect(); // エフェクト再生
                new Thread(() -> tapNote.startSE()).start();
                break;
            case 'k':
                models.get(3).tapJudge();
                views.get(3).startEffect(); // エフェクト再生
                new Thread(() -> tapNote.startSE()).start();
                break;
        }
    }

    // キーが離されたときの処理。d,f,j,kキー
    public void keyReleased(KeyEvent e) {
        char c = e.getKeyChar();
        switch (c) {
            case 'd':
                views.get(0).setKeyPressed(false); // キー押下解除
                break;
            case 'f':
                views.get(1).setKeyPressed(false);
                break;
            case 'j':
                views.get(2).setKeyPressed(false);
                break;
            case 'k':
                views.get(3).setKeyPressed(false);
                break;
        }
    }
}

//////////////////////////////////////////////////
// main class
// (GUIを組み立てているので，view の一部と考えてもよい)
class LaneFrame extends JFrame {
    private BufferedImage backgroundImage; // 背景画像
    Controller cont;
    ArrayList<NotesModel> models;
    ArrayList<ViewLane> views;
    ViewUpdate vu;

    public LaneFrame() {
        // 背景画像をロード
        try {
            String imagePath = "Blender-UV-Grid-01.jpg"; // パスは適宜修正
            backgroundImage = ImageIO.read(new File(imagePath));
            if (backgroundImage == null) {
                System.err.println("Failed to load image: " + imagePath);
            } else {
                System.out.println("Image loaded successfully: " + imagePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 背景パネルを生成し、フレームのcontentPaneとして設定
        BackgroundPanel backgroundPanel = new BackgroundPanel(backgroundImage);
        // 必要に応じてレイアウト設定。null レイアウトなら好きな位置に配置できる。
        backgroundPanel.setLayout(null);
        this.setContentPane(backgroundPanel);

        // モデル，ビュー，コントローラを生成
        models = new ArrayList<>();
        views = new ArrayList<>();
        vu = new ViewUpdate(models);
        cont = new Controller(models, views, vu);
        // モデルとビューを生成してリストに追加
        for (int i = 0; i < 4; i++) {
            NotesModel model = new NotesModel(i, null); // 一時的にViewをnull
            ViewLane view = new ViewLane(model, cont, vu);
            model.setView(view); // モデルにビューをセット

            view.setLaneImage("images/lane" + i + ".png"); // レーン画像を設定

            view.setKeyImages("images/default" + i + ".png", "images/pressed" + i + ".png");

            models.add(model);
            views.add(view);

            view.setOpaque(false); // 背景を透過
            view.setFocusable(true);
            backgroundPanel.add(view);

            //レーン画像を設定
        }

        // ウィンドウの基本設定
        this.setTitle("Lane Test");
        this.setSize(1600, 1200);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);

        // コンポーネントの位置を初期化
        updateComponentPositions();

        // リサイズ時に位置を更新
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateComponentPositions();
            }
        });
        loadNotes("test.csv");

        // フォーカスを設定
        for (ViewLane view : views) {
            view.requestFocusInWindow();
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g); // コンポーネントの描画を先に呼び出す
    
        // 背景画像の描画
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, this.getWidth(), this.getHeight(), null);
        }
    }

    private void updateComponentPositions() {
        int width = this.getWidth();
        int height = this.getHeight();
        //System.out.println("width: " + width + ", height: " + height);

        // 各レーンを相対的に配置
        int laneWidth = Math.max(width / 1600 * 150, 150); // ウィンドウ幅に対してレーンの幅を設定
        int laneHeight = height; // ウィンドウ高さをレーンの高さに設定
        int startX = Math.max((width - 4 * laneWidth) / 2, 0); // レーンの左端を中央に揃える
        int startY = 0; // 上側の余白を設定

        for (int i = 0; i < views.size(); i++) {
            ViewLane view = views.get(i);
            view.setBounds(startX + i * laneWidth, startY, laneWidth, laneHeight);
            view.updateJudgmentLine(laneHeight); // 判定ラインを更新
        }
    }

        /**
     * 譜面データのCSVを取り込み、適切にaddNote()する
     * @param path CSVファイルの相対アドレス
     */
private void loadNotes(String path) {
    String csvFile = path;
    String line;
    int measure = 0; 
    int index = 0; 
    int beats = -1; 
    double bpm = -1; 

    try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
        int timeAddNotes = 0;
        while ((line = br.readLine()) != null) {
            String[] valuesLine = line.split(",", -1); 

            if (valuesLine.length < 5) {
                System.out.println("Skipped line due to insufficient columns: " + line);
                continue;
            }

            if (valuesLine[4].startsWith("#")) {
                continue;
            }

            if (!valuesLine[4].isEmpty()) {
                beats = Integer.parseInt(valuesLine[4].split("/")[1]);
            }

            if (valuesLine.length > 5 && !valuesLine[5].isEmpty()) {
                bpm = Integer.parseInt(valuesLine[5]);
            }

            if (valuesLine.length > 6 && !valuesLine[6].isEmpty()) {
                timeAddNotes += Integer.parseInt(valuesLine[6]);
            }

            index = (index + 1) % beats;
            if (index == 0) {
                measure++;
            }

            for (int i = 0; i < models.size(); i++) {
                if (valuesLine.length > i && valuesLine[i].equals("1")) {
                    models.get(i).addNote(timeAddNotes, 0);
                    System.out.println("Note is inserted!:" + timeAddNotes + "[type:0][measure:" + measure + "][index:" + index + "]");
                } else if (valuesLine.length > i && valuesLine[i].equals("2")) {
                    models.get(i).addNote(timeAddNotes, 1);
                    System.out.println("Note is inserted!:" + timeAddNotes + "[type:1][measure:" + measure + "][index:" + index + "]");
                }
            }

            timeAddNotes += getInterval(bpm, beats);
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}


    /**
     * 指定した拍子・BPMにおける最小音符間隔をミリ秒で返す。
     * 例：BPM 60, beats 8 の場合、BPM60における八分音符の長さをミリ秒で返す。
     * @param bpm
     * @param beats
     */
    private int getInterval(double bpm, int beats) {
        return (int)(60000.0 / bpm * (4.0 / beats));
    }

    public static void main(String[] args) {
        new LaneFrame();
    }
}