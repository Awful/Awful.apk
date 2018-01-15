//
// Created by baka kaba on 16/08/2017.
//
// Functions to automatically embed page content, e.g. turn an Instagram URL into a widget
//

var listener;

function processThreadEmbeds() {

	// map preference keys to their corresponding embed functions
	var embedFunctions = {
		'inlineInstagram': embedInstagram,
		'replaceVimeo': replaceVimeo,
		'inlineTweets': embedTweets,
		'inlineVines': embedVines,
		'inlineWebm': embedVideos
	};

	// check all embed preference keys - any set to true, run their embed function
	for (var embedType in embedFunctions) {
		if (listener.getPreference(embedType) == 'true') {
			embedFunctions[embedType].call();
		}
	}

	function embedInstagram() {
		// get each Instagram link, and replace it with the HTML from the embed API call
		var promises = [];
		document.querySelectorAll('.postcontent a[href*="instagr.am/p"],.postcontent a[href*="instagram.com/p"]')
			.forEach(function(instagramLink) {
				var url = instagramLink.getA('href');
				var api_call = 'https://api.instagram.com/oembed?omitscript=true&url=' + url + '&callback=?';

				Ajax({
					url: api_call,
					success: function (data) {
						instagramLink.replaceWith(data.html);
					}
				});
			});
		// do nothing if we have nothing to embed
		if (promises.length == 0) {
			return;
		}

		// add the embed script, and run it after all the HTML widgets have been added
		var instagramEmbedScript = new document.createElement('script');
		instagramEmbedScript.setAttribute('src', 'https://platform.instagram.com/en_US/embeds.js');
		document.getElementsByTagName('body').append(instagramEmbedScript);
	}

	function replaceVimeo() {
		document.querySelectorAll('.postcontent .bbcode_video object param[value^="http://vimeo.com"]').forEach(function each(vimeoPlayer) {
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

			param.closest('div.bbcode_video').replaceWith('<div class="videoWrapper"></div>').append(vimeoIframe);
		});
	}

	function embedTweets() {
		var tweets = document.querySelectorAll('.postcontent a[href*="twitter.com"]');

		//NWS/NMS links
		//tweets = tweets.not(".postcontent:has(img[title=':nws:']) a").not(".postcontent:has(img[title=':nms:']) a");

		// spoiler'd links
		Array.prototype.filter.call(tweets, filterNwsAndSpoiler);
		tweets.forEach(function(tweet) {

			var tweetUrl = tweet.getAttribute('href');
			Ajax({
				url: 'https://publish.twitter.com/oembed?omit_script=true&url='+escape(tweetUrl),
				success: function (data) {
					var div = document.createElement('div');
					div.classList.add('tweet');
					tweet.parentNode.insertBefore(div, tweet);
					tweet.parentNode.removeChild(tweet);
					div.appendChild(tweet);
					div.innerHTML = data.html;
					if(document.getElementById('theme-css').getAttribute('data-dark-theme') === 'true'){
						div.querySelector('blockquote').setAttribute('data-theme', 'dark');
					}
					window.twttr.widgets.load(div);
				}
			});
		});
	}

	function embedVines() {
		var vines = document.querySelectorAll('.postcontent a[href*="://vine.co/v/"]');

		Array.prototype.filter.call(vines, filterNwsAndSpoiler);

		// spoiler'd links
		vines.forEach(function(vine) {
			vine.innerHTML = '<iframe class="vine-embed" src="' + vine.getAttribute('href') + '/embed/simple" frameborder="0"></iframe>'+
							'<script async src="http://platform.vine.co/static/scripts/embed.js" charset="utf-8"></script>';
		});
	}

	function embedVideos() {
		var videos = document.querySelectorAll('.postcontent a[href$="webm"],.postcontent a[href$="gifv"],.postcontent a[href$="mp4"]');


		Array.prototype.filter.call(videos, filterNwsAndSpoiler);
		videos.forEach(function(video) {
			var hasThumbnail;
			video.setAttribute('href', video.getAttribute('href').replace('.gifv','.mp4'));
			var videoURL = video.getAttribute('href');
			if(videoURL.indexOf('imgur.com') !== -1){
				hasThumbnail = videoURL.substring(0, videoURL.lastIndexOf('.'))+'m.jpg';
				video.setAttribute('href', videoURL.replace('.webm','.mp4'));
			} else if(videoURL.indexOf('gfycat.com') !== -1){
				hasThumbnail = 'https://thumbs' + videoURL.substring(videoURL.indexOf('.'), videoURL.lastIndexOf('.')) + '-mobile.jpg';
				video.setAttribute('href','https://thumbs' + videoURL.substring(videoURL.indexOf('.'), videoURL.lastIndexOf('.')) + '-mobile.mp4');
			}
			video.replaceWith('<video loop width="100%" muted="true" controls preload="none" '+(hasThumbnail !== undefined ?'poster="'+hasThumbnail+'"':'')+' > <source src="'+videoURL+'" type="video/'+videoURL.substring(videoURL.lastIndexOf('.')+1)+'"> </video>');
		});
	}

	function filterNwsAndSpoiler(tweet){
		// NWS/NMS tweet
		var nmws = tweet.closest('.postcontent').querySelector('img[title=":nws:"], img[title=":nms:"]') !== null;
		var spoilerTweet = tweet.parentElement.classList.contains('bbc-spoiler');
		return !nmws && !spoilerTweet;
	}
}