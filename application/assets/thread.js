var prefs = JSON.parse(preferences);

function toggleinfo(info){
	if($(info).children('.postinfo-title').hasClass('extended')){
		$(info).children('.avatar-cell').removeClass('extended');
		$(info).children('.avatar-cell').children('.avatar').removeClass('extended');
		$(info).children('.postinfo-title').removeClass('extended');
		$(info).children('.postinfo-regdate').removeClass('extended');
	}else{
		$(info).children('.avatar-cell').addClass('extended');
		$(info).children('.avatar-cell').children('.avatar').addClass('extended');
		$(info).children('.postinfo-title').addClass('extended');
		$(info).children('.postinfo-regdate').addClass('extended');
	}
}
function toggleoptions(menu){
	$(menu).parent().parent().children('.postoptions').toggleClass('extended');
}

function changeCSS(file){
	$('head').children('link').first().attr('href',file);
}


function pageinit() {
    $('.quote').on('click', function(event) {
    	listener.onQuoteClick($(this).parent().parent().attr('id').replace(/post/,''));
    });
    $('.edit').on('click', function(event) {
        listener.onEditClick($(this).parent().parent().attr('id').replace(/post/,''));
    });
    $('.more').on('click', function(event) {
        listener.onMoreClick($(this).parent().parent().attr('id').replace(/post/,''), $(this).attr('username'), $(this).attr('userid'));
    });
    $('.sendpm_button').on('click', function(event) {
        listener.onSendPMClick($(this).attr('username'));
    });
    $('.lastread').on('click', function(event) {
        listener.onLastReadClick($(this).attr('lastreadurl'));
    });
    $('.copyurl_button').on('click', function(event) {
        listener.onCopyUrlClick($(this).attr('id').replace(/#post/,''));
    });
    $('.userposts_button').on('click', function(event) {
        listener.onUserPostsClick($(this).attr('id').replace(/#post/,''));
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
	$('.avatar-cell').on('click', function(event) {
	  $(this).closest('tr').find('.avatar-text').toggle();
	});
	
	$('.postinfo').on('click',function(){
		toggleinfo($(this));
	});
	$('.postmenu').on('click',function(){
		toggleoptions($(this));
	});

    $('.quote_link').each(function(){
		var id = this.hash;
    	try{
		if($(id).size() > 0 && $(id).css("visibility") !== "none"){
			$(this).click(function(e){
				window.scrollTo(0,$(id).offset().top);
				e.preventDefault();
			});
		}
    	}catch(error){
    	}
	});
	
	$('.postcontent').find('div.bbcode_video object param[value^="http://vimeo.com"]').each(function(){
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
	  
    var salr = new SALR(prefs);
	
	$('.timg').on('click',function () {
		$(this).removeClass('timg');
		if(!$(this).parent().is('a')){
			$(this).wrap('<a href="'+$(this).attr('src')+'" />');
		}
	});
	
	$(window).bind('reorient', function() {
		$('iframe').each(function() {
	    	$(this).height($(this).width()/16*9);
		});
	});
	$.reorient.start();
	$('iframe').each(function(){$(this).height($(this).width()/16*9)});
	
};



//$(window).on('load', function() {
//	//listener.debugMessage('load');
//	//window.stop();
//});

function loadpagehtml(){
    if(window.topScrollID){
        window.clearTimeout(window.topScrollID);
    }
    window.topScrollItem = null;
    window.topScrollPos = 0;
    window.topScrollCount = 0;
    changeCSS(listener.getCSS());
    var html=listener.getBodyHtml();
    document.getElementById("container").innerHTML=html;
    pageinit();
    window.topScrollID = window.setTimeout(scrollPost, 1000);
}

function registerPreBlocks(){
	$('pre').each(function(){
		var pos = $(this).offset().top;
		listener.addCodeBounds(pos, pos+$(this).height());
	});
}

function scrollPost() {
    var postjump = listener.getPostJump();
    if (postjump != "") {
        try{
            window.topScrollItem = $("#post"+postjump).first();
            window.topScrollPos = window.topScrollItem.offset().top;
            window.scrollTo(0,window.topScrollPos);
	        window.topScrollCount = 200;
	  	    window.topScrollID = window.setTimeout(scrollUpdate, 500);
        }catch(error){
            scrollLastRead();
        }
    } else {
        scrollLastRead();
    }
}

function scrollLastRead(){
	try{
	    window.topScrollItem = $('.unread').first();
	    window.topScrollPos = window.topScrollItem.offset().top;
	    window.topScrollCount = 200;
		window.scrollTo(0, window.topScrollPos);
	  	window.topScrollID = window.setTimeout(scrollUpdate, 500);
    }catch(error){
	    window.topScrollCount = 0;
	    window.topScrollItem = null;
    }
}

function scrollUpdate(){
	try{
	    if(window.topScrollCount > 0 && window.topScrollItem){
            var newpos = window.topScrollItem.offset().top;
            window.topScrollCount = 0;
            window.scrollBy(0, newpos-window.topScrollPos);
            window.topScrollPos = newpos;
            window.topScrollCount--;
            window.topScrollID = window.setTimeout(scrollUpdate, 100);
	    }
    }catch(error){
        window.topScrollCount = 0;
	    window.topScrollItem = null;
    }
}

function showInlineImage(url){
	//listener.debugMessage('showInlineImage');
	image = ($('a[href="'+url+'"]').children('img[src="'+url+'"]').size() < 1)?'<img src="'+url+'" />':'';
	$('a[href="'+url+'"]').append(image);
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

function refreshHidden(){
	if(window.hideRead){
		$('.toggleread').show();
		$('.read').hide();
  	}else{
  		$('.read').show();
	  	$('.toggleread').hide();
	}
}

