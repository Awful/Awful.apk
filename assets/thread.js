$(document).ready(function() {
    $('.action-button').live('click', function(event) {
        if ($(this).hasClass("editable")) {
            listener.onEditablePostClick($(this).attr('id'), $(this).attr('lastreadurl'));
        } else {
            listener.onPostClick($(this).attr('id'), $(this).attr('lastreadurl'));
        }
    });

    var posts = JSON.parse(post_list);
    var prefs = JSON.parse(preferences);

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

        light = !light;

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
        };

        thread.append(ich.post(post_data));
    }

    var salr = new SALR(prefs);

    $(window).scrollTop($('.unread').first().offset().top);
});

$(window).load(function() {
    $(window).scrollTop($('.unread').first().offset().top);
});
