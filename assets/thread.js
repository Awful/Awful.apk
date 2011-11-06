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

    setYPosition();
});

$(window).load(function() {
    setYPosition();
});

function setYPosition() {
	if(prefs.yPos == "-1"){
	    if (prefs.postjumpid != "") {
	    	try{
	    		$(window).scrollTop($("#".concat(prefs.postjumpid)).first().offset().top);
	    	}catch(error){}
	    } else {
		    try{
		        $(window).scrollTop($('.unread').first().offset().top);
		    }catch(error){}
	    }
    }
}
