# tank-battle-tcp-server（接入层 / Gateway）
## 职责
1. 维护与客户端的长连接（TCP / WebSocket） 
2. 协议编解码（客户端协议） 
3. 连接管理、心跳、限流、简单鉴权 
4. 把客户端请求转发给业务服务器 
5. 把业务服务器的响应 / 推送转发给对应客户端
6. TcpServer <--> 业务服务器 之间通信采用 gRPC

## Handler 设计
1. LoggingHandler（Sharable）：记录“有一个包进来了 / 出去了”，方便排查网络问题，不关心具体是创建房间还是聊天。
2. FrameDecoder（每连接一个）：确保每次往下传的是“一个完整的消息帧”，不被粘包/拆包影响。
3. ChecksumHandler（Sharable）：用CRC32校验协议头中的校验码
4. GameMessageDecoder（Sharable）：把“原始二进制包”翻译成“有语义的 GameMessage 对象”，标明这是 CREATE_ROOM / JOIN_ROOM / CHAT_PUBLIC。
5. GameMessageEncoder（Sharable）：把业务层构造的 GameMessage（比如“创建成功”、“新玩家加入”、“聊天内容”）编码成字节发给客户端。
6. SessionHandler（每连接一个，有状态）：维护这个连接对应哪个玩家playerId、登录状态
7. GameDispatchHandler（Sharable） 根据 msgType 把GameMessage组装成内部DTO，通过gRPC转发给业务服务器。接到响应后转成GameMessage。

**注意，这里列的顺序就是add到pipeline中的顺序。入站会按照1-7执行，出站则是反过来从7-1执行**