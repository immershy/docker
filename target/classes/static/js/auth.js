/**
 * Created by zmcc on 17/3/8.
 */
$(function () {
    var appId = '6556a0d49f94afd61cf94b56013871737fe380c964702153084033de140e750c';
    var callbackUrl = 'http://172.23.31.94:8888';
    var tokenUrl = 'http://git.komect.net/oauth/authorize?client_id=' + appId + '&redirect_uri=' + callbackUrl + '&response_type=code';

    var code = qs("code");

    var isSockOpen = false;
    var stompClient = null;


    /**
     * 判断当前是否登录
     */
    if (code == null) {
        showPage("page1")
    } else {
        showPage("page2")
        getProjectList(code);
    }

    function showPage(page) {
        $("div[page=page1]").hide();
        $("div[page=page2]").hide();
        $("div[page=page3]").hide();

        $("div[page=" + page + "]").show();
    }

    $("#gitauth").click(function () {
        window.location = tokenUrl;
    });

    var flushScreen = true;
    /**
     * 选中项目后,请求项目的分支,填充对应的list
     */
    $("body").on('change', 'select', function () {
        console.log("下拉菜单选中");
        var ele = $(this);

        if (!(ele.attr("id") === "projselect")) {
            console.log("type is not project, return .....");
            return;
        }

        var projectId = ele.val();
        console.log("选中的项目id:" + projectId);
        var output = "";
        $.ajax({
            url: "/git/projects/" + projectId + "/branches",
            dataType: "json",
            type: "GET",
            data: {
                code: code
            },
            success: function (data) {
                for (i = 0; i < data.length; i++) {
                    output += "<option " + (i == 0 ? "selected=selected" : "") + " value='" + data[i].name + "'>" + data[i].name + "</option>";
                }
                console.log("分支选中生成的子菜单: " + output);
                if (data.length > 0) {
                    $("#branchselect").html(output);
                }
            }
        });
    });

    /**
     * 部署该镜像
     */
    $("body").on('click', 'button', function () {
        var img = $(this).attr("img");
        if (img == null) {
            return;
        }

        $("#appname").val(img);
        $("#img").val(img);

        // 部署按钮提交
        $("#deploy").click(function () {
            $.ajax({
                url: "/git/deploy",
                type: "POST",
                dataType: "json",
                data: {
                    appname: $("#appname").val(),
                    name: $("#img").val(),
                    podnum: $("#podnum").val(),
                    namespace: $("#namespace").val(),
                    containerPort: $("#containerPort").val(),
                    nodePort: $("#nodePort").val()
                },
                success: function (data) {
                    alert(data);
                }
            })
        });

    });

    /**
     * 菜单
     */
    $("button").click(function () {
        var page = $(this).attr("page");
        if (page == null) {
            return;
        }
        showPage(page);
    });

    /**
     * 提交编译按钮点击事件
     */
    $("#submit").click(function () {
        var url = $("option:selected", $("#projselect")).attr("url");
        //var name = $("option:selected", $("#projselect")).attr("name");
        var name = $("#imgname").val();
        if (name == null) {
            name = $("option:selected", $("#projselect")).attr("name");
        }
        var branch = $("#branchselect").val();
        var dockerfile = $("#selectedFile").val();
        var cmd = $("input[name=cmd]").val();

        $.ajax({
                url: "/git/build",
                type: "POST",
                dataType: "json",
                data: {
                    url: url,
                    name: name,
                    branch: branch,
                    dockerfile: dockerfile,
                    cmd: cmd
                },
                success: function (data) {
                    console.log("镜像编译结果:" + data);
                }
            }
        );

        // 日志输出框清空
        flushScreen = true;

        if (!isSockOpen) {
            $("#screen").show();
            // open socket
            // connect -> subscribe
            connect();
        }
    });

    /**
     * 日志清空
     */
    $("#logclear").click(function () {
        $("#screen").val("");
    });

    function connect() {
        var socket = new SockJS("/log-websocket");
        stompClient = Stomp.over(socket);
        stompClient.connect({}, function (frame) {
            console.log("log web socket conncted....");
            isSockOpen = true;
            stompClient.subscribe("/topic/logs", function (logs) {
                if (flushScreen) {
                    flushScreen = false;
                    $("#screen").val("");
                }
                var message = JSON.parse(logs.body).content;
                console.log("msg received:" + message);
                var old = $("#screen").val();
                $("#screen").val(old + "\r\n" + message);
            });
            stompClient.send("/app/logs", {}, {});
        });
    }


    /**
     * 获取当前登录用户的项目列表
     * @param code
     */
    function getProjectList(code) {
        var output = "";
        $.ajax({
            url: "/git/projects",
            type: "GET",
            dataType: "json",
            data: {
                code: code
            },
            success: function (data) {
                for (i = 0; i < data.length; i++) {
                    output += "<option name='" + data[i].name + "' url='" + data[i].http_url_to_repo + "' " + (i == 0 ? "selected='selected'" : "") + " value='" + data[i].id + "'>" + data[i].path_with_namespace + "</option>";
                    //output += "<label><input name='" + data[i].name + "' type='radio' value='" + data[i].id + "' />" + data[i].path_with_namespace + "</label><br/>";
                }
                $("#projselect").html(output);
            }
        });
    }

    function qs(key) {
        key = key.replace(/[*+?^$.\[\]{}()|\\\/]/g, "\\$&"); // escape RegEx meta chars
        var match = location.search.match(new RegExp("[?&]" + key + "=([^&]+)(&|$)"));
        return match && decodeURIComponent(match[1].replace(/\+/g, " "));
    }

})
