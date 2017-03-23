package com.cmcc.service;

import com.cmcc.domain.Constant;
import com.cmcc.domain.Logs;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * Created by zmcc on 17/3/23.
 */
@Service
public class LogService {

    public static Boolean started = false;

    @Autowired
    private SimpMessagingTemplate template;

    public void broadcastLog() {
        try {
            File f = new File(Constant.BUILD_LOG_LOCATION);
            System.out.println("file:" + f.exists());
            long length = f.length();
            RandomAccessFile rcs = new RandomAccessFile(f, "rw");
            // jump to end of file
            if (Constant.POS == 0) {
                rcs.seek(length);
            }
            while (Constant.READ_LOG) {
                String line = rcs.readLine();
                if (line == null) {
                    Thread.sleep(1000);
                    continue;
                }
                Constant.POS += rcs.getFilePointer();
                template.convertAndSend("/topic/logs", new Logs(line));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}