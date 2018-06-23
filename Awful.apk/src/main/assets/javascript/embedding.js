'use strict';

var listener;

/**
 * Functions to automatically embed page content, e.g. turn an Instagram URL into a widget
 * @author baka kaba
 * @param {Element} [post] Can be set to limit the scope of where the embeds are processed, defaults to the document
 */
function processThreadEmbeds(post) {

	var replacementArea = post || document;
	// map preference keys to their corresponding embed functions
	var embedFunctions = {
		inlineInstagram: embedInstagram,
		inlineTweets: embedTweets,
		inlineVines: embedVines,
		inlineWebm: embedVideos
	};

	// check all embed preference keys - any set to true, run their embed function
	for (var embedType in embedFunctions) {
		if (listener.getPreference(embedType) === 'true') {
			embedFunctions[embedType].call(post);
		}
	}
	// There's no vimeo setting
	replaceVimeo();

	/**
	 * Replaces all instagram links with instagram embeds
	 */
	function embedInstagram() {
		if (!document.getElementById('instagramScript')) {
			// add the embed script, and run it after all the HTML widgets have been added
			var instagramEmbedScript = document.createElement('script');
			instagramEmbedScript.setAttribute('src', 'https://platform.instagram.com/en_US/embeds.js');
			instagramEmbedScript.id = 'instagramScript';
			document.getElementsByTagName('body')[0].appendChild(instagramEmbedScript);
		}
		var instagrams = replacementArea.querySelectorAll('.postcontent a[href*="instagr.am/p"],.postcontent a[href*="instagram.com/p"]');
		if (instagrams.length > 0) {
			instagrams.forEach(function eachInstagramLink(instagramLink) {
				var url = instagramLink.getAttribute('href');
				var apiCall = 'https://api.instagram.com/oembed?omitscript=true&url=' + url + '&callback=?';

				JSONP.get(apiCall, {}, function getInstagrams(data) {
					instagramLink.outerHTML = data.html;
					window.requestAnimationFrame(window.instgrm.Embeds.process);
				});
			});
		}
	}

	/**
	 * Replaces all broken Vimeo Flash plugins with iframe versions that Actually Work™
	 */
	function replaceVimeo() {
		replacementArea.querySelectorAll('.postcontent .bbcode_video object param[value^="http://vimeo.com"]').forEach(function each(vimeoPlayer) {
			var param = vimeoPlayer;
			var videoID = param.getAttribute('value').match(/clip_id=(\d+)/);
			if (videoID === null) {
				return;
			}
			videoID = videoID[1];
			var object = param.closest('object');

			var vimeoIframe = document.createElement('iframe');
			vimeoIframe.setAttribute('src', 'http://player.vimeo.com/video/' + videoID + '?byline=0&portrait=0');
			vimeoIframe.setAttribute('width', object.getAttribute('width'));
			vimeoIframe.setAttribute('height', object.getAttribute('height'));
			vimeoIframe.setAttribute('frameborder', 0);
			vimeoIframe.setAttribute('webkitAllowFullScreen', '');
			vimeoIframe.setAttribute('allowFullScreen', '');

			var videoWrapper = document.createElement('div');
			videoWrapper.classList.add('videoWrapper');
			videoWrapper.appendChild(vimeoIframe);
			param.closest('div.bbcode_video').replaceWith(videoWrapper);
		});
	}

	/**
	 * Replaces all twitter status links (tweets) with twitter embeds
	 */
	function embedTweets() {
		var tweets = replacementArea.querySelectorAll('.postcontent a[href*="twitter.com"]');

		tweets = Array.prototype.filter.call(tweets, function isTweet(twitterURL) {
			return twitterURL.href.match(RegExp('https?://(?:[\\w\\.]*\\.)?twitter.com/[\\w_]+/status(?:es)?/([\\d]+)'));
		});
		tweets = Array.prototype.filter.call(tweets, filterNwsAndSpoiler);
		tweets.forEach(function eachTweet(tweet) {
			var tweetUrl = tweet.href;
			JSONP.get('https://publish.twitter.com/oembed?omit_script=true&url=' + escape(tweetUrl), {}, function getTworts(data) {
				var div = document.createElement('div');
				div.classList.add('tweet');
				tweet.parentNode.insertBefore(div, tweet);
				tweet.parentNode.removeChild(tweet);
				div.appendChild(tweet);
				div.innerHTML = data.html;
				if (document.getElementById('theme-css').dataset.darkTheme === 'true') {
					div.querySelector('blockquote').dataset.theme = 'dark';
				}
				if (window.twttr) {
					window.twttr.widgets.load(div);
				} else {
					missedEmbeds.push(div);
				}
			});
		});
	}

	/**
	 * Replaces all vine links with vines embeds.
	 */
	function embedVines() {
		var vines = replacementArea.querySelectorAll('.postcontent a[href*="://vine.co/v/"]');

		vines = Array.prototype.filter.call(vines, filterNwsAndSpoiler);
		vines.forEach(function eachVine(vine) {
			vine.innerHTML = '<iframe class="vine-embed" src="' + vine.href + '/embed/simple" frameborder="0"></iframe>' + '<script async src="http://platform.vine.co/static/scripts/embed.js" charset="utf-8"></script>';
		});
	}

	/**
	 * Replaces all links to .webm, .mp4 or .gifv video files with a video tag playing that video.
	 */
	function embedVideos() {
		var videos = replacementArea.querySelectorAll('.postcontent a[href$="webm"], .postcontent a[href$="gifv"], .postcontent a[href$="mp4"]');

		videos = Array.prototype.filter.call(videos, filterNwsAndSpoiler);
		videos.forEach(function eachVideo(video) {
			var hasThumbnail;
			video.setAttribute('href', video.href.replace('.gifv', '.mp4'));
			var videoURL = video.href;
			if (videoURL.indexOf('imgur.com') !== -1) {
				hasThumbnail = videoURL.substring(0, videoURL.lastIndexOf('.')) + 'm.jpg';
				video.setAttribute('href', videoURL.replace('.webm', '.mp4'));
			} else if (videoURL.indexOf('gfycat.com') !== -1) {
				hasThumbnail = 'https://thumbs' + videoURL.substring(videoURL.indexOf('.'), videoURL.lastIndexOf('.')) + '-mobile.jpg';
				video.setAttribute('href', 'https://thumbs' + videoURL.substring(videoURL.indexOf('.'), videoURL.lastIndexOf('.')) + '-mobile.mp4');
			}
			video.outerHTML = '<video loop width="100%" muted="true" controls preload="none" ' + (hasThumbnail !== undefined ? 'poster="' + hasThumbnail + '"' : '') + ' > <source src="' + videoURL + '" type="video/' + videoURL.substring(videoURL.lastIndexOf('.') + 1) + '"> </video>';
		});
	}

	/**
	 * Checks whether an element should be embedded depending on whether there are NWS/NMS emoticons in the post or if it is spoilered
	 * @param {Element} element The element that might be NWS or spoilered
	 * @returns {Boolean} False if the element is spoilered or possibly NWS/NMS
	 */
	function filterNwsAndSpoiler(element) {
		// NWS/NMS element
		var nmws = element.closest('.postcontent').querySelector('img[title=":nws:"], img[title=":nms:"]') !== null;
		// Spoilered element
		var spoileredElement = element.parentElement.classList.contains('bbc-spoiler');
		return !nmws && !spoileredElement;
	}
}