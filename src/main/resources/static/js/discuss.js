$(function () {
    $("#topBtn").click(setTop);
    $("#goodBtn").click(setGood);
    $("#deleteBtn").click(setDelete);
});


function like(btn,entityType,entityId,entityUserId,postId) {
    $.post(
        "/like",
        {
            entityType:entityType,
            entityId:entityId,
            entityUserId:entityUserId,
            postId:postId
        },
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0){
                //code为0表示接收成功了
                $(btn).children("i").text(data.likeCount);
                $(btn).children("b").text(data.likeStatus==1?'已赞':'赞');
            }else {
                //接收失败
                alert(data.msg);
            }
        }
    );
}

function setTop() {
    $.post(
        "/discuss/top",
        {
            id:$("#postId").val()
        },
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0){
                //接收成功
                $("#topBtn").attr("disabled","disabled");
            }else{
                alert(data.msg);
            }
        }
    );
}

function setGood() {
    $.post(
        "/discuss/good",
        {
            id:$("#postId").val()
        },
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0){
                //接收成功
                $("#goodBtn").attr("disabled","disabled");
            }else{
                alert(data.msg);
            }
        }
    );
}

function setDelete() {
    $.post(
        "/discuss/delete",
        {
            id:$("#postId").val()
        },
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0){
                //接收成功
                window.location.href = "/index";
            }else{
                alert(data.msg);
            }
        }
    );
}