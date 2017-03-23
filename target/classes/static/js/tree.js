/**
 * Created by zmcc on 17/3/14.
 */
$(function () {

    var code = qs("code");
    var setting = {
        check: {
            enable: true,
            chkboxType: {"Y": "", "N": ""}
        },
        view: {
            dblClickExpand: false
        },
        data: {
            simpleData: {
                enable: true
            }
        },
        callback: {
            beforeClick: beforeClick,
            onCheck: onCheck
        }
    };


    var zNodes = [];

    function beforeClick(treeId, treeNode) {
        var zTree = $.fn.zTree.getZTreeObj("treeDemo");
        zTree.checkNode(treeNode, !treeNode.checked, null, true);
        return false;
    }

    function onCheck(e, treeId, treeNode) {
        var zTree = $.fn.zTree.getZTreeObj("treeDemo"),
            nodes = zTree.getCheckedNodes(true),
            v = "";
        for (var i = 0, l = nodes.length; i < l; i++) {
            v += nodes[i].name + ",";
        }
        if (v.length > 0) v = v.substring(0, v.length - 1);
        var cityObj = $("#selectdockfile");
        cityObj.attr("value", v);

        //treeNode.open = true;
        console.log("selected file:" + treeNode.path);
        $("#selectedFile").val("/" + treeNode.path);
        getNodes(treeNode);
    }


    $("[popup=true]").click(function (e) {
        zNodes = [];
        console.log("pop up ......");
        getNodes(null);
        var dockObj = $("#selectdockfile");
        var dockOffset = $("#selectdockfile").offset();

        $("#menuContent").css({
            left: dockOffset.left + "px",
            top: dockOffset.top + dockObj.outerHeight() + "px"
        }).slideDown("fast");

        $("body").bind("mousedown", onBodyDown);

    });

    function hideMenu() {
        $("#menuContent").fadeOut("fast");
        $("body").unbind("mousedown", onBodyDown);
    }

    function onBodyDown(event) {
        if (!(event.target.id == "menuBtn" || event.target.id == "selectdockfile" || event.target.id == "menuContent" || $(event.target).parents("#menuContent").length > 0)) {
            hideMenu();
        }
    }


    function getNodes(node) {
        var projectId = $("option:selected", $("#projselect")).val();
        var branch = $("#branchselect").val();

        console.log("当前选中项目及分支:" + projectId + " " + branch);

        $.ajax({
            url: "/git/tree",
            type: "GET",
            dataType: "json",
            data: {
                projectId: projectId,
                branch: branch,
                path: node == null ? "" : node.path,
                code: code
            },
            success: function (data) {
                if (data.length <= 0) {
                    return;
                }
                for (i = 0; i < data.length; i++) {
                    var item = {
                        open: true,
                        nocheck: (data[i].type == "tree") ? false : true,
                        id: data[i].id,
                        pId: node == null ? 0 : node.id,
                        name: data[i].name,
                        path: node == null ? data[i].name : (node.path + "/" + data[i].name)
                    };
                    zNodes.push(item);
                }
                $.fn.zTree.init($("#treeDemo"), setting, zNodes);
            }
        });
    }


    function qs(key) {
        key = key.replace(/[*+?^$.\[\]{}()|\\\/]/g, "\\$&"); // escape RegEx meta chars
        var match = location.search.match(new RegExp("[?&]" + key + "=([^&]+)(&|$)"));
        return match && decodeURIComponent(match[1].replace(/\+/g, " "));
    }

    $(document).ready(function () {
        $.fn.zTree.init($("#treeDemo"), setting, zNodes);
    });

})
