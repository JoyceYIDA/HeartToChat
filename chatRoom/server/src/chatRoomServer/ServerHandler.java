package chatRoomServer;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.SocketHandler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerHandler {
    //Map来保存多个客户端和消息
    private static Map<String,Socket> socketMap=new ConcurrentHashMap<>();//ConcurrentHashMap线程安全

    public static class SocketThread implements Runnable{

        private Socket client;

        public SocketThread(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                Scanner scanner = new Scanner(client.getInputStream());

                String msgFromClient="";//初始化输入字符串
                while(true) {
                    if (scanner.hasNext()) {
                        msgFromClient = scanner.nextLine();
                        //Windows下，输出为"\r\n",而\r不会被消除，所以输出的信息最后都会有\r，所以要消除它
                        Pattern pattern = Pattern.compile("\r");
                        Matcher matcher = pattern.matcher(msgFromClient);
                        msgFromClient = matcher.replaceAll("");
                    }
//                注册
                    if (msgFromClient.startsWith("-register:")) {
                        String name=msgFromClient.split("\\:")[1];
                        register(name,client);
                    }
//                群聊
                    if (msgFromClient.startsWith("-group:")) {
                        String msg=msgFromClient.split("\\:")[1];
                        groupChat(msg);
                    }
//                私聊
                    if (msgFromClient.startsWith("-private:")) {
                        String name=msgFromClient.split("\\:")[1].split("\\-")[0];
                        String msg=msgFromClient.split("\\:")[1].split("\\-")[1];
                        privateChat(name,msg);
                    }
//                退出
                    if (msgFromClient.startsWith("-exit:")) {
                        String name=msgFromClient.split("\\:")[1];
                        exit(name);
                    }
                }
            }catch(Exception e){

            }
        }
        private void register(String name,Socket socket){
            //把用户注册的信息放进map中
            socketMap.put(name,socket);
            System.out.println("用户"+name+"来了！");
            System.out.println("当前聊天室人数为："+socketMap.size());
            try {
                PrintStream print=new PrintStream(socket.getOutputStream(),true,"UTF-8");
                print.println("注册成功");
                print.println("当前聊天室人数为："+socketMap.size());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        private void groupChat(String msg){
            //群聊实际上就是遍历map，把消息发送给每个客户端
            //先把map转为set
            Set<Map.Entry<String,Socket>> entrySet=socketMap.entrySet();
            //取得迭代器
            Iterator<Map.Entry<String,Socket>> iterator=entrySet.iterator();
            while(iterator.hasNext()){
                //取得每个实体
                Map.Entry<String,Socket> client=iterator.next();
                PrintStream printStream=null;
                try {
                    printStream=new PrintStream(client.getValue().getOutputStream(),true,"UTF-8");
                    printStream.println("群聊消息为"+msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        private void privateChat(String name,String msg){
            //取得对应的Socket
            Socket client=socketMap.get(name);
            try {
                PrintStream printStream=new PrintStream(client.getOutputStream(),true,"UTF-8");
                printStream.println("收到的消息为："+msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        private void exit(String name){
            //使用迭代器移除
            Set<Map.Entry<String,Socket>> entrySet=socketMap.entrySet();
            Iterator<Map.Entry<String,Socket>> iterator=entrySet.iterator();
            while(iterator.hasNext()){
                Map.Entry<String,Socket> client=iterator.next();
                if(name==client.getKey()){
                    iterator.remove();
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
//        int DEFAULT_THREAD=100;
//        int THREADS=DEFAULT_THREAD;

        ServerSocket serverSocket=new ServerSocket(6666);
        //创建线程池
        ExecutorService socketExecutor=Executors.newFixedThreadPool(20);
        System.out.println("等待用户输入-----");
        for(int i=0;i<20;i++){
            Socket client=serverSocket.accept();
            System.out.println("有新的客户端连接");
            socketExecutor.submit(new SocketThread(client));
            System.out.println("客户端的端口号为："+client.getPort());
            System.out.println("当前聊天室人数为："+socketMap.size());
        }
        //关闭socket和线程池
        socketExecutor.shutdown();
        serverSocket.close();
    }
}
