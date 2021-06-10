## 数据结构

*下述TableItem和InfoItem类起到结构体的作用，JAVA中并不使用结构体。*

### 路由表 routeTable

HashMap<String,TableItem>，键为目的节点id，值为TableItem类。

定义如下：

```
class TableItem {
	String Neighbor;  // 邻居节点id。若直达，即目的结点id。
	float distance;  // 到达目的节点的距离或代价。可以不是整数
}

HashMap<String,TableItem> routeTable;
```

### 路由信息 routeInfo

HashMap<String, InfoItem>, 键为目的节点id，值为InfoItem类。

定义如下：

```
class InfoItem {
	String SrcNode;  // 发送节点id。
	String Neighbor;  // 对应于到目的节点的下一跳
	float Distance;  // 到达目的节点的距离或代价。可以不是整数
}

HashMap<String,InfoItem> routeInfo;
```



### 邻居表 neighborTable

HashMap<String, float>，键为邻居节点id，值为距离。

定义如下：

```
HashMap<String,float> neighborTable;
```





## 方法

### 读取

@brief: 根据x.txt，初始化邻居表



### 发送

@brief: 根据路由表，发送路由信息。

tip1：假如x到达z的下一跳是y，则x发送时不将该条目转发给y。

tip2：一定要转发以邻居节点为目的节点的这一项。即y是x的邻居，x一定会给y发送目的节点为y的项。

### 接收

@brief: 根据接收到的路由信息，更新路由表

情景：A接收到来自B的路由信息，更新A->C的路由表项。

设发送的路由信息中存在一项：
SrcNode：B
DestNode：C
Distance：10

case1.若A路由表中到达C的项的Neighbor字段为B。即设A路由表中存在一项：
DestNode：C
Distance：20
Neighbor：B
则无论如何，更新Distance为dis(A,B)+10。

case2.若A路由表中对应DestNode为C的一项的Neighbor字段不为B。即设A路由表中存在一项：
DestNode：C
Distance：20
Neighbor：X

则仅当dis(A,X)+10>20时，更新Distance和Neighbor。