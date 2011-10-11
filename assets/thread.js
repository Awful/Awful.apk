var prefs = JSON.parse(preferences);

$(document).ready(function() {
    $('.action-button').live('click', function(event) {
        if ($(this).hasClass("editable")) {
            listener.onEditablePostClick($(this).attr('id'), $(this).attr('lastreadurl'));
        } else {
            listener.onPostClick($(this).attr('id'), $(this).attr('lastreadurl'), $(this).attr('username'));
        }
    });

    var posts = JSON.parse(post_list);

    var thread = $("#thread-body");
    var light = true;

    for (var i = 0; i < posts.length; i++) {
        current = JSON.parse(posts[i]);

        var background;

        if (current.previouslyRead == "true") {
            background = light ? prefs.readBackgroundColor : prefs.readBackgroundColor2;
        } else {
            background = light ? prefs.backgroundColor : prefs.backgroundColor2;
        }
		if(prefs.alternateColors == "true"){
        light = !light;
		}
        var post_data = {
            postId: current.id,
            username: current.username,
            postdate: current.date,
            avatar: current.avatar,
            content: current.content,
            background: background,
            lastread: (current.previouslyRead == "true") ? "read" : "unread",
            lastreadurl: current.lastReadUrl,
            editable: (current.editable == "true") ? "editable" : "noneditable",
            fontColor: prefs.fontColor,
            fontSize: prefs.fontSize,
            opColor: (current.isOp == "true") ? prefs.OPColor : "",
        };

        thread.append(ich.post(post_data));
    }

    var salr = new SALR(prefs);

    setYPosition(prefs.yPos);
});

$(window).load(function() {
	if(prefs.postjumpid == ""){
    	setYPosition(prefs.yPos);
    }else{
		$(window).scrollTop($("#".concat(prefs.postjumpid)).first().offset().top);
    }
});

function setYPosition(position) {
    try {
        $(window).scrollTop($('.unread').first().offset().top);
    } catch (error) {
        if (position != "-1") {
            $(window).scrollTop(position);
        }
    }
}
