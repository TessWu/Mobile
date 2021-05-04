package self.skzeratal.tilelocalization;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import self.skzeratal.tilelocalization.croppedimage.CroppedImageActivity;
import self.skzeratal.tilelocalization.floorplan.FloorPlanActivity;
import self.skzeratal.tilelocalization.video.VideoActivity;

public class SocketClient extends AsyncTask<String, Void, Void> {
    private OnPostExecuted object;
    private String type;
    private Socket socket;
    public String queue;

    public SocketClient(VideoActivity videoActivity) {
        socket = new Socket();
        queue = ""; //queue是放我要傳甚麼東西
    }

    public SocketClient (OnPostExecuted object) {
        this.object = object;
        socket = new Socket();
        queue = "";
    }

    @Override
    protected Void doInBackground(String... data) {
        Log.v("tess","it's  socketclientdoinbackground");
        type = data[0];

        switch (type) {
            case "STORE":
                store(data);
                break;
            case "UPDATE":
                break;
            case "RESTORE":
                restore();
                break;
            case "MATCH":
                match(data);
                break;
            case "MATC1":
                matc1(data);
                break;
            case "MATC2":
                matc2(data);
                break;
        }
        return null;
    }

    protected void onPostExecute(Void result) {
        Log.v("tess","it's  socketclientonPostExecute");
        switch (type) {
            case "STORE":
                object.onPostExecuted(queue);
                break;
            case "RESTORE": //打開室內平面圖，已經建好了很多個點，那我怎麼知道那些點在哪裡，要先去問server；它也要先打開socket問server說東西在哪裡，server會傳給你，東西就會放到queue裡，我再從queue裡去做事情
                object.onPostExecuted(queue);
                break;
            case "MATCH":
                object.onPostExecuted(queue);
                break;
            case "MATC1":
                object.onPostExecuted(queue);
                break;
            case "MATC2":
                object.onPostExecuted(queue);
                break;
        }
    }

    public void Stop() {
        try {
            socket.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        Log.v("tess","it's  socketclient close");
    }

    private void store(String... data) {
        Log.v("tess","it's  socketclient store");
        try {
            String payloadSegment;
            socket = new Socket();
            socket.setSoTimeout(60000);
            socket.connect(new InetSocketAddress("140.123.97.82", 60417));
            socket.getOutputStream().write("STORE".getBytes());

            while (data[1].length() > 0) {
                if (data[1].length() >= 1024) {
                    payloadSegment = data[1].substring(0, 1024);
                    data[1] = data[1].substring(1024);
                    socket.getOutputStream().write(payloadSegment.getBytes());
                }
                else {
                    payloadSegment = data[1];
                    data[1] = data[1].substring(0, 0);
                    socket.getOutputStream().write(payloadSegment.getBytes());
                }
            }

            socket.getOutputStream().write((";" + data[2] + ";" + data[3] + ";" + data[4] + ";" + data[5] + ";STORE").getBytes());

            byte[] message = new byte[1024];
            int count = socket.getInputStream().read(message, 0, 1024);

            if (count > 0) {
                String payload = new String(message, "UTF-8");
                queue += payload.substring(5, count - 5);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        Log.v("tess","it's  socketclient store end");
    }

    private void restore() {
        Log.v("tess","it's  socketclient restore");
        byte[] message = new byte[1024];

        try {
            socket = new Socket();
            socket.setSoTimeout(5000);
            socket.connect(new InetSocketAddress("140.123.97.82", 60417));
            socket.getOutputStream().write("RESTORE".getBytes());

            int count = 0;

            do {
                 count = socket.getInputStream().read(message, 0, 1024);

                 if (count > 0) {
                     String payload = new String(message, "UTF-8");
                     queue += payload.substring(0, count);

                     if (queue.endsWith("RESTORE")) {
                         if (queue.startsWith("RESTORE")) {
                             queue = queue.substring(7, queue.length() - 7);
                         }
                         else {
                             queue = "";
                         }
                         break;
                     }
                 }
            } while (count > 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.v("tess","it's  socketclient restore end");
    }

    private void test(String...data) {

    }
    //要去做比對
    private void match(String... data) {
        Log.v("tess","it's  socketclient match");
        try {
            String payloadSegment;

            socket = new Socket(); //先開一個socket
            socket.setSoTimeout(60000); //設定socket多少時間會timeout，可以設長一點，因為比對會比較長會一直timeout
            socket.connect(new InetSocketAddress("140.123.97.82", 60417));
            socket.getOutputStream().write("MATCH".getBytes()); //執行到這一行時server會收到MATCH這5個字，server會知道原來這個人是要做match
            //切base64的字串
            while (data[1].length() > 0) { //把很大的字串切成很多份，然後每一份就是payload的segment
                if (data[1].length() >= 1024) { //最多一個封包存1024，所以這邊要切
                    payloadSegment = data[1].substring(0, 1024);
                    data[1] = data[1].substring(1024);//然後把segment切1024
                    socket.getOutputStream().write(payloadSegment.getBytes()); //一次就是傳1024，傳到它切不了為止，切不了為止就是小於1024
                    Log.v("tess","我有進來match的if");
                }
                else { //把最後那一份小於1024的尾巴再傳出去
                    payloadSegment = data[1];
                    data[1] = data[1].substring(0, 0);
                    socket.getOutputStream().write(payloadSegment.getBytes());
                    Log.v("tess","我有進來match的else");
                }

            }
            //以上都傳完了就再繼續往下傳
            //socket.getOutputStream().write((";" + data[2] + ";MATCH").getBytes()); //這邊是用分號來做字串的辨識 data[1]是圖片，data[2]是角度(應該是) ，match結尾，server那邊就知道我這邊傳完了
            socket.getOutputStream().write((";MATCH").getBytes());
            Log.v("tess","我有跑到match的getoutputstream這邊");
            //影像評估是放在server，這邊只是試做
            byte[] message = new byte[1024];
            int count = socket.getInputStream().read(message, 0, 1024);
            Log.v("tess","我有跑到match的inputstream這邊");

            if (count > 0) {
                String payload = new String(message, "UTF-8");
                queue += payload.substring(5, count - 5);
                Log.v("tess","我有跑到match的count>0這邊");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            Log.v("tess","我有跑到match的catch這邊");
        }
        Log.v("tess","it's  socketclient match end");
    }

    private void matc1(String... data) {
        Log.v("tess","it's  socketclient match1");
        try {
            String payloadSegment;

            socket = new Socket();
            socket.setSoTimeout(60000);
            socket.connect(new InetSocketAddress("140.123.97.82", 60417));
            socket.getOutputStream().write("MATC1".getBytes());

            while (data[1].length() > 0) {
                if (data[1].length() >= 1024) {
                    payloadSegment = data[1].substring(0, 1024);
                    data[1] = data[1].substring(1024);
                    socket.getOutputStream().write(payloadSegment.getBytes());
                    Log.v("tess","我進來matc1的if");
                }
                else {
                    payloadSegment = data[1];
                    data[1] = data[1].substring(0, 0);
                    socket.getOutputStream().write(payloadSegment.getBytes());
                    Log.v("tess","我進來matc1的else");
                }
            }

            //socket.getOutputStream().write((";" + data[2] + ";MATC1").getBytes());  //在這邊會出錯，因為我沒有拿到拍攝角，所以傳不進來
            socket.getOutputStream().write((";MATC1").getBytes());
            Log.v("tess","我有跑到write這邊");

            byte[] message = new byte[1024];
            int count = socket.getInputStream().read(message, 0, 1024);
            Log.v("tess","我有到 matc1 getinputStream裡面");

            if (count > 0) {
                String payload = new String(message, "UTF-8");
                queue += payload.substring(5, count - 5);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            Log.v("tess","我到matc1 的catch裡");

        }
        Log.v("tess","it's  socketclient match1 end");
    }

    private void matc2(String... data) {
        Log.v("tess","it's  socketclient match2 ");
        try {
            String payloadSegment;

            socket = new Socket();
            socket.setSoTimeout(60000);
            socket.connect(new InetSocketAddress("140.123.97.82", 60417));
            socket.getOutputStream().write("MATC2".getBytes());

            while (data[1].length() > 0) {
                if (data[1].length() >= 1024) {
                    payloadSegment = data[1].substring(0, 1024);
                    data[1] = data[1].substring(1024);
                    socket.getOutputStream().write(payloadSegment.getBytes());
                }
                else {
                    payloadSegment = data[1];
                    data[1] = data[1].substring(0, 0);
                    socket.getOutputStream().write(payloadSegment.getBytes());
                }

            }

            socket.getOutputStream().write((";" + data[2] + ";MATC2").getBytes());

            byte[] message = new byte[1024];
            int count = socket.getInputStream().read(message, 0, 1024);

            if (count > 0) {
                String payload = new String(message, "UTF-8");
                queue += payload.substring(5, count - 5);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        Log.v("tess","it's  socketclient match2  end");
    }
}