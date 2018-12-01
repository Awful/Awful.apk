'use strict';

var listener;

/**
 * Functions to automatically embed page content, e.g. turn an Instagram URL into a widget
 * @author baka kaba
 * @param {Element} replacementArea The scope of where the embeds are processed, defaults to the document
 */
function processThreadEmbeds(replacementArea) {

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
			embedFunctions[embedType].call(replacementArea);
		}
	}
	// There's no vimeo setting
	replaceVimeo();

	/**
	 * Replaces all instagram links with instagram embeds
	 */
	function embedInstagram() {
		var instagrams = replacementArea.querySelectorAll('.postcontent a[href*="instagr.am/p"],.postcontent a[href*="instagram.com/p"]');
		if (instagrams.length > 0) {
			if (!document.getElementById('instagramScript')) {
			// add the embed script, and run it after all the HTML widgets have been added
				var instagramEmbedScript = document.createElement('script');
				instagramEmbedScript.setAttribute('src', 'https://platform.instagram.com/en_US/embeds.js');
				instagramEmbedScript.id = 'instagramScript';
				document.body.appendChild(instagramEmbedScript);
			}
			instagrams.forEach(function eachInstagramLink(instagramLink) {
				var instaUrl = instagramLink.href;
				fetch('https://api.instagram.com/oembed?omitscript=true&url=' + escape(instaUrl)).then(function parseResponse(response) {
					return response.json();
				}).then(function getInstagrams(data) {
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
		replacementArea.querySelectorAll('.postcontent .bbcode_video object embed[src^="https://vimeo.com"]').forEach(function each(vimeoPlayer) {
			var videoID = vimeoPlayer.getAttribute('src').match(/clip_id=(\d+)/);
			if (videoID === null) {
				return;
			}
			videoID = videoID[1];

			var vimeoIframe = document.createElement('iframe');
			vimeoIframe.setAttribute('src', 'http://player.vimeo.com/video/' + videoID + '?byline=0&portrait=0');
			vimeoIframe.setAttribute('frameborder', 0);
			vimeoIframe.setAttribute('webkitAllowFullScreen', '');
			vimeoIframe.setAttribute('allowFullScreen', '');

			var videoWrapper = document.createElement('div');
			videoWrapper.classList.add('videoWrapper');
			videoWrapper.appendChild(vimeoIframe);
			vimeoPlayer.closest('.bbcode_video').replaceWith(videoWrapper);
		});
	}

	/**
	 * Replaces all twitter status links (tweets) with twitter embeds
	 */
	function embedTweets() {
		var tweets = replacementArea.querySelectorAll('.postcontent a[href*="twitter.com"]');

		tweets = Array.prototype.reduce.call(tweets, function reduceTweets(filteredTwoops, twitterURL) {
			var urlMatch = twitterURL.href.match(/https?:\/\/(?:[\w.]*\.)?twitter\.com\/[\w_]+\/status(?:es)?\/([\d]+)/);
			if (urlMatch && filterNwsAndSpoiler(twitterURL)) {
				twitterURL.href = urlMatch[0];
				filteredTwoops.push(twitterURL);
				return filteredTwoops;
			}
		}, []);

		tweets.forEach(function eachTweet(tweet) {
			var tweetUrl = tweet.href;
			JSONP.get('https://publish.twitter.com/oembed?omit_script=true&url=' + escape(tweetUrl), {}, function getTworts(data) {
				var div = document.createElement('div');
				div.classList.add('tweet');
				tweet.parentNode.replaceChild(div, tweet);
				div.innerHTML = data.html;
				if (document.getElementById('theme-css').dataset.darkTheme === 'true') {
					div.querySelector('blockquote').dataset.theme = 'dark';
				}
				if (window.twttr.init) {
					window.twttr.widgets.load(div);
				} else {
					window.missedEmbeds.push(div);
				}
			});
		});
	}

	/**
	 * Replaces all vine links with vines embeds.
	 */
	function embedVines() {
		var vines = replacementArea.querySelectorAll('.postcontent a[href*="://vine.co/v/"]');

		if (vines.length === 0) {
			return;
		}

		vines = Array.prototype.filter.call(vines, filterNwsAndSpoiler);
		vines.forEach(function eachVine(vine) {
			var vineStock = document.createElement('div');
			vineStock.classList.add('vine-container');
			var vineFrame = document.createElement('iframe');
			vineFrame.classList.add('vine-embed');
			vineFrame.setAttribute('src', vine.href + '/embed/simple');
			vineFrame.setAttribute('frameborder', 0);
			vineStock.appendChild(vineFrame);
			vine.replaceWith(vineStock);
		});

		if (document.getElementById('vineScript')) {
			document.getElementById('vineScript').remove();
		}

		// add the embed script, and run it after all the HTML widgets have been added
		var vineEmbedScript = document.createElement('script');
		vineEmbedScript.setAttribute('src', 'https://platform.vine.co/static/scripts/embed.js');
		vineEmbedScript.setAttribute('charset', 'utf-8');
		vineEmbedScript.id = 'vineScript';
		document.body.appendChild(vineEmbedScript);
	}

	/**
	 * Replaces all links to .webm, .mp4 or .gifv video files with a video tag playing that video.
	 */
	function embedVideos() {
		var videos = replacementArea.querySelectorAll('.postcontent a[href$="webm"], .postcontent a[href$="gifv"], .postcontent a[href$="mp4"]');

		videos = Array.prototype.filter.call(videos, filterNwsAndSpoiler);
		videos.forEach(function eachVideo(video) {
			var hasThumbnail;
			var videoURL = video.href.replace('.gifv', '.mp4');
			if (videoURL.indexOf('imgur.com') !== -1) {
				hasThumbnail = videoURL.substring(0, videoURL.lastIndexOf('.')) + 'm.jpg';
				videoURL = videoURL.replace('.webm', '.mp4');
			} else if (videoURL.indexOf('gfycat.com') !== -1) {
				hasThumbnail = 'https://thumbs' + videoURL.substring(videoURL.indexOf('.'), videoURL.lastIndexOf('.')) + '-mobile.jpg';
				videoURL = 'https://thumbs' + videoURL.substring(videoURL.indexOf('.'), videoURL.lastIndexOf('.')) + '-mobile.mp4';
			}

			var videoElement = document.createElement('video');
			videoElement.setAttribute('loop', 'true');
			videoElement.setAttribute('width', '100%');
			videoElement.setAttribute('muted', 'true');
			videoElement.setAttribute('controls', 'true');
			videoElement.setAttribute('preload', 'none');
			if (hasThumbnail) {
				videoElement.setAttribute('poster', hasThumbnail);
			}

			var sourceElement = document.createElement('source');
			sourceElement.setAttribute('src', videoURL);
			sourceElement.setAttribute('type', 'video/' + videoURL.substring(videoURL.lastIndexOf('.') + 1));
			videoElement.appendChild(sourceElement);

			video.replaceWith(videoElement);
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