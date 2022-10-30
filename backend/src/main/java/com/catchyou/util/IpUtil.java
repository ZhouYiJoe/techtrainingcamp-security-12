package com.catchyou.util;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.*;
import java.util.Enumeration;

@Slf4j
public class IpUtil {

    private static final String LOCAL_IP = "127.0.0.1";
    /**
     * 获取IP地址
     *
     * 使用Nginx等反向代理软件， 则不能通过request.getRemoteAddr()获取IP地址
     * 如果使用了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP地址，X-Forwarded-For中第一个非unknown的有效IP字符串，则为真实IP地址
     */
    public static String getIpAddr(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return "0:0:0:0:0:0:0:1".equals(ip) ? LOCAL_IP : ip;
    }


    /**
     * 根据ip获取归属地
     * @param ip
     * @return
     */
    public static String getAddress(String ip) {
        // IP地址查询
        final String IP_URL = "http://whois.pconline.com.cn/ipJson.jsp?ip="+ip;

        // 未知地址
        final String UNKNOWN = "未知";
        // 内网不查询
        if (internalIp(ip)) {
            return "内网";
        }
        try {
            String rspStr = HttpUtil.get(IP_URL);
            if (StringUtils.isEmpty(rspStr)) {
                log.error("获取地理位置异常 {}", ip);
                return UNKNOWN;
            }
            String substring = rspStr.substring(rspStr.lastIndexOf("{"), rspStr.indexOf("}")+1);
            JSONObject jsonObject = JSONUtil.parseObj(substring);
            return jsonObject.getStr("addr");
        } catch (Exception e) {
            log.error("获取地理位置异常 {}", ip);
        }
        return UNKNOWN;
    }

    // 获取MAC地址的方法
    public static String getMACAddress(String ip) throws Exception {
        // 获得网络接口对象（即网卡），并得到mac地址，mac地址存在于一个byte数组中。
        InetAddress ia = InetAddress.getByName(ip);
        byte[] mac = NetworkInterface.getByInetAddress(ia).getHardwareAddress();
        // 如果没有访问网卡信息的权限，则返回null，一般本地网卡的信息是没有权限访问的
        if (mac == null) return null;
        // 下面代码是把mac地址拼装成String
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            if (i != 0) {
                sb.append("-");
            }
            // mac[i] & 0xFF 是为了把byte转化为正整数
            String s = Integer.toHexString(mac[i] & 0xFF);
            // System.out.println("--------------");
            // System.err.println(s);
            sb.append(s.length() == 1 ? 0 + s : s);
        }
        // 把字符串所有小写字母改为大写成为正规的mac地址并返回
        return sb.toString().toUpperCase();
    }


    /**
     * 查看是否内部ip
     * @param ip
     * @return
     */
    public static boolean internalIp(String ip) {
        boolean res = false;
        byte[] addr = textToNumericFormatV4(ip);
        if (addr != null && ip != null) {
            res = internalIp(addr) || LOCAL_IP.equals(ip);
        }
        return res;
    }

    private static boolean internalIp(byte[] addr) {
        final byte b0 = addr[0];
        final byte b1 = addr[1];
        // 10.x.x.x/8
        final byte SECTION_1 = 0x0A;
        // 172.16.x.x/12
        final byte SECTION_2 = (byte) 0xAC;
        final byte SECTION_3 = (byte) 0x10;
        final byte SECTION_4 = (byte) 0x1F;
        // 192.168.x.x/16
        final byte SECTION_5 = (byte) 0xC0;
        final byte SECTION_6 = (byte) 0xA8;
        boolean flag = false;
        switch (b0) {
            case SECTION_1:
                flag = true;
                break;
            case SECTION_2:
                if (b1 >= SECTION_3 && b1 <= SECTION_4) {
                    flag = true;
                }
                break;
            case SECTION_5:
                if (b1 == SECTION_6) {
                    flag = true;
                }
                break;
            default:
                break;
        }
        return flag;
    }

    /**
     * 将IPv4地址转换成字节
     *IPv4地址
     * @param text
     * @return byte 字节
     */
    public static byte[] textToNumericFormatV4(String text) {
        if (text.length() == 0) {
            return null;
        }

        byte[] bytes = new byte[4];
        String[] elements = text.split("\\.", -1);
        try {
            long l;
            int i;
            switch (elements.length) {
                case 1:
                    l = Long.parseLong(elements[0]);
                    if ((l < 0L) || (l > 4294967295L)) {
                        return null;
                    }
                    bytes[0] = (byte) (int) (l >> 24 & 0xFF);
                    bytes[1] = (byte) (int) ((l & 0xFFFFFF) >> 16 & 0xFF);
                    bytes[2] = (byte) (int) ((l & 0xFFFF) >> 8 & 0xFF);
                    bytes[3] = (byte) (int) (l & 0xFF);
                    break;
                case 2:
                    l = Integer.parseInt(elements[0]);
                    if ((l < 0L) || (l > 255L)) {
                        return null;
                    }
                    bytes[0] = (byte) (int) (l & 0xFF);
                    l = Integer.parseInt(elements[1]);
                    if ((l < 0L) || (l > 16777215L)) {
                        return null;
                    }
                    bytes[1] = (byte) (int) (l >> 16 & 0xFF);
                    bytes[2] = (byte) (int) ((l & 0xFFFF) >> 8 & 0xFF);
                    bytes[3] = (byte) (int) (l & 0xFF);
                    break;
                case 3:
                    for (i = 0; i < 2; ++i) {
                        l = Integer.parseInt(elements[i]);
                        if ((l < 0L) || (l > 255L)) {
                            return null;
                        }
                        bytes[i] = (byte) (int) (l & 0xFF);
                    }
                    l = Integer.parseInt(elements[2]);
                    if ((l < 0L) || (l > 65535L)) {
                        return null;
                    }
                    bytes[2] = (byte) (int) (l >> 8 & 0xFF);
                    bytes[3] = (byte) (int) (l & 0xFF);
                    break;
                case 4:
                    for (i = 0; i < 4; ++i) {
                        l = Integer.parseInt(elements[i]);
                        if ((l < 0L) || (l > 255L)) {
                            return null;
                        }
                        bytes[i] = (byte) (int) (l & 0xFF);
                    }
                    break;
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            log.error("数字格式化异常",e);
            return null;
        }
        return bytes;
    }

    /**
     * 获取本地ip
     * @return
     */
    public static String getLocalIP() {
        String ip = "";
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            InetAddress addr;
            try {
                addr = InetAddress.getLocalHost();
                ip = addr.getHostAddress();
            } catch (UnknownHostException e) {
                log.error("获取失败",e);
            }
            return ip;
        } else {
            try {
                Enumeration<?> e1 = (Enumeration<?>) NetworkInterface
                        .getNetworkInterfaces();
                while (e1.hasMoreElements()) {
                    NetworkInterface ni = (NetworkInterface) e1.nextElement();
                    if (!ni.getName().equals("eth0")) {
                        continue;
                    } else {
                        Enumeration<?> e2 = ni.getInetAddresses();
                        while (e2.hasMoreElements()) {
                            InetAddress ia = (InetAddress) e2.nextElement();
                            if (ia instanceof Inet6Address) {
                                continue;
                            }
                            ip = ia.getHostAddress();
                            return ip;
                        }
                        break;
                    }
                }
            } catch (SocketException e) {
                log.error("获取失败",e);
            }
        }
        return "";
    }
}

