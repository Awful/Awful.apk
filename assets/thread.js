$(document).ready(function() {
    var thread = $("#thread-body");
    
    var post_data = {
        username: "Ferg",
        postdate: "September 2, 2011",
        avatar: "http://fi.somethingawful.com/safs/titles/9e/ab/00115838.0002.jpg",
        content: '<div class="bbc-block"><h4>Thermopyle posted:</h4><blockquote>That\'s a good idea.  Might be nice to whip together a mockup for people to try out?<br /></blockquote></div><br />Im going to give it a whirl. I think I\'ll release what I have first and then get cracking on that.<img src="https://lh5.googleusercontent.com/-KxDfEtEzXAg/Tl1iU7VMDKI/AAAAAAAAAXo/M2f5Dhnxg7M/w120/user38305_pic9684_1259807148.gif" /><p class="editedby">'
    };

    for (var i = 0; i < 40; i++) {
        thread.append(ich.post(post_data));
    }
});
