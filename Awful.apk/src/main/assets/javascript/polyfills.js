'use strict';
/* eslint-disable */

(function (ElementProto) {
	if (typeof ElementProto.matches !== 'function') {
		ElementProto.matches = ElementProto.msMatchesSelector || ElementProto.mozMatchesSelector || ElementProto.webkitMatchesSelector || function matches(selector) {
			var element = this;
			var elements = (element.document || element.ownerDocument).querySelectorAll(selector);
			var index = 0;

			while (elements[index] && elements[index] !== element) {
				++index;
			}

			return Boolean(elements[index]);
		};
	}

	if (typeof ElementProto.closest !== 'function') {
		ElementProto.closest = function closest(selector) {
			var element = this;

			while (element && element.nodeType === 1) {
				if (element.matches(selector)) {
					return element;
				}

				element = element.parentNode;
			}

			return null;
		};
	}

	if (typeof ElementProto.replaceWith !== 'function') {
		ElementProto.replaceWith = function replaceWith(Ele) {
            var parent = this.parentNode,
                i = arguments.length,
                firstIsNode = +(parent && typeof Ele === 'object');
            if (!parent) return;

            while (i-- > firstIsNode){
                if (parent && typeof arguments[i] !== 'object'){
                    arguments[i] = document.createTextNode(arguments[i]);
                } if (!parent && arguments[i].parentNode){
                    arguments[i].parentNode.removeChild(arguments[i]);
                    continue;
                }
                parent.insertBefore(this.previousSibling, arguments[i]);
            }
            if (firstIsNode) parent.replaceChild(Ele, this);
        };
	}
})(window.Element.prototype);

(function (ElementProto) {
	if (typeof ElementProto.forEach !== 'function') {
		ElementProto.forEach = function forEach(callback, thisArg) {
			thisArg = thisArg || window;
			for (var i = 0; i < this.length; i++) {
				callback.call(thisArg, this[i], i, this);
			}
		};
	}
})(window.NodeList.prototype);

(function (ElementProto) {
	if (typeof ElementProto.endsWith !== 'function') {
		ElementProto.endsWith = function endsWith(search, this_len) {
			if (this_len === undefined || this_len > this.length) {
				this_len = this.length;
			}
			return this.substring(this_len - search.length, this_len) === search;
		};
	}
	if (typeof ElementProto.startsWith !== 'function') {
		ElementProto.startsWith = function startsWith(search, pos) {
			return this.substr(!pos || pos < 0 ? 0 : +pos, search.length) === search;
		};
	}
})(window.String.prototype);