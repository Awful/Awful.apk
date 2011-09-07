$(document).ready(function() {
    var thread = $("#thread-body");

    var posts = [
        {
            username: 'Ferg',
            date: 'Sep 3, 2011',
            avatar: 'http://fi.somethingawful.com/safs/titles/9e/ab/00115838.0002.jpg',
            previouslyRead: "true",
            content: '<div class="bbc-block"><h4>falz posted:</h4><blockquote>We\'ll still be able to have a dark background option, right?<br /></blockquote></div><br />Yeah everything will be configurable<!-- google_ad_section_end --><!-- EndContentMarker --><p class="editedby">',
        },
        {
            username: 'Ferg',
            date: 'Sep 3, 2011',
            avatar: 'http://fi.somethingawful.com/safs/titles/9e/ab/00115838.0002.jpg',
            previouslyRead: "true",
            content: '<div class="bbc-block"><h4>falz posted:</h4><blockquote>We\'ll still be able to have a dark background option, right?<br /></blockquote></div><br />Yeah everything will be configurable<!-- google_ad_section_end --><!-- EndContentMarker --><p class="editedby">',
        },
        {
            username: 'Ferg',
            date: 'Sep 3, 2011',
            avatar: 'http://fi.somethingawful.com/safs/titles/9e/ab/00115838.0002.jpg',
            previouslyRead: "true",
            content: '<div class="bbc-block"><h4>falz posted:</h4><blockquote>We\'ll still be able to have a dark background option, right?<br /></blockquote></div><br />Yeah everything will be configurable<!-- google_ad_section_end --><!-- EndContentMarker --><p class="editedby">',
        },
        {
            username: 'Ferg',
            date: 'Sep 3, 2011',
            avatar: 'http://fi.somethingawful.com/safs/titles/9e/ab/00115838.0002.jpg',
            previouslyRead: "true",
            content: '<div class="bbc-block"><h4>falz posted:</h4><blockquote>We\'ll still be able to have a dark background option, right?<br /></blockquote></div><br />Yeah everything will be configurable<!-- google_ad_section_end --><!-- EndContentMarker --><p class="editedby">',
        },
        {
            username: 'Ferg',
            date: 'Sep 3, 2011',
            avatar: 'http://fi.somethingawful.com/safs/titles/9e/ab/00115838.0002.jpg',
            previouslyRead: "false",
            content: '<div class="bbc-block"><h4>falz posted:</h4><blockquote>We\'ll still be able to have a dark background option, right?<br /></blockquote></div><br />Yeah everything will be configurable<!-- google_ad_section_end --><!-- EndContentMarker --><p class="editedby">',
        },
        {
            username: 'Ferg',
            date: 'Sep 3, 2011',
            avatar: 'http://fi.somethingawful.com/safs/titles/9e/ab/00115838.0002.jpg',
            previouslyRead: "false",
            content: '<div class="bbc-block"><h4>falz posted:</h4><blockquote>We\'ll still be able to have a dark background option, right?<br /></blockquote></div><br />Yeah everything will be configurable<!-- google_ad_section_end --><!-- EndContentMarker --><p class="editedby">',
        },
        {
            username: 'Ferg',
            date: 'Sep 3, 2011',
            avatar: 'http://fi.somethingawful.com/safs/titles/9e/ab/00115838.0002.jpg',
            previouslyRead: "false",
            content: '<div class="bbc-block"><h4>falz posted:</h4><blockquote>We\'ll still be able to have a dark background option, right?<br /></blockquote></div><br />Yeah everything will be configurable<!-- google_ad_section_end --><!-- EndContentMarker --><p class="editedby">',
        },
        {
            username: 'Ferg',
            date: 'Sep 3, 2011',
            avatar: 'http://fi.somethingawful.com/safs/titles/9e/ab/00115838.0002.jpg',
            previouslyRead: "false",
            content: '<div class="bbc-block"><h4>falz posted:</h4><blockquote>We\'ll still be able to have a dark background option, right?<br /></blockquote></div><br />Yeah everything will be configurable<!-- google_ad_section_end --><!-- EndContentMarker --><p class="editedby">',
        },
    ];

    $('.action-button').live('click', function(event) {
        listener.onPostClick($(this).attr('id'));
    });

    var posts = JSON.parse(post_list);

    var light = true;

    /*
    var lightReadColor = preferences.backgroundColor;
    var darkReadColor = preferences.backgroundColor2;

    var lightColor = preferences.readBackgroundColor;
    var darkColor = preferences.readBackgroundColor2;
    */

    var lightReadColor = "#e7eff5";
    var darkReadColor = "#dbe8f5";

    var darkColor = "#e8e8e8";
    var lightColor = "#f4f4f4";

    var foundLastRead = false;

    for (var i = 0; i < posts.length; i++) {
        current = JSON.parse(posts[i]);
        // current = posts[i];

        var background;

        if (current.previouslyRead == "true") {
            background = light ? lightReadColor : darkReadColor;
        } else {
            background = light ? lightColor : darkColor;
        }

        var post_data = {
            postId: current.id,
            username: current.username,
            postdate: current.date,
            avatar: current.avatar,
            content: current.content,
            background: background,
            lastread: (current.previouslyRead == "true") ? "read" : "unread",
        };

        thread.append(ich.post(post_data));

        light = !light;
    }

    $(window).scrollTop($('.unread').first().offset().top);
});

$(window).load(function() {
    $(window).scrollTop($('.unread').first().offset().top);
});
