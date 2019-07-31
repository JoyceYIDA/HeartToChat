package chatRoomClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;


//向服务器发送信息的线程
class SendToServer implements Runnable{
    private Socket client;

    public SendToServer(Socket client) {
        this.client = client;
    }
    @Override
    public void run() {
        //获取输出流
        try {
            PrintStream printStream=new PrintStream(client.getOutputStream(),true,"UTF-8");
            Scanner in=new Scanner(System.in);
            while(true){
                System.out.print("请输入要向服务器发送的信息>");
                String sendStr="";
                if(in.hasNext()){
                    sendStr=in.nextLine();
                }
                printStream.println(sendStr);
                //通过字符串来控制关闭
                if(sendStr.contains("bye")){
                    System.out.println("客户端退出聊天室");
                    printStream.close();
                    in.close();
                    client.close();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


//读取客户端发送信息的线程
class ReadFromServer implements Runnable{
    private Socket client;

    ReadFromServer(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            Scanner scanner=new Scanner(client.getInputStream());
            while(true) {
                if (client.isClosed()) {
                    System.out.println("客户端已经关闭了！！！");
                    scanner.close();
                    break;
                }
                if(scanner.hasNext()){
                    System.out.println("服务器发送的内容为："+scanner.nextLine());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

public class ClientHandler{
    public static void main(String[] args) throws Exception{
        Socket client=new Socket("127.0.0.1",6666);
        //启动读线程与写线程
        Thread readThread=new Thread(new ReadFromServer(client));
        Thread sendThread=new Thread(new SendToServer(client));
        readThread.start();
        sendThread.start();
    }
}
