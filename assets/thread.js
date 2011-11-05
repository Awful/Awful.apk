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

    scrollPost();
});

$(window).load(function() {
    scrollPost();
});

function scrollPost() {
    if (prefs.postjumpid != "") {
    	try{
    		$(window).scrollTop($("#".concat(prefs.postjumpid)).first().offset().top);
    	}catch(error){
    		scrollLastRead();
    	}
    } else {
	    scrollLastRead();
    }
}

function scrollLastRead(){
	try{
        $(window).scrollTop($('.unread').first().offset().top);
    }catch(error){
    	try{
    		$(window).scrollTop($('.read').last().offset().top);
		}catch(error){}
    }
}
