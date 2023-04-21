package com.navinfo.collect.library.utils;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * 
 * @author manle
 * 
 * Contains function adapt date times
 */

public class DateUtils {

	/**
	 * Get date after sync from NTP
	 * @param hosts {@link String[]} hosts server NTP
	 * @param timeOut {@link Integer} milisecod
	 * @return Date
	 */
	public static Date getNTPDate(String[] hosts, int timeOut) {
        NTPUDPClient client = new NTPUDPClient();
        client.setDefaultTimeout(timeOut);
        for (String host : hosts) {
            try {
                InetAddress hostAddr = InetAddress.getByName(host);
                TimeInfo info = client.getTime(hostAddr);
                NtpV3Packet message = info.getMessage();
                long serverTime = message.getTransmitTimeStamp().getTime();

                Date date = new Date(serverTime);
                return date;

            }
            catch (IOException e) {
            }
        }
        client.close();
        return null;
    }
	
	/**
	 * Format time
	 * @param date
	 * @return
	 */
	public static String formatTime(final Date date){
		SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
		String time = timeFormat.format(date);
		return time;
		
	}
}
