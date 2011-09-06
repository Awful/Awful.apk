$(document).ready(function() {
    var thread = $("#thread-body");

    var posts = [
        {
            username: 'Ferg',
            date: 'Sep 3, 2011',
            avatar: 'http://fi.somethingawful.com/safs/titles/9e/ab/00115838.0002.jpg',
            content: '<div class="bbc-block"><h4>falz posted:</h4><blockquote>We\'ll still be able to have a dark background option, right?<br /></blockquote></div><br />Yeah everything will be configurable<!-- google_ad_section_end --><!-- EndContentMarker --><p class="editedby">',
        },
        {
            username: 'Ferg',
            date: 'Sep 3, 2011',
            avatar: 'http://fi.somethingawful.com/safs/titles/9e/ab/00115838.0002.jpg',
            content: '<div class="bbc-block"><h4>falz posted:</h4><blockquote>We\'ll still be able to have a dark background option, right?<br /></blockquote></div><br />Yeah everything will be configurable<!-- google_ad_section_end --><!-- EndContentMarker --><p class="editedby">',
        },
        {
            username: 'Ferg',
            date: 'Sep 3, 2011',
            avatar: 'http://fi.somethingawful.com/safs/titles/9e/ab/00115838.0002.jpg',
            content: '<div class="bbc-block"><h4>falz posted:</h4><blockquote>We\'ll still be able to have a dark background option, right?<br /></blockquote></div><br />Yeah everything will be configurable<!-- google_ad_section_end --><!-- EndContentMarker --><p class="editedby">',
        },
        {
            username: 'Ferg',
            date: 'Sep 3, 2011',
            avatar: 'http://fi.somethingawful.com/safs/titles/9e/ab/00115838.0002.jpg',
            content: '<div class="bbc-block"><h4>falz posted:</h4><blockquote>We\'ll still be able to have a dark background option, right?<br /></blockquote></div><br />Yeah everything will be configurable<!-- google_ad_section_end --><!-- EndContentMarker --><p class="editedby">',
        },
        {
            username: 'Ferg',
            date: 'Sep 3, 2011',
            avatar: 'http://fi.somethingawful.com/safs/titles/9e/ab/00115838.0002.jpg',
            content: '<div class="bbc-block"><h4>falz posted:</h4><blockquote>We\'ll still be able to have a dark background option, right?<br /></blockquote></div><br />Yeah everything will be configurable<!-- google_ad_section_end --><!-- EndContentMarker --><p class="editedby">',
        },
        {
            username: 'Ferg',
            date: 'Sep 3, 2011',
            avatar: 'http://fi.somethingawful.com/safs/titles/9e/ab/00115838.0002.jpg',
            content: '<div class="bbc-block"><h4>falz posted:</h4><blockquote>We\'ll still be able to have a dark background option, right?<br /></blockquote></div><br />Yeah everything will be configurable<!-- google_ad_section_end --><!-- EndContentMarker --><p class="editedby">',
        },
        {
            username: 'Ferg',
            date: 'Sep 3, 2011',
            avatar: 'http://fi.somethingawful.com/safs/titles/9e/ab/00115838.0002.jpg',
            content: '<div class="bbc-block"><h4>falz posted:</h4><blockquote>We\'ll still be able to have a dark background option, right?<br /></blockquote></div><br />Yeah everything will be configurable<!-- google_ad_section_end --><!-- EndContentMarker --><p class="editedby">',
        },
        {
            username: 'Ferg',
            date: 'Sep 3, 2011',
            avatar: 'http://fi.somethingawful.com/safs/titles/9e/ab/00115838.0002.jpg',
            content: '<div class="bbc-block"><h4>falz posted:</h4><blockquote>We\'ll still be able to have a dark background option, right?<br /></blockquote></div><br />Yeah everything will be configurable<!-- google_ad_section_end --><!-- EndContentMarker --><p class="editedby">',
        },
    ];

    $('.post-cell').live('click', function(event) {
        listener.onPostClick($(this).attr('id'));
    });

    var posts = JSON.parse(post_list);

    var light = true;

    var lightReadColor = "#e7eff5";
    var darkReadColor = "#dbe8f5";

    var darkColor = "#e8e8e8";
    var lightColor = "#f4f4f4";

    var foundLastRead = false;

    for (var i = 0; i < posts.length; i++) {
        current = JSON.parse(posts[i]);
        // current = posts[i];

        var background;

        if (current.previouslyRead) {
            background = light ? lightReadColor : darkReadColor;
        } else {
            if (!foundLastRead) {
                true;
            }

            background = light ? lightColor : darkColor;
        }

        var post_data = {
            postId: current.id,
            username: current.username,
            postdate: current.date,
            avatar: current.avatar,
            content: current.content,
            background: background,
        };

        thread.append(ich.post(post_data));

        light = !light;
    }
});
