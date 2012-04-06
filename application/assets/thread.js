var prefs = JSON.parse(preferences);

$(document).ready(function() {
    $('.quote_button').live('click', function(event) {
        listener.onQuoteClick($(this).attr('id'));
    });
    $('.edit_button').live('click', function(event) {
        listener.onEditClick($(this).attr('id'));
    });
    $('.more_button').live('click', function(event) {
        listener.onMoreClick($(this).attr('id'), $(this).attr('username'), $(this).attr('userid'));
    });
    $('.sendpm_button').live('click', function(event) {
        listener.onSendPMClick($(this).attr('username'));
    });
    $('.lastread_button').live('click', function(event) {
        listener.onLastReadClick($(this).attr('lastreadurl'));
    });
    $('.copyurl_button').live('click', function(event) {
        listener.onCopyUrlClick($(this).attr('id'));
    });
    $('.userposts_button').live('click', function(event) {
        listener.onUserPostsClick($(this).attr('id'));
    });
    if(prefs.showSpoilers){
    $('.bbc-spoiler').removeAttr('onmouseover');
    $('.bbc-spoiler').removeAttr('onmouseout');
    $('.bbc-spoiler').removeClass('bbc-spoiler');
    }else{
	$('.bbc-spoiler').mouseover( function(){ this.style.color=prefs.backgroundcolor;});
    $('.bbc-spoiler').mouseout ( function(){ this.style.color=this.style.backgroundColor=prefs.postcolor;});
    }

	$('.userinfo-row').click(function(event) {
	  $(this).closest('tr').next().find('.avatar-text-phone').toggle();
	  $(this).closest('tr').find('.button-row').toggle();
	});
	
    var salr = new SALR(prefs);
});

$(window).load(function() {
    scrollPost();
});
$(window).ready(function() {
    scrollPost();
});


function scrollPost() {
	if(prefs.scrollPosition > 0){
		$(window).scrollTop(prefs.scrollPosition);
	}else{
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
}

function scrollLastRead(){
	try{
        $(window).scrollTop($('.unread').first().offset().top);
    }catch(error){
    }
}

