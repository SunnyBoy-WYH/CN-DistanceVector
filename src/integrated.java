import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

class TableItem {

    String nextHop;  // 邻居节点id。若直达，即目的结点id。
    float distance;  // 到达目的节点的距离或代价。可以不是整数

    public TableItem(String nextHop, float distance) {
        this.nextHop = nextHop;
        this.distance = distance;
    }
}

class InfoItem {
    String srcNode;  // 发送节点id。
    float distance;  // 到达目的节点的距离或代价。可以不是整数

    public InfoItem(String srcNode, float distance) {
        this.srcNode = srcNode;
        this.distance = distance;
    }
}

class NeighborItem {
    float distance;  // 到达邻居的距离
    int PORT;  // 邻居的端口号

    public NeighborItem(float distance, int PORT) {
        this.distance = distance;
        this.PORT = PORT;
    }
}


class MySender extends TimerTask {  //原本是继承Thread的，现在改成使用计时器的多线程方法
	
    private HashMap<String, InfoItem> sendRouteInfo;  // 全局变量从sendStr改为sendRoutInfo
    private InetAddress loc;
    static String IP = "127.0.0.1";
    private DatagramSocket sender;
    private HashMap<String, NeighborItem> neighborTable;

    private int logSeq;  // 日志序号
    private int subSeq;  // 发送日志的子序号，初始化为0
    private String logDir;  // 日志路径
    private String myId;  // 用于写发送日志

    // 改动：第二个参数原先是sendStr，但现在是sendRouteInfo，在发送线程内部将其转换为字符串。
    // 这样做的原因是：writeSendLog必须在发送线程内部实现，且需要以sendRoutInfo为参数。
    // 连带改动：现在将infoToString参数放在发送线程内部。
    public MySender(DatagramSocket server, HashMap<String, InfoItem> sendRouteInfo, HashMap<String, NeighborItem> neighborTable,
                    int logSeq, String logDir, String myId) throws UnknownHostException, SocketException{

        this.sendRouteInfo = sendRouteInfo;
        loc = InetAddress.getByName(IP);
        sender = server;
        this.neighborTable = neighborTable;

        this.logSeq = logSeq;
        this.subSeq = 0;
        this.logDir = logDir;
        this.myId = myId;
    }

    private static String infoToString(HashMap<String, InfoItem> sendRouteInfo) {
        // 将sendRouteInfo手动转化成字符串。
        // 格式：由空格隔开的三元组(dst, src, dis)
        String sendStr = "";
        for(String dst : sendRouteInfo.keySet()) {
            InfoItem iItem = sendRouteInfo.get(dst);
            sendStr += ( "(" + dst + "," + iItem.srcNode + "," + String.valueOf(iItem.distance) + ")");
            sendStr += " ";
        }

        sendStr = sendStr.substring(0,sendStr.length()-1);  // 删除末尾多余的空格
        sendStr += "@";  // 作为有效信息结束标志
        return sendStr;
    }


    private void writeSendLog() throws Exception{
        // sendRoutInfo格式：
        // dst1: (src, dis1)
        // dst2: (src, dis2)
        BufferedWriter writer = new BufferedWriter(new FileWriter(logDir, true));  // 以追加方式打开

        // 写头信息
        String line;
        line = "Sent. Source Node = " + myId + "; Sequence Number = " + logSeq + "-" + (++subSeq) + "\n";
        writer.append(line);

        // 遍历写路由信息
        for(String dst : sendRouteInfo.keySet()) {
            InfoItem iItem = sendRouteInfo.get(dst);
            line = "DestNode = " + dst + "; Distance = " + iItem.distance + "\n";
            writer.append(line);
        }
        writer.append("\n");  // 回车，与下一次的日志分隔开
        writer.close();
    }

    public void run() {  // 覆写run()方法，作为线程 的操作主体
        try {
            writeSendLog();  // 写发送日志

            String sendStr = infoToString(this.sendRouteInfo);   // 将sendRoutInfo转化为sendStr。表示在生命周期中发送的路由信息
            byte[] send_byte = sendStr.getBytes("UTF-8");

            for(String dst : neighborTable.keySet()){  //遍历邻居表，发送信息
                int TARPORT = neighborTable.get(dst).PORT;
                //System.out.println("dst: "+dst+" "+TARPORT);
            	DatagramPacket packet = new DatagramPacket(send_byte, send_byte.length, loc, TARPORT);
                sender.send(packet);
                //System.out.println("I am sending to "+dst);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
};

public class integrated {


    // 下述常量来自系统配置文件sysconfig.txt
    static int freq;  // 发送间隔，单位ms
    static float inf;  // 规定的不可达距离
    static int maxValidTime;  // 超时间隔，单位ms
    // 下述常量来自命令行参数args
    static String myId;  // 我的id
    static int SERVERPORT; // 我的接收端口，从命令行参数获得
    //static int SENDERPORT = 41659; //我的发送端口，任意设置即可
    static String IP = "127.0.0.1";

    // 数据表
    static private HashMap<String, TableItem> routeTable = new HashMap<>();  // 路由表。键：目的节点id
    static private HashMap<String, NeighborItem> neighborTable = new HashMap<>();  // 邻居距离表。键：目的节点id

    // 日志
    static private String logDir;  // 日志路径
    static private int logSeq; // 日志序号

    private static void init(String[] args) throws Exception {
        // dvsim x 51000 x.txt

        BufferedReader reader;
        String line;
        // Step1.读取系统配置文件，初始化全局变量
        reader = new BufferedReader(new FileReader("../../../config2/sysconfig.txt"));  // TODO 运行字节码时路径添加../
        line = reader.readLine().split("=")[1];
        freq = Integer.parseInt(line);
        line = reader.readLine().split("=")[1];
        inf = Float.parseFloat(line);
        line = reader.readLine().split("=")[1];
        maxValidTime = Integer.parseInt(line);
        //System.out.println(args[0]+"  "+args[1]+"  "+args[2]);
        myId = args[0]; //原本是1
        SERVERPORT = Integer.parseInt(args[1]);  //原本是2

        // Step2. 读取x.txt，初始化邻居表
        // y 4.0 52002
        // z 50.0 52003
        reader = new BufferedReader(new FileReader("../../../config2/" + args[2]));  // TODO 运行字节码时路径添加../,原本是3！
        while((line = reader.readLine()) != null) {
            String[] strs = line.split(" ");
            NeighborItem nItem = new NeighborItem(Float.parseFloat(strs[1]), Integer.parseInt(strs[2]));
            neighborTable.put(strs[0], nItem);  // 邻居表增加一项
        }

        // Step3. 根据邻居距离表，初始化路由表
        // y : (y, 4.0)
        // z : (z, 50.0)
        for(String dst : neighborTable.keySet()) {
            NeighborItem nItem = neighborTable.get(dst);
            TableItem tItem = new TableItem(dst, nItem.distance);  // 直达，下一跳就是dst
            routeTable.put(dst, tItem);  // 路由表增加一项
        }

        // Step4. 为logDir赋值, 并清空日志文件中内容
        logDir = "../../../log2/log_" + myId + ".txt";
        BufferedWriter writer = new BufferedWriter(new FileWriter(logDir));  // 以写方式打开
        writer.close();

    }

    private static HashMap<String, InfoItem> genRouteInfo() {
        // 根据路由表，构造路由信息
        // 路由表:
        // y : (y, 4)
        // z : (z, 50)

        // 路由信息:
        // y : (x, 4.0)
        // z : (x, 50.0)

        HashMap<String, InfoItem> sendRouteInfo = new HashMap<>();

        for(String dst : routeTable.keySet()) {
            float cost = routeTable.get(dst).distance;
            InfoItem iItem = new InfoItem(myId, cost);
            //sendRouteInfo.put(tItem.nextHop, iItem);
            sendRouteInfo.put(dst, iItem);
        }

        return sendRouteInfo;
    }


    private static HashMap<String, InfoItem> stringToInfo(String recvStr) {
        // 将字符串转化成recvRouteInfo
        // 格式：由空格隔开的三元组(dst, src, dis)
        HashMap<String, InfoItem> recvRouteInfo = new HashMap<>();

        recvStr = recvStr.substring(0, recvStr.indexOf("@"));  // 截取@之前的部分，不包括@
        String[] strs = recvStr.split(" ");
        // System.out.println("The length of strs :" + strs.length());
        for(String tuple:strs) {
            //System.out.println("元组长度：" + tuple.length());
            tuple = tuple.substring(1, tuple.length()-1);  // 去除头尾的(和)，这里有时会报错，似乎是tuple.length比1小？！
            //System.out.println(tuple);
            String[] elems = tuple.split(",");
            InfoItem iItem = new InfoItem(elems[1], Float.parseFloat(elems[2]));
            recvRouteInfo.put(elems[0], iItem);
        }

        return recvRouteInfo;
    }

    private static void writeRecvLog(HashMap<String,List<InfoItem>> Buffer, int seqRecv, String logDir) throws Exception{
        // BUffer格式：
        // src1:[(dst1, dis1), (dst2, dis2)]
        // src2:[(dst1, dis3), (dst2, dis4)]
        BufferedWriter writer = new BufferedWriter(new FileWriter(logDir, true));  // 以追加方式打开

        // 写头信息
        String line;
        line = "Received. Source Node = " + myId + "; Sequence Number = " + seqRecv + "\n";
        writer.append(line);

        // 遍历写路由信息
        for(String src : Buffer.keySet()) {
            for(InfoItem iItem : Buffer.get(src)) {
                // 注意：这里srcNode属性实际代表的是dst
                line = "DestNode = " + iItem.srcNode + "; Distance = " + iItem.distance + "; Neighbor = " + src + "\n";
                writer.append(line);
            }
        }

        writer.append("\n");  // 回车，与下一次的日志分隔开
        writer.close();
    }

    private static HashMap<String, TableItem> Distance_Vector(HashMap<String,List<InfoItem>> Buffer, Set<String> reachableList){  //根据更新现有路由表 ，执行DV算法
        //初始化的时候把到所有点的距离都设置为Max就行！
        HashMap<String, TableItem> newrouteTable = new HashMap<>();

        // Step1. 初始化到所有可达节点的距离为inf.(可达节点:所有邻居的路由信息中dst的并集)
        for (String i : reachableList) {
             newrouteTable.put(i, new TableItem("NULL", inf));
        }

        // newrouteTable.put(myId, new TableItem("NULL",0));  // 到自己的距离置为0.现在不需要,因为新路由表中不出现dst是自己的项

        // Step2. 初始化到所有邻居节点的距离,根据邻居表初始化
        for(String neibour : neighborTable.keySet() ) {
            float cost = neighborTable.get(neibour).distance;
            newrouteTable.put(neibour, new TableItem(neibour,cost));
        }

        // 对每个邻居发来的消息进行迭代, 更新路由表
        for(String neibour : Buffer.keySet()) {
            float to_neibour = neighborTable.get(neibour).distance; // 获得我到邻居的距离

            List<InfoItem> from_neibour = Buffer.get(neibour);
            for(InfoItem iItem : from_neibour) {
                String target = iItem.srcNode ; // 此时item.src实际上是target

                float cost = iItem.distance + to_neibour;
                if(cost < newrouteTable.get(target).distance) {
                    newrouteTable.put(target, new TableItem(neibour,cost));
                }
            }
        }

        newrouteTable.remove(myId);  //新路由表中不出现dst是自己的项

        //for(String item: newrouteTable.keySet() ) {
            //System.out.println("The item is:" + item + " distance: " + newrouteTable.get(item).distance);
        //}

        //路由表包含所有信息：1.到自己，为0；2.到邻居（除非改变邻居表，否则不改）
        return newrouteTable;
    }
    

    public static void main(String args[]) throws Exception {

        init(args);  // 初始化邻居表和路由表
        DatagramSocket server = new DatagramSocket(SERVERPORT);


        // 模拟常开的路由器，始终发送和接收路由信息。 每轮循环结束前更新路由表。
        while (true){
            System.out.println("第" + (logSeq+1) + "轮发送/接收...");
        	Timer timer = new Timer(true);
            Set<String> reachableList = new HashSet<String>();  // 从本轮收到的路由信息中，获取可达节点
            HashMap<String,List<InfoItem> > Buffer = new HashMap<String,List<InfoItem> >();  // 缓存本次循环接收到的路由信息

			try {
			    server.setSoTimeout(maxValidTime);  // 设置超时时间，若在此时间内未收到来自全部邻居的路由表，触发超时逻辑，认为有邻居挂了
                HashMap<String, InfoItem> sendRouteInfo = genRouteInfo();  // 根据当前路由表生成路由信息
				MySender mt1 = new MySender(server, sendRouteInfo, neighborTable, ++logSeq, logDir, myId);  //初始化发送进程。定时向每一个邻居发送路由信息，只需要初始化一次
				InetAddress loc = InetAddress.getByName(IP);   //接收
		       
                byte[] buf = new byte[200];   // 注意，这一步带来很大的问题
		        DatagramPacket repacket = new DatagramPacket(buf, buf.length, loc, SERVERPORT);  // 收发公用一个端口
		       
		        timer.schedule(mt1, 0, freq);  // 启动定时器
		        while(true) {  // 阻塞接收报文。不再阻塞的条件：1.收到所有邻居的路由信息 2.超时
		            try {
		                // Step1. 接收reMsg并转换为recvRouteInfo
		                server.receive(repacket);
		                String reMsg = new String(repacket.getData());
		                //System.out.println(reMsg);  //小心这个reMsg，之后很多是不知名内容！？
                        HashMap<String, InfoItem> recvRouteInfo = stringToInfo(reMsg); // 原来的tmpTable。获得邻居发来的路由表

                        // Step2. 根据recvRouteInfo，填充buffer
		                String src = "";
                        List<InfoItem> tmpList = new ArrayList<>();  // 原来的tmpStore。元素格式：(dst, dis)
		                for(String dst : recvRouteInfo.keySet()) {
		                	InfoItem iItem = recvRouteInfo.get(dst);
		                	src = iItem.srcNode;   // 发给我的邻居是谁
		                	reachableList.add(dst);  // 维护reachableList
		                	tmpList.add(new InfoItem(dst,iItem.distance));
		                }
		                
		                Buffer.put(src, tmpList); //src在表中存在，则覆盖；不存在则添加

		                if(Buffer.size() == neighborTable.size()) {  //停止接收，也停止发送
		                	break;
		                }

		            } catch (IOException e) {
		                System.out.println("Time out!Some Node might dead!");
		            }
		       }

			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			
			timer.cancel();  // 停止发送
            writeRecvLog(Buffer, logSeq, logDir);  // 写接收日志
			
			Thread.currentThread().sleep(3000);  // 这一步的目的是不是防止继续发送？

			routeTable = Distance_Vector(Buffer, reachableList);  //根据接收到的Buffer更新路由表


			/*System.out.println("Please Input choice!");
			Scanner input=new Scanner(System.in);
			flag=input.next();
			if(flag=="P") {
			//将暂停
			}
			else if(flag=="S") {
				break;   //停止循环
			}
			else if(flag=="A") {
			//可以修改节点信息
			}	
			else {
				System.out.println("Input error!");
			}*/
			
		}
    }
}
