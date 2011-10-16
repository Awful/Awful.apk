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
    setYPosition(preferences.yPos);
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
