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
    $('.menu_button').live('click', function(event) {
        listener.onMenuClick($(this).attr('id'), $(this).attr('username'), $(this).attr('userid'), $(this).attr('lastreadurl'), $(this).attr('editable'));
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
	  $(this).closest('tr').next().find('.avatar-text').toggle();
	});
	$('.toggleread').click(function(event) {
		window.hideRead = false;
		refreshHidden();
	  	$('.toggleread').hide();
	  	window.setTimeout("scrollLastRead()", 500);
	});
	$('.avatar-cell').click(function(event) {
	  $(this).closest('tr').find('.avatar-text').toggle();
	});
	$('.tablet.username').click(function(event) {
	  $(this).closest('tr').find('.avatar-text').toggle();
	});
	$('.tablet.postdate').click(function(event) {
	  $(this).closest('tr').find('.avatar-text').toggle();
	});
    var salr = new SALR(prefs);
    
    $("img[title*=':'],img[title*=';']").load(function(index) {
    	$(this).height(($(this).height() *  prefs.postFontSize / 15));
	});
	$('.timg').click(function () {
		$(this).removeClass('timg');
		if(!$(this).parent().is('a')){
			$(this).wrap('<a href="'+$(this).attr('src')+'" />');
		}
	});
	
		  $('.post-content').find('div.bbcode_video object param[value^="http://vimeo.com"]').each(function(){
		    var videoID = $(this).attr('value').match(/clip_id=(\d+)/)
		    if (videoID === null) return
		    videoID = videoID[1]
		    var object = $(this).closest('object')
		    $(this).closest('div.bbcode_video').replaceWith($('<iframe/>', {
		      src: "http://player.vimeo.com/video/" + videoID + "?byline=0&portrait=0",
		      width: object.attr('width'),
		      height: object.attr('height'),
		      frameborder: 0,
		      webkitAllowFullScreen: '',
		      allowFullScreen: ''
		    }))
		  })		
});



$(window).load(function() {
	//listener.debugMessage('load');
	//window.stop();
});



$(window).ready(function() {
	//listener.debugMessage('ready');
    window.setTimeout("scrollPost()", 1000);
    $('.quote_link').each(function(){
		var id = this.hash.replace(/#post/,'.').concat(':visible');
		if($(id).length > 0){
			$(this).click(function(e){
				$(window).scrollTop($(id).offset().top);
				e.preventDefault();
			});
		}
	});

});

function registerPreBlocks(){
	$('pre').each(function(){
		var pos = $(this).offset().top;
		listener.addCodeBounds(pos, pos+$(this).height());
	});
}

function scrollPost() {
	//listener.debugMessage('scrollPost');
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
	//listener.debugMessage('scrollLastRead');
	try{
        $(window).scrollTop($('.unread:visible').first().offset().top);
    }catch(error){
    }
}

function showInlineImage(url){
	//listener.debugMessage('showInlineImage');
	//If it's not a GIF, or if it is a GIF and the user has not disabled GIF animation, then load the image, else switch from gif.png to .gif
	if(url.indexOf(".gif") == -1 || (url.indexOf(".gif") != -1 && !prefs.disableGifs)){
	$('a[href="'+url+'"]').append(function(){
		if($(this).children('img[src="'+url+'"]').length < 1){
			return '<img src="'+url+'" />';
		}
		return "";
		});
	}else{
		var alt = $('img[alt="'+url+'"]').attr('alt');
		$('img[alt="'+url+'"]').attr('src', alt);
	}
}

function gifHide() {
	//listener.debugMessage('gifHide');
	var minBound = $(window).scrollTop()-($(window).height()/2);
	var maxBound = $(window).scrollTop()+$(window).height()*1.5;
	$(".gif").each(function (){
		if($(this).offset().top > maxBound || ($(this).offset().top + $(this).height()) < minBound){
			$(this).css("visibility", "hidden");
		}else{
			$(this).css("visibility", "visible");
		}
	});
}

function showTabletUI(){
	window.isTablet = true;
	refreshHidden();
}

function showPhoneUI(){
	window.isTablet = false;
	refreshHidden();
}

function refreshHidden(){
	if(window.hideRead){
		$('.toggleread').show();
		$('.read').hide();
		if(window.isTablet){
			$('.unread.tablet').show();
			$('.phone').hide();
		}else{
			$('.tablet').hide();
			$('.unread.phone').show();
		}
  	}else{
  		$('.read').show();
	  	$('.toggleread').hide();
		if(window.isTablet){
			$('.tablet').show();
			$('.phone').hide();
		}else{
			$('.tablet').hide();
			$('.phone').show();
		}
	}
}

