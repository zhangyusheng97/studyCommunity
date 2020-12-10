$(function(){
	$(".follow-btn").click(follow);
});

function follow() {
	var entityId = $("#entityId").val();
	var btn = this;
	if ($(btn).hasClass("btn-info")) {
		$.post(
			"/follow",
			{
				entityType: 3,
				entityId: entityId
			},
			function (data) {
				data = $.parseJSON(data);
				if (data.code == 0) {
					//成功之后
					window.location.reload();
				} else {
					alert(data.msg);
				}
			}
		);
	}else{
		//取消关注
		$.post(
			"/unfollow",
			{
				entityType: 3,
				entityId: entityId
			},
			function (data) {
				data = $.parseJSON(data);
				if (data.code == 0) {
					//成功之后
					window.location.reload();
				} else {
					alert(data.msg);
				}
			}
		);
	}
		// 关注TA
	// 	$(btn).text("已关注").removeClass("btn-info").addClass("btn-secondary");
		// 取消关注
	// 	$(btn).text("关注TA").removeClass("btn-secondary").addClass("btn-info");
}