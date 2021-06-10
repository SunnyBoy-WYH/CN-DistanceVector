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

    String nextHop;  // �ھӽڵ�id����ֱ���Ŀ�Ľ��id��
    float distance;  // ����Ŀ�Ľڵ�ľ������ۡ����Բ�������

    public TableItem(String nextHop, float distance) {
        this.nextHop = nextHop;
        this.distance = distance;
    }
}

class InfoItem {
    String srcNode;  // ���ͽڵ�id��
    float distance;  // ����Ŀ�Ľڵ�ľ������ۡ����Բ�������

    public InfoItem(String srcNode, float distance) {
        this.srcNode = srcNode;
        this.distance = distance;
    }
}

class NeighborItem {
    float distance;  // �����ھӵľ���
    int PORT;  // �ھӵĶ˿ں�

    public NeighborItem(float distance, int PORT) {
        this.distance = distance;
        this.PORT = PORT;
    }
}


class MySender extends TimerTask {  //ԭ���Ǽ̳�Thread�ģ����ڸĳ�ʹ�ü�ʱ���Ķ��̷߳���
	
    private HashMap<String, InfoItem> sendRouteInfo;  // ȫ�ֱ�����sendStr��ΪsendRoutInfo
    private InetAddress loc;
    static String IP = "127.0.0.1";
    private DatagramSocket sender;
    private HashMap<String, NeighborItem> neighborTable;

    private int logSeq;  // ��־���
    private int subSeq;  // ������־������ţ���ʼ��Ϊ0
    private String logDir;  // ��־·��
    private String myId;  // ����д������־

    // �Ķ����ڶ�������ԭ����sendStr����������sendRouteInfo���ڷ����߳��ڲ�����ת��Ϊ�ַ�����
    // ��������ԭ���ǣ�writeSendLog�����ڷ����߳��ڲ�ʵ�֣�����Ҫ��sendRoutInfoΪ������
    // �����Ķ������ڽ�infoToString�������ڷ����߳��ڲ���
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
        // ��sendRouteInfo�ֶ�ת�����ַ�����
        // ��ʽ���ɿո��������Ԫ��(dst, src, dis)
        String sendStr = "";
        for(String dst : sendRouteInfo.keySet()) {
            InfoItem iItem = sendRouteInfo.get(dst);
            sendStr += ( "(" + dst + "," + iItem.srcNode + "," + String.valueOf(iItem.distance) + ")");
            sendStr += " ";
        }

        sendStr = sendStr.substring(0,sendStr.length()-1);  // ɾ��ĩβ����Ŀո�
        sendStr += "@";  // ��Ϊ��Ч��Ϣ������־
        return sendStr;
    }


    private void writeSendLog() throws Exception{
        // sendRoutInfo��ʽ��
        // dst1: (src, dis1)
        // dst2: (src, dis2)
        BufferedWriter writer = new BufferedWriter(new FileWriter(logDir, true));  // ��׷�ӷ�ʽ��

        // дͷ��Ϣ
        String line;
        line = "Sent. Source Node = " + myId + "; Sequence Number = " + logSeq + "-" + (++subSeq) + "\n";
        writer.append(line);

        // ����д·����Ϣ
        for(String dst : sendRouteInfo.keySet()) {
            InfoItem iItem = sendRouteInfo.get(dst);
            line = "DestNode = " + dst + "; Distance = " + iItem.distance + "\n";
            writer.append(line);
        }
        writer.append("\n");  // �س�������һ�ε���־�ָ���
        writer.close();
    }

    public void run() {  // ��дrun()��������Ϊ�߳� �Ĳ�������
        try {
            writeSendLog();  // д������־

            String sendStr = infoToString(this.sendRouteInfo);   // ��sendRoutInfoת��ΪsendStr����ʾ�����������з��͵�·����Ϣ
            byte[] send_byte = sendStr.getBytes("UTF-8");

            for(String dst : neighborTable.keySet()){  //�����ھӱ�������Ϣ
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


    // ������������ϵͳ�����ļ�sysconfig.txt
    static int freq;  // ���ͼ������λms
    static float inf;  // �涨�Ĳ��ɴ����
    static int maxValidTime;  // ��ʱ�������λms
    // �����������������в���args
    static String myId;  // �ҵ�id
    static int SERVERPORT; // �ҵĽ��ն˿ڣ��������в������
    //static int SENDERPORT = 41659; //�ҵķ��Ͷ˿ڣ��������ü���
    static String IP = "127.0.0.1";

    // ���ݱ�
    static private HashMap<String, TableItem> routeTable = new HashMap<>();  // ·�ɱ�����Ŀ�Ľڵ�id
    static private HashMap<String, NeighborItem> neighborTable = new HashMap<>();  // �ھӾ��������Ŀ�Ľڵ�id

    // ��־
    static private String logDir;  // ��־·��
    static private int logSeq; // ��־���

    private static void init(String[] args) throws Exception {
        // dvsim x 51000 x.txt

        BufferedReader reader;
        String line;
        // Step1.��ȡϵͳ�����ļ�����ʼ��ȫ�ֱ���
        reader = new BufferedReader(new FileReader("../../../config2/sysconfig.txt"));  // TODO �����ֽ���ʱ·�����../
        line = reader.readLine().split("=")[1];
        freq = Integer.parseInt(line);
        line = reader.readLine().split("=")[1];
        inf = Float.parseFloat(line);
        line = reader.readLine().split("=")[1];
        maxValidTime = Integer.parseInt(line);
        //System.out.println(args[0]+"  "+args[1]+"  "+args[2]);
        myId = args[0]; //ԭ����1
        SERVERPORT = Integer.parseInt(args[1]);  //ԭ����2

        // Step2. ��ȡx.txt����ʼ���ھӱ�
        // y 4.0 52002
        // z 50.0 52003
        reader = new BufferedReader(new FileReader("../../../config2/" + args[2]));  // TODO �����ֽ���ʱ·�����../,ԭ����3��
        while((line = reader.readLine()) != null) {
            String[] strs = line.split(" ");
            NeighborItem nItem = new NeighborItem(Float.parseFloat(strs[1]), Integer.parseInt(strs[2]));
            neighborTable.put(strs[0], nItem);  // �ھӱ�����һ��
        }

        // Step3. �����ھӾ������ʼ��·�ɱ�
        // y : (y, 4.0)
        // z : (z, 50.0)
        for(String dst : neighborTable.keySet()) {
            NeighborItem nItem = neighborTable.get(dst);
            TableItem tItem = new TableItem(dst, nItem.distance);  // ֱ���һ������dst
            routeTable.put(dst, tItem);  // ·�ɱ�����һ��
        }

        // Step4. ΪlogDir��ֵ, �������־�ļ�������
        logDir = "../../../log2/log_" + myId + ".txt";
        BufferedWriter writer = new BufferedWriter(new FileWriter(logDir));  // ��д��ʽ��
        writer.close();

    }

    private static HashMap<String, InfoItem> genRouteInfo() {
        // ����·�ɱ�����·����Ϣ
        // ·�ɱ�:
        // y : (y, 4)
        // z : (z, 50)

        // ·����Ϣ:
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
        // ���ַ���ת����recvRouteInfo
        // ��ʽ���ɿո��������Ԫ��(dst, src, dis)
        HashMap<String, InfoItem> recvRouteInfo = new HashMap<>();

        recvStr = recvStr.substring(0, recvStr.indexOf("@"));  // ��ȡ@֮ǰ�Ĳ��֣�������@
        String[] strs = recvStr.split(" ");
        // System.out.println("The length of strs :" + strs.length());
        for(String tuple:strs) {
            //System.out.println("Ԫ�鳤�ȣ�" + tuple.length());
            tuple = tuple.substring(1, tuple.length()-1);  // ȥ��ͷβ��(��)��������ʱ�ᱨ���ƺ���tuple.length��1С����
            //System.out.println(tuple);
            String[] elems = tuple.split(",");
            InfoItem iItem = new InfoItem(elems[1], Float.parseFloat(elems[2]));
            recvRouteInfo.put(elems[0], iItem);
        }

        return recvRouteInfo;
    }

    private static void writeRecvLog(HashMap<String,List<InfoItem>> Buffer, int seqRecv, String logDir) throws Exception{
        // BUffer��ʽ��
        // src1:[(dst1, dis1), (dst2, dis2)]
        // src2:[(dst1, dis3), (dst2, dis4)]
        BufferedWriter writer = new BufferedWriter(new FileWriter(logDir, true));  // ��׷�ӷ�ʽ��

        // дͷ��Ϣ
        String line;
        line = "Received. Source Node = " + myId + "; Sequence Number = " + seqRecv + "\n";
        writer.append(line);

        // ����д·����Ϣ
        for(String src : Buffer.keySet()) {
            for(InfoItem iItem : Buffer.get(src)) {
                // ע�⣺����srcNode����ʵ�ʴ������dst
                line = "DestNode = " + iItem.srcNode + "; Distance = " + iItem.distance + "; Neighbor = " + src + "\n";
                writer.append(line);
            }
        }

        writer.append("\n");  // �س�������һ�ε���־�ָ���
        writer.close();
    }

    private static HashMap<String, TableItem> Distance_Vector(HashMap<String,List<InfoItem>> Buffer, Set<String> reachableList){  //���ݸ�������·�ɱ� ��ִ��DV�㷨
        //��ʼ����ʱ��ѵ����е�ľ��붼����ΪMax���У�
        HashMap<String, TableItem> newrouteTable = new HashMap<>();

        // Step1. ��ʼ�������пɴ�ڵ�ľ���Ϊinf.(�ɴ�ڵ�:�����ھӵ�·����Ϣ��dst�Ĳ���)
        for (String i : reachableList) {
             newrouteTable.put(i, new TableItem("NULL", inf));
        }

        // newrouteTable.put(myId, new TableItem("NULL",0));  // ���Լ��ľ�����Ϊ0.���ڲ���Ҫ,��Ϊ��·�ɱ��в�����dst���Լ�����

        // Step2. ��ʼ���������ھӽڵ�ľ���,�����ھӱ��ʼ��
        for(String neibour : neighborTable.keySet() ) {
            float cost = neighborTable.get(neibour).distance;
            newrouteTable.put(neibour, new TableItem(neibour,cost));
        }

        // ��ÿ���ھӷ�������Ϣ���е���, ����·�ɱ�
        for(String neibour : Buffer.keySet()) {
            float to_neibour = neighborTable.get(neibour).distance; // ����ҵ��ھӵľ���

            List<InfoItem> from_neibour = Buffer.get(neibour);
            for(InfoItem iItem : from_neibour) {
                String target = iItem.srcNode ; // ��ʱitem.srcʵ������target

                float cost = iItem.distance + to_neibour;
                if(cost < newrouteTable.get(target).distance) {
                    newrouteTable.put(target, new TableItem(neibour,cost));
                }
            }
        }

        newrouteTable.remove(myId);  //��·�ɱ��в�����dst���Լ�����

        //for(String item: newrouteTable.keySet() ) {
            //System.out.println("The item is:" + item + " distance: " + newrouteTable.get(item).distance);
        //}

        //·�ɱ����������Ϣ��1.���Լ���Ϊ0��2.���ھӣ����Ǹı��ھӱ����򲻸ģ�
        return newrouteTable;
    }
    

    public static void main(String args[]) throws Exception {

        init(args);  // ��ʼ���ھӱ��·�ɱ�
        DatagramSocket server = new DatagramSocket(SERVERPORT);


        // ģ�ⳣ����·������ʼ�շ��ͺͽ���·����Ϣ�� ÿ��ѭ������ǰ����·�ɱ�
        while (true){
            System.out.println("��" + (logSeq+1) + "�ַ���/����...");
        	Timer timer = new Timer(true);
            Set<String> reachableList = new HashSet<String>();  // �ӱ����յ���·����Ϣ�У���ȡ�ɴ�ڵ�
            HashMap<String,List<InfoItem> > Buffer = new HashMap<String,List<InfoItem> >();  // ���汾��ѭ�����յ���·����Ϣ

			try {
			    server.setSoTimeout(maxValidTime);  // ���ó�ʱʱ�䣬���ڴ�ʱ����δ�յ�����ȫ���ھӵ�·�ɱ�������ʱ�߼�����Ϊ���ھӹ���
                HashMap<String, InfoItem> sendRouteInfo = genRouteInfo();  // ���ݵ�ǰ·�ɱ�����·����Ϣ
				MySender mt1 = new MySender(server, sendRouteInfo, neighborTable, ++logSeq, logDir, myId);  //��ʼ�����ͽ��̡���ʱ��ÿһ���ھӷ���·����Ϣ��ֻ��Ҫ��ʼ��һ��
				InetAddress loc = InetAddress.getByName(IP);   //����
		       
                byte[] buf = new byte[200];   // ע�⣬��һ�������ܴ������
		        DatagramPacket repacket = new DatagramPacket(buf, buf.length, loc, SERVERPORT);  // �շ�����һ���˿�
		       
		        timer.schedule(mt1, 0, freq);  // ������ʱ��
		        while(true) {  // �������ձ��ġ�����������������1.�յ������ھӵ�·����Ϣ 2.��ʱ
		            try {
		                // Step1. ����reMsg��ת��ΪrecvRouteInfo
		                server.receive(repacket);
		                String reMsg = new String(repacket.getData());
		                //System.out.println(reMsg);  //С�����reMsg��֮��ܶ��ǲ�֪�����ݣ���
                        HashMap<String, InfoItem> recvRouteInfo = stringToInfo(reMsg); // ԭ����tmpTable������ھӷ�����·�ɱ�

                        // Step2. ����recvRouteInfo�����buffer
		                String src = "";
                        List<InfoItem> tmpList = new ArrayList<>();  // ԭ����tmpStore��Ԫ�ظ�ʽ��(dst, dis)
		                for(String dst : recvRouteInfo.keySet()) {
		                	InfoItem iItem = recvRouteInfo.get(dst);
		                	src = iItem.srcNode;   // �����ҵ��ھ���˭
		                	reachableList.add(dst);  // ά��reachableList
		                	tmpList.add(new InfoItem(dst,iItem.distance));
		                }
		                
		                Buffer.put(src, tmpList); //src�ڱ��д��ڣ��򸲸ǣ������������

		                if(Buffer.size() == neighborTable.size()) {  //ֹͣ���գ�Ҳֹͣ����
		                	break;
		                }

		            } catch (IOException e) {
		                System.out.println("Time out!Some Node might dead!");
		            }
		       }

			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			
			timer.cancel();  // ֹͣ����
            writeRecvLog(Buffer, logSeq, logDir);  // д������־
			
			Thread.currentThread().sleep(3000);  // ��һ����Ŀ���ǲ��Ƿ�ֹ�������ͣ�

			routeTable = Distance_Vector(Buffer, reachableList);  //���ݽ��յ���Buffer����·�ɱ�


			/*System.out.println("Please Input choice!");
			Scanner input=new Scanner(System.in);
			flag=input.next();
			if(flag=="P") {
			//����ͣ
			}
			else if(flag=="S") {
				break;   //ֹͣѭ��
			}
			else if(flag=="A") {
			//�����޸Ľڵ���Ϣ
			}	
			else {
				System.out.println("Input error!");
			}*/
			
		}
    }
}
