/**
 * Initializes the container holding all the posts.
 */
function containerInit() {
	var container = $('#container');
	if (listener.getPreference('showSpoilers') != 'true') {
		container.on('click', '.bbc-spoiler', function spoilerClick() {
			$(this).toggleClass('spoiled');
		});
	}
	container.on('click', '.toggleread', showReadPosts);
	container.on('click', '.postinfo',	toggleInfo);
	container.on('click', '.postmenu', showPostMenu);
	container.on('click', 'a[href^="showthread.php?action=showpost"]', loadIgnoredPost);
	container.on('click', '.timg', enlargeTimg);
	container.on('longTap', '.postcontent img[title],.postcontent canvas[title]', function() {
		// title popup on long-press
		listener.popupText($(this).attr('title'));
	});
	container.on('touchend touchleave touchcancel', '.bbc-block.pre, .bbc-block.code, .bbc-block.php', listener.resumeSwipe);
	container.on('touchstart', '.bbc-block.pre, .bbc-block.code, .bbc-block.php', listener.haltSwipe);
	container.on('click', '.quote_link', handleQuoteLink);
	if (listener.getPreference('inlineWebm') == 'true' && listener.getPreference('autostartWebm') == 'true') {
		$(window).scrollEnd(pauseVideosOutOfView, 2000);
	}
}

/**
 * Loads the thread html into the container
 * @param checkFirst If true checks whether the webview has actually already been initialized
 */
function loadPageHtml(checkFirst) {
	if (checkFirst !== undefined && document.getElementById('container').innerHTML != '') {
		return;
	}
	if (window.topScrollID) {
		window.clearTimeout(window.topScrollID);
	}
	window.topScrollItem = null;
	window.topScrollPos = 0;
	window.topScrollCount = 0;
	var html = listener.getBodyHtml();
	document.getElementById('container').innerHTML = html;
	pageInit();
	window.topScrollID = window.setTimeout(scrollPost, 1000);
	$(window).on('load', function() {
		changeCSS(listener.getCSS());
	});
}

/**
 * Initializes the newly added posts that have just been added to the container
 */
function pageInit() {
	var spoilers = $('.bbc-spoiler');
	spoilers.removeAttr('onmouseover');
	spoilers.removeAttr('onmouseout');
	if (listener.getPreference('showSpoilers') == 'true') {
		spoilers.removeClass('bbc-spoiler');
	}
	// hide-old posts
	if ($('.toggleread').length > 0) {
		$('.read').hide();
	}
	if (listener.getPreference('hideSignatures') == 'true') {
		$('section.postcontent .signature').hide();
	}
	processThreadEmbeds();
	var postContent = $('.postcontent');
	postContent.find('div.bbcode_video object param[value^="http://vimeo.com"]').each(function() {
		var param = $(this);
		var videoID = param.attr('value').match(/clip_id=(\d+)/);
		if (videoID === null) {
			return;
		}
		videoID = videoID[1];
		var object = param.closest('object');
		param.closest('div.bbcode_video').replaceWith('<div class="videoWrapper"></div>').append($('<iframe/>', {
			src: 'http://player.vimeo.com/video/' + videoID + '?byline=0&portrait=0',
			width: object.attr('width'),
			height: object.attr('height'),
			frameborder: 0,
			webkitAllowFullScreen: '',
			allowFullScreen: ''
		}));
	});
	try {
		var salr = new SALR(listener);
	} catch (error) {
		console.log(error);
	}
	if (listener.getPreference('disableGifs') == 'true') {
		$('img[title][src$=".gif"]').on('load', function() {
			freezeGif($(this).get(0));
		});
		postContent.on('tap', 'img[title][src$=".gif"]', function() {
			freezeGif($(this).get(0));
		});
		postContent.on('tap', 'canvas[title][src$=".gif"]', function() {
			var canvas = $(this);
			canvas.replaceWith('<img src="' + canvas.attr('src') + '" title="' + canvas.attr('title') + '" />');
		});
	}
}

/**
 * Eventhandler that pauses all videos that have been scrolled out of the viewport and starts all videos currently in the viewport
 */
function pauseVideosOutOfView() {
	$('video').each(function() {
		var video = $(this);
		if (video.is(':inviewport') && !video.parent().is('blockquote') && video.children('source').first().attr('src').indexOf('webm') == -1) {
			video[0].play();
		} else {
			video[0].pause();
		}
	});
}

/**
 * Scrolls the webview to a certain post or the first unread post
 */
function scrollPost() {
	var postjump = listener.getPostJump();
	if (postjump != '') {
		try {
			window.topScrollItem = $('#post' + postjump).first();
			window.topScrollPos = window.topScrollItem.offset().top;
			window.scrollTo(0, window.topScrollPos);
			window.topScrollCount = 200;
			window.topScrollID = window.setTimeout(scrollUpdate, 500);
		} catch (error) {
			scrollLastRead();
		}
	} else {
		scrollLastRead();
	}
}

/**
 * Scrolls the webview to the first unread post
 */
function scrollLastRead() {
	try {
		window.topScrollItem = $('.unread').first();
		window.topScrollPos = window.topScrollItem.offset().top;
		window.topScrollCount = 100;
		window.scrollTo(0, window.topScrollPos);
		window.topScrollID = window.setTimeout(scrollUpdate, 500);
	} catch (error) {
		window.topScrollCount = 0;
		window.topScrollItem = null;
	}
}

/**
 * Updates the scroll position
 */
function scrollUpdate() {
	try {
		if (window.topScrollCount > 0 && window.topScrollItem) {
			var newPosition = window.topScrollItem.offset().top;
			if (newPosition - window.topScrollPos > 0) {
				window.scrollBy(0, newPosition - window.topScrollPos);
			}
			window.topScrollPos = newPosition;
			window.topScrollCount--;
			window.topScrollID = window.setTimeout(scrollUpdate, 200);
		}
	} catch (error) {
		window.topScrollCount = 0;
		window.topScrollItem = null;
	}
}

/**
 * Makes already read posts visible
 */
function showReadPosts() {
	$('.read').show();
	$('.toggleread').hide();
	window.setTimeout(scrollLastRead, 200);
}

/**
 * Load an image url and replace links with the image. Handles paused gifs and basic text links.
 * @param url The image URL
 */
function showInlineImage(url) {
	var LOADING = 'loading';
	var FROZEN_GIF = 'playGif';
	var isAlreadyLoading = function() {
		return $(this).hasClass(LOADING);
	};
	var isAlreadyInlined = function() {
		return $('img[src="' + url + '"]', this).size() > 0;
	};
	// basically treating anything not marked as a frozen gif as a text link
	var isTextLink = function() {
		return !($(this).hasClass(FROZEN_GIF));
	};
	var addEmptyImg = function() {
		$(this).append(Zepto('<img src="" />'));
	};
	var setLoading = function() {
		$(this).addClass(LOADING);
	};
	var setInlined = function() {
		$(this).removeClass(LOADING + ' ' + FROZEN_GIF);
	};
	var inlineImage = function() {
		$('img', this).first().attr('src', url).css({
			width: 'auto',
			height: 'auto'
		});
	};
	// skip anything that's already loading/loaded
	var imageLinks = $('a[href="' + url + '"]').not(isAlreadyLoading).not(isAlreadyInlined);
	imageLinks.filter(isTextLink).each(addEmptyImg);
	imageLinks.each(setLoading);
	loadImage(url, function() {
		// when the image is loaded, inline it everywhere and update the links
		imageLinks.each(inlineImage).each(setInlined);
	});
}

/**
 * Changes the font-face of the webview
 * @param font The name of the font
 */
function changeFontFace(font) {
	var fontFace = $('#font-face');
	if (font == 'default') {
		fontFace.remove();
	}
	if (fontFace.length) {
		fontFace.remove();
		$('head').append('<style id=\'font-face\' type=\'text/css\'>@font-face { font-family: userselected; src: url(\'content://com.ferg.awfulapp.webprovider/' + font + '\'); }</style>');
	} else {
		$('head').append('<style id=\'font-face\' type=\'text/css\'>@font-face { font-family: userselected; src: url(\'content://com.ferg.awfulapp.webprovider/' + font + '\'); }</style>');
	}
}

/**
 * Paints a gif on a canvas and replaces the original image with the canvas.
 * @param {Element} image Gif image that will be turned into a still canvas
 */
function freezeGif(image) {
	var canvas = document.createElement('canvas');
	var imageWidth = canvas.width = image.naturalWidth;
	var imageHeight = canvas.height = image.naturalHeight;
	canvas.getContext('2d').drawImage(image, 0, 0, imageWidth, imageHeight);
	try {
		image.src = canvas.toDataURL('image/gif'); // if possible, retain all css aspects
	} catch (e) { // cross-domain -- mimic original with all its tag attributes
		for (var i = 0, attribute = image.attributes[i]; i < image.attributes.length; i++)
			canvas.setAttribute(attribute.name, attribute.value);
		image.parentNode.replaceChild(canvas, image);
	}
}

/**
 * Replaces the previously ignored post with the loaded version
 * @param id The postId of the ignored post
 */
function insertIgnoredPost(id) {
	$('#ignorePost-' + id).replaceWith(listener.getIgnorePostHtml(id));
}

/**
 * Updates the background color of all posters that were previously, or are now, marked by the user
 * @param users A string of users seperated by commas 
 */
function updateMarkedUsers(users) {
	$('article.marked').removeClass('marked');
	var userArray = users.split(',');
	$.each(userArray, function(index, username) {
		$('.postinfo-poster:contains(' + username + ')').closest('article').addClass('marked');
	});
}

/**
 * Loads an image. Over the internet.
 * @param url The url of the image that is loaded
 * @param {Function} callback A function that should be called when the image is loaded
 */
function loadImage(url, callback) {
	$('<img src="' + url + '">').on('load', callback);
}

/**
 * Handles a quote link click event depending on the URL of the link. Moves the webview if the post is on the same page
 * @param {Event} event The click-event triggered by the user
 */
function handleQuoteLink(event) {
	var id = this.hash;
	try {
		if ($(id).size() > 0) {
			event.preventDefault();
			if ($(id).css('display') == 'none') {
				var count = $('.read').size();
				$('.toggleread').hide();
				$('.read').show(0, function() {
					if (--count == 0) {
						window.scrollTo(0, $(id).offset().top);
					}
				});
			} else {
				window.scrollTo(0, $(id).offset().top);
			}
		}
	} catch (error) {
		console.log(error);
	}
}

/**
 * Expands or retracts the postinfo 
 */
function toggleInfo() {
	var info = $(this);
	if (info.children('.postinfo-title').hasClass('extended')) {
		info.children('.avatar-cell').removeClass('extended');
		info.children('.avatar-cell').children('.avatar').removeClass('extended');
		info.children('.postinfo-title').removeClass('extended');
		info.children('.postinfo-regdate').removeClass('extended');
		if (listener.getPreference('disableGifs') == 'true' && info.find('.avatar').children('img').first().is('[src$=".gif"]')) {
			freezeGif(info.find('.avatar').children('img').first().get(0));
		}
	} else {
		info.children('.avatar-cell').addClass('extended');
		info.children('.avatar-cell').children('.avatar').addClass('extended');
		info.children('.postinfo-title').addClass('extended');
		info.children('.postinfo-regdate').addClass('extended');
		if (info.find('canvas').get(0) !== undefined) {
			info.find('canvas').first().replaceWith('<img src="' + info.find('canvas').first().attr('src') + '" />');
		}
	}
}

/**
 * Triggers the display of the postmenu
 */
function showPostMenu() {
	var postMenu = $(this);
	listener.onMoreClick(
		postMenu.closest('article').attr('id').replace(/post/, ''),
		postMenu.attr('username'),
		postMenu.attr('userid'),
		postMenu.attr('lastreadurl'),
		postMenu[0].hasAttribute('editable'),
		(postMenu[0].hasAttribute('isMod') || postMenu[0].hasAttribute('isAdmin')),
		postMenu[0].hasAttribute('isPlat')
	);
}

/**
 * Changes the styling of the webview
 * @param  file Name of the CSS to be used
 */
function changeCSS(file) {
	$('head').children('link').first().attr('href', file);
}

/**
 * Loads an ignored post
 * @param {Event} event User-triggered click event
 */
function loadIgnoredPost(event) {
	event.preventDefault();
	var url = $(this).attr('href');
	var id = url.substring(url.indexOf('#') + 1);
	listener.loadIgnoredPost(id);
	$(this).replaceWith('<span id="ignorePost-' + id + '">Loading Post, please wait...</span>');
}

/**
 * Removes the timg class from a timg to turn it into a normal image
 */
function enlargeTimg() {
	var tImg = $(this);
	tImg.removeClass('timg');
	if (!tImg.parent().is('a')) {
		tImg.wrap('<a href="' + tImg.attr('src') + '" />');
	}
}