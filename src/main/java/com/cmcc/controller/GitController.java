package com.cmcc.controller;

import com.cmcc.domain.GitToken;
import com.cmcc.service.GitTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.UUID;

/**
 * Created by zmcc on 17/3/8.
 */
@RestController
@RequestMapping("/git")
public class GitController {

    @Autowired
    private GitTokenService tokenService;

    @RequestMapping(value = "/callback", method = RequestMethod.GET)
    public GitToken gitCallBack(@RequestParam("code") String code) {
        GitToken token = tokenService.getToken(code);
        return token;
    }

    /**
     * 获取用户项目列表
     *
     * @param code
     * @return
     */
    @RequestMapping(value = "/projects", method = RequestMethod.GET)
    public ResponseEntity<?> projects(@RequestParam("code") String code) {
        return new ResponseEntity<Object>(tokenService.getProjects(code), HttpStatus.OK);
    }

    /**
     * 获取项目分支
     *
     * @param pid
     * @param code
     * @return
     */
    @RequestMapping(value = "/projects/{pid}/branches", method = RequestMethod.GET)
    public ResponseEntity<?> branches(@PathVariable("pid") String pid, @RequestParam("code") String code) {
        return new ResponseEntity<Object>(tokenService.getBranches(pid, code), HttpStatus.OK);
    }

    /**
     * 编译镜像文件
     *
     * @param url
     * @param branch
     * @param dockerfile
     * @param name
     * @return
     */
    @RequestMapping(value = "/build", method = RequestMethod.POST)
    public ResponseEntity<?> build(@RequestParam("url") String url, @RequestParam("branch") String branch,
                                   @RequestParam("dockerfile") String dockerfile, @RequestParam("name") String name,
                                   @RequestParam(value = "cmd", defaultValue = "mvn clean install -DskipTests") String cmd) {
        if (branch == null || branch.trim().equals("")) {
            branch = "master";
        }

        if (dockerfile == null || dockerfile.trim().equals("")) {
            dockerfile = "./";
        }

        // http://
        int idx = url.indexOf("http://");
        url = "http://" + "zhengweihz:309122580qw@" + url.replaceAll("http://", "");

        cmd = "build " + url + " " + branch + " " + dockerfile + " " + name + " \"" + cmd + "\"";

        File f = new File("/tmp/docker/" + UUID.randomUUID().toString());
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            fos.write(cmd.getBytes());
            fos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return new ResponseEntity<Object>("success", HttpStatus.OK);
    }


    @RequestMapping("/tree")
    public ResponseEntity<?> tree(@RequestParam("projectId") String proj, @RequestParam("branch") String branch,
                                  @RequestParam(value = "path", required = false) String path, @RequestParam("code") String code) {
        return new ResponseEntity<Object>(tokenService.getTree(proj, branch, path, code), HttpStatus.OK);
    }

    /**
     * 获取当前镜像列表
     */
    @RequestMapping("/images")
    public ResponseEntity<?> images() {
        return new ResponseEntity<Object>(tokenService.getImages(), HttpStatus.OK);
    }

    @RequestMapping(value = "/deploy", method = RequestMethod.POST)
    public ResponseEntity<?> deploy(@RequestParam("appname") String appname,
                                    @RequestParam("name") String name,
                                    @RequestParam(value = "podnum", defaultValue = "1") String podnum,
                                    @RequestParam("containerPort") String containerPort,
                                    @RequestParam("nodePort") String nodePort,
                                    @RequestParam(value = "namespace", defaultValue = "default") String namespace) {
        String s = tokenService.deploy(appname, name, podnum, containerPort, nodePort, namespace);
        return new ResponseEntity<Object>(s, HttpStatus.OK);
    }

    @RequestMapping("/services")
    public ResponseEntity<?> services(@RequestParam(value = "namespace", defaultValue = "default") String namespace) {
        return new ResponseEntity<Object>(tokenService.getServices(namespace), HttpStatus.OK);
    }
}
