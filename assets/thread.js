var prefs = JSON.parse(preferences);

$(document).ready(function() {
    $('.action-button').live('click', function(event) {
        if ($(this).hasClass("editable")) {
            listener.onEditablePostClick($(this).attr('id'), $(this).attr('lastreadurl'));
        } else {
            listener.onPostClick($(this).attr('id'), $(this).attr('lastreadurl'), $(this).attr('username'));
        }
    });

    var salr = new SALR(prefs);

    setYPosition(prefs.yPos);
});

$(window).load(function() {
    if (prefs.postjumpid == "" || prefs.yPos != "-1") {
        setYPosition(prefs.yPos);
    } else {
        $(window).scrollTop($("#".concat(prefs.postjumpid)).first().offset().top);
    }
});

function setYPosition(position) {
    if (position == "-1") {
        try {
            $(window).scrollTop($('.unread').first().offset().top);
        } catch (error) {}
    } else {
        $(window).scrollTop(position * 1);
    }
}
