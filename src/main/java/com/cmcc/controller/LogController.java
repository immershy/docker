package com.cmcc.controller;

import com.cmcc.domain.Constant;
import com.cmcc.domain.Logs;
import com.cmcc.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * Created by zmcc on 17/3/13.
 */
@Controller
public class LogController {


    @Autowired
    private LogService logService;

    @MessageMapping("/logs")
    public void logs() throws Exception {
        if (logService.started) {
            return;
        }
        logService.broadcastLog();
    }

}
