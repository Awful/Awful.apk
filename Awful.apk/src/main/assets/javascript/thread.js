/**
 * Initializes the container holding all the posts.
 */
function containerInit() {
	var container = document.getElementById('container');
	
	container.addEventListener('click', function containerClick(event) {
		var that = event.target;
		if (findInPath(event, 'bbc-spoiler') && listener.getPreference('showSpoilers') != 'true') {
			that.classList.toggle('spoiled');
		}
		if (findInPath(event, 'toggleread')) {
			showReadPosts();
		}
		if (findInPath(event, 'postinfo')) {
			toggleInfo(findInPath(event, 'postinfo', true));
		}
		if (findInPath(event, 'postmenu')) {
			showPostMenu(findInPath(event, 'postmenu', true));
		}
		if (findInPath(event, 'timg')) {
			enlargeTimg(findInPath(event, 'timg', true));
		}
		if (findInPath(event, 'quote_link')) {
			handleQuoteLink(event);
		}
		if (that.tagName === 'a' && that.getAttribute('href').startsWith('showthread.php?action=showpost')){
			loadIgnoredPost(event);
		}
	});
	container.addEventListener('longTap', function(event) {
		if((event.target.tagName === 'img' || event.target.tagName === 'img') && event.target.hasAttribute('title')) {
			// title popup on long-press
			listener.popupText(event.target.getAttribute('title'));
		}
	});
	container.addEventListener('touchend touchleave touchcancel', function(event) {
		if(event.target.classList.contains('bbc-block') && (event.target.classList.contains('pre') || event.target.classList.contains('code') || event.target.classList.contains('php'))) {
			listener.resumeSwipe();
		}
	});
	container.addEventListener('touchstart', function(event) {
		if((event.target.tagName === 'img' || event.target.tagName === 'img') && event.target.hasAttribute('title')) {
			listener.haltSwipe();
		}
	});
	container.addEventListener('tap', function(event){
		var that = event.target;
		if(that.tagName === 'img' && that.hasAttribute('title') && that.getAttribute('src').endsWith('.gif')){
			freezeGif(that);
		}
		if(that.tagName === 'canvas' && that.hasAttribute('title') && that.getAttribute('src').endsWith('.gif')){
			that.replaceWith('<img src="' + that.getAttribute('src') + '" title="' + that.getAttribute('title') + '" />');
		}
	});
	if (listener.getPreference('inlineWebm') == 'true' && listener.getPreference('autostartWebm') == 'true') {
		window.addEventListener('scroll', function containerScroll(){
			//TODO: debounce;
			debounce(pauseVideosOutOfView, 2000);
		});
	}
}

function findInPath(event, cssClass, returnElement) {
	var search = event.path.filter(function(node){ return node.classList && node.classList.contains(cssClass);});
	return returnElement ? search[0] : search.length > 0;
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
	document.addEventListener('DOMContentLoaded', function updateCssForPage() {
		changeCSS(listener.getCSS());
	});
}

/**
 * Initializes the newly added posts that have just been added to the container
 */
function pageInit() {
	var spoilers = document.querySelectorAll('.bbc-spoiler');
	spoilers.forEach(function(spoiler){
		spoiler.removeAttribute('onmouseover');
		spoiler.removeAttribute('onmouseout');
		if (listener.getPreference('showSpoilers') == 'true') {
			spoiler.classList.remove('bbc-spoiler');
		}
	});
	// hide-old posts
	if (document.querySelector('.toggleread') !== null) {
		document.querySelectorAll('.read').forEach(function each(post){
			post.style.display = 'none';
		});
	}
	if (listener.getPreference('hideSignatures') == 'true') {
		document.querySelectorAll('section.postcontent .signature').forEach(function each(signature){
			signature.remove();
		});
	}
	processThreadEmbeds();
	pauseVideosOutOfView();

	if (listener.getPreference('hideSignatures') == 'true') {
		highlightOwnUsername();
	}
	
	if (listener.getPreference('hideSignatures') == 'true') {
		highlightOwnQuotes();
	}

	if (listener.getPreference('disableGifs') == 'true') {
		document.querySelectorAll('img[title][src$=".gif"]').forEach(function each(gif){
			gif.addEventListener('load', function() {
				freezeGif(this);
			});
		});
	}
}

/**
 * Eventhandler that pauses all videos that have been scrolled out of the viewport and starts all videos currently in the viewport
 */
function pauseVideosOutOfView() {
	document.querySelectorAll('video').forEach(function(video) {
		if (isElementInViewport(video) && !video.parentElement.tagName === 'blockquote' && video.firstChild.getAttribute('src').indexOf('webm') == -1) {
			video.play();
		} else {
			video.pause();
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
			window.topScrollItem = document.getElementById('#post' + postjump);
			window.topScrollPos = window.topScrollItem.getBoundingClientRect().top;
			//window.scrollTo(0, window.topScrollPos);
			window.topScrollCount = 200;
			window.topScrollID = window.setTimeout(scrollUpdate, 500);
		} catch (error) {
			//scrollLastRead();
		}
	} else {
		//scrollLastRead();
	}
}

/**
 * Scrolls the webview to the first unread post
 */
function scrollLastRead() {
	try {
		window.topScrollItem = document.querySelector('.unread');
		window.topScrollPos = window.topScrollItem.getBoundingClientRect().top;
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
			var newPosition = window.topScrollItem.getBoundingClientRect().top;
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
	document.querySelectorAll('.read').forEach(function showAllReadPosts(post) {
		post.style.display = '';
	});
	document.querySelector('.toggleread').remove();
	//window.setTimeout(scrollLastRead, 200);
}

/**
 * Load an image url and replace links with the image. Handles paused gifs and basic text links.
 * @param url The image URL
 */
function showInlineImage(url) {
	var LOADING = 'loading';
	var FROZEN_GIF = 'playGif';
	// basically treating anything not marked as a frozen gif as a text link

	var addEmptyImg = function(link) {
		if(!link.classList.contains(FROZEN_GIF)){
			var image = document.createElement('img');
			image.src = '';
			link.append(image);
		} else {
			link.classList.add(LOADING);
		}
	};
	var inlineImage = function(link) {
		var image = link.querySelector('img');
		image.src = url;
		image.style.height = 'auto';
		image.style.width = 'auto';
		link.classList.remove(LOADING);
		link.classList.remove(FROZEN_GIF);
	};
	// skip anything that's already loading/loaded
	var imageLinks = document.querySelectorAll('a[href="' + url + '"]:not(.loading)');
	imageLinks.forEach(addEmptyImg);
	
	var pseudoImage = document.createElement('img');
	pseudoImage.src = url;
	pseudoImage.addEventListener('load', function(){
		// when the image is loaded, inline it everywhere and update the links
		imageLinks.forEach(inlineImage);
		pseudoImage.remove();
	});
}

/**
 * Changes the font-face of the webview
 * @param font The name of the font
 */
function changeFontFace(font) {
	var fontFace = document.getElementById('#font-face');
	if (font == 'default') {
		fontFace.remove();
	}
	if (fontFace.length) {
		fontFace.remove();
		document.getElementsByTagName('head')[0].append('<style id=\'font-face\' type=\'text/css\'>@font-face { font-family: userselected; src: url(\'content://com.ferg.awfulapp.webprovider/' + font + '\'); }</style>');
	} else {
		document.getElementsByTagName('head')[0].append('<style id=\'font-face\' type=\'text/css\'>@font-face { font-family: userselected; src: url(\'content://com.ferg.awfulapp.webprovider/' + font + '\'); }</style>');
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
	document.getElementById('#ignorePost-' + id).replaceWith(listener.getIgnorePostHtml(id));
}

/**
 * Updates the background color of all posters that were previously, or are now, marked by the user
 * @param users A string of users seperated by commas 
 */
function updateMarkedUsers(users) {
	document.querySelectorAll('article.marked').forEach(function each(){
		this.classList.remove('marked');
	});
	var userArray = users.split(',');
	userArray.forEach(function each(username) {
		document.querySelectorAll('.postmenu[username=' + username + ']').closest('article').classList.add('marked');
	});
}

/**
 * Handles a quote link click event depending on the URL of the link. Moves the webview if the post is on the same page
 * @param {Event} event The click-event triggered by the user
 */
function handleQuoteLink(event, that) {
	var id = that.hash;
	try {
		var postOfID = document.getElementById(id);
		if (postOfID) {
			event.preventDefault();
			if (postOfID.style.display === 'none') {
				var readPosts = document.querySelectorAll('.read');
				document.querySelector('.toggleread').remove();
				readPosts.forEach(function(){
					readPosts.style.display = '';
				});
				if (--readPosts.length == 0) {
					window.scrollTo(0, postOfID.getBoundingClientRect().top);
				}	
			} else {
				window.scrollTo(0, postOfID.getBoundingClientRect().top);
			}
		}
	} catch (error) {
		console.log(error);
	}
}

/**
 * Expands or retracts the postinfo 
 */
function toggleInfo(info) {
	if (info.querySelector('.postinfo-title').classList.contains('extended')) {
		info.querySelector('.avatar-cell').classList.remove('extended');
		info.querySelector('.avatar-cell .avatar').classList.remove('extended');
		info.querySelector('.postinfo-title').classList.remove('extended');
		info.querySelector('.postinfo-regdate').classList.remove('extended');
		if (listener.getPreference('disableGifs') == 'true' && info.querySelector('.avatar img').src.endsWith('.gif')) {
			freezeGif(info.querySelector('.avatar img'));
		}
	} else {
		info.querySelector('.avatar-cell').classList.add('extended');
		info.querySelector('.avatar-cell .avatar').classList.add('extended');
		info.querySelector('.postinfo-title').classList.add('extended');
		info.querySelector('.postinfo-regdate').classList.add('extended');
		if (info.querySelector('canvas') !== null) {
			var avatar = document.createElement('img');
			avatar.src = info.querySelector('canvas').getAttribute('src');
			info.querySelector('canvas').replaceWith(avatar);
		}
	}
}

/**
 * Triggers the display of the postmenu
 */
function showPostMenu(postMenu) {
	listener.onMoreClick(
		postMenu.closest('article').getAttribute('id').replace(/post/, ''),
		postMenu.getAttribute('username'),
		postMenu.getAttribute('userid'),
		postMenu.getAttribute('lastreadurl'),
		postMenu.hasAttribute('editable'),
		(postMenu.hasAttribute('isMod') || postMenu.hasAttribute('isAdmin')),
		postMenu.hasAttribute('isPlat')
	);
}

/**
 * Changes the styling of the webview
 * @param  file Name of the CSS to be used
 */
function changeCSS(file) {
	document.getElementsByTagName('head')[0].querySelector('link').setAttribute('href', file);
}

/**
 * Loads an ignored post
 * @param {Event} event User-triggered click event
 */
function loadIgnoredPost(event) {
	event.preventDefault();
	var url = this.setAttribute('href');
	var id = url.substring(url.indexOf('#') + 1);
	listener.loadIgnoredPost(id);
	this.replaceWith('<span id="ignorePost-' + id + '">Loading Post, please wait...</span>');
}

/**
 * Removes the timg class from a timg to turn it into a normal image
 */
function enlargeTimg(tImg) {
	tImg.classList.remove('timg');
	if (!tImg.parentElement.tagName === 'a') {
		var link = document.createElement('a');
		tImg.parentNode.insertBefore(link, tImg);
		tImg.parentNode.removeChild(tImg);
		link.appendChild(tImg);
	}
}

function isElementInViewport (el) {

	var rect = el.getBoundingClientRect();

	return (
		rect.top >= 0 &&
		rect.left >= 0 &&
		rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) && 
		rect.right <= (window.innerWidth || document.documentElement.clientWidth) 
	);
}

/**
 * Highlight the user's username in posts
 */
function highlightOwnUsername() {
	function getTextNodesIn(node) {
		var textNodes = [];

		function getTextNodes(node) {
			if (node.nodeType == 3) {
				textNodes.push(node);
			} else {
				for (var i = 0, len = node.childNodes.length; i < len; ++i) {
					getTextNodes(node.childNodes[i]);
				}
			}
		}

		getTextNodes(node);
		return textNodes;
	}

	var selector = '.postcontent:contains("' + listener.getPreference('username') + '")';
	
	var regExp = new RegExp('\\b'+listener.getPreference('username')+'\\b', 'g');
	var styled = '<span class="usernameHighlight">' + listener.getPreference('username') + '</span>';
	document.querySelectorAll(selector).forEach(function() {
		getTextNodesIn(this).forEach(function(node) {
			if (node.wholeText.match(regExp)) {
				var newNode = node.ownerDocument.createElement('span');
				newNode.innerHTML = node.wholeText.replace(regExp, styled);
				node.parentNode.replaceChild(newNode, node);
			}
		});
	});
}

/**
 * Highlight the quotes of the user themselves.
 */
function highlightOwnQuotes() {
	var usernameQuoteMatch = listener.getPreference('username') + ' posted:';
	var quotes = document.querySelectorAll('.bbc-block h4');
	Array.prototype.filter.call(quotes, function(quote){
		return quote.innerHTML.indexOf(usernameQuoteMatch) !== -1;
	});
	quotes.forEach(function(quote) {
		quote.parentElement.classList.add('self');
		// Replace the styling from username highlighting
		quote.querySelectorAll('.usernameHighlight').forEach(function(name) {
			name.classList.remove('usernameHighlight');
		});
	});
}

function debounce(func, wait, immediate) {
	var timeout;
	return function() {
		var context = this, args = arguments;
		var later = function() {
			timeout = null;
			if (!immediate) func.apply(context, args);
		};
		var callNow = immediate && !timeout;
		clearTimeout(timeout);
		timeout = setTimeout(later, wait);
		if (callNow) func.apply(context, args);
	};
};