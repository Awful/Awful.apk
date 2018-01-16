'use strict';

/*
 * Based on:
 * https://github.com/erikarenhill/Lightweight-JSONP
 *
 * Lightweight JSONP fetcher
 * Copyright 2010-2012 Erik Karlsson. All rights reserved.
 * BSD licensed
 */

/*
 * Usage:
 *
 * JSONP.get( 'someUrl.php', {param1:'123', param2:'456'}, function(data){
 *   //do something with data, which is the JSON object you should retrieve from someUrl.php
 * });
 */
var JSONP = (function JSONP() {
	var counter = 0;
	var head;
	var config = {};

	/**
	 * Loads the url
	 * @param {String} url URL to load
	 * @param {Function} pfnError Error handler
	 */
	function load(url, pfnError) {
		var script = document.createElement('script');
		var done = false;
		script.src = url;
		script.async = true;
		script.classList.add('JSONP');

		var errorHandler = pfnError || config.error;
		if (typeof errorHandler === 'function') {
			script.onerror = function onerror(exception) {
				errorHandler({
					url: url,
					event: exception
				});
			};
		}

		/**
		 * Called when the ready state has changed
		 */
		function readyStateChanged() {
			if (!done && (!this.readyState || this.readyState === 'loaded' || this.readyState === 'complete')) {
				done = true;
				script.onload = null;
				script.onreadystatechange = null;
				if (script && script.parentNode) {
					script.parentNode.removeChild(script);
				}
			}
		}

		script.onload = readyStateChanged;
		script.onreadystatechange = readyStateChanged;

		if (!head) {
			head = document.getElementsByTagName('head')[0];
		}
		head.appendChild(script);
	}

	/**
	 * Performs a JSONP request against the requested URL
	 * @param {String} url URL that is requested
	 * @param {Object} params optional parameters
	 * @param {Function} callback Function that is to be called after the request completes
	 * @param {String} callbackName Optional callback name
	 * @returns {String} The unique callback name that was actually used
	 */
	function jsonp(url, params, callback, callbackName) {
		var query = (url || '').indexOf('?') === -1 ? '?' : '&';
		var key;

		callbackName = callbackName || config.callbackName || 'callback';
		var uniqueName = callbackName + '_json' + ++counter;

		params = params || {};
		for (key in params) {
			if (params.hasOwnProperty(key)) {
				query += encodeURIComponent(key) + '=' + encodeURIComponent(params[key]) + '&';
			}
		}

		window[uniqueName] = function jsonpCallback(data) {
			callback(data);
			try {
				delete window[uniqueName];
			} catch (error) {
				window.console.log(error);
			}
			window[uniqueName] = null;
		};

		load(url + query + callbackName + '=' + uniqueName);
		return uniqueName;
	}

	/**
	 * Overwrites the default configuration
	 * @param {Object} customConfig Configuration object
	 */
	function setDefaults(customConfig) {
		config = customConfig;
	}
	return {
		get: jsonp,
		init: setDefaults
	};
}());