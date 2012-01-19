var prefs = JSON.parse(preferences);

$(document).ready(function() {
    $('.action-button').live('click', function(event) {
        if ($(this).hasClass("editable")) {
            listener.onEditablePostClick($(this).attr('id'), $(this).attr('lastreadurl'));
        } else {
            listener.onPostClick($(this).attr('id'), $(this).attr('lastreadurl'), $(this).attr('username'));
        }
    });
    if(prefs.showSpoilers){
    $('.bbc-spoiler').removeAttr('onmouseover');
    $('.bbc-spoiler').removeAttr('onmouseout');
    $('.bbc-spoiler').removeClass('bbc-spoiler');
    }else{
	$('.bbc-spoiler').mouseover( function(){ this.style.color=prefs.backgroundcolor;});
    $('.bbc-spoiler').mouseout ( function(){ this.style.color=this.style.backgroundColor=prefs.postcolor;});
    }

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
    }
}
