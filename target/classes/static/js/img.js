/**
 * Created by zmcc on 17/3/15.
 */
$(function () {

    function getImages() {
        $.ajax({
            type: "GET",
            dataType: "json",
            url: "/git/images",
            success: function (data) {
                var repos = data.repositories;
                if (repos.length <= 0) {
                    return;
                }
                $("#imgtable").html("");
                for (i = 0; i < repos.length; i++) {
                    var clz = "success";
                    if (i % 2 == 0) {
                        clz = "fail";
                    }
                    var img = "<tr class='" + clz + "'><td>" + (i + 1) + "</td><td>" + repos[i] + "</td><td><button img='" + repos[i] + "' class='btn' type='button'>部署该镜像</button></td></tr>";
                    $("#imgtable").append(img);
                }
            }
        });


        $.ajax({
            type: "GET",
            dataType: "json",
            url: "/git/services",
            success: function (data) {
                var items = data.items;
                if (items.length <= 0) {
                    return;
                }
                $("#servicetable").html("");
                for (i = 0; i < items.length; i++) {
                    var clz = "success";
                    if (i % 2 == 0) {
                        clz = "fail";
                    }
                    var service = "<tr class='" + clz + "'><td>" + (i + 1) + "</td><td>" + items[i].metadata.name + "</td><td>" +
                        "<a popover='popover' class='btn' data-content='" + JSON.stringify(items[i]) + "'>详情</a>"
                        + "</td>" +
                        "<td></td></tr>";
                    $("#servicetable").append(service);
                    $("a[popover=popover]").popover();
                }
            }
        });
    }

    $("body").on('click', '[img=img]', function () {
        var name = $(this).attr("name");
        window.open("container.html?name=" + name);
    });
    getImages();
    setInterval(getImages, 5000);

})