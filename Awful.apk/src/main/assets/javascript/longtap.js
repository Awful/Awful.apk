/**
 * https://www.npmjs.com/package/longtap
 */

var cancelEvents = ['touchmove', 'touchend', 'touchleave', 'touchcancel'];

Longtap.threshold = 750;
var timeout;

function Longtap(handler, options) {
	options = options || {};
	listener.handler = handler;
	var threshold = options.threshold || Longtap.threshold;

	return listener;

	function listener(e1) {
		if (timeout) {
			return cleanUp();
		}
		if (!e1.touches || e1.touches.length > 1) return;
		var context = this;
		var args = arguments;
		timeout = setTimeout(done, threshold);

		function done() {
			// only send args[0] (event)
			handler.call(context, args[0]);
		}

		function cleanUp() {
			clearTimeout(timeout);
			timeout = null;
			cancelEvents.forEach(function (name) {
				unbind(document, name, cleanUp);
			});
		}

		cancelEvents.forEach(function (name) {
			bind(document, name, cleanUp);
		});
	}
}
