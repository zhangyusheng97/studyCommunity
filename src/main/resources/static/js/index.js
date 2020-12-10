$(function () {
    $("#publishBtn").click(publish);
});

function publish() {
    $("#publishModal").modal("hide");
    //发出请求前将csrf标签设置到消息的请求头中
    // var token = $("meta[name='_csrf']").attr("content");
    // var header = $("mata[name='_csrf_header']").attr("content");

    // $(document).ajaxSend(function (e,xhr,options) {
    //     xhr.setRequestHeader(header,token);
    // });

    //获取标签的的title和内容
    var title = $("#recipient-name").val();
    var content = $("#message-text").val();
    //发送异步的请求
    $.post("discuss/add",
        {
            title: title,
            content: content
        },
        function (data) {
            data = $.parseJSON(data);
            //在提示框中显示返回的消息
            $("#hintBody").text(data.msg);
            //显示提示框
            $("#hintModal").modal("show");
            //2秒后自动隐藏
            setTimeout(function () {
                $("#hintModal").modal("hide");
                if (data.code == 0) {
                    window.location.reload();
                }
            }, 2000);
        });
}