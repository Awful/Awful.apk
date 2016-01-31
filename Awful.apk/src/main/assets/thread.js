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

function loadpagehtml(){
    if(window.topScrollID){
        window.clearTimeout(window.topScrollID);
    }
    window.topScrollItem = null;
    window.topScrollPos = 0;
    window.topScrollCount = 0;
    var html = listener.getBodyHtml();
    document.getElementById("container").innerHTML = html;
    pageinit();
    window.topScrollID = window.setTimeout(scrollPost, 1000);
    $(window).on('load', function(){
        changeCSS(listener.getCSS());
    });
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
    $('.lastread').on('click', function(event) {
        listener.onLastReadClick($(this).attr('lastreadurl'));
    });
    
    $('.bbc-spoiler').removeAttr('onmouseover');
    $('.bbc-spoiler').removeAttr('onmouseout');
    if(listener.getPreference("showSpoilers") == "true"){
    	$('.bbc-spoiler').removeClass('bbc-spoiler');
    }else{
    	$('.bbc-spoiler').click( function(){ $(this).toggleClass('spoiled');});
    }

    // hide-old posts
    if($('.toggleread').length > 0){
        $('.read').hide();
    }
    $('.toggleread').click(function(event) {
        $('.read').show();
          $('.toggleread').hide();
          window.setTimeout(scrollLastRead, 200);
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
	        if($(id).size() > 0){
	            $(this).click(function(e){
	                e.preventDefault();
                    if($(id).css("display") == "none"){
                        var count = $('.read').size();
                        $('.toggleread').hide();
                        $('.read').show(0, function(){
                            if(--count == 0){
                                window.scrollTo(0,$(id).offset().top);
                            }
                        });
                    }else{
                        window.scrollTo(0,$(id).offset().top);
                    }
	            });
	        }
        }catch(error){
        	console.log(error);
        }
    });

    $('a[href^="showthread.php?action=showpost"]').on('click',function(e){
            e.preventDefault();
            var url = $(this).attr('href'),
                id = url.substring(url.indexOf("#")+1);
                console.log(url);
                console.log(id);
            listener.loadIgnoredPost(id);
            $(this).replaceWith('<span id="ignorePost-'+id+'">Loading Post, please wait...</span>')
    });

     if(listener.getPreference("hideSignatures") == "true"){
        //$('section.postcontent .signature').hide();
        $('section.postcontent .signature').parent().children().each(function() {

           var sig = $(this).parent().find('.signature').index();

           if($(this).index() >= sig ) {

               $(this).hide();

           }
        });
     }

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
    try{
        var salr = new SALR(listener);
    }catch(error){
        console.log(error);
    }
    $('.timg').on('click',function () {
        $(this).removeClass('timg');
        if(!$(this).parent().is('a')){
            $(this).wrap('<a href="'+$(this).attr('src')+'" />');
        }
    });

    if(listener.getPreference("disableGifs") == "true"){
        $('[title][src$=".gif"]').on('load', function (){
            freezeGif($(this).get(0));
        });
    }

    $(window).bind('reorient', function() {
        $('iframe').each(function() {
            $(this).height($(this).width()/16*9);
        });
    });
    $.reorient.start();
    $('iframe').each(function(){$(this).height($(this).width()/16*9)});

    $('.bbc-block.pre, .bbc-block.code, .bbc-block.php').on('touchend touchleave touchcancel',function(){
        listener.resumeSwipe();
    }).on('touchstart',function(){
        listener.haltSwipe();
    })
    if(listener.getPreference("inlineWebm") == "true" && listener.getPreference("autostartWebm")){
        $(window).scrollEnd(function(){
            pauseVideosOutOfView();
        }, 1000);
    }
};

function pauseVideosOutOfView(){
    $('video').each(function(){
        if ($(this).is(":inviewport")) {
            $(this)[0].play();
        } else {
            $(this)[0].pause();
        }
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
        window.topScrollCount = 100;
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
            if(newpos-window.topScrollPos > 0){
                window.scrollBy(0, newpos-window.topScrollPos);
            }
            window.topScrollPos = newpos;
            window.topScrollCount--;
            window.topScrollID = window.setTimeout(scrollUpdate, 200);
        }
    }catch(error){
        window.topScrollCount = 0;
        window.topScrollItem = null;
    }
}

function showInlineImage(url){
    // listener.debugMessage('showInlineImage');
    imageLink = $('a[href="'+url+'"]');
    firstLink = imageLink.first();
    if(firstLink.hasClass('playGif')){
      imageLink.addClass('loading');
      loadImage(firstLink.attr('href'), function(){
      showInlineGif(firstLink.attr('href'),true);
        imageLink.removeClass('playGif').removeClass('loading');
      });
    }else if(firstLink.children('img[src="'+url+'"], img[src="file:///android_res/drawable/gif.png"]').size() < 1){
        imageLink.append('<img src="'+url+'" />');
    }else{
    imageLink.each(function(){
    image = $(this).children().first();
            if(image.attr('src') == "file:///android_res/drawable/gif.png"){
                  image.attr('src', url);
                  image.css({
                         width: 'auto',
                         height: 'auto'
                       });
            }
    })
    }
}

function gifHide() {
    // listener.debugMessage('gifHide');
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

function changeFontFace(font){
	if(font == 'default'){
		$('#font-face').remove();
	}
	if($('#font-face').length){
		$('#font-face').remove();
		$('head').append("<style id='font-face' type='text/css'>@font-face { font-family: userselected; src: url('content://com.ferg.awfulapp.webprovider/"+font+"'); }</style>");
	}else{
		$('head').append("<style id='font-face' type='text/css'>@font-face { font-family: userselected; src: url('content://com.ferg.awfulapp.webprovider/"+font+"'); }</style>");
	}
}

function freezeGif(i) {
    var c = document.createElement('canvas');
    var w = c.width = i.naturalWidth;
    var h = c.height = i.naturalHeight;
    c.getContext('2d').drawImage(i, 0, 0, w, h);
    try {
        i.src = c.toDataURL("image/gif"); // if possible, retain all css aspects
    } catch(e) { // cross-domain -- mimic original with all its tag attributes
        for (var j = 0, a; a = i.attributes[j]; j++)
            c.setAttribute(a.name, a.value);
        i.parentNode.replaceChild(c, i);
    }
}

function insertIgnoredPost(id){
    $('#ignorePost-'+id).replaceWith(listener.getIgnorePostHtml(id));
}

function updateMarkedUsers(users){
    $('article.marked').removeClass('marked');
    var userArray = users.split(',');
    $.each(userArray, function(idx, username){
        $('.postinfo-poster:contains('+username+')').parent().parent().parent().addClass('marked');
    });
}

function loadImage(url, callback) {
$('<img src="'+ url +'">').on('load', callback);
}
function showInlineGif(url, gifplay){
    imageLink = $('a[href="'+url+'"]');
    imageLink.each(function(){
    image = $(this).children().first();
                  image.attr('src', url);
                  image.css({
                         width: 'auto',
                         height: 'auto'
                       });
    })
}
