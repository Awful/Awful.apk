$(document).ready(function() {
    var thread = $("#thread-body");

    var posts = JSON.parse(post_list);
    console.log(posts);
    for (var i = 0; i < posts.length; i++) {
        current = JSON.parse(posts[i]);
        var post_data = {
            username: current.username,
            postdate: current.date,
            avatar: current.avatar,
            content: current.content
        };

        thread.append(ich.post(post_data));
    }
});
