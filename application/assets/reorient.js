/* Released under the MIT License.
 * Ross Smith
 * 2011-07-11
 * evolutioneer at gmail dot com
 */

(function($) {
	
	var interval;
	var orientation;
	var intervalTime = 500;
	
	var intervalFunction = function()
	{
		var orient = Math.abs(window.orientation) === 90 ? 'landscape' : 'portrait';
		if(orient != $.reorient.orientation)
		{
			$('body')
				.removeClass('no-orientation')
				.removeClass(orientation)
				.addClass(orient);
			$.reorient.orientation = orient;
			$(window).trigger('reorient');
		}
	};
	
	var start = function()
	{
		$.reorient.interval = setInterval("$.reorient.intervalFunction.apply($.reorient);", intervalTime);
	};
	
	var stop = function()
	{
		clearInterval($.reorient.interval);
	};
	
	$.reorient = {
		interval: interval,
		intervalTime: intervalTime,
		orientation: orientation,
		intervalFunction: intervalFunction,
		start: start,
		stop: stop
	};
	
})(Zepto);
