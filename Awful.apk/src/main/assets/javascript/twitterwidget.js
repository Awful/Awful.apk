'use strict';
window.missedEmbeds = [];

window.twttr = (function twttr(page, tagName, id) {
	var scriptTag;
	var firstTag = page.getElementsByTagName(tagName)[0];
	var twttr = window.twttr || {};
	if (page.getElementById(id)) {
		return twttr;
	}

	twttr._e = []; // eslint-disable-line id-length
	twttr.ready = function ready(callback) {
		twttr._e.push(callback);
	};
	twttr.insertTag = function insertTag() {
		scriptTag = page.createElement(tagName);
		scriptTag.id = id;
		scriptTag.src = 'https://platform.twitter.com/widgets.js';
		if (page.getElementById(id)) {
			firstTag.parentNode.replaceChild(scriptTag, page.getElementById(id));
		} else {
			firstTag.parentNode.insertBefore(scriptTag, firstTag);
		}
	};

	twttr.ready(function loadEmbeds(twttr) {
		window.missedEmbeds.forEach(function loadEmbed(embed) {
			twttr.widgets.load(embed);
		});
	});

	return twttr;
}(document, 'script', 'twitter-wjs'));
/* eslint-enable */
