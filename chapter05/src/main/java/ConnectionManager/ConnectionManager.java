package ConnectionManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class ConnectionManager {
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    public static void setConnection(Socket s, PrintWriter w, BufferedReader r) {
        socket = s;
        out = w;
        in = r;
    }

    public static Socket getSocket() { return socket; }
    public static PrintWriter getWriter() { return out; }
    public static BufferedReader getReader() { return in; }

    public static void release() {
        // 关闭顺序：先关闭读写流，再关闭socket
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                in = null;
            }
        }

        if (out != null) {
            out.close();
            out = null;
        }

        if (socket != null) {
            try {
                // 避免关闭未连接或已关闭的socket
                if (!socket.isClosed() && socket.isConnected()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                socket = null;
            }
        }
    }

    // 检查连接是否有效
    public static boolean isConnectionActive() {
        return socket != null &&
                !socket.isClosed() &&
                socket.isConnected() &&
                !socket.isInputShutdown() &&
                !socket.isOutputShutdown();
    }

    // 安全发送消息
    public static boolean sendMessage(String message) {
        if (out != null && !out.checkError() && isConnectionActive()) {
            out.println(message);
            out.flush();
            return true;
        }
        return false;
    }
}