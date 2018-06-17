'use strict';

/**
 * Based on
 * https://www.npmjs.com/package/longtap
 */

/**
 * Returns a listener function for a longtap
 * @param {Function} handler Function to handle the longtap that is calculated
 * @param {Object} options Object offering customization
 * @returns {Function} Eventhandling function
 */
function Longtap(handler, options) {
	options = options || {};
	listener.handler = handler;
	var threshold = options.threshold || 750;
	var cancelEvents = ['touchmove', 'touchend', 'touchcancel'];
	var timeout;

	return listener;

	/**
	 * Eventhandler for touchstart that calculates whether the touch is in fact a longtap
	 * @param {Event} baseEvent The overlaying touchstart Event that is fired by the user
	 * @returns {undefined}
	 */
	function listener(baseEvent) {
		if (timeout) {
			cleanUp();
			return;
		}
		if (!baseEvent.touches || baseEvent.touches.length > 1) {
			return;
		}
		timeout = setTimeout(done, threshold);

		/**
		 * Function that is called when the longtap is done
		 */
		function done() {
			handler.call(this, baseEvent);
		}

		/**
		 * Function that cleans up the eventhandler
		 */
		function cleanUp() {
			clearTimeout(timeout);
			timeout = null;
			cancelEvents.forEach(function eachEventCancel(name) {
				document.removeEventListener(name, cleanUp, false);
			});
		}
		cancelEvents.forEach(function eachEventRegister(name) {
			document.addEventListener(name, cleanUp, false);
		});
	}
}